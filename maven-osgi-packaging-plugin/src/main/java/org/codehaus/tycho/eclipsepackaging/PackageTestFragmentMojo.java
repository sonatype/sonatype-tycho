package org.codehaus.tycho.eclipsepackaging;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.tycho.osgitools.BundleFile;
import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.osgitools.OsgiStateController;

/**
 * Creates a jar-based plugin and attaches it as an artifact
 * 
 * @goal package-test-fragment
 * @phase package
 * @requiresProject true
 */
public class PackageTestFragmentMojo extends AbstractMojo {

	/**
	 * @parameter expression="${project.build.testOutputDirectory}"
	 * @required
	 */
	protected File testOutputDirectory;

	/**
	 * @parameter expression="${project}"
	 * @required
	 */
	protected MavenProject project;

	/**
	 * The Jar archiver.
	 * 
	 * parameter
	 * expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
	 * required
	 */
	private JarArchiver jarArchiver = new JarArchiver();

	/**
	 * Name of the generated JAR.
	 * 
	 * @parameter alias="jarName" expression="${project.build.finalName}-tests"
	 * @required
	 */
	protected String finalName;

	/**
	 * @parameter expression="${component.org.apache.maven.project.MavenProjectHelper}
	 */
	protected MavenProjectHelper projectHelper;

	/**
	 * @parameter expression="${maven.test.skip}" default-value="false"
	 */
	private boolean skip;

	/** @parameter expression="${project.build.directory}" */
	private File outputDir;

	/** @component */
	private OsgiState state;

	public void execute() throws MojoExecutionException {
		createPlugin();
	}

	private void createPlugin() throws MojoExecutionException {
		if (skip || !testOutputDirectory.exists()) {
			return;
		}

		try {
            Properties buildProperties = new Properties();
            buildProperties.load(new FileInputStream(new File(project
					.getBasedir(), "build.properties")));

			File fragment = new File(project.getBuild().getDirectory(),
					finalName + ".jar");
			if (fragment.exists()) {
				fragment.delete();
			}

			jarArchiver.addDirectory(testOutputDirectory,
					Util.DEFAULT_INCLUDES, Util.DEFAULT_EXCLUDES);
			File fragmentManifest = createFragmentManifest();
			jarArchiver.setManifest(fragmentManifest);
			jarArchiver.setDestFile(fragment);
			jarArchiver.createArchive();
			projectHelper.attachArtifact(project, "jar", "tests", fragment);
		} catch (Exception e) {
			throw new MojoExecutionException("", e);
		}
	}

	private File createFragmentManifest() throws IOException {
		File file = new File(project.getBasedir(), "META-INF/MANIFEST.MF");
		BundleFile bundle = new BundleFile(state.loadManifest(file), file);
		
		Manifest mft = new Manifest();
		Attributes attr = mft.getMainAttributes();
		attr.putValue("Manifest-Version", "1.0");
		attr.putValue("Bundle-ManifestVersion", "2");
		attr.putValue("Bundle-Name", bundle.getName() + " - Tests");
		attr.putValue("Bundle-SymbolicName", bundle.getSymbolicName() + ".tests");
		attr.putValue("Bundle-Version", bundle.getVersion());
		attr.putValue("Fragment-Host", bundle.getSymbolicName());
		File result = new File(project.getBuild().getDirectory(),
				"test-manifest.mf");
		FileOutputStream fos = new FileOutputStream(result);
		mft.write(fos);
		fos.close();
		return result;
	}
}
