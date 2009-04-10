package org.codehaus.tycho;

import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;


/**
 * Target platform content resolver.
 * 
 * TODO This interface and its implementations require further refinement.
 *      I need to decide if new resolver instance is required for each project.
 */
public interface TargetPlatformResolver
{
    public TargetPlatform resolvePlatform( MavenProject project );

    public void setMavenProjects( List<MavenProject> projects );

    public void setLocalRepository( ArtifactRepository localRepository );

    /**
     * Set MavenExecutionRequest execution properties.
     *  
     * This is a workaround for questionable Maven design decision (i.e. project
     * properties have everything from inherited and interpolated but without
     * session properties)
     */
    public void setProperties( Properties properties );
}
