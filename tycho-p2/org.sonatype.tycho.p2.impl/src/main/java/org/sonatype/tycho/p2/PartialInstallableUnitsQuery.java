package org.sonatype.tycho.p2;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.MatchQuery;

@SuppressWarnings( "restriction" )
public class PartialInstallableUnitsQuery
    extends MatchQuery
{

    public static final PartialInstallableUnitsQuery QUERY = new PartialInstallableUnitsQuery();

    @Override
    public boolean isMatch( Object candidate )
    {
        if ( !( candidate instanceof IInstallableUnit ) )
        {
            return false;
        }

        IInstallableUnit iu = (IInstallableUnit) candidate;

        return isPartialIU( iu );
    }

    public static boolean isPartialIU( IInstallableUnit iu )
    {
        return Boolean.valueOf( iu.getProperty( IInstallableUnit.PROP_PARTIAL_IU ) ).booleanValue();
    }

}
