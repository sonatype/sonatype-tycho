package org.sonatype.tycho.p2.publisher;

import java.lang.reflect.Method;

import org.eclipse.equinox.p2.publisher.AbstractPublisherApplication;

@SuppressWarnings( "restriction" )
public class CategoryPublisherApplication
    extends org.eclipse.equinox.internal.p2.updatesite.CategoryPublisherApplication
{

    public CategoryPublisherApplication()
    {
        try
        {
            // TODO HACK to work around Eclipse bug https://bugs.eclipse.org/315757 so that context repositories can be
            // set
            final Method setupAgentMethod = AbstractPublisherApplication.class.getDeclaredMethod( "setupAgent" ); //$NON-NLS-1$
            setupAgentMethod.setAccessible( true );
            setupAgentMethod.invoke( this );
        }
        catch ( final Exception e )
        {
            e.printStackTrace();
            throw new RuntimeException( e );
        }
    }

}
