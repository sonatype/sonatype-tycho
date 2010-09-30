package org.sonatype.tycho.p2.maven.repository;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.sonatype.tycho.p2.repository.LocalTychoRepositoryIndex;

public class LocalArtifactRepositoryFactory
    extends ArtifactRepositoryFactory
{

    @Override
    public IArtifactRepository create( URI location, String name, String type, Map<String, String> properties )
        throws ProvisionException
    {
        throw new ProvisionException( new Status( IStatus.ERROR, Activator.ID,
                                                  ProvisionException.REPOSITORY_UNKNOWN_TYPE,
                                                  "This factory does not support creation of repositories", null ) );
    }

    @Override
    public IArtifactRepository load( URI location, int flags, IProgressMonitor monitor )
        throws ProvisionException
    {
        if ( location.getScheme().equals( "file" ) )
        {
            final File localRepositoryDirectory = new File( location );
            if ( localRepositoryDirectory.isDirectory()
                && new File( localRepositoryDirectory, LocalTychoRepositoryIndex.ARTIFACTS_INDEX_RELPATH ).exists() )
            {
                return new LocalArtifactRepository( localRepositoryDirectory );
            } 
        }
        throw new ProvisionException( new Status( IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND,
                                                  "No local tycho repository found at: " + location, null ) );
    }

}
