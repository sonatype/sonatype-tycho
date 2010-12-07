package org.sonatype.tycho.p2.tools;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class RepositoryReferencesTest
{
    private static final File LOCATION_A = new File( "a/location" );

    private static final File LOCATION_B = new File( "another/location" );

    private static final File LOCATION_C = new File( "yet/another/location" );

    RepositoryReferences subject;

    @Before
    public void initSubject()
    {
        subject = new RepositoryReferences();
    }

    @Test
    public void testNoMetadataRepo()
    {
        assertEquals( 0, subject.getMetadataRepositories().size() );
    }

    @Test
    public void testNoArtifactRepo()
    {
        assertEquals( 0, subject.getArtifactRepositories().size() );
    }

    @Test
    public void testMetadataReposWithOrder()
    {
        subject.addMetadataRepository( LOCATION_B );
        subject.addMetadataRepository( LOCATION_A );

        List<URI> repositories = subject.getMetadataRepositories();

        assertEquals( 2, repositories.size() );
        assertEquals( LOCATION_B.toURI(), repositories.get( 0 ) );
        assertEquals( LOCATION_A.toURI(), repositories.get( 1 ) );
    }

    @Test
    public void testArtifactReposWithOrder()
    {
        subject.addArtifactRepository( LOCATION_B );
        subject.addArtifactRepository( LOCATION_C );
        subject.addArtifactRepository( LOCATION_A );

        List<URI> repositories = subject.getArtifactRepositories();

        assertEquals( 3, repositories.size() );
        assertEquals( LOCATION_B.toURI(), repositories.get( 0 ) );
        assertEquals( LOCATION_C.toURI(), repositories.get( 1 ) );
        assertEquals( LOCATION_A.toURI(), repositories.get( 2 ) );
    }
}
