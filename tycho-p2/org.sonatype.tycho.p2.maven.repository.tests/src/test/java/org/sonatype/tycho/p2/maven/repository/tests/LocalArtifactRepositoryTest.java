package org.sonatype.tycho.p2.maven.repository.tests;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.maven.repository.LocalArtifactRepository;

@SuppressWarnings( "restriction" )
public class LocalArtifactRepositoryTest
{

    private File basedir = new File( "target/repository" ).getAbsoluteFile();

    @Before
    public void cleanupRepository()
    {
        deleteDir( basedir );
    }

    private void deleteDir( File dir )
    {
        File[] files = dir.listFiles();
        if ( files != null )
        {
            for ( File file : files )
            {
                if ( file.isDirectory() )
                {
                    deleteDir( file );

                }
                file.delete();
            }
        }
    }

    @Test
    public void getP2Location()
    {
        LocalArtifactRepository repo = new LocalArtifactRepository( basedir );

        ArtifactDescriptor desc = newBundleArtifactDescriptor( false );

        URI location = repo.getLocation( desc );
        Assert.assertEquals( new File( basedir, "p2/osgi/bundle/org.sonatype.tycho.test.p2/1.0.0/org.sonatype.tycho.test.p2-1.0.0.jar" ).toURI(), location );

        ProcessingStepDescriptor[] steps = new ProcessingStepDescriptor[] { 
              new ProcessingStepDescriptor( "org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true ) 
        };
        desc.setProcessingSteps( steps );
        desc.setProperty( IArtifactDescriptor.FORMAT, "packed" );

        location = repo.getLocation( desc );
        Assert.assertEquals( new File( basedir, "p2/osgi/bundle/org.sonatype.tycho.test.p2/1.0.0/org.sonatype.tycho.test.p2-1.0.0-pack200.jar.pack.gz" ).toURI(),
                             location );
    }

    private ArtifactDescriptor newBundleArtifactDescriptor( boolean maven )
    {
        ArtifactKey key =
            new ArtifactKey( PublisherHelper.OSGI_BUNDLE_CLASSIFIER, "org.sonatype.tycho.test."
                + ( maven ? "maven" : "p2" ), Version.createOSGi( 1, 0, 0 ) );
        ArtifactDescriptor desc = new ArtifactDescriptor( key );

        if ( maven )
        {
            desc.setProperty( RepositoryLayoutHelper.PROP_GROUP_ID, "group" );
            desc.setProperty( RepositoryLayoutHelper.PROP_ARTIFACT_ID, key.getId() );
            desc.setProperty( RepositoryLayoutHelper.PROP_VERSION, key.getVersion().toString() );
        }

        return desc;
    }

    @Test
    public void getMavenLocation()
    {
        LocalArtifactRepository repo = new LocalArtifactRepository( basedir );

        ArtifactDescriptor desc = newBundleArtifactDescriptor( true );

        URI location = repo.getLocation( desc );
        Assert.assertEquals( new File( basedir,
                                       "group/org.sonatype.tycho.test.maven/1.0.0/org.sonatype.tycho.test.maven-1.0.0.jar" ).toURI(),
                             location );
    }

    @Test
    public void addP2Artifact()
        throws Exception
    {
        LocalArtifactRepository repo = new LocalArtifactRepository( basedir );

        ArtifactDescriptor desc = newBundleArtifactDescriptor( false );

        writeDummyArtifact( repo, desc );

        Assert.assertTrue( new File( basedir, "p2/osgi/bundle/org.sonatype.tycho.test.p2/1.0.0/org.sonatype.tycho.test.p2-1.0.0.jar" ).exists() );
        Assert.assertTrue( repo.contains( desc.getArtifactKey() ) );
        Assert.assertTrue( repo.contains( desc ) );
    }

    private void writeDummyArtifact( LocalArtifactRepository repo, ArtifactDescriptor desc )
        throws ProvisionException, IOException
    {
        OutputStream os = repo.getOutputStream( desc );
        os.write( 111 );
        os.close();
    }

    @Test
    public void addMavenArtifact()
        throws Exception
    {
        LocalArtifactRepository repo = new LocalArtifactRepository( basedir );

        ArtifactDescriptor desc = newBundleArtifactDescriptor( true );

        writeDummyArtifact( repo, desc );

        Assert.assertTrue( new File( basedir,
                                     "group/org.sonatype.tycho.test.maven/1.0.0/org.sonatype.tycho.test.maven-1.0.0.jar" ).exists() );
        Assert.assertTrue( repo.contains( desc.getArtifactKey() ) );
        Assert.assertTrue( repo.contains( desc ) );
    }

    @Test
    public void reload()
        throws Exception
    {
        LocalArtifactRepository repo = new LocalArtifactRepository( basedir );
        ArtifactDescriptor mavenArtifact = newBundleArtifactDescriptor( true );
        ArtifactDescriptor p2Artifact = newBundleArtifactDescriptor( false );

        writeDummyArtifact( repo, mavenArtifact );
        writeDummyArtifact( repo, p2Artifact );

        repo.save();

        repo = new LocalArtifactRepository( basedir );
        Assert.assertTrue( repo.contains( mavenArtifact.getArtifactKey() ) );
        Assert.assertTrue( repo.contains( p2Artifact.getArtifactKey() ) );
    }

}
