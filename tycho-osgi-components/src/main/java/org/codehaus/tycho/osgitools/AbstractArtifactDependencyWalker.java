package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.PluginDescription;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.FeatureRef;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.model.ProductConfiguration;
import org.codehaus.tycho.model.UpdateSite;

public abstract class AbstractArtifactDependencyWalker
    implements ArtifactDependencyWalker
{
    private final TargetPlatform platform;

    private final TargetEnvironment[] environments;

    public AbstractArtifactDependencyWalker( TargetPlatform platform )
    {
        this( platform, null );
    }

    public AbstractArtifactDependencyWalker( TargetPlatform platform, TargetEnvironment environment )
    {
        this.platform = platform;

        this.environments = environment != null ? new TargetEnvironment[] { environment } : null;
    }

    public void traverseUpdateSite( UpdateSite site, ArtifactDependencyVisitor visitor )
    {
        Set<File> visited = new HashSet<File>();

        for ( FeatureRef ref : site.getFeatures() )
        {
            traverseFeature( ref, visitor, visited );
        }
    }

    public void traverseFeature( File location, Feature feature, ArtifactDependencyVisitor visitor )
    {
        traverseFeature( location, feature, null, visitor, new HashSet<File>() );
    }

    protected void traverseFeature( File location, Feature feature, FeatureRef featureRef,
                                    ArtifactDependencyVisitor visitor, Set<File> visited )
    {
        ArtifactKey key = new ArtifactKey( TychoProject.ECLIPSE_FEATURE, feature.getId(), feature.getVersion() );
        MavenProject project = platform.getMavenProject( location );

        DefaultFeatureDescription description =
            new DefaultFeatureDescription( key, location, project, feature, featureRef );

        if ( visitor.visitFeature( description ) )
        {
            for ( PluginRef ref : feature.getPlugins() )
            {
                traversePlugin( ref, visitor, visited );
            }

            for ( FeatureRef ref : feature.getIncludedFeatures() )
            {
                traverseFeature( ref, visitor, visited );
            }
        }
    }

    public void traverseProduct( ProductConfiguration product, ArtifactDependencyVisitor visitor )
    {
        Set<File> visited = new HashSet<File>();

        if ( product.useFeatures() )
        {
            for ( FeatureRef ref : product.getFeatures() )
            {
                traverseFeature( ref, visitor, visited );
            }
        }
        else
        {
            for ( PluginRef ref : product.getPlugins() )
            {
                traversePlugin( ref, visitor, visited );
            }
        }
    }

    protected void traverseFeature( FeatureRef ref, ArtifactDependencyVisitor visitor, Set<File> visited )
    {
        File location = platform.getArtifact( TychoProject.ECLIPSE_FEATURE, ref.getId(), ref.getVersion() );

        if ( location != null )
        {
            Feature feature = Feature.loadFeature( location );
            traverseFeature( location, feature, ref, visitor, visited );
        }
        else
        {
            visitor.missingFeature( ref );
        }
    }

    private void traversePlugin( PluginRef ref, ArtifactDependencyVisitor visitor, Set<File> visited )
    {
        if ( !matchTargetEnvironment( ref ) )
        {
            return;
        }

        ArtifactKey key = platform.getArtifactKey( TychoProject.ECLIPSE_PLUGIN, ref.getId(), ref.getVersion() );

        if ( key != null )
        {
            File location = platform.getArtifact( key );

            if ( !visited.add( location ) )
            {
                return;
            }

            MavenProject project = platform.getMavenProject( location );
            PluginDescription description = new DefaultPluginDescription( key, location, project, ref );
            visitor.visitPlugin( description );
        }
        else
        {
            visitor.missingPlugin( ref );
        }
    }

    private boolean matchTargetEnvironment( PluginRef pluginRef )
    {
        String pluginOs = pluginRef.getOs();
        String pluginWs = pluginRef.getWs();
        String pluginArch = pluginRef.getArch();

        if ( environments == null )
        {
            // match all environments be default
            return true;

            // no target environments, only include environment independent plugins
            // return pluginOs == null && pluginWs == null && pluginArch == null;
        }

        for ( TargetEnvironment environment : environments )
        {
            if ( environment.match( pluginOs, pluginWs, pluginArch ) )
            {
                return true;
            }
        }

        return false;
    }

}
