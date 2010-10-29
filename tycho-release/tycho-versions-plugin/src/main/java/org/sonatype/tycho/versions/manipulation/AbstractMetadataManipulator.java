package org.sonatype.tycho.versions.manipulation;

import java.util.Set;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.versions.engine.VersionChange;
import org.sonatype.tycho.versions.engine.MetadataManipulator;
import org.sonatype.tycho.versions.engine.ProjectMetadata;
import org.sonatype.tycho.versions.pom.MutablePomFile;

public abstract class AbstractMetadataManipulator
    implements MetadataManipulator
{

    @Requirement
    protected Logger logger;

    protected boolean isBundle( ProjectMetadata project )
    {
        MutablePomFile pom = project.getMetadata( MutablePomFile.class );
        return isBundle( pom );
    }

    protected boolean isBundle( MutablePomFile pom )
    {
        String packaging = pom.getPackaging();
        return ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals( packaging ) || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals( packaging );
    }

    protected boolean isFeature( ProjectMetadata project )
    {
        String packaging = project.getMetadata( MutablePomFile.class ).getPackaging();
        return isFeature( packaging );
    }

    protected boolean isFeature( String packaging )
    {
        return ArtifactKey.TYPE_ECLIPSE_FEATURE.equals( packaging );
    }

    public boolean addMoreChanges( ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges )
    {
        return false;
    }
}
