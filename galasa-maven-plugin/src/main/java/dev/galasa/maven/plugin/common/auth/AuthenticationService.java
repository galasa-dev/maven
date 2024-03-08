/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.maven.plugin.auth;

public interface AuthenticationService {
    public String getJWT() throws AuthenticationException ;
}
