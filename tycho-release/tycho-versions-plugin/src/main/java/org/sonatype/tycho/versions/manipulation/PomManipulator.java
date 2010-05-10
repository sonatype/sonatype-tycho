package org.sonatype.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.tycho.versions.engine.MetadataManipulator;
import org.sonatype.tycho.versions.engine.ProjectMetadata;
import org.sonatype.tycho.versions.engine.VersionChange;
import org.sonatype.tycho.versions.engine.VersionsEngine;
import org.sonatype.tycho.versions.pom.MutablePomFile;
import org.sonatype.tycho.versions.pom.Parent;

@Component( role = MetadataManipulator.class, hint = "pom" )
public class PomManipulator
    extends AbstractMetadataManipulator
{
    @Override
    public boolean addMoreChanges( ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges )
    {
        MutablePomFile pom = project.getMetadata( MutablePomFile.class );
        Parent parent = pom.getParent();
        if ( parent != null && isGavEquals( parent, change ) && !isVersionEquals( change.getNewVersion(), parent.getVersion() ) )
        {
            String explicitVersion = pom.getVersion();
            if ( explicitVersion == null || isVersionEquals( explicitVersion, change.getVersion() ) )
            {
                return allChanges.add( new VersionChange( pom, change.getVersion(), change.getNewVersion() ) );
            }
        }

        return false;
    }

    public void applyChange( ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges )
    {
        MutablePomFile pom = project.getMetadata( MutablePomFile.class );
        Parent parent = pom.getParent();

        String version = VersionsEngine.toMavenVersion( change.getVersion() );
        String newVersion = VersionsEngine.toMavenVersion( change.getNewVersion() );
        if ( isGavEquals( pom, change ) )
        {
            logger.info( "  pom.xml//project/version: " + version + " => " + newVersion );
            pom.setVersion( newVersion );
        }
        else
        {
            if ( parent != null && isGavEquals( parent, change ) )
            {
                logger.info( "  pom.xml//project/parent/version: " + version + " => "
                    + newVersion );
                parent.setVersion( newVersion );
            }
        }

        // TODO update other references
    }

    private boolean isGavEquals( MutablePomFile pom, VersionChange change )
    {
        return change.getGroupId().equals( pom.getEffectiveGroupId() )
            && change.getArtifactId().equals( pom.getArtifactId() )
            && isVersionEquals( change.getVersion(), pom.getVersion() );
    }

    private boolean isGavEquals( Parent parent, VersionChange change )
    {
        return change.getGroupId().equals( parent.getGroupId() )
            && change.getArtifactId().equals( parent.getArtifactId() )
            && isVersionEquals( change.getVersion(), parent.getVersion() );
    }

    public void writeMetadata( ProjectMetadata project )
        throws IOException
    {
        MutablePomFile pom = project.getMetadata( MutablePomFile.class );
        if ( pom != null )
        {
            MutablePomFile.write( pom, new File( project.getBasedir(), "pom.xml" ) );
        }
    }

    private boolean isVersionEquals( String a, String b )
    {
        return VersionsEngine.toCanonicalVersion( a ).equals( VersionsEngine.toCanonicalVersion( b ) );
    }

}
