package org.sonatype.tycho.test.TYCHO411targetLocationTypes;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class TargetLocationTypesTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void testMultiplatformReactorBuild()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO411targetLocationTypes", false );
        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog( "Target location type: Directory is not supported" );
        verifier.verifyTextInLog( "Target location type: Profile is not supported" );
    }

}
