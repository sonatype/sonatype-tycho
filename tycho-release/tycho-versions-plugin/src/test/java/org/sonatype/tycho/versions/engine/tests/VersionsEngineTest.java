package org.sonatype.tycho.versions.engine.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.tycho.testing.TestUtil;
import org.sonatype.tycho.versions.engine.VersionsEngine;

public class VersionsEngineTest
    extends PlexusTestCase
{
    public void testSimple()
        throws Exception
    {
        File basedir = TestUtil.getBasedir( "projects/simple" );

        VersionsEngine engine = lookup( VersionsEngine.class );
        engine.addBasedir( basedir );
        engine.addVersionChange( "simple", "1.0.1.qualifier" );
        engine.apply();

        assertPom( basedir );
        assertBundleManifest( basedir );
    }

    public void testMultimodule()
        throws Exception
    {
        File basedir = TestUtil.getBasedir( "projects/multimodule" );

        VersionsEngine engine = lookup( VersionsEngine.class );
        engine.addBasedir( basedir );
        engine.addVersionChange( "parent", "1.0.1.qualifier" );
        engine.apply();

        assertPom( basedir );

        assertPom( new File( basedir, "bundle" ) );
        assertBundleManifest( new File( basedir, "bundle" ) );

        assertPom( new File( basedir, "feature01" ) );
        assertFeatureXml( new File( basedir, "feature01" ) );

        assertPom( new File( basedir, "feature02" ) );
        assertFeatureXml( new File( basedir, "feature02" ) );

        assertPom( new File( basedir, "site" ) );
        assertSiteXml( new File( basedir, "site" ) );

        assertPom( new File( basedir, "product" ) );
        assertProductFile( new File( basedir, "product" ), "product.product" );
    }

    public void testProfile()
        throws Exception
    {
        File basedir = TestUtil.getBasedir( "projects/profile" );

        VersionsEngine engine = lookup( VersionsEngine.class );
        engine.addBasedir( basedir );
        engine.addVersionChange( "parent", "1.0.1.qualifier" );
        engine.apply();

        assertPom( basedir );

        assertPom( new File( basedir, "bundle01" ) );
        assertBundleManifest( new File( basedir, "bundle01" ) );

        assertPom( new File( basedir, "bundle02" ) );
        assertBundleManifest( new File( basedir, "bundle02" ) );
    }

    public void testAggregator()
        throws Exception
    {
        File basedir = TestUtil.getBasedir( "projects/aggregator" );

        VersionsEngine engine = lookup( VersionsEngine.class );
        engine.addBasedir( basedir );
        engine.addVersionChange( "aggregator", "1.0.1.qualifier" );
        engine.addVersionChange( "parent", "1.0.1.qualifier" );
        engine.apply();

        assertPom( basedir );

        assertPom( new File( basedir, "parent" ) );

        assertPom( new File( basedir, "bundle" ) );
        assertBundleManifest( new File( basedir, "bundle" ) );

        assertPom( new File( basedir, "detached" ) );
        assertBundleManifest( new File( basedir, "detached" ) );
    }

    public void testDeepNesting()
        throws Exception
    {
        File basedir = TestUtil.getBasedir( "projects/deepnesting" );

        VersionsEngine engine = lookup( VersionsEngine.class );
        engine.addBasedir( basedir );
        engine.addVersionChange( "parent", "1.0.1.qualifier" );
        engine.apply();

        assertPom( basedir );

        assertPom( new File( basedir, "child" ) );

        assertPom( new File( basedir, "child/grandchild" ) );

        assertPom( new File( basedir, "child/grandchild/bundle" ) );
        assertBundleManifest( new File( basedir, "child/grandchild/bundle" ) );
    }

    public void testDeepNestingInverseOrder()
        throws Exception
    {
        File basedir = TestUtil.getBasedir( "projects/deepnesting" );

        VersionsEngine engine = lookup( VersionsEngine.class );
        engine.addBasedir( basedir );
        engine.addVersionChange( "child", "1.0.1.qualifier" );
        engine.addVersionChange( "parent", "1.0.1.qualifier" );
        engine.apply();

        assertPom( basedir );

        assertPom( new File( basedir, "child" ) );

        assertPom( new File( basedir, "child/grandchild" ) );

        assertPom( new File( basedir, "child/grandchild/bundle" ) );
        assertBundleManifest( new File( basedir, "child/grandchild/bundle" ) );
    }

    public void testExplicitVersion()
        throws Exception
    {
        File basedir = TestUtil.getBasedir( "projects/exlicitversion" );

        VersionsEngine engine = lookup( VersionsEngine.class );
        engine.addBasedir( basedir );
        engine.addVersionChange( "parent", "1.0.1.qualifier" );
        engine.apply();

        assertPom( basedir );

        assertPom( new File( basedir, "otherversion" ) );
        assertBundleManifest( new File( basedir, "otherversion" ) );

        assertPom( new File( basedir, "sameversion" ) );
        assertBundleManifest( new File( basedir, "sameversion" ) );
    }

    private void assertPom( File basedir )
        throws IOException
    {
        assertFileContent( new File( basedir, "pom.xml" ) );
    }

    private void assertBundleManifest( File basedir )
        throws IOException
    {
        assertFileContent( new File( basedir, "META-INF/MANIFEST.MF" ) );
    }

    private void assertFeatureXml( File basedir )
        throws IOException
    {
        assertFileContent( new File( basedir, "feature.xml" ) );
    }

    private void assertSiteXml( File basedir )
        throws IOException
    {
        assertFileContent( new File( basedir, "site.xml" ) );
    }

    private void assertProductFile( File basedir, String name )
        throws IOException
    {
        assertFileContent( new File( basedir, name ) );
    }

    private void assertFileContent( File actual )
        throws IOException
    {
        File expected = new File( actual.getParentFile(), actual.getName() + "_expected" );
        assertEquals( toAsciiString( expected ), toAsciiString( actual ) );
    }

    private String toAsciiString( File file )
        throws IOException
    {
        BufferedReader r = new BufferedReader( new InputStreamReader( new FileInputStream( file ) ) );
        StringBuilder sb = new StringBuilder();
        String str;
        while ( ( str = r.readLine() ) != null )
        {
            sb.append( str ).append( '\n' );
        }
        return sb.toString();
    }
}
