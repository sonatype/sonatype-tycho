package org.codehaus.tycho.osgitest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.model.Platform;
import org.codehaus.tycho.osgitools.EquinoxBundleResolutionState;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleException;
import org.sonatype.tycho.ProjectType;
import org.sonatype.tycho.TargetPlatform;

public class TestEclipseRuntime
    extends AbstractLogEnabled
    implements TargetPlatform
{

    private Set<File> sites = new LinkedHashSet<File>();

    private ArrayList<File> bundles;

    private Properties properties = new Properties();

    private TargetPlatform sourcePlatform;

    private EquinoxBundleResolutionState resolver;

    private PlexusContainer plexus;

    public void initialize()
    {
        this.bundles = new ArrayList<File>();
        bundles.addAll( sourcePlatform.getArtifactFiles( ProjectType.OSGI_BUNDLE, ProjectType.ECLIPSE_TEST_PLUGIN ) );

        sites.addAll( sourcePlatform.getSites() );
    }

    public List<File> getArtifactFiles( String... types )
    {
        if ( isBundle( types ) )
        {
            return bundles;
        }
        return sourcePlatform.getArtifactFiles( types );
    }

    private boolean isBundle( String[] types )
    {
        for ( String type : types )
        {
            if ( ProjectType.OSGI_BUNDLE.equals( type ) || ProjectType.ECLIPSE_TEST_PLUGIN.equals( type ) )
            {
                return true;
            }
        }
        return false;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public String getProperty( String key )
    {
        return properties.getProperty( key );
    }

    private boolean shouldUseP2()
    {
        return resolver.getBundle( "org.eclipse.equinox.simpleconfigurator", TychoConstants.HIGHEST_VERSION ) != null;
    }

    private boolean shouldUseUpdateManager()
    {
        return resolver.getBundle( "org.eclipse.update.configurator", TychoConstants.HIGHEST_VERSION ) != null;
    }

    private void createBundlesInfoFile( File target )
        throws IOException
    {
        StringBuilder sb = new StringBuilder();
        for ( BundleDescription bundle : resolver.getBundles() )
        {
            File location = new File( bundle.getLocation() );

            // TODO dirty hack -- compensate for .qualifier expansion
            Manifest manifest = resolver.loadManifest( location );
            Attributes attributes = manifest.getMainAttributes();
            String version = attributes.getValue( "Bundle-Version" );

            sb.append( bundle.getSymbolicName() ).append( ',' );
            sb.append( version ).append( ',' );
            sb.append( location.toURL().toExternalForm() ).append( ',' );

            Integer level = START_LEVEL.get( bundle.getSymbolicName() );
            if ( level != null )
            {
                sb.append( level ).append( ',' ); // start level
                sb.append( "true" ); // autostart
            }
            else
            {
                sb.append( "4" ).append( ',' ); // start level
                sb.append( "false" ); // autostart
            }
            sb.append( '\n' );
        }
        fileWrite( new File( target, TychoConstants.BUNDLES_INFO_PATH ), sb.toString() );
    }

    private void createPlatformXmlFile( File work )
        throws IOException
    {
        Platform platform = new Platform();

        Map<String, List<String>> sitePlugins = new LinkedHashMap<String, List<String>>();
        Map<String, List<Platform.Feature>> siteFeatures = new LinkedHashMap<String, List<Platform.Feature>>();

        for ( File bundle : bundles )
        {
            String siteUrl = getSiteUrl( bundle );
            if ( siteUrl == null )
            {
                throw new RuntimeException( "Can't determine site for bundle at " + bundle.getAbsolutePath() );
            }
            List<String> plugins = sitePlugins.get( siteUrl );
            if ( plugins == null )
            {
                plugins = new ArrayList<String>();
                sitePlugins.put( siteUrl, plugins );
            }
            plugins.add( getRelativeUrl( siteUrl, bundle ) );
        }

        Set<String> sites = new LinkedHashSet<String>();
        sites.addAll( sitePlugins.keySet() );
        sites.addAll( siteFeatures.keySet() );
        for ( String siteUrl : sites )
        {
            Platform.Site site = new Platform.Site( siteUrl );
            site.setPlugins( sitePlugins.get( siteUrl ) );
            site.setFeatures( siteFeatures.get( siteUrl ) );

            platform.addSite( site );
        }

        Platform.write( platform, new File( work, TychoConstants.PLATFORM_XML_PATH ) );
    }

    private String toOsgiBundles( List<BundleDescription> bundles )
        throws IOException
    {
        StringBuilder result = new StringBuilder();
        for ( BundleDescription bundle : bundles )
        {
            Integer level = START_LEVEL.get( bundle.getSymbolicName() );
            if ( level != null && level.intValue() == -1 )
            {
                continue; // system bundle
            }
            if ( result.length() > 0 )
            {
                result.append( "," );
            }
            File file = new File( bundle.getLocation() );
            result.append( appendAbsolutePath( file ) );
            if ( level != null )
            {
                result.append( '@' ).append( level ).append( ":start" );
            }
        }
        return result.toString();
    }

    private String appendAbsolutePath( File file )
        throws IOException
    {
        String url = file.getAbsolutePath().replace( '\\', '/' );
        return "reference:file:" + url;
    }

    private static void fileWrite( File file, String data )
        throws IOException
    {
        file.getParentFile().mkdirs();
        FileUtils.fileWrite( file.getAbsolutePath(), data );
    }

    private static final Map<String, Integer> START_LEVEL = new HashMap<String, Integer>();

    private File location;

    static
    {
        START_LEVEL.put( "org.eclipse.equinox.common", 2 );
        START_LEVEL.put( "org.eclipse.core.runtime", 4 );
        START_LEVEL.put( "org.eclipse.equinox.simpleconfigurator", 1 );
        START_LEVEL.put( "org.eclipse.update.configurator", 3 );
        START_LEVEL.put( "org.eclipse.osgi", -1 );
    }

    public void addBundle( File file )
    {
        bundles.add( file );
    }

    public void create()
    {
        try
        {
            resolver = (EquinoxBundleResolutionState) plexus.lookup( BundleResolutionState.class );
        }
        catch ( ComponentLookupException e )
        {
            throw new RuntimeException( "Could not instantiate required component", e );
        }

        for ( File file : bundles )
        {
            try
            {
                resolver.addBundle( file, true );
            }
            catch ( BundleException e )
            {
                getLogger().debug( "Exception resolving test runtime", e );
            }
        }

        try
        {
            Properties p = new Properties();

            String newOsgiBundles;

//            if ( shouldUseP2() )
//            {
//                createBundlesInfoFile( location );
//                createPlatformXmlFile( location );
//                newOsgiBundles = "org.eclipse.equinox.simpleconfigurator@1:start";
//            }
//            else if ( shouldUseUpdateManager() )
//            {
//                createPlatformXmlFile( location );
//                newOsgiBundles = "org.eclipse.equinox.common@2:start, org.eclipse.update.configurator@3:start, org.eclipse.core.runtime@start";
//            }
//            else
            /* use plain equinox */{
                newOsgiBundles = toOsgiBundles( resolver.getBundles() );
            }

            p.setProperty( "osgi.bundles", newOsgiBundles );

            // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=234069
            p.setProperty( "osgi.bundlefile.limit", "100" );

            // @see SimpleConfiguratorConstants#PROP_KEY_EXCLUSIVE_INSTALLATION
            // p.setProperty("org.eclipse.equinox.simpleconfigurator.exclusiveInstallation", "false");

            p.setProperty( "osgi.install.area", "file:" + location.getAbsolutePath().replace( '\\', '/' ) );
            p.setProperty( "osgi.configuration.cascaded", "false" );
            p.setProperty( "osgi.framework", "org.eclipse.osgi" );
            p.setProperty( "osgi.bundles.defaultStartLevel", "4" );

            // fix osgi.framework
            String url = p.getProperty( "osgi.framework" );
            if ( url != null )
            {
                File file;
                BundleDescription desc = resolver.getBundle( url, TychoConstants.HIGHEST_VERSION );
                if ( desc != null )
                {
                    url = "file:" + new File( desc.getLocation() ).getAbsolutePath().replace( '\\', '/' );
                }
                else if ( url.startsWith( "file:" ) )
                {
                    String path = url.substring( "file:".length() );
                    file = new File( path );
                    if ( !file.isAbsolute() )
                    {
                        file = new File( location, path );
                    }
                    url = "file:" + file.getAbsolutePath().replace( '\\', '/' );
                }
            }
            if ( url != null )
            {
                p.setProperty( "osgi.framework", url );
            }

            new File( location, "configuration" ).mkdir();
            FileOutputStream fos = new FileOutputStream( new File( location, TychoConstants.CONFIG_INI_PATH ) );
            try
            {
                p.store( fos, null );
            }
            finally
            {
                fos.close();
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Exception creating test eclipse runtime", e );
        }
    }

    public File getLocation()
    {
        return location;
    }

    public void setSourcePlatform( TargetPlatform sourcePlatform )
    {
        this.sourcePlatform = sourcePlatform;
    }

    public void setLocation( File location )
    {
        this.location = location;
    }

    public void setPlexusContainer( PlexusContainer plexus )
    {
        this.plexus = plexus;
    }

    private String getRelativeUrl( String siteUrl, File location )
    {
        String locationStr = toUrl( location );
        if ( !locationStr.startsWith( siteUrl ) )
        {
            throw new IllegalArgumentException();
        }
        return locationStr.substring( siteUrl.length() );
    }

    private String toUrl( File file )
    {
        try
        {
            return file.getCanonicalFile().toURL().toExternalForm();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Unexpected IOException", e );
        }
    }

    private String getSiteUrl( File location )
    {
        String locationStr = toUrl( location );
        for ( File site : sites )
        {
            String siteUrl = toUrl( site );
            if ( locationStr.startsWith( siteUrl ) )
            {
                return siteUrl;
            }
        }
        return null;
        // throw new RuntimeException("Can't determine site location for " + locationStr);
    }

    public List<File> getSites()
    {
        return new ArrayList<File>( sites );
    }

}
