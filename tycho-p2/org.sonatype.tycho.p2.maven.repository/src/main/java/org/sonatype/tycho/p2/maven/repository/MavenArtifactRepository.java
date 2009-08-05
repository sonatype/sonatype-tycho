package org.sonatype.tycho.p2.maven.repository;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRequest;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRequest;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.facade.internal.GAV;
import org.sonatype.tycho.p2.facade.internal.RepositoryReader;
import org.sonatype.tycho.p2.facade.internal.TychoRepositoryIndex;

@SuppressWarnings( "restriction" )
public class MavenArtifactRepository
    extends AbstractMavenArtifactRepository
{

    public MavenArtifactRepository( URI location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator )
    {
        super( location, projectIndex, contentLocator );
    }

    @Override
    public IStatus getArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        return downloadArtifact( descriptor, destination );
    }

    public IStatus getRawArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        return downloadArtifact( descriptor, destination );
    }

    private IStatus downloadArtifact( IArtifactDescriptor descriptor, OutputStream destination )
    {
        GAV gav = RepositoryLayoutHelper.getGAV( descriptor.getProperties() );

        if ( gav == null )
        {
            return new Status( IStatus.ERROR, Activator.ID, "Not a Maven artifact" );
        }

        try
        {
            // TODO properly deal with pack200 and binary artifacts
            InputStream contents = new BufferedInputStream( getContentLocator().getContents(
                gav,
                null /* classifier */,
                "jar" /* extension */) );

            try
            {
                if ( destination != null )
                {
                    FileUtils.copyStream( contents, false, destination, false );
                }
            }
            finally
            {
                contents.close();
            }
        }
        catch ( IOException e )
        {
            return new Status( IStatus.ERROR, Activator.ID, "Could not retrieve Maven artifact", e );
        }

        return Status.OK_STATUS;
    }

    @Override
    public IStatus getArtifacts( IArtifactRequest[] requests, IProgressMonitor monitor )
    {
        final MultiStatus overallStatus = new MultiStatus( Activator.ID, IStatus.OK, null, null );

        SubMonitor subMonitor = SubMonitor.convert( monitor, requests.length );
        try
        {
            for ( int i = 0; i < requests.length; i++ )
            {
                if ( monitor.isCanceled() )
                {
                    return Status.CANCEL_STATUS;
                }
                IStatus result = getArtifact( (ArtifactRequest) requests[i], subMonitor.newChild( 1 ) );
                if ( !result.isOK() )
                {
                    overallStatus.add( result );
                }
            }
        }
        finally
        {
            subMonitor.done();
        }

        return overallStatus;
    }

    private IStatus getArtifact( ArtifactRequest request, IProgressMonitor monitor )
    {
        request.setSourceRepository( this );
        request.perform( monitor );
        return request.getResult();
    }

    @Override
    public OutputStream getOutputStream( IArtifactDescriptor descriptor )
        throws ProvisionException
    {
        // can't really happen for this read-only repository
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus resolve( IArtifactDescriptor descriptor )
    {
        return downloadArtifact( descriptor, null );
    }
}
