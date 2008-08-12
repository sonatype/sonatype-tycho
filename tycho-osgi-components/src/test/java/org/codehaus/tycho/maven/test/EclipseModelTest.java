package org.codehaus.tycho.maven.test;

import java.io.File;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.UpdateSite;

public class EclipseModelTest extends TestCase {

	File target = new File("target/modelio");
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		target.mkdirs();
	}

	public void testUpdateSite() throws Exception {
		UpdateSite site = UpdateSite.read(new File("src/test/resources/modelio/site.xml"));
		
		List<UpdateSite.FeatureRef> features = site.getFeatures();
		assertEquals(2, features.size());
		assertEquals("featureB", features.get(1).getId());
		assertEquals("2.0.0", features.get(1).getVersion());
		
		Map<String, String> archives = site.getArchives();
		assertEquals(2, archives.size());
		assertEquals("http://www.company.com/updates/plugins/pluginA_1.0.0.jar", archives.get("plugins/pluginA_1.0.0.jar"));

		features.get(0).setVersion("3.0.0");
		
		site.removeArchives();
		assertTrue(site.getArchives().isEmpty());
		
		File updatedFile = new File(target, "site.xml");
		UpdateSite.write(site, updatedFile);
		UpdateSite updated = UpdateSite.read(updatedFile);
		assertEquals("3.0.0", updated.getFeatures().get(0).getVersion());
		assertTrue(updated.getArchives().isEmpty());
	}

	public void testFeature() throws Exception {
		Feature feature = Feature.read(new File("src/test/resources/modelio/feature.xml"));

		assertEquals("1.0.0", feature.getVersion());

		List<Feature.PluginRef> plugins = feature.getPlugins();
		assertEquals(1, plugins.size());
		assertEquals("pluginA", plugins.get(0).getId());

		List<Feature.FeatureRef> features = feature.getIncludedFeatures();
		assertEquals(1, features.size());

		List<Feature.RequiresRef> requires = feature.getRequires();
		assertEquals(1, requires.size());
		assertEquals("pluginB", requires.get(0).getImports().get(0).getPlugin());
		assertEquals("featureC", requires.get(0).getImports().get(1).getFeature());
		
		feature.setVersion("1.2.3");
		plugins.get(0).setVersion("3.4.5");

		File updatedFile = new File(target, "feature.xml");
		Feature.write(feature, updatedFile);
		Feature updated = Feature.read(updatedFile);
		assertEquals("1.2.3", updated.getVersion());
		assertEquals("3.4.5", updated.getPlugins().get(0).getVersion());
	}
}
