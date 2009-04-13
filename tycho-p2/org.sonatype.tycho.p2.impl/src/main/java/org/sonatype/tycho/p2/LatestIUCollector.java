package org.sonatype.tycho.p2;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;

@SuppressWarnings( "restriction" )
public class LatestIUCollector
    extends Collector
{

    private final Map<String, IInstallableUnit> units = new LinkedHashMap<String, IInstallableUnit>();

    @Override
    public boolean accept( Object object )
    {
        if ( object instanceof IInstallableUnit )
        {
            IInstallableUnit iu = (IInstallableUnit) object;

            IInstallableUnit old = units.get( iu.getId() );

            if ( old == null || old.getVersion().compareTo( iu.getVersion() ) < 0 )
            {
                if ( old != null )
                {
                    getCollection().remove( old );
                }
                units.put( iu.getId(), iu );
                getCollection().add( iu );
            }
        }

        return true;
    }
}
