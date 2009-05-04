package org.sonatype.tycho.p2.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryIO;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryIO;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.junit.Test;
import org.sonatype.tycho.p2.Activator;

@SuppressWarnings("restriction")
public class SiteTest
{
    @Test
    public void test() throws Exception
    {
        IProxyService proxyService = (IProxyService) ServiceHelper.getService(
            Activator.getContext(),
            IProxyService.class.getName() );
        
        proxyService.setProxiesEnabled( true );
        proxyService.setSystemProxiesEnabled( true );
        proxyService.setProxyData( null );

        
        IMetadataRepositoryManager metadataRepositoryManager = (IMetadataRepositoryManager) ServiceHelper.getService(
            Activator.getContext(),
            IMetadataRepositoryManager.class.getName() );
        if ( metadataRepositoryManager == null )
        {
            throw new IllegalStateException( "No metadata repository manager found" ); //$NON-NLS-1$
        }

        IArtifactRepositoryManager artifactRepositoryManager = (IArtifactRepositoryManager) ServiceHelper.getService(
            Activator.getContext(),
            IArtifactRepositoryManager.class.getName() );
        if ( artifactRepositoryManager == null )
        {
            throw new IllegalStateException( "No artifact repository manager found" ); //$NON-NLS-1$
        }

//        URI location = new URL( "http://subclipse.tigris.org/update_1.4.x" ).toURI();
        URI location = new URL( "http://download.eclipse.org/releases/ganymede" ).toURI();
        NullProgressMonitor monitor = new NullProgressMonitor();
        IMetadataRepository metadataRepository = metadataRepositoryManager.loadRepository( location, monitor );
        IArtifactRepository artifactRepository = artifactRepositoryManager.loadRepository( location, monitor );

        File metadataFile = new File( "target/content.xml" );
        metadataFile.getParentFile().mkdirs();

        OutputStream os = new FileOutputStream( metadataFile );
        try
        {
            new MetadataRepositoryIO().write( metadataRepository, os );
        }
        finally
        {
            os.close();
        }

        File artifactsFile = new File( "target/artifacts.xml" );
        artifactsFile.getParentFile().mkdirs();

        os = new FileOutputStream( artifactsFile );
        try
        {
            SimpleArtifactRepository simple = (SimpleArtifactRepository) artifactRepository.getAdapter( SimpleArtifactRepository.class );
            new SimpleArtifactRepositoryIO().write( simple, os );
        }
        finally
        {
            os.close();
        }
        System.out.println( metadataRepository );

//        artifactRepositories.add(  );
        
    }
}
