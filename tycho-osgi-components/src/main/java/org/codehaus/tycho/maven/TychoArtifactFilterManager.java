package org.codehaus.tycho.maven;

import org.apache.maven.ArtifactFilterManager;
import org.apache.maven.DefaultArtifactFilterManager;
import org.codehaus.plexus.component.annotations.Component;

@Component( role = ArtifactFilterManager.class )
public class TychoArtifactFilterManager extends DefaultArtifactFilterManager {

	@SuppressWarnings("unchecked")
    public TychoArtifactFilterManager() {
		excludedArtifacts.add( "tycho-osgi-components" );
		excludedArtifacts.add( "org.eclipse.osgi" );
		excludedArtifacts.add( "tycho-p2-facade" );
        excludedArtifacts.add( "tycho-equinox" );
    }

}
