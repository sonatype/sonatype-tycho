package org.sonatype.tycho.p2.maven.repository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

@SuppressWarnings( "restriction" )
public class MavenMirrorRequest
    extends MirrorRequest
{

    private final LocalArtifactRepository localRepository;

    public MavenMirrorRequest( IArtifactKey key, LocalArtifactRepository localRepository )
    {
        super( key, localRepository, null, null );

        this.localRepository = localRepository;
    }

    @Override
    public void perform( IProgressMonitor monitor )
    {
        IArtifactDescriptor descriptor = getArtifactDescriptor();

        if ( source instanceof AbstractMavenArtifactRepository )
        {
            IStatus result = null;

            if ( descriptor != null )
            {
                // resolve artifact
                result = ((AbstractMavenArtifactRepository) source).resolve( descriptor );

                // update local metadata tycho index if successful
                if ( result != null && result.isOK() )
                {
                    localRepository.addDescriptor( descriptor );
                }
            }

            if ( result == null )
            {
                result = new Status( IStatus.ERROR, Activator.ID, "Could not resovle artifact " + artifact + " from repository " + source.getName() );
            }

            setResult( result );

            return;
        }

        // not a maven repository, check local repo to avoid duplicate downloads

        if ( descriptor != null && localRepository.contains( descriptor ) )
        {
            setResult( Status.OK_STATUS );

            return;
        }

        // not a maven repo and not in maven local repo, delegate to p2 implementation

        super.perform( monitor );
    }

    private IArtifactDescriptor getArtifactDescriptor()
    {
        for ( IArtifactDescriptor descriptor : source.getArtifactDescriptors( getArtifactKey() ) )
        {
            if( descriptor.getProperty(IArtifactDescriptor.FORMAT) == null )
            {
                return descriptor;
            }
        }

        return null;
    }

}
