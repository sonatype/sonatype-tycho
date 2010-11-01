package org.sonatype.tycho;

import java.io.File;
import java.util.Set;

import org.sonatype.tycho.resolver.DependentMavenProjectProxy;

public interface ArtifactDescriptor
{
    public ArtifactKey getKey();

    public File getLocation();

    public DependentMavenProjectProxy getMavenProject();

    // TODO should this come from separate P2ArtifactDescriptor interface?
    public Set<Object> getInstallableUnits();
}
