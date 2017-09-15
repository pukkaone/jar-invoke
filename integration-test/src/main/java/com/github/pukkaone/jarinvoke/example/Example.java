package com.github.pukkaone.jarinvoke.example;

import java.util.Map;
import org.elasticsearch.index.fielddata.ScriptDocValues;

/**
 * Example Java methods invoked from Elasticsearch script.
 */
public class Example {

  public static String echoVariables(
      Map<String, Object> variables, Map<String, ScriptDocValues> docLookup) {

    return variables.toString();
  }

  public static Object getDocValue(
      Map<String, Object> variables, Map<String, ScriptDocValues> docLookup) {

    String fieldName = variables.get("field").toString();
    return docLookup.get(fieldName).get(0);
  }
}
