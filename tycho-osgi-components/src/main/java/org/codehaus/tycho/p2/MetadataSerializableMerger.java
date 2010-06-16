package org.codehaus.tycho.p2;

import java.util.Collection;

public interface MetadataSerializableMerger<T extends MetadataSerializable>
{

    /**
     * Merges the contents of the given repositories.
     * 
     * @param repositories 
     * @return A new {@link MetadataSerializable} with the merged contents of the repositories.
     */
    MetadataSerializable merge(Collection<T> repositories);
}
