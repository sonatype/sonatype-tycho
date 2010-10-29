package org.codehaus.tycho.osgitest.test;

import java.util.ArrayList;

import junit.framework.Assert;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.tycho.osgitest.TestFramework;
import org.codehaus.tycho.osgitools.DefaultArtifactKey;
import org.codehaus.tycho.osgitools.DefaultClasspathEntry;
import org.sonatype.tycho.classpath.ClasspathEntry;

public class TestFrameworkTest
    extends PlexusTestCase
{

    public void testJunit_v3_only()
        throws Exception
    {
        ArrayList<ClasspathEntry> cp = new ArrayList<ClasspathEntry>();

        cp.add( newDefaultClasspathEntry( TestFramework.TEST_JUNIT, "3.8.2" ) );

        Assert.assertEquals( TestFramework.TEST_JUNIT, new TestFramework().getTestFramework( cp ) );
    }

    public void testJunit_v4_only()
        throws Exception
    {
        ArrayList<ClasspathEntry> cp = new ArrayList<ClasspathEntry>();

        cp.add( newDefaultClasspathEntry( TestFramework.TEST_JUNIT, "4.5.0" ) );

        Assert.assertEquals( TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework( cp ) );
    }

    public void testJunit4_only()
        throws Exception
    {
        ArrayList<ClasspathEntry> cp = new ArrayList<ClasspathEntry>();

        cp.add( newDefaultClasspathEntry( TestFramework.TEST_JUNIT4, "4.5.0" ) );

        Assert.assertEquals( TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework( cp ) );
    }

    public void testJunit_v3_and_v4()
        throws Exception
    {
        ArrayList<ClasspathEntry> cp = new ArrayList<ClasspathEntry>();

        cp.add( newDefaultClasspathEntry( TestFramework.TEST_JUNIT, "3.8.2" ) );
        cp.add( newDefaultClasspathEntry( TestFramework.TEST_JUNIT, "4.5.0" ) );

        Assert.assertEquals( TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework( cp ) );
    }

    public void testJunit_and_Junit4()
        throws Exception
    {
        ArrayList<ClasspathEntry> cp = new ArrayList<ClasspathEntry>();

        cp.add( newDefaultClasspathEntry( TestFramework.TEST_JUNIT, "3.8.2" ) );
        cp.add( newDefaultClasspathEntry( TestFramework.TEST_JUNIT4, "4.5.0" ) );

        Assert.assertEquals( TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework( cp ) );
    }

    private ClasspathEntry newDefaultClasspathEntry( String id, String version )
    {
        return new DefaultClasspathEntry( null, new DefaultArtifactKey( org.sonatype.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN, id, version ), null, null );
    }
}
