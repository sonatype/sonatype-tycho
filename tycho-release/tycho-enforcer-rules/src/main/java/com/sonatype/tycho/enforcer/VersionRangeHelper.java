package com.sonatype.tycho.enforcer;

import org.eclipse.osgi.service.resolver.VersionRange;

public class VersionRangeHelper
{
    /**
     * Returns true, if r2 is fully contained in r1
     */
    public static boolean isIncluded( VersionRange r1, VersionRange r2 )
    {
        int minCompare = r2.getMinimum().compareTo( r1.getMinimum() );

        if ( minCompare < 0 )
        {
            // r2 lower bound is outside of r1
            return false;
        }
        else if ( minCompare == 0 && ( !r1.getIncludeMinimum() && r2.getIncludeMinimum() ) )
        {
            return false;
        }

        int maxCompare = r2.getMaximum().compareTo( r1.getMaximum() );

        if ( maxCompare > 0 )
        {
            // r2 upper bound outside of r1
            return false;
        }
        else if ( maxCompare == 0 && ( !r1.getIncludeMaximum() && r2.getIncludeMaximum() ) )
        {
            return false;
        }

        return true;
    }

}
