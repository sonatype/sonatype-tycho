package org.codehaus.tycho.targetplatform;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.tycho.CLITools;
import org.codehaus.tycho.TychoException;

/**
 * @goal deploy-bundles
 */
public class DeployBundlesMojo extends AbstractDeployBundlesMojo {

	/**
	 * @component
	 */
	private ArtifactDeployer deployer;
	
	/**
	 * 
	 * 
	 * @parameter expression="${remote}"
	 * @required
	 */
	private String remoteRepository;
	
	public void deployArtifact(Artifact artifact) throws TychoException {
		ArtifactRepository deploymentRepository = cliTools.createRemoteRepository(remoteRepository);
		
		try {
			deployer.deploy(artifact.getFile(), artifact, deploymentRepository, localRepository);
		} catch (ArtifactDeploymentException e) {
			throw new TychoException("Could not deploy artifact", e);
		}
	}

}
