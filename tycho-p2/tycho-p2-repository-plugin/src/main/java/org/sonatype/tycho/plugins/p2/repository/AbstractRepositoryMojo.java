package org.sonatype.tycho.plugins.p2.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.utils.TychoProjectUtils;
import org.sonatype.tycho.p2.tools.BuildContext;
import org.sonatype.tycho.p2.tools.TargetEnvironment;

public abstract class AbstractRepositoryMojo
    extends AbstractMojo
{
    /** @parameter expression="${session}" */
    private MavenSession session;

    /** @parameter expression="${project}" */
    private MavenProject project;

    /**
     * Build qualifier. Recommended way to set this parameter is using build-qualifier goal.
     * 
     * @parameter expression="${buildQualifier}"
     */
    private String qualifier;

    protected MavenProject getProject()
    {
        return project;
    }

    protected MavenSession getSession()
    {
        return session;
    }

    protected File getBuildDirectory()
    {
        return new File( getProject().getBuild().getDirectory() );
    }

    protected BuildContext getBuildContext()
    {
        return new BuildContext( qualifier, getEnvironmentsForFacade(), getBuildDirectory() );
    }

    protected File getAssemblyRepositoryLocation()
    {
        return new File( getBuildDirectory(), "repository" );
    }

    /**
     * Returns the configured environments in a format suitable for the p2 tools facade.
     */
    private List<TargetEnvironment> getEnvironmentsForFacade()
    {
        // TODO use shared class everywhere?

        List<org.codehaus.tycho.TargetEnvironment> original =
            TychoProjectUtils.getTargetPlatformConfiguration( project ).getEnvironments();
        List<TargetEnvironment> converted = new ArrayList<TargetEnvironment>( original.size() );
        for ( org.codehaus.tycho.TargetEnvironment env : original )
        {
            converted.add( new TargetEnvironment( env.getWs(), env.getOs(), env.getArch() ) );
        }
        return converted;
    }
}
