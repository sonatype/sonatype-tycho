package com.sonatype.nexus.p2.impl.test;

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryIO;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.junit.Test;
import org.sonatype.tycho.p2.Activator;

@SuppressWarnings( "restriction" )
public class GalileoProxyTest
{
    private IProgressMonitor monitor = new ConsoleProgressMonitor();

    private URI location;

    public GalileoProxyTest()
        throws Exception
    {
        location = new URI( "http://download.eclipse.org/eclipse/updates/3.6-I-builds" );
    }

    @Test
    public void testArtifactRepository()
        throws Exception
    {
        IArtifactRepositoryManager artifactRepositoryManager =
            (IArtifactRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IArtifactRepositoryManager.class.getName() );

        IArtifactRepository artifactRepository = artifactRepositoryManager.loadRepository( location, monitor );
        SimpleArtifactRepository simple =
            (SimpleArtifactRepository) artifactRepository.getAdapter( SimpleArtifactRepository.class );
        new SimpleArtifactRepositoryIO().write( simple, System.out );

        System.out.println( simple );
    }

    @Test
    public void testMetadataRepository()
        throws Exception
    {
        IMetadataRepositoryManager repositoryManager =
            (IMetadataRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IMetadataRepositoryManager.class.getName() );

        IMetadataRepository repository = repositoryManager.loadRepository( location, monitor );

        File localFile = new File( "/tmp/xxx" );
        IMetadataRepository localRepository =
            repositoryManager.createRepository( localFile.toURI(), localFile.getName(),
                                                IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null );
        repositoryManager.removeRepository( localFile.toURI() );

        Collector collector = repository.query( InstallableUnitQuery.ANY, new Collector(), monitor );

        localRepository.addInstallableUnits( (IInstallableUnit[]) collector.toArray( IInstallableUnit.class ) );
    }
}
