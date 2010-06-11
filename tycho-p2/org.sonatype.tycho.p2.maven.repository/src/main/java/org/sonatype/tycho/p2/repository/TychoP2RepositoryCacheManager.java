package org.sonatype.tycho.p2.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;

import copied.org.eclipse.equinox.internal.p2.repository.CacheManager;

/**
 * @author igor
 */
public class TychoP2RepositoryCacheManager
    extends CacheManager
{
    public static final String CACHE_RELPATH = ".cache/tycho/p2-repository-metadata";

    private boolean offline;

    private File localRepositoryLocation;

    public TychoP2RepositoryCacheManager()
    {
        super( null );
    }

    @Override
    public File createCache( URI repositoryLocation, String prefix, IProgressMonitor monitor )
        throws IOException, ProvisionException
    {
        File cacheFile = getCache( repositoryLocation, prefix );
        if ( offline )
        {
            if ( cacheFile != null )
            {
                return cacheFile;
            }

            throw new ProvisionException( "Repository system is offline and no local cache available for "
                + repositoryLocation.toString() );
        }
        else
        {
            return super.createCache( repositoryLocation, prefix, monitor );
        }
    }

    @Override
    protected File getCacheDirectory()
    {
        return new File( localRepositoryLocation, CACHE_RELPATH );
    }

    public void setOffline( boolean offline )
    {
        this.offline = offline;
    }

    public void setLocalRepositoryLocation( File localRepositoryLocation )
    {
        this.localRepositoryLocation = localRepositoryLocation;
    }
}
