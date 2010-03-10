package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.Properties;
import java.util.jar.Manifest;

import org.eclipse.osgi.util.ManifestElement;

// TODO cleanup and rework to consistently use equinox implementation
public interface BundleManifestReader
{

    Manifest loadManifest( File bundleLocation );

    Properties toProperties( Manifest mf );

    /**
     * returns null if header is not present in the manifest
     */
    ManifestElement[] parseHeader( String header, Manifest mf );

    /**
     * Returns true if Eclipse-BundleShape header is set to dir.
     * 
     * http://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/bundle_manifest.html
     * 
     * http://eclipsesource.com/blogs/2009/01/20/tip-eclipse-bundleshape/
     * 
     * TODO this method does not belong here
     */
    boolean isDirectoryShape( Manifest mf );

}
