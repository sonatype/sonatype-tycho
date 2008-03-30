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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.tycho.osgicompiler.ClasspathComputer3_0.ClasspathElement;
import org.codehaus.tycho.osgicompiler.copied.AbstractCompilerMojo;
import org.codehaus.tycho.osgicompiler.copied.CompilationFailureException;
import org.codehaus.tycho.osgitools.OsgiStateController;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;

public abstract class AbstractOsgiCompilerMojo extends AbstractCompilerMojo {

	/**
	 * @parameter expression="${failOnError}" default-value="true"
	 */
	private boolean failOnError;

	/**
	 * A temporary directory to extract embedded jars
	 * 
	 * @parameter expression="${project.build.directory}/plugins"
	 * @required
	 * @readonly
	 */
	private File storage;

	/**
	 * @parameter expression="${plugin.artifacts}"
	 * @required
	 * @readonly
	 */
	private List pluginArtifacts;

	/**
	 * @parameter expression="${project}"
	 */
	private MavenProject project;

	private List/* <String> */classPathElements = new ArrayList/* <String> */();

	public void execute() throws MojoExecutionException,
			CompilationFailureException {
		classPathElements = computeClassPath(project.getBasedir(),
				getCompileArtifacts());

		super.execute();

		projectArtifact.setFile(getOutputDirectory());
	}

	protected abstract List/* <Artifact> */getCompileArtifacts();

	protected List/* <String> */getClasspathElements() {
		return classPathElements;
	}

	public List/* <String> */ computeClassPath(File baseDir,
			List/* <Artifact> */ artifacts) throws MojoExecutionException {

		OSGiStateHelper helper = new OSGiStateHelper(project, getLog(), pluginArtifacts, storage);
		OsgiStateController state = helper.createOSGiState(artifacts, failOnError);

		ClasspathComputer3_0 cc = new ClasspathComputer3_0(state, helper.getEPM());
		BundleDescription thisBundle = helper.getThisBundle();

		ResolverError[] bundleErrors = state.getState().getResolverErrors(thisBundle);
		
		List classpath = cc.getClasspath(thisBundle);
		List result = new ArrayList(classpath.size());
		for (Iterator it = classpath.iterator(); it.hasNext();) {
			ClasspathElement cp = (ClasspathElement) it.next();
			result.add(cp.getPath() + cp.getAccessRules());
		}
		result.add(getOutputDirectory() + "[+**/*]");
		
		
		
		return result;

	}

	/**
	 * Project artifacts.
	 * 
	 * @parameter expression="${project.artifact}"
	 * @required
	 * @readonly
	 * @todo this is an export variable, really
	 */
	private Artifact projectArtifact;

	/**
	 * A list of inclusion filters for the compiler.
	 * 
	 * @parameter
	 */
	private Set includes = new HashSet();

	/**
	 * A list of exclusion filters for the compiler.
	 * 
	 * @parameter
	 */
	private Set excludes = new HashSet();

	protected abstract List getCompileSourceRoots();

	protected abstract File getOutputDirectory();

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

}
