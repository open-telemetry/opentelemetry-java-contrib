/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import io.opentelemetry.maven.MavenGoal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;

public class MojoGoalExecutionHandlerConfiguration {

  public static Map<MavenGoal, MojoGoalExecutionHandler> loadMojoGoalExecutionHandler(
      ClassLoader classLoader) {

    // built-in handlers
    List<MojoGoalExecutionHandler> builtInHandlers =
        Arrays.asList(
            new GoogleJibBuildHandler(),
            new MavenDeployHandler(),
            new SnykMonitorHandler(),
            new SnykTestHandler(),
            new SpringBootBuildImageHandler());

    List<MojoGoalExecutionHandler> spiHandlers = new ArrayList();
    // Must use the classloader of the class rather the default ThreadContextClassloader to prevent
    // java.util.ServiceConfigurationError:
    //    io.opentelemetry.maven.handler.MojoGoalExecutionHandler:
    //    io.opentelemetry.maven.handler.SpringBootBuildImageHandler not a subtype
    ServiceLoader.load(MojoGoalExecutionHandler.class, classLoader)
        .forEach(handler -> spiHandlers.add(handler));

    Map<MavenGoal, MojoGoalExecutionHandler> mojoGoalExecutionHandlers = new HashMap<>();

    Stream.concat(builtInHandlers.stream(), spiHandlers.stream())
        .forEach(
            handler ->
                handler
                    .getSupportedGoals()
                    .forEach(
                        goal -> {
                          MojoGoalExecutionHandler previousHandler =
                              mojoGoalExecutionHandlers.put(goal, handler);
                          if (previousHandler != null) {
                            throw new IllegalStateException(
                                "More than one handler found for maven goal "
                                    + goal
                                    + ": "
                                    + previousHandler
                                    + ", "
                                    + handler);
                          }
                        }));
    return mojoGoalExecutionHandlers;
  }

  private MojoGoalExecutionHandlerConfiguration() {}
}
