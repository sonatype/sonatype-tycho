package org.codehaus.tycho.osgitools;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.osgi.service.resolver.ResolverError;

/**
 * @plexus.component role="org.codehaus.tycho.osgitools.OsgiDependencyVerifier"
 *                   hint="default"
 */
public class DefaultOsgiDependencyVerifier extends AbstractLogEnabled implements
		OsgiDependencyVerifier {

	public ResolverError[] resolve(File[] bundles) {
		OsgiStateController state = new OsgiStateController();
		for (int i = 0; i < bundles.length; i++) {
			File jar = bundles[i];
			try {
				state.addBundle(jar);
			} catch (Exception e) {
				getLogger().error("Error adding bundle " + jar);
			}
		}
		state.resolveState();

		ResolverError[] errors = state.getRelevantErrors();

		return errors;
	}

	public ResolverError[] resolve(Artifact[] artifacts) {
		File[] files = new File[artifacts.length];
		for (int i = 0; i < files.length; i++) {
			files[i] = artifacts[i].getFile();
		}
		return resolve(files);
	}

}
