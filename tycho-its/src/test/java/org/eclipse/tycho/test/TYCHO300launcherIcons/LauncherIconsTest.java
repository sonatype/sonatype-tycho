package org.eclipse.tycho.test.TYCHO300launcherIcons;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;


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
