package org.eclipse.tycho.osgitools;

import java.io.File;
import java.util.Set;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;

public class DefaultArtifactDescriptor
    implements ArtifactDescriptor
{

    private final ArtifactKey key;

    private final File location;

    private final ReactorProject project;

    private final String classifier;

    private final Set<Object> installableUnits;

    public DefaultArtifactDescriptor( ArtifactKey key, File location, ReactorProject project, String classifier,
                                      Set<Object> installableUnits )
    {
        this.key = key;
        this.location = location;
        this.project = project;
        this.classifier = classifier;
        this.installableUnits = installableUnits;
    }

    public ArtifactKey getKey()
    {
        return key;
    }

    public File getLocation()
    {
        return location;
    }

    public ReactorProject getMavenProject()
    {
        return project;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public Set<Object> getInstallableUnits()
    {
        return installableUnits;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( key.toString() ).append( ": " );
        if ( project != null )
        {
            sb.append( project.toString() );
        }
        else
        {
            sb.append( location );
        }
        return sb.toString();
    }
}
