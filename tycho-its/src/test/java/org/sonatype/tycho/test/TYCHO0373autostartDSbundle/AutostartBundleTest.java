package org.sonatype.tycho.test.TYCHO0373autostartDSbundle;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.osgi.framework.Version;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class AutostartBundleTest
    extends AbstractTychoIntegrationTest
{
    private static final Version MINIMUM_ECLIPSE_VERSION = new Version( 3, 5, 0 );

    @Test
    public void implicitDSAutostart()
        throws Exception
    {
        if ( isApplicable() )
        {
            Verifier verifier = getVerifier( "/TYCHO0373autostartDSbundle/implicit/ds.test" );
            verifier.executeGoal( "integration-test" );
            verifier.verifyErrorFreeLog();
        }
    }

    @Test
    public void explicitBundleStartLevel()
        throws Exception
    {
        if ( isApplicable() )
        {
            Verifier verifier = getVerifier( "/TYCHO0373autostartDSbundle/explicit" );
            verifier.executeGoal( "integration-test" );
            verifier.verifyErrorFreeLog();
        }
    }

    /** Declarative services were introduced in eclipse 3.5 */
    private boolean isApplicable()
    {
        return MINIMUM_ECLIPSE_VERSION.compareTo( getEclipseVersion() ) <= 0;
    }
}
