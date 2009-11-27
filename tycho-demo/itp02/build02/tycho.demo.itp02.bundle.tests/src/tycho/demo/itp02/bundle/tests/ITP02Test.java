package tycho.demo.itp02.bundle.tests;

import org.junit.Assert;
import org.junit.Test;

import tycho.demo.itp02.bundle.ITP02;

public class ITP02Test
{

    @Test
    public void basic()
    {
        ITP02 testee = new ITP02();
        Assert.assertEquals( "maven-bundle-plugin", testee.getMessage() );
    }
}
