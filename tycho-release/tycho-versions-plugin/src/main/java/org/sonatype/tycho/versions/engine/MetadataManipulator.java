package org.sonatype.tycho.versions.engine;

import java.io.IOException;
import java.util.Set;

public interface MetadataManipulator
{
    public void applyChange( ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges );

    public boolean addMoreChanges( ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges );

    public void writeMetadata( ProjectMetadata project ) throws IOException;
}
