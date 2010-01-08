package org.codehaus.tycho.buildversion;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Updates maven version to match OSGi version
 * 
 * @goal synchronize-versions
 * @phase verify
 * @TODO ^^^ should be pre-install
 */
public class SynchronizeMavenVersionMojo
    extends AbstractVersionMojo
{

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !project.getArtifact().isSnapshot() )
        {
            // lets do it for snapshot artifacts only for now
            return;
        }

        String version = getOSGiVersion();

        if ( version != null )
        {
            updateVersion( version );
        }
    }

    private void updateVersion( String version )
    {
        project.getArtifact().setVersion( version );
        for ( Artifact artifact : project.getAttachedArtifacts() )
        {
            artifact.setVersion( version );
        }
    }

}
