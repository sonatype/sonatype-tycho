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
package org.eclipse.tycho.test.tycho46;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Ignore;
import org.junit.Test;

public class Tycho46MapfileTestDisabled extends AbstractTychoIntegrationTest {

    private static final int BUFFER_SIZE = 1024;

    @Test
    @Ignore
    public void testCVS() throws Exception {
        Verifier verifier = getVerifier("tycho46");

        File basedir = new File(verifier.getBasedir());

        // CVS "repo" setup
        File cvsPath = new File(basedir, "CVSFolder");
        InputStream cvsZip = getClass().getResourceAsStream("/tycho46/cvs.zip");
        extract(cvsPath, cvsZip);

        // directory.txt setup
        File directory = new File(basedir, "directory.txt");
        String directoryContent = readFileToString(directory).toString();
        directoryContent = directoryContent.replace("%{path to cvs}", cvsPath.getAbsolutePath());
        writeStringToFile(directory, directoryContent);

        verifier.setAutoclean(false);
        verifier.getCliOptions().add("-Dmapfile=directory.txt");
        verifier.getCliOptions().add("-Dmaven.scm.provider.cvs.implementation=cvs_native");
        verifier.executeGoal("org.eclipse.tycho:tycho-pomgenerator-plugin:import-mapfile");
        verifier.verifyErrorFreeLog();

        File directorySrc = new File(verifier.getBasedir(), "directory.src");
        Assert.assertTrue("Must create directory.src", directorySrc.exists());

        File testFile = new File(directorySrc, "test/testFile.txt");
        Assert.assertTrue("Should 'downloaded' testFile.txt", testFile.exists());
        Assert.assertEquals("Wrong testFile.txt content", "!blank file", readFileToString(testFile).toString());
    }

    private static void extract(File destination, InputStream inputStream) throws IOException, FileNotFoundException {
        ZipInputStream zin = new ZipInputStream(inputStream);
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }

            File dest = new File(destination, entry.getName());
            dest.getParentFile().mkdirs();

            FileOutputStream fos = new FileOutputStream(dest);
            byte data[] = new byte[BUFFER_SIZE];

            BufferedOutputStream out = new BufferedOutputStream(fos, BUFFER_SIZE);
            int count;
            while ((count = zin.read(data, 0, BUFFER_SIZE)) != -1) {
                out.write(data, 0, count);
            }
            out.flush();
            out.close();
        }

        zin.close();
    }
}
