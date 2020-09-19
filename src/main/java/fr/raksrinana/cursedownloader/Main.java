package fr.raksrinana.cursedownloader;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.file.CurseFile;
import com.therandomlabs.curseapi.file.CurseFiles;
import com.therandomlabs.curseapi.util.OkHttpUtils;
import fr.raksrinana.cursedownloader.cli.CLIParameters;
import fr.raksrinana.cursedownloader.model.Configuration;
import fr.raksrinana.cursedownloader.model.ModFile;
import fr.raksrinana.cursedownloader.model.UpdateTags;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import static java.time.ZoneOffset.UTC;

@Slf4j
public class Main implements Callable<Void>{
	private static final DateTimeFormatter DF = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	private static ObjectMapper mapper;
	
	public static void main(String[] args){
		// args = Arrays.copyOfRange(args, 5, 10);
		var parameters = new CLIParameters();
		var cli = new CommandLine(parameters);
		cli.registerConverter(Path.class, Paths::get);
		cli.setUnmatchedArgumentsAllowed(true);
		try{
			cli.parseArgs(args);
		}
		catch(final CommandLine.ParameterException e){
			log.error("Failed to parse arguments", e);
			cli.usage(System.out);
			return;
		}
		
		final var factoryBuilder = new JsonFactoryBuilder();
		factoryBuilder.enable(JsonReadFeature.ALLOW_TRAILING_COMMA);
		mapper = new ObjectMapper(factoryBuilder.build());
		mapper.setVisibility(mapper.getSerializationConfig()
				.getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		try{
			if(Objects.nonNull(parameters.getClientPath())){
				Files.createDirectories(parameters.getClientPath());
			}
			if(Objects.nonNull(parameters.getServerPath())){
				Files.createDirectories(parameters.getServerPath());
			}
		}
		catch(IOException e){
			log.error("Failed to create mods folders", e);
			return;
		}
		loadSettings(parameters.getSettingsPath()).ifPresentOrElse(configuration -> {
			if(parameters.isUpdate()){
				checkUpdates(configuration.getEverything(), configuration.getUpdateTags());
			}
			processUpdates(configuration.getAll(), parameters.getClientPath(), parameters.getServerPath());
			processUpdates(configuration.getClient(), parameters.getClientPath());
			processUpdates(configuration.getServer(), parameters.getServerPath());
			saveSettings(parameters.getSettingsPath(), configuration);
		}, () -> log.warn("Mod list couldn't be loaded"));
		OkHttpUtils.getClient().connectionPool().evictAll();
		OkHttpUtils.getClient().dispatcher().executorService().shutdown();
	}
	
	@NonNull
	public static Optional<Configuration> loadSettings(@NonNull final Path path){
		if(Files.exists(path)){
			try(final var fis = Files.newBufferedReader(path)){
				return Optional.ofNullable(mapper.readValue(fis, Configuration.class));
			}
			catch(final IOException e){
				log.error("Failed to read settings in {}", path, e);
			}
		}
		return Optional.empty();
	}
	
	private static void checkUpdates(Set<ModFile> mods, UpdateTags updateTags){
		final var scanner = new Scanner(System.in);
		final var fallbackDate = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, UTC);
		mods.forEach(mod -> {
			final var currentFile = getCurseFile(mod);
			final var baseDate = currentFile.map(CurseFile::uploadTime).orElse(fallbackDate);
			getCurseProjectFiles(mod).ifPresent(files -> {
				final var newerFiles = files.stream()
						.filter(file -> file.uploadTime().isAfter(baseDate))
						.filter(file -> file.gameVersionStrings().containsAll(updateTags.getRequired()))
						.filter(file -> file.gameVersionStrings().stream().noneMatch(tag -> updateTags.getExcluded().contains(tag)))
						.limit(20)
						.collect(Collectors.toSet());
				askSelection(newerFiles, currentFile.orElse(null), scanner).map(CurseFile::id).ifPresent(mod::setFile);
			});
		});
	}
	
	private static void saveSettings(@NonNull Path path, @NonNull Configuration configuration){
		try{
			mapper.writeValue(path.toFile(), configuration);
		}
		catch(IOException e){
			log.error("Failed to write settings to {}", path, e);
		}
	}
	
	@NonNull
	private static Optional<CurseFile> getCurseFile(@NonNull ModFile modFile){
		try{
			return CurseAPI.file(modFile.getProject(), modFile.getFile());
		}
		catch(CurseException | IllegalArgumentException e){
			log.warn("Failed to get mod file {} => {}", modFile, e.getMessage());
		}
		return Optional.empty();
	}
	
	@NonNull
	private static Optional<CurseFiles<CurseFile>> getCurseProjectFiles(@NonNull ModFile modFile){
		try{
			return CurseAPI.files(modFile.getProject());
		}
		catch(CurseException e){
			log.warn("Failed to get project files {} => {}", modFile, e.getMessage());
		}
		return Optional.empty();
	}
	
	private static Optional<CurseFile> askSelection(Set<CurseFile> files, CurseFile currentFile, Scanner scanner){
		if(files.isEmpty()){
			return Optional.empty();
		}
		log.info("Current file is: {}", Optional.ofNullable(currentFile).map(Main::formatFile).orElse("Unknown"));
		final var selection = files.stream().sorted(Comparator.comparing(CurseFile::uploadTime).reversed())
				.map(Main::formatFile)
				.collect(Collectors.joining("\n"));
		log.info("Please select your choice:\n" + selection);
		final var response = scanner.nextInt();
		return files.stream().filter(file -> Objects.equals(response, file.id())).findFirst();
	}
	
	private static String formatFile(CurseFile file){
		return MessageFormat.format("{0,number,#} => {1} ({2} : {3}) {4} on {5}",
				file.id(),
				file.displayName(),
				file.releaseType().name(),
				file.nameOnDisk(),
				file.gameVersionStrings(),
				file.uploadTime().format(DF));
	}
	
	private static void processUpdates(Set<ModFile> modFiles, Path... outputs){
		final var outputSet = Arrays.stream(outputs)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		modFiles.stream().sorted().forEach(modFile -> {
			log.info("Processing {}", modFile);
			getCurseFile(modFile)
					.ifPresentOrElse(file -> outputSet.forEach(output -> downloadFileToFolder(output, file)),
							() -> log.warn("Mod not found {}", modFile));
		});
	}
	
	private static void downloadFileToFolder(@NonNull Path directory, @NonNull CurseFile file){
		final var target = directory.resolve(file.projectID() + "_" + file.id() + "_" + file.nameOnDisk());
		if(Files.exists(target)){
			log.debug("File {} already present", target);
			return;
		}
		try{
			Files.walkFileTree(directory, new DeleteSameModFileWalker(file));
			log.info("Downloading {} to {}", file.displayName(), target);
			file.download(target);
		}
		catch(Exception e){
			log.error("Failed to download {} to {}", file, target, e);
		}
	}
	
	@Override
	public Void call() throws Exception{
		return null;
	}
}
