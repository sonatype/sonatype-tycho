package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.osgitools.BundleReader;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Constants;

/**
 * Creates target platform based on local eclipse installation.
 */
@Component( role = TargetPlatformResolver.class, hint = LocalTargetPlatformResolver.ROLE_HINT, instantiationStrategy = "per-lookup" )
public class LocalTargetPlatformResolver
    extends AbstractTargetPlatformResolver
    implements TargetPlatformResolver
{

    public static final String ROLE_HINT = "local";

    @Requirement
    private EclipseInstallationLayout layout;

    @Requirement
    private BundleReader manifestReader;

    public TargetPlatform resolvePlatform( MavenSession session, MavenProject project, List<Dependency> dependencies )
    {
        DefaultTargetPlatform platform = new DefaultTargetPlatform();

        for ( File site : layout.getSites() )
        {
            platform.addSite( site );

            for ( File plugin : layout.getPlugins( site ) )
            {
                Manifest mf = manifestReader.loadManifest( plugin );

                ManifestElement[] id = manifestReader.parseHeader( Constants.BUNDLE_SYMBOLICNAME, mf );
                ManifestElement[] version = manifestReader.parseHeader( Constants.BUNDLE_VERSION, mf );

                if ( id != null && version != null )
                {
                    platform.addArtifactFile( new ArtifactKey( TychoProject.ECLIPSE_PLUGIN, id[0].getValue(),
                                                               version[0].getValue() ), plugin );
                }
            }

            for ( File feature : layout.getFeatures( site ) )
            {
                Feature desc = Feature.loadFeature( feature );
                ArtifactKey key = new ArtifactKey( TychoProject.ECLIPSE_FEATURE, desc.getId(), desc.getVersion() );

                platform.addArtifactFile( key, feature );
            }
        }

        addProjects( session, platform );

        if ( platform.isEmpty() )
        {
            getLogger().warn( "Could not find any bundles or features in " + layout.getLocation() );
        }

        return platform;
    }

    public void setLocation( File location )
        throws IOException
    {
        layout.setLocation( location.getCanonicalFile() );
    }
}
