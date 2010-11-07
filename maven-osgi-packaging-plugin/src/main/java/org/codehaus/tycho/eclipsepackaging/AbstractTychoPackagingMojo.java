package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.AbstractScanner;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.osgitools.DefaultReactorProject;
import org.sonatype.tycho.ReactorProject;

/**
 * @requiresProject
 */
public abstract class AbstractTychoPackagingMojo
    extends AbstractMojo
{
    /** @parameter expression="${session}" */
    protected MavenSession session;

    /** @parameter expression="${project}" */
    protected MavenProject project;

    /** @parameter default-value="true" */
    protected boolean useDefaultExcludes;

    /**
     * Build qualifier. Recommended way to set this parameter is using build-qualifier goal.
     * 
     * @parameter expression="${buildQualifier}"
     */
    protected String qualifier;

    /** @component */
    protected PlexusContainer plexus;

    /** @component */
    protected MavenProjectHelper projectHelper;

    /**
     * @component role="org.codehaus.tycho.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;

    protected List<String> toFilePattern( String pattern )
    {
        ArrayList<String> result = new ArrayList<String>();

        if ( pattern != null )
        {
            StringTokenizer st = new StringTokenizer( pattern, "," );
            while ( st.hasMoreTokens() )
            {
                result.add( st.nextToken().trim() );
            }
        }

        return result;
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

    protected ArtifactDependencyWalker getDependencyWalker()
    {
        return getTychoProjectFacet().getDependencyWalker( project );
    }

    protected TychoProject getTychoProjectFacet()
    {
        return getTychoProjectFacet( project.getPackaging() );
    }

    protected TychoProject getTychoProjectFacet( String packaging )
    {
        TychoProject facet = projectTypes.get( packaging );
        if ( facet == null )
        {
            throw new IllegalStateException( "Unknown or unsupported packaging type " + packaging );
        }
        return facet;
    }

    protected TargetPlatform getTargetPlatform()
    {
        return getTychoProjectFacet().getTargetPlatform( project );
    }

    protected void expandVersion()
    {
        ReactorProject reactorProject = DefaultReactorProject.adapt( project );
        String originalVersion = getTychoProjectFacet().getArtifactKey( reactorProject ).getVersion();
        reactorProject.setExpandedVersion( originalVersion, qualifier );
    }

}
