package com.github.pukkaone.jarinvoke.plugin;

import com.github.pukkaone.jarinvoke.Command;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;

/**
 * Creates command search script.
 */
@RequiredArgsConstructor
public class CommandSearchScriptLeafFactory implements SearchScript.LeafFactory {

  private final Command command;
  private final Map<String, Object> params;
  private final SearchLookup lookup;

  @Override
  public SearchScript newInstance(LeafReaderContext context) throws IOException {
    return new CommandSearchScript(command, params, lookup, context);
  }

  @Override
  public boolean needs_score() {
    return false;
  }
}
