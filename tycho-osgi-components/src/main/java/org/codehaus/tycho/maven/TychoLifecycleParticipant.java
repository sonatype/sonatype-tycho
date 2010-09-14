package org.codehaus.tycho.maven;

import org.apache.maven.execution.MavenSession;

/**
 * @TODO find a way to configure equinox proxy from tycho-p2-facade, without this ugly callback hack
 * 
 * @author igor
 */
public interface TychoLifecycleParticipant
{
    public void configure( MavenSession session );
}
