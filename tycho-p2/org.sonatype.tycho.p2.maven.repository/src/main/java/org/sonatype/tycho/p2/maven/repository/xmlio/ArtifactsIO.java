package org.sonatype.tycho.p2.maven.repository.xmlio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryIO;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.ArtifactDescriptorQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.sonatype.tycho.p2.maven.repository.Activator;

@SuppressWarnings( "restriction" )
public class ArtifactsIO
{

    public Set<IArtifactDescriptor> readXML( InputStream is )
        throws IOException
    {
		try
		{
			final URL location = new File( "p2artifacts.xml" ).toURI().toURL();
			final NullProgressMonitor monitor = new NullProgressMonitor();
			final IProvisioningAgent agent = Activator.getProvisioningAgent();
			final IArtifactRepository repository = new SimpleArtifactRepositoryIO( agent ).read( location, is, monitor );
			
			return repository.descriptorQueryable().query(ArtifactDescriptorQuery.ALL_DESCRIPTORS, monitor).toSet();
		}
		catch (Exception e) 
		{
			throw new IOException("Cannot write metadata", e);
		}
    }

    public void writeXML( Set<IArtifactDescriptor> descriptors, OutputStream os )
        throws IOException
    {
        writeXML(new File("p2artifacts.xml").toURI(), descriptors, os);
    }

    public void writeXML( LinkedHashSet<IArtifactDescriptor> descriptors, File file )
        throws IOException
    {
        file.delete();
    	OutputStream os = new BufferedOutputStream( new FileOutputStream( file ) );
        try
        {
            writeXML( file.getAbsoluteFile().toURI(), descriptors, os );
        }
        finally
        {
            os.close();
        }
    }
    
    public void writeXML( URI location, final Set<IArtifactDescriptor> descriptors, OutputStream os )
    	throws IOException
    {
    	try {
			final IProvisioningAgent agent = Activator.getProvisioningAgent();
			final Repository repository = new Repository( agent, "Tycho", location, null );
			repository.addDescriptors( descriptors.toArray( new IArtifactDescriptor[descriptors.size()] ) );
			new SimpleArtifactRepositoryIO( agent ).write( (SimpleArtifactRepository)repository, os );
		} 
    	catch (Exception e) 
    	{
			throw new IOException("Could not write artifacts", e);
		}
    }    
    
    private class Repository extends SimpleArtifactRepository
    {

		public Repository(IProvisioningAgent agent, String repositoryName,
				URI location, Map<String, String> properties) {
			super(agent, repositoryName, location, properties);
		}

		@Override
		public void save() {
			// do nothing
		}
    	
    }

}
