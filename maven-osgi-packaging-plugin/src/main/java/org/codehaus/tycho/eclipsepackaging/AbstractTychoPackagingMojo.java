package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.IOException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.FeatureResolutionState;
import org.codehaus.tycho.TychoSession;
import org.codehaus.tycho.maven.TychoMavenSession;
import org.sonatype.tycho.osgi.EquinoxEmbedder;
import org.sonatype.tycho.p2.facade.P2Generator;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;

public abstract class AbstractTychoPackagingMojo
    extends AbstractMojo
    implements Initializable
{
    /** @parameter expression="${session}" */
    protected MavenSession session;

    /** @parameter expression="${project}" */
    protected MavenProject project;

    /** @component */
    protected PlexusContainer plexus;

    /** @component */
    private EquinoxEmbedder equinox;

    protected TychoSession tychoSession;

    protected FeatureResolutionState featureResolutionState;

    protected BundleResolutionState bundleResolutionState;

    protected P2Generator p2;

    /** @component */
    protected MavenProjectHelper projectHelper;

    protected void initializeProjectContext()
    {
        if ( !( session instanceof TychoMavenSession ) )
        {
            throw new IllegalArgumentException( getClass().getSimpleName() + " mojo only works with Tycho distribution" );
        }

        TychoMavenSession tms = (TychoMavenSession) session;

        tychoSession = tms.getTychoSession();

        featureResolutionState = tychoSession.getFeatureResolutionState( project );

        bundleResolutionState = tychoSession.getBundleResolutionState( project );
    }

    public void initialize()
        throws InitializationException
    {
        this.p2 = equinox.getService( P2Generator.class );
    }

    protected void attachP2Metadata()
        throws MojoExecutionException
    {
        File file = project.getArtifact().getFile();

        if ( file == null || !file.canRead() )
        {
            throw new IllegalStateException();
        }

        File content = new File( project.getBuild().getDirectory(), "p2content.xml" );
        File artifacts = new File( project.getBuild().getDirectory(), "p2artifacts.xml" );

        try
        {
            p2.generateMetadata( file, project.getPackaging(), project.getGroupId(), project.getArtifactId(), project
                .getVersion(), content, artifacts );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not generate P2 metadata", e );
        }

        projectHelper.attachArtifact(
            project,
            RepositoryLayoutHelper.EXTENSION_P2_METADATA,
            RepositoryLayoutHelper.CLASSIFIER_P2_METADATA,
            content );

        projectHelper.attachArtifact(
            project,
            RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS,
            RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS,
            artifacts );
    }
}
