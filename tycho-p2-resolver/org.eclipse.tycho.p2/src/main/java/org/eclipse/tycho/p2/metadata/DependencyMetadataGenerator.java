package org.eclipse.tycho.p2.metadata;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DependencyMetadataGenerator
{
    /**
     * Generates dependency-only artifact metadata
     */
    public Set<Object/* IInstallableUnit */> generateMetadata( IArtifactFacade artifact,
                                                               List<Map<String, String>> environments );
}
