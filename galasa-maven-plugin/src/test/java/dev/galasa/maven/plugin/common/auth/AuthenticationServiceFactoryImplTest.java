/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.maven.plugin.common.auth;

import java.net.URL;

import org.junit.Test;

import dev.galasa.maven.plugin.MockHttpClient;

public class AuthenticationServiceFactoryImplTest {
    
    @Test
    public void testCanCreateAuthenticationService() throws Exception {
        AuthenticationServiceFactory f = new AuthenticationServiceFactoryImpl();
        MockHttpClient client = new MockHttpClient();
        f.newAuthenticationService(new URL("http://fakeurl"), "my:token", client);
    }
}
