package org.codehaus.tycho.maven.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.tycho.osgitools.EclipsePluginPathFinder;

public class PluginPathFinderTest extends PlexusTestCase {

	public void testTargetPlatform() throws Exception {
		EclipsePluginPathFinder finder = new EclipsePluginPathFinder(false);

		File targetPlatform = new File("src/test/resources/targetplatforms/wtp-2.0").getCanonicalFile();
		List<File> sites = getCannonicalFiles(finder.getSites(targetPlatform));

		assertEquals(3, sites.size());
		assertTrue(sites.toString(), sites.contains(targetPlatform));
		assertTrue(sites.toString(), sites.contains(new File(targetPlatform, "dropins/zest-3.4")));
		assertTrue(sites.toString(), sites.contains(new File(targetPlatform, "../subclipse-1.3").getCanonicalFile()));
	}

	public void testPlugins33() throws Exception {
		EclipsePluginPathFinder finder = new EclipsePluginPathFinder(false);

		File targetPlatform = new File("src/test/resources/targetplatforms/wtp-2.0").getCanonicalFile();
		List<File> plugins = getCannonicalFiles(finder.getPlugins(targetPlatform));

		assertEquals(2, plugins.size());
		assertTrue(plugins.contains(new File(targetPlatform, "plugins/com.ibm.icu.source_3.6.1.v20070906").getCanonicalFile()));
		assertTrue(plugins.contains(new File(targetPlatform, "plugins/org.eclipse.datatools.enablement.sybase.asa.models_1.0.0.200706071.jar").getCanonicalFile()));
	}

	public void testPlugins34() throws Exception {
		EclipsePluginPathFinder finder = new EclipsePluginPathFinder(false);

		File targetPlatform = new File("src/test/resources/targetplatforms/wtp-3.0").getCanonicalFile();
		List<File> plugins = getCannonicalFiles(finder.getPlugins(targetPlatform));

		assertEquals(2, plugins.size());
//		assertTrue(plugins.contains(new File(targetPlatform, "plugins/com.ibm.icu_3.8.1.v20080402.jar").getCanonicalFile()));
//		assertTrue(plugins.contains(new File(targetPlatform, "plugins/org.junit4_4.3.1").getCanonicalFile()));
		assertTrue(plugins.contains(new File(targetPlatform, "dropins/com.ibm.icu.source_3.6.1.v20070906").getCanonicalFile()));
		assertTrue(plugins.contains(new File(targetPlatform, "dropins/org.eclipse.datatools.enablement.sybase.asa.models_1.0.0.200706071.jar").getCanonicalFile()));
	}

	public void testSites34() throws Exception {
		EclipsePluginPathFinder finder = new EclipsePluginPathFinder(false);

		File targetPlatform = new File("src/test/resources/targetplatforms/wtp-3.0").getCanonicalFile();
		List<File> sites = getCannonicalFiles(finder.getSites(targetPlatform));

		assertEquals(5, sites.size());
		assertTrue(sites.toString(), sites.contains(targetPlatform));
		assertTrue(sites.toString(), sites.contains(new File(targetPlatform, "dropins/ajdt")));
		assertTrue(sites.toString(), sites.contains(new File(targetPlatform, "dropins/eclipse")));
		assertTrue(sites.toString(), sites.contains(new File(targetPlatform, "dropins/emf/eclipse")));
		assertTrue(sites.toString(), sites.contains(new File(targetPlatform, "../subclipse-1.3").getCanonicalFile()));
	}

	private List<File> getCannonicalFiles(Set<File> files) throws IOException {
		ArrayList<File> result = new ArrayList<File>();
		for (File file : files) {
			result.add(file.getCanonicalFile());
		}
		return result;
	}

	public void testSitesSimple() throws Exception {
		EclipsePluginPathFinder finder = new EclipsePluginPathFinder(false);

		File targetPlatform = new File("src/test/resources/targetplatforms/simple").getCanonicalFile();
		List<File> sites = new ArrayList<File>(finder.getSites(targetPlatform));

		assertEquals(1, sites.size());
		assertEquals(targetPlatform, sites.get(0));
	}
}
