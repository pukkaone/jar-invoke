package com.github.pukkaone.jarinvoke.plugin;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import org.elasticsearch.Version;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.IndexSettings;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

/**
 * {@link JarInvokePlugin} integration test.
 */
@RunWith(JUnit4.class)
public class JarInvokePluginIT {

  private static final String POM_PATH =
      "/com/github/pukkaone/integration-test/0-SNAPSHOT/integration-test-0-SNAPSHOT.pom";
  private static final String JAR_PATH =
      "/com/github/pukkaone/integration-test/0-SNAPSHOT/integration-test-0-SNAPSHOT.jar";
  private static final String CLUSTER_NAME = "elasticsearch-test";
  private static final String INDEX = "example";
  private static final String TYPE = "document";

  private static EmbeddedElastic embeddedElastic;
  private static TransportClient client;
  private static String requireStatement;

  @ClassRule
  public static WireMockClassRule wireMock = new WireMockClassRule(wireMockConfig()
      .dynamicPort()
      .withRootDirectory("build/resources/test"));

  private static int findAvailableTcpPort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    }
  }

  private static TransportClient createClient(int transportTcpPort) throws IOException {
    Settings settings = Settings.builder()
        .put(PopularProperties.CLUSTER_NAME, CLUSTER_NAME)
        .build();
    TransportClient client = new PreBuiltTransportClient(settings)
        .addTransportAddress(
            new TransportAddress(InetAddress.getLoopbackAddress(), transportTcpPort));
    return client;
  }

  private static SearchResponse executeScript(String scriptSource, Map<String, Object> parameters) {
    Script script = new Script(
        ScriptType.INLINE,
        JarInvokeScriptEngine.LANGUAGE,
        scriptSource,
        parameters);
    SearchResponse response = client.prepareSearch(INDEX)
        .addScriptField("dummy", script)
        .setSize(1)
        .get();
    assertThat(response.getFailedShards()).isEqualTo(0);
    return response;
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    int transportTcpPort = findAvailableTcpPort();

    Path pluginFile = Paths.get(".")
        .toAbsolutePath()
        .resolve("../plugin/build/distributions/plugin.zip")
        .normalize();

    embeddedElastic = EmbeddedElastic.builder()
        .withElasticVersion(Version.CURRENT.toString())
        .withSetting(PopularProperties.CLUSTER_NAME, CLUSTER_NAME)
        .withSetting(PopularProperties.TRANSPORT_TCP_PORT, transportTcpPort)
        .withPlugin("file:" + pluginFile)
        .withIndex(INDEX, IndexSettings.builder()
            .withSettings(JarInvokePluginIT.class.getResourceAsStream("settings.json"))
            .withType(TYPE, JarInvokePluginIT.class.getResourceAsStream("mappings.json"))
            .build())
        .build()
        .start();
    client = createClient(transportTcpPort);

    embeddedElastic.index(INDEX, TYPE, "{\"id\": 1, \"title\": \"Volume\"}");

    stubFor(head(urlPathEqualTo(POM_PATH))
            .willReturn(ok()));
    stubFor(get(urlPathEqualTo(POM_PATH))
            .willReturn(ok().withBodyFile("integration-test.pom")));
    stubFor(head(urlPathEqualTo(JAR_PATH))
            .willReturn(ok()));
    stubFor(get(urlPathEqualTo(JAR_PATH))
            .willReturn(ok().withBodyFile("integration-test.jar")));

    requireStatement = "hello = require('http://localhost:" + wireMock.port() + "/', 'com.github.pukkaone:integration-test:0-SNAPSHOT')\n";
  }

  @AfterClass
  public static void afterClass() {
    embeddedElastic.stop();
  }

  @Test
  public void should_read_script_parameters() {
    SearchResponse response = executeScript(
        requireStatement +
        "hello.invoke('com.github.pukkaone.jarinvoke.example.Example', 'echoVariables')",
        ImmutableMap.of("factor", 1));

    DocumentField dummy = response.getHits().getAt(0).field("dummy");
    assertThat(dummy.<String>getValue()).isEqualTo("{factor=1}");
  }

  @Test
  public void should_read_long_doc_value() {
    SearchResponse response = executeScript(
        requireStatement +
        "hello.invoke('com.github.pukkaone.jarinvoke.example.Example', 'getDocValue')",
        ImmutableMap.of("field", "id"));

    DocumentField dummy = response.getHits().getAt(0).field("dummy");
    assertThat(dummy.<Long>getValue()).isEqualTo(1L);
  }

  @Test
  public void should_read_string_doc_value() {
    SearchResponse response = executeScript(
        requireStatement +
        "hello.invoke('com.github.pukkaone.jarinvoke.example.Example', 'getDocValue')",
        ImmutableMap.of("field", "title"));

    DocumentField dummy = response.getHits().getAt(0).field("dummy");
    assertThat(dummy.<String>getValue()).isEqualTo("Volume");
  }

  @Test
  public void should_load_module() {
    SearchResponse response = executeScript(
        "hello = load('http://localhost:" + wireMock.port() + "/', 'com.github.pukkaone:integration-test:0-SNAPSHOT')",
        Collections.emptyMap());

    DocumentField dummy = response.getHits().getAt(0).field("dummy");
    assertThat(dummy.<Boolean>getValue()).isTrue();
  }
}
