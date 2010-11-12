package org.sonatype.tycho.p2.impl.publisher;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.sonatype.tycho.p2.DependencyMetadataGenerator;
import org.sonatype.tycho.p2.IArtifactFacade;

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

        super.generateMetadata( artifact, environments, units, artifactDescriptors );

        return new LinkedHashSet<Object>( units );
    }

}
