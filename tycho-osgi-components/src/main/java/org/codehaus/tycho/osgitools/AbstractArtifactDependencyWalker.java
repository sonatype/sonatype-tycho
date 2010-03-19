package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.ArtifactDescription;
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
import org.codehaus.tycho.utils.PlatformPropertiesUtils;

public abstract class AbstractArtifactDependencyWalker
    implements ArtifactDependencyWalker
{
    public static final String EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher";

    private final TargetPlatform platform;

    private final TargetEnvironment[] environments;

    public AbstractArtifactDependencyWalker( TargetPlatform platform )
    {
        this( platform, null );
    }

    public AbstractArtifactDependencyWalker( TargetPlatform platform, TargetEnvironment[] environments )
    {
        this.platform = platform;
        this.environments = environments;
    }

    public void traverseUpdateSite( UpdateSite site, ArtifactDependencyVisitor visitor )
    {
        Map<ArtifactKey, File> visited = new HashMap<ArtifactKey, File>();

        for ( FeatureRef ref : site.getFeatures() )
        {
            traverseFeature( ref, visitor, visited );
        }
    }

    public void traverseFeature( File location, Feature feature, ArtifactDependencyVisitor visitor )
    {
        traverseFeature( location, feature, null, visitor, new HashMap<ArtifactKey, File>() );
    }

    protected void traverseFeature( File location, Feature feature, FeatureRef featureRef,
                                    ArtifactDependencyVisitor visitor, Map<ArtifactKey, File> visited )
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
        Map<ArtifactKey, File> visited = new HashMap<ArtifactKey, File>();

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

        Set<String> bundles = new HashSet<String>();
        for ( ArtifactKey key : visited.keySet() )
        {
            if ( TychoProject.ECLIPSE_PLUGIN.equals( key.getType() ) )
            {
                bundles.add( key.getId() );
            }
        }

        // RCP apparently implicitly includes equinox.launcher and corresponding native fragments
        // See also org.sonatype.tycho.p2.ProductDependenciesAction.perform

        if ( !bundles.contains( EQUINOX_LAUNCHER ) )
        {
            PluginRef ref = new PluginRef( "plugin" );
            ref.setId( EQUINOX_LAUNCHER );
            traversePlugin( ref, visitor, visited );
        }

        if ( environments != null )
        {
            for ( TargetEnvironment environment : environments )
            {
                String os = environment.getOs();
                String ws = environment.getWs();
                String arch = environment.getArch();

                String id;

                // for Mac OS X there is no org.eclipse.equinox.launcher.carbon.macosx.x86 folder,
                // only a org.eclipse.equinox.launcher.carbon.macosx folder.
                // see http://jira.codehaus.org/browse/MNGECLIPSE-1075
                if ( PlatformPropertiesUtils.OS_MACOSX.equals( os ) && PlatformPropertiesUtils.ARCH_X86.equals( arch ) )
                {
                    id = "org.eclipse.equinox.launcher." + ws + "." + os;
                }
                else
                {
                    id = "org.eclipse.equinox.launcher." + ws + "." + os + "." + arch;
                }

                if ( !bundles.contains( id ) )
                {
                    PluginRef ref = new PluginRef( "plugin" );
                    ref.setId( id );
                    ref.setOs( os );
                    ref.setWs( ws );
                    ref.setArch( arch );
                    ref.setUnpack( true );
                    traversePlugin( ref, visitor, visited );
                }
            }
        }
    }

    protected void traverseFeature( FeatureRef ref, ArtifactDependencyVisitor visitor, Map<ArtifactKey, File> visited )
    {
        ArtifactDescription artifact = platform.getArtifact( TychoProject.ECLIPSE_FEATURE, ref.getId(), ref.getVersion() );

        if ( artifact != null )
        {
            if ( visited.containsKey( artifact.getKey() ) )
            {
                return;
            }

            File location = artifact.getLocation();

            Feature feature = Feature.loadFeature( location );
            traverseFeature( location, feature, ref, visitor, visited );
        }
        else
        {
            visitor.missingFeature( ref );
        }
    }

    private void traversePlugin( PluginRef ref, ArtifactDependencyVisitor visitor, Map<ArtifactKey, File> visited )
    {
        if ( !matchTargetEnvironment( ref ) )
        {
            return;
        }

        ArtifactDescription artifact = platform.getArtifact( TychoProject.ECLIPSE_PLUGIN, ref.getId(), ref.getVersion() );

        if ( artifact != null )
        {
            ArtifactKey key = artifact.getKey();

            if ( visited.containsKey( key ) )
            {
                return;
            }

            File location = artifact.getLocation();

            visited.put( key, location );

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
