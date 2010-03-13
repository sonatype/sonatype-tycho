package org.codehaus.tycho.osgitools;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.ArtifactDescription;
import org.codehaus.tycho.ArtifactKey;

public class DefaultArtifactDescription
    implements ArtifactDescription
{

    private ArtifactKey key;

    private File location;

    private MavenProject project;

    public DefaultArtifactDescription( ArtifactKey key, File location, MavenProject project )
    {
        this.key = key;
        this.location = location;
        this.project = project;
    }

    public ArtifactKey getKey()
    {
        return key;
    }

    public File getLocation()
    {
        return location;
    }

    public MavenProject getMavenProject()
    {
        return project;
    }
}
