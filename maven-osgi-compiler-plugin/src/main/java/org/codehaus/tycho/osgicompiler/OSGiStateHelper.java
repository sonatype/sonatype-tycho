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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.osgitools.OsgiStateController;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ResolverError;

public class OSGiStateHelper {

	private MavenProject project;
	private Log log;
	private List/*<Artifact>*/ pluginArtifacts;
	private BundleDescription thisBundle;
	private File storage;
	private BundleStorageManager bsm;

    /**
     * @todo make a plexus component out of this
     *
     * @param project
     * @param log
     * @param pluginArtifacts
     * @param storage
     */
    public OSGiStateHelper(MavenProject project, Log log,
			List/*<Artifact>*/ pluginArtifacts, File storage) {
		this.project = project;
		this.log = log;
		this.pluginArtifacts = pluginArtifacts;
		this.storage = storage;
	}

	public Log getLog() {
		return log;
	}

	/**
	 * Check if the bundle exports the org.osgi.framework package.
	 * 
	 * @param bd
	 * @return
	 */
	private boolean isSystemBundle(BundleDescription bd) {
		ExportPackageDescription[] exports = bd.getExportPackages();
		for (int i = 0; i < exports.length; i++) {
			if ("org.osgi.framework".equals(exports[i].getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * TODO resolve this from the repository
	 * @return
	 * @throws MojoFailureException
	 */
	private Artifact getOSGIArtifact() throws MojoFailureException {
		for (Iterator it = pluginArtifacts.iterator(); it.hasNext(); ) {
			Artifact a= (Artifact) it.next();
			if (a.getGroupId().equals("org.eclipse.osgi")
					&& a.getArtifactId().equals("org.eclipse.osgi")) {
				return a;
			}
		}
		throw new MojoFailureException("OSGi bundle not found");
	}

	public OsgiStateController createOSGiState(Collection/*<Artifact>*/ artifacts,
			boolean failOnError) throws MojoExecutionException {
		File baseDirectory = project.getBasedir();
		File outputDirectory = new File(project.getBuild().getOutputDirectory());
		File manifest = new File(baseDirectory, "META-INF/MANIFEST.MF");
		OsgiStateController state = new OsgiStateController(new File(project.getBuild().getDirectory()));
		try {
			if (manifest.exists()) {
				if (!outputDirectory.exists()) {
					outputDirectory.mkdirs();
				}
				getLog().debug("Adding META-INF/MANIFEST.MF");
				thisBundle = state.addBundle(manifest, baseDirectory);
			} else {

				File bundle = new File(project.getBuild().getDirectory(),
						project.getBuild().getFinalName() + ".jar");
				getLog().debug("Adding " + bundle);
				thisBundle = state.addBundle(bundle);
			}
		} catch (Exception e) {
			getLog()
					.warn(
							"Could not add project as a bundle. Not a plugin project ?");
			getLog().debug(e);
		}

		bsm = new BundleStorageManager(storage);
		boolean systemBundleFound = false;
		for (Iterator it = artifacts.iterator(); it.hasNext(); ) {
			Artifact a = (Artifact) it.next();
			try {
				String path = bsm.addBundle(a.getFile());
				BundleDescription bundle = state.addBundle(new File(path));
				systemBundleFound |= isSystemBundle(bundle);
				getLog().info("Added artifact to osgi state: " + a.getFile());
			} catch (Exception e) {
				getLog().warn("Could not add artifact " + a.getFile());
				getLog().debug(e);
			}
		}

		try {
			if (!systemBundleFound) {
				state.addBundle(getOSGIArtifact().getFile());
			}
		} catch (Exception e) {
			throw new MojoExecutionException("Could not find system artifact!", e);
		}

		state.resolveState();
		
		ResolverError[] errors = state.getRelevantErrors(thisBundle);
		for (int i = 0; i < errors.length; i++) {
			ResolverError error = errors[i];
			getLog().error("Bundle "  + error.getBundle().getSymbolicName() + " - " + error.toString());
		}
		
		if (errors.length > 0 && failOnError)
		{
			throw new MojoExecutionException(
					"Errors found while verifying installation " + thisBundle.toString());
		}

		return state;
	}

	public BundleDescription getThisBundle() {
		return thisBundle;
	}

	public BundleStorageManager getEPM() {
		return bsm;
	}
}
