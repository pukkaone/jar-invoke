package com.github.pukkaone.jarinvoke;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.index.fielddata.ScriptDocValues;

/**
 * Command to load JAR file.
 */
@Data
@RequiredArgsConstructor
public class LoadCommand implements Command {

  private final ModuleResolver moduleResolver;
  private final String moduleName;
  private final String repositoryUri;
  private final String jarCoordinates;

  @Override
  public Object execute(Map<String, Object> variables, Map<String, ScriptDocValues<?>> docLookup) {
    moduleResolver.load(moduleName, repositoryUri, jarCoordinates);
    return true;
  }
}
