package org.codehaus.tycho;

import java.io.File;
import java.util.List;
import java.util.jar.Manifest;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.osgi.framework.BundleException;

public interface BundleResolutionState
{
    public BundleDescription getBundleByLocation( File location );

    public BundleDescription getBundle( String symbolicName, String version );

    public void assertResolved( BundleDescription desc )
        throws BundleException;

    public BundleDescription getSystemBundle();

    public List<BundleDescription> getDependencies( BundleDescription bundle );

    public StateHelper getStateHelper();

    public List<BundleDescription> getBundles();

    public Manifest loadManifest( File bundleLocation );

    public String getManifestAttribute( BundleDescription bundle, String name );
}
