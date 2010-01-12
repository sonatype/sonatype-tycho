package org.codehaus.tycho.buildnumber.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.jar.JarFile;

import org.apache.maven.Maven;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.eclipsepackaging.PackagePluginMojo;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;

public class BinIncludesTest extends AbstractTychoMojoTestCase {

	protected Maven maven;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		maven = lookup(Maven.class);
	}

	@Override
	protected void tearDown() throws Exception {
		maven = null;
		super.tearDown();
	}

	public void testNoDot() throws Exception {
		File basedir = getBasedir("projects/binIncludes");
		PackagePluginMojo mojo = execMaven(basedir);
		createDummyClassFile(basedir);
		mojo.execute();
		JarFile pluginJar = new JarFile(new File(basedir,
				"p001/target/test.jar"));
		try {
			assertNull(
					"class files from target/classes must not be included in plugin jar if no '.' in bin.includes",
					pluginJar.getEntry("TestNoDot.class"));
		} finally {
			pluginJar.close();
		}
	}

	private PackagePluginMojo execMaven(File basedir) throws Exception {
		File pom = new File(basedir, "p001/pom.xml");
		MavenExecutionRequest request = newMavenExecutionRequest(pom);
		request.getProjectBuildingRequest().setProcessPlugins(false);
		MavenExecutionResult result = maven.execute(request);
		MavenProject project = result.getProject();
		ArrayList<MavenProject> projects = new ArrayList<MavenProject>();
		projects.add(project);
		MavenSession session = new MavenSession(getContainer(), request,
				result, projects);
		PackagePluginMojo mojo = getMojo(project, session);
		return mojo;
	}

	private void createDummyClassFile(File basedir) throws IOException {
		File classFile = new File(basedir,
				"p001/target/classes/TestNoDot.class");
		classFile.getParentFile().mkdirs();
		classFile.createNewFile();
	}

	private PackagePluginMojo getMojo(MavenProject project, MavenSession session)
			throws Exception {
		PackagePluginMojo mojo = (PackagePluginMojo) lookupMojo(
				"package-plugin", project.getFile());
		setVariableValueToObject(mojo, "project", project);
		setVariableValueToObject(mojo, "session", session);
		return mojo;
	}

}
