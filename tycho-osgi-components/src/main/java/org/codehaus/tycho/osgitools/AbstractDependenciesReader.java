package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.FeatureResolutionState;
import org.codehaus.tycho.MavenSessionUtils;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.maven.DependenciesReader;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.model.Feature.FeatureRef;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;

public abstract class AbstractDependenciesReader
    extends AbstractLogEnabled
    implements DependenciesReader
{

    public static final String P2_CLASSIFIER_BUNDLE = "osgi.bundle";

    public static final String P2_CLASSIFIER_FEATURE = "org.eclipse.update.feature";

    protected static final List<Dependency> NO_DEPENDENCIES = new ArrayList<Dependency>();

    protected Dependency newExternalDependency( String location, String p2Classifier, String artifactId, String version )
    {
        File file = new File( location );
        if ( !file.exists() || !file.isFile() || !file.canRead() )
        {
            getLogger().warn( "Dependency at location " + location
                + " can not be represented in Maven model and will not be visible to non-OSGi aware Maven plugins" );
            return null;
        }

        Dependency dependency = new Dependency();
        dependency.setArtifactId( artifactId );
        dependency.setGroupId( "p2." + p2Classifier ); // See also RepositoryLayoutHelper#getP2Gav
        dependency.setVersion( version );
        dependency.setScope( Artifact.SCOPE_SYSTEM );
        dependency.setSystemPath( location );
        return dependency;
    }

    protected Dependency newProjectDependency( MavenProject otherProject )
    {
        if ( otherProject == null )
        {
            return null;
        }

        Dependency dependency = new Dependency();
        dependency.setArtifactId( otherProject.getArtifactId() );
        dependency.setGroupId( otherProject.getGroupId() );
        dependency.setVersion( otherProject.getVersion() );
        dependency.setType( otherProject.getPackaging() );
        dependency.setScope( Artifact.SCOPE_PROVIDED );
        return dependency;
    }

    protected Collection<Dependency> getFeaturesDependencies( MavenProject project, List<FeatureRef> features,
                                                              MavenSession session )
    {
        FeatureResolutionState state = getFeatureResolutionState( session, project );
        Collection<Dependency> result = new ArrayList<Dependency>();
        for ( Feature.FeatureRef featureRef : features )
        {
            FeatureDescription otherFeature = state.getFeature( featureRef.getId(), featureRef.getVersion() );
            if ( otherFeature == null )
            {
                continue;
            }
            MavenProject mavenProject = MavenSessionUtils.getMavenProject( session, otherFeature.getLocation() );
            Dependency dependency;
            if ( mavenProject != null )
            {
                dependency = newProjectDependency( mavenProject );
            }
            else
            {
                dependency =
                    newExternalDependency( otherFeature.getLocation().getAbsolutePath(),
                                           P2_CLASSIFIER_FEATURE,
                                           otherFeature.getId(),
                                           otherFeature.getVersion().toString() );
            }
            if ( dependency != null )
            {
                result.add( dependency );
            }
        }
        return result;
    }

    protected Collection<Dependency> getPluginsDependencies( MavenProject project, List<PluginRef> plugins,
                                                             MavenSession session )
        throws MavenExecutionException
    {
        BundleResolutionState state = getBundleResolutionState( session, project );

        Collection<Dependency> result = new ArrayList<Dependency>();
        for ( PluginRef pluginRef : plugins )
        {
            BundleDescription bundle = state.getBundle( pluginRef.getId(), getPluginVersion( pluginRef.getVersion() ) );
            if ( bundle == null )
            {
                continue;
            }
            Dependency dependency = newBundleDependency( session, bundle );
            if ( dependency != null )
            {
                result.add( dependency );
            }
        }
        return result;
    }

    private String getPluginVersion( String version )
    {
        return version == null || "0.0.0".equals( version ) ? TychoConstants.HIGHEST_VERSION : version;
    }

    protected Dependency newBundleDependency( MavenSession session, BundleDescription supplier )
    {
        MavenProject otherProject = MavenSessionUtils.getMavenProject( session, supplier.getLocation() );

        Dependency dependency;
        if ( otherProject != null )
        {
            dependency = newProjectDependency( otherProject );
        }
        else
        {
            String artifactId = supplier.getSymbolicName();
            String version = supplier.getVersion().toString();

            dependency = newExternalDependency( supplier.getLocation(), P2_CLASSIFIER_BUNDLE, artifactId, version );
        }
        return dependency;
    }

    public FeatureResolutionState getFeatureResolutionState( MavenSession session, MavenProject project )
    {
        FeatureResolutionState resolver =
            (FeatureResolutionState) project.getContextValue( TychoConstants.CTX_FEATURE_RESOLUTION_STATE );

        if ( resolver == null )
        {
            TargetPlatform platform = (TargetPlatform) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM );

            if ( platform != null )
            {
                resolver = new FeatureResolutionState( getLogger(), session, platform );

                project.setContextValue( TychoConstants.CTX_FEATURE_RESOLUTION_STATE, resolver );
            }

        }

        return resolver;
    }

    public BundleResolutionState getBundleResolutionState( MavenSession session, MavenProject project )
    {
        BundleResolutionState resolver =
            (BundleResolutionState) project.getContextValue( TychoConstants.CTX_BUNDLE_RESOLUTION_STATE );

        if ( resolver == null )
        {
            TargetPlatform platform = (TargetPlatform) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM );

            if ( platform != null )
            {
                resolver = EquinoxBundleResolutionState.newInstance( session.getContainer(), session, project );
            }

            project.setContextValue( TychoConstants.CTX_BUNDLE_RESOLUTION_STATE, resolver );
        }
        return resolver;
    }

}
