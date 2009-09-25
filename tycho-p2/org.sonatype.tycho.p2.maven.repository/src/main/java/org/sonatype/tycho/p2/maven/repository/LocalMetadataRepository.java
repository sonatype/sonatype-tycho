package org.sonatype.tycho.p2.maven.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.facade.internal.GAV;
import org.sonatype.tycho.p2.facade.internal.LocalTychoRepositoryIndex;
import org.sonatype.tycho.p2.facade.internal.RepositoryReader;
import org.sonatype.tycho.p2.facade.internal.TychoRepositoryIndex;
import org.sonatype.tycho.p2.maven.repository.xmlio.MetadataIO;

@SuppressWarnings( "restriction" )
public class LocalMetadataRepository
    extends AbstractMavenMetadataRepository
{

    /**
     * Create new repository
     */
    public LocalMetadataRepository( URI location, String name, Map properties )
    {
        super( location, properties, null, null );
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
    public LocalMetadataRepository( URI location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator )
    {
        super( location, null, projectIndex, contentLocator );
    }

    @Override
    public void addInstallableUnits( IInstallableUnit[] newUnits )
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
            gavUnits = new HashSet<IInstallableUnit>();
            unitsMap.put( gav, gavUnits );
        }
        gavUnits.add( unit );
    }

    public void save()
    {
        File basedir = new File( getLocation() );

        // XXX lock
        LocalTychoRepositoryIndex index = new LocalTychoRepositoryIndex( basedir );

        MetadataIO io = new MetadataIO();

        for ( Map.Entry<GAV, Set<IInstallableUnit>> gavEntry : unitsMap.entrySet() )
        {
            GAV gav = gavEntry.getKey();

            String relpath = RepositoryLayoutHelper.getRelativePath(
                gav,
                RepositoryLayoutHelper.CLASSIFIER_P2_METADATA,
                RepositoryLayoutHelper.EXTENSION_P2_METADATA );

            File file = new File( basedir, relpath );
            file.getParentFile().mkdirs();

            try
            {
                io.writeXML( units, file );

                index.addProject( gav );
            }
            catch ( IOException e )
            {
                // XXX not good
            }
        }

        try
        {
            index.save();
        }
        catch ( IOException e )
        {
            // XXX not good
        }
    }

    @Override
    public boolean isModifiable()
    {
        return true;
    }

}
