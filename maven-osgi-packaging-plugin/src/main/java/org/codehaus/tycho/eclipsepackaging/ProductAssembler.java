package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.util.jar.Manifest;

import org.apache.maven.execution.MavenSession;
import org.codehaus.tycho.PluginDescription;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.osgitools.BundleManifestReader;

public class ProductAssembler
    extends UpdateSiteAssembler
{

    private final TargetEnvironment environment;

    private boolean includeSources;

    private final BundleManifestReader manifestReader;

    public ProductAssembler( MavenSession session, BundleManifestReader manifestReader, File target, TargetEnvironment environment )
    {
        super( session, target );
        this.manifestReader = manifestReader;
        setUnpackPlugins( true );
        setUnpackFeatures( true );
        this.environment = environment;
    }
    
    @Override
    public void visitPlugin( PluginDescription plugin )
    {
        if ( !matchEntivonment( plugin ) )
        {
            return;
        }

        if ( !includeSources && isSourceBundle( plugin ) )
        {
            return;
        }

        super.visitPlugin( plugin );
    }

    private boolean isSourceBundle( PluginDescription plugin )
    {
        Manifest mf = manifestReader.loadManifest( plugin.getLocation() );
        return manifestReader.parseHeader( "Eclipse-SourceBundle", mf ) != null;
    }

    @Override
    protected boolean isDirectoryShape( PluginDescription plugin, File location )
    {
        if ( super.isDirectoryShape( plugin, location ) )
        {
            return true;
        }

        Manifest mf = manifestReader.loadManifest( location );

        return manifestReader.isDirectoryShape( mf );
    }

    protected boolean matchEntivonment( PluginDescription plugin )
    {
        PluginRef ref = plugin.getPluginRef();
        return ref == null || environment == null || environment.match( ref.getOs(), ref.getWs(), ref.getArch() );
    }

    public void setIncludeSources( boolean includeSources )
    {
        this.includeSources = includeSources;
    }

}
