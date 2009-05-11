package com.sonatype.nexus.p2.impl.test;

import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryIO;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.junit.Test;
import org.sonatype.tycho.p2.Activator;

@SuppressWarnings( "restriction" )
public class GalileoProxyTest
{
    private IProgressMonitor monitor = new NullProgressMonitor();
    
    @Test
    public void test() throws Exception
    {
        IArtifactRepositoryManager artifactRepositoryManager = (IArtifactRepositoryManager) ServiceHelper.getService(
            Activator.getContext(),
            IArtifactRepositoryManager.class.getName() );

        URI location = new URI( "http://download.eclipse.org/releases/galileo" );
        IArtifactRepository artifactRepository = artifactRepositoryManager.loadRepository( location, monitor );
        SimpleArtifactRepository simple =
            (SimpleArtifactRepository) artifactRepository.getAdapter( SimpleArtifactRepository.class );
        new SimpleArtifactRepositoryIO().write( simple, System.out );

        System.out.println( simple );
    }
}
