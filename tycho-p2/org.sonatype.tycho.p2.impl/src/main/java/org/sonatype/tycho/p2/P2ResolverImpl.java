package org.sonatype.tycho.p2;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.Projector;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRequest;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.osgi.framework.InvalidSyntaxException;
import org.sonatype.tycho.p2.facade.internal.LocalRepositoryReader;
import org.sonatype.tycho.p2.facade.internal.LocalTychoRepositoryIndex;
import org.sonatype.tycho.p2.facade.internal.P2Logger;
import org.sonatype.tycho.p2.facade.internal.P2RepositoryCache;
import org.sonatype.tycho.p2.facade.internal.P2ResolutionResult;
import org.sonatype.tycho.p2.facade.internal.P2Resolver;
import org.sonatype.tycho.p2.facade.internal.RepositoryReader;
import org.sonatype.tycho.p2.facade.internal.TychoRepositoryIndex;
import org.sonatype.tycho.p2.maven.repository.LocalArtifactRepository;
import org.sonatype.tycho.p2.maven.repository.LocalMetadataRepository;
import org.sonatype.tycho.p2.maven.repository.MavenArtifactRepository;
import org.sonatype.tycho.p2.maven.repository.MavenMetadataRepository;
import org.sonatype.tycho.p2.maven.repository.MavenMirrorRequest;

