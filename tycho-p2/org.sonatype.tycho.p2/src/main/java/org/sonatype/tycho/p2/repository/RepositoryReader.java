package org.sonatype.tycho.p2.repository;

import java.io.IOException;
import java.io.InputStream;


public interface RepositoryReader
{

    InputStream getContents( GAV gav, String classifier, String extension )
        throws IOException;

    InputStream getContents( String remoteRelpath )
        throws IOException;
}
