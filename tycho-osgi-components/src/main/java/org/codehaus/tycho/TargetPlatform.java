package org.codehaus.tycho;

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;

public interface TargetPlatform
{
    /**
     * Returns all artifacts of the given type. 
     */
    public List<ArtifactDescription> getArtifacts( String type );

    /**
     * Returns artifact of the given type and id and best matching version.
     * 
     * 0.0.0 will matches latest version
     * 1.2.3 will matches 1.2.3 with latest qualifier
     * 1.2.3.qualifier matches this exact version
     *
     * TODO should we use version ranges explicitly here? Currently,
     * there is no way to match 1.2.3 without qualifier.
     */
    public ArtifactDescription getArtifact( String type, String id, String version );

    public MavenProject getMavenProject( File location );
}
