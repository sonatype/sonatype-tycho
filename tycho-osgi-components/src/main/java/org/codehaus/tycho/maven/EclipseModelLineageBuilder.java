package org.codehaus.tycho.maven;

import org.apache.maven.project.build.model.DefaultModelLineageBuilder;
import org.codehaus.tycho.osgitools.OsgiState;

public class EclipseModelLineageBuilder extends DefaultModelLineageBuilder {
	
	private OsgiState osgiState;

//	@Override
//	protected Model readModel(File pomFile) throws ProjectBuildingException {
//		Model model = super.readModel(pomFile);
//		File mf = new File(pomFile.getParentFile(), "META-INF/MANIFEST.MF");
//		if (mf.canRead()) {
//			try {
//				BundleDescription bundleDescription = osgiState.addBundle(pomFile.getParentFile());
//
//				model.setGroupId(EclipseMavenProjetBuilder.getGroupId(bundleDescription));
//				model.setArtifactId(bundleDescription.getSymbolicName());
//				model.setVersion(bundleDescription.getVersion().toString());
//				model.setPackaging("osgi-bundle");
//			} catch (BundleException e) {
//				throw new ProjectBuildingException(pomFile.getAbsolutePath(), "Can't read OSGi Bundle manifest");
//			}
//		}
//		return model;
//	}

}
