package org.sonatype.tycho.plugins.p2.publisher;

import java.io.File;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.sonatype.tycho.equinox.EquinoxServiceFactory;
import org.sonatype.tycho.p2.tools.FacadeException;
import org.sonatype.tycho.p2.tools.RepositoryReferences;
import org.sonatype.tycho.p2.tools.mirroring.MirrorApplicationService;

/**
 * @goal assemble-repository
 */
public class AssembleRepositoryMojo
    extends AbstractP2Mojo
{
    /**
     * @parameter default-value="true"
     */
    private boolean compress;

    /** @component */
    private EquinoxServiceFactory p2;

    @SuppressWarnings( "unchecked" )
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            File destination = getAssemblyRepositoryLocation();
            destination.mkdirs();

            Collection<Object> rootIUs = (Collection<Object>) getProject().getContextValue( PUBLISHED_ROOT_IUS );
            if ( rootIUs == null || rootIUs.size() == 0 )
            {
                throw new MojoFailureException( "No content specified for p2 repository" );
            }

            final MirrorApplicationService mirrorApp = p2.getService( MirrorApplicationService.class );
            RepositoryReferences sources = getVisibleRepositories( true );
            int flags = compress ? MirrorApplicationService.REPOSITORY_COMPRESS : 0;
            mirrorApp.mirror( sources, destination, rootIUs, getBuildContext(), flags );
        }
        catch ( FacadeException e )
        {
            throw new MojoExecutionException( "Could not assemble p2 repository", e );
        }
    }
}
