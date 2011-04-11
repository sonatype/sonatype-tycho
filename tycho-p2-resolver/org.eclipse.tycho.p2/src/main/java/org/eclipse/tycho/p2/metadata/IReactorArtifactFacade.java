package org.eclipse.tycho.p2.metadata;

import java.util.Set;

public interface IReactorArtifactFacade
    extends IArtifactFacade
{
    /**
     * Conventional sources jar bundle symbolic name suffix.
     */
    public static final String SOURCE_BUNDLE_SUFFIX = ".source";

    public Set<Object/*IInstallableUnit*/> getDependencyMetadata();
}
