package org.sonatype.tycho.osgi;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

@Component( role = EquinoxEmbedder.class )
public class DefaultEquinoxEmbedder
    extends AbstractLogEnabled
    implements EquinoxEmbedder
{
    private static final String SYSPROP_EQUINOX_RUNTIMELOCATION = "equinox-runtimeLocation";

    @Configuration( value = "${equinox-runtimeLocation}" )
    private File runtimeLocation;

    private BundleContext frameworkContext;

    @Requirement
    private EquinoxLocator equinoxLocator;

    private String[] nonFrameworkArgs;

    public synchronized void start()
        throws Exception
    {
        if ( frameworkContext != null )
        {
            return;
        }

        if ( "Eclipse".equals( System.getProperty( "org.osgi.framework.vendor" ) ) )
        {
            throw new IllegalStateException( "Nested Equinox instance is not supported" );
        }

        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=308949
        // restore TCCL to make sure equinox classloader does not leak into our clients
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try
        {
            doStart();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( tccl );
        }
    }

    protected void doStart()
        throws Exception
    {
        String p2RuntimeLocation = getRuntimeLocation().getAbsolutePath();

        System.setProperty( "osgi.framework.useSystemProperties", "false" ); //$NON-NLS-1$ //$NON-NLS-2$

        Map<String, String> properties = new HashMap<String, String>();
        properties.put( "osgi.install.area", p2RuntimeLocation );
        properties.put( "osgi.syspath", p2RuntimeLocation + "/plugins" );
        properties.put( "osgi.configuration.area", p2RuntimeLocation + "/configuration" );

        // this tells framework to use our classloader as parent, so it can see classes that we see
        properties.put( "osgi.parentClassloader", "fwk" );

        // properties.put( "eclipse.p2.data.area", dataArea.getAbsolutePath() );

        // debug
        // properties.put( "osgi.console", "" );
        // properties.put( "osgi.debug", "" );
        // properties.put( "eclipse.consoleLog", "true" );

        // TODO switch to org.eclipse.osgi.launch.Equinox
        // EclipseStarter is not helping here

        EclipseStarter.setInitialProperties( properties );

        EclipseStarter.startup( nonFrameworkArgs != null ? nonFrameworkArgs : new String[0], null );

        frameworkContext = EclipseStarter.getSystemBundleContext();

        PackageAdmin packageAdmin = null;
        ServiceReference packageAdminRef = frameworkContext.getServiceReference( PackageAdmin.class.getName() );
        if ( packageAdminRef != null )
        {
            packageAdmin = (PackageAdmin) frameworkContext.getService( packageAdminRef );
        }

        if ( packageAdmin == null )
        {
            throw new IllegalStateException( "Could not obtain PackageAdmin service" );
        }

        for ( Bundle bundle : frameworkContext.getBundles() )
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

        frameworkContext.ungetService( packageAdminRef );
    }

    public File getRuntimeLocation()
    {
        // first, check system property
        String locatition = System.getProperty( SYSPROP_EQUINOX_RUNTIMELOCATION );
        if ( locatition != null )
        {
            File file;
            try
            {
                file = new File( locatition ).getCanonicalFile();
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Unexpected IOException", e );
            }

            if ( !isValidP2RuntimeLocation( file ) )
            {
                throw new RuntimeException( "Cannot find P2 runtime at specified location " + runtimeLocation );
            }

            return file;
        }

        // second, check explicit component configuration
        if ( isValidP2RuntimeLocation( runtimeLocation ) )
        {
            return runtimeLocation;
        }

        if ( isValidP2RuntimeLocation( equinoxLocator.getRuntimeLocation() ) )
        {
            return equinoxLocator.getRuntimeLocation();
        }

        throw new RuntimeException( "Could not determine P2 runtime location" );
    }

    private static boolean isValidP2RuntimeLocation( File runtimeLocation )
    {
        return runtimeLocation != null && runtimeLocation.isDirectory();
    }

    public <T> T getService( Class<T> clazz )
    {
        try
        {
            start();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

        // TODO technically, we're leaking service references here
        ServiceReference serviceReference = frameworkContext.getServiceReference( clazz.getName() );

        if ( serviceReference == null )
        {
            throw new IllegalStateException( "Service is not registered " + clazz );
        }

        return clazz.cast( frameworkContext.getService( serviceReference ) );
    }

    public void setNonFrameworkArgs( String[] args )
    {
        nonFrameworkArgs = args;
    }
}
