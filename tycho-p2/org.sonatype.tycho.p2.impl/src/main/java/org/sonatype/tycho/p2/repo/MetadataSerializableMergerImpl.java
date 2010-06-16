package org.sonatype.tycho.p2.repo;

import java.util.Collection;
import java.util.HashSet;

import org.codehaus.tycho.p2.MetadataSerializable;
import org.codehaus.tycho.p2.MetadataSerializableMerger;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.sonatype.tycho.p2.Activator;

public class MetadataSerializableMergerImpl
    implements MetadataSerializableMerger<MetadataSerializableImpl>
{

    public MetadataSerializable merge( Collection<MetadataSerializableImpl> repositories )
    {
        Collection<IInstallableUnit> units = new HashSet<IInstallableUnit>();
        for ( MetadataSerializableImpl serializable : repositories )
        {
            units.addAll(serializable.getUnits());
        }
        try
        {
            return new MetadataSerializableImpl( units, Activator.newProvisioningAgent() );
        }
        catch ( ProvisionException e )
        {
            //Does not occur
            throw new RuntimeException(e);
        }
    }

}
