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
    ManifestElement[] parseHeader(String header, Manifest mf );
}
