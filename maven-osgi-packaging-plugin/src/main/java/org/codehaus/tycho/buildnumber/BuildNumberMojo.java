package org.codehaus.tycho.buildnumber;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * This mojo provides (sane) timestamp based build number. Unlike buildnumber-maven-plugin,
 * it guarantees that the same timestamp is used for all modules. 
 * 
 * @goal timestamp
 */
public class BuildNumberMojo extends AbstractMojo {

	/**
	 * @parameter expression="${session}"
	 */
	private MavenSession session;

	/**
     * You can rename the buildNumber property name to another property name if desired.
     *
     * @parameter expression="${maven.buildNumber.buildNumberPropertyName}" default-value="buildNumber"
     */
    private String buildNumberPropertyName;

    /**
     * Specify a message as specified by java.text.SimpleDateFormat.
     *
     * @parameter default-value="yyyyMMdd-HHmm"
     */
    private SimpleDateFormat format;
    
    public void setFormat(String format) {
    	this.format = new SimpleDateFormat(format);
    }

	public void execute() throws MojoExecutionException, MojoFailureException {
		String value = session.getExecutionProperties().getProperty(buildNumberPropertyName);
		if (value == null) {
			value = format.format(new Date());
			session.getExecutionProperties().setProperty(buildNumberPropertyName, value);
		}
	}

}
