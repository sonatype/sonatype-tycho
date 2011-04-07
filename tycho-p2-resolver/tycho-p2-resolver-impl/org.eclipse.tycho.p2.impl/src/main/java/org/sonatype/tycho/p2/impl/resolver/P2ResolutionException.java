package org.sonatype.tycho.p2.impl.resolver;

public abstract class P2ResolutionException
    extends RuntimeException
{
    private static final long serialVersionUID = 4777222946987138665L;

    protected P2ResolutionException( String message )
    {
        super( message );
    }
}
