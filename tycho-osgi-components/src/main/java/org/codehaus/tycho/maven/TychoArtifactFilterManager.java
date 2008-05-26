package org.codehaus.tycho.maven;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.ArtifactFilterManager;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionSetFilter;

/**
 * @plexus.component role="org.apache.maven.ArtifactFilterManager"
 *                   role-hint="default"
 *                   
 * Copy&paste of DefaultArtifactFilterManager. Needed to exclude tycho components
 * from getting loaded into plugin class realms
 */
public class TychoArtifactFilterManager implements ArtifactFilterManager {

    private static final Set<String> DEFAULT_EXCLUSIONS;

    static
    {
        Set<String> artifacts = new HashSet<String>();

        artifacts.add( "classworlds" );
        artifacts.add( "plexus-classworlds" );
        artifacts.add( "commons-cli" );
        artifacts.add( "doxia-sink-api" );
        artifacts.add( "jsch" );
        artifacts.add( "maven-artifact" );
        artifacts.add( "maven-artifact-manager" );
        artifacts.add( "maven-build-context" );
        artifacts.add( "maven-core" );
        artifacts.add( "maven-error-diagnoser" );
        artifacts.add( "maven-lifecycle" );
        artifacts.add( "maven-model" );
        artifacts.add( "maven-monitor" );
        artifacts.add( "maven-plugin-api" );
        artifacts.add( "maven-plugin-descriptor" );
        artifacts.add( "maven-plugin-parameter-documenter" );
        artifacts.add( "maven-profile" );
        artifacts.add( "maven-project" );
        artifacts.add( "maven-reporting-api" );
        artifacts.add( "maven-repository-metadata" );
        artifacts.add( "maven-settings" );
        //adding shared/maven-toolchain project here, even though not part of the default
        //distro yet.
        artifacts.add( "maven-toolchain" );
        artifacts.add( "plexus-component-api" );
        artifacts.add( "plexus-container-default" );
        artifacts.add( "plexus-interactivity-api" );
        artifacts.add( "wagon-provider-api" );
        artifacts.add( "wagon-file" );
        artifacts.add( "wagon-http-lightweight" );
        artifacts.add( "wagon-manager" );
        artifacts.add( "wagon-ssh" );
        artifacts.add( "wagon-ssh-external" );
        
        artifacts.add( "tycho-osgi-components" );
        artifacts.add( "org.eclipse.osgi" );

        DEFAULT_EXCLUSIONS = artifacts;
    }

    private Set<String> excludedArtifacts = new HashSet<String>( DEFAULT_EXCLUSIONS );

    /**
     * Returns the artifact filter for the core + extension artifacts.
     *
     * @see org.apache.maven.ArtifactFilterManager#getArtifactFilter()
     */
    public ArtifactFilter getArtifactFilter()
    {
        return new ExclusionSetFilter( excludedArtifacts );
    }

    /**
     * Returns the artifact filter for the standard core artifacts.
     *
     * @see org.apache.maven.ArtifactFilterManager#getExtensionArtifactFilter()
     */
    public ArtifactFilter getCoreArtifactFilter()
    {
        return new ExclusionSetFilter( DEFAULT_EXCLUSIONS );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.ArtifactFilterManager#excludeArtifact(java.lang.String)
     */
    public void excludeArtifact( String artifactId )
    {
        excludedArtifacts.add( artifactId );
    }
}
