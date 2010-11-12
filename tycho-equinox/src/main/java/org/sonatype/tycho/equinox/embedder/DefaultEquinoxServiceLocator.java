package org.sonatype.tycho.equinox.embedder;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.tycho.equinox.EquinoxServiceFactory;

@Component( role = EquinoxServiceFactory.class )
public class DefaultEquinoxServiceLocator
    implements EquinoxServiceFactory
{
    @Requirement
    private EquinoxEmbedder equinox;

    public <T> T getService( Class<T> clazz )
    {
        return equinox.getService( clazz );
    }

    public <T> T getService( Class<T> clazz, String filter )
    {
        return equinox.getService( clazz, filter );
    }
}
