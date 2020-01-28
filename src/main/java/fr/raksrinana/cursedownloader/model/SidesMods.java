package fr.raksrinana.cursedownloader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor
public class SidesMods{
	@JsonProperty("all")
	private final Set<ModFile> all = new HashSet<>();
	@JsonProperty("client")
	private final Set<ModFile> client = new HashSet<>();
	@JsonProperty("server")
	private final Set<ModFile> server = new HashSet<>();
}
