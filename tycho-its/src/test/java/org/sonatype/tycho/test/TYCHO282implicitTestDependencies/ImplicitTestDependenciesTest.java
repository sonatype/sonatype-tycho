package org.sonatype.tycho.test.TYCHO282implicitTestDependencies;

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class ImplicitTestDependenciesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testLocalMavenRepository() throws Exception {
        Verifier v01 = getVerifier( "TYCHO282implicitTestDependencies", false );
        v01.getCliOptions().add( "-Dp2.repo=" + toURI( new File( "repositories/e342" ) ) );
        v01.executeGoal( "install" );
        v01.verifyErrorFreeLog();
	}

	private String toURI( File file ) throws IOException
    {
        return file.getCanonicalFile().toURI().normalize().toString();
    }

}
