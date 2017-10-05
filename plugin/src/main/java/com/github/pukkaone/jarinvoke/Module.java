package com.github.pukkaone.jarinvoke;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenRemoteRepositories;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenRemoteRepository;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenUpdatePolicy;

/**
 * Context for loading JAR file.
 */
public class Module implements Closeable {

  private URLClassLoader classLoader;
  private Map<String, Method> nameToMethodMap = new ConcurrentHashMap<>();

  /**
   * Loads JAR file from Maven repository.
   *
   * @param repositoryUri
   *     repository URI
   * @param mavenCoordinates
   *     group ID, artifact ID and version separated by {@code :}
   */
  public Module(String repositoryUri, String mavenCoordinates) {
    ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      // Because Maven.configureResolver() uses the current thread context class
      // loader to load classes.
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

      File[] jarFiles = Maven.configureResolver()
          .withRemoteRepo(createRemoteRepostory(repositoryUri))
          .resolve(mavenCoordinates)
          .withTransitivity()
          .asFile();
      URL[] urls = Arrays.stream(jarFiles)
          .map(Module::toURL)
          .toArray(URL[]::new);
      classLoader = new URLClassLoader(urls, getClass().getClassLoader());
    } finally {
      Thread.currentThread().setContextClassLoader(originalContextClassLoader);
    }
  }

  private static MavenRemoteRepository createRemoteRepostory(String repositoryUri) {
    String repositoryId = "dynamic" + System.currentTimeMillis();
    return MavenRemoteRepositories.createRemoteRepository(repositoryId, repositoryUri, "default")
        .setUpdatePolicy(MavenUpdatePolicy.UPDATE_POLICY_ALWAYS);
  }

  private static URL toURL(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Cannot convert to URL, file " + file, e);
    }
  }

  private Method doResolveMethod(String className, String methodName) {
    try {
      Class<?> clazz = classLoader.loadClass(className);
      return clazz.getMethod(methodName, Map.class, Map.class);
    } catch (ReflectiveOperationException e) {
      String message = String.format(
          "Cannot get method, class %s, method %s", className, methodName);
      throw new IllegalStateException(message, e);
    }
  }

  private Method resolveMethod(String className, String methodName) {
    String fullQualifiedMethodName = className + '.' + methodName;
    return nameToMethodMap.computeIfAbsent(
        fullQualifiedMethodName, key -> doResolveMethod(className, methodName));
  }

  /**
   * Invokes static method of a Java class. The method must accept two Map parameters.
   *
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
      String className,
      String methodName,
      Map<String, Object> variables,
      Map<String, ScriptDocValues> docLookup) {

    try {
      Method method = resolveMethod(className, methodName);
      return method.invoke(null, variables, docLookup);
    } catch (ReflectiveOperationException e) {
      String message = String.format(
          "Cannot invoke, class %s, method %s", className, methodName);
      throw new IllegalStateException(message, e);
    }
  }

  @Override
  public void close() {
    try {
      classLoader.close();
    } catch (IOException e) {
      // ignore
    }

    classLoader = null;
  }
}
