package org.sonatype.tycho.p2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryIO;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryIO;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataWriter;
import org.eclipse.equinox.internal.p2.updatesite.metadata.UpdateSiteMetadataRepositoryFactory;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.ITouchpointType;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.BundleShapeAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.publisher.eclipse.IBundleShapeAdvice;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.sonatype.tycho.p2.facade.ItemMetadata;
import org.sonatype.tycho.p2.facade.P2Facade;
import org.sonatype.tycho.p2.facade.P2ResolutionRequest;
import org.sonatype.tycho.p2.facade.P2ResolutionResultCollector;
import org.sonatype.tycho.p2.facade.RepositoryContentLocator;

@SuppressWarnings( { "restriction", "unchecked" } )
public class P2Impl
    implements P2Facade
{

    private static final IInstallableUnit[] IU_ARRAY = new IInstallableUnit[0];

    private static class InstallableUnitRenderer
        extends MetadataWriter
    {
        public InstallableUnitRenderer( OutputStream output, ProcessingInstruction[] piElements )
            throws UnsupportedEncodingException
        {
            super( output, piElements );
        }

        public void writeInstallableUnits( IInstallableUnit[] ius )
        {
            super.writeInstallableUnits( Arrays.asList( ius ).iterator(), ius.length );
        }
    }

    private static class ArtifactRenderer
        extends SimpleArtifactRepositoryIO
    {
        private class RelaxedWriter
            extends Writer
        {
            public RelaxedWriter( OutputStream output )
                throws IOException
            {
                super( output );
            }

            public void writeArtifacts( IArtifactDescriptor[] artifactDescriptors )
            {
                start( ARTIFACTS_ELEMENT );
                attribute( COLLECTION_SIZE_ATTRIBUTE, artifactDescriptors.length );
                for ( IArtifactDescriptor descriptor : artifactDescriptors )
                {
                    IArtifactKey key = descriptor.getArtifactKey();
                    start( ARTIFACT_ELEMENT );
                    attribute( ARTIFACT_CLASSIFIER_ATTRIBUTE, key.getClassifier() );
                    attribute( ID_ATTRIBUTE, key.getId() );
                    attribute( VERSION_ATTRIBUTE, key.getVersion() );
                    writeProcessingSteps( descriptor.getProcessingSteps() );
                    writeProperties( descriptor.getProperties() );
                    writeProperties( REPOSITORY_PROPERTIES_ELEMENT, ( (ArtifactDescriptor) descriptor )
                        .getRepositoryProperties() );
                    end( ARTIFACT_ELEMENT );
                }
                end( ARTIFACTS_ELEMENT );
            }

            private void writeProcessingSteps( ProcessingStepDescriptor[] processingSteps )
            {
                if ( processingSteps.length > 0 )
                {
                    start( PROCESSING_STEPS_ELEMENT );
                    attribute( COLLECTION_SIZE_ATTRIBUTE, processingSteps.length );
                    for ( int i = 0; i < processingSteps.length; i++ )
                    {
                        start( PROCESSING_STEP_ELEMENT );
                        attribute( ID_ATTRIBUTE, processingSteps[i].getProcessorId() );
                        attribute( STEP_DATA_ATTRIBUTE, processingSteps[i].getData() );
                        attribute( STEP_REQUIRED_ATTRIBUTE, processingSteps[i].isRequired() );
                        end( PROCESSING_STEP_ELEMENT );
                    }
                    end( PROCESSING_STEPS_ELEMENT );
                }
            }

        }

        private RelaxedWriter writer;

        public ArtifactRenderer( OutputStream output )
            throws IOException
        {
            writer = new RelaxedWriter( output );
        }

        public void writeArtifact( IArtifactDescriptor artifactDescriptor )
        {
            writer.writeArtifacts( new IArtifactDescriptor[] { artifactDescriptor } );

            writer.flush();
        }
    }

    private SimpleArtifactRepositoryIO artifactIO = new SimpleArtifactRepositoryIO();

    private MetadataRepositoryIO metadataIO = new MetadataRepositoryIO();

    private static class RelaxedFeatureAction
        extends FeaturesAction
    {
        public RelaxedFeatureAction()
        {
            super( new File[0] );
        }

        @Override
        public Feature[] getFeatures( File[] locations )
        {
            return super.getFeatures( locations );
        }

        @Override
        public IInstallableUnit createGroupIU( Feature feature, List childIUs, IPublisherInfo info )
        {
            // TODO Auto-generated method stub
            return super.createGroupIU( feature, childIUs, info );
        }
    }

    private static final String TMP_PROFILE_NAME = "UselessProfile";

    public ItemMetadata getBundleMetadata( File bundleLocation )
    {
        BundleDescription bd = BundlesAction.createBundleDescription( bundleLocation );
        IPublisherInfo info = new PublisherInfo();
        info.addAdvice( new BundleShapeAdvice(
            bd.getSymbolicName(),
            Version.fromOSGiVersion( bd.getVersion() ),
            IBundleShapeAdvice.JAR ) );
        IArtifactKey key = BundlesAction.createBundleArtifactKey( bd.getSymbolicName(), bd.getVersion().toString() );
        IInstallableUnit iu = BundlesAction.createBundleIU( bd, key, info );
        IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor( key, bundleLocation );

        return newItemMetadata( key, new IInstallableUnit[] { iu }, ad );
    }

    private ItemMetadata newItemMetadata( IArtifactKey key, IInstallableUnit[] ius, IArtifactDescriptor ad )
    {
        byte[] iuXml = renderIUs( ius );
        byte[] artifactXml = renderArtifact( ad );
        return new ItemMetadata( key.getId(), key.getVersion().toString(), iuXml, artifactXml );
    }

    private byte[] renderArtifact( IArtifactDescriptor ad )
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            ArtifactRenderer renderer = new ArtifactRenderer( out );
            renderer.writeArtifact( ad );

            out.flush();

            return out.toByteArray();
        }
        catch ( IOException e )
        {
            // ah?
        }

        return null;
    }

    public ItemMetadata getFeatureMetadata( File file )
    {
        RelaxedFeatureAction featureAction = new RelaxedFeatureAction();

        Feature[] features = featureAction.getFeatures( new File[] { file } );
        IPublisherInfo info = new PublisherInfo();
        IArtifactKey key = FeaturesAction.createFeatureArtifactKey( features[0].getId(), features[0].getVersion() );
        IInstallableUnit jarIu = FeaturesAction.createFeatureJarIU( features[0], info );
        IInstallableUnit groupIu = featureAction.createGroupIU( features[0], Collections.singletonList( jarIu ), info );
        IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor( key, file );

        return newItemMetadata( key, new IInstallableUnit[] { jarIu, groupIu }, ad );
    }

    private byte[] renderIUs( IInstallableUnit[] ius )
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            InstallableUnitRenderer renderer = new InstallableUnitRenderer( out, null );
            renderer.writeInstallableUnits( ius );
            renderer.flush();

            out.flush();

            return out.toByteArray();
        }
        catch ( IOException e )
        {
            // ah?
        }

        return null;
    }

    public void publish( File location, List<File> bundles, List<File> features )
        throws Exception
    {
        PublisherInfo info = new PublisherInfo();

        info.setArtifactRepository( Publisher.createArtifactRepository(
            location.toURI(),
            location.getName(),
            true /* append */,
            false /* compress */,
            true /* reusePackedFiles */) );

        info.setMetadataRepository( Publisher.createMetadataRepository(
            location.toURI(),
            location.getName(),
            true /* append */,
            false /* compress */) );

        ArrayList<IPublisherAction> actions = new ArrayList<IPublisherAction>();
        actions.add( new FeaturesAction( features.toArray( new File[features.size()] ) ) );
        actions.add( new BundlesAction( bundles.toArray( new File[bundles.size()] ) ) );

        new Publisher( info ).publish(
            (IPublisherAction[]) actions.toArray( new IPublisherAction[actions.size()] ),
            new NullProgressMonitor() );
    }

    public void resolve( P2ResolutionRequest req, P2ResolutionResultCollector result )
        throws Exception
    {
        IPlanner planner = (IPlanner) ServiceHelper.getService( Activator.getContext(), IPlanner.class.getName() );
        if ( planner == null )
        {
            throw new ProvisionException( "No planner service found." );
        }

        IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(
            Activator.getContext(),
            IProfileRegistry.class.getName() );
        if ( profileRegistry == null )
        {
            throw new ProvisionException( "No profile registry found." );
        }

        IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService( Activator
            .getContext(), IMetadataRepositoryManager.class.getName() );
        if ( manager == null )
        {
            throw new IllegalStateException( "No metadata repository manager found" ); //$NON-NLS-1$
        }

        IMetadataRepository metadataRepository = createMetadataRepository( manager, req );
        List<SimpleArtifactRepository> artifactRepositories = createArtifactRepositories( req );

        InstallableUnitsQuery query = new InstallableUnitsQuery( req.getRootInstallableUnits() );
        Collector rootCollector = metadataRepository.query( query, new Collector(), null );
        Collection<IInstallableUnit> rootIUs = rootCollector.toCollection();

        Collector iuCollector = new Collector();
        for ( String env : req.getTargetEnvironments() )
        {
            Map properties = new HashMap();
            properties.put( IProfile.PROP_ENVIRONMENTS, env );
            properties.put( IProfile.PROP_INSTALL_FEATURES, "true" );
            profileRegistry.removeProfile( TMP_PROFILE_NAME );
            IProfile profile = profileRegistry.addProfile( TMP_PROFILE_NAME, properties, null );
            ProfileChangeRequest preq = new ProfileChangeRequest( profile );
            preq.addInstallableUnits( rootIUs.toArray( new IInstallableUnit[rootIUs.size()] ) );
            ProvisioningPlan plan = planner.getProvisioningPlan( preq, null, null );
            plan.getAdditions().query( InstallableUnitQuery.ANY, iuCollector, null );
            profileRegistry.removeProfile( profile.getProfileId() );
        }

        ArrayList<IInstallableUnit> ius = new ArrayList<IInstallableUnit>( iuCollector.toCollection() );
        IInstallableUnit groupIU = createGroupIU( req, rootIUs );
        ius.add( groupIU );
        IInstallableUnit categoryIU = createCategoryIU( req, groupIU );
        ius.add( categoryIU );

        IMetadataRepository targetMetadata = new LocalMetadataRepository( new URI( "file:xxx" ), "xxx", null );
        targetMetadata.setProperty( IRepository.PROP_COMPRESSED, "true" );
        targetMetadata.setProperty( IRepository.PROP_TIMESTAMP, Long.toString( System.currentTimeMillis() ) );

        SimpleArtifactRepository targetArtifacts = new SimpleArtifactRepository( "xxx", new URI( "file:xxx" ), null );
        targetArtifacts.setProperty( IRepository.PROP_COMPRESSED, "true" );
        targetArtifacts.setProperty( Publisher.PUBLISH_PACK_FILES_AS_SIBLINGS, "true" );
        targetArtifacts.setProperty( IRepository.PROP_TIMESTAMP, Long.toString( System.currentTimeMillis() ) );

        targetMetadata.addInstallableUnits( ius.toArray( IU_ARRAY ) );

        for ( IInstallableUnit iu : ius )
        {
            if ( "true".equalsIgnoreCase( iu.getProperty( IInstallableUnit.PROP_TYPE_GROUP ) ) )
            {
                continue;
            }
            boolean found = false;
            for ( IArtifactKey akey : iu.getArtifacts() )
            {
                for ( IArtifactRepository artifactRepository : artifactRepositories )
                {
                    IArtifactDescriptor[] artifactDescriptors = artifactRepository.getArtifactDescriptors( akey );
                    if ( artifactDescriptors != null && artifactDescriptors.length > 0 )
                    {
                        found = true;
                        for ( IArtifactDescriptor adesc : artifactDescriptors )
                        {
                            targetArtifacts.addDescriptor( adesc );
                            String artifactPath = ( (SimpleArtifactRepository) artifactRepository )
                                .getLocation( adesc ).getSchemeSpecificPart();
                            String targetRepositoryId = (String) artifactRepository.getProperties().get(
                                PROP_REPOSITORY_ID );
                            result.createLinkItem( artifactPath, targetRepositoryId, artifactPath );
                        }
                    }
                }
            }
            if ( !found )
            {
                // throw new RuntimeException( "iu " + iu.getId() + "_" + iu.getVersion()
                // + " is not found in any artifact repository" );
            }
        }

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        metadataIO.write( targetMetadata, buf );
        result.setItemContent( "/content.xml", new ByteArrayInputStream( buf.toByteArray() ), "text/plain" );

        buf.reset();
        artifactIO.write( targetArtifacts, buf );
        result.setItemContent( "/artifacts.xml", new ByteArrayInputStream( buf.toByteArray() ), "text/plain" );
    }

    private boolean isPacked( IArtifactDescriptor adesc )
    {
        return "packed".equals( adesc.getProperty( IArtifactDescriptor.FORMAT ) );
    }

    private IInstallableUnit createCategoryIU( P2ResolutionRequest req, IInstallableUnit member )
    {
        InstallableUnitDescription cat = new MetadataFactory.InstallableUnitDescription();
        cat.setSingleton( true );

        cat.setId( req.getId() + ".category.group" );
        cat.setVersion( Version.emptyVersion );
        cat.setProperty( IInstallableUnit.PROP_NAME, req.getName() != null ? req.getName() : req.getId() );
        cat.setProperty( IInstallableUnit.PROP_DESCRIPTION, req.getName() != null ? req.getName() : req.getId() );

        ArrayList reqsConfigurationUnits = new ArrayList();
        VersionRange memberRange = new VersionRange( member.getVersion(), true, member.getVersion(), true );
        reqsConfigurationUnits.add( MetadataFactory.createRequiredCapability( IInstallableUnit.NAMESPACE_IU_ID, member
            .getId(), memberRange, member.getFilter(), false, false ) );
        cat.setRequiredCapabilities( (IRequiredCapability[]) reqsConfigurationUnits
            .toArray( new IRequiredCapability[reqsConfigurationUnits.size()] ) );

        ArrayList providedCapabilities = new ArrayList();
        providedCapabilities.add( PublisherHelper.createSelfCapability( cat.getId(), Version.emptyVersion ) );
        cat.setCapabilities( (IProvidedCapability[]) providedCapabilities
            .toArray( new IProvidedCapability[providedCapabilities.size()] ) );

        cat.setArtifacts( new IArtifactKey[0] );
        cat.setProperty( IInstallableUnit.PROP_TYPE_CATEGORY, "true" ); //$NON-NLS-1$

        return MetadataFactory.createInstallableUnit( cat );
    }

    private IInstallableUnit createGroupIU( P2ResolutionRequest req, Collection<IInstallableUnit> memberIUs )
    {
        InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
        String id = req.getId() + ".feature.group";
        iu.setId( id );

        Version version = Version.parseVersion( req.getVersion() );
        iu.setVersion( version );

        iu.setProperty( IInstallableUnit.PROP_NAME, req.getName() != null ? req.getName() : req.getId() );

        ArrayList<IRequiredCapability> required = new ArrayList<IRequiredCapability>();
        for ( IInstallableUnit member : memberIUs )
        {
            VersionRange range = new VersionRange( member.getVersion(), true, member.getVersion(), true );
            String requiredId = member.getId();
            required.add( MetadataFactory.createRequiredCapability(
                IInstallableUnit.NAMESPACE_IU_ID,
                requiredId,
                range,
                null /* filter */,
                false /* optional */,
                false /* multiple */) );
        }
        iu
            .setRequiredCapabilities( (IRequiredCapability[]) required
                .toArray( new IRequiredCapability[required.size()] ) );

        iu.setTouchpointType( ITouchpointType.NONE );

        iu.setProperty( IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString() );

        ArrayList<IProvidedCapability> providedCapabilities = new ArrayList<IProvidedCapability>();
        providedCapabilities
            .add( MetadataFactory.createProvidedCapability( PublisherHelper.IU_NAMESPACE, id, version ) );
        iu.setCapabilities( (IProvidedCapability[]) providedCapabilities
            .toArray( new IProvidedCapability[providedCapabilities.size()] ) );

        return MetadataFactory.createInstallableUnit( iu );
    }

    private List<SimpleArtifactRepository> createArtifactRepositories( P2ResolutionRequest req )
    {
        ArrayList<SimpleArtifactRepository> repositories = new ArrayList<SimpleArtifactRepository>();

        for ( RepositoryContentLocator repo : req.getRepositories() )
        {
            try
            {
                URI uri = new URI( "nexus:/" );
                SimpleArtifactRepository repository = (SimpleArtifactRepository) artifactIO.read( null, repo
                    .getItemInputStream( "/artifacts.xml" ), new NullProgressMonitor() );
                repository.initializeAfterLoad( uri );
                repositories.add( repository );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
        }

        return repositories;
    }

    public IMetadataRepository createMetadataRepository( IMetadataRepositoryManager manager, P2ResolutionRequest req )
        throws ProvisionException,
            URISyntaxException
    {
        long timestamp = System.currentTimeMillis() * 100;
        URI location;
        do
        {
            location = new URI( "temporary:" + timestamp++ );
        }
        while ( manager.contains( location ) );

        TransientMetadataRepository composite = (TransientMetadataRepository) manager.createRepository( location, Long
            .toString( timestamp ), TransientMetadataRepository.class.getName(), null );

        for ( RepositoryContentLocator repo : req.getRepositories() )
        {
            try
            {
                composite.add( metadataIO.read(
                    null,
                    repo.getItemInputStream( "/content.xml" ),
                    new NullProgressMonitor() ) );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
        }

        return composite;
    }

}
