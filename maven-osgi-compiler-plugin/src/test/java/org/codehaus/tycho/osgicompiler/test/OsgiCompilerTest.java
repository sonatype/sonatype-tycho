package org.codehaus.tycho.osgicompiler.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.tycho.osgicompiler.AbstractOsgiCompilerMojo;
import org.codehaus.tycho.osgicompiler.ClasspathComputer3_0;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;

public class OsgiCompilerTest extends AbstractTychoMojoTestCase {

	protected Maven maven;
	protected File storage;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		maven = (Maven) lookup(Maven.ROLE);
		storage = new File(getBasedir(), "target/storage");
		FileUtils.deleteDirectory(storage);
	}

	private List<MavenProject> getSortedProjects(File basedir, File platform) throws Exception {
		File pom = new File(basedir, "pom.xml");
		MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.setLocalRepository(getLocalRepository());
        if (platform != null) {
        	request.getProperties().put("tycho.targetPlatform", platform.getCanonicalPath());
        }
		MavenExecutionResult result = new DefaultMavenExecutionResult();
		ReactorManager reactorManager = maven.createReactorManager(request, result);
		if (result.getExceptions().size() > 0) {
			throw new RuntimeException(result.getExceptions().toString());
		}
		@SuppressWarnings("unchecked")
		List<MavenProject> projects = reactorManager.getSortedProjects();
		calculateConcreteState(projects, request);
		return projects;
	}

	private AbstractOsgiCompilerMojo getMojo(MavenProject project) throws Exception {
		AbstractOsgiCompilerMojo mojo = (AbstractOsgiCompilerMojo) lookupMojo("compile", project.getFile());
		setVariableValueToObject(mojo, "project", project);
		setVariableValueToObject(mojo, "storage", storage);
		setVariableValueToObject(mojo, "projectArtifact", project.getArtifact());
		setVariableValueToObject(mojo, "outputDirectory", new File(project.getBuild().getOutputDirectory()).getAbsoluteFile());
		// tycho-compiler-jdt does not support forked compilation
//		        setVariableValueToObject(mojo, "fork", fork? Boolean.TRUE: Boolean.FALSE);
		return mojo;
	}

	public void testAccessRestrictionCompilationError() throws Exception {
		File basedir = getBasedir("projects/accessrules");
		List<MavenProject> projects = getSortedProjects(basedir, null);

		try {
			for (MavenProject project : projects) {
				if (!"pom".equals(project.getPackaging())) {
					getMojo(project).execute();
				}
			}
			fail("Restricted package access");
		} catch (MojoFailureException e) {
			assertTrue(e.getLongMessage().contains("P001Impl is not accessible"));
		}
	}

	public void testAccessRulesClasspath() throws Exception {
		File basedir = getBasedir("projects/accessrules");
		List<MavenProject> projects = getSortedProjects(basedir, null);

		getMojo(projects.get(1)).execute();
		getMojo(projects.get(2)).execute();
		getMojo(projects.get(3)).execute();

		MavenProject project = projects.get(4);
		List<String> cp = getMojo(project).getClasspathElements();
		assertEquals(4, cp.size());
		assertEquals(getClasspathElement(project.getBasedir(), "target/classes", ""), cp.get(0));
		assertEquals(getClasspathElement(new File(getBasedir()), "target/projects/accessrules/p001/target/classes", "[+p001/*:-**/*]"), cp.get(1));
		assertEquals(getClasspathElement(new File(getBasedir()), "target/projects/accessrules/p003/target/classes", "[+p003/*:-**/*]"), cp.get(2));
		assertEquals(getClasspathElement(new File(getBasedir()), "target/projects/accessrules/p004/target/classes", "[+p004/*:-**/*]"), cp.get(3));
	}

	public void testClasspath() throws Exception {
		File basedir = getBasedir("projects/classpath");
		List<MavenProject> projects = getSortedProjects(basedir, new File(getBasedir(), "src/test/resources/projects/classpath/platform"));

		MavenProject project;
		List<String> cp;

		// simple project
		project = projects.get(1);
		cp = getMojo(project).getClasspathElements();
		assertEquals(1, cp.size());
		assertEquals(getClasspathElement(project.getBasedir(), "target/classes", ""), cp.get(0));

		// project with nested lib
		project = projects.get(2);
		cp = getMojo(project).getClasspathElements();
		assertEquals(2, cp.size());
		assertEquals(getClasspathElement(project.getBasedir(), "target/classes", ""), cp.get(0));
		assertEquals(getClasspathElement(project.getBasedir(), "lib/lib.jar", ""), cp.get(1));

		// project with external dependency with nested jar
		project = projects.get(3);
		cp = getMojo(project).getClasspathElements();
		assertEquals(3, cp.size());
		assertEquals(getClasspathElement(project.getBasedir(), "target/classes", ""), cp.get(0));
		assertEquals(getClasspathElement(new File(getBasedir()), "src/test/resources/projects/classpath/platform/plugins/p003_0.0.1.jar", "[-**/*]"), cp.get(1));
		assertEquals(getClasspathElement(new File(getBasedir()), "target/storage/p003_1.0.0/lib/lib.jar", "[-**/*]"), cp.get(2));
	}

	private String getClasspathElement(File base, String path, String accessRules) throws IOException {
		String file = new File(base, path).getCanonicalPath();
		return file.replace('\\', '/') + accessRules.replace(":", ClasspathComputer3_0.ACCESS_RULE_SEPARATOR);
	}
}
