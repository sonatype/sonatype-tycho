package org.codehaus.tycho.maven;

import java.io.IOException;

import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.codehaus.tycho.osgitools.GroupMapper;
import org.codehaus.tycho.osgitools.OsgiState;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class EclipseMavenProjetBuilder extends DefaultMavenProjectBuilder {

	private OsgiState osgiState;

//	@Override
//	protected Model readModel(String projectId, File file, boolean strict) throws ProjectBuildingException {
//		Model model = super.readModel(projectId, file, strict);
//		File mf = new File(file.getParentFile(), "META-INF/MANIFEST.MF");
//		if (mf.canRead()) {
//			try {
//				BundleDescription bundleDescription = osgiState.addBundle(file.getParentFile());
//
//				model.setGroupId(getGroupId(bundleDescription));
//				model.setArtifactId(bundleDescription.getSymbolicName());
//				model.setVersion(bundleDescription.getVersion().toString());
//				model.setPackaging("osgi-bundle");
//
//			} catch (BundleException e) {
//				throw new ProjectBuildingException(file.getAbsolutePath(), "Can't read OSGi Bundle manifest");
//			}
//		}
//		return model;
//	}

	public static String getGroupId(OsgiState osgiState, BundleDescription desc) {

		String groupId = osgiState.getGroupId(desc);
		
		if (groupId == null) {

			String name = desc.getSymbolicName();
	
			GroupMapper mapper;
			try {
				mapper = new GroupMapper(null);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			groupId = mapper.getGroupId(name);
		}

		return groupId;
		
//		if (name.startsWith("org.eclipse")) {
//			return "org.eclipse";
//		} else if (name.startsWith("com.ibm.icu")) {
//			return "com.ibm";
//		}
//		return name;
	}

//	@Override
//	protected Artifact createProjectArtifact(MavenProject project, File projectDir) throws ProjectBuildingException {
//		File mf = new File(projectDir, "META-INF/MANIFEST.MF");
//		if (mf.canRead()) {
//			try {
//				Manifest m = new Manifest(new FileInputStream(mf));
//				Attributes attributes = m.getMainAttributes();
//				project.setGroupId(getValue(attributes.getValue("Bundle-SymbolicName")));
//				project.setArtifactId(getValue(attributes.getValue("Bundle-SymbolicName")));
//				project.setVersion(attributes.getValue("Bundle-Version"));
//				project.setPackaging("eclipse-plugin");
//			} catch (IOException e) {
//				throw new ProjectBuildingException(projectDir.getAbsolutePath(), "Can't read OSGi Bundle manifest");
//			}
//		}
//		return super.createProjectArtifact(project, projectDir);
//	}

	
}
