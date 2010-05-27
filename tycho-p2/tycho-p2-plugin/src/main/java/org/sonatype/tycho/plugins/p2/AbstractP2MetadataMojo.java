package org.sonatype.tycho.plugins.p2;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.sonatype.tycho.osgi.EquinoxEmbedder;

public abstract class AbstractP2MetadataMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     * @required
     */
    protected MavenProject project;

    /**
     * Metadata repository name
     * 
     * @parameter default-value="${project.name}"
     * @required
     */
    protected String metadataRepositoryName;

    /**
     * Generated update site location (must match update-site mojo configuration)
     * 
     * @parameter expression="${project.build.directory}/site"
     */
    protected File target;

    /**
     * Artifact repository name
     * 
     * @parameter default-value="${project.name} Artifacts"
     * @required
     */
    protected String artifactRepositoryName;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for the process, never
     * timing out.
     * 
     * @parameter expression="${p2.timeout}"
     */
    private int forkedProcessTimeoutInSeconds;

    /**
     * Arbitrary JVM options to set on the command line.
     * 
     * @parameter
     */
    private String argLine;

    /**
     * @parameter default-value="true"
     */
    protected boolean generateP2Metadata;

    /** @component */
    private EquinoxEmbedder p2;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !generateP2Metadata )
        {
            return;
        }

        try
        {
            if ( getUpdateSiteLocation().isDirectory() )
            {
                generateMetadata();
            }
            else
            {
                getLog().warn( getUpdateSiteLocation().getAbsolutePath() + " does not exist or is not a directory" );
            }
        }
        catch ( MojoFailureException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Cannot generate P2 metadata", e );
        }
    }

    private void generateMetadata()
        throws Exception
    {
        Commandline cli = new Commandline();

        cli.setWorkingDirectory( project.getBasedir() );

        String executable = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
        if ( File.separatorChar == '\\' )
        {
            executable = executable + ".exe";
        }
        cli.setExecutable( executable );

        cli.addArguments( new String[] { "-jar", getEquinoxLauncher().getCanonicalPath(), } );

        cli.addArguments( new String[] { "-nosplash", //
            "-application", getPublisherApplication(), } );

        addArguments( cli );

        if ( argLine != null && argLine.trim().length() > 0 )
        {
            cli.addArguments( new String[] { "-vmargs", argLine, } );
        }

        getLog().info( "Command line:\n\t" + cli.toString() );

        StreamConsumer out = new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                System.out.println( line );
            }
        };

        StreamConsumer err = new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                System.err.println( line );
            }
        };

        int result = CommandLineUtils.executeCommandLine( cli, out, err, forkedProcessTimeoutInSeconds );
        if ( result != 0 )
        {
            throw new MojoFailureException( "P2 publisher return code was " + result );
        }
    }

    protected void addArguments( Commandline cli )
        throws IOException, MalformedURLException
    {
        cli.addArguments( new String[] { "-source", getUpdateSiteLocation().getCanonicalPath(), //
            "-metadataRepository", getUpdateSiteLocation().toURL().toExternalForm(), //
            "-metadataRepositoryName", metadataRepositoryName, //
            "-artifactRepository", getUpdateSiteLocation().toURL().toExternalForm(), //
            "-artifactRepositoryName", artifactRepositoryName, //
            "-noDefaultIUs", } );
    }

    protected abstract String getPublisherApplication();

    protected File getUpdateSiteLocation()
    {
        return target;
    }

    private File getEquinoxLauncher()
        throws MojoFailureException
    {
        // XXX dirty hack
        File p2location = p2.getRuntimeLocation();
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( p2location );
        ds.setIncludes( new String[] { "plugins/org.eclipse.equinox.launcher_*.jar" } );
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        if ( includedFiles == null || includedFiles.length != 1 )
        {
            throw new MojoFailureException( "Can't locate org.eclipse.equinox.launcher bundle in " + p2location );
        }
        return new File( p2location, includedFiles[0] );
    }

}
