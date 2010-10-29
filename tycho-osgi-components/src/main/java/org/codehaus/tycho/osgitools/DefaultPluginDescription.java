package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.PluginDescription;
import org.codehaus.tycho.model.PluginRef;
import org.sonatype.tycho.ArtifactKey;

public class DefaultPluginDescription
    extends DefaultArtifactDescriptor
    implements PluginDescription
{

    private PluginRef pluginRef;

    public DefaultPluginDescription( ArtifactKey key, File location, MavenProject project, PluginRef pluginRef,
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
