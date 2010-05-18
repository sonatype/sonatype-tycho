package org.sonatype.tycho.p2.facade;

import java.io.File;
import java.io.IOException;

import org.sonatype.tycho.p2.facade.internal.IArtifactFacade;

public interface P2Generator
{
    public void generateMetadata( IArtifactFacade artifact, File p2Content, File p2Artifacts )
        throws IOException;
}
