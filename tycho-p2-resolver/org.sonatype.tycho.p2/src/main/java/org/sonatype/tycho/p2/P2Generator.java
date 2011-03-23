package org.sonatype.tycho.p2;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface P2Generator
{
    /**
     * @param artifacts
     * @param artifactsToBeAttached The passed data maps maven artifact classifier to artifacts. It
     *            is intended for adding additional artifacts during meta-data generation. Artifacts
     *            in this map will be attached with given classifier to the maven project for which
     *            meta-data is generated.
     * @param targetDir location to store artifacts created during meta data generation (e.g. root
     *            file zip)
     * @throws IOException
     */
    public void generateMetadata( List<IArtifactFacade> artifacts, Map<String, IArtifactFacade> artifactsToBeAttached,
                                  File targetDir )
        throws IOException;
}
