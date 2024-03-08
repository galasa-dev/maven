/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.maven.plugin.auth;

import java.net.URL;

import org.apache.http.client.HttpClient;

public class AuthenticationServiceFactoryImpl implements AuthenticationServiceFactory {
    @Override
    public AuthenticationService newAuthenticationService(URL apiServerUrl, String galasaAccessToken, HttpClient httpClient) throws AuthenticationException {
        return new AuthenticationServiceImpl(apiServerUrl, galasaAccessToken, httpClient);
    }
}
