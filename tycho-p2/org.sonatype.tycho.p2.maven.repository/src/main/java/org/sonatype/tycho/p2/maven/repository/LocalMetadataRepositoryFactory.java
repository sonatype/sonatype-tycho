package org.sonatype.tycho.p2.maven.repository;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.MetadataRepositoryFactory;

@SuppressWarnings( "restriction" )
public class LocalMetadataRepositoryFactory
    extends MetadataRepositoryFactory
{

    @Override
    public IMetadataRepository create( URI location, String name, String type, Map properties )
        throws ProvisionException
    {
        return new LocalMetadataRepository( location, name, properties );
    }

    @Override
    public IMetadataRepository load( URI location, int flags, IProgressMonitor monitor )
        throws ProvisionException
    {
        File basedir = new File( location );
        if ( new File( basedir, LocalMetadataRepository.METADATA_FILE ).exists() )
        {
            return new LocalMetadataRepository( location );
        }

        // not our repository
        return null;
    }

    @Override
    public IStatus validate( URI location, IProgressMonitor monitor )
    {
        return null;
    }

}
