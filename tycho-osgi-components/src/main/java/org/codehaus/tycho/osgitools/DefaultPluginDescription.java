package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.Set;

import org.codehaus.tycho.PluginDescription;
import org.codehaus.tycho.model.PluginRef;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.resolver.DependentMavenProjectProxy;

public class DefaultPluginDescription
    extends DefaultArtifactDescriptor
    implements PluginDescription
{

    private PluginRef pluginRef;

    public DefaultPluginDescription( ArtifactKey key, File location, DependentMavenProjectProxy project, PluginRef pluginRef,
                                     Set<Object> installableUnits )
    {
        super( key, location, project, installableUnits );
        this.pluginRef = pluginRef;
    }

    public PluginRef getPluginRef()
    {
        return pluginRef;
    }

}
