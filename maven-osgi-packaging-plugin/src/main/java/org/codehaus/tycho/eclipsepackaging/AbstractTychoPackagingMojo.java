package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.AbstractScanner;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.FeatureResolutionState;
import org.codehaus.tycho.TychoConstants;

public abstract class AbstractTychoPackagingMojo
    extends AbstractMojo
{
    /** @parameter expression="${session}" */
    protected MavenSession session;

    /** @parameter expression="${project}" */
    protected MavenProject project;

    /** @parameter default-value="true" */
    protected boolean useDefaultExcludes;

    /** @component */
    protected PlexusContainer plexus;

    protected FeatureResolutionState featureResolutionState;

    protected BundleResolutionState bundleResolutionState;

    /** @component */
    protected MavenProjectHelper projectHelper;

    protected void initializeProjectContext()
    {
        featureResolutionState =
            (FeatureResolutionState) project.getContextValue( TychoConstants.CTX_FEATURE_RESOLUTION_STATE );

        bundleResolutionState =
            (BundleResolutionState) project.getContextValue( TychoConstants.CTX_BUNDLE_RESOLUTION_STATE );
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
