package org.sonatype.tycho.plugins.p2;

import java.io.File;
import java.io.IOException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.sonatype.tycho.p2.repository.LocalTychoRepositoryIndex;

/**
 * @goal update-local-index
 */
public class UpdateLocalIndexMojo
    extends AbstractMojo
{
    /** @parameter expression="${session}" */
    private MavenSession session;

    /** @parameter expression="${project}" */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException,
            MojoFailureException
    {
        File location = new File( session.getLocalRepository().getBasedir() );

        try
        {
            LocalTychoRepositoryIndex.addProject( location, project.getGroupId(), project.getArtifactId(), project
                .getArtifact().getVersion() );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not update local repository index", e );
        }
    }

}
