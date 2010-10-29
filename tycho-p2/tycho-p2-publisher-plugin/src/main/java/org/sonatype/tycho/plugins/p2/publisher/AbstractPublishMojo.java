package org.sonatype.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.tycho.TargetPlatform;
import org.sonatype.tycho.p2.facade.P2MetadataRepositoryWriter;
import org.sonatype.tycho.p2.facade.internal.P2ApplicationLauncher;

public abstract class AbstractPublishMojo
    extends AbstractP2Mojo
{
    static String PUBLISHER_BUNDLE_ID = "org.eclipse.equinox.p2.publisher";

    /**
     * @parameter default-value="true"
     */
    private boolean compress;

    /** @component */
    private P2ApplicationLauncher launcher;

    /** @component */
    private P2MetadataRepositoryWriter metadataRepositoryWriter;
    
    /**
     * Kill the forked process after a certain number of seconds. If set to 0, wait forever for the process, never
     * timing out.
     * 
     * @parameter expression="${p2.timeout}" default-value="0"
     */
    private int forkedProcessTimeoutInSeconds;

    protected void executePublisherApplication( String publishApplicationName, String[] additionalArgs )
        throws MojoExecutionException, MojoFailureException
    {
        try
        {

            /*
             * Restore the p2 view on the Tycho build target platform that was calculated earlier (see
             * org.sonatype.tycho.p2.resolver.P2ResolverImpl .toResolutionResult). We cannot access the computation
             * logic from here because it is contained in the OSGi bundle "org.sonatype.tycho.p2.impl" in Tycho's
             * "OSGi layer" that cannot be accessed from current (lower) Mojo-Layer.
             */
            String contextRepositoryUrl =
                materializeRepository( new File( getProject().getBuild().getDirectory() ), getTargetPlatform(),
                                       getQualifier() );

            P2ApplicationLauncher launcher = this.launcher;

            launcher.setWorkingDirectory( getProject().getBasedir() );
            launcher.setApplicationName( publishApplicationName );
            launcher.addArguments( "-artifactRepository", getRepositoryUrl(), //
                                   "-metadataRepository", getRepositoryUrl(), //
                                   "-contextMetadata", contextRepositoryUrl, //
                                   "-append", //
                                   "-publishArtifacts" );
            launcher.addArguments( getCompressFlag() );
            launcher.addArguments( additionalArgs );

            int result = launcher.execute( forkedProcessTimeoutInSeconds );
            if ( result != 0 )
            {
                throw new MojoFailureException( "P2 publisher return code was " + result );
            }
        }
        catch ( MojoFailureException e )
        {
            throw e;
        }
        catch ( Exception ioe )
        {
            throw new MojoExecutionException( "Unable to execute the publisher", ioe );
        }
    }

    String materializeRepository( File targetDirectory, TargetPlatform targetPlatform, String qualifier )
        throws IOException
    {
        File repositoryLocation = new File( targetDirectory, "targetMetadataRepository" );
        repositoryLocation.mkdirs();
        FileOutputStream stream = new FileOutputStream( new File( repositoryLocation, "content.xml" ) );
        try
        {
            metadataRepositoryWriter.write( stream, targetPlatform, qualifier );
        }
        finally
        {
            stream.close();
        }
        return repositoryLocation.toURL().toExternalForm();
    }

    /**
     * @return The '-compress' flag or empty if we don't want to compress.
     */
    private String[] getCompressFlag()
    {
        return compress ? new String[] { "-compress" } : new String[0];
    }

    /**
     * @return The value of the -metadataRepository and -artifactRepository (always the same for us so far)
     */
    private String getRepositoryUrl()
        throws MalformedURLException
    {
        return getTargetRepositoryLocation().toURL().toExternalForm();
    }
}
