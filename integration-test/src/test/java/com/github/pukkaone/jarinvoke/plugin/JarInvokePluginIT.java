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
import java.util.Map;
import org.elasticsearch.Version;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.SearchHitField;
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
    Settings settings = Settings.settingsBuilder()
        .put(PopularProperties.CLUSTER_NAME, CLUSTER_NAME)
        .build();
    TransportClient client = TransportClient.builder()
        .settings(settings)
        .build();
    client.addTransportAddress(
        new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), transportTcpPort));
    return client;
  }

  private static SearchResponse executeScript(String scriptSource, Map<String, Object> parameters) {
    Script script = new Script(
        scriptSource,
        ScriptService.ScriptType.INLINE,
        JarInvokePlugin.NAME,
        parameters);
    SearchResponse response = client.prepareSearch(INDEX)
        .addScriptField("dummy", script)
        .setSize(1)
        .get();
    assertThat(response.getFailedShards()).isEqualTo(0);
    return response;
  }

  private static void loadModule() {
    stubFor(head(urlPathEqualTo(POM_PATH))
            .willReturn(ok()));
    stubFor(get(urlPathEqualTo(POM_PATH))
            .willReturn(ok().withBodyFile("integration-test.pom")));
    stubFor(head(urlPathEqualTo(JAR_PATH))
            .willReturn(ok()));
    stubFor(get(urlPathEqualTo(JAR_PATH))
            .willReturn(ok().withBodyFile("integration-test.jar")));

    executeScript(
        "hello = load('http://localhost:" + wireMock.port() + "/', 'com.github.pukkaone:integration-test:0-SNAPSHOT')",
        null);
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    int transportTcpPort = findAvailableTcpPort();

    embeddedElastic = EmbeddedElastic.builder()
        .withElasticVersion(Version.CURRENT.number())
        .withSetting(PopularProperties.CLUSTER_NAME, CLUSTER_NAME)
        .withSetting(PopularProperties.TRANSPORT_TCP_PORT, transportTcpPort)
        .withSetting("script.indexed", true)
        .withSetting("script.inline", true)
        .withSetting("security.manager.enabled", false)
        .withPlugin("file:../plugin/build/distributions/plugin.zip")
        .withCleanInstallationDirectoryOnStop(true)
        .withIndex(INDEX, IndexSettings.builder()
            .withSettings(JarInvokePluginIT.class.getResourceAsStream("settings-mappings.json"))
            .build())
        .build()
        .start();
    client = createClient(transportTcpPort);

    embeddedElastic.index(INDEX, TYPE, "{\"id\": 1, \"title\": \"Volume\"}");

    loadModule();
  }

  @AfterClass
  public static void afterClass() {
    embeddedElastic.stop();
  }

  @Test
  public void should_read_script_parameters() {
    SearchResponse response = executeScript(
        "hello.invoke('com.github.pukkaone.jarinvoke.example.Example', 'echoVariables')",
        ImmutableMap.of("factor", 1));

    SearchHitField dummy = response.getHits().getAt(0).field("dummy");
    assertThat(dummy.<String>getValue()).isEqualTo("{factor=1}");
  }

  @Test
  public void should_read_long_doc_value() {
    SearchResponse response = executeScript(
        "hello.invoke('com.github.pukkaone.jarinvoke.example.Example', 'getDocValue')",
        ImmutableMap.of("field", "id"));

    SearchHitField dummy = response.getHits().getAt(0).field("dummy");
    assertThat(dummy.<Long>getValue()).isEqualTo(1L);
  }

  @Test
  public void should_read_string_doc_value() {
    SearchResponse response = executeScript(
        "hello.invoke('com.github.pukkaone.jarinvoke.example.Example', 'getDocValue')",
        ImmutableMap.of("field", "title"));

    SearchHitField dummy = response.getHits().getAt(0).field("dummy");
    assertThat(dummy.<String>getValue()).isEqualTo("volume");
  }
}
