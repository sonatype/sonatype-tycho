package org.sonatype.tycho.p2.maven.repository.tests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.tycho.p2.maven.repository.LocalArtifactRepository;
import org.sonatype.tycho.p2.maven.repository.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.maven.repository.xstream.PropertiesConverter;
import org.sonatype.tycho.p2.maven.repository.xstream.VersionConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.io.xml.XppDriver;

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

        Assert.assertEquals( new File( basedir, "p2/plugins/org.sonatype.tycho.test.p2_1.0.0.jar" ).toURI(), repo
            .getLocation( desc ) );

        ProcessingStepDescriptor[] steps = new ProcessingStepDescriptor[] { new ProcessingStepDescriptor(
            "org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true ) }; //$NON-NLS-1$
        desc.setProcessingSteps( steps );
        desc.setProperty( IArtifactDescriptor.FORMAT, "packed" );

        Assert.assertEquals(
            new File( basedir, "p2/plugins/org.sonatype.tycho.test.p2_1.0.0.jar.pack.gz" ).toURI(),
            repo.getLocation( desc ) );
    }

    private ArtifactDescriptor newBundleArtifactDescriptor( boolean maven )
    {
        ArtifactKey key = new ArtifactKey( PublisherHelper.OSGI_BUNDLE_CLASSIFIER, "org.sonatype.tycho.test."
            + ( maven ? "maven" : "p2" ), new Version( 1, 0, 0 ) );
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

        Assert.assertEquals( new File(
            basedir,
            "group/org.sonatype.tycho.test.maven/1.0.0/org.sonatype.tycho.test.maven-1.0.0.jar" ).toURI(), repo
            .getLocation( desc ) );
    }

    @Test
    public void addP2Artifact()
        throws Exception
    {
        LocalArtifactRepository repo = new LocalArtifactRepository( basedir );

        ArtifactDescriptor desc = newBundleArtifactDescriptor( false );

        writeDummyArtifact( repo, desc );

        Assert.assertTrue( new File( basedir, "p2/plugins/org.sonatype.tycho.test.p2_1.0.0.jar" ).exists() );
        Assert.assertTrue( repo.contains( desc.getArtifactKey() ) );
        Assert.assertTrue( repo.contains( desc ) );
    }

    private void writeDummyArtifact( LocalArtifactRepository repo, ArtifactDescriptor desc )
        throws ProvisionException,
            IOException
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

        Assert.assertTrue( new File(
            basedir,
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

    public void xstream()
        throws Exception
    {
        IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService( Activator
            .getContext(), IArtifactRepositoryManager.class.getName() );
        if ( manager == null )
        {
            throw new IllegalStateException( "No artifact repository manager found" ); //$NON-NLS-1$
        }

        IArtifactRepository repository = manager.loadRepository(
            new File( "/var/tmp/p2/ganymede-sr2/" ).toURI(),
            new NullProgressMonitor() );
        ArrayList<IArtifactDescriptor> descriptors = new ArrayList<IArtifactDescriptor>();
        for ( IArtifactKey key : repository.getArtifactKeys() )
        {
            descriptors.addAll( Arrays.asList( repository.getArtifactDescriptors( key ) ) );
        }

        // ///

        CompositeClassLoader cl = new CompositeClassLoader();
        cl.add( ArtifactDescriptor.class.getClassLoader() );

        XStream xs = new XStream( null, new XppDriver(), cl );
        xs.alias( "artifact", ArtifactDescriptor.class );
        xs.registerConverter( new VersionConverter() );
        xs.registerLocalConverter( ArtifactDescriptor.class, "properties", new PropertiesConverter() );
        xs.registerLocalConverter( ArtifactDescriptor.class, "repositoryProperties", new PropertiesConverter() );

        xs.alias( "key", ArtifactKey.class );
        xs.useAttributeFor( ArtifactKey.class, "id" );
        xs.useAttributeFor( ArtifactKey.class, "classifier" );
        xs.useAttributeFor( ArtifactKey.class, "version" );

        xs.aliasField( "processing", ArtifactDescriptor.class, "processingSteps" );
        xs.alias( "step", ProcessingStepDescriptor.class );
        xs.useAttributeFor( ProcessingStepDescriptor.class, "processorId" );
        xs.aliasAttribute( ProcessingStepDescriptor.class, "processorId", "id" );
        xs.useAttributeFor( ProcessingStepDescriptor.class, "required" );

        OutputStream os = new BufferedOutputStream( new FileOutputStream( "target/artifacts.xml" ) );
        xs.toXML( descriptors, os );
        os.close();

        InputStream is = new BufferedInputStream( new FileInputStream( "target/artifacts.xml" ) );
        descriptors = (ArrayList<IArtifactDescriptor>) xs.fromXML( is );
        is.close();
    }
}
