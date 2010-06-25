package org.sonatype.tycho.plugins.p2;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.maven.TychoP2RuntimeLocator;
import org.codehaus.tycho.model.Target;
import org.codehaus.tycho.model.Target.Location;
import org.codehaus.tycho.model.Target.Repository;
import org.codehaus.tycho.model.Target.Unit;
import org.codehaus.tycho.utils.ExecutionEnvironmentUtils;
import org.codehaus.tycho.utils.PlatformPropertiesUtils;
import org.sonatype.tycho.osgi.EquinoxEmbedder;
import org.sonatype.tycho.osgi.EquinoxLocator;
import org.sonatype.tycho.p2.facade.internal.P2Logger;
import org.sonatype.tycho.p2.facade.internal.P2RepositoryCache;
import org.sonatype.tycho.p2.facade.internal.P2ResolutionResult;
import org.sonatype.tycho.p2.facade.internal.P2Resolver;
import org.sonatype.tycho.p2.facade.internal.P2ResolverFactory;

/**
 * Quick&dirty way to update .target file to use latest versions of IUs available from specified metadata repositories.
 * 
 * @goal update-target
 */
public class UpdateTargetMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${target}"
     */
    private File targetFile;

    /**
     * @parameter expression="${session}"
     */
    private MavenSession session;

    /** @component */
    private TychoP2RuntimeLocator p2runtime;

    /** @component */
    private EquinoxLocator equinoxLocator;

    /** @component */
    private EquinoxEmbedder equinox;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            File p2Directory = p2runtime.locateTychoP2Runtime( session );
            if ( p2Directory != null )
            {
                equinoxLocator.setRuntimeLocation( p2Directory );
                getLog().debug( "Using P2 runtime at " + p2Directory );
            }

            Target target = Target.read( targetFile );

            P2ResolverFactory factory = equinox.getService( P2ResolverFactory.class );
            P2Resolver p2 = factory.createResolver();
            p2.setRepositoryCache( new P2RepositoryCache() );
            p2.setLocalRepositoryLocation( new File( session.getLocalRepository().getBasedir() ) );
            p2.setLogger( new P2Logger()
            {
                public void debug( String message )
                {
                    if ( message != null && message.length() > 0 )
                    {
                        getLog().info( message ); // TODO
                    }
                }

                public void info( String message )
                {
                    if ( message != null && message.length() > 0 )
                    {
                        getLog().info( message );
                    }
                }

                public boolean isDebugEnabled()
                {
                    return getLog().isDebugEnabled();
                }
            } );

            for ( Location location : target.getLocations() )
            {
                for ( Repository repository : location.getRepositories() )
                {
                    URI uri = new URI( repository.getLocation() );
                    p2.addP2Repository( uri );
                }

                for ( Unit unit : location.getUnits() )
                {
                    p2.addDependency( P2Resolver.TYPE_INSTALLABLE_UNIT, unit.getId(), "0.0.0" );
                }
            }

            P2ResolutionResult result = p2.resolveMetadata( getEnvironments().get( 0 ) );

            Map<String, String> ius = new HashMap<String, String>();
            for ( ArtifactKey key : result.getArtifacts().keySet() )
            {
                ius.put( key.getId(), key.getVersion() );
            }

            for ( Location location : target.getLocations() )
            {
                for ( Unit unit : location.getUnits() )
                {
                    String version = ius.get( unit.getId() );
                    if ( version != null )
                    {
                        unit.setVersion( version );
                    }
                    else
                    {
                        getLog().error( "Resolution result does not contain root installable unit " + unit.getId() );
                    }
                }
            }

            Target.write( target, targetFile );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Could not update " + targetFile.getAbsolutePath(), e );
        }
    }

    private List<Map<String, String>> getEnvironments()
    {
        Properties properties = new Properties();
        properties.put( PlatformPropertiesUtils.OSGI_OS, PlatformPropertiesUtils.getOS( properties ) );
        properties.put( PlatformPropertiesUtils.OSGI_WS, PlatformPropertiesUtils.getWS( properties ) );
        properties.put( PlatformPropertiesUtils.OSGI_ARCH, PlatformPropertiesUtils.getArch( properties ) );
        ExecutionEnvironmentUtils.loadVMProfile( properties );

        // TODO does not belong here
        properties.put( "org.eclipse.update.install.features", "true" );

        Map<String, String> map = new LinkedHashMap<String, String>();
        for ( Object key : properties.keySet() )
        {
            map.put( key.toString(), properties.getProperty( key.toString() ) );
        }

        ArrayList<Map<String, String>> result = new ArrayList<Map<String, String>>();
        result.add( map );
        return result;
    }

}
