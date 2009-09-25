package org.sonatype.tycho.p2.maven.repository;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
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
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.facade.internal.GAV;
import org.sonatype.tycho.p2.facade.internal.LocalRepositoryReader;
import org.sonatype.tycho.p2.facade.internal.LocalTychoRepositoryIndex;
import org.sonatype.tycho.p2.facade.internal.RepositoryReader;
import org.sonatype.tycho.p2.facade.internal.TychoRepositoryIndex;
import org.sonatype.tycho.p2.maven.repository.xmlio.ArtifactsIO;

@SuppressWarnings( "restriction" )
public class LocalArtifactRepository
    extends AbstractMavenArtifactRepository
{

    public LocalArtifactRepository( File location )
    {
        this( location, new LocalTychoRepositoryIndex( location ), new LocalRepositoryReader( location ) );
    }

    public LocalArtifactRepository( File location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator )
    {
        super( location.toURI(), projectIndex, contentLocator );
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
    public IStatus getArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
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

    public IStatus getRawArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
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

    public URI getLocation( IArtifactDescriptor descriptor )
    {
        return getLocationFile( descriptor ).toURI();
    }

    private File getLocationFile( IArtifactDescriptor descriptor )
    {
        GAV gav = getGAV( descriptor );

        File basedir = getBasedir();

        String classifier = null;
        String extension = null;

        if ( "packed".equals( descriptor.getProperty( IArtifactDescriptor.FORMAT ) ) )
        {
            classifier = "pack200";
            extension = "jar.pack.gz";
        }

        return new File( basedir, RepositoryLayoutHelper.getRelativePath( gav, classifier, extension ) );
    }

    public GAV getGAV( IArtifactDescriptor descriptor )
    {
        GAV gav = RepositoryLayoutHelper.getGAV( ( (ArtifactDescriptor) descriptor ).getProperties() );

        if ( gav == null )
        {
            gav = getP2GAV( descriptor );
        }

        return gav;
    }

    @Override
    public IStatus resolve( IArtifactDescriptor descriptor )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains( IArtifactDescriptor descriptor )
    {
        return super.contains( descriptor ) && getLocationFile( descriptor ).canRead();
    }
}
