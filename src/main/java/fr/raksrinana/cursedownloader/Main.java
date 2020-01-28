package fr.raksrinana.cursedownloader;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.file.CurseFile;
import com.therandomlabs.curseapi.util.OkHttpUtils;
import fr.raksrinana.cursedownloader.cli.CLIParameters;
import fr.raksrinana.cursedownloader.model.ModFile;
import fr.raksrinana.cursedownloader.model.SidesMods;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class Main{
	public static void main(final String[] args){
		final var parameters = new CLIParameters();
		try{
			JCommander.newBuilder().addObject(parameters).build().parse(args);
		}
		catch(final ParameterException e){
			log.error("Failed to parse arguments", e);
			e.usage();
			return;
		}
		try{
			Files.createDirectories(parameters.getServerPath());
			Files.createDirectories(parameters.getClientPath());
		}
		catch(IOException e){
			log.error("Failed to create mods folders", e);
			return;
		}
		loadSettings(parameters.getSettingsPath()).ifPresentOrElse(sidesMods -> {
			processUpdates(sidesMods.getAll(), parameters.getClientPath(), parameters.getServerPath());
			processUpdates(sidesMods.getClient(), parameters.getClientPath());
			processUpdates(sidesMods.getServer(), parameters.getServerPath());
		}, () -> log.warn("Mod list couldn't be loaded"));
		OkHttpUtils.getClient().connectionPool().evictAll();
		OkHttpUtils.getClient().dispatcher().executorService().shutdown();
	}
	
	@NonNull
	public static Optional<SidesMods> loadSettings(@NonNull final Path path){
		if(path.toFile().exists()){
			final var mapper = new ObjectMapper();
			mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY).withGetterVisibility(JsonAutoDetect.Visibility.NONE).withSetterVisibility(JsonAutoDetect.Visibility.NONE).withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
			mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
			mapper.enable(JsonParser.Feature.ALLOW_TRAILING_COMMA);
			final var objectReader = mapper.readerFor(SidesMods.class);
			try(final var fis = new FileInputStream(path.toFile())){
				return Optional.ofNullable(objectReader.readValue(fis));
			}
			catch(final IOException e){
				log.error("Failed to read settings in {}", path, e);
			}
		}
		return Optional.empty();
	}
	
	private static void processUpdates(Set<ModFile> modFiles, Path... outputs){
		final var outputSet = Set.of(outputs);
		modFiles.forEach(modFile -> {
			log.info("Processing {}", modFile);
			getCurseFile(modFile).ifPresentOrElse(file -> outputSet.forEach(output -> downloadFileToFolder(output, file)), () -> log.warn("Mod not found {}", modFile));
		});
	}
	
	@NonNull
	private static Optional<CurseFile> getCurseFile(@NonNull ModFile modFile){
		try{
			return CurseAPI.file(modFile.getProject(), modFile.getFile());
		}
		catch(CurseException e){
			log.warn("Failed to get mod file {}", modFile, e);
		}
		return Optional.empty();
	}
	
	private static void downloadFileToFolder(@NonNull Path directory, @NonNull CurseFile file){
		final var target = directory.resolve(file.projectID() + "_" + file.id() + "_" + file.nameOnDisk());
		if(Files.exists(target)){
			log.debug("File {} already present", target);
			return;
		}
		try{
			Files.walkFileTree(directory, new DeleteSameModFileWalker(file));
			log.info("Downloading {} to {}", file, target);
			file.download(target);
		}
		catch(Exception e){
			log.error("Failed to download {} to {}", file, target, e);
		}
	}
}
