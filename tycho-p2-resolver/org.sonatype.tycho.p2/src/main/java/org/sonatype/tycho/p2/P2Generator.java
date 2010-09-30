package org.sonatype.tycho.p2;

import java.io.File;
import java.io.IOException;


public interface P2Generator
{
    public void generateMetadata( IArtifactFacade artifact, File p2Content, File p2Artifacts )
        throws IOException;
}
