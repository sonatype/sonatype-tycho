package org.codehaus.tycho.eclipsepackaging;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.FeatureResolutionState;
import org.codehaus.tycho.TychoSession;
import org.codehaus.tycho.maven.TychoMavenSession;

public abstract class AbstractTychoPackagingMojo
    extends AbstractMojo
    implements Contextualizable
{
    /** @parameter expression="${session}" */
    protected MavenSession session;

    /**
     * @parameter expression="${project}"
     */
    protected MavenProject project;

    protected PlexusContainer plexus;

    protected TychoSession tychoSession;

    protected FeatureResolutionState featureResolutionState;

    protected BundleResolutionState bundleResolutionState;

    public void contextualize( Context ctx )
        throws ContextException
    {
        plexus = (PlexusContainer) ctx.get( PlexusConstants.PLEXUS_KEY );
    }

    protected void initializeProjectContext()
    {
        if ( !( session instanceof TychoMavenSession ) )
        {
            throw new IllegalArgumentException( getClass().getSimpleName() + " mojo only works with Tycho distribution" );
        }

        TychoMavenSession tms = (TychoMavenSession) session;

        tychoSession = tms.getTychoSession();

        featureResolutionState = tychoSession.getFeatureResolutionState( project );

        bundleResolutionState = tychoSession.getBundleResolutionState( project );
    }
}
