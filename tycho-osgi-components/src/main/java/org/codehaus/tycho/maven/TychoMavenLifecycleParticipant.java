package org.codehaus.tycho.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.tycho.osgitools.BundleReader;
import org.codehaus.tycho.osgitools.DefaultBundleReader;
import org.codehaus.tycho.resolver.DefaultTychoDependencyResolver;
import org.sonatype.tycho.equinox.embedder.EquinoxEmbedder;
import org.sonatype.tycho.equinox.embedder.EquinoxRuntimeLocator;

@Component( role = AbstractMavenLifecycleParticipant.class, hint = "TychoMavenLifecycleListener" )
public class TychoMavenLifecycleParticipant
    extends AbstractMavenLifecycleParticipant
{
    @Requirement
    private Logger logger;

    @Requirement
    private TychoP2RuntimeLocator p2runtime;

    @Requirement
    private EquinoxRuntimeLocator equinoxLocator;

    @Requirement
    private EquinoxEmbedder equinoxEmbedder;

    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private DefaultTychoDependencyResolver resolver;

    @Requirement( role = TychoLifecycleParticipant.class )
    private List<TychoLifecycleParticipant> lifecycleParticipants;

    public void afterProjectsRead( MavenSession session )
        throws MavenExecutionException
    {
        if ( "maven".equals( session.getUserProperties().get( "tycho.mode" ) ) )
        {
            return;
        }

        if ( session.getUserProperties().containsKey( "m2e.version" ) )
        {
            return;
        }

        System.setProperty( "osgi.framework.useSystemProperties", "false" ); //$NON-NLS-1$ //$NON-NLS-2$

        File localRepository = new File( session.getLocalRepository().getBasedir() );
        ( (DefaultBundleReader) bundleReader ).setLocationRepository( localRepository );

        File p2Directory = p2runtime.locateTychoP2Runtime( session );
        if ( p2Directory != null )
        {
            equinoxLocator.addRuntimeLocation( p2Directory );
            logger.debug( "Using P2 runtime at " + p2Directory );
        }

        File secureStorage = new File( session.getLocalRepository().getBasedir(), ".meta/tycho.secure_storage" );
        List<String> nonFrameworkArgs = new ArrayList<String>();
        nonFrameworkArgs.add( "-eclipse.keyring" );
        nonFrameworkArgs.add( secureStorage.getAbsolutePath() );
        // TODO nonFrameworkArgs.add("-eclipse.password");
        // nonFrameworkArgs.add("");
        if ( logger.isDebugEnabled() )
        {
            nonFrameworkArgs.add( "-debug" );
            nonFrameworkArgs.add( "-consoleLog" );
        }
        equinoxEmbedder.setNonFrameworkArgs( nonFrameworkArgs.toArray( new String[0] ) );

        for ( TychoLifecycleParticipant participant : lifecycleParticipants )
        {
            participant.configure( session );
        }

        List<MavenProject> projects = session.getProjects();

        resolver.setupProjects( session, projects );

        for ( MavenProject project : projects )
        {
            resolver.resolveProject( session, project );
        }
    }

}
