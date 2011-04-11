package org.eclipse.tycho.resolver;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.TargetPlatformConfiguration;
import org.eclipse.tycho.TargetPlatformResolver;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.osgitools.targetplatform.LocalTargetPlatformResolver;

@Component( role = DefaultTargetPlatformResolverFactory.class )
public class DefaultTargetPlatformResolverFactory
{
    private static final String DEFAULT_RESOLVER_HINT = "p2";

    @Requirement
    private Logger logger;

    @Requirement
    private PlexusContainer container;

    public TargetPlatformResolver lookupPlatformResolver( MavenProject project )
    {
        Properties properties = (Properties) project.getContextValue( TychoConstants.CTX_MERGED_PROPERTIES );
        TargetPlatformConfiguration configuration =
            (TargetPlatformConfiguration) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION );

        String property = properties.getProperty( "tycho.targetPlatform" );
        TargetPlatformResolver resolver;
        if ( property != null )
        {
            logger.warn( "-Dtycho.targetPlatform is deprecated and WILL be removed in the next Tycho version." );

            File location = new File( property );
            if ( !location.exists() || !location.isDirectory() )
            {
                throw new RuntimeException( "Invalid target platform location: " + property );
            }

            try
            {
                resolver = container.lookup( TargetPlatformResolver.class, LocalTargetPlatformResolver.ROLE_HINT );
            }
            catch ( ComponentLookupException e )
            {
                throw new RuntimeException( "Could not instantiate required component", e );
            }

            try
            {
                ( (LocalTargetPlatformResolver) resolver ).setLocation( new File( property ) );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Could not create target platform", e );
            }

            return resolver;
        }

        String resolverRole = configuration.getTargetPlatformResolver();
        if ( resolverRole == null )
        {
            resolverRole = DEFAULT_RESOLVER_HINT;
        }

        try
        {
            resolver = container.lookup( TargetPlatformResolver.class, resolverRole );
        }
        catch ( ComponentLookupException e )
        {
            throw new RuntimeException( "Could not instantiate required component", e );
        }

        return resolver;
    }
}
