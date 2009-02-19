package org.codehaus.tycho.buildnumber;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * This mojo generates build qualifier according to the rules outlined in
 * http://help.eclipse.org/ganymede/topic/org.eclipse.pde.doc.user/tasks/pde_version_qualifiers.htm
 * 
 * 1. explicit -DforceContextQualifier
 * 2. forceContextQualifier from ${project.baseDir}/build.properties
 * 3. the tag that was used to fetch the bundle (only when using map file)
 * 4. a time stamp in the form YYYYMMDDHHMM (ie 200605121600)
 * 
 * The generated qualifier is assigned to buildQualifier project property 
 * (actual property name is configurable). 
 * 
 * Implementation guarantees that the same timestamp is used for all projects
 * in reactor build. Different projects can use different formats to expand
 * the timestamp, however (highly not recommended but possible).
 * 
 * @goal build-qualifier
 */
public class BuildQualifierMojo extends AbstractMojo {

	public static final String BUILD_QUALIFIER_PROPERTY = "buildQualifier";
	private static final String REACTOR_BUILD_TIMESTAMP_PROPERTY = "reactorBuildTimestampProperty";

	/**
	 * @parameter expression="${session}"
	 */
	private MavenSession session;

	/**
     * You can rename the buildQualifier property name to another property name if desired.
     *
     * @parameter expression="${maven.buildNumber.buildNumberPropertyName}" default-value="buildQualifier"
     */
    private String buildNumberPropertyName;

    /**
     * Specify a message format as specified by java.text.SimpleDateFormat.
     *
     * @parameter default-value="yyyyMMddHHmm"
     */
    private SimpleDateFormat format;

    /**
	 * @parameter expression="${project}"
	 * @required
     */
    private MavenProject project;

    /**
     * @parameter default-value="${project.baseDir}/build.properties"
     */
    private File buildPropertiesFile;

    /**
     * @parameter expression="${forceContextQualifier}"
     */
    private String forceContextQualifier;

    public void setFormat(String format) {
    	this.format = new SimpleDateFormat(format);
    }

	public void execute() throws MojoExecutionException, MojoFailureException {
		String qualifier = forceContextQualifier;
		
		if (qualifier == null) {
			qualifier = getBuildProperties().getProperty("forceContextQualifier");
		}

		if (qualifier == null) {
			Date timestamp = getSessionTimestamp();
			qualifier = format.format(timestamp);
			
		}

		project.getProperties().put(BUILD_QUALIFIER_PROPERTY, qualifier);
	}

	private Date getSessionTimestamp() {
		Date timestamp;
		String value = session.getExecutionProperties().getProperty(buildNumberPropertyName);
		if (value != null) {
			timestamp = new Date(Long.parseLong(value));
		} else {
			timestamp = new Date();
			session.getExecutionProperties().setProperty(REACTOR_BUILD_TIMESTAMP_PROPERTY, Long.toString(timestamp.getTime()));
		}
		return timestamp;
	}

	// TODO move to a helper, we must have ~100 implementations of this logic
	private Properties getBuildProperties() {
		Properties props = new Properties();
		try {
			if (buildPropertiesFile.canRead()) {
				InputStream is = new BufferedInputStream(new FileInputStream(buildPropertiesFile));
				try {
					props.load(is);
				} finally {
					is.close();
				}
			}
		} catch (IOException e) {
			getLog().warn("Exception reading build.properties file", e);
		}
		return props;
	}
}
