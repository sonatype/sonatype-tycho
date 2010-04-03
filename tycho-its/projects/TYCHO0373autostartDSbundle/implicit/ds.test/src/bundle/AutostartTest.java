package bundle;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import test.component.IComponent;

public class AutostartTest
{
    @Test
    public void test()
    {
        ServiceReference serviceReference = Activator.context.getServiceReference( IComponent.class.getName() );
        Assert.assertNotNull( serviceReference );
    }
}
