package org.sonatype.tycho.p2.tools.impl.publisher;

import java.io.File;
import java.util.Collection;

import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.repository.ICompositeRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.sonatype.tycho.p2.tools.FacadeException;
import org.sonatype.tycho.p2.tools.impl.Activator;
import org.sonatype.tycho.p2.tools.publisher.BuildContext;
import org.sonatype.tycho.p2.tools.publisher.PublisherService;
import org.sonatype.tycho.p2.tools.publisher.PublisherServiceFactory;

@SuppressWarnings( "restriction" )
public class PublisherServiceFactoryImpl
    implements PublisherServiceFactory
{

    public PublisherService createPublisher( File targetRepository, Collection<File> contextMetadataRepositories,
                                             Collection<File> contextArtifactRepositories, BuildContext context,
                                             int flags )
        throws FacadeException
    {
        IProvisioningAgent agent = null;
        try
        {
            // create an own instance of the provisioning agent to prevent cross talk with other things
            // that happen in the Tycho OSGi runtime
            final File agentConfigurationFolder = new File( context.getTargetDirectory(), "p2agent" );
            agent = Activator.createProvisioningAgent( agentConfigurationFolder.toURI() );

            final PublisherInfo publisherInfo = new PublisherInfo();
            publisherInfo.setArtifactOptions( IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH );

            final boolean compress = ( flags & REPOSITORY_COMPRESS ) != 0;
            final boolean reusePackedFiles = false; // TODO check if we can/should use this
            final String repositoryName = "eclipse-repository"; // TODO proper name for repo, e.g. GAV? (pending in TYCHO-513)
            final IArtifactRepository targetArtifactRepo =
                Publisher.createArtifactRepository( agent, targetRepository.toURI(), repositoryName, compress,
                                                    reusePackedFiles );
            publisherInfo.setArtifactRepository( targetArtifactRepo );

            final boolean append = true;
            publisherInfo.setMetadataRepository( Publisher.createMetadataRepository( agent, targetRepository.toURI(),
                                                                                     repositoryName, append, compress ) );

            // the ProductAction needs to know for which configurations it needs to generate configIUs
            publisherInfo.setConfigurations( context.getConfigurations() );

            // set context repositories
            if ( contextMetadataRepositories != null && contextMetadataRepositories.size() > 0 )
            {
                final CompositeMetadataRepository contextMetadata =
                    CompositeMetadataRepository.createMemoryComposite( agent );
                addToComposite( contextMetadataRepositories, contextMetadata );
                publisherInfo.setContextMetadataRepository( contextMetadata );
            }
            if ( contextArtifactRepositories != null && contextArtifactRepositories.size() > 0 )
            {
                final CompositeArtifactRepository contextArtifact =
                    CompositeArtifactRepository.createMemoryComposite( agent );
                addToComposite( contextArtifactRepositories, contextArtifact );
                publisherInfo.setContextArtifactRepository( contextArtifact );
            }

            return new PublisherServiceImpl( context, publisherInfo, agent );
        }
        catch ( ProvisionException e )
        {
            if ( agent != null )
            {
                agent.stop();
            }
            throw new FacadeException( e );
        }
    }

    private void addToComposite( Collection<File> repositoryLocations, ICompositeRepository<?> compositeRepository )
    {
        for ( File repositoryLocation : repositoryLocations )
        {
            compositeRepository.addChild( repositoryLocation.toURI() );
        }
    }
}
