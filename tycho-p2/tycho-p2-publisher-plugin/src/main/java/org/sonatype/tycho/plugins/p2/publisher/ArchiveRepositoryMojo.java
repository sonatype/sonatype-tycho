package org.sonatype.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;

/**
 * Just zip the repository.
 * 
 * @goal archive-repository
 */
public final class ArchiveRepositoryMojo
    extends AbstractP2Mojo
{

    /**
     * @component role="org.codehaus.plexus.archiver.Archiver" role-hint="zip"
     */
    private Archiver inflater;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        File destFile = new File( getBuildDirectory(), getProject().getArtifactId() + ".zip" );

        try
        {
            inflater.addDirectory( getAssemblyRepositoryLocation() );
            inflater.setDestFile( destFile );
            inflater.createArchive();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error packing product", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error packing product", e );
        }

        getProject().getArtifact().setFile( destFile );
    }

}
