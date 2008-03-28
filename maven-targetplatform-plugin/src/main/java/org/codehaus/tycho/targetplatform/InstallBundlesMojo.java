package org.codehaus.tycho.targetplatform;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.codehaus.tycho.TychoException;

/**
 * @goal install-bundles
 */
public class InstallBundlesMojo extends AbstractDeployBundlesMojo {

	/**
	 * @component
	 */
	private ArtifactInstaller installer;
	
	public void deployArtifact(Artifact artifact) throws TychoException {
		try {
			installer.install(artifact.getFile(), artifact, localRepository);
		} catch (ArtifactInstallationException e) {
			throw new TychoException("Could not install artifact", e);
		}
	}

}
