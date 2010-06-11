package org.sonatype.tycho.p2.facade.internal;

import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.HashMap;

import org.codehaus.plexus.component.annotations.Component;

@Component( role = P2RepositoryCache.class )
public class P2RepositoryCache
{

    private HashMap<URI, SoftReference<Object>> artifactRepositories = new HashMap<URI, SoftReference<Object>>();

    private HashMap<URI, SoftReference<Object>> metadataRepositories = new HashMap<URI, SoftReference<Object>>();

    private HashMap<String, TychoRepositoryIndex> indexes = new HashMap<String, TychoRepositoryIndex>();

    public Object getArtifactRepository( URI uri )
    {
        return dereference( artifactRepositories.get( uri ) );
    }

    private Object dereference( SoftReference<Object> reference )
    {
        return reference != null ? reference.get() : null;
    }

    public Object getMetadataRepository( URI uri )
    {
        return dereference( metadataRepositories.get( uri ) );
    }

    public void putRepository( URI uri, Object metadataRepository, Object artifactRepository )
    {
        if ( metadataRepository != null )
        {
            metadataRepositories.put( uri, new SoftReference<Object>( metadataRepository ) );
        }

        if ( artifactRepository != null )
        {
            artifactRepositories.put( uri, new SoftReference<Object>( artifactRepository ) );
        }
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
