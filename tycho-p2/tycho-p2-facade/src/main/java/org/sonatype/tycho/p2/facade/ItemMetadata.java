package org.sonatype.tycho.p2.facade;


public class ItemMetadata
{
    private final String id;

    private final String version;

    private final byte[] iuXml;

    private final byte[] artifactXml;

    public ItemMetadata( String id, String version, byte[] iuXml, byte[] artifactXml )
    {
        this.id = id;
        this.version = version;
        this.iuXml = iuXml;
        this.artifactXml = artifactXml;
    }

    public byte[] getIUXml()
    {
        return iuXml;
    }

    public byte[] getArtifactXml()
    {
        return artifactXml;
    }

    public String getId()
    {
        return id;
    }

    public String getVersion()
    {
        return version;
    }
}
