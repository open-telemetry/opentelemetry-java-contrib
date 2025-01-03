/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.auth.springapp;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

  private final Random random = new Random();

  @GetMapping("/ping")
  public String ping() {
    int busyTime = random.nextInt(200);
    busyloop(busyTime);
    return "pong";
  }

  @WithSpan
  private static long busyloop(int busyMillis) {
    Instant start = Instant.now();
    Instant end;
    long counter = 0;
    do {
      counter++;
      end = Instant.now();
    } while (Duration.between(start, end).toMillis() < busyMillis);
    return counter;
  }
}
