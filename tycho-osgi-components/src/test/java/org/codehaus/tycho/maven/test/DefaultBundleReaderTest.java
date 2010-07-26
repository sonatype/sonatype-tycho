package org.codehaus.tycho.maven.test;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.tycho.osgitools.DefaultBundleReader;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;

public class DefaultBundleReaderTest
    extends AbstractTychoMojoTestCase
{

    private File cacheDir;

    private DefaultBundleReader bundleReader;

    @Override
    protected void setUp()
        throws Exception
    {
        cacheDir = File.createTempFile( "cache", "" );
        cacheDir.delete();
        cacheDir.mkdirs();
        bundleReader = new DefaultBundleReader();
        bundleReader.setLocationRepository( cacheDir );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        FileUtils.deleteDirectory( cacheDir );
    }

    public void testExtractDirClasspathEntries()
        throws Exception
    {
        File bundleWithNestedDirClasspath =
            new File( getBasedir(), "src/test/resources/bundlereader/testNestedDirClasspath_1.0.0.201007261122.jar" );
        File libDirectory = bundleReader.getEntry( bundleWithNestedDirClasspath, "lib/" );
        assertTrue( "directory classpath entry lib/ not extracted", libDirectory.isDirectory() );
        assertTrue( new File( libDirectory, "log4j.properties" ).isFile() );
        assertTrue( new File( libDirectory, "subdir/test.txt" ).isFile() );
    }
}
