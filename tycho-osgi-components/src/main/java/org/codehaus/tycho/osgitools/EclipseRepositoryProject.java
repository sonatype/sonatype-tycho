package org.codehaus.tycho.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.Category;
import org.codehaus.tycho.model.FeatureRef;
import org.codehaus.tycho.model.ProductConfiguration;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.ReactorProject;

/**
 * An eclipse repository project produces a p2 repository where a set of products are published.
 */
@Component( role = TychoProject.class, hint = org.sonatype.tycho.ArtifactKey.TYPE_ECLIPSE_REPOSITORY )
public class EclipseRepositoryProject
    extends AbstractArtifactBasedProject
{

    /**
     * The published repository is always under the id of the maven project: this published
     * repository can contain multiple products.
     */
    public ArtifactKey getArtifactKey( ReactorProject project )
    {
        String id = project.getArtifactId();
        String version = getOsgiVersion( project );

        return new DefaultArtifactKey( org.sonatype.tycho.ArtifactKey.TYPE_ECLIPSE_REPOSITORY, id, version );
    }

    @Override
    protected ArtifactDependencyWalker newDependencyWalker( MavenProject project, TargetEnvironment environment )
    {
        final List<ProductConfiguration> products = loadProducts( project );
        final List<Category> categories = loadCategories( project );
        return new AbstractArtifactDependencyWalker( getTargetPlatform( project, environment ),
                                                     getEnvironments( project, environment ) )
        {
            public void walk( ArtifactDependencyVisitor visitor )
            {
                WalkbackPath visited = new WalkbackPath();
                for ( ProductConfiguration product : products )
                {
                    traverseProduct( product, visitor, visited );
                }
                for ( Category category : categories )
                {
                    for ( FeatureRef feature : category.getFeatures() )
                    {
                        traverseFeature( feature, visitor, visited );
                    }
                }
            }
        };
    }

    /**
     * Parses the category configuration files
     *
     * @param project
     * @return
     */
    public List<Category> loadCategories( final MavenProject project )
    {
        List<Category> categories = new ArrayList<Category>();
        for ( File file : getCategoryFiles( project ) )
        {
            try
            {
                categories.add( Category.read( file ) );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Could not read product configuration file " + file.getAbsolutePath(), e );
            }
        }
        return categories;
    }
    

	/**
     * Parses the product configuration files
     *
     * @param project
     * @return
     */
    protected List<ProductConfiguration> loadProducts( final MavenProject project )
    {
        List<ProductConfiguration> products = new ArrayList<ProductConfiguration>();
        for ( File file : getProductFiles( project ) )
        {
            try
            {
                products.add( ProductConfiguration.read( file ) );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Could not read product configuration file " + file.getAbsolutePath(), e );
            }
        }
        return products;
    }

	private List<File> getCategoryFiles(MavenProject project)
	{
		List<File> res = new ArrayList<File>();
		File categoryFile = new File(project.getBasedir(), "category.xml");
		if (categoryFile.exists())
		{
			res.add(categoryFile);
		}
		return res;
	}
    
    /**
     * Looks for all files at the base of the project that extension is ".product" Duplicated in the
     * P2GeneratorImpl
     *
     * @param project
     * @return The list of product files to parse for an eclipse-repository project
     */
    public List<File> getProductFiles( MavenProject project )
    {
        File projectLocation = project.getBasedir();
        List<File> res = new ArrayList<File>();
        for ( File f : projectLocation.listFiles() )
        {
            if ( f.isFile() && f.getName().endsWith( ".product" ) )
            {
                res.add( f );
            }
        }
        return res;
    }
}
