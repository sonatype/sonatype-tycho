package org.sonatype.tycho.p2;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;

@SuppressWarnings( "restriction" )
public class TransientMetadataRepository
    extends AbstractMetadataRepository
{
    private ArrayList<IMetadataRepository> members = new ArrayList<IMetadataRepository>();

    public TransientMetadataRepository()
    {
    }

    public TransientMetadataRepository( URI location, String name, String type, Map properties )
    {
        super( name, type, "1", location, null, null, properties );
    }

    @Override
    public void initialize( RepositoryState state )
    {
    }

    public Collector query( Query query, Collector collector, IProgressMonitor monitor )
    {
        return query.perform( members.iterator(), collector );
    }

    public void add( IMetadataRepository member )
    {
        members.add( member );
    }

    @Override
    public boolean isModifiable()
    {
        return true;
    }
}
