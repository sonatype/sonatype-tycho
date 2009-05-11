package org.sonatype.tycho.p2;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.sonatype.tycho.p2.facade.P2Generator;
import org.sonatype.tycho.p2.facade.internal.P2Resolver;
import org.sonatype.tycho.p2.facade.internal.P2ResolverFactory;

public class Activator
    implements BundleActivator
{
    private static Activator instance;

    private BundleContext context;

    public Activator()
    {
        this.instance = this;
    }

    public void start( BundleContext context )
        throws Exception
    {
        this.context = context;
        context.registerService( P2ResolverFactory.class.getName(), new P2ResolverFactory()
        {
            public P2Resolver createResolver()
            {
                return new P2ResolverImpl();
            }
        }, null );
        context.registerService( P2Generator.class.getName(), new P2GeneratorImpl( false ), null );
    }

    public void stop( BundleContext context )
        throws Exception
    {
    }

    public static BundleContext getContext()
    {
        return instance.context;
    }
}
