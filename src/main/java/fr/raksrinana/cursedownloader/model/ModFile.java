package fr.raksrinana.cursedownloader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor
public class ModFile implements Comparable<ModFile>{
	@JsonProperty("name")
	private String name;
	@JsonProperty("project")
	private int project;
	@JsonProperty("file")
	private int file;
	
	@Override
	public String toString(){
		return getName() + '(' + getProject() + ')';
	}
	
	@Override
	public int compareTo(ModFile o){
		return getName().compareTo(o.getName());
	}
}
