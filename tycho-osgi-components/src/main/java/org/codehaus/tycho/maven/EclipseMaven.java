package org.codehaus.tycho.maven;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.model.Activation;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.tycho.TychoSession;
import org.codehaus.tycho.osgitools.MutableTychoSession;
import org.codehaus.tycho.osgitools.targetplatform.LocalTargetPlatformResolver;
import org.codehaus.tycho.osgitools.targetplatform.MavenTargetPlatformResolver;
import org.codehaus.tycho.osgitools.targetplatform.Tycho03TargetPlatformResolver;
import org.codehaus.tycho.osgitools.utils.TychoVersion;
import org.codehaus.tycho.p2.P2ArtifactRepositoryLayout;
import org.sonatype.tycho.TargetPlatformResolver;
import org.sonatype.tycho.p2.facade.P2Facade;

@Component(role = Maven.class)
public class EclipseMaven extends DefaultMaven {

    @Requirement
    private P2Facade p2;

    private MutableTychoSession tychoSession;

	@Override
	protected List getProjects(MavenExecutionRequest request) throws MavenExecutionException {
		request.setProperty("tycho-version", TychoVersion.getTychoVersion());
		List<MavenProject> projects = super.getProjects(request);

		resolveOSGiState(projects, request);

		return projects;
	}

	@Override
	protected MavenSession createSession( MavenExecutionRequest request, ReactorManager reactorManager,
	    EventDispatcher dispatcher )
	{
        TychoMavenSession session = new TychoMavenSession( container, request, dispatcher, reactorManager, this.tychoSession );

        return session;
	}

	/** For tests only. This mething will be removed after maven 3.0 alpha-3 */
	@Deprecated
	public TychoSession getTychoSession()
	{
	    return tychoSession;
	}

	private void resolveOSGiState(List<MavenProject> projects, MavenExecutionRequest request) throws MavenExecutionException {
        
	    try
        {
            tychoSession = container.lookup(MutableTychoSession.class);
        }
        catch ( ComponentLookupException e )
        {
            throw new MavenExecutionException( e.getMessage(), new IOException() );
        }

        Properties props = getGlobalProperties(request);

		TargetPlatformResolver resolver;

//		String property = props.getProperty("tycho.targetPlatform");
//
//        if ( property != null )
//        {
//            File location = new File( property );
//            if ( !location.exists() || !location.isDirectory() )
//            {
//                throw new RuntimeException( "Invalid target platform location" );
//            }
//
//            try
//            {
//                resolver = container.lookup( TargetPlatformResolver.class, LocalTargetPlatformResolver.ROLE_HINT );
//            }
//            catch ( ComponentLookupException e )
//            {
//                throw new MavenExecutionException( "Could not instantiate required component", new IOException() );
//            }
//
//            resolver.addRepository( new File( property ).toURI() );
//        }
//        else
//        {
//            try
//            {
//                resolver = container.lookup( TargetPlatformResolver.class, Tycho03TargetPlatformResolver.ROLE_HINT );
//            }
//            catch ( ComponentLookupException e )
//            {
//                throw new MavenExecutionException( "Could not instantiate required component", new IOException() );
//            }
//        }

		resolver = p2.createResolver();

		// XXX different projects can have different repositories. resolver MUST be per-project 
		Set<URI> repositories = new LinkedHashSet<URI>();
        for (MavenProject project : projects) {
            for (ArtifactRepository repository : (List<ArtifactRepository>) project.getRemoteArtifactRepositories() ) {
                if (repository.getLayout() instanceof P2ArtifactRepositoryLayout) {
                    try
                    {
                        repositories.add(new URL(repository.getUrl()).toURI());
                    }
                    catch ( Exception e )
                    {
                        getLogger().info("Could not parse P2 repository URL", e);
                    }
                }
            }
        }

        for (URI repository : repositories) {
            resolver.addRepository( repository );
        }

		resolver.setLocalRepositoryLocation( new File( request.getLocalRepository().getBasedir() ) );

		resolver.setProperties( props );

		// collect all projects
		for (MavenProject project : projects) {
		    tychoSession.addProject(project);
            try {
                DependenciesReader dr = (DependenciesReader) container.lookup(DependenciesReader.class, project.getPackaging());
                dr.addProject(resolver, project);
            } catch (ComponentLookupException e) {
                // no biggie 
            }
        }

		if ( resolver instanceof MavenTargetPlatformResolver )
		{
		    MavenTargetPlatformResolver mavenResolver = (MavenTargetPlatformResolver) resolver;

		    mavenResolver.setMavenProjects( projects );

		    mavenResolver.setLocalRepository( request.getLocalRepository() );
		}

		for (MavenProject project : projects) {
            try {
                DependenciesReader dr = (DependenciesReader) container.lookup(DependenciesReader.class, project.getPackaging());
                tychoSession.setTargetPlatform( project, resolver.resolvePlatform(project.getBasedir()) );
                for (Dependency dependency : dr.getDependencies(project, tychoSession)) {
                    project.getModel().addDependency(dependency);
                }
            } catch (ComponentLookupException e) {
                // no biggie 
            }
        }

	}

	// XXX there must be an easier way
	private Properties getGlobalProperties(MavenExecutionRequest request) {
		List<String> activeProfiles = request.getActiveProfiles();
		Map<String, Profile> profiles = request.getProfileManager().getProfilesById();

		Properties props = new Properties();
		props.putAll(System.getProperties());
		for (Profile profile : profiles.values()) {
			Activation activation = profile.getActivation();
			if ((activation != null && activation.isActiveByDefault()) || activeProfiles.contains(profile.getId())) {
				props.putAll(profile.getProperties());
			}
		}

		props.putAll(request.getProperties());
		props.putAll(request.getUserProperties());

		return props;
	}

}
