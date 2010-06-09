package org.sonatype.tycho.p2.facade;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.model.Target;
import org.codehaus.tycho.model.Target.Location;
import org.codehaus.tycho.osgitools.targetplatform.AbstractTargetPlatformResolver;
import org.codehaus.tycho.osgitools.targetplatform.DefaultTargetPlatform;
import org.codehaus.tycho.osgitools.targetplatform.MultiEnvironmentTargetPlatform;
import org.codehaus.tycho.p2.P2ArtifactRepositoryLayout;
import org.codehaus.tycho.utils.ExecutionEnvironmentUtils;
import org.codehaus.tycho.utils.PlatformPropertiesUtils;
import org.sonatype.tycho.osgi.EquinoxEmbedder;
import org.sonatype.tycho.p2.facade.internal.ArtifactFacade;
import org.sonatype.tycho.p2.facade.internal.DefaultTychoRepositoryIndex;
import org.sonatype.tycho.p2.facade.internal.MavenProjectFacade;
import org.sonatype.tycho.p2.facade.internal.MavenRepositoryReader;
import org.sonatype.tycho.p2.facade.internal.P2Logger;
import org.sonatype.tycho.p2.facade.internal.P2RepositoryCache;
import org.sonatype.tycho.p2.facade.internal.P2ResolutionResult;
import org.sonatype.tycho.p2.facade.internal.P2Resolver;
import org.sonatype.tycho.p2.facade.internal.P2ResolverFactory;
import org.sonatype.tycho.p2.facade.internal.TychoRepositoryIndex;

