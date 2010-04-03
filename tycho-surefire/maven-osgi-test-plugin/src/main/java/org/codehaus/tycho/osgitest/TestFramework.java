package org.codehaus.tycho.osgitest;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.osgitools.DependencyComputer;
import org.codehaus.tycho.osgitools.DependencyComputer.DependencyEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class TestFramework
{

    public static final String TEST_JUNIT = "org.junit";

    public static final String TEST_JUNIT4 = "org.junit4";

    public String getTestFramework( BundleResolutionState bundleResolutionState, BundleDescription bundle )
        throws MojoExecutionException
    {
        List<DependencyEntry> dependencies =
            new DependencyComputer().computeDependencies( bundleResolutionState, bundle );
        String result = null;
        for ( DependencyEntry dependencyEntry : dependencies )
        {
            String testFramework = getTestFramework( dependencyEntry.desc, result );
            if ( testFramework != null )
            {
                result = testFramework;
            }
        }
        if ( result != null )
        {
            return result;
        }
        throw new MojoExecutionException( "Could not determine test framework used by test bundle " + bundle.toString() );
    }

    private static String getTestFramework( BundleDescription bundleDesc, String currentTestFramework )
    {
        String symbolicName = bundleDesc.getSymbolicName();
        if ( TestFramework.TEST_JUNIT.equals( symbolicName ) )
        {
            if ( bundleDesc.getVersion().getMajor() < 4 )
            {
                // junit4 has precedence over junit3 if both are present
                if ( !TestFramework.TEST_JUNIT4.equals( currentTestFramework ) )
                {
                    return TestFramework.TEST_JUNIT;
                }
            }
            else
            {
                return TestFramework.TEST_JUNIT4;
            }
        }
        else if ( TestFramework.TEST_JUNIT4.equals( symbolicName ) )
        {
            return TestFramework.TEST_JUNIT4;
        }
        return null;
    }

}
