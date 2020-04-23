package fr.raksrinana.cursedownloader.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@NoArgsConstructor
public class CLIParameters{
	@Parameter(names = {
			"-c",
			"--client"
	}, description = "The path to the client mods folder", converter = PathConverter.class)
	private Path clientPath = Paths.get("client/mods");
	@Parameter(names = {
			"-s",
			"--server"
	}, description = "The path to the server mods folder", converter = PathConverter.class)
	private Path serverPath = Paths.get("server/mods");
	@Parameter(names = {
			"-m",
			"--mods"
	}, description = "The path to the settings file", converter = PathConverter.class)
	private Path settingsPath = Paths.get("mods.json5");
	@Parameter(names = {
			"-u",
			"--update"
	}, description = "Flag to check mod updates before downloading")
	private boolean update = false;
	@Parameter(names = {
			"-h",
			"--help"
	}, help = true)
	private boolean help = false;
}
