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
     * Defines whether the artifacts of the included products, features, and bundles shall be
     * assembled into a p2 artifact repository. If <code>false</code>, only a p2 metadata repository
     * is created.
     * 
     * @parameter default-value="true"
     */
    private boolean createArtifactRepository;

    /**
     * Defines whether all transitive dependencies shall be included in the resulting repository. By
     * default, only features and bundles directly referenced in products and categories and their
     * "include" dependencies will be included in the result. To build a completely self-contained
     * repository, set this parameter to <code>true</code>.
     * 
     * @parameter default-value="false"
     */
    private boolean includeAllDependencies;

    /**
     * Defines whether the resulting p2 metadata should be compressed.
     * 
     * @parameter default-value="true"
     */
    private boolean compress;
    
    /**
     * Defines the name of the p2 repository. The default value is the project name.
     * 
     * @parameter default-value="${project.name}"
     */
    private String repositoryName;

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

            int flags = compress ? MirrorApplicationService.REPOSITORY_COMPRESS : 0;
            if ( includeAllDependencies )
            {
                flags = flags | MirrorApplicationService.INCLUDE_ALL_DEPENDENCIES;
            }
            if ( createArtifactRepository )
            {
                flags = flags | MirrorApplicationService.MIRROR_ARTIFACTS;
            }

            MirrorApplicationService mirrorApp = p2.getService( MirrorApplicationService.class );
            mirrorApp.mirror( sources, destination, rootIUs, getBuildContext(), flags, repositoryName );
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
