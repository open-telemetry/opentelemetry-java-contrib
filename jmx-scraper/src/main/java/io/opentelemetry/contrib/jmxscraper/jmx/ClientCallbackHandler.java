/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.jmx;

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
  public ClientCallbackHandler(String username, String password, String realm) {
    this.username = username;
    this.password = password != null ? password.toCharArray() : null;
    this.realm = realm;
  }

  @Override
  public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
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
