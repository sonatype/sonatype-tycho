package org.sonatype.tycho.p2.facade.internal;

import java.io.IOException;
import java.io.InputStream;


public interface RepositoryReader
{

    InputStream getContents( GAV gav, String classifier, String extension )
        throws IOException;

}
