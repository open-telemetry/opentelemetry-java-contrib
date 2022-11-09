package io.opentelemetry.resourceproviders;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppServerServiceNameDetectorTest {

  @Mock
  private AppServer appServer;
  @Mock
  private AppServerServiceNameDetector.DirectoryTool dirTool;
  @Mock
  private ParseBuddy parseBuddy;

  @Test
  void detectNullServerClass() throws Exception {
    AppServerServiceNameDetector detector = new AppServerServiceNameDetector(appServer);
    assertThat(detector.detect()).isNull();
  }

  @Test
  void nullDeploymentDir() throws Exception {
    doReturn(AppServer.class).when(appServer).getServerClass();
    AppServerServiceNameDetector detector = new AppServerServiceNameDetector(appServer);
    assertThat(detector.detect()).isNull();
  }

  @Test
  void detectMissingDir() throws Exception {
    Path deploymentDir = Paths.get("/fake", "location");

    doReturn(AppServer.class).when(appServer).getServerClass();
    when(appServer.getDeploymentDir()).thenReturn(deploymentDir);
    when(dirTool.isDirectory(deploymentDir)).thenReturn(false);

    AppServerServiceNameDetector detector = new AppServerServiceNameDetector(appServer, null, dirTool);
    assertThat(detector.detect()).isNull();
  }

  @Test
  void detect_explodedApp() throws Exception {
    Path deploymentDir = Paths.get("/fake", "location");
    Path path1 = Paths.get("path1.xml");
    Path path2 = Paths.get("something/");

    doReturn(AppServer.class).when(appServer).getServerClass();
    when(appServer.getDeploymentDir()).thenReturn(deploymentDir);
    when(appServer.isValidAppName(path1)).thenReturn(false);
    when(appServer.isValidAppName(path2)).thenReturn(true);
    when(dirTool.isDirectory(deploymentDir)).thenReturn(true);
    when(dirTool.list(deploymentDir)).thenReturn(Stream.of(path1, path2));
    when(dirTool.isDirectory(path2)).thenReturn(true);
    when(parseBuddy.handleExplodedApp(path2)).thenReturn("RadicalService99");

    AppServerServiceNameDetector detector = new AppServerServiceNameDetector(appServer, parseBuddy, dirTool);
    assertThat(detector.detect()).isEqualTo("RadicalService99");
  }

  @Test
  void detect_packagedWar() throws Exception {
    Path deploymentDir = Paths.get("/fake", "location");
    Path path1 = Paths.get("meh");
    Path path2 = Paths.get("excellent.war");

    doReturn(AppServer.class).when(appServer).getServerClass();
    when(appServer.getDeploymentDir()).thenReturn(deploymentDir);
    when(appServer.isValidAppName(path1)).thenReturn(false);
    when(appServer.isValidAppName(path2)).thenReturn(true);
    when(dirTool.isDirectory(deploymentDir)).thenReturn(true);
    when(dirTool.list(deploymentDir)).thenReturn(Stream.of(path1, path2));
    when(dirTool.isDirectory(path2)).thenReturn(false);
    when(parseBuddy.handlePackagedWar(path2)).thenReturn("WhatIsItGoodFor");

    AppServerServiceNameDetector detector = new AppServerServiceNameDetector(appServer, parseBuddy, dirTool);
    assertThat(detector.detect()).isEqualTo("WhatIsItGoodFor");
  }

  @Test
  void detect_packagedEar() throws Exception {
    Path deploymentDir = Paths.get("/fake", "location");
    Path path1 = Paths.get("meh");
    Path path2 = Paths.get("excellent.ear");

    doReturn(AppServer.class).when(appServer).getServerClass();
    when(appServer.getDeploymentDir()).thenReturn(deploymentDir);
    when(appServer.isValidAppName(path1)).thenReturn(false);
    when(appServer.isValidAppName(path2)).thenReturn(true);
    when(appServer.supportsEar()).thenReturn(true);

    when(dirTool.isDirectory(deploymentDir)).thenReturn(true);
    when(dirTool.list(deploymentDir)).thenReturn(Stream.of(path1, path2));
    when(dirTool.isDirectory(path2)).thenReturn(false);
    when(parseBuddy.handlePackagedEar(path2)).thenReturn("Cochlea");

    AppServerServiceNameDetector detector = new AppServerServiceNameDetector(appServer, parseBuddy, dirTool);
    assertThat(detector.detect()).isEqualTo("Cochlea");
  }

  @Test
  void detect_nothing() throws Exception {
    Path deploymentDir = Paths.get("/fake", "location");
    Path path1 = Paths.get("meh");

    doReturn(AppServer.class).when(appServer).getServerClass();
    when(appServer.getDeploymentDir()).thenReturn(deploymentDir);
    when(appServer.isValidAppName(path1)).thenReturn(true);
    when(appServer.supportsEar()).thenReturn(true);

    when(dirTool.isDirectory(deploymentDir)).thenReturn(true);
    when(dirTool.list(deploymentDir)).thenReturn(Stream.of(path1));

    AppServerServiceNameDetector detector = new AppServerServiceNameDetector(appServer, parseBuddy, dirTool);
    assertThat(detector.detect()).isNull();
  }

}