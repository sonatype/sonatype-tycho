package org.sonatype.tycho.p2.maven.repository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.facade.internal.GAV;
import org.sonatype.tycho.p2.facade.internal.RepositoryReader;
import org.sonatype.tycho.p2.facade.internal.TychoRepositoryIndex;
import org.sonatype.tycho.p2.maven.repository.xmlio.MetadataIO;

@SuppressWarnings( "restriction" )
public abstract class AbstractMavenMetadataRepository
    extends AbstractMetadataRepository
{
    private static final String REPOSITORY_TYPE = AbstractMavenMetadataRepository.class.getName();

    private static final String REPOSITORY_VERSION = "1.0.0";

    protected final TychoRepositoryIndex projectIndex;

    protected final RepositoryReader contentLocator;

    protected Set<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();

    protected Map<GAV, Set<IInstallableUnit>> unitsMap = new HashMap<GAV, Set<IInstallableUnit>>();

    public AbstractMavenMetadataRepository( URI location, Map properties, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator )
    {
        super( location.toString(), REPOSITORY_TYPE, REPOSITORY_VERSION, location, null, null, properties );

        this.projectIndex = projectIndex;
        this.contentLocator = contentLocator;

        if ( projectIndex != null && contentLocator != null)
        {
            load();
        }
    }

    protected void load()
    {
        MetadataIO io = new MetadataIO();

        for ( GAV gav : projectIndex.getProjectGAVs() )
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
