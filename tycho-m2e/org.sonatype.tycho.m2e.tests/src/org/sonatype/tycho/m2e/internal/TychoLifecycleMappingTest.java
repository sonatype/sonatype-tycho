package org.sonatype.tycho.m2e.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.natures.PDE;
import org.maven.ide.eclipse.configurators.AbstractLifecycleMappingTest;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;

@SuppressWarnings( "restriction" )
public class TychoLifecycleMappingTest
    extends AbstractLifecycleMappingTest
{
    private IMavenProjectFacade importProjectAndAssertLifecycleMappingType( String pomName )
        throws Exception
    {
        IMavenProjectFacade facade = importMavenProject( pomName );
        ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping( facade, monitor );

        assertTrue( lifecycleMapping instanceof TychoLifecycleMapping );
        
        return facade;
    }
    
    public void testTychoLifecycleMapping_EclipsePlugin()
        throws Exception
    {
        IMavenProjectFacade facade = importProjectAndAssertLifecycleMappingType( "projects/lifecyclemapping/tycho-eclipse-plugin/pom.xml" );

        IProject project = facade.getProject();
        assertTrue( project.hasNature( PDE.PLUGIN_NATURE ) );
        assertTrue( project.hasNature( JavaCore.NATURE_ID ) );
        assertNotNull(PluginRegistry.findModel( project ));
        // project.build( 6, monitor );
    }

    public void testTychoLifecycleMapping_EclipseTestPlugin()
        throws Exception
    {
        IMavenProjectFacade facade = importProjectAndAssertLifecycleMappingType( "projects/lifecyclemapping/tycho-eclipse-test-plugin/pom.xml" );

        IProject project = facade.getProject();
        assertTrue( project.hasNature( PDE.PLUGIN_NATURE ) );
        assertTrue( project.hasNature( JavaCore.NATURE_ID ) );
        assertNotNull(PluginRegistry.findModel( project ));
    }

    public void testTychoLifecycleMapping_EclipseFeature()
        throws Exception
    {
        IMavenProjectFacade facade = importProjectAndAssertLifecycleMappingType( "projects/lifecyclemapping/tycho-eclipse-feature/pom.xml" );

        IProject project = facade.getProject();
        assertTrue( project.hasNature( PDE.FEATURE_NATURE ) );
    }

    public void testTychoLifecycleMapping_EclipseUpdateSite()
        throws Exception
    {
        IMavenProjectFacade facade = importProjectAndAssertLifecycleMappingType( "projects/lifecyclemapping/tycho-eclipse-update-site/pom.xml" );

        IProject project = facade.getProject();
        assertTrue( project.hasNature( PDE.SITE_NATURE ) );
    }
}
