package org.sonatype.tycho.p2.maven.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.facade.internal.GAV;
import org.sonatype.tycho.p2.facade.internal.LocalTychoRepositoryIndex;
import org.sonatype.tycho.p2.facade.internal.RepositoryReader;
import org.sonatype.tycho.p2.facade.internal.TychoRepositoryIndex;
import org.sonatype.tycho.p2.maven.repository.xmlio.MetadataIO;

public class LocalMetadataRepository
    extends AbstractMavenMetadataRepository
{

    private Set<GAV> changedGAVs = new LinkedHashSet<GAV>();

    /**
     * Create new repository
     */
    public LocalMetadataRepository( URI location, String name )
    {
        super( location, null, null );
        if ( !location.getScheme().equals( "file" ) )
        {
            throw new IllegalArgumentException( "Invalid local repository location: " + location ); //$NON-NLS-1$
        }

        // when creating a repository, we must ensure it exists on disk so a subsequent load will succeed
        save();
    }

    /**
     * Local existing repository
     */
    public LocalMetadataRepository( URI location, TychoRepositoryIndex projectIndex,
                                    RepositoryReader contentLocator )
    {
        super( location, projectIndex, contentLocator );
    }

    @Override
    public void addInstallableUnits( Collection<IInstallableUnit> newUnits )
    {
        for ( IInstallableUnit unit : newUnits )
        {
            GAV gav = RepositoryLayoutHelper.getGAV( unit.getProperties() );

            addInstallableUnit( unit, gav );
        }

        save();
    }

    public void addInstallableUnit( IInstallableUnit unit, GAV gav )
    {
        this.units.add( unit );

        Set<IInstallableUnit> gavUnits = unitsMap.get( gav );
        if ( gavUnits == null )
        {
            gavUnits = new LinkedHashSet<IInstallableUnit>();
            unitsMap.put( gav, gavUnits );
        }
        gavUnits.add( unit );

        changedGAVs.add( gav );
    }

    public void save()
    {
        File basedir = new File( getLocation() );

        // XXX lock
        LocalTychoRepositoryIndex index = new LocalTychoRepositoryIndex( basedir, LocalTychoRepositoryIndex.METADATA_INDEX_RELPATH );

        MetadataIO io = new MetadataIO();

        for ( GAV gav : changedGAVs )
        {
            Set<IInstallableUnit> gavUnits = unitsMap.get( gav );

            if ( gavUnits != null && !gavUnits.isEmpty() )
            {
                String relpath =
                    RepositoryLayoutHelper.getRelativePath( gav, RepositoryLayoutHelper.CLASSIFIER_P2_METADATA,
                                                            RepositoryLayoutHelper.EXTENSION_P2_METADATA );

                File file = new File( basedir, relpath );
                file.getParentFile().mkdirs();

                try
                {
                    io.writeXML( gavUnits, file );

                    index.addProject( gav );
                }
                catch ( IOException e )
                {
                    throw new RuntimeException( e );
                }
            }
        }

        try
        {
            index.save();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

        changedGAVs.clear();
    }

    @Override
    public boolean isModifiable()
    {
        return true;
    }

}
