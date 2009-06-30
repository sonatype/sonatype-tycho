package org.codehaus.tycho.maven;

import java.util.Set;

import org.apache.maven.ArtifactFilterManagerDelegate;
import org.codehaus.plexus.component.annotations.Component;

@Component( role = ArtifactFilterManagerDelegate.class )
public class TychoArtifactFilterManager
    implements ArtifactFilterManagerDelegate
{

    public void addCoreExcludes( Set<String> excludes )
    {
        excludes.add( "tycho-osgi-components" );
        excludes.add( "org.eclipse.osgi" );
        excludes.add( "tycho-p2-facade" );
        excludes.add( "tycho-equinox" );
    }

    public void addExcludes( Set<String> excludes )
    {
        // nothing
    }

}
