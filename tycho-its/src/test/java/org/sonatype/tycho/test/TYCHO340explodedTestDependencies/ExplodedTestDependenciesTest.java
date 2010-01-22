package org.sonatype.tycho.test.TYCHO340explodedTestDependencies;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class ExplodedTestDependenciesTest extends AbstractTychoIntegrationTest {

	@Test
	public void testLocalMavenRepository() throws Exception {
		Verifier v01 = getVerifier("TYCHO340explodedTestDependencies", false);
		v01.executeGoal("install");
		v01.verifyErrorFreeLog();

		File antHome = new File(
				v01.localRepo,
				"p2/osgi/bundle/org.apache.ant/1.7.1.v20090120-1145/org.apache.ant_1.7.1.v20090120-1145");
		Assert.assertTrue(antHome.isDirectory());
	}

}
