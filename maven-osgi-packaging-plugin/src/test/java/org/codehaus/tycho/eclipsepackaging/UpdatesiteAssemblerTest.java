package org.codehaus.tycho.eclipsepackaging;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.eclipsepackaging.UpdateSiteAssembler;
import org.junit.Test;


public class UpdatesiteAssemblerTest {

	
	@Test
	public void testIsSourceBundle() throws Exception {
		MavenProject mockMavenProject = new MavenProject();
		mockMavenProject.getModel().getProperties().setProperty("sourceBundleSuffix", ".foo");
		mockMavenProject.setArtifactId("test.id");
		mockMavenProject.setPackaging("eclipse-plugin");
		UpdateSiteAssembler updateSiteAssembler = new UpdateSiteAssembler(null, null);
		Assert.assertTrue(updateSiteAssembler.isSourceBundle(mockMavenProject, "test.id.foo"));
	}
}
