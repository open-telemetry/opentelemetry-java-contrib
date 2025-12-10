/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.cel.testapp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Simple HTTP server for testing CEL-based sampler extension.
 *
 * <p>This application provides multiple endpoints to test different sampling behaviors:
 * - /api/data - Should be sampled (API endpoint)
 * - /healthcheck - Should be dropped (health check endpoint)
 * - /metrics - Should be dropped (metrics endpoint)
 * - /hello - Should be sampled (regular endpoint)
 */
public class SimpleServer {

  private static final Logger logger = Logger.getLogger(SimpleServer.class.getName());

  public static void main(String[] args) throws Exception {
    int port = 8080;
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

    // Regular endpoints that should be sampled
    server.createContext("/hello", new ResponseHandler("Hello from test app!"));
    server.createContext("/api/data", new ResponseHandler("API data response"));

    // Health/monitoring endpoints that should be dropped
    server.createContext("/healthcheck", new ResponseHandler("OK"));
    server.createContext("/metrics", new ResponseHandler("metrics=1"));

    server.setExecutor(null);

    logger.info("Starting server on port " + port);
    server.start();
  }

  static class ResponseHandler implements HttpHandler {
    private final String response;

    ResponseHandler(String response) {
      this.response = response;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

      exchange.sendResponseHeaders(200, responseBytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }

      logger.info("Handled request to " + exchange.getRequestURI().getPath());
    }
  }
}