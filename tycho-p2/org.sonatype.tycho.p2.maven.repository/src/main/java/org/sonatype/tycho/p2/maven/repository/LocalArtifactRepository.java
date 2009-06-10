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
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRequest;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.AbstractArtifactRepository;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.facade.internal.GAV;
import org.sonatype.tycho.p2.facade.internal.LocalTychoRepositoryIndex;
import org.sonatype.tycho.p2.maven.repository.xmlio.ArtifactsIO;

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

        LocalTychoRepositoryIndex index = new LocalTychoRepositoryIndex( location );

        ArtifactsIO io = new ArtifactsIO();

        for ( GAV gav : index.getProjectGAVs() )
        {
            try
            {
                String relpath = getMetadataRelpath( gav );

                InputStream is = new BufferedInputStream( new FileInputStream( new File( location, relpath ) ) );
                try
                {
                    Set<IArtifactDescriptor> gavDescriptors = (Set<IArtifactDescriptor>) io.readXML( is );

                    if ( !gavDescriptors.isEmpty() )
                    {
                        IArtifactKey key = gavDescriptors.iterator().next().getArtifactKey();
                        descriptorsMap.put( key, gavDescriptors );
                        descriptors.addAll( gavDescriptors );
                    }
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
        }
    }

    private void saveMaven()
    {
        File location = getBasedir();

        LocalTychoRepositoryIndex index = new LocalTychoRepositoryIndex( location );

        Properties properties = new Properties();

        ArtifactsIO io = new ArtifactsIO();

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

                index.addProject( gav );

                String relpath = getMetadataRelpath( gav );

                File file = new File( location, relpath );
                file.getParentFile().mkdirs();

                try
                {
                    OutputStream os = new BufferedOutputStream( new FileOutputStream( file ) );
                    try
                    {
                        io.writeXML( keyDescriptors, os );
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
            }
        }

        try
        {
            index.save();
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private String getMetadataRelpath( GAV gav )
    {
        String relpath = RepositoryLayoutHelper.getRelativePath(
            gav,
            RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS,
            RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS );
        return relpath;
    }

    public void save()
    {
        saveMaven();
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
        GAV gav = RepositoryLayoutHelper.getGAV( ( (ArtifactDescriptor) descriptor ).getProperties() );

        if ( gav == null )
        {
            gav = getP2GAV( descriptor );
        }

        File basedir = getBasedir();

        String classifier = null;
        String extension = null;

        if ( "packed".equals( descriptor.getProperty( IArtifactDescriptor.FORMAT ) ) )
        {
            classifier = "pack200";
            extension = "jar.pack.gz";
        }

        return new File( basedir, RepositoryLayoutHelper.getRelativePath( gav, classifier, extension ) ).toURI();
    }

    public File getBasedir()
    {
        return new File( getLocation() );
    }

    @Override
    public boolean isModifiable()
    {
        return true;
    }
}
