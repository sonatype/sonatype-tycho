package org.sonatype.tycho.p2.facade;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.tycho.DefaultTargetPlatform;
import org.codehaus.tycho.ExecutionEnvironmentUtils;
import org.codehaus.tycho.PlatformPropertiesUtils;
import org.codehaus.tycho.ProjectType;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.model.Target;
import org.codehaus.tycho.osgitools.targetplatform.AbstractTargetPlatformResolver;
import org.codehaus.tycho.p2.P2ArtifactRepositoryLayout;
import org.sonatype.tycho.osgi.EquinoxEmbedder;
import org.sonatype.tycho.p2.facade.internal.DefaultTychoRepositoryIndex;
import org.sonatype.tycho.p2.facade.internal.MavenRepositoryReader;
import org.sonatype.tycho.p2.facade.internal.P2Logger;
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

    public TargetPlatform resolvePlatform( MavenProject project, List<Dependency> dependencies )
    {
        TargetPlatformConfiguration configuration = (TargetPlatformConfiguration) project
            .getContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION );

        P2Resolver resolver = resolverFactory.createResolver();

        resolver.setLogger( new P2Logger()
        {
            public void debug( String message )
            {
                if ( message.length() > 0 )
                {
                    getLogger().info( message ); // TODO
                }
            }

            public void info( String message )
            {
                getLogger().info( message );
            }
        } );

        for ( MavenProject otherProject : projects )
        {
            resolver.addMavenProject(
                otherProject.getBasedir(),
                otherProject.getPackaging(),
                otherProject.getGroupId(),
                otherProject.getArtifactId(),
                otherProject.getVersion() );
        }

        resolver.setLocalRepositoryLocation( new File( localRepository.getBasedir() ) );

        Properties properties = new Properties();
        TargetEnvironment environment = configuration.getEnvironment();
        properties.put( PlatformPropertiesUtils.OSGI_OS, environment.getOs() );
        properties.put( PlatformPropertiesUtils.OSGI_WS, environment.getWs() );
        properties.put( PlatformPropertiesUtils.OSGI_ARCH, environment.getArch() );
        ExecutionEnvironmentUtils.loadVMProfile( properties );

        properties.put( "org.eclipse.update.install.features", "true" );
        resolver.setProperties( properties );

        if ( dependencies != null )
        {
            for ( Dependency dependency : dependencies )
            {
                resolver.addDependency( dependency.getType(), dependency.getArtifactId(), dependency.getVersion() );
            }
        }

        P2ResolutionResult result;

        for ( ArtifactRepository repository : project.getRemoteArtifactRepositories() )
        {
            try
            {
                URI uri = new URL( repository.getUrl() ).toURI();

                if ( repository.getLayout() instanceof P2ArtifactRepositoryLayout )
                {
                    resolver.addP2Repository( uri );
                }
                else
                {
                    try
                    {
                        MavenRepositoryReader reader = plexus.lookup( MavenRepositoryReader.class );
                        reader.setArtifactRepository( repository );
                        reader.setLocalRepository( localRepository );
    
                        TychoRepositoryIndex index = new DefaultTychoRepositoryIndex( reader );
    
                        resolver.addMavenRepository( uri, index, reader );
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
                try
                {
                    URI uri = new URI( location.getRepositoryLocation() );
                    if ( uris.add( uri ) )
                    {
                        resolver.addP2Repository( uri );
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

        result = resolver.resolveProject( project.getBasedir() );

        DefaultTargetPlatform platform = createPlatform();

        platform.addSite( new File( localRepository.getBasedir() ) );

        for ( File bundle : result.getBundles() )
        {
            platform.addArtifactFile( ProjectType.OSGI_BUNDLE, bundle );
        }

        for ( File feature : result.getFeatures() )
        {
            platform.addArtifactFile( ProjectType.ECLIPSE_FEATURE, feature );
        }

        addProjects( platform );

        return platform;
    }

    public void initialize()
        throws InitializationException
    {
        this.resolverFactory = equinox.getService( P2ResolverFactory.class );
    }
}
