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
     * Serialises the content of this repository into the output stream. If qualifier is not null, replaces the build
     * qualifier with the given value. The caller is responsible for closing the stream.
     */
    void serialize( OutputStream stream, Set<Object> installableUnits, String qualifier )
        throws IOException;
}
