/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.maven.plugin;

import java.net.URL;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import dev.galasa.maven.plugin.common.BootstrapLoader;
import dev.galasa.maven.plugin.common.BootstrapLoaderImpl;
import dev.galasa.maven.plugin.common.ErrorRaiser;
import dev.galasa.maven.plugin.common.GalasaRestApiMetadata;
import dev.galasa.maven.plugin.common.TestCatalogArtifact;
import dev.galasa.maven.plugin.common.TestCatalogArtifactDeployer;
import dev.galasa.maven.plugin.common.URLCalculator;
import dev.galasa.maven.plugin.common.WrappedLog;
import dev.galasa.maven.plugin.common.auth.AuthenticationServiceFactory;
import dev.galasa.maven.plugin.common.auth.AuthenticationServiceFactoryImpl;

/**
 * Merge all the test catalogs on the dependency list
 */
@Mojo(name = "deploytestcat", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class DeployTestCatalog extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true)
    public MavenProject project;

    @Parameter(defaultValue = "${galasa.test.stream}", readonly = true, required = false)
    public String       testStream;

    // To deploy the test catalog we need to authenticate using this token.
    @Parameter(defaultValue = "${galasa.token}", readonly = true , required = false)
    public String       galasaAccessToken;

    @Parameter(defaultValue = "${galasa.bootstrap}", readonly = true, required = false)
    public URL          bootstrapUrl;
    
    // This spelling of the property is old/wrong/deprecated.
    @Parameter(defaultValue = "${galasa.skip.bundletestcatatlog}", readonly = true, required = false)
    public boolean      skipBundleTestCatalogOldSpelling;
    
    @Parameter(defaultValue = "${galasa.skip.bundletestcatalog}", readonly = true, required = false)
    public boolean      skipBundleTestCatalog;
    
    // This spelling of the property is old/wrong/deprecated.
    @Parameter(defaultValue = "${galasa.skip.deploytestcatatlog}" , readonly = true, required = false)
    public boolean      skipDeployTestCatalogOldSpelling;
    
    @Parameter(defaultValue = "${galasa.skip.deploytestcatalog}", readonly = true, required = false)
    public boolean      skipDeployTestCatalog;

    public void execute() throws MojoExecutionException, MojoFailureException {

        boolean skip = (skipBundleTestCatalog || skipBundleTestCatalogOldSpelling);
        boolean skipDeploy = (skipDeployTestCatalog || skipDeployTestCatalogOldSpelling);

        getLog().info("DeployTestCatalog - execute()");
        if (skip || skipDeploy) {
            getLog().info("Skipping Deploy Test Catalog - because the property galasa.skip.deploytestcatalog or galasa.skip.bundletestcatalog is set");
            return;
        }

        if (testStream == null) {
            getLog().warn("Skipping Deploy Test Catalog - test stream name is missing");
            return;
        }

        if (bootstrapUrl == null) {
            getLog().warn("Skipping Deploy Test Catalog - Bootstrap URL is missing");
            return;
        }

        if (!"galasa-obr".equals(project.getPackaging())) {
            getLog().info("Skipping Bundle Test Catalog deploy, not a galasa-obr project");
            return;
        }

        Artifact testCatalogArtifact = getTestCatalogArtifact();

        if (testCatalogArtifact == null) {
            getLog().warn("Skipping Bundle Test Catalog deploy, no test catalog artifact present");
            return;
        }

        // Instantiate maven-specific versions of the interfaces.
        WrappedLog wrappedLog = new WrappedLogMaven(getLog());
        AuthenticationServiceFactory authFactory = new AuthenticationServiceFactoryImpl();
        ErrorRaiser<MojoExecutionException> errorRaiser = new ErrorRaiserMavenImpl(getLog());
        URLCalculator<MojoExecutionException> urlCalculator = new URLCalculator<MojoExecutionException>(errorRaiser);
        GalasaRestApiMetadata restApiMetadata = new GalasaRestApiMetadata();
        BootstrapLoader<MojoExecutionException> bootstrapLoader = new BootstrapLoaderImpl<MojoExecutionException>(wrappedLog,errorRaiser);
        TestCatalogArtifact<MojoExecutionException> wrappedTestCatalogArtifact = new TestCatalogArtifactMavenImpl(testCatalogArtifact, errorRaiser);
        TestCatalogArtifactDeployer<MojoExecutionException> deployer = new TestCatalogArtifactDeployer<MojoExecutionException>(
            wrappedLog, errorRaiser, bootstrapLoader, urlCalculator, restApiMetadata, authFactory);

        // Deploy the test catalog to the Galasa server.
        deployer.deployToServer(bootstrapUrl, testStream, galasaAccessToken, wrappedTestCatalogArtifact);
    }

    private Artifact getTestCatalogArtifact() {
        Artifact artifact = null;
        for (Artifact a : project.getAttachedArtifacts()) {
            if ("testcatalog".equals(a.getClassifier()) && "json".equals(a.getType())) {
                artifact = a;
                break;
            }
        }
        return artifact;
    }
    
}
