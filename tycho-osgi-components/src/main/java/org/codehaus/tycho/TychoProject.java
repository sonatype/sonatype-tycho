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

    public ArtifactDependencyWalker getDependencyWalker( MavenProject project );

    /** @deprecated temporary, until target platform configuration properly supports multiple environments */   
    public ArtifactDependencyWalker getDependencyWalker( MavenProject project, TargetEnvironment environment );

    public TargetPlatform getTargetPlatform( MavenProject project );

    // implementation must not depend on target platform
    public ArtifactKey getArtifactKey( MavenProject project );

}
