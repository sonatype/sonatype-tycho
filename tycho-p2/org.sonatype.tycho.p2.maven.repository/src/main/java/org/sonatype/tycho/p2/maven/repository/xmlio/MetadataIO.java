package org.sonatype.tycho.p2.maven.repository.xmlio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryIO;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.sonatype.tycho.p2.maven.repository.Activator;

@SuppressWarnings( "restriction" )
public class MetadataIO
{

    public Set<IInstallableUnit> readXML( InputStream is )
        throws IOException
    {
		try
		{
			URL location = new File( "p2metadata.xml" ).toURI().toURL();
			NullProgressMonitor monitor = new NullProgressMonitor();
			IProvisioningAgent agent = Activator.getProvisioningAgent();
			IMetadataRepository repository = new MetadataRepositoryIO( agent ).read( location, is, monitor );
			
			return repository.query(QueryUtil.ALL_UNITS, monitor).toSet();
		}
		catch (Exception e) 
		{
			throw new IOException("Cannot write metadata", e);
		}
    }

    public void writeXML( Set<IInstallableUnit> units, OutputStream os )
        throws IOException
    {
        // new Writer( os ).write( units );
    	writeXML( new File("p2metadata.xml").toURI(), units, os );
    }

    public void writeXML( Set<IInstallableUnit> units, File file )
        throws IOException
    {
    	file.delete();
    	OutputStream os = new BufferedOutputStream( new FileOutputStream( file ) );
        try
        {
            writeXML( file.getAbsoluteFile().toURI(), units, os );
        }
        finally
        {
            os.close();
        }
    }
    
    private void writeXML( URI location, final Set<IInstallableUnit> units, OutputStream os )
    	throws IOException
	{
	    // new Writer( os ).write( units );
		try
		{
			final IProvisioningAgent agent = Activator.getProvisioningAgent();
			final Repository repository = new Repository( agent, location, "Tycho", null);
			repository.addInstallableUnits(units);
			new MetadataRepositoryIO( agent ).write(repository, os);
		}
		catch (Exception e) 
		{
			throw new IOException("Cannot write metadata", e);
		}
	} 
    
    private class Repository extends LocalMetadataRepository
    {

		public Repository(IProvisioningAgent agent, URI location, String name,
				Map<String, String> properties) {
			super(agent, location, name, properties);
		}
		
		@Override
		public void save() {
			// do nothing
		}		
    	
    }
    
}
