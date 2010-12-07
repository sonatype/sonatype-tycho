package org.sonatype.tycho.plugins.p2.director;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.tycho.TargetEnvironment;
import org.sonatype.tycho.equinox.EquinoxServiceFactory;
import org.sonatype.tycho.p2.facade.RepositoryReferenceTool;
import org.sonatype.tycho.p2.tools.RepositoryReferences;
import org.sonatype.tycho.p2.tools.director.DirectorApplicationWrapper;

/**
 * @phase package
 * @goal materialize-products
 */
@SuppressWarnings( "nls" )
public final class DirectorMojo
    extends AbstractProductMojo
{
    /** @component */
    private EquinoxServiceFactory p2;

    /** @parameter default-value="DefaultProfile" */
    private String profile;

    /** @component */
    private RepositoryReferenceTool repositoryReferenceTool;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( getEnvironments().size() == 0 )
        {
            getLog().warn( "Cannot materialize products. Specify target-platform-configuration <environments/>." );
        }
        for ( Product product : getProductConfig().getProducts() )
        {
            for ( TargetEnvironment env : getEnvironments() )
            {
                final DirectorApplicationWrapper director = p2.getService( DirectorApplicationWrapper.class );
                int flags = RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE;
                RepositoryReferences sources =
                    repositoryReferenceTool.getVisibleRepositories( getProject(), getSession(), flags );

                File destination = getProductMaterializeDirectory( product, env );
                String rootFolder = product.getRootFolder();
                if ( rootFolder != null && rootFolder.length() > 0 )
                {
                    destination = new File( destination, rootFolder );
                }

                String metadataRepositoryURLs = toCommaSeparatedList( sources.getMetadataRepositories() );
                String artifactRepositoryURLs = toCommaSeparatedList( sources.getArtifactRepositories() );
                String[] args = new String[] { "-metadatarepository", metadataRepositoryURLs, //
                    "-artifactrepository", artifactRepositoryURLs, //
                    "-installIU", product.getId(), //
                    "-destination", destination.getAbsolutePath(), //
                    "-profile", profile, //
                    "-profileProperties", "org.eclipse.update.install.features=true", //
                    "-roaming", //
                    "-p2.os", env.getOs(), "-p2.ws", env.getWs(), "-p2.arch", env.getArch() };
                getLog().info( "Calling director with arguments: " + Arrays.toString( args ) );
                final Object result = director.run( args );
                if ( !DirectorApplicationWrapper.EXIT_OK.equals( result ) )
                {
                    throw new MojoFailureException( "P2 director return code was " + result );
                }
            }
        }
    }

    private String toCommaSeparatedList( List<URI> repositories )
    {
        if ( repositories.size() == 0 )
        {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for ( URI uri : repositories )
        {
            result.append( uri.toString() );
            result.append( ',' );
        }
        result.setLength( result.length() - 1 );
        return result.toString();
    }
}
