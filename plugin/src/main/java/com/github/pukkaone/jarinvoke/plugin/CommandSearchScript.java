package com.github.pukkaone.jarinvoke.plugin;

import com.github.pukkaone.jarinvoke.Command;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;

/**
 * Runs command during search.
 */
@Getter
@Setter
public class CommandSearchScript extends SearchScript {

  private final Command command;

  public CommandSearchScript(
      Command command,
      Map<String, Object> params,
      SearchLookup lookup,
      LeafReaderContext leafContext) {

    super(params, lookup, leafContext);
    this.command = command;
  }

  @Override
  public Object run() {
    return command.execute(getParams(), getDoc());
  }

  @Override
  public double runAsDouble() {
    return ((Number) run()).doubleValue();
  }
}
