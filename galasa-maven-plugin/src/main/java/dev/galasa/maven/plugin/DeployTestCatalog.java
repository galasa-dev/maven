/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.maven.plugin;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Merge all the test catalogs on the dependency list
 * 
 * @author Michael Baylis
 *
 */
@Mojo(name = "deploytestcat", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class DeployTestCatalog extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${galasa.skip.bundletestcatatlog}", readonly = true, required = false)
    private boolean      skip;

    @Parameter(defaultValue = "${galasa.skip.deploytestcatatlog}", readonly = true, required = false)
    private boolean      skipDeploy;

    @Parameter(defaultValue = "${galasa.test.stream}", readonly = true, required = false)
    private String       testStream;

    @Parameter(defaultValue = "${galasa.bootstrap}", readonly = true, required = false)
    private URL          bootstrapUrl;

    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip || skipDeploy) {
            getLog().info("Skipping Deploy Test Catalog");
            return;
        }

        if (testStream == null) {
            getLog().info("Skipping Deploy Test Catalog - test stream name is missing");
            return;
        }

        if (bootstrapUrl == null) {
            getLog().info("Skipping Deploy Test Catalog - Bootstrap URL is missing");
            return;
        }

        if (!"galasa-obr".equals(project.getPackaging())) {
            getLog().info("Skipping Bundle Test Catalog deploy, not a galasa-obr project");
            return;
        }

        try {
            Artifact artifact = null;
            for (Artifact a : project.getAttachedArtifacts()) {
                if ("testcatalog".equals(a.getClassifier()) && "json".equals(a.getType())) {
                    artifact = a;
                    break;
                }
            }
            if (artifact == null) {
                getLog().info("Skipping Bundle Test Catalog deploy, no test catalog artifact present");
                return;
            }

            // *** Get the bootstrap
            Properties bootstrapProperties = new Properties();
            try {
                URLConnection connection = bootstrapUrl.openConnection();
                bootstrapProperties.load(connection.getInputStream());
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to load the bootstrap properties", e);
            }

            // *** Calculate the testcatalog url
            String sTestcatalogUrl = bootstrapProperties.getProperty("framework.testcatalog.url");
            if (sTestcatalogUrl == null || sTestcatalogUrl.trim().isEmpty()) {
                String sBootstrapUrl = bootstrapUrl.toString();
                if (!sBootstrapUrl.endsWith("/bootstrap")) {
                    throw new MojoExecutionException(
                            "Unable to calculate the test catalog url, the bootstrap url does not end with /bootstrap, need a framework.testcatalog.url property in the bootstrap");
                }

                sTestcatalogUrl = sBootstrapUrl.substring(0, sBootstrapUrl.length() - 10) + "/testcatalog";
            }

            // *** Check to see if we need authentication
            String sAuthenticationUrl = bootstrapProperties.getProperty("framework.authentication.url");
            if (sAuthenticationUrl != null) {
                throw new MojoExecutionException("Unable to support Galasa authentication at the moment");
            }

            // *** Get the test catalog url
            URL testCatalogUrl = new URL(sTestcatalogUrl + "/" + testStream);

            HttpURLConnection conn = (HttpURLConnection) testCatalogUrl.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("PUT");
            conn.addRequestProperty("Content-Type", "application/json");
            conn.addRequestProperty("Accept", "application/json");

            FileUtils.copyFile(artifact.getFile(), conn.getOutputStream());
            int rc = conn.getResponseCode();
            String message = conn.getResponseMessage();

            InputStream is = null;
            if (rc < HttpURLConnection.HTTP_BAD_REQUEST) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            String response = "";
            if (is != null) {
                response = IOUtils.toString(is, "utf-8");
            }

            conn.disconnect();

            if (rc >= HttpURLConnection.HTTP_BAD_REQUEST) {
                getLog().error("Deploy to Test Catalog Store failed:-");
                getLog().error(Integer.toString(rc) + " - " + message);
                if (!response.isEmpty()) {
                    getLog().error(response);
                }

                throw new MojoExecutionException("Deploy to Test Catalog Store failed");
            }

            getLog().info("Test Catalog successfully deployed to " + testCatalogUrl.toString());

        } catch (Throwable t) {
            throw new MojoExecutionException("Problem merging the test catalog", t);
        }

    }

}
