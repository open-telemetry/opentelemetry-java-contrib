/*
 * Copyright The OpenTelemetry Authors
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

package io.opentelemetry.contrib.jmxmetrics;

import javax.annotation.Nullable;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

public class ClientCallbackHandler implements CallbackHandler {
  private final String username;
  @Nullable private final char[] password;
  private final String realm;

  /**
   * Constructor for the {@link ClientCallbackHandler}, a CallbackHandler implementation for
   * authenticating with an MBean server.
   *
   * @param username - authenticating username
   * @param password - authenticating password (plaintext)
   * @param realm - authenticating realm
   */
  public ClientCallbackHandler(final String username, final String password, final String realm) {
    this.username = username;
    this.password = password != null ? password.toCharArray() : null;
    this.realm = realm;
  }

  @Override
  public void handle(final Callback[] callbacks) throws UnsupportedCallbackException {
    for (Callback callback : callbacks) {
      if (callback instanceof NameCallback) {
        ((NameCallback) callback).setName(this.username);
      } else if (callback instanceof PasswordCallback) {
        ((PasswordCallback) callback).setPassword(this.password);
      } else if (callback instanceof RealmCallback) {
        ((RealmCallback) callback).setText(this.realm);
      } else {
        throw new UnsupportedCallbackException(callback);
      }
    }
  }
}
