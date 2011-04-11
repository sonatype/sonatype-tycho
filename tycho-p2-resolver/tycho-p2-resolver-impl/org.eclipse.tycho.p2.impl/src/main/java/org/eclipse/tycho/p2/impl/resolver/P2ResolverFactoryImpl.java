package org.eclipse.tycho.p2.impl.resolver;

import org.eclipse.tycho.p2.resolver.P2Resolver;
import org.eclipse.tycho.p2.resolver.P2ResolverFactory;

public class P2ResolverFactoryImpl
    implements P2ResolverFactory
{

    public P2Resolver createResolver()
    {
        return new P2ResolverImpl();
    }

}
