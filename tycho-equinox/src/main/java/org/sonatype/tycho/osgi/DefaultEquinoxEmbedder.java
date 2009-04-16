package org.sonatype.tycho.osgi;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

@Component( role = EquinoxEmbedder.class )
public class DefaultEquinoxEmbedder
    extends AbstractLogEnabled
    implements EquinoxEmbedder, Initializable
{
    private static final String SYSPROP_EQUINOX_RUNTIMELOCATION = "equinox-runtimeLocation";

    private static final String SYSPROP_MAVEN_HOME = "maven.home";

    private static final String SYSPROP_IGNORE_MISSING_RUNTIME = "tycho.equinoxIgnore";

    @Configuration( value = "${equinox-runtimeLocation}" )
    private File runtimeLocation;

    private BundleContext frameworkContext;

    public synchronized void start()
        throws Exception
    {
        if ( frameworkContext != null )
        {
            return;
        }

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

        // properties.put( "eclipse.p2.data.area", dataArea.getAbsolutePath() );

        // debug
        properties.put( "osgi.console", "" );
        properties.put( "osgi.debug", "" );
        properties.put( "eclipse.consoleLog", "true" );

        // TODO switch to org.eclipse.osgi.launch.Equinox
        // EclipseStarter is not helping here

        EclipseStarter.setInitialProperties( properties );
        EclipseStarter.startup( new String[0], null );

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

    public void initialize()
        throws InitializationException
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
                throw new InitializationException( "Unexpected IOException", e );
            }

            if ( !isValidP2RuntimeLocation( file ) )
            {
                throw new InitializationException( "Cannot find P2 runtime at specified location " + runtimeLocation );
            }

            runtimeLocation = file;

            return;
        }

        // second, check explicit component configuration
        if ( isValidP2RuntimeLocation( runtimeLocation ) )
        {
            return;
        }

        // lastly, try ${maven.home}/p2
        String mavenHome = System.getProperty( SYSPROP_MAVEN_HOME );
        if ( mavenHome != null )
        {
            File file;
            try
            {
                file = new File( mavenHome, "p2" ).getCanonicalFile();
            }
            catch ( IOException e )
            {
                throw new InitializationException( "Unexpected IOException", e );
            }

            if ( isValidP2RuntimeLocation( file ) )
            {
                runtimeLocation = file;

                return;
            }

        }

        if ( !Boolean.getBoolean( SYSPROP_IGNORE_MISSING_RUNTIME ) )
        {
            throw new InitializationException( "Cannot find P2 runtime at specified location " + runtimeLocation );
        }
    }

    private static boolean isValidP2RuntimeLocation( File runtimeLocation )
    {
        return runtimeLocation != null && runtimeLocation.exists() && runtimeLocation.isDirectory();
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

        return clazz.cast( frameworkContext.getService( serviceReference ) );
    }

    public File getRuntimeLocation()
    {
        return runtimeLocation;
    }
}
