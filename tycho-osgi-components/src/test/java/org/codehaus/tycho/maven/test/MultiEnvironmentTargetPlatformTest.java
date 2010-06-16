package org.codehaus.tycho.maven.test;

import java.util.Arrays;
import java.util.HashSet;

import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.osgitools.targetplatform.DefaultTargetPlatform;
import org.codehaus.tycho.osgitools.targetplatform.MultiEnvironmentTargetPlatform;
import org.codehaus.tycho.p2.MetadataSerializable;
import org.codehaus.tycho.p2.MetadataSerializableMerger;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class MultiEnvironmentTargetPlatformTest
{

    private MultiEnvironmentTargetPlatform subject;
    private MetadataSerializableMerger<MetadataSerializable> merger;

    @SuppressWarnings( "unchecked" )
    @Before
    public void setUp() {
        merger = EasyMock.createMock( MetadataSerializableMerger.class );
        
        subject = new MultiEnvironmentTargetPlatform(merger);
    }
    
    @Test
    public void testGetP2MetadataSerializableSingleEnvironment() {
        MetadataSerializable repo = createNiceRepoMock();
        subject.addPlatform( new TargetEnvironment("gtk", "linux", "x86", "en"), createTargetPlatform( repo ) );
        
        MetadataSerializable mergedSerializable = EasyMock.createNiceMock( MetadataSerializable.class );
        EasyMock.expect( merger.merge( new HashSet<MetadataSerializable>(Arrays.asList( repo )) ) ).andReturn( mergedSerializable );

        EasyMock.replay(  merger );
        
        MetadataSerializable actual = subject.getP2MetadataSerializable();
        
        Assert.assertSame( mergedSerializable, actual );
    }

    @Test
    public void testGetP2MetadataSerializableMultipleEnvironments() {
        MetadataSerializable repo1 = createNiceRepoMock();
        subject.addPlatform( new TargetEnvironment("gtk", "linux", "x86", "en"), createTargetPlatform( repo1 ) );
        
        MetadataSerializable repo2 = createNiceRepoMock();
        subject.addPlatform( new TargetEnvironment("win32", "win32", "x86", "en"), createTargetPlatform( repo2 ) );

        MetadataSerializable mergedSerializable = EasyMock.createNiceMock( MetadataSerializable.class );
        EasyMock.expect( merger.merge( new HashSet<MetadataSerializable>(Arrays.asList( repo1, repo2 )) ) ).andReturn( mergedSerializable );

        EasyMock.replay(  merger );
        
        MetadataSerializable actual = subject.getP2MetadataSerializable();
        
        Assert.assertSame( mergedSerializable, actual );
    }

    private MetadataSerializable createNiceRepoMock()
    {
        MetadataSerializable repo = EasyMock.createNiceMock( MetadataSerializable.class );
        EasyMock.replay( repo );
        return repo;
    }

    private DefaultTargetPlatform createTargetPlatform( MetadataSerializable platformSerializable )
    {
        DefaultTargetPlatform platform = new DefaultTargetPlatform();
        platform.setP2MetadataRepository( platformSerializable );
        return platform;
    }

}
