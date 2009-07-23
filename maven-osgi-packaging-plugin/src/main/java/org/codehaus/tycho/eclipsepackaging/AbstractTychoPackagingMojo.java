package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.AbstractScanner;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.FeatureResolutionState;
import org.codehaus.tycho.TychoConstants;
import org.sonatype.tycho.osgi.EquinoxEmbedder;
import org.sonatype.tycho.p2.facade.P2Generator;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;

public abstract class AbstractTychoPackagingMojo
    extends AbstractMojo
{
    /** @parameter expression="${session}" */
    protected MavenSession session;

    /** @parameter expression="${project}" */
    protected MavenProject project;

    /** @parameter default-value="true" */
    protected boolean attachP2Metadata;

    /** @parameter default-value="true" */
    protected boolean useDefaultExcludes;

    /** @component */
    protected PlexusContainer plexus;

    /** @component */
    private EquinoxEmbedder equinox;

    protected FeatureResolutionState featureResolutionState;

    protected BundleResolutionState bundleResolutionState;

    private P2Generator p2;

    /** @component */
    protected MavenProjectHelper projectHelper;

    protected void initializeProjectContext()
    {
        featureResolutionState =
            (FeatureResolutionState) project.getContextValue( TychoConstants.CTX_FEATURE_RESOLUTION_STATE );

        bundleResolutionState =
            (BundleResolutionState) project.getContextValue( TychoConstants.CTX_BUNDLE_RESOLUTION_STATE );
    }

    protected P2Generator getP2Generator()
    {
        if ( p2 == null )
        {
            p2 = equinox.getService( P2Generator.class );

            if ( p2 == null )
            {
                throw new IllegalStateException( "Could not acquire P2 metadata service" );
            }
        }
        return p2;
    }

    protected void attachP2Metadata()
        throws MojoExecutionException
    {
        if ( !attachP2Metadata )
        {
            return;
        }

        File file = project.getArtifact().getFile();

        if ( file == null || !file.canRead() )
        {
            throw new IllegalStateException();
        }

        File content = new File( project.getBuild().getDirectory(), "p2content.xml" );
        File artifacts = new File( project.getBuild().getDirectory(), "p2artifacts.xml" );

        try
        {
            getP2Generator().generateMetadata( file,
                                               project.getPackaging(),
                                               project.getGroupId(),
                                               project.getArtifactId(),
                                               project.getVersion(),
                                               content,
                                               artifacts );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not generate P2 metadata", e );
        }

        projectHelper.attachArtifact( project,
                                      RepositoryLayoutHelper.EXTENSION_P2_METADATA,
                                      RepositoryLayoutHelper.CLASSIFIER_P2_METADATA,
                                      content );

        projectHelper.attachArtifact( project,
                                      RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS,
                                      RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS,
                                      artifacts );
    }

    protected List<String> toFilePattern( String pattern )
    {
        if ( pattern == null )
        {
            return new ArrayList<String>();
        }

        return Arrays.asList( pattern.split( "," ) );
    }

    protected FileSet getFileSet( File basedir, List<String> includes, List<String> excludes )
    {
        DefaultFileSet fileSet = new DefaultFileSet();
        fileSet.setDirectory( basedir );
        fileSet.setIncludes( includes.toArray( new String[includes.size()] ) );

        Set<String> allExcludes = new LinkedHashSet<String>();
        if ( excludes != null )
        {
            allExcludes.addAll( excludes );
        }
        if ( useDefaultExcludes )
        {
            allExcludes.addAll( Arrays.asList( AbstractScanner.DEFAULTEXCLUDES ) );
        }

        fileSet.setExcludes( allExcludes.toArray( new String[allExcludes.size()] ) );

        return fileSet;
    }
}
