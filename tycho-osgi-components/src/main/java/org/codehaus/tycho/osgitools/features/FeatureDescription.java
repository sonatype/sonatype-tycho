package org.codehaus.tycho.osgitools.features;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.model.Feature;
import org.osgi.framework.Version;

public interface FeatureDescription {

	String getId();

	Version getVersion();

	File getLocation();

	Feature getFeature();

	void setUserProperty(String key, Object value);

	Object getUserProperty(String key);

	void setMavenProject(MavenProject project);

	MavenProject getMavenProject();

}
