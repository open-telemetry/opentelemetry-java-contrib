/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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

  String handlePackagedWar(Path path) {
    return handlePackaged(path, "WEB-INF/web.xml", newWebXmlHandler());
  }

  String handlePackagedEar(Path path) {
    return handlePackaged(path, "META-INF/application.xml", newAppXmlHandler());
  }

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

  String handleExplodedWar(Path path) {
    return handleExploded(path, path.resolve("WEB-INF/web.xml"), newWebXmlHandler());
  }

  String handleExplodedEar(Path path) {
    return handleExploded(path, path.resolve("META-INF/application.xml"), newAppXmlHandler());
  }

  private String handleExploded(Path path, Path descriptor, DescriptorHandler handler) {
    if (filesystem.isRegularFile(descriptor)) {
      return handle(() -> filesystem.newInputStream(descriptor), path, handler);
    }

    return null;
  }

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

  private static String parseDescriptor(InputStream inputStream, DescriptorHandler handler)
      throws ParserConfigurationException, SAXException, IOException {
    if (SaxParserFactoryHolder.saxParserFactory == null) {
      return null;
    }
    SAXParser saxParser = SaxParserFactoryHolder.saxParserFactory.newSAXParser();
    saxParser.parse(inputStream, handler);
    return handler.displayName;
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
    private boolean setDisplayName;
    String displayName;

    DescriptorHandler(String rootElementName) {
      this.rootElementName = rootElementName;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
      if (displayName == null
          && rootElementName.equals(currentElement.peek())
          && "display-name".equals(qName)) {
        String lang = attributes.getValue("xml:lang");
        if (lang == null || "".equals(lang)) {
          lang = "en"; // en is the default language
        }
        if ("en".equals(lang)) {
          setDisplayName = true;
        }
      }
      currentElement.push(qName);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      currentElement.pop();
      setDisplayName = false;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
      if (setDisplayName) {
        displayName = new String(ch, start, length);
      }
    }
  }

  private static class SaxParserFactoryHolder {
    private static final SAXParserFactory saxParserFactory = getSaxParserFactory();

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
