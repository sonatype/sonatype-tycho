package org.sonatype.tycho.osgi;

import java.io.File;

public interface EquinoxEmbedder
{

    public <T> T getService( Class<T> clazz );
    
    /**
     * Register a callback which will be notified when all bundles are started.
     */
    public void registerAfterStartCallback( Runnable callback);
    
    public File getRuntimeLocation();

    public void setNonFrameworkArgs( String[] strings );


}
