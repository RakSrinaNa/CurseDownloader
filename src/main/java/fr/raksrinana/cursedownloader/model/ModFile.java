package fr.raksrinana.cursedownloader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.StringJoiner;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor
public class ModFile{
	@JsonProperty("name")
	private String name;
	@JsonProperty("project")
	private int project;
	@JsonProperty("file")
	private int file;
	
	@Override
	public String toString(){
		return new StringJoiner(", ", ModFile.class.getSimpleName() + "[", "]").add("name=" + name).add("project=" + project).add("file=" + file).toString();
	}
}
