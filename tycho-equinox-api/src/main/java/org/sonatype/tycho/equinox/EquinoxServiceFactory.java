package org.sonatype.tycho.equinox;

/**
 * "Client" interface to access OSGi services registered with Equinox framework.
 */
public interface EquinoxServiceFactory
{
    public <T> T getService( Class<T> clazz );

    public <T> T getService( Class<T> clazz, String filter );
}
