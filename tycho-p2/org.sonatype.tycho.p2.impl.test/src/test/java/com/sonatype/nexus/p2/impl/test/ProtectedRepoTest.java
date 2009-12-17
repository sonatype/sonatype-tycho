package com.sonatype.nexus.p2.impl.test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryIO;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.repository.RepositoryStatusHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.junit.Test;
import org.sonatype.tycho.p2.Activator;

@SuppressWarnings( "restriction" )
public class ProtectedRepoTest
{
    private IProgressMonitor monitor = new ConsoleProgressMonitor();

    private URI location;

    public ProtectedRepoTest()
        throws Exception
    {
        location = new URI( "http://repository.sonatype.org/content/sites/com-sites/adobe/flexbuilder3/" );
    }

    //@Test
    public void testArtifactRepository()
        throws Exception
    {
        IArtifactRepositoryManager artifactRepositoryManager =
            (IArtifactRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IArtifactRepositoryManager.class.getName() );

        IArtifactRepository artifactRepository = artifactRepositoryManager.loadRepository( location, monitor );
        SimpleArtifactRepository simple =
            (SimpleArtifactRepository) artifactRepository.getAdapter( SimpleArtifactRepository.class );
        new SimpleArtifactRepositoryIO().write( simple, System.out );

        System.out.println( simple );
    }

    @Test
    public void testMetadataRepository()
        throws Exception
    {
        IMetadataRepositoryManager repositoryManager =
            (IMetadataRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IMetadataRepositoryManager.class.getName() );

        setCredentials( location, "ifedorenko", "1x82hdu7" );

        IMetadataRepository repository = repositoryManager.loadRepository( location, monitor );

        File localFile = new File( "/tmp/xxx" );
        IMetadataRepository localRepository =
            repositoryManager.createRepository( localFile.toURI(), localFile.getName(),
                                                IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null );
        repositoryManager.removeRepository( localFile.toURI() );

        Collector collector = repository.query( InstallableUnitQuery.ANY, new Collector(), monitor );

        localRepository.addInstallableUnits( (IInstallableUnit[]) collector.toArray( IInstallableUnit.class ) );
    }

    public void setCredentials( URI location, String username, String password ) throws CoreException
    {
        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();

        // if URI is not opaque, just getting the host may be enough
        String host = location.getHost();
        if (host == null) {
            String scheme = location.getScheme();
            if (URIUtil.isFileURI(location) || scheme == null)
                // If the URI references a file, a password could possibly be needed for the directory
                // (it could be a protected zip file representing a compressed directory) - in this
                // case the key is the path without the last segment.
                // Using "Path" this way may result in an empty string - which later will result in
                // an invalid key.
                host = new Path(location.toString()).removeLastSegments(1).toString();
            else
                // it is an opaque URI - details are unknown - can only use entire string.
                host = location.toString();
        }
        String nodeKey;
        try {
            nodeKey = URLEncoder.encode(host, "UTF-8"); //$NON-NLS-1$
        } catch (UnsupportedEncodingException e2) {
            // fall back to default platform encoding
            try {
                // Uses getProperty "file.encoding" instead of using deprecated URLEncoder.encode(String location) 
                // which does the same, but throws NPE on missing property.
                String enc = System.getProperty("file.encoding");//$NON-NLS-1$
                if (enc == null)
                    throw new UnsupportedEncodingException("No UTF-8 encoding and missing system property: file.encoding"); //$NON-NLS-1$
                nodeKey = URLEncoder.encode(host, enc);
            } catch (UnsupportedEncodingException e) {
                throw RepositoryStatusHelper.internalError(e);
            }
        }
        String nodeName = IRepository.PREFERENCE_NODE + '/' + nodeKey;

        ISecurePreferences prefNode = securePreferences.node(nodeName);

        try
        {
            prefNode.put(IRepository.PROP_USERNAME, username, true );
            prefNode.put(IRepository.PROP_PASSWORD, password, true );
        }
        catch ( StorageException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID, "Could not store repository access credentials", e ) );
        }
    }
}
