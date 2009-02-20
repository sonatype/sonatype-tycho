/*
 * 	Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.tycho.osgicompiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.tycho.osgicompiler.copied.AbstractCompilerMojo;
import org.codehaus.tycho.osgicompiler.copied.CompilationFailureException;
import org.codehaus.tycho.osgitools.DependencyComputer;
import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.osgitools.project.BuildOutputJar;
import org.codehaus.tycho.osgitools.project.EclipsePluginProject;
import org.codehaus.tycho.utils.ArtifactRef;

public abstract class AbstractOsgiCompilerMojo extends AbstractCompilerMojo {

	/**
	 * A temporary directory to extract embedded jars
	 * 
	 * @parameter expression="${project.build.directory}/plugins"
	 * @required
	 * @readonly
	 */
	private File storage;

	/**
	 * @parameter expression="${project}"
	 */
	private MavenProject project;

	private EclipsePluginProject pdeProject;

	/**
	 * If set to true, compiler will use source folders defined in build.properties 
	 * file and will ignore ${project.compileSourceRoots}/${project.testCompileSourceRoots}.
	 * 
	 * Compilation will fail with an error, if this parameter is set to true
	 * but the project does not have valid build.properties file.
	 *  
	 * @parameter default-value="true" 
	 */
	private boolean usePdeSourceRoots;

	/**
	 * Transitively add specified maven artifacts to compile classpath 
	 * in addition to elements calculated according to OSGi rules. 
	 * All packages from additional entries will be accessible at compile time. 
	 * 
	 * Useful when OSGi runtime classpath contains elements not defined
	 * using normal dependency mechanisms. For example, when Eclipse Equinox
	 * is started from application server with -Dosgi.parentClassloader=fwk
	 * parameter.
	 * 
	 * DO NOT USE. This is a stopgap solution to allow refactoring of tycho-p2 code
	 * to a separate set of components.
	 *  
	 * @parameter 
	 */
	private ArtifactRef[] extraClasspathElements;

	/** @component */
	private OsgiState state;

	/** @component */
	private DependencyComputer dependencyComputer;

	/** @parameter expression="${session}" */
	private MavenSession session;

	/** @component */
	private ArtifactFactory artifactFactory;

	/** @component */
	private ArtifactResolver resolver;

	/** @component */
	private MavenProjectBuilder mavenProjectBuilder;

	/** @parameter expression="${project.build.directory}" */
	private File buildTarget;

	private ClasspathComputer classpathComputer;

	/**
	 * A list of inclusion filters for the compiler.
	 * 
	 * @parameter
	 */
	private Set<String> includes = new HashSet<String>();

	/**
	 * A list of exclusion filters for the compiler.
	 * 
	 * @parameter
	 */
	private Set<String> excludes = new HashSet<String>();

	/**
	 * Current build output jar
	 */
	private BuildOutputJar outputJar;

	public void execute() throws MojoExecutionException, CompilationFailureException {
		pdeProject = state.getEclipsePluginProject(project);

		if (usePdeSourceRoots) {
			getLog().info("Using compile source roots from build.properties");
		}

		for (BuildOutputJar jar : pdeProject.getOutputJars()) {
			this.outputJar = jar;
			this.outputJar.getOutputDirectory().mkdirs();
			super.execute();
			getClasspathComputer().addOutputDirectory(this.outputJar.getOutputDirectory());
		}

		// this does not include classes from nested jars
		BuildOutputJar dotOutputJar = pdeProject.getDotOutputJar();
		if (dotOutputJar != null) {
			project.getArtifact().setFile(dotOutputJar.getOutputDirectory());
		}
	}

	@Override
	protected File getOutputDirectory() {
		return outputJar.getOutputDirectory();
	}

	public List<String> getClasspathElements() throws MojoExecutionException {
		List<String> classpath = getClasspathComputer().computeClasspath();
		
		if (extraClasspathElements != null) {
	    	ArtifactRepository localRepository = session.getLocalRepository();
	    	List remoteRepositories = project.getRemoteArtifactRepositories();
			for (ArtifactRef a : extraClasspathElements) {
				try {

					// immediate artifact
					Artifact artifact = artifactFactory.createArtifact(a.getGroupId(), a.getArtifactId(), a.getVersion(), "compile", "jar");
					resolver.resolve(artifact, remoteRepositories, localRepository);
					classpath.add(artifact.getFile().getAbsolutePath() + "[+**/*]");

					// transitive dependencies
					Artifact pomArtifact = artifactFactory.createArtifact(a.getGroupId(), a.getArtifactId(), a.getVersion(), "pom", "pom");
					resolver.resolve(pomArtifact, remoteRepositories, localRepository);

					FileUtils.copyFileToDirectory(pomArtifact.getFile(), buildTarget);

					ProjectBuilderConfiguration pbc = new DefaultProjectBuilderConfiguration();
			        pbc.setLocalRepository( localRepository );

					MavenProject pomProject = mavenProjectBuilder.buildProjectWithDependencies(new File(buildTarget, pomArtifact.getFile().getName()), pbc).getProject();

	    			for (Iterator j = pomProject.getArtifacts().iterator(); j.hasNext();) {
	    				Artifact b = (Artifact) j.next();
						classpath.add(b.getFile().getAbsolutePath() + "[+**/*]");
					}
					
				} catch (Exception e) {
					throw new MojoExecutionException("Could not resolve extra classpath entry", e);
				}
			}
		}
		
		return classpath;
	}

	private ClasspathComputer getClasspathComputer() {
		if (classpathComputer == null) {
			classpathComputer = new ClasspathComputer(state, dependencyComputer, project, storage);
		}
		return classpathComputer;
	}

	protected final List<String> getCompileSourceRoots() throws MojoExecutionException {
		return usePdeSourceRoots? getPdeCompileSourceRoots(): getConfiguredCompileSourceRoots();
	}

	protected abstract List<String> getConfiguredCompileSourceRoots();

	protected SourceInclusionScanner getSourceInclusionScanner(int staleMillis) {
		SourceInclusionScanner scanner = null;

		if (includes.isEmpty() && excludes.isEmpty()) {
			scanner = new StaleSourceScanner(staleMillis);
		} else {
			if (includes.isEmpty()) {
				includes.add("**/*.java");
			}
			scanner = new StaleSourceScanner(staleMillis, includes, excludes);
		}

		return scanner;
	}

	protected SourceInclusionScanner getSourceInclusionScanner(
			String inputFileEnding) {
		SourceInclusionScanner scanner = null;

		if (includes.isEmpty() && excludes.isEmpty()) {
			includes = Collections.singleton("**/*." + inputFileEnding);
			scanner = new SimpleSourceInclusionScanner(includes,
					Collections.EMPTY_SET);
		} else {
			if (includes.isEmpty()) {
				includes.add("**/*." + inputFileEnding);
			}
			scanner = new SimpleSourceInclusionScanner(includes, excludes);
		}

		return scanner;
	}

	protected List<String> getPdeCompileSourceRoots() throws MojoExecutionException {
		ArrayList<String> roots = new ArrayList<String>();
		for (File folder : outputJar.getSourceFolders()) {
			try {
				roots.add(folder.getCanonicalPath());
			} catch (IOException e) {
				throw new MojoExecutionException("Unexpected IOException", e);
			}
		}
		return roots;
	}

	@Override
	protected CompilerConfiguration getCompilerConfiguration(List<String> compileSourceRoots) throws MojoExecutionException {
		CompilerConfiguration compilerConfiguration = super.getCompilerConfiguration(compileSourceRoots);
		if (usePdeSourceRoots) {
			Properties props = pdeProject.getBuildProperties();
			String encoding = props.getProperty("javacDefaultEncoding." + outputJar);
			if (encoding != null) {
				compilerConfiguration.setSourceEncoding(encoding);
			}
		}
		return compilerConfiguration;
	}
}
