package org.codehaus.tycho.buildversion;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.tycho.TychoProject;

public abstract class AbstractVersionMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter default-value="${project.packaging}"
     * @required
     * @readonly
     */
    protected String packaging;

    /**
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    protected String getOSGiVersion() throws MojoFailureException
    {
        TychoProject dependencyReader;
        try
        {
            dependencyReader = (TychoProject) session.lookup( TychoProject.class.getName(), packaging );
        }
        catch ( ComponentLookupException e )
        {
            throw new MojoFailureException( "Could not locate required component", e );
        }

        return dependencyReader.getArtifactKey( project ).getVersion();
    }

}
