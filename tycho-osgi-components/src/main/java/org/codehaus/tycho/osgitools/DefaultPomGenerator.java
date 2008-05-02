package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.osgi.framework.Constants;

/**
 * @plexus.component role="org.codehaus.tycho.osgitools.PomGenerator"
 *                   role-hint="default"
 */
public class DefaultPomGenerator extends AbstractLogEnabled implements
		PomGenerator {

	/**
	 * 
	 * @param groupMapper
	 * @param deployedArtifacts
	 *            list of BundleDescription
	 * @param versionQualifier
	 * @param bundle
	 * @return
	 */
	public Model createBundlePom(GroupMapper groupMapper,
			List deployedArtifacts, String versionQualifier,
			BundleDescription bundle) {
		Model pom = new Model();
		File file = new File(bundle.getLocation());
		OsgiStateController state = new OsgiStateController(null);
		BundleFile b = new BundleFile(state.loadManifest(file), file);
		pom.setModelVersion("4.0.0");
		pom.setArtifactId(bundle.getSymbolicName());
		pom.setGroupId(groupMapper.getGroupId(bundle.getSymbolicName()));
		pom
				.setVersion(getVersion(bundle, versionQualifier,
						deployedArtifacts));
		pom.setName(b.getName());
		pom.setDescription(b.getDescription());
		Organization org = new Organization();
		org.setName(b.getOrganization());
		org.setUrl(b.getContactAddress());
		pom.setOrganization(org);
		License license = new License();
		license.setDistribution("repo");
		license.setName(b.getCopyright());
		pom.setLicenses(Collections.singletonList(license));

		BundleDescription[] dependencies = OsgiStateController
				.getDependentBundles(bundle);
		Artifact[] artifacts = new Artifact[dependencies.length];
		for (int i = 0; i < artifacts.length; i++) {
			Dependency d = new Dependency();
			d.setArtifactId(dependencies[i].getSymbolicName());
			d.setGroupId(groupMapper.getGroupId(dependencies[i]
					.getSymbolicName()));
			d.setVersion(getVersion(dependencies[i], versionQualifier,
					deployedArtifacts));
			d.setOptional(isOptional(bundle, dependencies[i]));
			pom.addDependency(d);

		}

		return pom;
	}

	private boolean isOptional(BundleDescription root,
			BundleDescription dependency) {

		BundleSpecification[] required = root.getRequiredBundles();
		for (int i = 0; i < required.length; i++) {
			if (required[i].isSatisfiedBy(dependency)) {
				return required[i].isOptional();
			}
		}

		ImportPackageSpecification[] imports = root.getImportPackages();
		for (int i = 0; i < imports.length; i++) {
			ImportPackageSpecification ips = imports[i];
			ExportPackageDescription[] exports = dependency.getExportPackages();
			for (int j = 0; j < exports.length; j++) {
				ExportPackageDescription epd = exports[j];
				if (ips.isSatisfiedBy(epd)) {
					return !ImportPackageSpecification.RESOLUTION_STATIC
							.equals(ips
									.getDirective(Constants.RESOLUTION_DIRECTIVE));
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * @param bd
	 * @param versionQualifier
	 * @param deployedArtifacts
	 *            list of BundleDescription
	 * @return
	 */
	private String getVersion(BundleDescription bd, String versionQualifier,
			List deployedArtifacts) {
		String version = bd.getVersion().toString();

		if (versionQualifier != null && deployedArtifacts.contains(bd)) {
			version = version + versionQualifier;
		}
		return version;
	}

}
