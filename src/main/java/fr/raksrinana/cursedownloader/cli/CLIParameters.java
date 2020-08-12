package fr.raksrinana.cursedownloader.cli;

import lombok.Getter;
import lombok.NoArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@NoArgsConstructor
@Command(name = "cursedownloader", mixinStandardHelpOptions = true)
public class CLIParameters{
	@Option(names = {
			"-c",
			"--client"
	}, description = "The path to the client mods folder")
	private Path clientPath;
	@Option(names = {
			"-s",
			"--server"
	}, description = "The path to the server mods folder")
	private Path serverPath;
	@Option(names = {
			"-m",
			"--mods"
	}, description = "The path to the settings file")
	private Path settingsPath = Paths.get("mods.json5");
	@Option(names = {
			"-u",
			"--update"
	}, description = "Flag to check mod updates before downloading")
	private boolean update = false;
}
