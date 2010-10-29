package org.codehaus.tycho;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.sonatype.tycho.classpath.ClasspathEntry;

public interface BundleProject
    extends TychoProject
{
    public List<ClasspathEntry> getClasspath( MavenProject project );
    
	/**
	 * Returns the value of the specified attribute key in the project's
	 * MANIFEST, or null if the attribute was not found.
	 * 
	 * @param key
	 *            manifest attribute key
	 * @param project
	 *            associated maven project
	 * @return the String value of the specified attribute key, or null if not
	 *         found.
	 */
    public String getManifestValue(String key, MavenProject project);

}
