package org.codehaus.tycho.osgitools;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.osgi.service.resolver.ResolverError;

@Component( role = OsgiDependencyVerifier.class )
public class DefaultOsgiDependencyVerifier extends AbstractLogEnabled implements
		OsgiDependencyVerifier {

	@Requirement
	private OsgiState state;

	public ResolverError[] resolve(File outputDir, File[] bundles) {
		for (int i = 0; i < bundles.length; i++) {
			File jar = bundles[i];
			try {
				state.addBundle(jar);
			} catch (Exception e) {
				getLogger().error("Error adding bundle " + jar);
			}
		}
		state.resolveState();

		ResolverError[] errors = null; // state.getRelevantErrors();

		return errors;
	}

	public ResolverError[] resolve(File outputDir, Artifact[] artifacts) {
		File[] files = new File[artifacts.length];
		for (int i = 0; i < files.length; i++) {
			files[i] = artifacts[i].getFile();
		}
		return resolve(outputDir, files);
	}

}
