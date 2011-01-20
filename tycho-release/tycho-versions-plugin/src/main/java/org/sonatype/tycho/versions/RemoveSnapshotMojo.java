package org.sonatype.tycho.versions;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.sonatype.tycho.versions.engine.VersionsEngine;

/**
 * Removes the versions -SNAPSHOT/.qualifier part.
 * 
 * @author Beat Strasser
 * @goal remove-snapshot
 * @aggregator
 * @requiresProject true
 * @requiresDirectInvocation true
 */
public class RemoveSnapshotMojo extends AbstractMojo
{

    /**
     * Comma separated list of artifact ids to set the new version to.
     * <p/>
     * By default, the new version will be set on the current project and all
     * references to the project, including all <parent/> elements if the
     * project is a parent pom.
     * 
     * @parameter expression="${artifacts}"
     *            default-value="${project.artifactId}"
     */
    private String artifacts;

    /**
     * @parameter expression="${allModules}" default-value="false"
     */
    private boolean allModules;

    /**
     * @parameter expression="${session}"
     */
    protected MavenSession session;

    /**
     * @component
     */
    private VersionsEngine engine;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            engine.addBasedir( session.getCurrentProject().getBasedir() );

            if (allModules)
            {
                engine.addToReleaseVersionChangesForAllModules();
            }
            else
            {
                StringTokenizer st = new StringTokenizer( artifacts, "," );
                while (st.hasMoreTokens())
                {
                    String artifactId = st.nextToken().trim();
                    engine.addToReleaseVersionChange( artifactId );
                }
            }

            engine.apply();
        }
        catch (IOException e)
        {
            throw new MojoExecutionException( "Could not set version", e );
        }
    }

}
