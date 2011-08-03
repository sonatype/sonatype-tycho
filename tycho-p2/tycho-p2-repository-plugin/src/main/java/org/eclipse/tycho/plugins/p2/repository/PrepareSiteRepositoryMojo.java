package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

/**
 * @goal prepare-site-repository
 */
public class PrepareSiteRepositoryMojo extends AbstractRepositoryMojo implements LogEnabled {

    private Logger logger;

    /**
     * Directory containing the generated project sites and report distributions.
     * 
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new MojoFailureException("failed to create output directory");
            }
        }

        try {
            FileUtils.copyDirectoryStructure(getAssemblyRepositoryLocation(), outputDirectory);
        } catch (IOException e) {
            throw new MojoFailureException("error copying respository resources to output directory");
        }
    }

}
