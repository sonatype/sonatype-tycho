package org.sonatype.tycho.test.tycho026;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

/* java -jar \eclipse\plugins\org.eclipse.equinox.launcher_1.0.1.R33x_v20080118.jar -application org.eclipse.update.core.siteOptimizer -digestBuilder -digestOutputDir=d:\temp\eclipse\digest -siteXML=D:\sonatype\workspace\tycho\tycho-its\projects\tycho129\tycho.demo.site\target\site\site.xml  -jarProcessor -processAll -pack -outputDir d:\temp\eclipse\site D:\sonatype\workspace\tycho\tycho-its\projects\tycho129\tycho.demo.site\target\site */
public class Tycho26MissingFeatureTest extends AbstractTychoIntegrationTest {

	@Test(expected = VerificationException.class)
	public void test() throws Exception {
		Verifier verifier = getVerifier("/tycho026");
		verifier.setAutoclean(false);

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

}
