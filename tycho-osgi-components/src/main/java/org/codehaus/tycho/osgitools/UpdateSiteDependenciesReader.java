package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.FeatureResolutionState;
import org.codehaus.tycho.ProjectType;
import org.codehaus.tycho.TychoSession;
import org.codehaus.tycho.maven.DependenciesReader;
import org.codehaus.tycho.model.UpdateSite;
import org.codehaus.tycho.osgitools.features.FeatureDescription;

@Component( role = DependenciesReader.class, hint = ProjectType.ECLIPSE_UPDATE_SITE )
public class UpdateSiteDependenciesReader extends AbstractDependenciesReader {

    public List<Dependency> getDependencies(MavenProject project, TychoSession session) throws MavenExecutionException {
        FeatureResolutionState state = session.getFeatureResolutionState( project );

		try {
			File siteXml = new File(project.getBasedir(), "site.xml");
			UpdateSite site = UpdateSite.read(siteXml);

			Set<Dependency> result = new LinkedHashSet<Dependency>();
			for (UpdateSite.FeatureRef featureRef : site.getFeatures()) {
				if (null == featureRef) {
					getLogger().warn("unexpected null featureRef in project at " + siteXml.getPath());
					continue;
				}
				String id = featureRef.getId();
				String version = featureRef.getVersion();
				if (null == id || null == version) {
					getLogger().warn("Bad site feature id=" + id + " version=" + version + " for " + siteXml.getPath());
					continue;
				}
				FeatureDescription feature = state.getFeature(id, version);
				if (null == feature) {
					getLogger().warn("No OSGI feature for id=" + id + " version=" + version + " for " + siteXml.getPath());
					continue;
				}
				MavenProject mavenProject = session.getMavenProject(feature.getLocation());
				if (null == mavenProject) {
					getLogger().warn("No maven feature project for id=" + id + " version=" + version + " for " + siteXml.getPath());
					continue;
				}
				Dependency dependency = newProjectDependency(mavenProject);
				if (dependency != null) {
					result.add(dependency);
				}
			}

			return new ArrayList<Dependency>(result);
		} catch (Exception e) {
			String m = e.getMessage();
			if (null == m) {
				m = e.getClass().getName();
			}
			MavenExecutionException me = new MavenExecutionException(m, project.getFile());
			me.initCause(e);
			throw me;
		}
	}
}
