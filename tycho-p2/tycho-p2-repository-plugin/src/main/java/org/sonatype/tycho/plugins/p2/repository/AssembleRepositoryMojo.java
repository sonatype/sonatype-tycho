package org.sonatype.tycho.plugins.p2.repository;

import java.io.File;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.tycho.TychoConstants;
import org.sonatype.tycho.equinox.EquinoxServiceFactory;
import org.sonatype.tycho.p2.facade.RepositoryReferenceTool;
import org.sonatype.tycho.p2.tools.FacadeException;
import org.sonatype.tycho.p2.tools.RepositoryReferences;
import org.sonatype.tycho.p2.tools.mirroring.MirrorApplicationService;

/**
 * @goal assemble-repository
 */
public class AssembleRepositoryMojo
    extends AbstractRepositoryMojo
{
    /**
     * @parameter default-value="true"
     */
    private boolean compress;

    /** @component */
    private RepositoryReferenceTool repositoryReferenceTool;

    /** @component */
    private EquinoxServiceFactory p2;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            File destination = getAssemblyRepositoryLocation();
            destination.mkdirs();

            Collection<?> rootIUs =
                (Collection<?>) getProject().getContextValue( TychoConstants.CTX_PUBLISHED_ROOT_IUS );
            if ( rootIUs == null || rootIUs.size() == 0 )
            {
                throw new MojoFailureException( "No content specified for p2 repository" );
            }

            RepositoryReferences sources = getVisibleRepositories();
            
            MirrorApplicationService mirrorApp = p2.getService( MirrorApplicationService.class );
            int flags = compress ? MirrorApplicationService.REPOSITORY_COMPRESS : 0;
            mirrorApp.mirror( sources, destination, rootIUs, getBuildContext(), flags );
        }
        catch ( FacadeException e )
        {
            throw new MojoExecutionException( "Could not assemble p2 repository", e );
        }
    }

    protected RepositoryReferences getVisibleRepositories()
        throws MojoExecutionException, MojoFailureException
    {
        int flags = RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE;
        return repositoryReferenceTool.getVisibleRepositories( getProject(), getSession(), flags );
    }
}
