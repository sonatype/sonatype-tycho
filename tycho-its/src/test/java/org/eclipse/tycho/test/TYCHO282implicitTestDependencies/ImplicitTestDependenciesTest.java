package org.eclipse.tycho.test.TYCHO282implicitTestDependencies;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ImplicitTestDependenciesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testLocalMavenRepository() throws Exception {
        Verifier v01 = getVerifier( "TYCHO282implicitTestDependencies", false );
        v01.getCliOptions().add( "-Dp2.repo=" + toURI( new File( "repositories/e342" ) ) );
        v01.executeGoal( "install" );
        v01.verifyErrorFreeLog();
	}

}
