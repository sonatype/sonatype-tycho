/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.facade.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Parses directories for bundles
 * 
 * @author Michel Kraemer
 */
@Component(role = DirectoryBundleResolver.class, instantiationStrategy = "per-lookup")
public class DirectoryBundleResolver {
    @Requirement
    private Logger logger;

    public Collection<IArtifactFacade> resolve(File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalStateException("Directory expected");
        }

        //switch to plugins subdirectory if there is any
        File plugins = new File(dir, "plugins");
        if (plugins.exists() && plugins.isDirectory()) {
            dir = plugins;
        }

        Collection<IArtifactFacade> result = new ArrayList<IArtifactFacade>();
        File[] files = dir.listFiles();
        for (File file : files) {
            IArtifactFacade af = resolveFile(file);
            if (af != null) {
                result.add(af);
            }
        }

        return result;
    }

    private IArtifactFacade resolveFile(File file) {
        logger.debug("Resolving bundle: " + file);

        ZipFile jarFile = null;
        InputStream manifestStream = null;
        try {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                jarFile = new ZipFile(file, ZipFile.OPEN_READ);
                ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
                if (manifestEntry == null) {
                    //jar file is not a bundle. ignore it silently
                    return null;
                }
                manifestStream = jarFile.getInputStream(manifestEntry);
            } else if (file.isDirectory()) {
                File manifestFile = new File(file, JarFile.MANIFEST_NAME);
                if (!manifestFile.exists()) {
                    //directory does not contain a bundle. ignore it silently
                    return null;
                }
                manifestStream = new FileInputStream(manifestFile);
            } else {
                //unsupported file. ignore it silently
                return null;
            }

            HashMap<String, String> map = new HashMap<String, String>();
            ManifestElement.parseBundleManifest(manifestStream, map);

            String bsn = map.get(Constants.BUNDLE_SYMBOLICNAME);
            if (bsn == null) {
                //manifest does not contain a symbolic name. jar is no bundle. ignore it silently
                return null;
            }
            String version = map.get(Constants.BUNDLE_VERSION);
            if (version == null) {
                version = "0.0.0";
            }
            return new DirectoryArtifact(file, bsn, bsn, null, version);
        } catch (IOException e) {
            logger.error("Could not read manifest from bundle", e);
            return null;
        } catch (BundleException e) {
            logger.error("Could not parse bundle manifest", e);
            return null;
        } finally {
            if (manifestStream != null) {
                try {
                    manifestStream.close();
                } catch (IOException e) {
                    logger.error("Could not close manifest stream", e);
                }
            }
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    logger.error("Could not close bundle jar handle", e);
                }
            }
        }
    }

    private static class DirectoryArtifact implements IArtifactFacade {
        private final File location;
        private final String groupId;
        private final String artifactId;
        private final String classifier;
        private final String version;

        public DirectoryArtifact(File location, String groupId, String artifactId, String classifier, String version) {
            this.location = location;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.classifier = classifier;
            this.version = version;
        }

        public File getLocation() {
            return location;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getClassidier() {
            return classifier;
        }

        public String getVersion() {
            return version;
        }

        public String getPackagingType() {
            return ArtifactKey.TYPE_ECLIPSE_PLUGIN;
        }
    }
}
