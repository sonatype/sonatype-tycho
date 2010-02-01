package org.codehaus.tycho;

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;

public interface TargetPlatform
{
    /**
     * Returns all artifact files of given types. 
     * 
     * Implementation must insure artifacts corresponding to MavenProjects
     * are at the end of the list. 
     * 
     * TODO Probably not the best API spec.
     */
    public List<File> getArtifactFiles( String... types );

    public List<File> getSites();

    /**
     * Returns artifact of given type and id and best matching version.
     * 
     * 0.0.0 will matches latest version
     * 1.2.3 will matches 1.2.3 with latest qualifier
     * 1.2.3.qualifier matches this exact version
     *
     * TODO should we use version ranges explicitly here? Currently,
     * there is no way to match 1.2.3 without qualifier
     * 
     * TODO do we need to distinguish eclipse-plugin from eclipse-test-plugin???
     */
    public ArtifactKey getArtifactKey( String type, String id, String version );

    public File getArtifact( String type, String id, String version );

    public File getArtifact( ArtifactKey key );
    
    public MavenProject getMavenProject( File location );


}
