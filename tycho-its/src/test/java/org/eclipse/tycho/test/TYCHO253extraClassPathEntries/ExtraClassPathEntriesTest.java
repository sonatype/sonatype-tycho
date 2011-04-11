package org.eclipse.tycho.test.TYCHO253extraClassPathEntries;

import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class ExtraClassPathEntriesTest extends AbstractTychoIntegrationTest {
	@Test
	public void testJarsExtraClasspath() throws Exception {
		Verifier verifier = getVerifier("/TYCHO253extraClassPathEntries/org.eclipse.tycho.testExtraClasspathTest1");
        verifier.executeGoals(Arrays.asList("clean","install"));
		verifier.verifyErrorFreeLog();
	}
	
	@Test
	public void testExtraClasspath() throws Exception {
		Verifier verifier = getVerifier("/TYCHO253extraClassPathEntries/org.eclipse.tycho.testExtraClasspathTest2");
		verifier.executeGoals(Arrays.asList("clean","install"));
		verifier.verifyErrorFreeLog();
	}
	
	@Test
	public void testReferenceToInnerJar() throws Exception {
		Verifier verifier = getVerifier("/TYCHO253extraClassPathEntries");
        verifier.executeGoals(Arrays.asList("clean","install"));
		verifier.verifyErrorFreeLog();
	}
}