@SuppressWarnings( "restriction" )
public class P2ResolverImpl
    implements P2Resolver
{

    private static final IInstallableUnit[] IU_ARRAY = new IInstallableUnit[0];

    private static final IArtifactRequest[] ARTIFACT_REQUEST_ARRAY = new IArtifactRequest[0];

    private static final IRequiredCapability[] REQUIRED_CAPABILITY_ARRAY = new IRequiredCapability[0];

    private P2GeneratorImpl generator = new P2GeneratorImpl( true );

    private P2RepositoryCache repositoryCache;

    /**
     * All known P2 metadata repositories, including maven local repository 
     */
    private List<IMetadataRepository> metadataRepositories = new ArrayList<IMetadataRepository>();

    /**
     * All known P2 artifact repositories, NOT including maven local repository.
     */
    private List<IArtifactRepository> artifactRepositories = new ArrayList<IArtifactRepository>();

    /** maven local repository as P2 IArtifactRepository */
    private LocalArtifactRepository localRepository;

    /** maven local repository as P2 IMetadataRepository */
    private LocalMetadataRepository localMetadataRepository;

    private Map<File, Set<IInstallableUnit>> projectIUs = new HashMap<File, Set<IInstallableUnit>>();

    private IProgressMonitor monitor = new NullProgressMonitor();

    private Properties properties;

    private Map<File, String> projects = new LinkedHashMap<File, String>();

    private List<IRequiredCapability> additionalRequirements = new ArrayList<IRequiredCapability>();

    private P2Logger logger;

    public P2ResolverImpl()
    {
    }

    public void addMavenProject( File location, String type, String groupId, String artifactId, String version )
    {
        if ( !generator.isSupported( type ) )
        {
            return;
        }

        addMavenArtifact( location, type, groupId, artifactId, version );
    }

    public void addMavenArtifact( File location, String type, String groupId, String artifactId, String version )
    {
        LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();

        generator.generateMetadata( location, type, groupId, artifactId, version, units, null );

        projectIUs.put( location, units );

        projects.put( location, type );
    }

    public void addP2Repository( URI location )
    {
        IMetadataRepositoryManager metadataRepositoryManager =
            (IMetadataRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IMetadataRepositoryManager.class.getName() );
        if ( metadataRepositoryManager == null )
        {
            throw new IllegalStateException( "No metadata repository manager found" ); //$NON-NLS-1$
        }

        IArtifactRepositoryManager artifactRepositoryManager =
            (IArtifactRepositoryManager) ServiceHelper.getService( Activator.getContext(),
                                                                   IArtifactRepositoryManager.class.getName() );
        if ( artifactRepositoryManager == null )
        {
            throw new IllegalStateException( "No artifact repository manager found" ); //$NON-NLS-1$
        }

        try
        {
            IMetadataRepository metadataRepository = metadataRepositoryManager.loadRepository( location, monitor );
            IArtifactRepository artifactRepository = artifactRepositoryManager.loadRepository( location, monitor );
            metadataRepositories.add( metadataRepository );
            artifactRepositories.add( artifactRepository );

            // processPartialIUs( metadataRepository, artifactRepository );
        }
        catch ( ProvisionException e )
        {
            throw new RuntimeException( e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public P2ResolutionResult resolveProject( File projectLocation )
    {
        P2ResolutionResult result = new P2ResolutionResult();

        Dictionary newSelectionContext = SimplePlanner.createSelectionContext( properties );

        IInstallableUnit[] availableIUs = gatherAvailableInstallableUnits( monitor );

        Set<IInstallableUnit> rootIUs = getProjectIUs( projectLocation );

        Set<IInstallableUnit> extraIUs = createAdditionalRequirementsIU();

        Set<IInstallableUnit> rootWithExtraIUs = new LinkedHashSet<IInstallableUnit>();
        rootWithExtraIUs.addAll( rootIUs );
        rootWithExtraIUs.addAll( extraIUs );

        Slicer slicer = new Slicer( new QueryableArray( availableIUs ), newSelectionContext, false );
        IQueryable slice = slicer.slice( rootWithExtraIUs.toArray( IU_ARRAY ), monitor );

        if ( slice != null )
        {
            Projector projector = new Projector( slice, newSelectionContext, false );
            projector.encode( createMetaIU( rootIUs ),
                              extraIUs.toArray( IU_ARRAY ) /* alreadyExistingRoots */,
                              rootIUs.toArray( IU_ARRAY ) /* newRoots */,
                              monitor );
            IStatus s = projector.invokeSolver( monitor );
            if ( s.getSeverity() == IStatus.ERROR )
            {
                Set<Explanation> explanation = projector.getExplanation( monitor );

                System.out.println( explanation );

                throw new RuntimeException( new ProvisionException( s ) );
            }
            Collection<IInstallableUnit> newState = projector.extractSolution();

            fixSWT( newState, availableIUs, newSelectionContext );

            List<MavenMirrorRequest> requests = new ArrayList<MavenMirrorRequest>();
            for ( IInstallableUnit iu : newState )
            {
                if ( getReactorProjectBasedir( iu ) == null )
                {
                    for ( IArtifactKey key : iu.getArtifacts() )
                    {
                        requests.add( new MavenMirrorRequest( iu, key, localMetadataRepository, localRepository ) );
                    }
                }
            }

            for ( IArtifactRepository artifactRepository : artifactRepositories )
            {
                artifactRepository.getArtifacts( requests.toArray( ARTIFACT_REQUEST_ARRAY ), monitor );

                requests = filterCompletedRequests( requests );
            }

            localRepository.save();
            localMetadataRepository.save();

            // check for locally installed artifacts, which are not available from any remote repo
            for ( Iterator<MavenMirrorRequest> iter = requests.iterator(); iter.hasNext(); )
            {
                MavenMirrorRequest request = iter.next();
                if ( localRepository.contains( request.getArtifactKey() ) )
                {
                    iter.remove();
                }
            }

            if ( !requests.isEmpty() )
            {
                StringBuilder msg = new StringBuilder( "Could not download artifacts from any repository\n" );
                for ( MavenMirrorRequest request : requests )
                {
                    msg.append("   ").append( request.getArtifactKey().toExternalForm() ).append( '\n' );
                }

                throw new RuntimeException( msg.toString() );
            }

            for ( IInstallableUnit iu : newState )
            {
                File basedir = getReactorProjectBasedir( iu );
                if ( basedir != null )
                {
                    addReactorProject( result, basedir );
                }
                else
                {
                    for ( IArtifactKey key : iu.getArtifacts() )
                    {
                        addArtifactFile( result, key );
                    }
                }
            }
        }

        return result;
    }

    private File getReactorProjectBasedir( IInstallableUnit iu )
    {
        for ( Map.Entry<File, Set<IInstallableUnit>> entry : projectIUs.entrySet() )
        {
            if ( entry.getValue().contains( iu ) )
            {
                return entry.getKey();
            }
        }
        return null;
    }

    private void fixSWT( Collection<IInstallableUnit> ius, IInstallableUnit[] availableIUs, Dictionary selectionContext )
    {

        boolean swt = false;
        for ( IInstallableUnit iu : ius )
        {
            if ( "org.eclipse.swt".equals( iu.getId() ) )
            {
                swt = true;
                break;
            }
        }

        if ( !swt )
        {
            return;
        }

        IInstallableUnit swtFragment = null;

        all_ius: for ( IInstallableUnit iu : availableIUs )
        {
            if ( iu.getId().startsWith( "org.eclipse.swt" ) && isApplicable( selectionContext, iu.getFilter() ) )
            {
                for ( IProvidedCapability provided : iu.getProvidedCapabilities() )
                {
                    if ( "osgi.fragment".equals( provided.getNamespace() )
                        && "org.eclipse.swt".equals( provided.getName() ) )
                    {
                        if ( swtFragment == null || swtFragment.getVersion().compareTo( iu.getVersion() ) < 0 )
                        {
                            swtFragment = iu;
                        }
                        continue all_ius;
                    }
                }
            }
        }

        if ( swtFragment == null )
        {
            throw new RuntimeException( "Could not determine SWT implementation fragment bundle" );
        }

        ius.add( swtFragment );
    }

    protected boolean isApplicable( Dictionary selectionContext, String filter )
    {
        if ( filter == null )
        {
            return true;
        }

        try
        {
            return DirectorActivator.context.createFilter( filter ).match( selectionContext );
        }
        catch ( InvalidSyntaxException e )
        {
            return false;
        }
    }

    private LinkedHashSet<IInstallableUnit> getProjectIUs( File location )
    {
        LinkedHashSet<IInstallableUnit> ius = new LinkedHashSet<IInstallableUnit>( projectIUs.get( location ) );

        return ius;
    }

    private Set<IInstallableUnit> createAdditionalRequirementsIU()
    {
        LinkedHashSet<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();

        if ( !additionalRequirements.isEmpty() )
        {
            InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
            String time = Long.toString( System.currentTimeMillis() );
            iud.setId( "extra-" + time );
            iud.setVersion( Version.createOSGi( 0, 0, 0, time ) );
            iud.setRequiredCapabilities( additionalRequirements.toArray( REQUIRED_CAPABILITY_ARRAY ) );

            result.add( MetadataFactory.createInstallableUnit( iud ) );
        }

        return result;
    }

    private void addArtifactFile( P2ResolutionResult platform, IArtifactKey key )
    {
        File file = getLocalArtifactFile( key );
        if ( file == null )
        {
            return;
        }

        if ( PublisherHelper.OSGI_BUNDLE_CLASSIFIER.equals( key.getClassifier() ) )
        {
            platform.addBundle( file );
        }
        else if ( PublisherHelper.ECLIPSE_FEATURE_CLASSIFIER.equals( key.getClassifier() ) )
        {
            platform.addFeature( file );
        }

        // ignore other/unknown artifacts, like binary blobs for now.
        //throw new IllegalArgumentException();
    }

    private void addReactorProject( P2ResolutionResult platform, File basedir )
    {
        String type = projects.get( basedir );

        if ( P2Resolver.TYPE_ECLIPSE_PLUGIN.equals( type ) || P2Resolver.TYPE_ECLIPSE_TEST_PLUGIN.equals( type ) )
        {
            platform.addBundle( basedir );
        }
        else if ( P2Resolver.TYPE_ECLIPSE_FEATURE.equals( type ) )
        {
            platform.addFeature( basedir );
        }
        else if ( basedir.isFile() && basedir.getName().endsWith( ".jar" ) )
        {
            platform.addBundle( basedir );
        }

        // we don't care about eclipse-update-site and eclipse-application projects for now
        //throw new IllegalArgumentException();
    }

    private File getLocalArtifactFile( IArtifactKey key )
    {
        for ( IArtifactDescriptor descriptor : localRepository.getArtifactDescriptors( key ) )
        {
            URI uri = localRepository.getLocation( descriptor );
            if ( uri != null )
            {
                return new File( uri );
            }
        }

        return null;
    }

    private List<MavenMirrorRequest> filterCompletedRequests( List<MavenMirrorRequest> requests )
    {
        ArrayList<MavenMirrorRequest> filteredRequests = new ArrayList<MavenMirrorRequest>();
        for ( MavenMirrorRequest request : requests )
        {
            if ( request.getResult() == null || !request.getResult().isOK() )
            {
                filteredRequests.add( request );
            }
        }
        return filteredRequests;
    }

    @SuppressWarnings( "unchecked" )
    public IInstallableUnit[] gatherAvailableInstallableUnits( IProgressMonitor monitor )
    {
        Set<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();

        for ( Collection<IInstallableUnit> ius : projectIUs.values() )
        {
            result.addAll( ius );
        }
        
        SubMonitor sub = SubMonitor.convert( monitor, metadataRepositories.size() * 200 );
        for ( IMetadataRepository repository : metadataRepositories )
        {
            Collector matches = repository.query( InstallableUnitQuery.ANY, new Collector(), sub.newChild( 100 ) );
            for ( Iterator<IInstallableUnit> it = matches.iterator(); it.hasNext(); )
            {
                IInstallableUnit iu = it.next();

                if ( !PartialInstallableUnitsQuery.isPartialIU( iu ) )
                {
                    result.add( iu );
                }
                else
                {
                    System.out.println( "PARTIAL IU: " + iu );
                }
            }
        }
        sub.done();
        return result.toArray( IU_ARRAY );
    }

    private IInstallableUnit createMetaIU( Set<IInstallableUnit> rootIUs )
    {
        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        String time = Long.toString( System.currentTimeMillis() );
        iud.setId( time );
        iud.setVersion( Version.createOSGi( 0, 0, 0, time ) );

        ArrayList<IRequiredCapability> capabilities = new ArrayList<IRequiredCapability>();
        for ( IInstallableUnit iu : rootIUs )
        {
            VersionRange range = new VersionRange( iu.getVersion(), true, iu.getVersion(), true );
            capabilities.add( MetadataFactory.createRequiredCapability( IInstallableUnit.NAMESPACE_IU_ID,
                                                                        iu.getId(),
                                                                        range,
                                                                        iu.getFilter(),
                                                                        false /* optional */,
                                                                        !iu.isSingleton() /* multiple */,
                                                                        true /* greedy */) );
        }

        capabilities.addAll( additionalRequirements );

        iud.setRequiredCapabilities( (IRequiredCapability[]) capabilities.toArray( new IRequiredCapability[capabilities.size()] ) );
        return MetadataFactory.createInstallableUnit( iud );
    }

    public void setLocalRepositoryLocation( File location )
    {
        URI uri = location.toURI();

        localRepository = (LocalArtifactRepository) repositoryCache.getArtifactRepository( uri );
        localMetadataRepository = (LocalMetadataRepository) repositoryCache.getMetadataRepository( uri );

        if ( localRepository == null || localMetadataRepository == null )
        {
            TychoRepositoryIndex projectIndex = new LocalTychoRepositoryIndex( location );
            RepositoryReader contentLocator = new LocalRepositoryReader( location );
    
            localRepository = new LocalArtifactRepository( location, projectIndex, contentLocator );
            localMetadataRepository = new LocalMetadataRepository( uri, projectIndex, contentLocator );
            
            repositoryCache.putRepository( uri, localMetadataRepository, localRepository );
        }

        // XXX remove old
        metadataRepositories.add( localMetadataRepository );
    }

    public void setProperties( Properties properties )
    {
        this.properties = new Properties();
        this.properties.putAll( properties );
    }

    public void addDependency( String type, String id, String version )
    {
        if ( P2Resolver.TYPE_INSTALLABLE_UNIT.equals( type ) )
        {
            additionalRequirements.add( MetadataFactory.createRequiredCapability( IInstallableUnit.NAMESPACE_IU_ID,
                                                                                  id,
                                                                                  new VersionRange( version ),
                                                                                  null,
                                                                                  false,
                                                                                  true ) );
        }
        else if ( P2Resolver.TYPE_ECLIPSE_PLUGIN.equals( type ) )
        {
            // BundlesAction#CAPABILITY_NS_OSGI_BUNDLE
            additionalRequirements.add( MetadataFactory.createRequiredCapability( "osgi.bundle",
                                                                                  id,
                                                                                  new VersionRange( version ),
                                                                                  null,
                                                                                  false,
                                                                                  true ) );
        }
    }

    public void addMavenRepository( URI location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator )
    {
        MavenMetadataRepository metadataRepository = (MavenMetadataRepository) repositoryCache.getMetadataRepository( location );
        MavenArtifactRepository artifactRepository = (MavenArtifactRepository) repositoryCache.getArtifactRepository( location );
        
        if ( metadataRepository == null || artifactRepository == null )
        {
            metadataRepository = new MavenMetadataRepository( location, projectIndex, contentLocator );
            artifactRepository = new MavenArtifactRepository( location, projectIndex, contentLocator );
            
            repositoryCache.putRepository( location, metadataRepository, artifactRepository );
        }
        
        metadataRepositories.add( metadataRepository );
        artifactRepositories.add( artifactRepository );
    }

    public void setLogger( P2Logger logger )
    {
        this.logger = logger;
        this.monitor = new LoggingProgressMonitor( logger );
    }

    public void setRepositoryCache( P2RepositoryCache repositoryCache )
    {
        this.repositoryCache = repositoryCache;
    }

    // creating copy&paste from org.eclipse.equinox.internal.p2.repository.Credentials.forLocation(URI, boolean,
    // AuthenticationInfo)
    public void setCredentials( URI location, String username, String password )
    {
        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();

        // if URI is not opaque, just getting the host may be enough
        String host = location.getHost();
        if ( host == null )
        {
            String scheme = location.getScheme();
            if ( URIUtil.isFileURI( location ) || scheme == null )
            {
                // If the URI references a file, a password could possibly be needed for the directory
                // (it could be a protected zip file representing a compressed directory) - in this
                // case the key is the path without the last segment.
                // Using "Path" this way may result in an empty string - which later will result in
                // an invalid key.
                host = new Path( location.toString() ).removeLastSegments( 1 ).toString();
            }
            else
            {
                // it is an opaque URI - details are unknown - can only use entire string.
                host = location.toString();
            }
        }
        String nodeKey;
        try
        {
            nodeKey = URLEncoder.encode( host, "UTF-8" ); //$NON-NLS-1$
        }
        catch ( UnsupportedEncodingException e2 )
        {
            // fall back to default platform encoding
            try
            {
                // Uses getProperty "file.encoding" instead of using deprecated URLEncoder.encode(String location)
                // which does the same, but throws NPE on missing property.
                String enc = System.getProperty( "file.encoding" );//$NON-NLS-1$
                if ( enc == null )
                {
                    throw new UnsupportedEncodingException(
                                                            "No UTF-8 encoding and missing system property: file.encoding" ); //$NON-NLS-1$
                }
                nodeKey = URLEncoder.encode( host, enc );
            }
            catch ( UnsupportedEncodingException e )
            {
                throw new RuntimeException( e );
            }
        }
        String nodeName = IRepository.PREFERENCE_NODE + '/' + nodeKey;

        ISecurePreferences prefNode = securePreferences.node( nodeName );

        try
        {
            if ( !username.equals( prefNode.get( IRepository.PROP_USERNAME, username ) )
                || !password.equals( prefNode.get( IRepository.PROP_PASSWORD, password ) ) )
            {
                logger.info( "Redefining access credentials for repository host " + host );
            }
            prefNode.put( IRepository.PROP_USERNAME, username, false );
            prefNode.put( IRepository.PROP_PASSWORD, password, false );
        }
        catch ( StorageException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    @SuppressWarnings( "unchecked" )
    private void dumpInstallableUnits( IQueryable source )
    {
        Collector collector = source.query( InstallableUnitQuery.ANY, new Collector(), monitor );
        dumpInstallableUnits( collector.toCollection() );
    }

    private void dumpInstallableUnits( Collection<IInstallableUnit> ius )
    {
        for ( IInstallableUnit iu : ius )
        {
            System.out.println( iu.toString() );
        }
    }

    private void dumpInstallableUnits( IInstallableUnit[] ius )
    {
        for ( IInstallableUnit iu : ius )
        {
            System.out.println( iu.toString() );
        }
    }
    
}
