/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.maven.plugin.common;

import java.net.URL;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import dev.galasa.maven.plugin.ErrorRaiserMavenImpl;
import dev.galasa.maven.plugin.MockLog;

public class URLCalculatorTest {
    @Test
    public void TestCalculateTestCatalogUrlAddsSuffixesOk() throws Exception {
        String streamName = "mystream";
        String apiServerUrl = "https://myserver/api";
        MockLog mockLog = new MockLog();
        ErrorRaiser<MojoExecutionException> raiser = new ErrorRaiserMavenImpl(mockLog);
        URLCalculator<MojoExecutionException> calculator = new URLCalculator<MojoExecutionException> (raiser);
        URL calculatedUrl = calculator.calculateTestCatalogUrl(apiServerUrl, streamName);
        assertThat(calculatedUrl.toString()).isEqualTo("https://myserver/api/testcatalog/mystream");
    }


    @Test
    public void TestCalculateApiServerUrlUsesBootstrapPropertyIfSet() throws Exception {
        Properties props = new Properties();
        props.setProperty("framework.testcatalog.url", "https://my.explicitly.set/testcatalog");
        URL bootstrapUrl = new URL("https://my/bootstrap");
        MockLog mockLog = new MockLog();
        ErrorRaiser<MojoExecutionException> raiser = new ErrorRaiserMavenImpl(mockLog);
        URLCalculator<MojoExecutionException> calculator = new URLCalculator<MojoExecutionException> (raiser);
        String calculatedUrl = calculator.calculateApiServerUrl(props, bootstrapUrl);
        assertThat(calculatedUrl).isEqualTo("https://my.explicitly.set");
    }

    @Test
    public void TestCalculateTestCatalogUrlUsesBootstrapUrlByDefault() throws Exception {
        Properties props = new Properties();
        MockLog mockLog = new MockLog();
        ErrorRaiser<MojoExecutionException> raiser = new ErrorRaiserMavenImpl(mockLog);
        URLCalculator<MojoExecutionException> calculator = new URLCalculator<MojoExecutionException> (raiser);
        // props.setProperty("framework.testcatalog.url", "https://my.explicitly.set.bootstrap.url");
        String calculatedUrl = calculator.calculateApiServerUrl(props, new URL("https://my.bootstrap.url/bootstrap"));
        assertThat(calculatedUrl.toString()).isEqualTo("https://my.bootstrap.url");
    }

    @Test
    public void TestCalculateTestCatalogUrlSpotsBootstrapUrlWhichDoesntEndInBootstrap() throws Exception {
        Properties props = new Properties();
        MockLog mockLog = new MockLog();
        ErrorRaiser<MojoExecutionException> raiser = new ErrorRaiserMavenImpl(mockLog);
        URLCalculator<MojoExecutionException> calculator = new URLCalculator<MojoExecutionException> (raiser);
        // props.setProperty("framework.testcatalog.url", "https://my.explicitly.set/bootstrap-not-at-end");
        Exception ex = catchException( ()-> calculator.calculateApiServerUrl(props, new URL("https://my.bootstrap.url/bootstrap-not-at-end")) );
        assertThat(ex).isInstanceOf(MojoExecutionException.class);
        
        assertThat(ex).hasMessageContaining("Unable to calculate the url to the API server, the bootstrap url does not end with /bootstrap, need a framework.testcatalog.url property in the bootstrap");
    }

}
