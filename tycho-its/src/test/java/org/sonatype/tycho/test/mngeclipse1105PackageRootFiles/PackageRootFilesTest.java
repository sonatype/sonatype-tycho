package org.sonatype.tycho.test.mngeclipse1105PackageRootFiles;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class PackageRootFilesTest
    extends AbstractTychoIntegrationTest
{

    @Override
    @SuppressWarnings( "unchecked" )
    protected Verifier getVerifier( String test )
        throws Exception
    {
        Verifier verifier = super.getVerifier( test );

        verifier.getCliOptions().add( "-Dosgi.os=macosx" );
        verifier.getCliOptions().add( "-Dosgi.ws=carbon" );
        verifier.getCliOptions().add( "-Dosgi.arch=x86" );

        return verifier;
    }

    @Test
    public void test()
        throws Exception
    {
        Verifier verifier = getVerifier( "MNGECLIPSE1105rootfiles" );

        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();

        verifier.assertFilePresent( "target/product/eclipse/jre/the_entire_jre.txt" );
        verifier.assertFilePresent( "target/product/eclipse/license.txt" );

        verifier.assertFilePresent( "target/product/eclipse/configuration/config.macosx.txt" );
        verifier.assertFileNotPresent( "target/product/eclipse/configuration/config.linux.txt" );
        verifier.assertFileNotPresent( "target/product/eclipse/configuration/config.win32.txt" );
    }

}
