package com.github.pukkaone.jarinvoke;

import com.github.pukkaone.jarinvoke.parser.JarInvokeLexer;
import com.github.pukkaone.jarinvoke.parser.JarInvokeParser;
import com.github.pukkaone.jarinvoke.parser.JarInvokeParser.InvokeExpressionContext;
import com.github.pukkaone.jarinvoke.parser.JarInvokeParser.LoadStatementContext;
import com.github.pukkaone.jarinvoke.parser.JarInvokeParser.TranslationUnitContext;
import java.io.Closeable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.index.fielddata.ScriptDocValues;

/**
 * Resolves name to module.
 */
public class ModuleResolver implements Closeable {

  private Map<String, Module> nameToModuleMap = new ConcurrentHashMap<>();

  private static String trimQuotes(String input) {
    if (input.length() < 2) {
      return input;
    }

    if (input.startsWith("'") && input.endsWith("'")) {
      return input.substring(1, input.length() - 1);
    }

    if (input.startsWith("\"") && input.endsWith("\"")) {
      return input.substring(1, input.length() - 1);
    }

    return input;
  }

  private InvokeCommand createInvokeCommand(InvokeExpressionContext invokeStatement) {
    String moduleName = invokeStatement.IDENTIFIER().getText();
    String className = trimQuotes(invokeStatement.STRING_LITERAL(0).getText());
    String methodName = trimQuotes(invokeStatement.STRING_LITERAL(1).getText());
    return new InvokeCommand(this, moduleName, className, methodName);
  }

  private LoadCommand createLoadCommand(LoadStatementContext loadStatement) {
    String moduleName = loadStatement.IDENTIFIER().getText();
    String repositoryUri = trimQuotes(loadStatement.STRING_LITERAL(0).getText());
    String mavenCoordinates = trimQuotes(loadStatement.STRING_LITERAL(1).getText());
    return new LoadCommand(this, moduleName, repositoryUri, mavenCoordinates);
  }

  /**
   * Parses script source.
   *
   * @param scriptSource
   *     to parse
   * @return command
   */
  public Command parse(String scriptSource) {
    CharStream characters = CharStreams.fromString(scriptSource);
    JarInvokeLexer lexer = new JarInvokeLexer(characters);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    JarInvokeParser parser = new JarInvokeParser(tokens);
    TranslationUnitContext translationUnit = parser.translationUnit();

    if (translationUnit.invokeExpression() != null) {
      return createInvokeCommand(translationUnit.invokeExpression());
    } else if (translationUnit.loadStatement() != null) {
      return createLoadCommand(translationUnit.loadStatement());
    }

    throw new IllegalArgumentException("Failed to parse " + scriptSource);
  }

  private Void doLoad(String moduleName, String repositoryUri, String mavenCoordinates) {
    Module module = nameToModuleMap.get(moduleName);
    if (module != null) {
      module.close();
    }

    module = new Module(repositoryUri, mavenCoordinates);
    nameToModuleMap.put(moduleName, module);

    return null;
  }

  /**
   * Loads JAR file from Maven repository.
   *
   * @param moduleName
   *     name of module to assign
   * @param repositoryUri
   *     repository URI
   * @param mavenCoordinates
   *     group ID, artifact ID and version separated by {@code :}
   */
  public synchronized void load(String moduleName, String repositoryUri, String mavenCoordinates) {
    AccessController.doPrivileged((PrivilegedAction<Void>) () ->
        doLoad(moduleName, repositoryUri, mavenCoordinates));
  }

  private Object doInvoke(
      String moduleName,
      String className,
      String methodName,
      Map<String, Object> variables,
      Map<String, ScriptDocValues> docLookup) {

    Module module = nameToModuleMap.get(moduleName);
    if (module == null) {
      throw new IllegalArgumentException("Unknown module " + moduleName);
    }

    return module.invoke(className, methodName, variables, docLookup);
  }

  /**
   * Invokes static method of a Java class. The method must accept two Map parameters.
   *
   * @param moduleName
   *     module name
   * @param className
   *     class name
   * @param methodName
   *     method name
   * @param variables
   *     script variable names and values
   * @param docLookup
   *     document field names and values
   * @return method return value
   */
  public Object invoke(
      String moduleName,
      String className,
      String methodName,
      Map<String, Object> variables,
      Map<String, ScriptDocValues> docLookup) {

    return AccessController.doPrivileged((PrivilegedAction<Object>) () ->
        doInvoke(moduleName, className, methodName, variables, docLookup));
  }

  private Void doClose() {
    nameToModuleMap.forEach((name, module) -> module.close());
    nameToModuleMap.clear();

    return null;
  }

  @Override
  public void close() {
    AccessController.doPrivileged((PrivilegedAction<Void>) this::doClose);
  }
}
