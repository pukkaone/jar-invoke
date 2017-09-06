package com.github.pukkaone.jarinvoke.plugin;

import com.github.pukkaone.jarinvoke.Command;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.script.AbstractExecutableScript;

/**
 * Runs command.
 */
@RequiredArgsConstructor
public class CommandExecutableScript extends AbstractExecutableScript {

  private final Command command;
  private final Map<String, Object> vars;

  @Override
  public Object run() {
    return command.execute(vars, null);
  }
}
