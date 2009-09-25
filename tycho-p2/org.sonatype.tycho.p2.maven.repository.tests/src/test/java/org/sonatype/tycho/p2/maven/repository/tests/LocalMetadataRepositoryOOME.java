package org.sonatype.tycho.p2.maven.repository.tests;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings( "restriction" )
public class LocalMetadataRepositoryOOME
{
    private IProgressMonitor monitor = new NullProgressMonitor();

    private IMetadataRepositoryManager manager;

    @Before
    public void initializeMetadataRepositoryManager()
    {
        manager =
            (IMetadataRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IMetadataRepositoryManager.class.getName() );
        if ( manager == null )
        {
            throw new IllegalStateException( "No metadata repository manager found" ); //$NON-NLS-1$
        }
    }

    @Test
    public void loadLocalRepo()
        throws Exception
    {
        File location = new File( "/home/igor/.m2/repository" );
        IMetadataRepository repository = manager.loadRepository( location.toURI(), monitor );

        Collector collector = repository.query( InstallableUnitQuery.ANY, new Collector(), monitor );

        System.out.println( collector.size() );
    }
}
