package org.eclipse.tycho.test.TYCHO0374dirBundleShape;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class DirectoryBundleShapeTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void test()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO0374dirBundleShape" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();

        File basedir = new File( verifier.getBasedir() );

        assertDirectoryExists( basedir, "product/target/product/eclipse/plugins/bundle_1.0.0*" );
    }
}
