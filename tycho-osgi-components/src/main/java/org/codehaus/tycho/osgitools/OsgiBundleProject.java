package org.codehaus.tycho.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.jar.Manifest;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.PluginDescription;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.ProductConfiguration;
import org.codehaus.tycho.model.UpdateSite;
import org.codehaus.tycho.osgitools.DependencyComputer.DependencyEntry;
import org.codehaus.tycho.osgitools.project.EclipsePluginProjectImpl;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

@Component( role = TychoProject.class, hint = TychoProject.ECLIPSE_PLUGIN )
public class OsgiBundleProject
    extends AbstractTychoProject
{

    private static final String CTX_ARTIFACT_KEY = TychoConstants.CTX_BASENAME + "/osgiBundle/artifactKey";

    @Requirement
    private DependencyComputer dependencyComputer;

    public ArtifactDependencyWalker getDependencyWalker( MavenProject project, TargetEnvironment environment )
    {
        return getDependencyWalker( project );
    }

    public ArtifactDependencyWalker getDependencyWalker( MavenProject project )
    {
        final TargetPlatform platform = getTargetPlatform( project );
        final BundleResolutionState state = getBundleResolutionState( project );
        final BundleDescription bundleDescription = state.getBundleByLocation( project.getBasedir() );

        return new ArtifactDependencyWalker()
        {
            public void walk( ArtifactDependencyVisitor visitor )
            {
                for ( DependencyEntry entry : dependencyComputer.computeDependencies( state, bundleDescription ) )
                {
                    BundleDescription supplier = entry.desc;

                    String artifactId = supplier.getSymbolicName();
                    String version = supplier.getVersion().toString();
                    File location = new File( supplier.getLocation() );
                    MavenProject project = platform.getMavenProject( location );

                    String type = project != null ? project.getPackaging() : TychoProject.ECLIPSE_PLUGIN;
                    ArtifactKey key = new ArtifactKey( type, artifactId, version );

                    PluginDescription plugin = new DefaultPluginDescription( key, location, project, null );

                    visitor.visitPlugin( plugin );
                }
            }

            public void traverseFeature( File location, Feature feature, ArtifactDependencyVisitor visitor )
            {
            }

            public void traverseUpdateSite( UpdateSite site, ArtifactDependencyVisitor artifactDependencyVisitor )
            {
            }

            public void traverseProduct( ProductConfiguration productConfiguration, ArtifactDependencyVisitor visitor )
            {
            }
        };
    }

    @Override
    public void setTargetPlatform( MavenSession session, MavenProject project, TargetPlatform targetPlatform )
    {
        super.setTargetPlatform( session, project, targetPlatform );

        EquinoxBundleResolutionState resolver =
            EquinoxBundleResolutionState.newInstance( session.getContainer(), session, project );

        project.setContextValue( TychoConstants.CTX_BUNDLE_RESOLUTION_STATE, resolver );
    }

    protected EquinoxBundleResolutionState getBundleResolutionState( MavenProject project )
    {
        EquinoxBundleResolutionState resolver =
            (EquinoxBundleResolutionState) project.getContextValue( TychoConstants.CTX_BUNDLE_RESOLUTION_STATE );
        return resolver;
    }

    public ArtifactKey getArtifactKey( MavenProject project )
    {
        ArtifactKey key = (ArtifactKey) project.getContextValue( CTX_ARTIFACT_KEY );
        if ( key == null )
        {
            throw new IllegalStateException( "Project has not been setup yet " + project.toString() );
        }

        return key;
    }

    @Override
    public void setupProject( MavenSession session, MavenProject project )
    {
        BundleManifestReader manifestReader =
            EquinoxBundleResolutionState.newManifestReader( session.getContainer(), project );

        Manifest mf = manifestReader.loadManifest( project.getBasedir() );

        ManifestElement[] id = manifestReader.parseHeader( Constants.BUNDLE_SYMBOLICNAME, mf );
        ManifestElement[] version = manifestReader.parseHeader( Constants.BUNDLE_VERSION, mf );

        if ( id == null || version == null )
        {
            throw new IllegalArgumentException( "Missing bundle symbolic name or version for project "
                + project.toString() );
        }

        ArtifactKey key = new ArtifactKey( TychoProject.ECLIPSE_PLUGIN, id[0].getValue(), version[0].getValue() );
        project.setContextValue( CTX_ARTIFACT_KEY, key );
    }

    @Override
    public void resolve( MavenProject project )
    {
        EquinoxBundleResolutionState state = getBundleResolutionState( project );
        BundleDescription bundle = state.resolve( project );
        try
        {
            state.assertResolved( bundle );
            project.setContextValue( TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT, new EclipsePluginProjectImpl( project,
                                                                                                              bundle ) );
        }
        catch ( BundleException e )
        {
            throw new RuntimeException( e );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

    }
}
