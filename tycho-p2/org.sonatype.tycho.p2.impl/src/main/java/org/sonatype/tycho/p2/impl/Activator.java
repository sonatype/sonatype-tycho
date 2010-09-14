package org.sonatype.tycho.p2.impl;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.sonatype.tycho.p2.DirectorApplicationWrapper;
import org.sonatype.tycho.p2.MetadataSerializable;
import org.sonatype.tycho.p2.P2Generator;
import org.sonatype.tycho.p2.ProxyServiceFacade;
import org.sonatype.tycho.p2.impl.director.DirectorApplicationWrapperImpl;
import org.sonatype.tycho.p2.impl.proxy.ProxyServiceFacadeImpl;
import org.sonatype.tycho.p2.impl.publisher.P2GeneratorImpl;
import org.sonatype.tycho.p2.impl.repo.MetadataSerializableImpl;
import org.sonatype.tycho.p2.impl.resolver.P2ResolverImpl;
import org.sonatype.tycho.p2.resolver.P2Resolver;
import org.sonatype.tycho.p2.resolver.P2ResolverFactory;

public class Activator
    implements BundleActivator
{
    public static final String PLUGIN_ID = "org.sonatype.tycho.p2.impl";

    private static Activator instance;

    private BundleContext context;

    public Activator()
    {
        Activator.instance = this;
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
        context.registerService( DirectorApplicationWrapper.class.getName(), new DirectorApplicationWrapperImpl(), null );
        context.registerService( ProxyServiceFacade.class.getName(), new ProxyServiceFacadeImpl( context ), null );
        context.registerService( MetadataSerializable.class.getName(), new MetadataSerializableImpl( newProvisioningAgent() ), null );
    }

    public static IProvisioningAgent newProvisioningAgent()
        throws ProvisionException
    {
        BundleContext context = getContext();

        ServiceReference providerRef = context.getServiceReference( IProvisioningAgentProvider.SERVICE_NAME );
        IProvisioningAgentProvider provider = (IProvisioningAgentProvider) context.getService( providerRef );
        try
        {
            return provider.createAgent( null ); // null == currently running system
        }
        finally
        {
            context.ungetService( providerRef );
        }
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
