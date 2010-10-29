package org.sonatype.tycho.runtime;

public interface Adaptable
{
    public <T> T getAdapter( Class<T> adapter );
}
