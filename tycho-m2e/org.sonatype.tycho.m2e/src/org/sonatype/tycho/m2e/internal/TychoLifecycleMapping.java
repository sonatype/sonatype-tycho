package org.sonatype.tycho.m2e.internal;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.maven.ide.eclipse.project.configurator.IExtensionLifecycleMapping;
import org.maven.ide.eclipse.project.configurator.NoopLifecycleMapping;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

@SuppressWarnings( "restriction" )
public class TychoLifecycleMapping
    extends NoopLifecycleMapping
    implements IExtensionLifecycleMapping
{

    @Override
    public void configure( ProjectConfigurationRequest request, IProgressMonitor monitor )
        throws CoreException
    {
        MavenProject mavenProject = request.getMavenProject();
        IProject project = request.getProject();

        String packaging = mavenProject.getPackaging();
        if ( "eclipse-plugin".equals( packaging ) || "eclipse-test-plugin".equals( packaging ) )
        {
            configurePDEBundleProject( project, monitor );
        }
        else if ( "eclipse-feature".equals( packaging ) )
        {
            // see org.eclipse.pde.internal.ui.wizards.feature.AbstractCreateFeatureOperation
            if ( !project.hasNature( PDE.FEATURE_NATURE ) )
            {
                CoreUtility.addNatureToProject( project, PDE.FEATURE_NATURE, monitor );
            }
        }
        else if ( "eclipse-update-site".equals( packaging ) )
        {
            // see org.eclipse.pde.internal.ui.wizards.site.NewSiteProjectCreationOperation
            if ( !project.hasNature( PDE.SITE_NATURE ) )
            {
                CoreUtility.addNatureToProject(project, PDE.SITE_NATURE, monitor);
            }
        }
    }

    private void configurePDEBundleProject( IProject project, IProgressMonitor monitor )
        throws CoreException
    {
        // see org.eclipse.pde.internal.ui.wizards.plugin.NewProjectCreationOperation

        if ( !project.hasNature( PDE.PLUGIN_NATURE ) )
        {
            CoreUtility.addNatureToProject( project, PDE.PLUGIN_NATURE, null );
        }

        if ( !project.hasNature( JavaCore.NATURE_ID ) )
        {
            CoreUtility.addNatureToProject( project, JavaCore.NATURE_ID, null );
        }

        // PDE can't handle default JDT classpath
        IJavaProject javaProject = JavaCore.create(project);
        javaProject.setRawClasspath( new IClasspathEntry[0], true, monitor );

        // see org.eclipse.pde.internal.ui.wizards.tools.UpdateClasspathJob
        IPluginModelBase model = PluginRegistry.findModel( project );
        if ( model != null )
        {
            // PDE populates the model cache lazily from WorkspacePluginModelManager.visit() ResourceChangeListenter
            // Avoid NPE for now, but users may have to invoke PDE->UpdateClasspath manually  
            ClasspathComputer.setClasspath( project, model );
        }
    }

}
