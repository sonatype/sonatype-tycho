package org.eclipse.tycho.p2.impl.test;

import java.net.URI;

import org.eclipse.tycho.p2.resolver.P2RepositoryCache;

public class P2RepositoryCacheImpl
    implements P2RepositoryCache
{

    public Object getMetadataRepository( URI location )
    {
        return null;
    }

    public Object getArtifactRepository( URI location )
    {
        return null;
    }

    public void putRepository( URI location, Object metadataRepository, Object artifactRepository )
    {
    }

}
