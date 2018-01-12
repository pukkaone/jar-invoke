package com.github.pukkaone.jarinvoke.plugin;

import com.github.pukkaone.jarinvoke.Command;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;

/**
 * Creates command search script factory.
 */
@RequiredArgsConstructor
public class CommandSearchScriptFactory implements SearchScript.Factory {

  private final Command command;

  @Override
  public SearchScript.LeafFactory newFactory(Map<String, Object> params, SearchLookup lookup) {
    return new CommandSearchScriptLeafFactory(command, params, lookup);
  }
}
