package org.sonatype.tycho.p2;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRequest;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.AbstractArtifactRepository;

@SuppressWarnings( "restriction" )
public class TransientArtifactRepository
    extends AbstractArtifactRepository
{

    private Set<IArtifactDescriptor> descriptors = new LinkedHashSet<IArtifactDescriptor>();

    private Set<IArtifactKey> keys = new LinkedHashSet<IArtifactKey>();

    public TransientArtifactRepository()
    {
        super( "TemporaryArtifactRepository", TransientArtifactRepository.class.getName(), "1.0.0", null, null, null,
               null );
    }

    @Override
    public boolean contains( IArtifactDescriptor descriptor )
    {
        return descriptors.contains( descriptor );
    }

    @Override
    public boolean contains( IArtifactKey key )
    {
        return keys.contains( key );
    }

    @Override
    public IStatus getArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public IArtifactDescriptor[] getArtifactDescriptors( IArtifactKey key )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public IArtifactKey[] getArtifactKeys()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus getArtifacts( IArtifactRequest[] requests, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getOutputStream( IArtifactDescriptor descriptor )
        throws ProvisionException
    {
        descriptors.add( descriptor );
        return new OutputStream()
        {
            @Override
            public void write( int b )
                throws IOException
            {
            }
        };
    }

    @Override
    public void addDescriptor( IArtifactDescriptor descriptor )
    {
        descriptors.add( descriptor );
    }

    @Override
    public void addDescriptors( IArtifactDescriptor[] descriptors )
    {
        this.descriptors.addAll( Arrays.asList( descriptors ) );
    }

    public IStatus getRawArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
    }

    public Set<IArtifactDescriptor> getArtifactDescriptors()
    {
        return descriptors;
    }

    @Override
    public boolean isModifiable()
    {
        return true;
    }
}
