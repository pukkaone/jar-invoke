package com.github.pukkaone.jarinvoke;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.index.fielddata.ScriptDocValues;

/**
 * Command to load JAR file if not already loaded.
 */
@Data
@RequiredArgsConstructor
public class RequireCommand implements Command {

  private final ModuleResolver moduleResolver;
  private final String moduleName;
  private final String repositoryUri;
  private final String jarCoordinates;

  @Override
  public Object execute(Map<String, Object> variables, Map<String, ScriptDocValues<?>> docLookup) {
    moduleResolver.require(moduleName, repositoryUri, jarCoordinates);
    return true;
  }
}
