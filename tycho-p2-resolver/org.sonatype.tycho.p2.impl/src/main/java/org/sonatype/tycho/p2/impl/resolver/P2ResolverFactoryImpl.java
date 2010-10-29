package org.sonatype.tycho.p2.impl.resolver;

import org.sonatype.tycho.p2.resolver.P2Resolver;
import org.sonatype.tycho.p2.resolver.P2ResolverFactory;

public class P2ResolverFactoryImpl
    implements P2ResolverFactory
{

    public P2Resolver createResolver()
    {
        return new P2ResolverImpl();
    }

}
