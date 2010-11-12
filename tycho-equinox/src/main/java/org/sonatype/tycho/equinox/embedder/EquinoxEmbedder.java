package org.sonatype.tycho.equinox.embedder;

import org.sonatype.tycho.equinox.EquinoxServiceFactory;

public interface EquinoxEmbedder
{
    /**
     * {@link EquinoxServiceFactory#getService(Class)} is the preferred client API to locate Equinox services.
     */
    public <T> T getService( Class<T> clazz );

    /**
     * {@link EquinoxServiceFactory#getService(Class, String)} is the preferred client API to locate Equinox services.
     */
    public <T> T getService( Class<T> clazz, String filter );

    public void setNonFrameworkArgs( String[] strings );
}
