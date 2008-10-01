package org.codehaus.tycho.mapfile.test;

import static org.junit.Assert.*;

import org.codehaus.tycho.mapfile.MapEntry;
import org.codehaus.tycho.mapfile.MapfileUtils;
import org.junit.Test;

public class MapfileUtilsTest {

	@Test
	public void parseEmpty() {
		MapEntry map;
		map = MapfileUtils.parse("");
		assertNull(map);
		map = MapfileUtils.parse(null);
		assertNull(map);
		map = MapfileUtils.parse("     ");
		assertNull(map);
		map = MapfileUtils.parse("! hey I made a comment");
		assertNull(map);
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseGarbage() {
		MapfileUtils.parse("hello world");
	}

	@Test
	public void parsePlugin() {
		String plugin = "plugin@org.eclipse.test=v20070226,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,";

		MapEntry map = MapfileUtils.parse(plugin);
		assertNotNull("Mapentry should be parsed", map);
		assertEquals("plugin", map.getKind());
		assertEquals("org.eclipse.test", map.getName());
		assertEquals("v20070226", map.getVersion());
		assertEquals(":pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse", map
				.getScmUrl());
		assertNull(map.getScmPath());
	}

	@Test
	public void parseFragment() {
		String fragment = "fragment@org.eclipse.ant.optional.junit=v20030401,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,";

		MapEntry map = MapfileUtils.parse(fragment);
		assertNotNull("Mapentry should be parsed", map);
		assertEquals("fragment", map.getKind());
		assertEquals("org.eclipse.ant.optional.junit", map.getName());
		assertEquals("v20030401", map.getVersion());
		assertEquals(":pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse", map
				.getScmUrl());
		assertNull(map.getScmPath());
	}

	@Test
	public void parseFeature() {
		String feature = "feature@org.eclipse.emf.all=build_200808251517,:pserver:anonymous@dev.eclipse.org:/cvsroot/modeling,,org.eclipse.emf/org.eclipse.emf/features/org.eclipse.emf.all-feature";

		MapEntry map = MapfileUtils.parse(feature);
		assertNotNull("Mapentry should be parsed", map);
		assertEquals("feature", map.getKind());
		assertEquals("org.eclipse.emf.all", map.getName());
		assertEquals("build_200808251517", map.getVersion());
		assertEquals(":pserver:anonymous@dev.eclipse.org:/cvsroot/modeling",
				map.getScmUrl());
		assertEquals(
				"org.eclipse.emf/org.eclipse.emf/features/org.eclipse.emf.all-feature",
				map.getScmPath());
	}

}
