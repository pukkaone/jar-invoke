package com.github.pukkaone.jarinvoke.plugin;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.ScriptModule;

/**
 * Elasticsearch plugin to invoke Java code.
 */
public class JarInvokePlugin extends Plugin {

  public static final String NAME = "jar-invoke";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String description() {
    return "Invokes Java code";
  }

  public void onModule(ScriptModule module) {
    module.addScriptEngine(JarInvokeScriptEngine.class);
  }
}
