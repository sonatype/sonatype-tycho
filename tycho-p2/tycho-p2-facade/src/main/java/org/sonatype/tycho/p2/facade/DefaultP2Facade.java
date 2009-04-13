package org.sonatype.tycho.p2.facade;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Startable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StartingException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StoppingException;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

@Component( role = P2Facade.class )
public class DefaultP2Facade
    extends AbstractLogEnabled
    implements P2Facade, Startable, Initializable
{

    private static final String SYSPROP_P2_RUNTIMELOCATION = "p2-runtimeLocation";

    private P2Facade impl;

    @Configuration( value = "${p2-runtimeLocation}" )
    private File runtimeLocation;

    @Configuration( value = "${nexus-work}/p2" )
    private File dataArea;

    public ItemMetadata getBundleMetadata( File file )
    {
        try
        {
            return getImpl().getBundleMetadata( file );
        }
        catch ( Exception e )
        {
            getLogger().error( "Could not get IU XML", e );
        }

        return null;
    }

    public ItemMetadata getFeatureMetadata( File file )
    {
        try
        {
            return getImpl().getFeatureMetadata( file );
        }
        catch ( Exception e )
        {
            getLogger().error( "Could not get IU XML", e );
        }

        return null;
    }

    private synchronized P2Facade getImpl()
        throws Exception
    {
        if ( impl == null )
        {
            if ( runtimeLocation == null || !runtimeLocation.exists() || !runtimeLocation.isDirectory() )
            {
                throw new IllegalArgumentException( "Cannot find P2 runtime at specified location " + runtimeLocation );
            }

            String p2RuntimeLocation = runtimeLocation.getAbsolutePath();

            System.setProperty( "osgi.framework.useSystemProperties", "false" ); //$NON-NLS-1$ //$NON-NLS-2$

            Map<String, String> properties = new HashMap<String, String>();
            properties.put( "osgi.install.area", p2RuntimeLocation );
            properties.put( "osgi.syspath", p2RuntimeLocation + "/plugins" );
            properties.put( "osgi.configuration.area", p2RuntimeLocation + "/configuration" );

            // this tells framework to use our classloader as parent, so it can see classes that we see
            properties.put( "osgi.parentClassloader", "fwk" );

            properties.put( "eclipse.p2.data.area", dataArea.getAbsolutePath() );

            // debug
            properties.put( "osgi.console", "" );
            properties.put( "osgi.debug", "" );
            properties.put( "eclipse.consoleLog", "true" );

            // TODO switch to org.eclipse.osgi.launch.Equinox
            // EclipseStarter is not helping here

            EclipseStarter.setInitialProperties( properties );
            EclipseStarter.startup( new String[0], null );

            BundleContext bundleContext = EclipseStarter.getSystemBundleContext();

            PackageAdmin packageAdmin = null;
            ServiceReference packageAdminRef = bundleContext.getServiceReference( PackageAdmin.class.getName() );
            if ( packageAdminRef != null )
            {
                packageAdmin = (PackageAdmin) bundleContext.getService( packageAdminRef );
            }

            if ( packageAdmin == null )
            {
                throw new IllegalStateException( "Could not obtain PackageAdmin service" );
            }

            for ( Bundle bundle : bundleContext.getBundles() )
            {
                if ( ( packageAdmin.getBundleType( bundle ) & PackageAdmin.BUNDLE_TYPE_FRAGMENT ) == 0 )
                {
                    try
                    {
                        bundle.start();
                    }
                    catch ( BundleException e )
                    {
                        getLogger().warn( "Could not start bundle " + bundle.getSymbolicName(), e );
                    }
                }
            }

            bundleContext.ungetService( packageAdminRef );

            ServiceReference serviceReference = bundleContext.getServiceReference( P2Facade.class.getName() );

            impl = (P2Facade) bundleContext.getService( serviceReference );
        }

        return impl;
    }

    public void start()
        throws StartingException
    {
        // do nothing
    }

    public void stop()
        throws StoppingException
    {
        // TODO Auto-generated method stub
        if ( impl != null )
        {
            try
            {
                EclipseStarter.shutdown();
            }
            catch ( Exception e )
            {
                throw new StoppingException( "Could not stop Equinox", e );
            }
        }
    }

    public void publish( File location, List<File> bundles, List<File> features )
    {
        try
        {
            getImpl().publish( location, bundles, features );
        }
        catch ( Exception e )
        {
            getLogger().warn( "Could not create local P2 repository metadata", e );
        }
    }

    public void resolve( P2ResolutionRequest req, P2ResolutionResultCollector result )
        throws Exception
    {
        try
        {
            getImpl().resolve( req, result );
        }
        catch ( Exception e )
        {
            getLogger().warn( "Could not resolve P2 dependencies", e );
        }
    }

    public void getRepositoryArtifacts( String url, File destination )
    {
        try
        {
            getImpl().getRepositoryArtifacts( url, destination );
        }
        catch ( Exception e )
        {
            getLogger().warn( "Could not retrieve P2 repository metadata", e );
        }
    }

    public void getRepositoryContent( String url, File destination )
    {
        try
        {
            getImpl().getRepositoryContent( url, destination );
        }
        catch ( Exception e )
        {
            getLogger().warn( "Could not retrieve P2 repository metadata", e );
        }
    }

    public String getP2RuntimeLocation()
    {
        return runtimeLocation.getAbsolutePath();
    }

    public void initialize()
        throws InitializationException
    {
        if ( !isValidP2RuntimeLocation( runtimeLocation ) )
        {
            String locatition = System.getProperty( SYSPROP_P2_RUNTIMELOCATION );

            if ( locatition == null )
            {
                throw new InitializationException( "Cannot find P2 runtime at specified location " + runtimeLocation );
            }

            File file;
            try
            {
                file = new File( locatition ).getCanonicalFile();
            }
            catch ( IOException e )
            {
                throw new InitializationException( "Unexpected IOException", e );
            }

            if ( !isValidP2RuntimeLocation( file ) )
            {
                throw new InitializationException( "Cannot find P2 runtime at specified location " + locatition );
            }

            runtimeLocation = file;
        }
    }

    private static boolean isValidP2RuntimeLocation( File runtimeLocation )
    {
        return runtimeLocation != null && runtimeLocation.exists() && runtimeLocation.isDirectory();
    }
}
