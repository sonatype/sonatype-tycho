package org.sonatype.tycho.p2.repo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

import org.codehaus.tycho.p2.MetadataSerializable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryIO;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;

public class MetadataSerializableImpl
    implements MetadataSerializable
{

    private final Collection<IInstallableUnit> units;

    private final IProvisioningAgent agent;

    public MetadataSerializableImpl( Collection<IInstallableUnit> units, IProvisioningAgent agent )
    {
        super();
        this.units = units;
        this.agent = agent;
    }

    public Collection<IInstallableUnit> getUnits()
    {
        return units;
    }

    public void serialize( OutputStream stream )
        throws IOException
    {
        // TODO check if we can really "reuse" LocalMetadataRepository or should we implement our own Repository
        AbstractMetadataRepository targetRepo = new AbstractMetadataRepository( agent, "TychoTargetPlatform", LocalMetadataRepository.class.getName(), "0.0.1", null, null, null, null )
        {

            @Override
            public void initialize( RepositoryState state )
            {

            }

            public Collection<IRepositoryReference> getReferences()
            {
                return Collections.emptyList();
            }

            public IQueryResult<IInstallableUnit> query( IQuery<IInstallableUnit> query, IProgressMonitor monitor )
            {
                return query.perform( units.iterator() );
            }

        };

        new MetadataRepositoryIO( agent ).write( targetRepo, stream );
    }

}
