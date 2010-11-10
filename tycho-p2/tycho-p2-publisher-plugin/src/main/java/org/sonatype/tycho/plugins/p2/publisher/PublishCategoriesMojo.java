package org.sonatype.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.tycho.model.Category;
import org.sonatype.tycho.p2.tools.FacadeException;
import org.sonatype.tycho.p2.tools.publisher.PublisherService;

/**
 * This goal invokes the category publisher and publishes category information.
 * 
 * @see http://wiki.eclipse.org/Equinox/p2/Publisher
 * @goal publish-categories
 */
public class PublishCategoriesMojo
    extends AbstractPublishMojo
{
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        publishCategories();
    }

    private void publishCategories()
        throws MojoExecutionException
    {
        PublisherService publisherService = createPublisherService();
        try
        {
            for ( Category category : getCategories() )
            {
                final File buildCategoryFile =
                    prepareBuildCategory( category, new File( getProject().getBuild().getDirectory() ) );

                publisherService.publishCategories( buildCategoryFile );
            }
        }
        catch ( FacadeException e )
        {
            throw new MojoExecutionException( "Exception while publishing categories: " + e.getMessage(), e );
        }
        finally
        {
            publisherService.stop();
        }
    }

    /**
     * Writes the Tycho-internal representation of categories back to a category.xml.
     * 
     * @param category a category, with "qualifier" literals already replaced by the build
     *            qualifier.
     */
    private File prepareBuildCategory( Category category, File buildFolder )
        throws MojoExecutionException
    {
        try
        {
            File ret = new File( buildFolder, "category.xml" );
            buildFolder.mkdirs();
            Category.write( category, ret );
            return ret;
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "I/O exception while writing category definition to disk", e );
        }
    }

    private List<Category> getCategories()
    {
        return getEclipseRepositoryProject().loadCategories( getProject() );
    }
}
