package org.sonatype.tycho.p2.maven.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.sonatype.tycho.p2.facade.GAV;
import org.sonatype.tycho.p2.facade.LocalRepositoryIndex;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.maven.repository.xstream.MetadataIO;

import com.thoughtworks.xstream.XStreamException;

@SuppressWarnings( "restriction" )
public class LocalMetadataRepository
    extends AbstractMetadataRepository
{

    private static final String REPOSITORY_TYPE = LocalMetadataRepository.class.getName();

    private static final String REPOSITORY_VERSION = "1.0.0";

    private Set<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();

    private Map<GAV, Set<IInstallableUnit>> unitsMap = new HashMap<GAV, Set<IInstallableUnit>>();

    /**
     * Create new repository
     */
    public LocalMetadataRepository( URI location, String name, Map properties )
    {
        super( location.toString(), REPOSITORY_TYPE, REPOSITORY_VERSION, location, null, null, properties );
        if ( !location.getScheme().equals( "file" ) )
        {
            throw new IllegalArgumentException( "Invalid local repository location: " + location ); //$NON-NLS-1$
        }

        // when creating a repository, we must ensure it exists on disk so a subsequent load will succeed
        save();
    }

    @Override
    public void addInstallableUnits( IInstallableUnit[] newUnits )
    {
        for ( IInstallableUnit unit : newUnits )
        {
            GAV gav = RepositoryLayoutHelper.getGAV( unit.getProperties() );

            this.units.add( unit );

            Set<IInstallableUnit> gavUnits = unitsMap.get( gav );
            if ( gavUnits == null )
            {
                gavUnits = new HashSet<IInstallableUnit>();
                unitsMap.put( gav, gavUnits );
            }
            gavUnits.add( unit );
        }

        save();
    }

    private void save()
    {
        File basedir = new File( getLocation() );

        // XXX lock
        LocalRepositoryIndex index = new LocalRepositoryIndex( basedir );

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
            catch ( XStreamException e )
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

    private void load()
    {
        File basedir = new File( getLocation() );

        LocalRepositoryIndex index = new LocalRepositoryIndex( basedir );

        MetadataIO io = new MetadataIO();

        for ( GAV gav : index.getProjectGAVs() )
        {
            String relpath = RepositoryLayoutHelper.getRelativePath(
                gav,
                RepositoryLayoutHelper.CLASSIFIER_P2_METADATA,
                RepositoryLayoutHelper.EXTENSION_P2_METADATA );
            try
            {
                InputStream is = new BufferedInputStream( new FileInputStream( new File( basedir, relpath ) ) );
                try
                {
                    Set<IInstallableUnit> gavUnits = io.readXML( is );

                    unitsMap.put( gav, gavUnits );
                    units.addAll( gavUnits );
                }
                finally
                {
                    is.close();
                }
            }
            catch ( IOException e )
            {
                // too bad
            }
            catch ( XStreamException e )
            {
                // too bad
            }

        }
    }

    /**
     * Local existing repository
     */
    public LocalMetadataRepository( URI location )
    {
        super( location.toString(), REPOSITORY_TYPE, REPOSITORY_VERSION, location, null, null, null );

        load();
    }

    @Override
    public void initialize( RepositoryState state )
    {
    }

    public Collector query( Query query, Collector collector, IProgressMonitor monitor )
    {
        return query.perform( units.iterator(), collector );
    }

    @Override
    public boolean isModifiable()
    {
        return true;
    }

}
