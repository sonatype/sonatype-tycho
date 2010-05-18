package org.sonatype.tycho.p2.facade.internal;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.utils.SourceBundleUtils;

public class ProjectArtifactFacade extends MavenProjectFacade {

	public ProjectArtifactFacade(MavenProject wrappedProject) {
		super(wrappedProject);
	}

	/**
	 * Instead of project base dir (with <code>META-INF/MANIFEST.MF</code> in
	 * the file system) , in this case we reference the artifact jar (containing
	 * <code>META-INF/MANIFEST.MF</code>). Both are supported by the p2
	 * BundlesAction.
	 */
	@Override
	public File getLocation() {
		return wrappedProject.getArtifact().getFile();
	}

	public File getSourceArtifactLocation() {
		for (Artifact artifact : wrappedProject.getAttachedArtifacts()) {
			if (SourceBundleUtils.ARTIFACT_CLASSIFIER.equals(artifact.getClassifier())) {
				return artifact.getFile();
			}
		}
		throw new IllegalStateException(wrappedProject
				+ " does not provide any attached artifact with classifier '"
				+ SourceBundleUtils.ARTIFACT_CLASSIFIER + "'");
	}

}
