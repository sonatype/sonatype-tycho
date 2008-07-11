package org.codehaus.tycho.p2;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

/**
 * @plexus.component role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
 * 		role-hint="p2"
 */
public class P2ArtifactRepositoryLayout implements ArtifactRepositoryLayout {

	public String pathOf(Artifact artifact) {
		return ".p2-ignore";
	}

	public String pathOfLocalRepositoryMetadata(ArtifactMetadata metadata, ArtifactRepository repository) {
		return ".p2-ignore";
	}

	public String pathOfRemoteRepositoryMetadata(ArtifactMetadata metadata) {
		return ".p2-ignore";
	}

}
