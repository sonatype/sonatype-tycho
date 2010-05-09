package org.sonatype.tycho.p2.maven.repository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.facade.internal.GAV;

@SuppressWarnings( "restriction" )
public class MavenMirrorRequest
    extends MirrorRequest
{

    private final LocalArtifactRepository localRepository;
    private final IInstallableUnit iu;
    private final LocalMetadataRepository localMetadataRepository;

    public MavenMirrorRequest( IInstallableUnit iu, IArtifactKey key, LocalMetadataRepository localMetadataRepository, LocalArtifactRepository localRepository )
    {
        super( key, localRepository, null, null );
        this.iu = iu;
        this.localMetadataRepository = localMetadataRepository;

        this.localRepository = localRepository;
    }

    @Override
    public void perform( IArtifactRepository sourceRepository, IProgressMonitor monitor )
    {
        setSourceRepository(sourceRepository);

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

        if ( localRepository.contains( getArtifactKey() ) )
        {
            setResult( Status.OK_STATUS );

            return;
        }

        // not a maven repo and not in maven local repo, delegate to p2 implementation

        super.perform( sourceRepository, monitor );

        if ( getResult().isOK() )
        {
            // If we got here, we just copied artifact from p2 to local maven repo
            // Now need to create matching p2 metadata to describe installable unit
            GAV gav = null;
            if ( descriptor != null )
            {
                gav = localRepository.getP2GAV( descriptor );
            }
            if ( gav == null )
            {
                IArtifactKey key = getArtifactKey();
                gav = RepositoryLayoutHelper.getP2Gav( key.getClassifier(), key.getId(), key.toString() );
            }
            localMetadataRepository.addInstallableUnit( iu, gav );
        }
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
