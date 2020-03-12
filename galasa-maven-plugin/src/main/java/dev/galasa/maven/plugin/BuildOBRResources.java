/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.maven.plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;

import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;
import org.apache.felix.bundlerepository.impl.RepositoryImpl;
import org.apache.felix.bundlerepository.impl.ResourceImpl;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "obrresources", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class BuildOBRResources extends AbstractMojo {

    public enum OBR_URL_TYPE {
        file,
        mvn
    }

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File         outputDirectory;

    @Parameter(defaultValue = "${project.build.directory}", property = "targerDir", required = true)
    private File         projectTargetDirectory;

    @Parameter(defaultValue = "${galasa.obr.url.type}", property = "obrUrlType", required = false)
    private OBR_URL_TYPE obrUrlType;

    @Parameter(defaultValue = "false", property = "includeSelf", required = false)
    private boolean      includeSelf;

    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("BuildOBRResources: Building project " + project.getName() + ". includeSelf=" + includeSelf);
        if (obrUrlType == null) {
            obrUrlType = OBR_URL_TYPE.mvn;
        }

        DataModelHelper obrDataModelHelper = new DataModelHelperImpl();

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        File repositoryFile = new File(outputDirectory, "repository.obr");
        project.getArtifact().setFile(repositoryFile);

        if (includeSelf) {
            Artifact selfArtifact = new DefaultArtifact(project.getGroupId(), project.getArtifactId(),
                    project.getVersion(), Artifact.SCOPE_COMPILE, "jar", null,
                    project.getArtifact().getArtifactHandler());
            getLog().info("BuildOBRResources: Adding artifact " + selfArtifact.getId() + "as a dependency to "
                    + project.getName());
            selfArtifact.setResolved(true);
            selfArtifact.setFile(new File(projectTargetDirectory.getAbsolutePath() + "/" + project.getArtifactId() + "-"
                    + project.getVersion() + ".jar"));
            Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();
            dependencyArtifacts.add(selfArtifact);
            project.setDependencyArtifacts(dependencyArtifacts);
        }

        RepositoryImpl newRepository = new RepositoryImpl();

        for (Object dependency : project.getDependencyArtifacts()) {
            if (dependency instanceof DefaultArtifact) {
                DefaultArtifact artifact = (DefaultArtifact) dependency;
                if (artifact.isResolved() && artifact.getScope().equals("compile")) {
                    if (artifact.getFile().getName().endsWith(".jar")) {
                        processBundle(artifact, newRepository, obrDataModelHelper);
                    } else if (artifact.getFile().getName().endsWith(".obr")) {
                        processObr(artifact, newRepository, obrDataModelHelper);
                    }
                }
            }
        }

        if (newRepository.getResources() == null || newRepository.getResources().length == 0) {
            throw new MojoFailureException("No resources have been added to the repository");
        }

        getLog().info("BuildOBRResources: OBR Artifact ID is " + project.getArtifact().getId());

        newRepository.setName(project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        newRepository.setLastModified(sdf.format(Calendar.getInstance().getTime()));

        try {
            FileWriter fw = new FileWriter(repositoryFile);
            obrDataModelHelper.writeRepository(newRepository, fw);
            fw.close();
        } catch (Exception e) {
            throw new MojoExecutionException("Problem with writing repository.xml", e);
        }

        if (newRepository.getResources().length == 1) {
            getLog().info("BuildOBRResources: Repository created with " + newRepository.getResources().length
                    + " resource stored in " + repositoryFile.getAbsolutePath());
        } else {
            getLog().info("BuildOBRResources: Repository created with " + newRepository.getResources().length
                    + " resources stored in " + repositoryFile.getAbsolutePath());
        }
    }

    private void processObr(Artifact artifact, RepositoryImpl newRepository, DataModelHelper obrDataModelHelper)
            throws MojoExecutionException {

        try (FileReader fr = new FileReader(artifact.getFile())) {
            Repository mergeRepository = obrDataModelHelper.readRepository(fr);

            for (Resource resource : mergeRepository.getResources()) {
                newRepository.addResource(resource);
                getLog().info("BuildOBRResources: Merged bundle " + resource.getPresentationName() + " - "
                        + resource.getId() + " to repository");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to read existing OBR", e);
        }
    }

    private void processBundle(DefaultArtifact artifact, RepositoryImpl repository, DataModelHelper obrDataModelHelper)
            throws MojoExecutionException {

        try {
            getLog().info("BuildOBRResources: Processing artifact " + artifact.getId());
            ResourceImpl newResource = (ResourceImpl) obrDataModelHelper
                    .createResource(artifact.getFile().toURI().toURL());
            if (newResource == null) {
                throw new MojoExecutionException("Jar file not a bundle?");
            }

            URI name = null;
            switch (obrUrlType) {
                case mvn:
                    name = new URI("mvn:" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/"
                            + artifact.getBaseVersion() + "/" + artifact.getType());
                    break;
                case file:
                default:
                    name = artifact.getFile().toURI();
                    break;
            }
            newResource.put(Resource.URI, name);

            repository.addResource(newResource);

            getLog().info("BuildOBRResources: Added bundle " + newResource.getPresentationName() + " - "
                    + newResource.getId() + " to repository");
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to process dependency " + artifact.getGroupId() + ":"
                    + artifact.getArtifactId() + ":" + artifact.getVersion(), e);
        }
    }

}
