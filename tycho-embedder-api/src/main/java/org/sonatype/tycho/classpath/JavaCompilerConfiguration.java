package org.sonatype.tycho.classpath;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.sonatype.tycho.runtime.Adaptable;

/**
 * Computes and returns Tycho java compiler configuration, i.e. compile or test-compile mojos of
 * org.sonatype.tycho:maven-osgi-compiler-plugin.
 * 
 * Use {@link Adaptable#getAdapter(Class)} on compile or test-compile mojos to get instance of this intergace
 * 
 * @author igor
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface JavaCompilerConfiguration
{
    /**
     * Computes and returns compile classpath of a Tycho eclipse-plugin or eclipse-test-plugin project.
     */
    public List<ClasspathEntry> getClasspath()
        throws MojoExecutionException;

    /**
     * Computes and returns sourcepath of a Tycho eclipse-plugin or eclipse-test-plugin project.
     */
    public List<SourcepathEntry> getSourcepath()
        throws MojoExecutionException;

    public String getSourceLevel()
        throws MojoExecutionException;

    public String getTargetLevel()
        throws MojoExecutionException;

    public String getExecutionEnvironment()
        throws MojoExecutionException;
}
