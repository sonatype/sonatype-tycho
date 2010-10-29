package org.codehaus.tycho.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.tycho.osgitools.BundleReader;
import org.codehaus.tycho.osgitools.DefaultBundleReader;
import org.sonatype.tycho.equinox.embedder.EquinoxEmbedder;
import org.sonatype.tycho.resolver.TychoDependencyResolver;

@Component( role = AbstractMavenLifecycleParticipant.class, hint = "TychoMavenLifecycleListener" )
public class TychoMavenLifecycleParticipant
    extends AbstractMavenLifecycleParticipant
    implements Disposable
{
    @Requirement
    private Logger logger;

    @Requirement
    private EquinoxEmbedder equinoxEmbedder;

    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private TychoDependencyResolver resolver;

    private File secureStorage;

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

        // XXX why do we need this here?
        System.setProperty( "osgi.framework.useSystemProperties", "false" ); //$NON-NLS-1$ //$NON-NLS-2$

        File localRepository = new File( session.getLocalRepository().getBasedir() );
        ( (DefaultBundleReader) bundleReader ).setLocationRepository( localRepository );

        try
        {
            secureStorage = File.createTempFile( "tycho", "secure_storage" );
            secureStorage.deleteOnExit();
        }
        catch ( IOException e )
        {
            throw new MavenExecutionException( "Could not create Tycho secure store file", e );
        }

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

        List<MavenProject> projects = session.getProjects();

        resolver.setupProjects( session, projects );

        for ( MavenProject project : projects )
        {
            resolver.resolveProject( session, project );
        }
    }

    public void dispose()
    {
        secureStorage.delete();
    }

}
