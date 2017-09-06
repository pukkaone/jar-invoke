package com.github.pukkaone.jarinvoke;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.index.fielddata.ScriptDocValues;

/**
 * Command to invoke static method of a Java class.
 */
@Data
@RequiredArgsConstructor
public class InvokeCommand implements Command {

  private final ModuleResolver moduleResolver;
  private final String moduleName;
  private final String className;
  private final String methodName;

  @Override
  public Object execute(Map<String, Object> variables, Map<String, ScriptDocValues> docLookup) {
    return moduleResolver.invoke(moduleName, className, methodName, variables, docLookup);
  }
}
