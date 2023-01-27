/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.resourceproviders;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Helper class for parsing webserver xml files from various locations. */
class ParseBuddy {

  private static final Logger logger = Logger.getLogger(ParseBuddy.class.getName());

  private final AppServer appServer;
  private final Filesystem filesystem;

  ParseBuddy(AppServer appServer) {
    this(appServer, new Filesystem());
  }

  // Exists for testing
  ParseBuddy(AppServer appServer, Filesystem filesystem) {
    this.appServer = appServer;
    this.filesystem = filesystem;
  }

  @Nullable
  String handleExplodedApp(Path path) {
    String warResult = handleExplodedWar(path);
    if (warResult != null) {
      return warResult;
    }
    if (appServer.supportsEar()) {
      return handleExplodedEar(path);
    }
    return null;
  }

  @Nullable
  String handlePackagedWar(Path path) {
    return handlePackaged(path, "WEB-INF/web.xml", newWebXmlHandler());
  }

  @Nullable
  String handlePackagedEar(Path path) {
    return handlePackaged(path, "META-INF/application.xml", newAppXmlHandler());
  }

  @Nullable
  private String handlePackaged(Path path, String descriptorPath, DescriptorHandler handler) {
    try (ZipFile zip = filesystem.openZipFile(path)) {
      ZipEntry zipEntry = zip.getEntry(descriptorPath);
      if (zipEntry != null) {
        return handle(() -> zip.getInputStream(zipEntry), path, handler);
      }
    } catch (IOException exception) {
      if (logger.isLoggable(WARNING)) {
        logger.log(
            WARNING, "Failed to read '" + descriptorPath + "' from zip '" + path + "'.", exception);
      }
    }

    return null;
  }

  @Nullable
  String handleExplodedWar(Path path) {
    return handleExploded(path, path.resolve("WEB-INF/web.xml"), newWebXmlHandler());
  }

  @Nullable
  String handleExplodedEar(Path path) {
    return handleExploded(path, path.resolve("META-INF/application.xml"), newAppXmlHandler());
  }

  @Nullable
  private String handleExploded(Path path, Path descriptor, DescriptorHandler handler) {
    if (filesystem.isRegularFile(descriptor)) {
      return handle(() -> filesystem.newInputStream(descriptor), path, handler);
    }

    return null;
  }

  @Nullable
  private String handle(InputStreamSupplier supplier, Path path, DescriptorHandler handler) {
    try {
      try (InputStream inputStream = supplier.supply()) {
        String candidate = parseDescriptor(inputStream, handler);
        if (appServer.isValidResult(path, candidate)) {
          return candidate;
        }
      }
    } catch (Exception exception) {
      logger.log(WARNING, "Failed to parse descriptor", exception);
    }

    return null;
  }

  @Nullable
  private static String parseDescriptor(InputStream inputStream, DescriptorHandler handler)
      throws ParserConfigurationException, SAXException, IOException {
    if (SaxParserFactoryHolder.saxParserFactory == null) {
      return null;
    }
    SAXParser saxParser = SaxParserFactoryHolder.saxParserFactory.newSAXParser();
    saxParser.parse(inputStream, handler);
    return handler.getName();
  }

  private interface InputStreamSupplier {
    InputStream supply() throws IOException;
  }

  private static DescriptorHandler newWebXmlHandler() {
    return new DescriptorHandler("web-app");
  }

  private static DescriptorHandler newAppXmlHandler() {
    return new DescriptorHandler("application");
  }

  private static final class DescriptorHandler extends DefaultHandler {
    private final String rootElementName;
    private final Deque<String> currentElement = new ArrayDeque<>();
    private String key;
    private final Map<String, String> names = new HashMap<>();

    DescriptorHandler(String rootElementName) {
      this.rootElementName = rootElementName;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
      List<String> nameElements = Arrays.asList("display-name", "servlet-name");
      if ((names.size() < 2) // If we haven't already found both names
          && rootElementName.equals(
              currentElement.peekLast()) // And we're scoped to our root element
          && nameElements.contains(qName)) { // And this element is one of the two we like
        if (isEn(attributes)) {
          key = qName;
        }
      }
      currentElement.push(qName);
    }

    private boolean isEn(Attributes attributes) {
      String lang = attributes.getValue("xml:lang");
      if (lang == null || "".equals(lang)) {
        return true; // en is the default language
      }
      return "en".equals(lang);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      currentElement.pop();
      key = null;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
      if (key != null) {
        names.putIfAbsent(key, new String(ch, start, length));
      }
    }

    public String getName() {
      String displayName = names.get("display-name");
      return displayName == null ? names.get("servlet-name") : displayName;
    }
  }

  private static class SaxParserFactoryHolder {
    @Nullable private static final SAXParserFactory saxParserFactory = getSaxParserFactory();

    @Nullable
    private static SAXParserFactory getSaxParserFactory() {
      try {
        return SAXParserFactory.newInstance();
      } catch (Throwable throwable) {
        logger.log(FINE, "XML parser not available.", throwable);
      }
      return null;
    }
  }

  // Exists for testing
  static class Filesystem {
    boolean isRegularFile(Path path) {
      return Files.isRegularFile(path);
    }

    InputStream newInputStream(Path path) throws IOException {
      return Files.newInputStream(path);
    }

    ZipFile openZipFile(Path path) throws IOException {
      return new ZipFile(path.toFile());
    }
  }
}
