package org.codehaus.tycho.osgitest;

import java.io.File;

import junit.framework.TestCase;

import org.sonatype.tycho.equinox.launching.DefaultEquinoxInstallationDescription;
import org.sonatype.tycho.equinox.launching.internal.DefaultEquinoxInstallation;
import org.sonatype.tycho.equinox.launching.internal.EquinoxLaunchConfiguration;

public class TestMojoTest
    extends TestCase
{

    public void testVMArgLineMultipleArgs()
        throws Exception
    {
        DefaultEquinoxInstallation testRuntime =
            new DefaultEquinoxInstallation( new DefaultEquinoxInstallationDescription(), null );
        EquinoxLaunchConfiguration cli = new EquinoxLaunchConfiguration( testRuntime );
        TestMojo testMojo = new TestMojo();
        testMojo.addVMArgs( cli, "-Dfoo=bar -Dkey2=value2" );
        assertEquals( 2, cli.getVMArguments().length );
    }

    public void testProgramArgLineMultipleArgs()
        throws Exception
    {
        DefaultEquinoxInstallation testRuntime =
            new DefaultEquinoxInstallation( new DefaultEquinoxInstallationDescription(), null );
        EquinoxLaunchConfiguration cli = new EquinoxLaunchConfiguration( testRuntime );
        TestMojo testMojo = new TestMojo();
        testMojo.addProgramArgs( cli, "foo bar   baz" );
        assertEquals( 3, cli.getProgramArguments().length );
    }

}
