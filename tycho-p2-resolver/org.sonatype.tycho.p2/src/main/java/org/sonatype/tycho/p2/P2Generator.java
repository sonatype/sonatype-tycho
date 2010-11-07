package org.sonatype.tycho.p2;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface P2Generator
{
    public void generateMetadata( List<IArtifactFacade> artifacts, File p2Content, File p2Artifacts )
        throws IOException;
}
