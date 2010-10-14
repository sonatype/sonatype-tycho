package org.sonatype.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;
import org.codehaus.tycho.TargetPlatform;
import org.sonatype.tycho.equinox.embedder.EquinoxRuntimeLocator;
import org.sonatype.tycho.p2.facade.P2MetadataRepositoryWriter;

public abstract class AbstractPublishMojo
    extends AbstractP2Mojo
{
    static String PUBLISHER_BUNDLE_ID = "org.eclipse.equinox.p2.publisher";

    /**
     * @parameter default-value="true"
     */
    private boolean compress;

    /** @component */
    private EquinoxRuntimeLocator equinoxLocator;

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

            Commandline cli = createOSGiCommandline( publishApplicationName );
            cli.setWorkingDirectory( getProject().getBasedir() );
            cli.addArguments( new String[] { "-artifactRepository", getRepositoryUrl(), //
                "-metadataRepository", getRepositoryUrl(), //
                "-contextMetadata", contextRepositoryUrl, //
                "-append", //
                "-publishArtifacts" } );
            cli.addArguments( getCompressFlag() );
            cli.addArguments( additionalArgs );

            try
            {
                int result = executeCommandline( cli, forkedProcessTimeoutInSeconds );
                if ( result != 0 )
                {
                    throw new MojoFailureException( "P2 publisher return code was " + result );
                }
            }
            catch ( CommandLineException cle )
            {
                throw new MojoExecutionException( "P2 publisher failed to be executed ", cle );
            }
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Unable to execute the publisher", ioe );
        }
    }

    private int executeCommandline( Commandline cli, int timeout )
        throws CommandLineException
    {
        getLog().info( "Command line:\n\t" + cli.toString() );
        return CommandLineUtils.executeCommandLine( cli, new DefaultConsumer(),
                                                    new WriterStreamConsumer( new OutputStreamWriter( System.err ) ),
                                                    timeout );
    }

    static Commandline createOSGiCommandline( String applicationId, File equinoxLauncher )
    {
        Commandline cli = new Commandline();

        String executable = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
        cli.setExecutable( executable );
        cli.addArguments( new String[] { "-jar", equinoxLauncher.getAbsolutePath() } );
        cli.addArguments( new String[] { "-application", applicationId } );
        cli.addArguments( new String[] { "-nosplash" } );
        cli.addArguments( new String[] { "-consoleLog" } );
        return cli;
    }

    private Commandline createOSGiCommandline( String applicationId )
    {
        File equinoxLauncher = getEquinoxLauncher( equinoxLocator );
        return createOSGiCommandline( applicationId, equinoxLauncher );
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

    private static File getEquinoxLauncher( EquinoxRuntimeLocator equinoxLocator )
    {
        File p2location = equinoxLocator.getRuntimeLocations().get(0);
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( p2location );
        ds.setIncludes( new String[] { "plugins/org.eclipse.equinox.launcher_*.jar" } );
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        if ( includedFiles == null || includedFiles.length != 1 )
        {
            throw new IllegalStateException( "Can't locate org.eclipse.equinox.launcher bundle in " + p2location );
        }
        return new File( p2location, includedFiles[0] );
    }

}
