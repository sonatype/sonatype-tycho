package org.sonatype.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.tycho.model.Category;

/**
 * This goal invokes the category publisher and publishes category information.
 * 
 * @see http://wiki.eclipse.org/Equinox/p2/Publisher
 * @goal publish-categories
 */
public class PublishCategoriesMojo
    extends AbstractPublishMojo
{
    private static String CATEGORY_PUBLISHER_APP_NAME = PUBLISHER_BUNDLE_ID + ".CategoryPublisher";

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        publishCategories();
    }

    private void publishCategories()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            for ( Category category : getCategories() )
            {
                final File buildCategoryFile =
                    prepareBuildCategory( category, new File( getProject().getBuild().getDirectory() ), getQualifier() );

                List<String> categoryArgs = new ArrayList<String>();
                categoryArgs.add( "-categoryDefinition" );
                categoryArgs.add( buildCategoryFile.toURI().toURL().toExternalForm() );

                executePublisherApplication( CATEGORY_PUBLISHER_APP_NAME,
                                             (String[]) categoryArgs.toArray( new String[categoryArgs.size()] ) );
            }
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Unable to execute the publisher", ioe );
        }
    }

    private File prepareBuildCategory( Category category, File buildFolder, String qualifier )
        throws IOException
    {
        File ret = new File( buildFolder, "category.xml" );
        buildFolder.mkdirs();
        Category.write( category, ret );
        return ret;
    }

    private List<Category> getCategories()
    {
        return getEclipseRepositoryProject().loadCategories( getProject() );
    }
}
