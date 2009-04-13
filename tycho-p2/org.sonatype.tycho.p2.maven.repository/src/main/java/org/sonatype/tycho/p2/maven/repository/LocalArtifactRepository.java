package org.sonatype.tycho.p2.maven.repository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRequest;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.AbstractArtifactRepository;
import org.sonatype.tycho.p2.maven.repository.xstream.PropertiesConverter;
import org.sonatype.tycho.p2.maven.repository.xstream.VersionConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.io.xml.XppDriver;

@SuppressWarnings( "restriction" )
public class LocalArtifactRepository
    extends AbstractArtifactRepository
{

    public static final String VERSION = "1.0.0";

    private static final IArtifactDescriptor[] ARTIFACT_DESCRIPTOR_ARRAY = new IArtifactDescriptor[0];

    private static final IArtifactKey[] ARTIFACT_KEY_ARRAY = new IArtifactKey[0];

    private Map<IArtifactKey, Set<IArtifactDescriptor>> descriptorsMap = new HashMap<IArtifactKey, Set<IArtifactDescriptor>>();

    private Set<IArtifactDescriptor> descriptors = new HashSet<IArtifactDescriptor>();

    public LocalArtifactRepository( File location )
    {
        super(
            "Maven Local Repository",
            LocalArtifactRepository.class.getName(),
            VERSION,
            location.toURI(),
            null,
            null,
            null );

        loadMaven();
    }

    private void loadMaven()
    {
        File location = getBasedir();

        // artifactKey => gav map
        Properties properties = new Properties();

        try
        {
            InputStream is = new BufferedInputStream( new FileInputStream( new File( location, "p2/artifacts.properties" ) ) );
            try
            {
                properties.load( is );
            }
            finally
            {
                is.close();
            }
        }
        catch ( IOException e )
        {
            // lets assume this is a new repository
            return;
        }

        XStream xs = getXStream();

        for ( Map.Entry entry : properties.entrySet() )
        {
            try
            {
                IArtifactKey key = ArtifactKey.parse( (String) entry.getKey() );
                GAV gav = GAV.parse( (String) entry.getValue() );
                String relpath = RepositoryLayoutHelper.getRelativePath( gav, "p2artifacts", "xml" );
                InputStream is = new BufferedInputStream( new FileInputStream( new File( location, relpath ) ) );
                try
                {
                    Set<IArtifactDescriptor> gavDescriptors = (Set<IArtifactDescriptor>) xs.fromXML( is );

                    descriptorsMap.put( key, gavDescriptors );
                    descriptors.addAll( gavDescriptors );
                }
                finally
                {
                    is.close();
                }
            }
            catch ( IOException e )
            {
                // too bad
            }
            catch ( XStreamException e )
            {
                // too bad
            }
        }
    }

    private void saveMaven()
    {
        File location = getBasedir();

        Properties properties = new Properties();

        XStream xs = getXStream();

        for ( Map.Entry<IArtifactKey, Set<IArtifactDescriptor>> keyEntry : descriptorsMap.entrySet() )
        {
            Set<IArtifactDescriptor> keyDescriptors = keyEntry.getValue();
            if ( keyDescriptors != null && !keyDescriptors.isEmpty() )
            {
                IArtifactDescriptor random = keyDescriptors.iterator().next();
                GAV gav = RepositoryLayoutHelper.getGAV( random.getProperties() );

                if ( gav == null )
                {
                    gav = getP2GAV( random );
                }

                String relpath = RepositoryLayoutHelper.getRelativePath( gav, "p2artifacts", "xml" );

                File file = new File( location, relpath );
                file.getParentFile().mkdirs();

                try
                {
                    OutputStream os = new BufferedOutputStream( new FileOutputStream( file ) );
                    try
                    {
                        xs.toXML( keyDescriptors, os );
                    }
                    finally
                    {
                        os.close();
                    }

                    properties.put( keyEntry.getKey().toExternalForm(), gav.toExternalForm() );
                }
                catch ( IOException e )
                {
                    // XXX not good
                }
                catch ( XStreamException e )
                {
                    // XXX not good
                }
            }
        }
        
        try
        {
            OutputStream os = new BufferedOutputStream( new FileOutputStream( new File( location, "p2/artifacts.properties" ) ) );
            try
            {
                properties.store( os, null );
            }
            finally
            {
                os.close();
            }
        }
        catch ( IOException e )
        {
            // XXX not good
        }
        
    }
    
    public void save()
    {
        saveMaven();
    }

    public static XStream getXStream()
    {
        CompositeClassLoader cl = new CompositeClassLoader();
        cl.add( ArtifactDescriptor.class.getClassLoader() );

        XStream xs = new XStream( null, new XppDriver(), cl );
        xs.setMode(XStream.NO_REFERENCES);

        xs.registerConverter( new VersionConverter() );

        xs.alias( "artifact", ArtifactDescriptor.class );
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

        return xs;
    }

    @Override
    public boolean contains( IArtifactDescriptor descriptor )
    {
        return descriptors.contains( descriptor );
    }

    @Override
    public boolean contains( IArtifactKey key )
    {
        return descriptorsMap.containsKey( key );
    }

    @Override
    public IStatus getArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public IArtifactDescriptor[] getArtifactDescriptors( IArtifactKey key )
    {
        Set<IArtifactDescriptor> descriptors = descriptorsMap.get( key );
        if ( descriptors == null )
        {
            return ARTIFACT_DESCRIPTOR_ARRAY;
        }
        return descriptors.toArray( ARTIFACT_DESCRIPTOR_ARRAY );
    }

    @Override
    public IArtifactKey[] getArtifactKeys()
    {
        return descriptorsMap.keySet().toArray( ARTIFACT_KEY_ARRAY );
    }

    @Override
    public IStatus getArtifacts( IArtifactRequest[] requests, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized OutputStream getOutputStream( IArtifactDescriptor descriptor )
        throws ProvisionException
    {
        GAV gav = RepositoryLayoutHelper.getGAV( descriptor.getProperties() );

        if ( gav == null )
        {
            gav = getP2GAV( descriptor );
        }

        // TODO new LocalRepositoryM2("/repo").getWriter().writeArtifacts( ... )
        
        File basedir = getBasedir();
        File file = new File( basedir, RepositoryLayoutHelper.getRelativePath( gav, null, null ) );
        file.getParentFile().mkdirs();
        
        // TODO ideally, repository index should be updated after artifact has been written to the file

        ArtifactDescriptor newDescriptor = new ArtifactDescriptor( descriptor );
        newDescriptor.setRepository( this );
        descriptors.add( newDescriptor );

        IArtifactKey key = newDescriptor.getArtifactKey();
        Set<IArtifactDescriptor> keyDescriptors = descriptorsMap.get( key );
        if ( keyDescriptors == null )
        {
            keyDescriptors = new HashSet<IArtifactDescriptor>();
            descriptorsMap.put( key, keyDescriptors );
        }
        keyDescriptors.add( newDescriptor );

        try
        {
            return new FileOutputStream( file );
        }
        catch ( FileNotFoundException e )
        {
            throw new ProvisionException( "Could not create artifact file", e );
        }
    }

    private GAV getP2GAV( IArtifactDescriptor descriptor )
    {
        GAV gav;
        IArtifactKey key = descriptor.getArtifactKey();
        StringBuffer version = new StringBuffer();
        key.getVersion().toString( version );
        gav = new GAV( "p2", key.getId(), version.toString() );
        return gav;
    }

    public IStatus getRawArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
    }

    public URI getLocation( IArtifactDescriptor descriptor )
    {
        GAV gav = RepositoryLayoutHelper.getGAV( descriptor.getProperties() );

        if ( gav == null )
        {
            gav = getP2GAV( descriptor );
        }

        File basedir = getBasedir();

        return new File( basedir, RepositoryLayoutHelper.getRelativePath( gav, null, null ) ).toURI();
    }

    public File getBasedir()
    {
        return new File( getLocation() );
    }

}
