package org.codehaus.tycho.maven;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.monitor.event.EventDispatcher;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.tycho.TychoSession;

public class TychoMavenSession
    extends MavenSession
{

    private final TychoSession tychoSession;

    public TychoMavenSession( PlexusContainer container, MavenExecutionRequest request,
        EventDispatcher eventDispatcher, ReactorManager reactorManager, TychoSession tychoSession )
    {
        super( container, request, eventDispatcher, reactorManager );
        this.tychoSession = tychoSession;
    }

    public TychoSession getTychoSession()
    {
        return tychoSession;
    }
    
}
