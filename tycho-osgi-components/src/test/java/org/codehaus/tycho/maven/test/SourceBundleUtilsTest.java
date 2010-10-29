package org.codehaus.tycho.maven.test;

import static org.junit.Assert.assertEquals;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.utils.SourceBundleUtils;
import org.junit.Test;


public class SourceBundleUtilsTest {

	@Test
	public void testGetSourceBundleSuffix() throws Exception {
		MavenProject mockMavenProject = new MavenProject();
		mockMavenProject.getModel().getProperties().setProperty("sourceBundleSuffix", ".bar");
		mockMavenProject.setPackaging("eclipse-plugin");
		assertEquals(".bar", SourceBundleUtils.getSourceBundleSuffix(mockMavenProject));
	}

	@Test
	public void testGetDefaultSourceBundleSuffix() throws Exception {
		MavenProject mockMavenProject = new MavenProject();
		mockMavenProject.setPackaging("eclipse-plugin");
		assertEquals(".source", SourceBundleUtils.getSourceBundleSuffix(mockMavenProject));
	}


}
