package org.sonatype.tycho.osgi;

import java.io.File;

public interface EquinoxEmbedder
{

    public <T> T getService( Class<T> clazz );

    public File getRuntimeLocation();

}
