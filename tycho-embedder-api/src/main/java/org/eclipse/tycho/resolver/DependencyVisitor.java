package org.eclipse.tycho.resolver;

import org.eclipse.tycho.ArtifactDescriptor;

public interface DependencyVisitor
{
    /**
     * @TODO internally, we distinguish between bundles, features, etc
     */
    public boolean visit( ArtifactDescriptor artifact );
}
