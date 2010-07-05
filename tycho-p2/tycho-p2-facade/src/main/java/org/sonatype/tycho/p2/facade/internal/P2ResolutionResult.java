package org.sonatype.tycho.p2.facade.internal;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.p2.MetadataSerializable;

public class P2ResolutionResult
{

    private final Map<ArtifactKey, File> artifacts = new LinkedHashMap<ArtifactKey, File>();
    private MetadataSerializable metadataRepositorySerializable;

    public void addArtifact( String type, String id, String version, File bundle )
    {
        artifacts.put( new ArtifactKey( type, id, version ), bundle );
    }

    public Map<ArtifactKey, File> getArtifacts()
    {
        return artifacts;
    }

    public MetadataSerializable getMetadataRepositorySerializable()
    {
        return metadataRepositorySerializable;
    }

    public void setMetadataRepositorySerializable( MetadataSerializable metadataRepositorySerializable )
    {
        this.metadataRepositorySerializable = metadataRepositorySerializable;
    }

}
