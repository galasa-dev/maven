/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.maven.plugin.auth;

import org.apache.http.client.HttpClient;

import java.net.*;

public interface AuthenticationServiceFactory {
    AuthenticationService newAuthenticationService(URL apiServerUrl, String galasaAccessToken, HttpClient httpClient) throws AuthenticationException ;
}
