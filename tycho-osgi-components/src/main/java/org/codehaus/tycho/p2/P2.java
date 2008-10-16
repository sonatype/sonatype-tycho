package org.codehaus.tycho.p2;

import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.reactor.MavenExecutionException;


public interface P2 {
	static final String ROLE = P2.class.getName();

	String materializeTargetPlatform(String key, List<String> repositories, List<Artifact> rootIUs, Properties props) throws MavenExecutionException;

	/** 
	 * @returns local path to P2 runtime.
	 * @throws MavenExecutionException if P2 runtime cannot be located 
	 */
	String getP2RuntimeLocation() throws MavenExecutionException;
}
