package org.sonatype.tycho.versions.bundle.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.versions.bundle.ManifestAttribute;
import org.sonatype.tycho.versions.bundle.MutableBundleManifest;
import org.sonatype.tycho.versions.pom.tests.MutablePomFileTest;

public class MutableBundleManifestTest
{
    @Test
    public void echo()
        throws Exception
    {
        assertRoundtrip( "/manifests/m01.mf" );
        assertRoundtrip( "/manifests/m02.mf" );
        assertRoundtrip( "/manifests/m03.mf" );
    }

    @Test
    public void getters()
        throws IOException
    {
        MutableBundleManifest mf = getManifest( "/manifests/getters.mf" );

        Assert.assertEquals( "1.0.0.qualifier", mf.getVersion() );
        Assert.assertEquals( "TYCHO0214versionChange.bundle01", mf.getSymbolicName() );
    }

    @Test
    public void setVersion()
        throws Exception
    {
        MutableBundleManifest mf = getManifest( "/manifests/setVersion.mf" );

        mf.setVersion( "1.0.1.qualifier" );
        assertContents( mf, "/manifests/setVersion.mf_expected" );

        mf.setVersion( "1.0.1.qualifierrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr" );
        assertContents( mf, "/manifests/setVersion.mf_expected72" );
    }
    
    @Test
    public void addAttribute()
    	throws Exception
    {
    	MutableBundleManifest mf = getManifest( "/manifests/addheader.mf" );
    	mf.add(new ManifestAttribute("header", "value"));
    	assertContents(mf, "/manifests/addheader.mf_expected");
    }
    
    @Test
    public void shouldRoundtripWithoutLineEnding()
        throws Exception
    {
        // given
        String manifestStr = "Bundle-SymbolicName: name";
        
        // when
        InputStream manifestIs = new ByteArrayInputStream( manifestStr.getBytes("ascii") );
        MutableBundleManifest manifest = MutableBundleManifest.read( manifestIs );
        String written = toAsciiString( manifest );
        
        // then
        Assert.assertEquals(manifestStr, written);
        Assert.assertEquals("name", manifest.getSymbolicName());
    }

    @Test
    public void shouldPreserveWindowsLineEndings()
        throws Exception
    {
        // given
        String manifestStr = "Bundle-SymbolicName: name\r\nBundle-Version: version\r\n\r\nUnparsed1\r\nUnparsed2\r\n";
        
        // when
        InputStream manifestIs = new ByteArrayInputStream( manifestStr.getBytes("ascii") );
        MutableBundleManifest manifest = MutableBundleManifest.read( manifestIs );
        String written = toAsciiString( manifest );
        
        // then
        Assert.assertEquals(manifestStr, written);
        Assert.assertEquals("name", manifest.getSymbolicName());
        Assert.assertEquals("version", manifest.getVersion());
    }

    @Test
    public void shouldPreserveUnixLineEndings()
        throws Exception
    {
        // given
        String manifestStr = "Bundle-SymbolicName: name\nBundle-Version: version\n\nUnparsed1\nUnparsed2\n";
        
        // when
        InputStream manifestIs = new ByteArrayInputStream( manifestStr.getBytes("ascii") );
        MutableBundleManifest manifest = MutableBundleManifest.read( manifestIs );
        String written = toAsciiString( manifest );
        
        // then
        Assert.assertEquals(manifestStr, written);
        Assert.assertEquals("name", manifest.getSymbolicName());
        Assert.assertEquals("version", manifest.getVersion());
    }

    @Test
    public void shouldPreserveOldMacLineEndings()
        throws Exception
    {
        // given
        String manifestStr = "Bundle-SymbolicName: name\rBundle-Version: version\r\rUnparsed1\rUnparsed2\r";
        
        // when
        InputStream manifestIs = new ByteArrayInputStream( manifestStr.getBytes("ascii") );
        MutableBundleManifest manifest = MutableBundleManifest.read( manifestIs );
        String written = toAsciiString( manifest );
        
        // then
        Assert.assertEquals(manifestStr, written);
        Assert.assertEquals("name", manifest.getSymbolicName());
        Assert.assertEquals("version", manifest.getVersion());
    }

    private void assertRoundtrip( String path )
        throws IOException
    {
        MutableBundleManifest mf = getManifest( path );

        assertContents( mf, path );
    }

    private void assertContents( MutableBundleManifest mf, String path )
        throws UnsupportedEncodingException, IOException
    {
        Assert.assertEquals( toAsciiString( toByteArray( path ) ), toAsciiString( mf ) );
    }

    private String toAsciiString( MutableBundleManifest mf )
        throws IOException, UnsupportedEncodingException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        MutableBundleManifest.write( mf, buf );

        String actual = toAsciiString( buf.toByteArray() );
        return actual;
    }

    private MutableBundleManifest getManifest( String path )
        throws IOException
    {
        return MutableBundleManifest.read( getClass().getResourceAsStream( path ) );
    }

    private static byte[] toByteArray( String path )
        throws IOException
    {
        byte expected[];
        InputStream is = MutablePomFileTest.class.getResourceAsStream( path );
        try
        {
            expected = IOUtil.toByteArray( is );
        }
        finally
        {
            IOUtil.close( is );
        }
        return expected;
    }

    private static String toAsciiString( byte[] bytes )
        throws UnsupportedEncodingException
    {
    	return new String(bytes, "ascii");
    }

}