@Component( role = TargetPlatformResolver.class, hint = P2TargetPlatformResolver.ROLE_HINT, instantiationStrategy = "per-lookup" )
public class P2TargetPlatformResolver
    extends AbstractTargetPlatformResolver
    implements TargetPlatformResolver, Initializable
{

    public static final String ROLE_HINT = "p2";

    @Requirement
    private EquinoxEmbedder equinox;

    @Requirement
    private PlexusContainer plexus;

    @Requirement
    private RepositorySystem repositorySystem;

    private P2ResolverFactory resolverFactory;

    @Requirement( hint = "p2" )
    private ArtifactRepositoryLayout p2layout;

    @Requirement
    private P2RepositoryCache repositoryCache;

    @Requirement
    private ProjectDependenciesResolver projectDependenciesResolver;

    private static final ArtifactRepositoryPolicy P2_REPOSITORY_POLICY =
        new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER,
                                      ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );

    public TargetPlatform resolvePlatform( MavenSession session, MavenProject project, List<Dependency> dependencies )
    {
        P2Resolver resolver = resolverFactory.createResolver();

        try
        {
            return doResolvePlatform( session, project, dependencies, resolver );
        }
        finally
        {
            resolver.stop();
        }
    }

    protected TargetPlatform doResolvePlatform( MavenSession session, MavenProject project,
                                                List<Dependency> dependencies, P2Resolver resolver )
    {
        TargetPlatformConfiguration configuration =
            (TargetPlatformConfiguration) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION );

        resolver.setRepositoryCache( repositoryCache );

        resolver.setOffline( session.isOffline() );

        resolver.setLogger( new P2Logger()
        {
            public void debug( String message )
            {
                if ( message != null && message.length() > 0 )
                {
                    getLogger().info( message ); // TODO
                }
            }

            public void info( String message )
            {
                if ( message != null && message.length() > 0 )
                {
                    getLogger().info( message );
                }
            }
        } );

        Map<File, MavenProject> projects = new HashMap<File, MavenProject>();

        resolver.setLocalRepositoryLocation( new File( session.getLocalRepository().getBasedir() ) );

        resolver.setEnvironments( getEnvironments( configuration ) );

        for ( MavenProject otherProject : session.getProjects() )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "P2resolver.addMavenProject " + otherProject.toString() );
            }
            projects.put( otherProject.getBasedir(), otherProject );
            resolver.addMavenProject( new MavenProjectFacade( otherProject ) );
        }

        if ( dependencies != null )
        {
            for ( Dependency dependency : dependencies )
            {
                resolver.addDependency( dependency.getType(), dependency.getArtifactId(), dependency.getVersion() );
            }
        }

        if ( TargetPlatformConfiguration.POM_DEPENDENCIES_CONSIDER.equals( configuration.getPomDependencies() ) )
        {
            Set<String> projectIds = new HashSet<String>( session.getProjects().size() * 2 );
            for ( MavenProject p : session.getProjects() )
            {
                String key = ArtifactUtils.key( p.getGroupId(), p.getArtifactId(), p.getVersion() );
                projectIds.add( key );
            }

            ArrayList<String> scopes = new ArrayList<String>();
            scopes.add( Artifact.SCOPE_COMPILE );
            Collection<Artifact> artifacts;
            try
            {
                artifacts = projectDependenciesResolver.resolve( project, scopes, session );
            }
            catch ( MultipleArtifactsNotFoundException e )
            {
                Collection<Artifact> missing = new HashSet<Artifact>( e.getMissingArtifacts() );

                for ( Iterator<Artifact> it = missing.iterator(); it.hasNext(); )
                {
                    Artifact a = it.next();
                    String key = ArtifactUtils.key( a.getGroupId(), a.getArtifactId(), a.getBaseVersion() );
                    if ( projectIds.contains( key ) )
                    {
                        it.remove();
                    }
                }

                if ( !missing.isEmpty() )
                {
                    throw new RuntimeException( "Could not resolve project dependencies", e );
                }

                artifacts = e.getResolvedArtifacts();
                artifacts.removeAll( e.getMissingArtifacts() );
            }
            catch ( AbstractArtifactResolutionException e )
            {
                throw new RuntimeException( "Could not resolve project dependencies", e );
            }
            for ( Artifact artifact : artifacts )
            {
                String key =
                    ArtifactUtils.key( artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion() );
                if ( projectIds.contains( key ) )
                {
                    // resolved to an older snapshot from the repo, we only want the current project in the reactor
                    continue;
                }
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "P2resolver.addMavenArtifact " + artifact.toString() );
                }
                resolver.addMavenArtifact( new ArtifactFacade( artifact ) );
            }
        }

        for ( ArtifactRepository repository : project.getRemoteArtifactRepositories() )
        {
            try
            {
                URI uri = new URL( repository.getUrl() ).toURI();

                if ( repository.getLayout() instanceof P2ArtifactRepositoryLayout )
                {
                    if ( session.isOffline() )
                    {
                        getLogger().debug( "Offline mode, using local cache only for repository " + repository.getId()
                                               + " (" + repository.getUrl() + ")" );
                    }

                    try
                    {
                        Authentication auth = repository.getAuthentication();
                        if ( auth != null )
                        {
                            resolver.setCredentials( uri, auth.getUsername(), auth.getPassword() );
                        }

                        resolver.addP2Repository( uri );

                        getLogger().debug( "Added p2 repository " + repository.getId() + " (" + repository.getUrl()
                                               + ")" );
                    }
                    catch ( Exception e )
                    {
                        String msg =
                            "Failed to access p2 repository " + repository.getId() + " (" + repository.getUrl()
                                + "), will try to use local cache. Reason: " + e.getMessage();
                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().warn( msg, e );
                        }
                        else
                        {
                            getLogger().warn( msg );
                        }
                    }
                }
                else
                {
                    if ( !configuration.isIgnoreTychoRepositories() && !session.isOffline() )
                    {
                        try
                        {
                            MavenRepositoryReader reader = plexus.lookup( MavenRepositoryReader.class );
                            reader.setArtifactRepository( repository );
                            reader.setLocalRepository( session.getLocalRepository() );

                            String repositoryKey = getRepositoryKey( repository );
                            TychoRepositoryIndex index = repositoryCache.getRepositoryIndex( repositoryKey );
                            if ( index == null )
                            {
                                index = new DefaultTychoRepositoryIndex( reader );

                                repositoryCache.putRepositoryIndex( repositoryKey, index );
                            }

                            resolver.addMavenRepository( uri, index, reader );
                            getLogger().debug( "Added Maven repository " + repository.getId() + " ("
                                                   + repository.getUrl() + ")" );
                        }
                        catch ( FileNotFoundException e )
                        {
                            // it happens
                        }
                        catch ( Exception e )
                        {
                            getLogger().debug( "Unable to initialize remote Tycho repository", e );
                        }
                    }
                    else
                    {
                        String msg =
                            "Ignoring Maven repository " + repository.getId() + " (" + repository.getUrl() + ")";
                        if ( session.isOffline() )
                        {
                            msg += " while in offline mode";
                        }
                        getLogger().debug( msg );
                    }
                }
            }
            catch ( MalformedURLException e )
            {
                getLogger().warn( "Could not parse repository URL", e );
            }
            catch ( URISyntaxException e )
            {
                getLogger().warn( "Could not parse repository URL", e );
            }
        }

        Target target = configuration.getTarget();

        if ( target != null )
        {
            Set<URI> uris = new HashSet<URI>();

            for ( Target.Location location : target.getLocations() )
            {
                String type = location.getType();
                if ( !"InstallableUnit".equalsIgnoreCase( type ) )
                {
                    getLogger().warn( "Target location type: " + type + " is not supported" );
                    continue;
                }

                try
                {
                    URI uri = new URI( getMirror( location, session.getRequest().getMirrors() ) );
                    if ( uris.add( uri ) )
                    {
                        if ( session.isOffline() )
                        {
                            getLogger().debug( "Ignored repository " + uri + " while in offline mode" );
                        }
                        else
                        {
                            String id = location.getRepositoryId();
                            if ( id != null )
                            {
                                Server server = session.getSettings().getServer( id );

                                if ( server != null )
                                {
                                    resolver.setCredentials( uri, server.getUsername(), server.getPassword() );
                                }
                                else
                                {
                                    getLogger().info( "Unknown server id=" + id + " for repository location="
                                                          + location.getRepositoryLocation() );
                                }
                            }

                            try
                            {
                                resolver.addP2Repository( uri );
                            }
                            catch ( Exception e )
                            {
                                String msg =
                                    "Failed to access p2 repository " + uri + ", will try to use local cache. Reason: "
                                        + e.getMessage();
                                if ( getLogger().isDebugEnabled() )
                                {
                                    getLogger().warn( msg, e );
                                }
                                else
                                {
                                    getLogger().warn( msg );
                                }
                            }
                        }
                    }
                }
                catch ( URISyntaxException e )
                {
                    getLogger().debug( "Could not parse repository URL", e );
                }

                for ( Target.Unit unit : location.getUnits() )
                {
                    resolver.addDependency( P2Resolver.TYPE_INSTALLABLE_UNIT, unit.getId(), unit.getVersion() );
                }
            }
        }

        List<P2ResolutionResult> results = resolver.resolveProject( project.getBasedir() );

        MultiEnvironmentTargetPlatform multiPlatform = new MultiEnvironmentTargetPlatform();

        // FIXME this is just wrong
        for ( int i = 0; i < configuration.getEnvironments().size(); i++ )
        {
            TargetEnvironment environment = configuration.getEnvironments().get( i );
            P2ResolutionResult result = results.get( i );

            DefaultTargetPlatform platform = new DefaultTargetPlatform();

            platform.addSite( new File( session.getLocalRepository().getBasedir() ) );

            for ( Entry<ArtifactKey, File> entry : result.getArtifacts().entrySet() )
            {
                MavenProject otherProject = projects.get( entry.getValue() );
                if ( otherProject != null )
                {
                    platform.addMavenProject( entry.getKey(), otherProject );
                }
                else
                {
                    platform.addArtifactFile( entry.getKey(), entry.getValue() );
                }
            }

            // addProjects( session, platform );

            multiPlatform.addPlatform( environment, platform );
        }

        return multiPlatform;
    }

    private List<Map<String, String>> getEnvironments( TargetPlatformConfiguration configuration )
    {
        ArrayList<Map<String, String>> environments = new ArrayList<Map<String, String>>();

        for ( TargetEnvironment environment : configuration.getEnvironments() )
        {
            Properties properties = new Properties();
            properties.put( PlatformPropertiesUtils.OSGI_OS, environment.getOs() );
            properties.put( PlatformPropertiesUtils.OSGI_WS, environment.getWs() );
            properties.put( PlatformPropertiesUtils.OSGI_ARCH, environment.getArch() );
            ExecutionEnvironmentUtils.loadVMProfile( properties );

            // TODO does not belong here
            properties.put( "org.eclipse.update.install.features", "true" );

            Map<String, String> map = new LinkedHashMap<String, String>();
            for ( Object key : properties.keySet() )
            {
                map.put( key.toString(), properties.getProperty( key.toString() ) );
            }
            environments.add( map );
        }

        return environments;
    }

    private String getRepositoryKey( ArtifactRepository repository )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( repository.getId() );
        sb.append( '|' ).append( repository.getUrl() );
        return sb.toString();
    }

    private String getMirror( Location location, List<Mirror> mirrors )
    {
        String url = location.getRepositoryLocation();
        String id = location.getRepositoryId();
        if ( id == null )
        {
            id = url;
        }

        ArtifactRepository repository =
            repositorySystem.createArtifactRepository( id, url, p2layout, P2_REPOSITORY_POLICY, P2_REPOSITORY_POLICY );

        Mirror mirror = repositorySystem.getMirror( repository, mirrors );

        return mirror != null ? mirror.getUrl() : url;
    }

    public void initialize()
        throws InitializationException
    {
        this.resolverFactory = equinox.getService( P2ResolverFactory.class );
    }
}
