package org.sonatype.tycho.p2;

import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.MetadataRepositoryFactory;

@SuppressWarnings( "restriction" )
public class TransientMetadataRepositoryFactory
    extends MetadataRepositoryFactory
{

    @Override
    public IMetadataRepository create( URI location, String name, String type, Map properties )
        throws ProvisionException
    {
        return new TransientMetadataRepository( location, name, type, properties );
    }

    @Override
    public IMetadataRepository load( URI location, int flags, IProgressMonitor monitor )
        throws ProvisionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus validate( URI location, IProgressMonitor monitor )
    {
        return Status.OK_STATUS;
    }

}
