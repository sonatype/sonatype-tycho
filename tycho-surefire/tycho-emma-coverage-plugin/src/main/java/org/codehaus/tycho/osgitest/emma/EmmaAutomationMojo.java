package org.codehaus.tycho.osgitest.emma;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.tycho.TychoProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mojo that will try to automatically set the command line for the OSGi test runtime
 * used by the tycho OSGi test plugin.
 * <p/>
 * This mojo will only work for Maven projects with packaging type <code>eclipse-test-plugin</code>.
 * <p/>
 *
 * @phase pre-integration-test
 * @goal prepare-emma
 * @requiresProject true
 * @requiresDependencyResolution runtime
 */
public class EmmaAutomationMojo
    extends AbstractMojo
{

    /**
     * Comma-separated list of bundles to be instrumented.
     *
     * @parameter @expression="${coverage.bundlesToInstrument}"
     */
    private String bundlesToInstrument;

    /**
     * A regular expression used to match test bundlenames. If the expression matches the
     * automation mojo will try to use the {@link #nameReplacement} as name for the
     * bundle to instrument.
     * <p/>
     * For example: setting the properties to
     * <pre>
     * &lt;coverage.namePattern&gt;^([a-zA-Z_\-\.]+)\.tests(\.[a-zA-Z_\-\.]+)?$&lt;/coverage.namePattern&gt;
     * &lt;coverage.nameReplacement&gt;$1$2&lt;coverage.nameReplacement&gt;
     * </pre>
     * Will check if a bundle name contains <code>.tests.</code> as a name segment and will
     * attempt to instrument the bundle with the same name that does not have that <code>tests</code>
     * segment.
     * <p/>
     * If the test bundle name is <code>org.eclipse.foo.tests.bar</code> then
     * <code>org.eclipse.foo.bar</code> will be used as bundle to instrument.
     *
     * @parameter @expression="${coverage.namePattern}"
     */
    private String namePattern;

    /**
     * @see #namePattern
     * @parameter @expression="${coverage.nameReplacement}"
     */
    private String nameReplacement;

    /**
     * Comma-separated list of *,?-wildcard class name patterns.
     *
     * @parameter @expression="${coverage.instrumentationFilter}"
     */
    private String instrumentationFilter = null;

    /**
     * @parameter @expression="${coverage.disableInstrumentationFilter}"
     */
    private boolean disableInstrumentationFilter = false;

    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * Name of the property used in maven-osgi-test-plugin.
     * <p/>
     * Must match the expression for {@link org.codehaus.tycho.osgitest.TestMojo#argLine}
     */
    private static final String TYCHO_ARG_LINE = "tycho.testArgLine";

    private Attributes mainAttributes;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // only execute if this is invoked on a test plugin.
        if ( !TychoProject.ECLIPSE_TEST_PLUGIN.equals( project.getPackaging() ) )
        {
            return;
        }

        readManifest();

        StringBuilder argLine = new StringBuilder( 200 );

        // set up parts of the command line independently. Even if we don't
        // find a bundle to instrument there might still be value in the
        // class filter?
        setupOutputFile( argLine );

        setupBundlesToInstrument( argLine );

        setupInstrumentationFilter( argLine );

        setCommandLineForTestPlugin( argLine.toString() );
    }

    private void readManifest()
        throws MojoExecutionException
    {
        File mfFile = new File( project.getBasedir(), "META-INF/MANIFEST.MF" );
        if ( mfFile.exists() )
        {
            InputStream is;
            try
            {
                is = new FileInputStream( mfFile );
            }
            catch ( FileNotFoundException e )
            {
                throw new MojoExecutionException( "Failed to read existing manifest file", e );
            }
            Manifest mf;
            try
            {
                mf = new Manifest();
                mf.read( is );
                mainAttributes = mf.getMainAttributes();
            }
            catch ( IOException e )
            {
                getLog().warn( "Failed to read manifest file " + mfFile, e );
            }
            finally
            {
                try
                {
                    is.close();
                }
                catch ( IOException e )
                {
                    getLog().debug( "Failed to close InputStream for " + mfFile, e );
                }
            }
        }
    }

    private void setCommandLineForTestPlugin( String line )
    {
        // append or set what we have
        Properties projectProperties = project.getProperties();
        String s = projectProperties.getProperty( TYCHO_ARG_LINE );
        projectProperties.put( TYCHO_ARG_LINE, s == null ? line : line + ' ' + s );
    }

    private void setupOutputFile( StringBuilder argLine )
        throws MojoExecutionException
    {
        File resultFile = new File( project.getBuild().getDirectory(), "coverage.es" );
        String coverageSession = resultFile.getAbsolutePath();

        try
        {
            coverageSession = CommandLineUtils.quote( coverageSession );
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Failure to use path", e );
        }
        argLine.append( " -Demma.session.out.file=" ).append( coverageSession );
    }

    private void setupBundlesToInstrument( StringBuilder argLine )
    {
        // check which bundles to instrument
        if ( bundlesToInstrument == null )
        {
            // use fragment host as default
            bundlesToInstrument = getFragmentHost();
        }
        if ( bundlesToInstrument == null )
        {
            bundlesToInstrument = checkNameConvention();
        }

        if ( bundlesToInstrument == null )
        {
            getLog().warn( "Could not determine which bundle to instrument. Check configuration." );
        }
        else
        {
            argLine.append( " -Declemma.instrument.bundles=" ).append( bundlesToInstrument );
        }
    }

    /**
     * Gets the symbolic name of the fragment host from the OSGi manifest or
     * <code>null</code> if no host is defined.
     *
     * @return the the symbolic name of the fragment host from the OSGi manifest or
     *         <code>null</code> if no host is defined.
     */
    private String getFragmentHost()
    {
        return getSymbolicName( "Fragment-Host" );
    }

    private String getSymbolicName( String s )
    {
        if ( mainAttributes == null )
        {
            return null;
        }
        String name = mainAttributes.getValue( s );
        // cut off everything after the first semi-colon so that we lose the version
        int n = name.indexOf( ';' );
        if ( n >= 0 )
        {
            name = name.substring( 0, n );
        }
        return name;
    }

    private String checkNameConvention()
    {
        String pattern = namePattern;
        String replacement = nameReplacement;
        String myName = getSymbolicName( "Bundle-SymbolicName" );
        if ( ( pattern != null ) && ( replacement != null ) && ( myName != null ) )
        {
            NameConventionResolver resolver = new RegexpNameConventionResolver( pattern, replacement );

            Set<String> candidates = resolver.candidates( myName );
            if ( !candidates.isEmpty() )
            {
                return toCommaSeparatedList( candidates );
            }
        }
        return null;
    }

    private void setupInstrumentationFilter( StringBuilder argLine )
    {
        // only initialize if not set by plexus container.
        if ( instrumentationFilter == null )
        {
            instrumentationFilter = initInstrumentationFilter();
        }
        if ( instrumentationFilter != null )
        {
            argLine.append( " -Demma.filter=" ).append( instrumentationFilter );
        }
    }

    /**
     * Calculate the value of {@link #instrumentationFilter}.
     *
     * @return the instrumentation filter or <code>null</code> if nothing should
     *         be filtered.
     */
    private String initInstrumentationFilter()
    {
        // check if "disableInstrumentationFilter" has been set (if yes, emma.filter
        // will not be set and everything is instrumented)
        if ( disableInstrumentationFilter )
        {
            return null;
        }
        // no configuration options set -> find all Java source files from
        // the current (test) project and exclude them from instrumentation
        ClassNameCollector finder = new ClassNameCollector( project );
        Set<String> classesToExclude = finder.collectJavaClassNames();
        // set instrumentationFilter only if at least one Java class has
        // been found
        List<String> excludes = new ArrayList<String>();
        if ( classesToExclude.size() > 0 )
        {
            for ( String clazz : classesToExclude )
            {
                // append "*" to class name to include inner classes in
                // exclusion filter
                excludes.add( '-' + clazz + '*' );
            }
        }
        // mw 2010-03-24: for some reason we get coverage reported for
        // package "org.eclemma.runtime.equinox.internal". So just hardcode
        // an exclusion here...
        excludes.add( "-org.eclemma.runtime.equinox.internal.*" );
        return toCommaSeparatedList( excludes );
    }

    private static String toCommaSeparatedList( Collection<String> strings )
    {
        // count required length. Usually better than having the StringBuilder
        // copy memory.
        int n = -1;
        for ( String s : strings )
        {
            n += s.length();
            n++;
        }
        StringBuilder temp = new StringBuilder( n );
        boolean first = true;
        for ( String s : strings )
        {
            if ( first )
            {
                first = false;
            }
            else
            {
                temp.append( ',' );
            }
            temp.append( s );
        }
        return temp.toString();
    }

    private static interface NameConventionResolver
    {
        public Set<String> candidates( String name );
    }

    private static class RegexpNameConventionResolver
        implements NameConventionResolver
    {
        private final Pattern pattern;

        private final String replacement;

        public RegexpNameConventionResolver( String pattern, String replacement )
        {
            this.pattern = Pattern.compile( pattern );
            this.replacement = replacement;
        }

        public Set<String> candidates( String name )
        {
            Set<String> result = new HashSet<String>();
            Matcher matcher = pattern.matcher( name );
            if ( matcher.matches() )
            {
                String s = matcher.replaceAll( replacement );
                if ( ( s != null ) && ( s.length() > 0 ) )
                {
                    result.add( s );
                }
            }
            return result;
        }
    }
}
