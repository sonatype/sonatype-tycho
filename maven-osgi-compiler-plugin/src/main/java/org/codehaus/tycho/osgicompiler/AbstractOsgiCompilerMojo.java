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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.tycho.osgicompiler.ClasspathComputer3_0.ClasspathElement;
import org.codehaus.tycho.osgicompiler.copied.AbstractCompilerMojo;
import org.codehaus.tycho.osgicompiler.copied.CompilationFailureException;
import org.codehaus.tycho.osgitools.OsgiState;
import org.eclipse.osgi.service.resolver.BundleDescription;

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

	/** @component */
	private OsgiState state;

	private ArrayList<String> classpathElements;

	public void execute() throws MojoExecutionException, CompilationFailureException {
		if (usePdeSourceRoots) {
			getLog().info("Using compile source roots from build.properties");
		}
		List<String> libraries = getLibraries();

//		List<String> compileSourceRoots = removeEmptyCompileSourceRoots(getCompileSourceRoots());
//
//		if (compileSourceRoots.isEmpty()) {
//			getLog().info("No sources to compile");
//
//			return;
//		}
		
		for (String library : libraries) {
			this.libraryName = library;
			super.execute();
			classpathElements.add(getOutputDirectory().getAbsolutePath());
		}
		
//		projectArtifact.setFile(getOutputDirectory());
	}

	private List<String> getLibraries() throws MojoExecutionException {
		Properties props = getBuildProperties();
		List<String> availableSources = new ArrayList<String>();
		for (Object objKey : props.keySet()) {
			String key = objKey.toString();
			if(!key.startsWith("source.")) {
				continue;
			}
			String libraryName = key.substring(7);
			availableSources.add(libraryName);
		}
		
		List<String> libraries = new ArrayList<String>();
		
		String jarsOrder = props.getProperty("jars.compile.order");
		if(jarsOrder != null && !"".equals(jarsOrder)) {
			String[] jars = jarsOrder.split(",");
			
			libraries.addAll(Arrays.asList(jars));
		}
		
		for (String source : availableSources) {
			if(!libraries.contains(source)) {
				libraries.add(source);
			}
		}
		
		return libraries;
	}

	public List<String> getClasspathElements() {
		if(classpathElements == null) {
			BundleDescription thisBundle = state.getBundleDescription(project);
	
			ClasspathComputer3_0 cc = new ClasspathComputer3_0(thisBundle, state, storage);
	
			List<ClasspathElement> classpath = cc.getClasspath();
			classpathElements = new ArrayList<String>(classpath.size());
			for (Iterator<ClasspathElement> it = classpath.iterator(); it.hasNext();) {
				ClasspathElement cp = (ClasspathElement) it.next();
				classpathElements.add(cp.getPath() + cp.getAccessRules());
			}
		}

		return classpathElements;
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
	private Set<String> includes = new HashSet<String>();

	/**
	 * A list of exclusion filters for the compiler.
	 * 
	 * @parameter
	 */
	private Set<String> excludes = new HashSet<String>();

	private Properties buildProperties;

	/**
	 * TODO
	 */
	private String libraryName = ".";

	protected final List<String> getCompileSourceRoots() throws MojoExecutionException {
		return usePdeSourceRoots? getPdeCompileSourceRoots(): getConfiguredCompileSourceRoots();
	}

	protected abstract List<String> getConfiguredCompileSourceRoots();

	protected final File getOutputDirectory() {
		return ".".equals(libraryName) ? getConfiguredOutputDirectory() : new File(getConfiguredOutputDirectory().getParentFile(), libraryName.substring(0, libraryName.length() - 4) + "-classes");
	}

	protected abstract File getConfiguredOutputDirectory();

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
		Properties bp = getBuildProperties();
		// only consider primary jar for now
		ArrayList<String> sources = new ArrayList<String>();
		String sourcesRaw = bp.getProperty("source." + libraryName);
		if (sourcesRaw != null && sourcesRaw.length() > 0) {
			StringTokenizer st = new StringTokenizer(sourcesRaw, ",");
			while (st.hasMoreTokens()) {
				String sourcePath = st.nextToken();
				try {
					sources.add(new File(project.getBasedir(), sourcePath).getCanonicalPath());
				} catch (IOException e) {
					throw new MojoExecutionException("Unable to resolve source path " + sourcePath, e);
				}
			}
		}
		return sources;
	}

	private Properties getBuildProperties() throws MojoExecutionException {
		if(buildProperties == null) {
			File file = new File(project.getBasedir(), "build.properties");
			if (!file.canRead()) {
				throw new MojoExecutionException("Unable to read build.properties file");
			}
	
			buildProperties = new Properties();
			try {
				InputStream is = new FileInputStream(file);
				try {
					buildProperties.load(is);
				} finally {
					is.close();
				}
			} catch (IOException e) {
				throw new MojoExecutionException("Exception reading build.properties file", e);
			}
		}
		return buildProperties;
	}

}
