package org.eclipse.tycho.buildversion;

import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.TychoProject;
import org.eclipse.tycho.osgitools.DefaultReactorProject;

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
     * @component role="org.eclipse.tycho.TychoProject"
     */
    protected Map<String, TychoProject> projectTypes;

    protected String getOSGiVersion()
    {
        TychoProject projectType = projectTypes.get( packaging );
        if ( projectType == null )
        {
            return null;
        }

        return projectType.getArtifactKey( DefaultReactorProject.adapt( project ) ).getVersion();
    }

}
