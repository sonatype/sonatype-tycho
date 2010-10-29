package org.sonatype.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.model.FeatureRef;
import org.codehaus.tycho.model.UpdateSite;
import org.codehaus.tycho.model.UpdateSite.SiteFeatureRef;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.versions.engine.VersionChange;
import org.sonatype.tycho.versions.engine.MetadataManipulator;
import org.sonatype.tycho.versions.engine.ProjectMetadata;
import org.sonatype.tycho.versions.pom.MutablePomFile;

@Component( role = MetadataManipulator.class, hint = "eclipse-update-site" )
public class SiteXmlManipulator
    extends AbstractMetadataManipulator
{

    public void applyChange( ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges )
    {
        if ( isSite( project ) )
        {
            if ( isFeature( change.getProject().getPackaging() ) )
            {
                UpdateSite site = getSiteXml( project );

                for ( FeatureRef feature : site.getFeatures() )
                {
                    if ( change.getArtifactId().equals( feature.getId() )
                        && change.getVersion().equals( feature.getVersion() ) )
                    {
                        logger.info( "  site.xml//site/feature/@id=" + feature.getId() + "/@version: "
                            + change.getVersion() + " => " + change.getNewVersion() );
                        feature.setVersion( change.getNewVersion() );
                        String newUrl = feature.getId() + "_" + change.getNewVersion();
                        ( (SiteFeatureRef) feature ).setUrl( newUrl );
                    }
                }
            }
        }
    }

    private UpdateSite getSiteXml( ProjectMetadata project )
    {
        UpdateSite site = project.getMetadata( UpdateSite.class );
        if ( site == null )
        {
            File file = new File( project.getBasedir(), UpdateSite.SITE_XML );
            try
            {
                site = UpdateSite.read( file );
                project.putMetadata( site );
            }
            catch ( IOException e )
            {
                throw new IllegalArgumentException( "Could not read update site " + file, e );
            }
        }

        return site;
    }

    private boolean isSite( ProjectMetadata project )
    {
        MutablePomFile pom = project.getMetadata( MutablePomFile.class );
        return isSite( pom.getPackaging() );
    }

    private boolean isSite( String packaging )
    {
        return ArtifactKey.TYPE_ECLIPSE_UPDATE_SITE.equals( packaging );
    }

    public void writeMetadata( ProjectMetadata project )
        throws IOException
    {
        File basedir = project.getBasedir();
        UpdateSite site = project.getMetadata( UpdateSite.class );
        if ( site != null )
        {
            UpdateSite.write( site, new File( basedir, UpdateSite.SITE_XML ) );
        }
    }
}
