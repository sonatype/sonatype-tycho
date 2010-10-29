package org.sonatype.tycho.equinox.embedder;

public abstract class EquinoxLifecycleListener
{
    public abstract void afterFrameworkStarted( EquinoxEmbedder framework );
}
