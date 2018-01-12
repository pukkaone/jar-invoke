package com.github.pukkaone.jarinvoke.plugin;

import com.github.pukkaone.jarinvoke.Command;
import com.github.pukkaone.jarinvoke.ModuleResolver;
import java.util.Map;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;

/**
 * Script engine to invoke Java code.
 */
public class JarInvokeScriptEngine implements ScriptEngine {

  public static final String LANGUAGE = "jar-invoke";

  private ModuleResolver moduleResolver = new ModuleResolver();

  @Override
  public String getType() {
    return LANGUAGE;
  }

  @Override
  public <T> T compile(
      String scriptName,
      String scriptSource,
      ScriptContext<T> context,
      Map<String, String> params) {

    Command command = moduleResolver.parse(scriptSource);
    CommandSearchScriptFactory factory = new CommandSearchScriptFactory(command);
    return context.factoryClazz.cast(factory);
  }

  @Override
  public void close() {
    moduleResolver.close();
  }
}
