package org.codehaus.tycho;

import java.io.File;

import org.apache.maven.project.MavenProject;

public interface ArtifactDescription
{
    ArtifactKey getKey();

    File getLocation();

    MavenProject getMavenProject();

}
