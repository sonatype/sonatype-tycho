package org.sonatype.tycho;

import java.io.File;
import java.util.Set;

import org.apache.maven.project.MavenProject;

public interface ArtifactDescriptor
{
    public ArtifactKey getKey();

    public File getLocation();

    public MavenProject getMavenProject();

    // TODO should this come from separate P2ArtifactDescriptor interface?
    public Set<Object> getInstallableUnits();
}
