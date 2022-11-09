/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.resourceproviders;

import java.nio.file.Path;
import java.nio.file.Paths;

class GlassfishAppServer implements AppServer {

  private final String SERVICE_CLASS_NAME = "com.sun.enterprise.glassfish.bootstrap.ASMain";
  private final ResourceLocator locator;

  GlassfishAppServer(ResourceLocator locator) {
    this.locator = locator;
  }

  @Override
  public Path getDeploymentDir() {
    String instanceRoot = System.getProperty("com.sun.aas.instanceRoot");
    if (instanceRoot == null) {
      return null;
    }

    // besides autodeploy directory it is possible to deploy applications through admin console and
    // asadmin script, to detect those we would need to parse config/domain.xml
    return Paths.get(instanceRoot, "autodeploy");
  }

  @Override
  public Class<?> getServerClass() {
    return locator.findClass(SERVICE_CLASS_NAME);
  }
}
