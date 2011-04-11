package org.eclipse.tycho.test.TYCHO0432configurableFailIfNoTests;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class FailIfNoTestsTest
    extends AbstractTychoIntegrationTest
{

    @Test
    public void testNoTestsNoFailure()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO0432configurableFailIfNoTests" );
        verifier.getCliOptions().add( "-DfailIfNoTests=false" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testNoTestsFailureDefaultCase() throws Exception{
        Verifier verifier = getVerifier( "/TYCHO0432configurableFailIfNoTests" );
        try
        {
            verifier.executeGoal( "integration-test" );
            Assert.fail();
        }
        catch ( VerificationException e )
        {
            // expected
        }
        verifier.verifyTextInLog( "There are test failures");
    }

}
