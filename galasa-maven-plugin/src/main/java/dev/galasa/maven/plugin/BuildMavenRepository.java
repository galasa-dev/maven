/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.maven.plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * This goal will copy all artifacts to a pseudo maven repository
 * 
 * @author Michael Baylis
 *
 */
@Mojo(name = "mavenrepository", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class BuildMavenRepository extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject           project;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File                   outputDirectory;

    @Parameter(defaultValue = "${build.number}", property = "buildNumber", required = false)
    private int                    buildNumber;

    private final SimpleDateFormat sdf        = new SimpleDateFormat("yyyMMdd.HHmmss");
    private final SimpleDateFormat sdfUpdated = new SimpleDateFormat("yyyMMddHHmmss");

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (buildNumber <= 0) {
            buildNumber = 1;
        }

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        String snapshotVersionPrefix = sdf.format(gregorianCalendar.getTime());
        String updated = sdfUpdated.format(gregorianCalendar.getTime());

        MetadataXpp3Writer writer = new MetadataXpp3Writer();

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new MojoExecutionException("Unable to locate SHA-1 disgest", e);
        }

        Path targetDirectory;
        try {
            targetDirectory = Paths.get(outputDirectory.toURI());
            Files.createDirectories(targetDirectory);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create the target repository directory", e);
        }

        Set<Artifact> artifacts = project.getArtifacts();
        for (Artifact artifact : artifacts) {
            if (artifact.getScope().equals(Artifact.SCOPE_COMPILE)) {
                Path artifactFile = Paths.get(artifact.getFile().toURI());

                // *** Calculate the directory structure
                String[] groupSplit = artifact.getGroupId().split("\\.");
                Path groupPath = Paths.get("repository", groupSplit);
                Path targetGroupDirectory = targetDirectory.resolve(groupPath);
                Path targetArtifactDirectory = targetGroupDirectory.resolve(artifact.getArtifactId());
                Path targetVersionDirectory = targetArtifactDirectory.resolve(artifact.getBaseVersion());

                Path targetArtifactMetadata = targetArtifactDirectory.resolve("maven-metadata.xml");
                Path targetVersionMetadata = targetVersionDirectory.resolve("maven-metadata.xml");

                // *** Calculate the target artifact and pom filenames
                String targetArtifactFileName;
                String targetArtifactVersion;
                if (artifact.isSnapshot()) {
                    targetArtifactVersion = artifact.getBaseVersion().replace("SNAPSHOT", snapshotVersionPrefix) + "-"
                            + Integer.toString(buildNumber);
                    targetArtifactFileName = artifact.getArtifactId() + "-" + targetArtifactVersion;
                } else {
                    targetArtifactVersion = artifact.getBaseVersion();
                    targetArtifactFileName = artifact.getArtifactId() + "-" + artifact.getBaseVersion();
                }

                if (artifact.hasClassifier()) {
                    targetArtifactFileName = targetArtifactFileName + "-" + artifact.getClassifier();
                }
                targetArtifactFileName = targetArtifactFileName + "." + artifact.getType();
                String targetPomFilename = artifact.getArtifactId() + "-" + targetArtifactVersion + ".pom";

                // *** Calculate the original pom file
                Path originalPomFile = artifactFile
                        .resolveSibling(artifact.getArtifactId() + "-" + artifact.getBaseVersion() + ".pom");

                // *** Get the target artifact and pom paths
                Path targetArtifactFile = targetVersionDirectory.resolve(targetArtifactFileName);
                Path targetPomFile = targetVersionDirectory.resolve(targetPomFilename);

                // *** Write
                try {
                    Files.createDirectories(targetVersionDirectory);

                    Files.copy(artifactFile, targetArtifactFile, StandardCopyOption.REPLACE_EXISTING);
                    if (Files.exists(originalPomFile)) {
                        Files.copy(originalPomFile, targetPomFile, StandardCopyOption.REPLACE_EXISTING);
                    }

                    {
                        Metadata metadata = new Metadata();
                        metadata.setGroupId(artifact.getGroupId());
                        metadata.setArtifactId(artifact.getArtifactId());

                        Versioning versioning = new Versioning();
                        metadata.setVersioning(versioning);
                        versioning.setLatest(artifact.getBaseVersion());
                        versioning.setLastUpdatedTimestamp(gregorianCalendar.getTime());
                        versioning.addVersion(artifact.getBaseVersion());
                        if (artifact.isRelease()) {
                            versioning.setRelease(artifact.getBaseVersion());
                        }

                        writer.write(Files.newOutputStream(targetArtifactMetadata), metadata);
                    }

                    if (artifact.isSnapshot()) {
                        Metadata metadata = new Metadata();
                        metadata.setGroupId(artifact.getGroupId());
                        metadata.setArtifactId(artifact.getArtifactId());
                        metadata.setVersion(artifact.getBaseVersion());

                        Versioning versioning = new Versioning();
                        metadata.setVersioning(versioning);
                        versioning.setLastUpdatedTimestamp(gregorianCalendar.getTime());

                        Snapshot snapshot = new Snapshot();
                        versioning.setSnapshot(snapshot);
                        snapshot.setBuildNumber(buildNumber);
                        snapshot.setTimestamp(snapshotVersionPrefix);

                        SnapshotVersion sv = new SnapshotVersion();
                        versioning.addSnapshotVersion(sv);
                        sv.setExtension(artifact.getType());
                        sv.setVersion(targetArtifactVersion);
                        sv.setUpdated(updated);

                        writer.write(Files.newOutputStream(targetVersionMetadata), metadata);
                    }
                } catch (Exception e) {
                    throw new MojoExecutionException("Unable to copy artifact " + artifact, e);
                }

                Path artifactHash = artifactFile.resolveSibling(targetArtifactFile.getFileName() + ".sha1");
                Path pomHash = originalPomFile.resolveSibling(targetPomFile.getFileName() + ".sha1");
                Path metaVersionHash = artifactFile.resolveSibling(targetVersionMetadata.getFileName() + ".sha1");
                Path metaArtifactHash = originalPomFile.resolveSibling(targetArtifactMetadata.getFileName() + ".sha1");

                createHash(artifactFile, artifactHash, targetVersionDirectory, digest);
                createHash(originalPomFile, pomHash, targetVersionDirectory, digest);
                createHash(targetArtifactMetadata, metaArtifactHash, targetArtifactDirectory, digest);
                if (artifact.isSnapshot()) {
                    createHash(targetVersionMetadata, metaVersionHash, targetVersionDirectory, digest);
                }

                System.out.println("Copied artifact " + artifact.toString());
            }
        }
    }

    private void createHash(Path file, Path hash, Path targetDirectory, MessageDigest digest)
            throws MojoExecutionException {
        try {
            if (Files.exists(hash)) {
                Files.copy(hash, targetDirectory.resolve(hash.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            } else {
                String hex = Hex.encodeHexString(digest.digest(Files.readAllBytes(file)));
                Files.write(targetDirectory.resolve(hash.getFileName()), hex.getBytes("utf-8"),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create/copy the hash file", e);
        }
    }

}
