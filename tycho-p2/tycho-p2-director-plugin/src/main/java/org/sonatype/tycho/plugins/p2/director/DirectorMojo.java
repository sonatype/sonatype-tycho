package org.sonatype.tycho.plugins.p2.director;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.p2.DirectorApplicationWrapper;
import org.codehaus.tycho.p2.MetadataSerializable;
import org.sonatype.tycho.osgi.EquinoxEmbedder;

/**
 * @phase package
 * @goal materialize-products
 */
@SuppressWarnings( "nls" )
public final class DirectorMojo
    extends AbstractProductMojo
{
    /** @component */
    private EquinoxEmbedder p2;

    /** @parameter default-value="DefaultProfile" */
    private String profile;

    /**
     * Build qualifier. Recommended way to set this parameter is using build-qualifier goal.
     * 
     * @parameter expression="${buildQualifier}"
     */
    private String qualifier;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( getEnvironments().size() == 0 )
        {
            getLog().warn( "Cannot materialize products. Specify target-platform-configuration <environments/>." );
        }
        File repositoryLocation = new File( getBuildDirectory(), "repository" );
        for ( Product product : getProductConfig().getProducts() )
        {
            for ( TargetEnvironment env : getEnvironments() )
            {
                final DirectorApplicationWrapper director = p2.getService( DirectorApplicationWrapper.class );
                try
                {
                    String targetRepositoryUrl =
                        materializeRepository( getTargetPlatform().getP2MetadataSerializable(), getBuildDirectory() );
                    File destination = getProductMaterializeDirectory( product, env );
                    String[] args =
                        new String[] {
                            "-metadatarepository",
                            targetRepositoryUrl + "," + repositoryLocation.toURI().toString(), //
                            "-artifactrepository",
                            repositoryLocation.toURI().toString() + "," + getSession().getLocalRepository().getUrl(), //
                            "-installIU", product.getId(), //
                            "-destination", destination.toString(), //
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
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Failed to materialize target platform repository", e );
                }
            }
        }
    }

    private String materializeRepository( MetadataSerializable metadataRepositorySerializable, File targetDirectory )
        throws IOException
    {
        metadataRepositorySerializable.replaceBuildQualifier( qualifier );
        File repositoryLocation = new File( targetDirectory, "targetMetadataRepository" );
        repositoryLocation.mkdirs();
        FileOutputStream stream = new FileOutputStream( new File( repositoryLocation, "content.xml" ) );
        try
        {
            metadataRepositorySerializable.serialize( stream );
        }
        finally
        {
            stream.close();
        }
        return repositoryLocation.toURI().toURL().toExternalForm();
    }
}
