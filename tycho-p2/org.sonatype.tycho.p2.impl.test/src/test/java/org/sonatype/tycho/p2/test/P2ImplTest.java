package org.sonatype.tycho.p2.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.Assert;

import org.junit.Test;
import org.sonatype.tycho.p2.P2Impl;
import org.sonatype.tycho.p2.facade.ItemMetadata;
import org.sonatype.tycho.p2.facade.P2Facade;
import org.sonatype.tycho.p2.facade.P2ResolutionRequest;
import org.sonatype.tycho.p2.facade.P2ResolutionResultCollector;
import org.sonatype.tycho.p2.facade.RepositoryContentLocator;

public class P2ImplTest
{
    @Test
    public void bundleUIXml()
        throws IOException
    {
        P2Facade impl = new P2Impl();

        ItemMetadata metadata = impl.getBundleMetadata( new File(
            "resources/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" ) );

        Assert.assertEquals( "com.sonatype.nexus.p2.its.bundle", metadata.getId() );
        Assert.assertEquals( "1.0.0", metadata.getVersion() );

        Assert.assertNotNull( metadata.getIUXml() );
        Assert.assertNotNull( metadata.getArtifactXml() );

        assertContent( new File( "resources/com.sonatype.nexus.p2.its.bundle_1.0.0.iuxml" ), metadata.getIUXml() );
        assertContent( new File( "resources/com.sonatype.nexus.p2.its.bundle_1.0.0.artifactxml" ), metadata
            .getArtifactXml() );
    }

    @Test
    public void featureUIXml()
        throws IOException
    {
        P2Facade impl = new P2Impl();

        ItemMetadata metadata = impl.getFeatureMetadata( new File(
            "resources/com.sonatype.nexus.p2.its.feature_1.0.0.jar" ) );

        Assert.assertEquals( "com.sonatype.nexus.p2.its.feature", metadata.getId() );
        Assert.assertEquals( "1.0.0", metadata.getVersion() );

        Assert.assertNotNull( metadata.getIUXml() );
        Assert.assertNotNull( metadata.getArtifactXml() );

        assertContent( new File( "resources/com.sonatype.nexus.p2.its.feature_1.0.0.iuxml" ), metadata.getIUXml() );
        assertContent( new File( "resources/com.sonatype.nexus.p2.its.feature_1.0.0.artifactxml" ), metadata
            .getArtifactXml() );
    }

    private void assertContent( File expectedFile, byte[] actualContent )
        throws IOException
    {
        String expected = readAndClose( new FileInputStream( expectedFile ) );
        String actual = readAndClose( new ByteArrayInputStream( actualContent ) );

        Assert.assertEquals( expected, actual );
    }

    private String readAndClose( InputStream is )
        throws IOException
    {
        try
        {
            BufferedReader reader = new BufferedReader( new InputStreamReader( is, "UTF8" ) );
            StringBuilder content = new StringBuilder();
            int ch;
            while ( ( ch = reader.read() ) != -1 )
            {
                content.append( (char) ch );
            }
            return content.toString();
        }
        finally
        {
            is.close();
        }
    }

    @Test
    public void metadata()
        throws Exception
    {
        P2Facade impl = new P2Impl();

        P2ResolutionRequest req = new P2ResolutionRequest( "foo", "1.0.0", "bar" );
        
        req.addRootInstallableUnit( "com.sonatype.nexus.p2.its.feature.feature.group", "1.0.0" );
        req.addTargetEnvironment( "os=linux,ws=gtk,arch=x86_64" );
        req.addRepository( new RepositoryContentLocator()
        {
            public InputStream getItemInputStream( String path )
                throws IOException
            {
                return new FileInputStream( new File( "resources", path ) );
            }

            public String getId()
            {
                // TODO Auto-generated method stub
                return null;
            }
        } );
        
        P2ResolutionResultCollector res = new P2ResolutionResultCollector(){
        
            public void setItemContent( String path, InputStream content, String mimeType )
                throws Exception
            {
                // TODO Auto-generated method stub
                
            }
        
            public void createLinkItem( String path, String targetRepositoryId, String targetPath )
                throws Exception
            {
                // TODO Auto-generated method stub
                
            }
        };

        impl.resolve( req, res );
    }
}
