package org.eclipse.tycho.osgitools;

import java.io.File;
import java.util.Set;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.PluginDescription;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.model.PluginRef;

public class DefaultPluginDescription
    extends DefaultArtifactDescriptor
    implements PluginDescription
{

    private PluginRef pluginRef;

    public DefaultPluginDescription( ArtifactKey key, File location, ReactorProject project, String classifier,
                                     PluginRef pluginRef, Set<Object> installableUnits )
    {
        super( key, location, project, classifier, installableUnits );
        this.pluginRef = pluginRef;
    }

    public PluginRef getPluginRef()
    {
        return pluginRef;
    }

}
