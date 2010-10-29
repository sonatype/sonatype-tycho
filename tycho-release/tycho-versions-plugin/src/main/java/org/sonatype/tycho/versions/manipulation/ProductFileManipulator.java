package org.sonatype.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.model.FeatureRef;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.model.ProductConfiguration;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.versions.engine.VersionChange;
import org.sonatype.tycho.versions.engine.MetadataManipulator;
import org.sonatype.tycho.versions.engine.ProjectMetadata;
import org.sonatype.tycho.versions.pom.MutablePomFile;

@Component( role = MetadataManipulator.class, hint = "eclipse-application" )
public class ProductFileManipulator
    extends AbstractMetadataManipulator
{

    public void applyChange( ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges )
    {
        if ( isEclipseApplication( project ) )
        {
            String productFileName = getProductFileName( project );

            ProductConfiguration product = getProductFile( project );

            if ( isEclipseApplication( change.getProject().getPackaging() ) )
            {
                if ( change.getArtifactId().equals( product.getId() )
                    && change.getVersion().equals( product.getVersion() ) )
                {
                    logger.info( "  " + productFileName + "//product/@version: " + change.getVersion() + " => "
                        + change.getNewVersion() );
                    product.setVersion( change.getNewVersion() );
                }
            }
            else if ( isBundle( change.getProject() ) )
            {
                for ( PluginRef plugin : product.getPlugins() )
                {
                    if ( change.getArtifactId().equals( plugin.getId() )
                        && change.getVersion().equals( plugin.getVersion() ) )
                    {
                        logger.info( "  " + productFileName + "//product/plugins/plugin/@id=" + plugin.getId()
                            + "/@version: " + change.getVersion() + " => " + change.getNewVersion() );
                        plugin.setVersion( change.getNewVersion() );
                    }
                }
            }
            else if ( isFeature( change.getProject().getPackaging() ) )
            {
                for ( FeatureRef feature : product.getFeatures() )
                {
                    if ( change.getArtifactId().equals( feature.getId() )
                        && change.getVersion().equals( feature.getVersion() ) )
                    {
                        logger.info( "  " + productFileName + "//product/features/feature/@id=" + feature.getId()
                            + "/@version: " + change.getVersion() + " => " + change.getNewVersion() );
                        feature.setVersion( change.getNewVersion() );
                    }
                }
            }
        }
    }

    private ProductConfiguration getProductFile( ProjectMetadata project )
    {
        ProductConfiguration product = project.getMetadata( ProductConfiguration.class );
        if ( product == null )
        {
            String productFileName = getProductFileName( project );
            File file = new File( project.getBasedir(), productFileName );
            try
            {
                product = ProductConfiguration.read( file );
                project.putMetadata( product );
            }
            catch ( IOException e )
            {
                throw new IllegalArgumentException( "Could not read product configuration file " + file, e );
            }
        }
        return product;
    }

    protected String getProductFileName( ProjectMetadata project )
    {
        MutablePomFile pom = project.getMetadata( MutablePomFile.class );
        String productFileName = pom.getArtifactId() + ".product";
        return productFileName;
    }

    private boolean isEclipseApplication( ProjectMetadata project )
    {
        String packaging = project.getMetadata( MutablePomFile.class ).getPackaging();
        return isEclipseApplication( packaging );
    }

    private boolean isEclipseApplication( String packaging )
    {
        return ArtifactKey.TYPE_ECLIPSE_APPLICATION.equals( packaging );
    }

    public void writeMetadata( ProjectMetadata project )
        throws IOException
    {
        MutablePomFile pom = project.getMetadata( MutablePomFile.class );
        ProductConfiguration product = project.getMetadata( ProductConfiguration.class );
        if ( product != null )
        {
            ProductConfiguration.write( product, new File( project.getBasedir(), pom.getArtifactId() + ".product" ) );
        }
    }
}
