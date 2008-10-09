package org.sonatype.tycho.test.tycho012;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class LocalMavenRepositoryTest extends AbstractTychoIntegrationTest {

	@Test
	public void testLocalMavenRepository() throws Exception {
        Verifier v01 = getVerifier("tycho012/build01/bundle01", false);
    	v01.getCliOptions().add( "-DtargetPlatform=" + getTargetPlatforn() );
        v01.executeGoal("install");
        v01.verifyErrorFreeLog();

        Verifier v02 = getVerifier("tycho012/build02/bundle02", false);
    	v02.getCliOptions().add( "-DtargetPlatform=" + getTargetPlatforn() );
        v02.executeGoal("install");
        v02.verifyErrorFreeLog();
	}
}
