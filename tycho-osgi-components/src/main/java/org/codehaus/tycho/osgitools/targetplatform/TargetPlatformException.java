package org.codehaus.tycho.osgitools.targetplatform;

import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;

public class TargetPlatformException extends RuntimeException {

	public TargetPlatformException(String string, Map<Artifact, Exception> exceptions) {
		// TODO Auto-generated constructor stub
	}

	public TargetPlatformException(String string, ArtifactNotFoundException e) {
		// TODO Auto-generated constructor stub
	}

	public TargetPlatformException(String string) {
		// TODO Auto-generated constructor stub
	}

}
