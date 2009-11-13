package org.sonatype.tycho.test.TYCHO300launcherIcons;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;


public class LauncherIconsTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void testEclipseSourceBundleManifestAttributes()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO300launcherIcons/product" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();

        // TODO is there a good way to test changes to native launcher?
    }

}
