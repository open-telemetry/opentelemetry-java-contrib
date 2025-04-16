/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.model;

import java.util.Map;

public class Message {

  private Map<String, String> headers;

  private String id;

  private String body;

  public static Message create(Map<String, String> headers, String id, String body) {
    return new Message(headers, id, body);
  }

  private Message(Map<String, String> headers, String id, String body) {
    this.headers = headers;
    this.id = id;
    this.body = body;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }
}
