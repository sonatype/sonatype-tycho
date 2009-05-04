package org.sonatype.tycho.p2;

import java.io.File;
import java.net.URI;
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
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
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
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.osgi.framework.InvalidSyntaxException;
import org.sonatype.tycho.p2.facade.internal.P2ResolutionResult;
import org.sonatype.tycho.p2.facade.internal.P2Resolver;
import org.sonatype.tycho.p2.maven.repository.LocalArtifactRepository;
import org.sonatype.tycho.p2.maven.repository.LocalMetadataRepository;

@SuppressWarnings( "restriction" )
public class P2ResolverImpl
    implements P2Resolver
{

    private static final IInstallableUnit[] IU_ARRAY = new IInstallableUnit[0];

    private static final IArtifactRequest[] ARTIFACT_REQUEST_ARRAY = new IArtifactRequest[0];

    private static final IRequiredCapability[] REQUIRED_CAPABILITY_ARRAY = new IRequiredCapability[0];

    private P2GeneratorImpl generator = new P2GeneratorImpl( true );

    private List<IMetadataRepository> metadataRepositories = new ArrayList<IMetadataRepository>();

    private List<IArtifactRepository> artifactRepositories = new ArrayList<IArtifactRepository>();

    /** maven local repository */
    private LocalArtifactRepository localRepository;

    private Map<File, Set<IInstallableUnit>> projectIUs = new HashMap<File, Set<IInstallableUnit>>();

    private final NullProgressMonitor monitor = new NullProgressMonitor();

    private Properties properties;

    private Map<File, String> projects = new LinkedHashMap<File, String>();

    private List<IRequiredCapability> additionalRequirements = new ArrayList<IRequiredCapability>();

    public P2ResolverImpl()
    {
    }

    public void addMavenProject( File location, String type, String groupId, String artifactId, String version )
    {
        if ( !generator.isSupported( type ) )
        {
            return;
        }

        LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();

        generator.generateMetadata( location, type, groupId, artifactId, version, units, null );

        projectIUs.put( location, units );

        projects.put( location, type );
    }

    public void addRepository( URI location )
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

    public P2ResolutionResult resolve( File projectLocation )
    {
        P2ResolutionResult result = new P2ResolutionResult();

        Dictionary newSelectionContext = SimplePlanner.createSelectionContext( properties );

        IInstallableUnit[] availableIUs = gatherAvailableInstallableUnits( metadataRepositories, monitor );

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
            projector.encode( createMetaIU( rootIUs ), extraIUs.toArray( IU_ARRAY ) /* alreadyExistingRoots */,
                              rootIUs.toArray( IU_ARRAY ) /* newRoots */, monitor );
            IStatus s = projector.invokeSolver( monitor );
            if ( s.getSeverity() == IStatus.ERROR )
            {
                Set<Explanation> explanation = projector.getExplanation( monitor );

                System.out.println( explanation );

                throw new RuntimeException( new ProvisionException( s ) );
            }
            Collection<IInstallableUnit> newState = projector.extractSolution();

            fixSWT( newState, availableIUs, newSelectionContext );

            List<IArtifactRequest> requests = new ArrayList<IArtifactRequest>();
            for ( IInstallableUnit iu : newState )
            {
                keys: for ( IArtifactKey key : iu.getArtifacts() )
                {
                    if ( !localRepository.contains( key ) )
                    {
                        for ( IArtifactRepository artifactRepository : artifactRepositories )
                        {
                            if ( artifactRepository.contains( key ) )
                            {
                                requests.add( new MirrorRequest( key, localRepository, null, null ) );
                                continue keys;
                            }
                        }
                    }
                }
            }

            for ( IArtifactRepository artifactRepository : artifactRepositories )
            {
                artifactRepository.getArtifacts( requests.toArray( ARTIFACT_REQUEST_ARRAY ), monitor );

                requests = filterCompletedRequests( requests );
            }

            localRepository.save();

            if ( !requests.isEmpty() )
            {
                throw new RuntimeException( "Could not download artifacts from any repository" );
            }

            for ( IInstallableUnit iu : newState )
            {
                for ( IArtifactKey key : iu.getArtifacts() )
                {
                    addArtifactFile( result, key );
                }
            }
        }

        return result;
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

    private boolean isSubdir( File parent, File child )
    {
        return child.getAbsolutePath().startsWith( parent.getAbsolutePath() );
    }

    private LinkedHashSet<IInstallableUnit> getProjectIUs( File location )
    {
        LinkedHashSet<IInstallableUnit> ius = new LinkedHashSet<IInstallableUnit>( projectIUs.get( location ) );

        // XXX this code does not belong here. Move it to test mojo!!!
        // if ( ECLIPSE_TEST_PLUGIN.equals( projects.get( location ) ) )
        // {
        // LatestIUCollector testIUs = new LatestIUCollector();
        // InstallableUnitsQuery testIUsQuery = new InstallableUnitsQuery( IMPLICIT_TEST_IUS );
        //
        // for ( IMetadataRepository repository : metadataRepositories )
        // {
        // repository.query( testIUsQuery, testIUs, monitor );
        // }
        //
        // ius.addAll( testIUs.toCollection() );
        // }

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
            iud.setVersion( new Version( 0, 0, 0, time ) );
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

    private List<IArtifactRequest> filterCompletedRequests( List<IArtifactRequest> requests )
    {
        ArrayList<IArtifactRequest> filteredRequests = new ArrayList<IArtifactRequest>();
        for ( IArtifactRequest request : requests )
        {
            if ( request.getResult() == null || !request.getResult().isOK() )
            {
                filteredRequests.add( request );
            }
        }
        return filteredRequests;
    }

    @SuppressWarnings( "unchecked" )
    public IInstallableUnit[] gatherAvailableInstallableUnits( List<IMetadataRepository> repositories,
                                                               IProgressMonitor monitor )
    {
        Set<IInstallableUnit> result = new LinkedHashSet<IInstallableUnit>();

        for ( Collection<IInstallableUnit> ius : projectIUs.values() )
        {
            result.addAll( ius );
        }

        SubMonitor sub = SubMonitor.convert( monitor, repositories.size() * 200 );
        for ( IMetadataRepository repository : repositories )
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
                    System.out.println( "PARTIAL: " + iu );
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
        iud.setVersion( new Version( 0, 0, 0, time ) );

        ArrayList<IRequiredCapability> capabilities = new ArrayList<IRequiredCapability>();
        for ( IInstallableUnit iu : rootIUs )
        {
            VersionRange range = new VersionRange( iu.getVersion(), true, iu.getVersion(), true );
            capabilities.add( MetadataFactory.createRequiredCapability( IInstallableUnit.NAMESPACE_IU_ID, iu.getId(),
                                                                        range, iu.getFilter(), false /* optional */,
                                                                        !iu.isSingleton() /* multiple */, true /* greedy */) );
        }

        capabilities.addAll( additionalRequirements );

        iud.setRequiredCapabilities( (IRequiredCapability[]) capabilities.toArray( new IRequiredCapability[capabilities.size()] ) );
        return MetadataFactory.createInstallableUnit( iud );
    }

    public void setLocalRepositoryLocation( File location )
    {
        localRepository = new LocalArtifactRepository( location );

        // XXX remove old
        metadataRepositories.add( new LocalMetadataRepository( location.toURI() ) );
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
            additionalRequirements.add( MetadataFactory.createRequiredCapability( IInstallableUnit.NAMESPACE_IU_ID, id,
                                                                                  new VersionRange( version ), null,
                                                                                  false, true ) );
        }
        else if ( P2Resolver.TYPE_OSGI_BUNDLE.equals( type ) )
        {
            additionalRequirements.add( MetadataFactory.createRequiredCapability( IInstallableUnit.NAMESPACE_IU_ID, id,
                                                                                  new VersionRange( version ), null,
                                                                                  false, true ) );
        }
    }
}
