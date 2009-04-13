package org.sonatype.tycho.p2.test;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.director.Projector;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryIO;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings( "restriction" )
public class PorjectorTest
{

    private IProgressMonitor monitor = new NullProgressMonitor();

    @Test
    public void fragments()
        throws Exception
    {
        Properties newSelectionContext = newSelectionContext();
        
        InputStream is = getClass().getClassLoader().getResourceAsStream( "org/sonatype/tycho/p2/test/repo.xml" );

        IMetadataRepository repository;
        try
        {
            repository = new MetadataRepositoryIO().read( new URL( "http://xxx" ), is, monitor );
        }
        finally
        {
            is.close();
        }

        Collector slice = repository.query( InstallableUnitQuery.ANY, new Collector(), monitor );

        IInstallableUnit[] rootIUs = (IInstallableUnit[]) repository.query(
            new InstallableUnitQuery( "org.maven.ide.eclipse" ),
            new Collector(),
            monitor ).toArray( IInstallableUnit.class );

        IRequiredCapability requiredSwtFragment = MetadataFactory.createRequiredCapability(
            "osgi.fragment",
            "org.eclipse.swt",
            new VersionRange( "0.0.0" ), // [3.4.1.v3452b,3.4.1.v3452b]
            null /* filter */,
            false /* optional */,
            true /* multiple */,
            true /* greedy */);
        
        
        HackedP2Projector projector = new HackedP2Projector( slice, newSelectionContext );
        projector.encode(
            createMetaIU( rootIUs, requiredSwtFragment ),
            new IInstallableUnit[0] /* alreadyExistingRoots */,
            rootIUs /* newRoots */,
            monitor );
        projector.invokeSolver( monitor );

        Collection<IInstallableUnit> newState = projector.extractSolution();
        
        Assert.assertEquals( 5, newState.size() );

    }

//    @Test
    public void fragments2()
        throws Exception
    {
        Properties newSelectionContext = newSelectionContext();
        
        InputStream is = getClass().getClassLoader().getResourceAsStream( "org/sonatype/tycho/p2/test/repo2.xml" );

        IMetadataRepository repository;
        try
        {
            repository = new MetadataRepositoryIO().read( new URL( "http://xxx" ), is, monitor );
        }
        finally
        {
            is.close();
        }

        Collector slice = repository.query( InstallableUnitQuery.ANY, new Collector(), monitor );

        IInstallableUnit[] rootIUs = (IInstallableUnit[]) repository.query(
            new InstallableUnitQuery( "org.maven.ide.eclipse" ),
            new Collector(),
            monitor ).toArray( IInstallableUnit.class );

        Projector projector = new Projector( slice, newSelectionContext );
        projector.encode(
            createMetaIU( rootIUs, null ),
            new IInstallableUnit[0] /* alreadyExistingRoots */,
            rootIUs /* newRoots */,
            monitor );
        projector.invokeSolver( monitor );

        Collection<IInstallableUnit> newState = projector.extractSolution();

        Assert.assertEquals( 5, newState.size() );

    }


//    @Test
    public void requirePackage()
        throws Exception
    {
        Properties newSelectionContext = newSelectionContext();
        
        InputStream is = getClass().getClassLoader().getResourceAsStream( "org/sonatype/tycho/p2/test/repo3.xml" );

        IMetadataRepository repository;
        try
        {
            repository = new MetadataRepositoryIO().read( new URL( "http://xxx" ), is, monitor );
        }
        finally
        {
            is.close();
        }

        Collector slice = repository.query( InstallableUnitQuery.ANY, new Collector(), monitor );

        IInstallableUnit[] rootIUs = (IInstallableUnit[]) repository.query(
            new InstallableUnitQuery( "org.maven.ide.eclipse" ),
            new Collector(),
            monitor ).toArray( IInstallableUnit.class );
        
        Tracing.DEBUG_PLANNER_PROJECTOR = true;

        Projector projector = new Projector( slice, newSelectionContext );
        projector.encode(
            createMetaIU( rootIUs, null ),
            new IInstallableUnit[0] /* alreadyExistingRoots */,
            rootIUs /* newRoots */,
            monitor );
        projector.invokeSolver( monitor );

        Collection<IInstallableUnit> newState = projector.extractSolution();

        Assert.assertEquals( 4, newState.size() );

    }
    
    
    private Properties newSelectionContext()
    {
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
        return newSelectionContext;
    }

    private IInstallableUnit createMetaIU( IInstallableUnit[] rootIUs, IRequiredCapability requiredCapability )
    {
        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        String time = Long.toString( System.currentTimeMillis() );
        iud.setId( time );
        iud.setVersion( new Version( 0, 0, 0, time ) );

        ArrayList<IRequiredCapability> capabilities = new ArrayList<IRequiredCapability>();
        for ( IInstallableUnit iu : rootIUs )
        {
            VersionRange range = new VersionRange( iu.getVersion(), true, iu.getVersion(), true );
            capabilities.add( MetadataFactory.createRequiredCapability(
                IInstallableUnit.NAMESPACE_IU_ID,
                iu.getId(),
                range,
                iu.getFilter(),
                false /* optional */,
                !iu.isSingleton() /* multiple */,
                true /* greedy */) );
        }

        if ( requiredCapability != null )
        {
            capabilities.add( requiredCapability );
        }

        iud.setRequiredCapabilities( (IRequiredCapability[]) capabilities.toArray( new IRequiredCapability[capabilities
            .size()] ) );
        return MetadataFactory.createInstallableUnit( iud );
    }


}

