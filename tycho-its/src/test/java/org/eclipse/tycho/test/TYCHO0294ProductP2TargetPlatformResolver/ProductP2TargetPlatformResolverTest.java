package org.eclipse.tycho.test.TYCHO0294ProductP2TargetPlatformResolver;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ProductP2TargetPlatformResolverTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void testBasic()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO0294ProductP2TargetPlatformResolver" );
        verifier.getCliOptions().add( "-Dp2.repo=" + toURI( new File( "repositories/e342" ) ) );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        File target = new File( verifier.getBasedir(), "product.bundle-based/target" );

        assertDirectoryExists( target, "linux.gtk.x86_64/eclipse/plugins/org.eclipse.equinox.launcher.gtk.linux.x86_64_*" );
        assertDirectoryExists( target, "macosx.carbon.x86/eclipse/plugins/org.eclipse.equinox.launcher.carbon.macosx_*" );
        assertDirectoryExists( target, "win32.win32.x86/eclipse/plugins/org.eclipse.equinox.launcher.win32.win32.x86_*" );
    }

}
