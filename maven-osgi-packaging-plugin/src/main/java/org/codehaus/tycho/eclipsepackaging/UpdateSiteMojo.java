package org.codehaus.tycho.eclipsepackaging;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.FeatureDescription;
import org.codehaus.tycho.buildversion.VersioningHelper;
import org.codehaus.tycho.model.FeatureRef;
import org.codehaus.tycho.model.UpdateSite;
import org.codehaus.tycho.model.UpdateSite.SiteFeatureRef;

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
                    MavenProject otherProject = feature.getMavenProject();
                    String version;
                    if ( otherProject != null )
                    {
                        version = VersioningHelper.getExpandedVersion( otherProject, featureRef.getVersion() );
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
