/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.resourceproviders;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParseBuddyTest {

  final Path path = Paths.get("dir/");
  final Path webXml = Paths.get("dir/WEB-INF/web.xml");
  final Path applicationXml = Paths.get("dir/META-INF/application.xml");
  final InputStream webXmlStream =
      new ByteArrayInputStream(
          "<web-app><display-name>goats</display-name><servlet><servlet-name>xxx</servlet-name></servlet></web-app>"
              .getBytes(UTF_8));

  final InputStream webXmlStreamServletName =
      new ByteArrayInputStream(
          "<web-app><servlet><servlet-name>sheep</servlet-name></servlet></web-app>"
              .getBytes(UTF_8));

  final InputStream webXmlStreamBoth =
      new ByteArrayInputStream(
          ("<web-app>"
                  + "<servlet>"
                  + "<servlet-name>NOT-ME</servlet-name>"
                  + "<display-name>USE-ME</display-name>"
                  + "<display-name>ME-NEITHER</display-name>"
                  + "<display-name>NOT-THIS-ONE</display-name>"
                  + "</servlet>"
                  + "</web-app>")
              .getBytes(UTF_8));

  final InputStream multipleDisplayNames =
      new ByteArrayInputStream(
          ("<web-app>"
                  + "<display-name>USEME</display-name>"
                  + "<servlet>"
                  + "<servlet-name>NOT-SERVLET-NAME</servlet-name>"
                  + "<display-name>NOT-INNER-DISPLAY-NAME</display-name>"
                  + "</servlet>"
                  + "</web-app>")
              .getBytes(UTF_8));

  final InputStream appXmlStream =
      new ByteArrayInputStream(
          "<application><display-name>piglet</display-name></application>".getBytes(UTF_8));

  @Mock private AppServer appServer;
  @Mock private ParseBuddy.Filesystem filesystem;

  @Test
  void explodedApp_warUsesDisplayName() throws Exception {

    when(filesystem.isRegularFile(webXml)).thenReturn(true);
    when(filesystem.newInputStream(webXml)).thenReturn(webXmlStream);
    when(appServer.isValidResult(path, "goats")).thenReturn(true);

    ParseBuddy parseBuddy = new ParseBuddy(appServer, filesystem);

    String result = parseBuddy.handleExplodedApp(path);
    assertThat(result).isEqualTo("goats");
  }

  @Test
  void webXmlUsesOuterDisplayName() throws Exception {
    when(filesystem.isRegularFile(webXml)).thenReturn(true);
    when(filesystem.newInputStream(webXml)).thenReturn(multipleDisplayNames);
    when(appServer.isValidResult(path, "USEME")).thenReturn(true);

    ParseBuddy parseBuddy = new ParseBuddy(appServer, filesystem);

    String result = parseBuddy.handleExplodedApp(path);
    assertThat(result).isEqualTo("USEME");
  }

  @Test
  void explodedApp_warUsesServletName() throws Exception {
    when(filesystem.isRegularFile(webXml)).thenReturn(true);
    when(filesystem.newInputStream(webXml)).thenReturn(webXmlStreamServletName);
    when(appServer.isValidResult(path, "sheep")).thenReturn(true);

    ParseBuddy parseBuddy = new ParseBuddy(appServer, filesystem);

    String result = parseBuddy.handleExplodedApp(path);
    assertThat(result).isEqualTo("sheep");
  }

  @Test
  void prefersDisplayNameOverServletName() throws Exception {
    when(filesystem.isRegularFile(webXml)).thenReturn(true);
    when(filesystem.newInputStream(webXml)).thenReturn(webXmlStreamBoth);
    when(appServer.isValidResult(path, "USE-ME")).thenReturn(true);

    ParseBuddy parseBuddy = new ParseBuddy(appServer, filesystem);

    String result = parseBuddy.handleExplodedApp(path);
    assertThat(result).isEqualTo("USE-ME");
  }

  @Test
  void explodedApp_malformedWarXml() throws Exception {
    InputStream stream = new ByteArrayInputStream("<asdfasedsafj9j9asj98/////app>".getBytes(UTF_8));

    when(filesystem.isRegularFile(webXml)).thenReturn(true);
    when(filesystem.newInputStream(webXml)).thenReturn(stream);

    ParseBuddy parseBuddy = new ParseBuddy(appServer, filesystem);

    String result = parseBuddy.handleExplodedApp(path);
    assertThat(result).isNull();
  }

  @Test
  void explodedApp_ear() throws Exception {

    when(filesystem.isRegularFile(webXml)).thenReturn(false);
    when(filesystem.isRegularFile(applicationXml)).thenReturn(true);
    when(filesystem.newInputStream(applicationXml)).thenReturn(appXmlStream);
    when(appServer.supportsEar()).thenReturn(true);
    when(appServer.isValidResult(path, "piglet")).thenReturn(true);

    ParseBuddy parseBuddy = new ParseBuddy(appServer, filesystem);

    String result = parseBuddy.handleExplodedApp(path);
    assertThat(result).isEqualTo("piglet");
  }

  @Test
  void packagedWar() throws Exception {
    Path warFile = Paths.get("/path/to/amaze.war");

    ZipFile zipFile = mock(ZipFile.class);
    ZipEntry zipEntry = mock(ZipEntry.class);

    when(zipFile.getEntry("WEB-INF/web.xml")).thenReturn(zipEntry);
    when(appServer.isValidResult(warFile, "goats")).thenReturn(true);
    when(filesystem.openZipFile(warFile)).thenReturn(zipFile);
    when(zipFile.getInputStream(zipEntry)).thenReturn(webXmlStream);

    ParseBuddy parseBuddy = new ParseBuddy(appServer, filesystem);

    String result = parseBuddy.handlePackagedWar(warFile);
    assertThat(result).isEqualTo("goats");
  }

  @Test
  void handlePackagedThrows() throws Exception {
    Path warFile = Paths.get("/path/to/amaze.war");

    when(filesystem.openZipFile(warFile)).thenThrow(new IOException("boom"));

    ParseBuddy parseBuddy = new ParseBuddy(appServer, filesystem);

    String result = parseBuddy.handlePackagedWar(warFile);
    assertThat(result).isNull();
  }

  @Test
  void packagedEar() throws Exception {
    Path earFile = Paths.get("/path/to/amaze.ear");

    ZipFile zipFile = mock(ZipFile.class);
    ZipEntry zipEntry = mock(ZipEntry.class);

    when(zipFile.getEntry("META-INF/application.xml")).thenReturn(zipEntry);
    when(appServer.isValidResult(earFile, "piglet")).thenReturn(true);
    when(filesystem.openZipFile(earFile)).thenReturn(zipFile);
    when(zipFile.getInputStream(zipEntry)).thenReturn(appXmlStream);

    ParseBuddy parseBuddy = new ParseBuddy(appServer, filesystem);

    String result = parseBuddy.handlePackagedEar(earFile);
    assertThat(result).isEqualTo("piglet");
  }
}
