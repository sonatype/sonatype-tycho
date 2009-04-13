package org.sonatype.tycho.p2.facade;

import java.io.IOException;
import java.io.InputStream;

public interface RepositoryContentLocator
{
    InputStream getItemInputStream( String path ) throws IOException;

    String getId();
}
