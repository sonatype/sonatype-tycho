package org.codehaus.tycho.osgitest.emma;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.osgitools.project.BuildOutputJar;
import org.codehaus.tycho.osgitools.project.EclipsePluginProject;

/**
 * Searches all Java source files of a Maven projects and generates a list of
 * class names.
 */
class ClassNameCollector
{

    /**
     * Maven project to search.
     */
    private MavenProject project;

    /**
     * Constructor.
     *
     * @param project Maven to project to search
     */
    public ClassNameCollector( MavenProject project )
    {
        this.project = project;
    }

    /**
     * Searches for Java source files in "src/main/java" and "src" and returns a
     * set of found class names.
     *
     * @return set of class names
     */
    public Set<String> collectJavaClassNames()
    {
        Set<String> javaClassesInSources = new TreeSet<String>();

        EclipsePluginProject pdeProject =
            (EclipsePluginProject) project.getContextValue( TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT );
        List<File> srcFolders = new ArrayList<File>();
        if ( pdeProject.getDotOutputJar() != null )
        {
            srcFolders.addAll( pdeProject.getDotOutputJar().getSourceFolders() );
        }
        for ( BuildOutputJar jar : pdeProject.getOutputJars() )
        {
            srcFolders.addAll( jar.getSourceFolders() );
        }

        boolean sourcePathAdded = false;
        if ( !srcFolders.isEmpty() )
        {
            for ( int i = 0; i < srcFolders.size(); i++ )
            {
                File file = srcFolders.get( i );
                sourcePathAdded |= addSourcePath( javaClassesInSources, file );
            }
        }
        if ( sourcePathAdded )
        {
            return javaClassesInSources;
        }

        // check if the project's source path exists
        // note: getCompileSourceRoots() usually returns "src/main/java"
        // which doesn't exist for "eclipse-plugin" or "eclipse-test-plugin"
        // projects, there it is only "src"

        for ( String sourcePathObj : project.getCompileSourceRoots() )
        {
            File sourcePath = new File( sourcePathObj );
            addSourcePath( javaClassesInSources, sourcePath );
        }

        return javaClassesInSources;
    }

    private boolean addSourcePath( Set<String> javaClassesInSources, File sourcePath )
    {
        if ( sourcePath.isDirectory() )
        {
            Collection<String> javaFiles = collectJavaFiles( sourcePath );

            if ( !javaFiles.isEmpty() )
            {
                javaClassesInSources.addAll( javaFiles );
                return true;
            }
        }
        return false;
    }

    private Collection<String> collectJavaFiles( File directory )
    {
        List<String> classNames = new ArrayList<String>();
        collectJavaFiles( "", directory, classNames );
        return classNames;
    }

    private void collectJavaFiles( String prefix, File directory, Collection<String> classNames )
    {
        String[] names = directory.list();
        if ( names != null )
        {
            for ( String name : names )
            {
                File file = new File( directory, name );
                if ( file.isFile() )
                {
                    if ( name.endsWith( ".java" ) )
                    {
                        classNames.add( prefix.concat( name.substring( 0, name.length() - 5 ) ) );
                    }
                }
                else if ( file.isDirectory() )
                {
                    collectJavaFiles( prefix + name + '.', file, classNames );
                }
            }
        }
    }

}
