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
import java.io.FileFilter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.osgi.framework.BundleException;

import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.osgitools.OsgiStateController;

/**
 * @goal verify-configuration
 * @requiresProject false
 * @author awpyv
 * 
 */
public class ApplicationDependencyVerifierMojo extends AbstractMojo {

	/**
	 * @parameter expression="${pluginDirectory}"
	 * @required
	 */
	private File pluginDirectory;

	/**
	 * @parameter default-value="true"
	 * 
	 */
	private boolean failOnError;

	/** @parameter expression="${project.build.directory}" */
	private File outputDir;

	/** @component */
	private OsgiState state;

	public void execute() throws MojoExecutionException, MojoFailureException {

		File[] jars = pluginDirectory.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".jar")
						|| pathname.isDirectory();
			}
		});

		for (int i = 0; i < jars.length; i++) {
			File jar = jars[i];
			try {
				state.addBundle(jar);
			} catch (BundleException e) {
				getLog().error("Error adding bundle " + jar);
				throw new MojoExecutionException("Error adding bundle", e);
			}
		}
		state.resolveState();

		boolean errorsFound = false;

//		BundleDescription[] bundles = state.getState().getBundles();
//		for (int i = 0; i < bundles.length; i++) {
//			BundleDescription bundle = bundles[i];
//			ResolverError[] errors = state.getState().getResolverErrors(bundle);
//			if (errors.length > 0) {
//				getLog()
//						.error("Errors for bundle: " + bundle.getSymbolicName());
//				errorsFound = true;
//				for (int j = 0; j < errors.length; j++) {
//					ResolverError error = errors[j];
//					getLog().error(error.toString());
//				}
//			}
//		}

		if (!errorsFound) {
			getLog().info("Configuration verified: no resolving errors");
		}

		if (errorsFound && failOnError) {
			throw new MojoFailureException(
					"Errors found while verifying installation");
		}

	}

}
