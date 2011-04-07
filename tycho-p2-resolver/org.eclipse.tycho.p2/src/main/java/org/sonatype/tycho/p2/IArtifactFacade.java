package org.sonatype.tycho.p2;

import java.io.File;

/**
 * Facade which provides an interface for common properties of a maven {@see Artifact} or {@see MavenProject}. Needed to
 * generate p2 metadata {@see P2Generator} for both reactor projects and binary artifacts. For eclipse-plugin reactor
 * projects, also carries information about the corresponding eclipse source bundle.
 */
public interface IArtifactFacade
{
    public File getLocation();

    public String getGroupId();

    public String getArtifactId();

    public String getClassidier();

    public String getVersion();

    public String getPackagingType();
}
