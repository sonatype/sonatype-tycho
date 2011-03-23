package org.sonatype.tycho.p2.impl.publisher;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.sonatype.tycho.p2.DependencyMetadataGenerator;
import org.sonatype.tycho.p2.IArtifactFacade;
import org.sonatype.tycho.p2.impl.publisher.repo.TransientArtifactRepository;

@SuppressWarnings( "restriction" )
public class DefaultDependencyMetadataGenerator
    extends P2GeneratorImpl
    implements DependencyMetadataGenerator
{

    public DefaultDependencyMetadataGenerator()
    {
        super( true );
    }

    public Set<Object> generateMetadata( IArtifactFacade artifact, List<Map<String, String>> environments )
    {
        LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
        LinkedHashSet<IArtifactDescriptor> artifactDescriptors = new LinkedHashSet<IArtifactDescriptor>();

        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setArtifactOptions( IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH );
        publisherInfo.setArtifactRepository( new TransientArtifactRepository() );

        super.generateMetadata( artifact, environments, units, artifactDescriptors, publisherInfo );

        return new LinkedHashSet<Object>( units );
    }

}
