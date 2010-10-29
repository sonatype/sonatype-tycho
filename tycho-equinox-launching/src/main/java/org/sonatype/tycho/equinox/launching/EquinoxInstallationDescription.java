package org.sonatype.tycho.equinox.launching;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Version;
import org.sonatype.tycho.ArtifactDescriptor;
import org.sonatype.tycho.ArtifactKey;

public interface EquinoxInstallationDescription
{
    public static final Version EQUINOX_VERSION_3_3_0 = Version.parseVersion( "3.3.0" );

    public static final String EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher";

    // introspection

    public List<ArtifactDescriptor> getBundles();

    public ArtifactDescriptor getSystemBundle();

    public ArtifactDescriptor getBundle( String symbolicName, String highestVersion );

    public List<File> getFrameworkExtensions();

    public Set<String> getBundlesToExplode();

    public Map<String, BundleStartLevel> getBundleStartLevel();

    // mutators

    public void addBundle( ArtifactKey key, File basedir );

    public void addBundle( ArtifactKey key, File basedir, boolean override );

    public void addBundle( ArtifactDescriptor artifact );

    /**
     * This one is kinda odd, it reads bundle manifest to extract ArtifactKey.
     */
    // public void addBundle( File file, boolean override );

    public void addFrameworkExtensions( List<File> frameworkExtensions );

    public void addBundlesToExplode( List<String> bundlesToExplode );

    public void addBundleStartLevel( BundleStartLevel level );

}
