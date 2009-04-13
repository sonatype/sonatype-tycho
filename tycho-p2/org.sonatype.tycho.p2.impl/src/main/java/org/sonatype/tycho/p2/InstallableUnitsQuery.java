package org.sonatype.tycho.p2;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.MatchQuery;

@SuppressWarnings( "restriction" )
public class InstallableUnitsQuery
    extends MatchQuery
{

    private final Map<String, String> units;

    private final boolean strict;

    public InstallableUnitsQuery( Map<String, String> units )
    {
        this( units, true );
    }

    public InstallableUnitsQuery( Map<String, String> units, boolean strict )
    {
        this.units = units;
        this.strict = strict;
    }

    public InstallableUnitsQuery( String[] units )
    {
        this( toUnitsMap( units ), false );
    }

    private static Map<String, String> toUnitsMap( String[] units )
    {
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
        for ( String unit : units )
        {
            result.put( unit, null );
        }
        return result;
    }

    @Override
    public boolean isMatch( Object object )
    {
        if ( !( object instanceof IInstallableUnit ) )
        {
            return false;
        }

        IInstallableUnit candidate = (IInstallableUnit) object;

        String versionStr = units.get( candidate.getId() );

        if ( versionStr == null )
        {
            return !strict && units.containsKey( candidate.getId() );
        }

        return new Version( versionStr ).equals( candidate.getVersion() );
    }

}
