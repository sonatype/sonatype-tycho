package org.eclipse.tycho.test.tycho503;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TYCHO503DoubleEncodedUrlTest
    extends AbstractTychoIntegrationTest
{

    @Test
    public void testEclipseRepositoryModuleWithSpacesInPath()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO503Path With Spaces", false );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
    }

}
