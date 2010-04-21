package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * @goal update-site-packaging
 */
public class PackageUpdateSiteMojo extends AbstractMojo {
    /**
     * @parameter expression="${project}"
     * @required
     */
    protected MavenProject project;

    /**
     * Generated update site location (must match update-site mojo configuration)
     * 
     * @parameter expression="${project.build.directory}/site"
     */
    private File target;

    /**
     * If true, create site zip file. If false (the default), do not create site zip file.
     * 
     * Please note that due to limitations of maven-deploy-plugin and maven-install-plugin
     * plugins, site.zip will always be set as project's main artifact and will be 
     * deployed/installed to maven repository as a result. However, site.zip will only
     * contain site.xml file if this parameter is set to false (the default), so overhead
     * will be minimal. 
     * 
     * @parameter default-value="false"
     */
    private boolean archiveSite;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (target == null || !target.isDirectory()) {
            throw new MojoExecutionException(
                    "Update site folder does not exist at: " + target != null ? target.getAbsolutePath() : "null");
        }

        ZipArchiver zipper = new ZipArchiver();
        File destFile = new File(target.getParentFile(), "site.zip");
        try {
            if (archiveSite) {
                zipper.addDirectory(target);
            } else {
                zipper.addFile(new File(target, "site.xml"), "site.xml");
            }
            zipper.setDestFile(destFile);
            zipper.createArchive();
        } catch (IOException e) {
            throw new MojoExecutionException("Error packing update site", e);
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Error packing update site", e);
        }
        project.getArtifact().setFile(destFile);
    }

}
