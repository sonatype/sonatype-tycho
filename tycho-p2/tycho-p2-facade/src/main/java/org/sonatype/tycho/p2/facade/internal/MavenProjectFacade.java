package org.sonatype.tycho.p2.facade.internal;

import java.io.File;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.utils.SourceBundleUtils;

public class MavenProjectFacade implements IArtifactFacade {

	protected MavenProject wrappedProject;

	public MavenProjectFacade(MavenProject wrappedProject) {
		this.wrappedProject = wrappedProject;
	}

	public File getLocation() {
		return wrappedProject.getBasedir();
	}

	public String getGroupId() {
		return wrappedProject.getGroupId();
	}

	public String getArtifactId() {
		return wrappedProject.getArtifactId();
	}

	public String getVersion() {
		return wrappedProject.getVersion();
	}

	public String getPackagingType() {
		return wrappedProject.getPackaging();
	}

	public String getSourceBundleSuffix() {
		return SourceBundleUtils.getSourceBundleSuffix(wrappedProject);
	}

	public boolean hasSourceBundle() {
		// TODO this is a fragile way of checking whether we generate a source bundle
		// should we rather use MavenSession to get the actual configured mojo instance?
		for (Plugin plugin : wrappedProject.getBuildPlugins()) {
			if ("org.sonatype.tycho:maven-osgi-source-plugin".equals(plugin
					.getKey())) {
				return true;
			}
		}
		return false;
	}
	
}
