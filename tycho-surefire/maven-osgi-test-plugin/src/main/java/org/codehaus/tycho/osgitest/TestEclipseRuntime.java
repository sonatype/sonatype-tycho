package org.codehaus.tycho.osgitest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.tycho.ArtifactDescription;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.osgitools.BundleReader;
import org.codehaus.tycho.osgitools.targetplatform.DefaultTargetPlatform;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Constants;

public class TestEclipseRuntime
    extends AbstractLogEnabled
{
    private DefaultTargetPlatform bundles = new DefaultTargetPlatform();

    private Properties properties = new Properties();

    private List<File> frameworkExtensions = new ArrayList<File>();

    private static final Map<String, BundleStartLevel> DEFAULT_START_LEVEL = new HashMap<String, BundleStartLevel>();

    private File location;

    private List<String> bundlesToExplode;

    private Map<String, BundleStartLevel> startLevel = new HashMap<String, BundleStartLevel>( DEFAULT_START_LEVEL );

    private PlexusContainer plexus;

    private BundleReader manifestReader;

    static
    {
        setDefaultStartLevel( "org.eclipse.equinox.common", 2 );
        setDefaultStartLevel( "org.eclipse.core.runtime", 4 );
        setDefaultStartLevel( "org.eclipse.equinox.simpleconfigurator", 1 );
        setDefaultStartLevel( "org.eclipse.update.configurator", 3 );
        setDefaultStartLevel( "org.eclipse.osgi", -1 );
        setDefaultStartLevel( "org.eclipse.equinox.ds", 1 );
    }

    private static void setDefaultStartLevel( String id, int level )
    {
        DEFAULT_START_LEVEL.put( id, new BundleStartLevel( id, level, true ) );
    }

    public Properties getProperties()
    {
        return properties;
    }

    public String getProperty( String key )
    {
        return properties.getProperty( key );
    }

    private String toOsgiBundles( Map<ArtifactKey, File> bundles )
        throws IOException
    {
        StringBuilder result = new StringBuilder();
        for ( Map.Entry<ArtifactKey, File> entry : bundles.entrySet() )
        {
            BundleStartLevel level = startLevel.get( entry.getKey().getId() );
            if ( level != null && level.getLevel() == -1 )
            {
                continue; // system bundle
            }
            if ( result.length() > 0 )
            {
                result.append( "," );
            }
            result.append( appendAbsolutePath( entry.getValue() ) );
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

    public void create()
    {
        Map<ArtifactKey, File> effective = new LinkedHashMap<ArtifactKey, File>();

        for (ArtifactDescription artifact : bundles.getArtifacts( TychoProject.ECLIPSE_PLUGIN ) )
        {
            ArtifactKey key = artifact.getKey();
            File file = artifact.getLocation();
            Manifest mf = manifestReader.loadManifest( file );

            boolean directoryShape = bundlesToExplode.contains( key.getId() ) || manifestReader.isDirectoryShape( mf );

            if ( !file.isDirectory() && directoryShape )
            {
                String filename = key.getId() + "_" + key.getVersion();
                File unpacked = new File( location, "plugins/" + filename );

                unpacked.mkdirs();

                unpack( file, unpacked );

                effective.put( key, unpacked );
            }
            else
            {
                effective.put( key, file );
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
                newOsgiBundles = toOsgiBundles( effective );
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
                ArtifactDescription desc = getBundle( url, null );
                if ( desc != null )
                {
                    url = "file:" + desc.getLocation().getAbsolutePath().replace( '\\', '/' );
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

    public void setLocation( File location )
    {
        this.location = location;
    }

    public ArtifactDescription getBundle( String symbolicName, String highestVersion )
    {
        return bundles.getArtifact( TychoProject.ECLIPSE_PLUGIN, symbolicName, highestVersion );
    }

    public ArtifactDescription getSystemBundle()
    {
        return bundles.getArtifact( TychoProject.ECLIPSE_PLUGIN, FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, null );
    }

    public void setPlexusContainer( PlexusContainer plexus )
    {
        this.plexus = plexus;
        try
        {
            this.manifestReader = plexus.lookup( BundleReader.class );
        }
        catch ( ComponentLookupException e )
        {
            throw new IllegalStateException( "Could not lookup required component", e );
        }
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
        ArtifactDescription bundle = getSystemBundle();
        File srcFile = bundle.getLocation();
        File dstFile = new File( location, "plugins/" + srcFile.getName() );
        FileUtils.copyFileIfModified( srcFile, dstFile );

        return "file:" + dstFile.getAbsolutePath().replace( '\\', '/' );
    }

    public void addBundle( File file, boolean override )
    {
        Manifest mf = manifestReader.loadManifest( file );

        ManifestElement[] id = manifestReader.parseHeader( Constants.BUNDLE_SYMBOLICNAME, mf );
        ManifestElement[] version = manifestReader.parseHeader( Constants.BUNDLE_VERSION, mf );

        if ( id == null || version == null )
        {
            throw new IllegalArgumentException( "Not a bundle " + file.getAbsolutePath() );
        }

        if ( override )
        {
            bundles.removeAll( TychoProject.ECLIPSE_PLUGIN, id[0].getValue() );
        }

        bundles.addArtifactFile( new ArtifactKey( TychoProject.ECLIPSE_PLUGIN, id[0].getValue(), version[0].getValue() ), file );
    }

    public void addBundle( ArtifactDescription artifact )
    {
        bundles.addArtifact( artifact );
    }

}
