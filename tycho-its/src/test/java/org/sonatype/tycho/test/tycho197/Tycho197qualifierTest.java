package org.sonatype.tycho.test.tycho197;

import java.io.File;
import java.util.jar.Manifest;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.maven.it.Verifier;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.PluginRef;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

import de.schlichtherle.io.FileInputStream;

public class Tycho197qualifierTest extends AbstractTychoIntegrationTest {

	@SuppressWarnings("unchecked")
	@Test
	public void checkProduct() throws Exception {
		Verifier verifier = getVerifier("/tycho197/product-test");

		final String timestamp = "20022002-2002";
		verifier.getCliOptions().add("-DforceContextQualifier=" + timestamp);
		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());

		final String version = "1.0.0." + timestamp;
		String featureLabel = "features/Feature_" + version;
		String pluginLabel = "plugins/Plugin_" + version + ".jar";

		File product = new File(basedir, "Product/target/product");
		Assert
				.assertTrue("Product folder should exists", product
						.isDirectory());

		File feature = new File(product, featureLabel);
		Assert.assertTrue("Feature '" + featureLabel + "' should exists",
				feature.isDirectory());

		de.schlichtherle.io.File featureJar = new de.schlichtherle.io.File(
				feature, "feature.xml");
		Feature featureXml = Feature.read(new FileInputStream(featureJar));
		Assert.assertEquals("Invalid feature version", version, featureXml
				.getVersion());

		PluginRef pluginRef = featureXml.getPlugins().get(0);
		Assert.assertEquals("Invalid plugin version at feature.xml", version,
				pluginRef.getVersion());

		File plugin = new File(product, pluginLabel);
		Assert.assertTrue("Plugin '" + pluginLabel + "' should exists", plugin
				.isFile());

		de.schlichtherle.io.File manifest = new de.schlichtherle.io.File(
				plugin, "META-INF/MANIFEST.MF");
		Manifest man = new Manifest(new FileInputStream(manifest));
		String bundleVersion = man.getMainAttributes().getValue(
				"Bundle-Version");
		Assert.assertEquals("Invalid Bundle-Version at plugin Manifest.MF",
				version, bundleVersion);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void checkSite() throws Exception{
		Verifier verifier = getVerifier("/tycho197/site-test");

		final String timestamp = "20022002-2002";
		verifier.getCliOptions().add("-DforceContextQualifier=" + timestamp);
		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());

		final String version = "1.0.0." + timestamp;
		String featureLabel = "features/Feature_" + version;
		String pluginLabel = "plugins/Plugin_" + version + ".jar";
		featureLabel += ".jar";

		File site = new File(basedir, "Site/target/site");
		Assert.assertTrue("Site folder should exists", site.isDirectory());
		File siteXml = new File(site, "site.xml");
		Assert.assertTrue("Site.xml should exists", siteXml.isFile());
		String siteContet = FileUtils.readFileToString(siteXml);
		Assert.assertTrue("Site.xml should contain '" + featureLabel
				+ "'. Got:\n" + siteContet, siteContet.contains(featureLabel));

		File feature = new File(site, featureLabel);
		Assert.assertTrue("Feature '" + featureLabel + "' should exists",
				feature.isFile());

		de.schlichtherle.io.File featureJar = new de.schlichtherle.io.File(
				feature, "feature.xml");
		Feature featureXml = Feature.read(new FileInputStream(featureJar));
		Assert.assertEquals("Invalid feature version", version, featureXml
				.getVersion());

		PluginRef pluginRef = featureXml.getPlugins().get(0);
		Assert.assertEquals("Invalid plugin version at feature.xml", version,
				pluginRef.getVersion());

		File plugin = new File(site, pluginLabel);
		Assert.assertTrue("Plugin '" + pluginLabel + "' should exists", plugin
				.isFile());

		de.schlichtherle.io.File manifest = new de.schlichtherle.io.File(
				plugin, "META-INF/MANIFEST.MF");
		Manifest man = new Manifest(new FileInputStream(manifest));
		String bundleVersion = man.getMainAttributes().getValue(
				"Bundle-Version");
		Assert.assertEquals("Invalid Bundle-Version at plugin Manifest.MF",
				version, bundleVersion);
	}
}
