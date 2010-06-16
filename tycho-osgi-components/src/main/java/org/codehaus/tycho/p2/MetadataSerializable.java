package org.codehaus.tycho.p2;

import java.io.IOException;
import java.io.OutputStream;

/**
 *  Represents a p2 metadata repository. Facade only exposes serialized form (content.xml). 
 */
public interface MetadataSerializable
{

    /**
     * Serialize the content of this repository into the output stream. The caller is responsible for closing the stream.
     * 
     * @param stream
     * @throws IOException
     */
    void serialize(OutputStream stream) throws IOException;
    
}
