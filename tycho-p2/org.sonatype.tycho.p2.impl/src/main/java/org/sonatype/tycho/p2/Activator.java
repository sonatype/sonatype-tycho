package org.sonatype.tycho.p2;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.sonatype.tycho.p2.facade.P2Generator;
import org.sonatype.tycho.p2.facade.internal.P2Resolver;
import org.sonatype.tycho.p2.facade.internal.P2ResolverFactory;
import org.sonatype.tycho.p2.publisher.P2GeneratorImpl;
import org.sonatype.tycho.p2.resolver.P2ResolverImpl;

public class Activator
    implements BundleActivator
{
    public static final String PLUGIN_ID = "org.sonatype.tycho.p2.impl";

    private static Activator instance;

    private BundleContext context;

    private IProvisioningAgent agent;

    public Activator()
    {
        this.instance = this;
    }

    public void start( BundleContext context )
        throws Exception
    {
        this.context = context;

        ServiceReference providerRef = context.getServiceReference( IProvisioningAgentProvider.SERVICE_NAME );
        IProvisioningAgentProvider provider = (IProvisioningAgentProvider) context.getService( providerRef );
        agent = provider.createAgent( null ); // null == currently running system
        context.ungetService( providerRef );

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

    public static IProvisioningAgent getProvisioningAgent()
    {
        return instance.agent;
    }
}
