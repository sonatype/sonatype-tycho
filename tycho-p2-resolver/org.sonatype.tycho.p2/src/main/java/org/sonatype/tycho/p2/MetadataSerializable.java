package org.sonatype.tycho.p2;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * Represents a p2 metadata repository. Facade only exposes serialized form (content.xml).
 * 
 * @TODO better class name
 */
public interface MetadataSerializable
{

    /**
     * Writes the given set of installable units to the given output stream in standard p2 metadata
     * repository format. The caller is responsible for closing the stream.
     */
    void serialize( OutputStream stream, Set<?> installableUnits )
        throws IOException;
}
