package org.codehaus.tycho.osgitools;

import org.sonatype.tycho.ArtifactKey;

public class DefaultArtifactKey
    implements org.sonatype.tycho.ArtifactKey
{
    private final String type;

    private final String id;

    private final String version;

    public DefaultArtifactKey( String type, String id, String version )
    {
        this.id = id;
        this.type = type;
        this.version = version;
    }

    @Override
    public int hashCode()
    {
        int hash = getType().hashCode();
        hash = 17 * hash + getId().hashCode();
        hash = 17 * hash + getVersion().hashCode();
        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof ArtifactKey ) )
        {
            return false;
        }

        ArtifactKey other = (ArtifactKey) obj;

        return getType().equals( other.getType() ) && getId().equals( other.getId() )
            && getVersion().equals( other.getVersion() );
    }

    @Override
    public String toString()
    {
        return getType() + ":" + getId() + ":" + getVersion();
    }

    /**
     * @see ProjectType
     */
    public String getType()
    {
        return type;
    }

    /**
     * Eclipse/OSGi artifact id. Can differ from Maven artifactId.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Eclipse/OSGi artifact version. Can differ from Maven version. For maven projects, this version corresponds to
     * version specified in the project sources and does not reflect qualifier expansion.
     */
    public String getVersion()
    {
        return version;
    }
}