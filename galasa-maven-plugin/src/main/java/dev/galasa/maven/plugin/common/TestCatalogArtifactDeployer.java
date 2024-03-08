package dev.galasa.maven.plugin.file;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.MojoExecutionException;

import dev.galasa.maven.plugin.BootstrapLoader;
import dev.galasa.maven.plugin.GalasaRestApiMetadata;
import dev.galasa.maven.plugin.auth.AuthenticationService;
import dev.galasa.maven.plugin.auth.AuthenticationServiceFactory;
import dev.galasa.maven.plugin.error.ErrorRaiser;
import dev.galasa.maven.plugin.log.WrappedLog;
import dev.galasa.maven.plugin.url.URLCalculator;

public class TestCatalogArtifactDeployer<Ex extends Exception> {

    private WrappedLog log ;
    private ErrorRaiser<Ex> errorRaiser ;
    private BootstrapLoader<Ex> bootstrapLoader ;
    private URLCalculator<Ex> urlCalculator ;
    private GalasaRestApiMetadata restApiMetadata;
    private AuthenticationServiceFactory authFactory;

    public TestCatalogArtifactDeployer(
        WrappedLog log, 
        ErrorRaiser<Ex> errorRaiser, 
        BootstrapLoader<Ex> bootstrapLoader, 
        URLCalculator<Ex> urlCalculator, 
        GalasaRestApiMetadata restApiMetadata,
        AuthenticationServiceFactory authFactory
    ) {
        this.log = log ;
        this.errorRaiser = errorRaiser ;
        this.bootstrapLoader = bootstrapLoader;
        this.urlCalculator = urlCalculator;
        this.restApiMetadata = restApiMetadata;
        this.authFactory = authFactory;
    }

    public void deployToServer(URL bootstrapUrl, String testStream, String galasaAccessToken, TestCatalogArtifact<Ex> testCatalogArtifact ) throws Ex {

        Properties bootstrapProperties = bootstrapLoader.getBootstrapProperties(bootstrapUrl);

        String apiServerUrl = urlCalculator.calculateApiServerUrl(bootstrapProperties, bootstrapUrl);

        URL testcatalogUrl = urlCalculator.calculateTestCatalogUrl(apiServerUrl, testStream);

        String jwt = null ;
        // For now, if no galasa token is supplied, that's ok. It's optional.   
        // If no galasa access token supplied by the user, the jwt will stay as null.
        if ( (galasaAccessToken!=null) && (!galasaAccessToken.isEmpty()) ) {
            jwt = getAuthenticatedJwt(this.authFactory, galasaAccessToken, apiServerUrl) ;
        }

        publishTestCatalogToGalasaServer(restApiMetadata,testcatalogUrl,jwt, testCatalogArtifact);
    }

    private void publishTestCatalogToGalasaServer(GalasaRestApiMetadata restApiMetadata, URL testCatalogUrl, String jwt, TestCatalogArtifact<Ex> testCatalogArtifact) throws Ex {
 
        HttpURLConnection conn = null ;
        try {
            conn = (HttpURLConnection) testCatalogUrl.openConnection();
        } catch (IOException ioEx) {
            this.errorRaiser.raiseError(ioEx,"Problem publishing the test catalog. Could not open URL connection to the Galasa server.");
        }

        if (conn==null) {
            this.errorRaiser.raiseError("Deploy to Test Catalog Store failed. Could not open a URL connection to the Galasa server.");
        } else {
            try {
                postTestCatalogToGalasaServer(restApiMetadata, conn, testCatalogUrl, jwt, testCatalogArtifact);
            } finally {
                conn.disconnect();
            }
            this.log.info("Test Catalog successfully deployed to " + testCatalogUrl.toString());
        }
    }

    private void postTestCatalogToGalasaServer(
        GalasaRestApiMetadata restApiMetadata, 
        HttpURLConnection conn, 
        URL testCatalogUrl,     
        String jwt, 
        TestCatalogArtifact<Ex> testCatalogArtifact
    ) throws Ex {

        int rc = 0 ;
        String response = "";
        String message = "";
        
        try {
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("PUT");
            conn.addRequestProperty("Content-Type", "application/json");
            conn.addRequestProperty("Accept", "application/json");

            // Only add the jwt header if we have a jwt value.
            if (jwt == null) {
                this.log.info("Not sending a JWT bearer token to the server, as the galasa.token property was not supplied.");
            } else {
                conn.addRequestProperty("Authorization", "Bearer "+jwt);
            }

            conn.addRequestProperty("ClientApiVersion",restApiMetadata.getGalasaRestApiVersion()); // The version of the API we coded this against.

            testCatalogArtifact.transferTo(conn.getOutputStream());
            
            rc = conn.getResponseCode();
            message = conn.getResponseMessage();

            InputStream is = null;
            if (rc != HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            if (is != null) {
                response = IOUtils.toString(is, "utf-8");
            }
        } catch(IOException ioEx) {
            this.errorRaiser.raiseError(ioEx, "Problem publishing the test catalog. Problem dealing with response from Galasa server.");
        } 

        if (rc != HttpURLConnection.HTTP_OK) {
            this.log.error("Deploy to Test Catalog Store failed:-");
            this.log.error(Integer.toString(rc) + " - " + message);
            if (!response.isEmpty()) {
                this.log.error(response);
                errorRaiser.raiseError("Failed to deploy the test catalog. The server did not reply with OK (200)");
            }
        }
    }

    /**
     * Swap the galasa token for a JWT which we can use to talk to the API server on the ecosystem.
     * @return A JWT string (Java Web Token).
     * @throws MojoExecutionException
     */
    private String getAuthenticatedJwt(AuthenticationServiceFactory authFactory, String galasaAccessToken, String apiServerUrlString) throws Ex {
        String jwt = null ;
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            URL apiServerUrl = new URL(apiServerUrlString);
            AuthenticationService authTokenService = authFactory.newAuthenticationService(apiServerUrl,galasaAccessToken,httpClient);
            this.log.info("Turning the galasa access token into a JWT");
            jwt = authTokenService.getJWT();
            this.log.info("Java Web Token (JWT) obtained from the galasa ecosystem OK.");
        } catch( Exception ex) {
            this.errorRaiser.raiseError(ex,"Failure when exchanging the galasa access token with a JWT");
        } 
        return jwt;
    }
}
