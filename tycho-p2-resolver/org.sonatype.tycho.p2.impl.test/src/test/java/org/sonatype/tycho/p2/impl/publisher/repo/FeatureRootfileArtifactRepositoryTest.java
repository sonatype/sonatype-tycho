package org.sonatype.tycho.p2.impl.publisher.repo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.p2.IArtifactFacade;
import org.sonatype.tycho.p2.impl.publisher.FeatureRootAdvice;
import org.sonatype.tycho.p2.impl.publisher.MavenPropertiesAdvice;

@SuppressWarnings( "restriction" )
public class FeatureRootfileArtifactRepositoryTest
{
    private final static File DEFAULT_OUTPUT_DIR = new File( "resources/rootfiles" );

    @Test
    public void testRepoWithAttachedArtifacts()
        throws Exception
    {
        FeatureRootfileArtifactRepository subject =
            new FeatureRootfileArtifactRepository( createPublisherInfo( true ), DEFAULT_OUTPUT_DIR );

        IArtifactDescriptor artifactDescriptor =
            createArtifactDescriptor( PublisherHelper.BINARY_ARTIFACT_CLASSIFIER, "org.sonatype.tycho.test.p2" );

        subject.getOutputStream( artifactDescriptor );

        assertAttachedArtifact( subject.getPublishedArtifacts(), 1, "root", "org.sonatype.tycho.test.p2-1.0.0-root.zip" );

        Set<IArtifactDescriptor> artifactDescriptors = subject.getArtifactDescriptors();
        Assert.assertTrue( artifactDescriptors.size() == 1 );

        IArtifactDescriptor descriptor = artifactDescriptors.iterator().next();
        assertMavenProperties( descriptor, "root" );
    }

    @Test
    public void testRepoWithAttachedArtifactsAndConfigurations()
        throws Exception
    {
        FeatureRootfileArtifactRepository subject =
            new FeatureRootfileArtifactRepository( createPublisherInfo( true ), DEFAULT_OUTPUT_DIR );

        IArtifactDescriptor artifactDescriptor =
            createArtifactDescriptor( PublisherHelper.BINARY_ARTIFACT_CLASSIFIER,
                                      "org.sonatype.tycho.test.p2.win32.win32.x86" );

        subject.getOutputStream( artifactDescriptor );

        assertAttachedArtifact( subject.getPublishedArtifacts(), 1, "root.win32.win32.x86",
                                "org.sonatype.tycho.test.p2.win32.win32.x86-1.0.0-root.zip" );

        Set<IArtifactDescriptor> artifactDescriptors = subject.getArtifactDescriptors();
        Assert.assertTrue( artifactDescriptors.size() == 1 );

        IArtifactDescriptor descriptor = artifactDescriptors.iterator().next();
        assertMavenProperties( descriptor, "root.win32.win32.x86" );
    }

    @Test( expected = ProvisionException.class )
    public void testRepoWithoutMavenAdvice()
        throws ProvisionException
    {
        FeatureRootfileArtifactRepository subject =
            new FeatureRootfileArtifactRepository( createPublisherInfo( false ), DEFAULT_OUTPUT_DIR );

        IArtifactDescriptor artifactDescriptor =
            createArtifactDescriptor( PublisherHelper.BINARY_ARTIFACT_CLASSIFIER, "org.sonatype.tycho.test.p2" );
        subject.getOutputStream( artifactDescriptor );
    }

    @Test
    public void testRepoForNonBinaryArtifacts()
        throws ProvisionException
    {
        FeatureRootfileArtifactRepository subject =
            new FeatureRootfileArtifactRepository( createPublisherInfo( true ), DEFAULT_OUTPUT_DIR );

        IArtifactDescriptor artifactDescriptor =
            createArtifactDescriptor( "non-binary-classifier", "org.sonatype.tycho.test.p2" );
        subject.getOutputStream( artifactDescriptor );

        Map<String, IArtifactFacade> attachedArtifacts = subject.getPublishedArtifacts();
        Assert.assertTrue( attachedArtifacts.size() == 0 );
    }

    @Test
    public void testRepoWithInitEmptyAttachedArtifacts()
    {
        FeatureRootfileArtifactRepository subject = new FeatureRootfileArtifactRepository( null, null );
        Assert.assertTrue( subject.getPublishedArtifacts().isEmpty() );
    }

    private void assertMavenProperties( IArtifactDescriptor descriptor, String root )
    {
        Assert.assertEquals( descriptor.getProperty( "maven-groupId" ), "artifactGroupId" );
        Assert.assertEquals( descriptor.getProperty( "maven-artifactId" ), "artifactId" );
        Assert.assertEquals( descriptor.getProperty( "maven-version" ), "artifactVersion" );
        Assert.assertEquals( descriptor.getProperty( "maven-classifier" ), root );
        Assert.assertEquals( descriptor.getProperty( "maven-extension" ), "zip" );
    }

    private void assertAttachedArtifact( Map<String, IArtifactFacade> attachedArtifacts, int expectedSize,
                                         String expectedClassifier, String expectedLocationFileName )
    {
        Assert.assertTrue( attachedArtifacts.size() == 1 );

        IArtifactFacade artifactFacade = attachedArtifacts.get( expectedClassifier );

        Assert.assertEquals( artifactFacade.getClassidier(), expectedClassifier );
        Assert.assertEquals( artifactFacade.getLocation().getName(), expectedLocationFileName );
    }

    private PublisherInfo createPublisherInfo( boolean addMavenPropertyAdvice )
    {
        PublisherInfo publisherInfo = new PublisherInfo();

        publisherInfo.addAdvice( createFeatureRootAdvice() );
        if ( addMavenPropertyAdvice )
        {
            publisherInfo.addAdvice( createMavenPropertyAdvice() );
        }

        return publisherInfo;
    }

    private FeatureRootAdvice createFeatureRootAdvice()
    {
        return new FeatureRootAdvice( createRootFilesMap(), "artifactId" );
    }

    private MavenPropertiesAdvice createMavenPropertyAdvice()
    {
        return new MavenPropertiesAdvice( "artifactGroupId", "artifactId", "artifactVersion" );
    }

    private ArtifactDescriptor createArtifactDescriptor( String classifier, String artifactId )
    {
        ArtifactKey key = new ArtifactKey( classifier, artifactId, Version.createOSGi( 1, 0, 0 ) );
        ArtifactDescriptor desc = new ArtifactDescriptor( key );
        return desc;
    }

    private HashMap<String, Map<File, IPath>> createRootFilesMap()
    {
        HashMap<String, Map<File, IPath>> rootFilesMap = new HashMap<String, Map<File, IPath>>();

        HashMap<File, IPath> winRootFileMap = new HashMap<File, IPath>();
        winRootFileMap.put( new File( "rootfiles\file1.txt" ), new Path( "file1.txt" ) );

        HashMap<File, IPath> defaultRootFileMap = new HashMap<File, IPath>();
        defaultRootFileMap.put( new File( "rootfiles\file2.txt" ), new Path( "file2.txt" ) );

        rootFilesMap.put( "win32.win32.x86", winRootFileMap );
        rootFilesMap.put( "", defaultRootFileMap );

        return rootFilesMap;
    }
}
