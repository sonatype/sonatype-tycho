package com.sonatype.tycho.enforcer;

import org.eclipse.osgi.service.resolver.VersionRange;
import org.junit.Assert;
import org.junit.Test;

public class VersionRangeHelperTest
{
    @Test
    public void test()
    {
        Assert.assertTrue( isIncluded( "[1,2)", "[1,2)" ) );

        Assert.assertTrue( isIncluded( "[1,4)", "[2,3)" ) );

        Assert.assertFalse( isIncluded( "[1,2)", "[1,2]" ) );

        Assert.assertFalse( isIncluded( "[1,2)", "1" ) );

        Assert.assertFalse( isIncluded( "[1,2)", "[0.5,1)" ) );

        Assert.assertFalse( isIncluded( "[1,2)", "[0.5,1]" ) );
    }

    private boolean isIncluded( String r1, String r2 )
    {
        return VersionRangeHelper.isIncluded( new VersionRange( r1 ), new VersionRange( r2 ) );
    }
}
