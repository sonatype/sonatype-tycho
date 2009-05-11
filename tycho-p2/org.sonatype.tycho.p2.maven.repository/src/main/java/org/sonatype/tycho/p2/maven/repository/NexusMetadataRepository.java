package org.sonatype.tycho.p2.maven.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.facade.internal.GAV;
import org.sonatype.tycho.p2.facade.internal.TychoRepositoryIndex;
import org.sonatype.tycho.p2.facade.internal.RepositoryReader;
import org.sonatype.tycho.p2.maven.repository.xstream.MetadataIO;

import com.thoughtworks.xstream.XStreamException;

@SuppressWarnings( "restriction" )
public class NexusMetadataRepository
    extends AbstractMetadataRepository
{

    private final TychoRepositoryIndex projectIndex;

    private final RepositoryReader contentLocator;

    private final Set<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();

    public NexusMetadataRepository( TychoRepositoryIndex projectIndex, RepositoryReader contentLocator )
    {
        this.projectIndex = projectIndex;
        this.contentLocator = contentLocator;

        load();
    }

    private void load()
    {
        List<GAV> gavs = projectIndex.getProjectGAVs();

        MetadataIO io = new MetadataIO();

        for ( GAV gav : gavs )
        {
            try
            {
                InputStream is = contentLocator.getContents(
                    gav,
                    RepositoryLayoutHelper.CLASSIFIER_P2_METADATA,
                    RepositoryLayoutHelper.EXTENSION_P2_METADATA );
                try
                {
                    Set<IInstallableUnit> gavUnits = io.readXML( is );
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

    @Override
    public void initialize( RepositoryState state )
    {
    }

    public Collector query( Query query, Collector collector, IProgressMonitor monitor )
    {
        return query.perform( units.iterator(), collector );
    }

}
