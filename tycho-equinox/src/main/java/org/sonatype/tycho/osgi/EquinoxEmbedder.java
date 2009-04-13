package org.sonatype.tycho.osgi;

public interface EquinoxEmbedder
{

    public <T> T getService( Class<T> clazz );

}
