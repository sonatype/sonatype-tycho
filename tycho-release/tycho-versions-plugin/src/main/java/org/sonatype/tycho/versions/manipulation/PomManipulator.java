package org.sonatype.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.tycho.versions.engine.MetadataManipulator;
import org.sonatype.tycho.versions.engine.ProjectMetadata;
import org.sonatype.tycho.versions.engine.VersionChange;
import org.sonatype.tycho.versions.engine.VersionsEngine;
import org.sonatype.tycho.versions.pom.GAV;
import org.sonatype.tycho.versions.pom.DependencyManagement;
import org.sonatype.tycho.versions.pom.MutablePomFile;

@Component( role = MetadataManipulator.class, hint = "pom" )
public class PomManipulator
    extends AbstractMetadataManipulator
{
    @Override
    public boolean addMoreChanges( ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges )
    {
        MutablePomFile pom = project.getMetadata( MutablePomFile.class );
        GAV parent = pom.getParent();
        if ( parent != null && isGavEquals( parent, change )
            && !isVersionEquals( change.getNewVersion(), parent.getVersion() ) )
        {
            String explicitVersion = pom.getVersion();
            if ( explicitVersion == null )
            {
                return allChanges.add( new VersionChange( pom, change.getVersion(), change.getNewVersion() ) );
            }
        }

        return false;
    }

    public void applyChange( ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges )
    {
        MutablePomFile pom = project.getMetadata( MutablePomFile.class );
        GAV parent = pom.getParent();

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
                logger.info( "  pom.xml//project/parent/version: " + version + " => " + newVersion );
                parent.setVersion( newVersion );
            }
        }

        //
        // Dependencies and entries inside dependencyManagement sections are not
        // OSGI related. Nevertheless it might happen that dependencies like this
        // does occur inside OSGI related project. Hence we must be able to handle
        // it.
        //
        for ( GAV dependency : pom.getDependencies() )
        {
            if ( isGavEquals( dependency, change ) )
            {
                logger.info( "  pom.xml//project/dependencies/dependency/[ " + dependency.getGroupId() + ":"
                    + dependency.getArtifactId() + " ] " + version + " => " + newVersion );
                dependency.setVersion( newVersion );
            }
        }

        DependencyManagement dependencyManagment = pom.getDependencyManagement();

        if ( dependencyManagment != null )
        {
            for ( GAV dependency : dependencyManagment.getDependencies() )
            {
                if ( isGavEquals( dependency, change ) )
                {
                    logger.info( "  pom.xml//project/dependencyManagement/dependencies/dependency/[ "
                        + dependency.getGroupId() + ":" + dependency.getArtifactId() + " ] " + version + " => "
                        + newVersion );
                    dependency.setVersion( newVersion );
                }
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

    private boolean isGavEquals( GAV dependency, VersionChange change )
    {
        return change.getGroupId().equals( dependency.getGroupId() )
            && change.getArtifactId().equals( dependency.getArtifactId() )
            && isVersionEquals( change.getVersion(), dependency.getVersion() );
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
