package org.sonatype.tycho.test.tycho129;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

/* java -jar \eclipse\plugins\org.eclipse.equinox.launcher_1.0.1.R33x_v20080118.jar -application org.eclipse.update.core.siteOptimizer -digestBuilder -digestOutputDir=d:\temp\eclipse\digest -siteXML=D:\sonatype\workspace\tycho\tycho-its\projects\tycho129\tycho.demo.site\target\site\site.xml  -jarProcessor -processAll -pack -outputDir d:\temp\eclipse\site D:\sonatype\workspace\tycho\tycho-its\projects\tycho129\tycho.demo.site\target\site */
public class Tycho129Pack200SiteTest extends AbstractTychoIntegrationTest {

	@Test
	public void generatePackSite() throws Exception {
		Verifier verifier = getVerifier("/tycho129");
		verifier.setAutoclean(false);

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		File feature = new File(verifier.getBasedir(),
				"tycho.demo.site/target/site/features/tycho.demo.feature_1.0.0.jar.pack.gz");
		Assert.assertTrue("Feature pack should exist " + feature, feature
				.exists());

		File plugin = new File(verifier.getBasedir(),
				"tycho.demo.site/target/site/plugins/tycho.demo_1.0.0.jar.pack.gz");
		Assert.assertTrue("Plugin pack should exist " + plugin, plugin
						.exists());
	}

}
