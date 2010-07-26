package org.codehaus.tycho.osgitools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import copy.org.eclipse.core.runtime.internal.adaptor.PluginConverterImpl;

@Component( role = BundleReader.class )
public class DefaultBundleReader
    extends AbstractLogEnabled
    implements BundleReader
{
    public static final String CACHE_PATH = ".cache/tycho";

    private File cacheDir;

    private static final Map<File, Manifest> manifestCache = new HashMap<File, Manifest>();

    public Manifest loadManifest( File bundleLocation )
    {
        Manifest manifest = manifestCache.get( bundleLocation );
        if ( manifest == null )
        {
            manifest = doLoadManifest( bundleLocation );
            manifestCache.put( bundleLocation, manifest );
        }
        return manifest;
    }

    private Manifest doLoadManifest( File bundleLocation )
    {
        try
        {
            if ( bundleLocation.isDirectory() )
            {
                File m = new File( bundleLocation, JarFile.MANIFEST_NAME );
                if ( m.canRead() )
                {
                    return loadManifestFile( m );
                }
                m = convertPluginManifest( bundleLocation );
                if ( m != null && m.canRead() )
                {
                    return loadManifestFile( m );
                }
                return null;
            }

            // it's a file, make sure we can read it
            if ( !bundleLocation.canRead() )
            {
                return null;
            }

            // file but not a jar, assume it is MANIFEST.MF
            if ( !bundleLocation.getName().toLowerCase().endsWith( ".jar" ) )
            {
                return loadManifestFile( bundleLocation );
            }

            // it is a jar, lets see if it has OSGi bundle manifest
            ZipFile jar = new ZipFile( bundleLocation, ZipFile.OPEN_READ );
            try
            {
                ZipEntry me = jar.getEntry( JarFile.MANIFEST_NAME );
                if ( me != null )
                {
                    InputStream is = jar.getInputStream( me );
                    try
                    {
                        Manifest mf = new Manifest( is );
                        if ( mf.getMainAttributes().getValue( Constants.BUNDLE_SYMBOLICNAME ) != null )
                        {
                            return mf;
                        }
                    }
                    finally
                    {
                        is.close();
                    }
                }
            }
            finally
            {
                jar.close();
            }

            // it is a jar, does not have OSGi bundle manifest, lets try plugin.xml/fragment.xml
            File m = convertPluginManifest( bundleLocation );
            if ( m != null && m.canRead() )
            {
                return loadManifestFile( m );
            }
        }
        catch ( IOException e )
        {
            getLogger().warn( "Exception reading bundle manifest", e );
        }
        catch ( PluginConversionException e )
        {
            getLogger().warn( "Exception reading bundle manifest: " + e.getMessage() );
        }

        // not a bundle
        return null;
    }

    public Manifest loadManifestFile( File m )
        throws IOException
    {
        if ( !m.canRead() )
        {
            return null;
        }
        InputStream is = new FileInputStream( m );
        try
        {
            return new Manifest( is );
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    private File convertPluginManifest( File bundleLocation )
        throws PluginConversionException
    {
        PluginConverterImpl converter = new PluginConverterImpl( null, null );
        String name = bundleLocation.getName();
        if ( name.endsWith( ".jar" ) )
        {
            name = name.substring( 0, name.length() - 4 );
        }
        File manifestFile = new File( cacheDir, name + "/META-INF/MANIFEST.MF" );
        manifestFile.getParentFile().mkdirs();
        converter.convertManifest( bundleLocation, manifestFile, false /* compatibility */, "3.2" /* target version */,
                                   true /* analyse jars to set export-package */, null /* devProperties */);
        if ( manifestFile.exists() )
        {
            return manifestFile;
        }
        return null;
    }

    public void setLocationRepository( File basedir )
    {
        this.cacheDir = new File( basedir, CACHE_PATH );
    }

    public Properties toProperties( Manifest mf )
    {
        Attributes attrs = mf.getMainAttributes();
        Iterator<?> iter = attrs.keySet().iterator();
        Properties result = new Properties();
        while ( iter.hasNext() )
        {
            Attributes.Name key = (Attributes.Name) iter.next();
            result.put( key.toString(), attrs.get( key ) );
        }
        return result;
    }

    public ManifestElement[] parseHeader( String header, Manifest mf )
    {
        String property = toProperties( mf ).getProperty( header );

        if ( property == null )
        {
            return null;
        }

        try
        {
            return ManifestElement.parseHeader( header, property );
        }
        catch ( BundleException e )
        {
            throw new RuntimeException( e );
        }
    }

    public boolean isDirectoryShape( Manifest mf )
    {
        ManifestElement[] elements = parseHeader( "Eclipse-BundleShape", mf );

        return elements != null && elements.length > 0 && "dir".equals( elements[0].getValue() );
    }

    public File getEntry( File bundleLocation, String path )
    {
        if ( bundleLocation.isDirectory() )
        {
            File file = new File( bundleLocation, path );
            return file.exists() ? file : null;
        }

        File file = new File( cacheDir, bundleLocation.getName() + "/" + path );

        try
        {
            ZipFile zip = new ZipFile( bundleLocation );
            try
            {
                ZipEntry ze = zip.getEntry( path );
                if ( ze != null )
                {
                    if ( ze.isDirectory() )
                    {
                        ZipUnArchiver zipUnArchiver = new ZipUnArchiver( bundleLocation );
                        try
                        {
                            zipUnArchiver.extract( path, new File( cacheDir, bundleLocation.getName() ) );
                        }
                        catch ( ArchiverException e )
                        {
                            throw new RuntimeException( e );
                        }
                    }
                    else
                    {
                        InputStream is = zip.getInputStream( ze );
                        FileUtils.copyStreamToFile( new RawInputStreamFacade( is ), file );
                    }
                    return file;
                }
                else
                {
                    getLogger().debug( "Bundle entry " + bundleLocation + "!/" + path + " does not exist" );
                }
            }
            finally
            {
                zip.close();
            }
        }
        catch ( IOException e )
        {
            getLogger().warn( "Could not read bundle entry " + bundleLocation + "!/" + path, e );
        }

        return null;
    }
}
