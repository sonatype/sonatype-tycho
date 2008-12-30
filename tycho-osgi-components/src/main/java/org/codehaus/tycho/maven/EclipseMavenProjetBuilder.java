package org.codehaus.tycho.maven;

import java.io.IOException;

import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.codehaus.tycho.osgitools.GroupMapper;
import org.codehaus.tycho.osgitools.OsgiState;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class EclipseMavenProjetBuilder extends DefaultMavenProjectBuilder {

	@Deprecated
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

		// if (name.startsWith("org.eclipse")) {
		// return "org.eclipse";
		// } else if (name.startsWith("com.ibm.icu")) {
		// return "com.ibm";
		// }
		// return name;
	}

}
