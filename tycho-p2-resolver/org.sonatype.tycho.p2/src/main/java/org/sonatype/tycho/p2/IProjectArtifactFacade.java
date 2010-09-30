package org.sonatype.tycho.p2;

import java.io.File;

public interface IProjectArtifactFacade
    extends IArtifactFacade
{
    public File getSourceArtifactLocation();
}
