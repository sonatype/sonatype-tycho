package org.sonatype.tycho.p2.resolver;

public interface P2Logger
{
    public void info( String message );

    public void debug( String message );

    public boolean isDebugEnabled();
}
