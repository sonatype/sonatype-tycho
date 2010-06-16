package org.sonatype.tycho.p2.repo;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.tycho.p2.Activator;
import org.sonatype.tycho.p2.repo.MetadataSerializableImpl;
import org.sonatype.tycho.p2.repo.MetadataSerializableMergerImpl;
import org.sonatype.tycho.test.util.InstallableUnitUtil;

public class MetadataSerializableMergerImplTest
{

    private IProvisioningAgent agent;

    @Before
    public void setUp()
        throws ProvisionException
    {
        agent = Activator.newProvisioningAgent();
    }
    
    @After
    public void tearDown(){
        agent.stop();
    }

    @Test
    public void testMergeSerializables()
        throws ProvisionException
    {
        IInstallableUnit unit1 = InstallableUnitUtil.createIU( "org.example.test", "1.0.0" );
        IInstallableUnit unit2 = InstallableUnitUtil.createIU( "org.example.test2", "2.0.0" );
        IInstallableUnit unit3 = InstallableUnitUtil.createIU( "org.example.test3", "3.0.0" );
        
        Collection<MetadataSerializableImpl> repos = new HashSet<MetadataSerializableImpl>();
        
        repos.add( new MetadataSerializableImpl( Arrays.asList( unit1, unit2 ), agent ) );
        repos.add( new MetadataSerializableImpl( Arrays.asList( unit1, unit3 ), agent ) );

        MetadataSerializableMergerImpl subject = new MetadataSerializableMergerImpl();
        
        MetadataSerializableImpl mergedRepo = (MetadataSerializableImpl) subject.merge( repos );
        Assert.assertEquals("Merged repo should contain three IUs." , 3 , mergedRepo.getUnits().size());
        Assert.assertTrue( "Merged repo should contain unit 1.", mergedRepo.getUnits().contains( unit1 ));
        Assert.assertTrue( "Merged repo should contain unit 2.", mergedRepo.getUnits().contains( unit2 ));
        Assert.assertTrue( "Merged repo should contain unit 3.", mergedRepo.getUnits().contains( unit3 ));

    }
}
