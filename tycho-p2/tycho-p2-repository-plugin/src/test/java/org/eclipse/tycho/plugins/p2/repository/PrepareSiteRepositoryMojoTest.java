package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;
import java.io.FileInputStream;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PrepareSiteRepositoryMojoTest extends AbstractTychoMojoTestCase {

    private PrepareSiteRepositoryMojo mojo;

    @Rule
    private TemporaryFolder tmp = new TemporaryFolder();

    private File outputDirectory;

    private MavenProject project;

    private Build build;

    private File repositoryDirectory;

    @Before
    public void setUp() throws Exception {
        mojo = new PrepareSiteRepositoryMojo();
        outputDirectory = new File(tmp.getRoot(), "site");
        setVariableValueToObject(mojo, "outputDirectory", outputDirectory);

        project = new MavenProject();
        build = new Build();
        project.setBuild(build);
        repositoryDirectory = new File(getClass().getResource("/").getFile());
        build.setDirectory(repositoryDirectory.getAbsolutePath());
        setVariableValueToObject(mojo, "project", project);
    }

    @Test
    public void testExecuteCreatesInputDirectory() throws Exception {
        mojo.execute();

        assertTrue("failed to create site directory", outputDirectory.exists());
        assertTrue("created site directory as file", outputDirectory.isDirectory());
    }

    @Test
    public void testExecuteCopiesRepositoryFilesToInputDirectory() throws Exception {
        mojo.execute();

        File testFile = new File(outputDirectory, "test.txt");
        assertTrue(testFile.exists());
        assertEquals("TEST", IOUtil.toString(new FileInputStream(testFile)));
    }

}
