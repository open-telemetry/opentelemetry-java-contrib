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

import javax.annotation.Nullable;
import java.nio.file.Path;

class WebSphereAppServer implements AppServer {

  private static final String SERVER_CLASS_NAME = "com.ibm.wsspi.bootstrap.WSPreLauncher";
  private final ResourceLocator locator;

  WebSphereAppServer(ResourceLocator locator) {
    this.locator = locator;
  }

  @Override
  public boolean isValidAppName(Path path) {
    // query.ear is bundled with websphere
    String name = path.getFileName().toString();
    return !"query.ear".equals(name);
  }

  @Nullable
  @Override
  public Path getDeploymentDir() {
    // not used
    return null;
  }

  @Nullable
  @Override
  public Class<?> getServerClass() {
    return locator.findClass(SERVER_CLASS_NAME);
  }
}
