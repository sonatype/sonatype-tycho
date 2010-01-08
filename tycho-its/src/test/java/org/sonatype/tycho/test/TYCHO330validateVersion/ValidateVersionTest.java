package org.sonatype.tycho.test.TYCHO330validateVersion;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class ValidateVersionTest
    extends AbstractTychoIntegrationTest
{

    @Test
    public void testPlugin()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO330validateVersion/bundle", false );
        try
        {
            verifier.executeGoal( "verify" );
            Assert.fail();
        }
        catch ( VerificationException e )
        {
            // good enough for now
        }

    }

    @Test
    public void testFeature()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO330validateVersion/feature", false );
        try
        {
            verifier.executeGoal( "verify" );
            Assert.fail();
        }
        catch ( VerificationException e )
        {
            // good enough for now
        }
    }
}
