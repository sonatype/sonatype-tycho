package org.codehaus.tycho;

import org.apache.maven.project.MavenProject;


/**
 * tycho-specific behaviour associated with MavenProject instances. stateless.
 * 
 * TODO take target environments into account!
 */
public interface TychoProject
{
    public static final String ECLIPSE_PLUGIN = "eclipse-plugin";
    public static final String ECLIPSE_TEST_PLUGIN = "eclipse-test-plugin";
    public static final String ECLIPSE_FEATURE = "eclipse-feature";
    public static final String ECLIPSE_UPDATE_SITE = "eclipse-update-site";
    public static final String ECLIPSE_APPLICATION = "eclipse-application";
    public static final String[] PROJECT_TYPES = {
        ECLIPSE_PLUGIN,
        ECLIPSE_TEST_PLUGIN,
        ECLIPSE_FEATURE,
        ECLIPSE_UPDATE_SITE,
        ECLIPSE_APPLICATION
    };

    /**
     * Walks all project dependencies, regardless of runtime environment filters.
     */
    public ArtifactDependencyWalker getDependencyWalker( MavenProject project );

    /**
     * Walks project dependencies resolved for the specified runtime environment.
     */
    public ArtifactDependencyWalker getDependencyWalker( MavenProject project, TargetEnvironment environment );

    /**
     * Returns project build target platform. For projects targeting multiple 
     * runtime environments, returned target platforms includes artifacts 
     * for all supported runtime environments. 
     */
    public TargetPlatform getTargetPlatform( MavenProject project );

    /**
     * Returns project build target platform resolved for specified runtime environment.
     */
    public TargetPlatform getTargetPlatform( MavenProject project, TargetEnvironment environment );

    // implementation must not depend on target platform
    public ArtifactKey getArtifactKey( MavenProject project );

}
