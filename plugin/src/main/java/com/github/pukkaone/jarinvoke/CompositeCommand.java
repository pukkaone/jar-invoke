package com.github.pukkaone.jarinvoke;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.index.fielddata.ScriptDocValues;

/**
 * Executes multiple commands.
 */
@Data
@RequiredArgsConstructor
public class CompositeCommand implements Command {

  private final List<Command> commands;

  @Override
  public Object execute(Map<String, Object> variables, Map<String, ScriptDocValues> docLookup) {
    Object result = null;
    for (Command command : commands) {
      result = command.execute(variables, docLookup);
    }

    return result;
  }
}
