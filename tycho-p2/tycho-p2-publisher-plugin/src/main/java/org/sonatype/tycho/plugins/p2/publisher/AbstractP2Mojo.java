package org.sonatype.tycho.plugins.p2.publisher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.osgitools.EclipseRepositoryProject;
import org.sonatype.tycho.p2.facade.RepositoryReferenceTool;
import org.sonatype.tycho.p2.tools.BuildContext;
import org.sonatype.tycho.p2.tools.RepositoryReferences;
import org.sonatype.tycho.p2.tools.TargetEnvironment;

public abstract class AbstractP2Mojo
    extends AbstractMojo
{

    protected static final String PUBLISHED_ROOT_IUS = AbstractPublishMojo.class.getName() + "/publishedRootIUs";

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

    /** @component */
    private RepositoryReferenceTool repositoryReferenceTool;

    protected MavenProject getProject()
    {
        return project;
    }

    protected MavenSession getSession()
    {
        return session;
    }

    protected String getQualifier()
    {
        return qualifier;
    }

    protected File getBuildDirectory()
    {
        return new File( getProject().getBuild().getDirectory() );
    }

    protected EclipseRepositoryProject getEclipseRepositoryProject()
    {
        return (EclipseRepositoryProject) getTychoProjectFacet();
    }

    protected TychoProject getTychoProjectFacet()
    {
        return getTychoProjectFacet( project.getPackaging() );
    }

    private TychoProject getTychoProjectFacet( String packaging )
    {
        TychoProject facet;
        try
        {
            facet = (TychoProject) session.lookup( TychoProject.class.getName(), packaging );
        }
        catch ( ComponentLookupException e )
        {
            throw new IllegalStateException( "Could not lookup required component", e );
        }
        return facet;
    }

    protected TargetPlatform getTargetPlatform()
    {
        return getTychoProjectFacet().getTargetPlatform( project );
    }

    protected TargetPlatformConfiguration getTargetPlatformConfiguration()
    {
        TargetPlatformConfiguration configuration =
            (TargetPlatformConfiguration) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION );

        if ( configuration == null )
        {
            throw new IllegalStateException(
                                             "Project build target platform configuration has not been initialized properly." );
        }

        return configuration;
    }

    protected RepositoryReferences getVisibleRepositories( boolean includePublisherResults )
        throws MojoExecutionException, MojoFailureException
    {
        int flags = includePublisherResults ? RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE : 0;
        return repositoryReferenceTool.getVisibleRepositories( getProject(), getSession(), flags );
    }

    protected File getAssemblyRepositoryLocation()
    {
        return new File( getBuildDirectory(), "repository" );
    }

    protected BuildContext getBuildContext()
    {
        return new BuildContext( getQualifier(), getEnvironmentsForFacade(), getBuildDirectory() );
    }

    /**
     * Returns the configured environments in a format suitable for the p2 tools facade.
     */
    private List<TargetEnvironment> getEnvironmentsForFacade()
    {
        // TODO use shared class everywhere?
        // TODO insert currently running environment if none is specified explicitly? (for details see TYCHO-529)

        List<org.codehaus.tycho.TargetEnvironment> original = getTargetPlatformConfiguration().getEnvironments();
        List<TargetEnvironment> converted = new ArrayList<TargetEnvironment>( original.size() );
        for ( org.codehaus.tycho.TargetEnvironment env : original )
        {
            converted.add( new TargetEnvironment( env.getWs(), env.getOs(), env.getArch() ) );
        }
        return converted;
    }
}
