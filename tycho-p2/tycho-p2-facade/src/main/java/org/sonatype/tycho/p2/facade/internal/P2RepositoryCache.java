package org.sonatype.tycho.p2.facade.internal;

import java.net.URI;
import java.util.HashMap;

import org.codehaus.plexus.component.annotations.Component;

@Component( role = P2RepositoryCache.class )
public class P2RepositoryCache
{

    private HashMap<URI, Object> artifactRepositories = new HashMap<URI, Object>();

    private HashMap<URI, Object> metadataRepositories = new HashMap<URI, Object>();

    private HashMap<String, TychoRepositoryIndex> indexes = new HashMap<String, TychoRepositoryIndex>();

    public Object getArtifactRepository( URI uri )
    {
        return artifactRepositories.get( uri );
    }

    public Object getMetadataRepository( URI uri )
    {
        return metadataRepositories.get( uri );
    }

    public void putRepository( URI uri, Object metadataRepository, Object artifactRepository )
    {
        metadataRepositories.put( uri, metadataRepository );
        artifactRepositories.put( uri, artifactRepository );
    }

    public TychoRepositoryIndex getRepositoryIndex( String repositoryKey )
    {
        return indexes.get( repositoryKey );
    }

    public void putRepositoryIndex( String repositoryKey, TychoRepositoryIndex index )
    {
        indexes.put( repositoryKey, index );
    }
}
