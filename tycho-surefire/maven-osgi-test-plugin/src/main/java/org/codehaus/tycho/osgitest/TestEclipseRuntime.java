package org.codehaus.tycho.osgitest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.Platform;
import org.codehaus.tycho.osgitools.BundleManifestReader;
import org.codehaus.tycho.osgitools.EquinoxBundleResolutionState;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class TestEclipseRuntime
    extends AbstractLogEnabled
{

    private Set<File> sites = new LinkedHashSet<File>();

    private ArrayList<File> bundles;

    private Properties properties = new Properties();

    private TargetPlatform sourcePlatform;

    private EquinoxBundleResolutionState resolver;

    private List<File> frameworkExtensions = new ArrayList<File>();

    private static final Map<String, BundleStartLevel> DEFAULT_START_LEVEL = new HashMap<String, BundleStartLevel>();

    private File location;

    private List<String> bundlesToExplode;

    private Map<String, BundleStartLevel> startLevel = new HashMap<String, BundleStartLevel>( DEFAULT_START_LEVEL );

    private PlexusContainer plexus;

    static
    {
        setDefaultStartLevel( "org.eclipse.equinox.common", 2 );
        setDefaultStartLevel( "org.eclipse.core.runtime", 4 );
        setDefaultStartLevel( "org.eclipse.equinox.simpleconfigurator", 1 );
        setDefaultStartLevel( "org.eclipse.update.configurator", 3 );
        setDefaultStartLevel( "org.eclipse.osgi", -1 );
        setDefaultStartLevel( "org.eclipse.equinox.ds", 1 );
    }

    public void initialize()
    {
        this.bundles = new ArrayList<File>();
        bundles.addAll( sourcePlatform.getArtifactFiles( TychoProject.ECLIPSE_PLUGIN, TychoProject.ECLIPSE_TEST_PLUGIN ) );

        sites.addAll( sourcePlatform.getSites() );
    }

    private static void setDefaultStartLevel( String id, int level )
    {
        DEFAULT_START_LEVEL.put( id, new BundleStartLevel( id, level, true ) );
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
            if ( TychoProject.ECLIPSE_PLUGIN.equals( type ) || TychoProject.ECLIPSE_TEST_PLUGIN.equals( type ) )
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

            BundleStartLevel level = startLevel.get( bundle.getSymbolicName() );
            if ( level != null )
            {
                sb.append( level.getLevel() ).append( ',' ); // start level
                sb.append( Boolean.toString( level.isAutoStart() ) ); // autostart
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
            BundleStartLevel level = startLevel.get( bundle.getSymbolicName() );
            if ( level != null && level.getLevel() == -1 )
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
                result.append( '@' ).append( level.getLevel() );
                if ( level.isAutoStart() )
                {
                    result.append( ":start" );
                }
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

    public void addBundle( File file )
    {
        bundles.add( file );
    }

    public void create()
    {
        BundleManifestReader manifestReader = resolver.getBundleManifestReader();

        for ( File file : bundles )
        {
            try
            {
                Manifest mf = manifestReader.loadManifest( file );

                ManifestElement[] id = manifestReader.parseHeader( Constants.BUNDLE_SYMBOLICNAME, mf );
                ManifestElement[] version = manifestReader.parseHeader( Constants.BUNDLE_VERSION, mf );

                if ( !file.isDirectory() && id != null && version != null
                    && bundlesToExplode.contains( id[0].getValue() ) )
                {
                    String filename = id[0].getValue() + "_" + version[0].getValue();
                    File unpacked = new File( location, "plugins/" + filename );

                    unpacked.mkdirs();

                    unpack( file, unpacked );

                    resolver.addBundle( unpacked, true );
                }
                else
                {
                    resolver.addBundle( file, true );
                }
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

            // if ( shouldUseP2() )
            // {
            // createBundlesInfoFile( location );
            // createPlatformXmlFile( location );
            // newOsgiBundles = "org.eclipse.equinox.simpleconfigurator@1:start";
            // }
            // else if ( shouldUseUpdateManager() )
            // {
            // createPlatformXmlFile( location );
            // newOsgiBundles =
            // "org.eclipse.equinox.common@2:start, org.eclipse.update.configurator@3:start, org.eclipse.core.runtime@start";
            // }
            // else
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

            if ( !frameworkExtensions.isEmpty() )
            {
                Collection<String> bundleNames = unpackFrameworkExtensions( frameworkExtensions );
                p.setProperty( "osgi.framework", copySystemBundle() );
                p.setProperty( "osgi.framework.extensions", StringUtils.join( bundleNames.iterator(), "," ) );
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

    public BundleDescription getBundle( String symbolicName, String highestVersion )
    {
        return resolver.getBundle( symbolicName, highestVersion );
    }

    public BundleDescription getSystemBundle()
    {
        return resolver.getSystemBundle();
    }

    public void setPlexusContainer( PlexusContainer plexus )
    {
        this.plexus = plexus;
        resolver = EquinoxBundleResolutionState.newInstance( plexus, new File( location, ".manifests" ) );
    }

    public void setBundlesToExplode( List<String> bundlesToExplode )
    {
        this.bundlesToExplode = bundlesToExplode;
    }

    public void addBundleStartLevel( BundleStartLevel level )
    {
        startLevel.put( level.getId(), level );
    }

    private void unpack( File source, File destination )
    {
        UnArchiver unzip;
        try
        {
            unzip = plexus.lookup( UnArchiver.class, "zip" );
        }
        catch ( ComponentLookupException e )
        {
            throw new RuntimeException( "Could not lookup required component", e );
        }
        destination.mkdirs();
        unzip.setSourceFile( source );
        unzip.setDestDirectory( destination );
        try
        {
            unzip.extract();
        }
        catch ( ArchiverException e )
        {
            throw new RuntimeException( "Unable to unpack jar " + source, e );
        }
    }

    public void addFrameworkExtensions( Collection<File> frameworkExtensions )
    {
        this.frameworkExtensions.addAll( frameworkExtensions );
    }

    private List<String> unpackFrameworkExtensions( Collection<File> frameworkExtensions )
        throws IOException
    {
        List<String> bundleNames = new ArrayList<String>();

        BundleManifestReader manifestReader = resolver.getBundleManifestReader();

        for ( File bundleFile : frameworkExtensions )
        {
            Manifest mf = manifestReader.loadManifest( bundleFile );
            ManifestElement[] id = manifestReader.parseHeader( Constants.BUNDLE_SYMBOLICNAME, mf );
            ManifestElement[] version = manifestReader.parseHeader( Constants.BUNDLE_VERSION, mf );

            if ( id == null || version == null )
            {
                throw new IOException( "Invalid OSGi manifest in bundle " + bundleFile );
            }

            bundleNames.add( id[0].getValue() );

            File bundleDir = new File( location, "plugins/" + id[0].getValue() + "_" + version[0].getValue() );
            if ( bundleFile.isFile() )
            {
                unpack( bundleFile, bundleDir );
            }
            else
            {
                FileUtils.copyDirectoryStructure( bundleFile, bundleDir );
            }
        }

        return bundleNames;
    }

    private String copySystemBundle()
        throws IOException
    {
        BundleDescription bundle = resolver.getSystemBundle();
        File srcFile = new File( bundle.getLocation() );
        File dstFile = new File( location, "plugins/" + srcFile.getName() );
        FileUtils.copyFileIfModified( srcFile, dstFile );

        return "file:" + dstFile.getAbsolutePath().replace( '\\', '/' );
    }

}
