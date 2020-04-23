package fr.raksrinana.cursedownloader;

import com.therandomlabs.curseapi.file.CurseFile;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

@Slf4j
public class DeleteSameModFileWalker extends SimpleFileVisitor<Path>{
	private final Pattern pattern;
	
	public DeleteSameModFileWalker(@NonNull CurseFile file){
		this.pattern = Pattern.compile(file.projectID() + "_\\d+_.+");
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException{
		if(attrs.isDirectory()){
			return FileVisitResult.SKIP_SUBTREE;
		}
		if(pattern.matcher(file.getFileName().toString()).matches()){
			log.info("Deleting file {}", file);
			Files.delete(file);
		}
		return FileVisitResult.CONTINUE;
	}
}
