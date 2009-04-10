package org.codehaus.tycho.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.FeatureResolutionState;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoSession;
import org.codehaus.tycho.osgitools.project.EclipsePluginProject;
import org.codehaus.tycho.osgitools.project.EclipsePluginProjectImpl;

@Component( role = MutableTychoSession.class, instantiationStrategy = "per-lookup" )
public class MutableTychoSession
    extends AbstractLogEnabled
    implements TychoSession
{

    @Requirement
    PlexusContainer plexus;

    private Map<File, MavenProject> projects = new LinkedHashMap<File, MavenProject>();

    private Map<File, TargetPlatform> platforms = new LinkedHashMap<File, TargetPlatform>();

    private Map<File, BundleResolutionState> bundleResolvers = new LinkedHashMap<File, BundleResolutionState>();

    private Map<File, FeatureResolutionState> featureResolvers = new LinkedHashMap<File, FeatureResolutionState>();

    private Map<File, EclipsePluginProject> pdeProjects = new LinkedHashMap<File, EclipsePluginProject>();

    private Map<String, Object> sessionContext = new HashMap<String, Object>();

    public BundleResolutionState getBundleResolutionState( MavenProject project )
    {
        File basedir = project.getBasedir();

        BundleResolutionState resolver = null;

        if ( bundleResolvers.containsKey( basedir ) )
        {
            resolver = bundleResolvers.get( basedir );
        }
        else
        {
            TargetPlatform platform = platforms.get( basedir );

            if ( platform != null )
            {
                resolver = EquinoxBundleResolutionState.newInstance( plexus, this, project );
            }

            bundleResolvers.put( basedir, resolver );
        }
        return resolver;
    }

    public FeatureResolutionState getFeatureResolutionState( MavenProject project )
    {
        File basedir = project.getBasedir();

        FeatureResolutionState resolver = null;

        if ( featureResolvers.containsKey( basedir ) )
        {
            resolver = featureResolvers.get( basedir );
        }
        else
        {
            TargetPlatform platform = platforms.get( basedir );
            if ( platform != null )
            {
                resolver = new FeatureResolutionState( getLogger(), this, platform );
            }

            featureResolvers.put( basedir, resolver );
        }
        return resolver;
    }

    public MavenProject getMavenProject( File location )
    {
        return projects.get( location );
    }

    public MavenProject getMavenProject( String location )
    {
        return getMavenProject( new File( location ) );
    }

    public void setTargetPlatform( MavenProject project, TargetPlatform platform )
    {
        File basedir = project.getBasedir();
        projects.put( basedir, project );
        platforms.put( basedir, platform );
    }

    public void setProjects( List<MavenProject> projects )
    {
        for ( MavenProject project : projects )
        {
            this.projects.put( project.getBasedir(), project );
        }
    }

    public TargetPlatform getTargetPlatform( MavenProject project )
    {
        return platforms.get( project.getBasedir() );
    }

    public EclipsePluginProject getEclipsePluginProject( MavenProject project )
    {
        EclipsePluginProject pdeProject = pdeProjects.get( project.getBasedir() );
        if ( pdeProject == null )
        {
            BundleResolutionState resolver = getBundleResolutionState( project );

            try
            {
                pdeProject =
                    new EclipsePluginProjectImpl( project, resolver.getBundleByLocation( project.getBasedir() ) );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }

            pdeProjects.put( project.getBasedir(), pdeProject );
        }

        return pdeProject;
    }

    public Map<String, Object> getSessionContext()
    {
        return sessionContext;
    }

}
