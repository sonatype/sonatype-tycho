package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.FeatureDescription;
import org.codehaus.tycho.model.FeatureRef;
import org.codehaus.tycho.model.UpdateSite;
import org.codehaus.tycho.model.UpdateSite.SiteFeatureRef;
import org.sonatype.tycho.ReactorProject;

/**
 * @goal update-site
 */
public class UpdateSiteMojo
    extends AbstractTychoPackagingMojo
{

    /** @parameter expression="${project.build.directory}/site" */
    private File target;

    /** @parameter expression="${project.basedir}" */
    private File basedir;

    /** @parameter */
    private boolean inlineArchives;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        target.mkdirs();
        try {
            // remove content collected in former builds.
            // Even without clean goal the build result must not assembly out dated content
            FileUtils.cleanDirectory(target);
        } catch (IOException e) {
            throw new MojoFailureException("Unable to delete old update site content: " + target.getAbsolutePath(),e);
        }
        // expandVersion();

        try
        {
            UpdateSite site = UpdateSite.read( new File( basedir, UpdateSite.SITE_XML ) );

            UpdateSiteAssembler assembler = new UpdateSiteAssembler( session, target );
            assembler.setPack200( site.isPack200() );
            if ( inlineArchives )
            {
                assembler.setArchives( site.getArchives() );
            }

            getDependencyWalker().walk( assembler );
            getDependencyWalker().traverseUpdateSite( site, new ArtifactDependencyVisitor()
            {
                @Override
                public boolean visitFeature( FeatureDescription feature )
                {
                    FeatureRef featureRef = feature.getFeatureRef();
                    String id = featureRef.getId();
                    ReactorProject otherProject = feature.getMavenProject();
                    String version;
                    if ( otherProject != null )
                    {
                        version = otherProject.getExpandedVersion();
                    }
                    else
                    {
                        version = feature.getKey().getVersion();
                    }
                    String url = UpdateSiteAssembler.FEATURES_DIR + id + "_" + version + ".jar";
                    ( (SiteFeatureRef) featureRef ).setUrl( url );
                    featureRef.setVersion( version );
                    return false; // don't traverse included features
                }
            } );

            if ( inlineArchives )
            {
                site.removeArchives();
            }

            File file = new File( target, "site.xml" );
            UpdateSite.write( site, file );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
