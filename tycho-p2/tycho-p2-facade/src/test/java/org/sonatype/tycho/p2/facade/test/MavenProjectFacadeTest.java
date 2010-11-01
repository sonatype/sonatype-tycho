package org.sonatype.tycho.p2.facade.test;

import static org.junit.Assert.assertTrue;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.osgitools.DefaultDependentMavenProjectProxy;
import org.junit.Test;
import org.sonatype.tycho.p2.facade.internal.MavenProjectFacade;


public class MavenProjectFacadeTest {

	@Test
	public void testHasSourceBundle() throws Exception {
		MavenProject wrapped = new MavenProject();
		Plugin sourcePlugin = new Plugin();
		sourcePlugin.setArtifactId("maven-osgi-source-plugin");
		sourcePlugin.setGroupId("org.sonatype.tycho");
		wrapped.getBuild().addPlugin(sourcePlugin);
		MavenProjectFacade mavenProjectFacade = new MavenProjectFacade(DefaultDependentMavenProjectProxy.adapt(wrapped));
		assertTrue(mavenProjectFacade.hasSourceBundle());
		
	}
}
