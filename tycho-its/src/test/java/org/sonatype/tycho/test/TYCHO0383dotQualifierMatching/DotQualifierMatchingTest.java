package org.sonatype.tycho.test.TYCHO0383dotQualifierMatching;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class DotQualifierMatchingTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void testFeature()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO0383dotQualifierMatching/featureDotQualifier", false );
        verifier.getCliOptions().add( "-Dp2.repo=" + toURI( new File( "repositories/e342" ) ) );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();

        assertFileExists( new File( verifier.getBasedir() ), "target/site/plugins/org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar" );
    }

    @Test
    public void testProduct()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO0383dotQualifierMatching/productDotQualifier", false );
        verifier.getCliOptions().add( "-Dp2.repo=" + toURI( new File( "repositories/e342" ) ) );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();

        assertFileExists( new File( verifier.getBasedir() ), "productDotQualifier.product/target/linux.gtk.x86_64/eclipse/plugins/org.eclipse.osgi_3.4.3.R34x_v20081215-1030.jar" );
    }

}
