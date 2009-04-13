package org.sonatype.tycho.p2.test;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Properties;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.junit.Test;
import org.sonatype.tycho.p2.Activator;
import org.sonatype.tycho.p2.ApplicableFragmentsQuery;

@SuppressWarnings( "restriction" )
public class RepositoryTest
{
    @Test
    public void test()
        throws Exception
    {
        IMetadataRepositoryManager metadataRepositoryManager = (IMetadataRepositoryManager) ServiceHelper.getService(
            Activator.getContext(),
            IMetadataRepositoryManager.class.getName() );
        if ( metadataRepositoryManager == null )
        {
            throw new IllegalStateException( "No metadata repository manager found" ); //$NON-NLS-1$
        }

        URI location = new URL( "http://download.eclipse.org/eclipse/updates/3.4" ).toURI();
        NullProgressMonitor monitor = new NullProgressMonitor();
        IMetadataRepository metadataRepository = metadataRepositoryManager.loadRepository( location, monitor );

        Collector ius = metadataRepository.query( InstallableUnitQuery.ANY, new Collector(), monitor );

        for ( IInstallableUnit iu : (Collection<IInstallableUnit>) ius.toCollection() )
        {
            if ( iu.getFilter() != null )
            {
                System.out.println( iu );
            }
        }

        System.out.println( "================" );

        Properties newSelectionContext = new Properties();
        newSelectionContext.put( "osgi.arch", "x86_64" );
        newSelectionContext.put( "org.eclipse.equinox.p2.roaming", "true" );
        newSelectionContext.put( "osgi.ws", "gtk" );
        newSelectionContext.put( "org.eclipse.equinox.p2.cache", "/tmp/p2tmp/" );
        newSelectionContext.put( "org.eclipse.equinox.p2.installFolder", "/tmp/p2tmp/" );
        newSelectionContext.put( "org.eclipse.equinox.p2.environments", "osgi.ws=gtk,osgi.os=linux,osgi.arch=x86_64" );
        newSelectionContext.put( "org.eclipse.equinox.p2.flavor", "tooling" );
        newSelectionContext.put( "osgi.os", "linux" );
        newSelectionContext.put( "org.eclipse.update.install.features", "true" );

        Slicer slicer = new Slicer( metadataRepository, newSelectionContext );

        Collector rootIUs = metadataRepository.query(
            new InstallableUnitQuery( "org.eclipse.swt" ),
            new Collector(),
            monitor );

        Collector slice = slicer
            .slice( (IInstallableUnit[]) rootIUs.toArray( IInstallableUnit.class ), monitor ).query(
                InstallableUnitQuery.ANY,
                new Collector(),
                monitor );

        ApplicableFragmentsQuery fragementQuery = new ApplicableFragmentsQuery(
            slice.toCollection(),
            newSelectionContext );

        Collector fragements = metadataRepository.query( fragementQuery, new Collector(), monitor );

        System.out.println( "================" );

        for ( IInstallableUnit iu : (Collection<IInstallableUnit>) fragements.toCollection() )
        {
            System.out.println( iu );
        }
    }
}
