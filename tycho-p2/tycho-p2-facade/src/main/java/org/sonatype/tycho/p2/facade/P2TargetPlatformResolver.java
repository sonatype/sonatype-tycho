package org.sonatype.tycho.p2.facade;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.tycho.DefaultTargetPlatform;
import org.codehaus.tycho.ExecutionEnvironmentUtils;
import org.codehaus.tycho.PlatformPropertiesUtils;
import org.codehaus.tycho.ProjectType;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.osgitools.targetplatform.AbstractTargetPlatformResolver;
import org.codehaus.tycho.p2.P2ArtifactRepositoryLayout;
import org.sonatype.tycho.osgi.EquinoxEmbedder;
import org.sonatype.tycho.p2.facade.internal.P2ResolutionResult;
import org.sonatype.tycho.p2.facade.internal.P2Resolver;
import org.sonatype.tycho.p2.facade.internal.P2ResolverFactory;

@Component( role = TargetPlatformResolver.class, hint = P2TargetPlatformResolver.ROLE_HINT, instantiationStrategy = "per-lookup" )
public class P2TargetPlatformResolver
    extends AbstractTargetPlatformResolver
    implements TargetPlatformResolver, Initializable
{

    public static final String ROLE_HINT = "p2";

    @Requirement
    private EquinoxEmbedder equinox;

    private P2ResolverFactory resolverFactory;

    public TargetPlatform resolvePlatform( MavenProject project, List<Dependency> dependencies )
    {
        P2Resolver resolver = resolverFactory.createResolver();

        for ( MavenProject otherProject : projects )
        {
            resolver.addMavenProject(
                otherProject.getBasedir(),
                otherProject.getPackaging(),
                otherProject.getGroupId(),
                otherProject.getArtifactId(),
                otherProject.getVersion() );
        }

        for ( ArtifactRepository repository : project.getRemoteArtifactRepositories() )
        {
            if ( repository.getLayout() instanceof P2ArtifactRepositoryLayout )
            {
                try
                {
                    resolver.addRepository( new URL( repository.getUrl() ).toURI() );
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
        }

        resolver.setLocalRepositoryLocation( new File( localRepository.getBasedir() ) );

        Properties properties = new Properties( this.properties );
        properties.put( PlatformPropertiesUtils.OSGI_OS, PlatformPropertiesUtils.getOS( this.properties ) );
        properties.put( PlatformPropertiesUtils.OSGI_WS, PlatformPropertiesUtils.getWS( this.properties ) );
        properties.put( PlatformPropertiesUtils.OSGI_ARCH, PlatformPropertiesUtils.getArch( this.properties ) );
        ExecutionEnvironmentUtils.loadVMProfile( properties );
        properties.put("org.eclipse.update.install.features", "true" );
        resolver.setProperties( properties );

        if ( dependencies != null )
        {
            for ( Dependency dependency : dependencies )
            {
                resolver.addDependency( dependency.getType(), dependency.getArtifactId(), dependency.getVersion() );
            }
        }

        P2ResolutionResult result = resolver.resolve( project.getBasedir() );

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
