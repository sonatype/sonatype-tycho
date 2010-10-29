package org.codehaus.tycho.maven.test;

import java.io.File;
import java.io.IOException;

import org.codehaus.tycho.osgitools.DefaultArtifactKey;
import org.codehaus.tycho.osgitools.targetplatform.DefaultTargetPlatform;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.ArtifactKey;

public class DefaultTargetPlatformTest
{
    @Test
    public void testVersionMatch()
    {
        String type = "foo";
        String id = "foo";

        DefaultTargetPlatform tp = new DefaultTargetPlatform();

        addArtifact( tp, type, id, "1.1.0" );
        addArtifact( tp, type, id, "1.2.3" );
        addArtifact( tp, type, id, "1.2.3.aaa" );
        addArtifact( tp, type, id, "1.2.3.bbb" );
        addArtifact( tp, type, id, "1.2.3.ccc" );
        addArtifact( tp, type, id, "1.2.3.qualifier" );
        addArtifact( tp, type, id, "1.2.3.zzz" );

        addArtifact( tp, type, id, "5.6.7.zzz" );

        // 0.0.0 or null match the latest version
        Assert.assertEquals( "5.6.7.zzz", tp.getArtifact( type, id, null ).getKey().getVersion() );
        Assert.assertEquals( "5.6.7.zzz", tp.getArtifact( type, id, "0.0.0" ).getKey().getVersion() );

        // 1.2.3 matches the latest qualifier
        Assert.assertEquals( "1.1.0", tp.getArtifact( type, id, "1.1.0" ).getKey().getVersion() );
        Assert.assertEquals( "1.2.3.zzz", tp.getArtifact( type, id, "1.2.3" ).getKey().getVersion() );

        // 1.2.3.qualifier matches the latest qualifier
        Assert.assertEquals( "1.1.0", tp.getArtifact( type, id, "1.1.0.qualifier" ).getKey().getVersion() );
        Assert.assertEquals( "1.2.3.zzz", tp.getArtifact( type, id, "1.2.3.qualifier" ).getKey().getVersion() );

        // anything else matches just that exact version
        Assert.assertEquals( "1.2.3.bbb", tp.getArtifact( type, id, "1.2.3.bbb" ).getKey().getVersion() );

        // does not match anything
        Assert.assertNull( tp.getArtifact( type, id, "0.0.0.qualifier" ) );
        Assert.assertNull( tp.getArtifact( type, id, "1.0.0" ) );
        Assert.assertNull( tp.getArtifact( type, id, "1.0.0.qualifier" ) );
        Assert.assertNull( tp.getArtifact( type, id, "1.2.0" ) );
        Assert.assertNull( tp.getArtifact( type, id, "1.2.0.qualifier" ) );
        Assert.assertNull( tp.getArtifact( type, id, "9.9.9" ) );
        Assert.assertNull( tp.getArtifact( type, id, "9.9.9.qualifier" ) );
    }

    private void addArtifact( DefaultTargetPlatform tp, String type, String id, String version )
    {
        ArtifactKey key = new DefaultArtifactKey( type, id, version );
        tp.addArtifactFile( key, new File( version ), null );
    }

    public void testRelativePath()
        throws IOException
    {
        DefaultTargetPlatform tp = new DefaultTargetPlatform();

        File relative = new File( "relative.xml" );
        File canonical = new File( "canonical.xml" );

        tp.addArtifactFile( new DefaultArtifactKey( "foo", "relative", "1" ), relative, null );
        tp.addArtifactFile( new DefaultArtifactKey( "foo", "canonical", "1" ), canonical.getCanonicalFile(), null );

        Assert.assertNotNull( tp.getArtifact( relative.getCanonicalFile() ) );
        Assert.assertNotNull( tp.getArtifact( canonical ) );
    }
}
