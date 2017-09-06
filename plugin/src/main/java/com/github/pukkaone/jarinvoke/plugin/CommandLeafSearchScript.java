package com.github.pukkaone.jarinvoke.plugin;

import com.github.pukkaone.jarinvoke.Command;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.search.Scorer;
import org.elasticsearch.script.AbstractExecutableScript;
import org.elasticsearch.script.LeafSearchScript;
import org.elasticsearch.search.lookup.LeafSearchLookup;

/**
 * Runs command during search.
 */
@Getter
@Setter
public class CommandLeafSearchScript extends AbstractExecutableScript implements LeafSearchScript {

  private final LeafSearchLookup lookup;
  private Scorer scorer;
  private final Command command;
  private final Map<String, Object> vars;

  public CommandLeafSearchScript(
      Command command, Map<String, Object> vars, LeafSearchLookup lookup) {

    this.lookup = lookup;
    this.command = command;
    this.vars = vars;
  }

  @Override
  public void setDocument(int doc) {
    lookup.setDocument(doc);
  }

  @Override
  public void setSource(Map<String, Object> source) {
    lookup.source().setSource(source);
  }

  @Override
  public double runAsDouble() {
    return ((Number) run()).doubleValue();
  }

  @Override
  public float runAsFloat() {
    return ((Number) run()).floatValue();
  }

  @Override
  public long runAsLong() {
    return ((Number) run()).longValue();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object run() {
    return command.execute(vars, lookup.doc());
  }
}
