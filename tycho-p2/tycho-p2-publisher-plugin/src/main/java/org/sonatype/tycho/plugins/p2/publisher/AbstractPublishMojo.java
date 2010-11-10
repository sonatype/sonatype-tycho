package org.sonatype.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.sonatype.tycho.equinox.EquinoxServiceFactory;
import org.sonatype.tycho.p2.facade.P2MetadataRepositoryWriter;
import org.sonatype.tycho.p2.facade.internal.P2ApplicationLauncher;
import org.sonatype.tycho.p2.tools.FacadeException;
import org.sonatype.tycho.p2.tools.publisher.BuildContext;
import org.sonatype.tycho.p2.tools.publisher.PublisherService;
import org.sonatype.tycho.p2.tools.publisher.PublisherServiceFactory;

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

    /** @component */
    private EquinoxServiceFactory osgiServices;

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
            File contextRepositoryLocation =
                materializeRepository( new File( getProject().getBuild().getDirectory() ), getTargetPlatform(),
                                       getQualifier() );

            String contextRepositoryUrl = contextRepositoryLocation.toURL().toExternalForm();

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
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Unable to execute the publisher", ioe );
        }
    }

    protected PublisherService createPublisherService()
        throws MojoExecutionException
    {
        try
        {
            File buildDirectory = new File( getProject().getBuild().getDirectory() );

            // TODO document/refactor
            final File contextRepositoryLocation =
                materializeRepository( buildDirectory, getTargetPlatform(), getQualifier() );

            final PublisherServiceFactory publisherServiceFactory =
                osgiServices.getService( PublisherServiceFactory.class );
            final BuildContext context = new BuildContext( getQualifier(), getConfigurations(), buildDirectory );

            // pass in the Tycho target platform as context for the publishers
            final Collection<File> contextMetadataRepositories = Collections.singletonList( contextRepositoryLocation );
            final Collection<File> contextArtifactRepositories = Collections.emptyList();

            int flags = compress ? PublisherServiceFactory.REPOSITORY_COMPRESS : 0;
            return publisherServiceFactory.createPublisher( getTargetRepositoryLocation(), contextMetadataRepositories,
                                                            contextArtifactRepositories, context, flags );
        }
        catch ( FacadeException e )
        {
            throw new MojoExecutionException( "Exception while initializing the publisher service", e );
        }
    }

    /**
     * Restores the p2 view on the Tycho build target platform that was calculated earlier (see
     * org.sonatype.tycho.p2.resolver.P2ResolverImpl.toResolutionResult).
     */
    private File materializeRepository( File targetDirectory, TargetPlatform targetPlatform, String qualifier )
        throws MojoExecutionException
    {
        try
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
            return repositoryLocation;
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "I/O exception while writing the build target platform to disk", e );
        }
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

    /**
     * @return the list of the current configurations in the form "ws.os.arch"
     */
    private String[] getConfigurations()
    {
        final List<TargetEnvironment> envs = getTargetPlatformConfiguration().getEnvironments();
        return AbstractPublishMojo.getConfigurations( envs );
    }

    static String[] getConfigurations( final List<TargetEnvironment> envs )
    {
        if ( envs.isEmpty() )
        {
            // TODO this isn't a good idea, it leads to the simpleconfiguration not being used (see TYCHO-529)
            return new String[0];
        }

        final String[] configurations = new String[envs.size()];
        int ix = 0;
        for ( final TargetEnvironment env : envs )
        {
            configurations[ix++] = env.getWs() + "." + env.getOs() + "." + env.getArch();
        }
        return configurations;
    }
}
