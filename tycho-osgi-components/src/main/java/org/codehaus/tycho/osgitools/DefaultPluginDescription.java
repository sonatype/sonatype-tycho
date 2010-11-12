package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.Set;

import org.codehaus.tycho.PluginDescription;
import org.codehaus.tycho.model.PluginRef;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.ReactorProject;

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
