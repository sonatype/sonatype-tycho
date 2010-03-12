package org.codehaus.tycho.osgitest.test;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import junit.framework.Assert;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.tycho.osgitest.TestFramework;
import org.codehaus.tycho.osgitools.EquinoxBundleResolutionState;
import org.codehaus.tycho.utils.ExecutionEnvironmentUtils;
import org.codehaus.tycho.utils.PlatformPropertiesUtils;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleException;

public class TestFrameworkTest
    extends PlexusTestCase
{

    public void testRequireBundle()
        throws Exception
    {
        EquinoxBundleResolutionState state = newResolutionState();

        BundleDescription junit_v3 =
            bundleRequireBundle( state, "junit_v3", "org.junit;bundle-version=\"[3.0.0,4.0.0)\"" );

        BundleDescription junit_v4 = bundleRequireBundle( state, "junit_v4", "org.junit;bundle-version=\"4.0.0\"" );

        BundleDescription junit4 = bundleRequireBundle( state, "junit4", "org.junit4" );

        BundleDescription junit_and_junit4 =
            bundleRequireBundle( state, "junit_and_junit4",
                                 "org.junit;bundle-version=\"[3.0.0,4.0.0)\",org.junit4;bundle-version=\"4.0.0\"" );

        Properties properties = getFrameworkProperties();

        state.resolve( properties );
        state.assertResolved( junit_v3 );
        state.assertResolved( junit4 );
        state.assertResolved( junit_and_junit4 );

        Assert.assertEquals( TestFramework.TEST_JUNIT, new TestFramework().getTestFramework( state, junit_v3 ) );
        Assert.assertEquals( TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework( state, junit4 ) );
        Assert.assertEquals( TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework( state, junit_v4 ) );
        Assert.assertEquals( TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework( state, junit_and_junit4 ) );
    }

    public void testReprovide()
        throws Exception
    {
        EquinoxBundleResolutionState state = newResolutionState();
        bundleRequireBundle( state, "export_junit_v3", "org.junit;bundle-version=\"[3.0.0,4.0.0)\";reprovide=true" );
        bundleRequireBundle( state, "export_junit_v4", "org.junit;bundle-version=\"4.0.0\";reprovide=true" );
        bundleRequireBundle( state, "export_junit4", "org.junit4;reprovide=true" );

        BundleDescription junit_v3 = bundleRequireBundle( state, "junit_v3", "export_junit_v3" );

        BundleDescription junit_v4 = bundleRequireBundle( state, "junit_v4", "export_junit_v4" );

        BundleDescription junit4 = bundleRequireBundle( state, "junit4", "export_junit4" );

        BundleDescription junit_and_junit4 =
            bundleRequireBundle( state, "junit_and_junit4", "export_junit_v3,export_junit4" );

        Properties properties = getFrameworkProperties();

        state.resolve( properties );
        state.assertResolved( junit_v3 );
        state.assertResolved( junit4 );
        state.assertResolved( junit_and_junit4 );

        Assert.assertEquals( TestFramework.TEST_JUNIT, new TestFramework().getTestFramework( state, junit_v3 ) );
        Assert.assertEquals( TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework( state, junit4 ) );
        Assert.assertEquals( TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework( state, junit_v4 ) );
        Assert.assertEquals( TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework( state, junit_and_junit4 ) );
    }

    public void testImportPackage()
        throws Exception
    {
        EquinoxBundleResolutionState state = newResolutionState();

        BundleDescription junit_v3 =
            bundleImportPackage( state, "junit_v3", "junit.framework;specification-version=\"[3.0.0,4.0.0)\"" );

        BundleDescription junit_v4 =
            bundleImportPackage( state, "junit_v4", "junit.framework;specification-version=\"4.0.0\"" );

        Properties properties = getFrameworkProperties();

        state.resolve( properties );
        state.assertResolved( junit_v3 );
        state.assertResolved( junit_v4 );

        Assert.assertEquals( TestFramework.TEST_JUNIT, new TestFramework().getTestFramework( state, junit_v3 ) );
        Assert.assertEquals( TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework( state, junit_v4 ) );
    }

    protected Properties getFrameworkProperties()
    {
        Properties properties = new Properties();

        properties.put( PlatformPropertiesUtils.OSGI_OS, PlatformPropertiesUtils.getOS( properties ) );
        properties.put( PlatformPropertiesUtils.OSGI_WS, PlatformPropertiesUtils.getWS( properties ) );
        properties.put( PlatformPropertiesUtils.OSGI_ARCH, PlatformPropertiesUtils.getArch( properties ) );

        ExecutionEnvironmentUtils.loadVMProfile( properties );
        return properties;
    }

    protected EquinoxBundleResolutionState newResolutionState()
        throws BundleException
    {
        EquinoxBundleResolutionState state =
            EquinoxBundleResolutionState.newInstance( getContainer(), new File( "target/manifests" ) );
        state.addBundle( new File( "src/test/resources/org.junit_3.8.2.v20090203-1005" ), false );
        state.addBundle( new File( "src/test/resources/org.junit_4.8.1.v4_8_1_v20100114-1600" ), false );
        state.addBundle( new File( "src/test/resources/org.junit4_4.7.0.v20100104" ), false );
        return state;
    }

    private BundleDescription bundleRequireBundle( EquinoxBundleResolutionState state, String id, String requireBundle )
        throws BundleException
    {
        return addBundle( state, id, "Require-Bundle", requireBundle );
    }

    private BundleDescription bundleImportPackage( EquinoxBundleResolutionState state, String id, String importPackage )
        throws BundleException
    {
        return addBundle( state, id, "Import-Package", importPackage );
    }

    private BundleDescription addBundle( EquinoxBundleResolutionState state, String id, String... attrs )
        throws BundleException
    {
        Dictionary<String, String> mf = new Hashtable<String, String>();
        mf.put( "Bundle-SymbolicName", id );
        for ( int i = 0; i < attrs.length - 1; i += 2 )
        {
            mf.put( attrs[i], attrs[i + 1] );
        }

        return state.addBundle( mf, new File( id ), false );
    }

}
