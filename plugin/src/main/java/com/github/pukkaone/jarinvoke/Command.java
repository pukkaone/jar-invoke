package com.github.pukkaone.jarinvoke;

import java.util.Map;
import org.elasticsearch.index.fielddata.ScriptDocValues;

/**
 * Compiled command.
 */
public interface Command {

  /**
   * Executes command.
   *
   * @param variables
   *     script variable names and values
   * @param docLookup
   *     document field names and values
   * @return value which script returns
   */
  Object execute(Map<String, Object> variables, Map<String, ScriptDocValues<?>> docLookup);
}
