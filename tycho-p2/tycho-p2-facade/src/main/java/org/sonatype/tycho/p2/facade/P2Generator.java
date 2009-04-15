package org.sonatype.tycho.p2.facade;

import java.io.File;
import java.io.IOException;

public interface P2Generator
{
    public void generateMetadata( File location, String packaging, String groupId, String artifactId, String version,
        File content, File artifacts )
        throws IOException;
}
