package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.maven.DependenciesReader;

public abstract class AbstractDependenciesReader extends AbstractLogEnabled implements DependenciesReader {
	protected static final List<Dependency> NO_DEPENDENCIES = new ArrayList<Dependency>();

	
	protected Dependency newExternalDependency(String location, String groupId, String artifactId, String version) {
		File file = new File(location);
		if (!file.exists() || !file.isFile() || !file.canRead()) {
			getLogger().warn("Dependency at location " + location + " can not be represented in Maven model and will not be visible to non-OSGi aware Maven plugins");
			return null;
		}

		Dependency dependency = new Dependency();
		dependency.setArtifactId(artifactId);
		dependency.setGroupId(groupId);
		dependency.setVersion(version);
		dependency.setScope(Artifact.SCOPE_SYSTEM);
		dependency.setSystemPath(location);
		return dependency;
	}

	protected Dependency newProjectDependency(MavenProject otherProject) {
		if (otherProject == null) {
			return null;
		}

		Dependency dependency = new Dependency();
		dependency.setArtifactId(otherProject.getArtifactId());
		dependency.setGroupId(otherProject.getGroupId());
		dependency.setVersion(otherProject.getVersion());
		dependency.setScope(Artifact.SCOPE_PROVIDED);
		return dependency;
	}
	
}
