package org.sonatype.tycho.resolver;

import org.sonatype.tycho.ArtifactDescriptor;

public interface DependencyVisitor
{
    /**
     * @TODO internally, we distinguish between bundles, features, etc
     */
    public boolean visit( ArtifactDescriptor artifact );
}
