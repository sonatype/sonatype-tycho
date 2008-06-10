package org.codehaus.tycho.maven.test;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.maven.Maven;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.activation.DefaultProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

public class TychoTest extends PlexusTestCase {

	protected Maven maven;

	protected void setUp() throws Exception {
		super.setUp();
		if (getContainer().hasComponent(Maven.ROLE, "test")) {
			maven = (Maven) lookup(Maven.ROLE, "test");
		} else {
			// default over to the main project builder...
			maven = (Maven) lookup(Maven.ROLE);
		}
	}

	protected ArtifactRepository getLocalRepository() throws Exception {
		ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup(ArtifactRepositoryLayout.ROLE, "legacy");
		
		File path = new File("target/local-repo").getCanonicalFile();

		ArtifactRepository r = new DefaultArtifactRepository("local", "file://"	+ path, repoLayout);

		return r;
	}

	public void testModuleOrder() throws Exception {
		File pom = new File("src/test/resources/projects/moduleorder/pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);

		MavenExecutionResult result = new DefaultMavenExecutionResult();

		ReactorManager reactorManager = maven.createReactorManager(request, result);

		List projects = reactorManager.getSortedProjects();

		MavenProject p002 = (MavenProject) projects.get(1);
		MavenProject p001 = (MavenProject) projects.get(2);

		assertEquals("moduleorder.p001", p001.getArtifactId());
		assertEquals("moduleorder.p002", p002.getArtifactId());
	}

	private MavenExecutionRequest newMavenExecutionRequest(File pom) {
		Properties props = System.getProperties();
        ProfileActivationContext ctx = new DefaultProfileActivationContext( props, false );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
		request.setBaseDirectory(pom.getParentFile());
		request.setPom(pom);
		request.setProfileManager(new DefaultProfileManager( getContainer(), ctx ));
		request.setProperties(props);
		request.setUserProperties(props);
		return request;
	}

	public void testResolutionError() throws Exception {
		File pom = new File("src/test/resources/projects/resolutionerror/p001/pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);

		MavenExecutionResult result = new DefaultMavenExecutionResult();

		maven.createReactorManager(request, result);

		assertEquals(1, result.getExceptions().size());
	}
}
