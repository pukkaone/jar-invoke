package com.github.pukkaone.jarinvoke.plugin;

import com.github.pukkaone.jarinvoke.Command;
import com.github.pukkaone.jarinvoke.ModuleResolver;
import java.io.IOException;
import java.util.Map;
import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.LeafSearchScript;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;

/**
 * Script engine to invoke Java code.
 */
public class JarInvokeScriptEngine implements ScriptEngineService {

  private ModuleResolver moduleResolver = new ModuleResolver();

  @Override
  public String[] types() {
    return new String[] { JarInvokePlugin.NAME };
  }

  @Override
  public String[] extensions() {
    return types();
  }

  @Override
  public boolean sandboxed() {
    return false;
  }

  @Override
  public Object compile(String scriptSource, Map<String, String> params) {
    return moduleResolver.parse(scriptSource);
  }

  @Override
  public ExecutableScript executable(CompiledScript compiledScript, Map<String, Object> vars) {
    Command command = (Command) compiledScript.compiled();
    return new CommandExecutableScript(command, vars);
  }

  @Override
  public SearchScript search(
      CompiledScript compiledScript, SearchLookup lookup, Map<String, Object> vars) {

    Command command = (Command) compiledScript.compiled();
    return new SearchScript() {
      @Override
      public LeafSearchScript getLeafSearchScript(LeafReaderContext context) throws IOException {
        return new CommandLeafSearchScript(
            command,
            vars,
            lookup.getLeafSearchLookup(context));
      }

      @Override
      public boolean needsScores() {
        return false;
      }
    };
  }

  @Override
  public void close() {
    moduleResolver.close();
  }
}
