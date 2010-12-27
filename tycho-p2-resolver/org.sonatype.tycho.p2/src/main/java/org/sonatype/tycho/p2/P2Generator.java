package org.sonatype.tycho.p2;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface P2Generator
{
    /**
     * @param artifacts
     * @param attachedArtifacts The passed data maps classifier to artifacts. It is intended both for adding additional
     *            artifacts during meta data generation and using this map after meta data generation to attach the
     *            contained artifacts.
     * @param targetDir location to store artifacts created during meta data generation (e.g. root file zip)
     * @throws IOException
     */
    public void generateMetadata( List<IArtifactFacade> artifacts, Map<String, IArtifactFacade> attachedArtifacts,
                                  File targetDir )
        throws IOException;
}
