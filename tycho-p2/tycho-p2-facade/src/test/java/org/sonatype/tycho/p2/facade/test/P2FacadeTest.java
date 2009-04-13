package org.sonatype.tycho.p2.facade.test;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.sonatype.tycho.p2.facade.ItemMetadata;
import org.sonatype.tycho.p2.facade.P2Facade;

public class P2FacadeTest
    extends PlexusTestCase
{

    public void test()
        throws Exception
    {
        P2Facade p2 = lookup( P2Facade.class );

        ItemMetadata metadata = p2.getBundleMetadata( new File(
            "src/test/resources/com.sonatype.nexus.p2.its.bundle_1.0.0.jar" ) );

        assertEquals( "com.sonatype.nexus.p2.its.bundle", metadata.getId() );
        assertEquals( "1.0.0", metadata.getVersion() );

        assertNotNull( Xpp3DomBuilder.build( new ByteArrayInputStream( metadata.getIUXml() ), P2Facade.ENCODING ) );
        assertNotNull( Xpp3DomBuilder.build( new ByteArrayInputStream( metadata.getArtifactXml() ), P2Facade.ENCODING ) );
    }

    public void _testSubclipse()
        throws Exception
    {
        P2Facade p2 = lookup( P2Facade.class );

        p2.getRepositoryContent( "http://subclipse.tigris.org/update_1.4.x", new File( "target/content.xml" ) );
    }

}
