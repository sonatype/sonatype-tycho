package org.sonatype.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.tycho.eclipsepackaging.UpdateSiteAssembler;

/**
 * This goal invokes the feature and bundle publisher and publishes features and bundles referenced in product or category files.
 * @see http://wiki.eclipse.org/Equinox/p2/Publisher#Features_And_Bundles_Publisher_Application  
 * 
 * @goal publish-features-and-bundles
 */
public class PublishFeatureAndBundlesMojo
    extends AbstractPublishMojo
{

    // TODO: Add TRANSITIVE option to assemble a fully self contained repo by following all dependencies transitively.
    enum PublishOptions
    {
        TRUE, FALSE
    };

    private static String CONTENT_PUBLISHER_APP_NAME = "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher";

    /**
     * Defines whether features and bundles referenced in products and categories should be published to the resulting p2 repository.
     * Supported values: true, false
     * 
     * @parameter default-value="false"
     */
    private String publishArtifacts;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        PublishOptions option = PublishOptions.FALSE;
        try
        {
            option = PublishOptions.valueOf( publishArtifacts.toUpperCase() );
        }
        catch ( IllegalArgumentException e )
        {
            throw new MojoExecutionException( "Invalid build configuration value: publishArtifacts=" + publishArtifacts );
        }
        
        if ( option != PublishOptions.FALSE )
        {
            collectContent();
            publishContent();
        }
    }

    private void collectContent()
    {
        File assemblyTargetFolder = getTargetRepositoryLocation();
        assemblyTargetFolder.mkdirs();

        UpdateSiteAssembler contentAssembler = new UpdateSiteAssembler( getSession(), assemblyTargetFolder );
        getEclipseRepositoryProject().getDependencyWalker( getProject() ).walk( contentAssembler );
    }

    private void publishContent()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            List<String> contentArgs = new ArrayList<String>();
            contentArgs.add( "-source" );
            contentArgs.add( getTargetRepositoryLocation().getCanonicalPath() );

            executePublisherApplication( CONTENT_PUBLISHER_APP_NAME,
                                         (String[]) contentArgs.toArray( new String[contentArgs.size()] ) );

        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Unable to execute the publisher", ioe );
        }
    }
}
