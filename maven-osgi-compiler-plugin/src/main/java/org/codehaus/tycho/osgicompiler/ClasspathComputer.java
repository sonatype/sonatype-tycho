package org.codehaus.tycho.osgicompiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.osgitools.DependencyComputer;
import org.codehaus.tycho.osgitools.project.BuildOutputJar;
import org.codehaus.tycho.osgitools.project.EclipsePluginProject;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class ClasspathComputer {
	public static final String ACCESS_RULE_SEPARATOR = File.pathSeparator;

//	public static final String INCLUDE_ALL_RULE = "**/*";
	public static final String EXCLUDE_ALL_RULE = "-**/*";

	//@Required
	private final BundleResolutionState bundleResolutionState;

	//@Required
	private final DependencyComputer dependencyComputer;
	
	private final File storage;

	private final MavenProject project;
	private final BundleDescription bundle;

	private final Map<File, MavenProject> sessionProjects = new HashMap<File, MavenProject>();

	public ClasspathComputer(MavenSession session, DependencyComputer dependencyComputer, MavenProject project, File storage) {
		this.bundleResolutionState = (BundleResolutionState) project.getContextValue( TychoConstants.CTX_BUNDLE_RESOLUTION_STATE );
		this.dependencyComputer = dependencyComputer;
		this.project = project;
		this.storage = storage;

		this.bundle = bundleResolutionState.getBundleByLocation( project.getBasedir() );

		for ( MavenProject sessionProject : session.getProjects() )
		{
		    sessionProjects.put( sessionProject.getBasedir(), sessionProject );
		}
	}

	public void addOutputDirectory(File outputDirectory) {
		// TODO Auto-generated method stub
		
	}

	public List<String> computeClasspath() {
		Set<String> classpath = new LinkedHashSet<String>();

		// this project's entries first
		classpath.addAll(getProjectEntries(bundle, project));

		// dependencies
		for (DependencyComputer.DependencyEntry entry : dependencyComputer.computeDependencies(bundleResolutionState, bundle)) {
			addBundle(classpath, entry);
		}

		return new ArrayList<String>(classpath);
	}

	private void addBundle(Set<String> classpath, DependencyComputer.DependencyEntry dependency) {
		MavenProject project = sessionProjects.get( new File( dependency.desc.getLocation() ) );
		List<String> entries;
		if (project != null) {
			entries = getProjectEntries(dependency.desc, project);
		} else {
			entries = getBundleEntries(dependency.desc);
		}

		StringBuilder rules = new StringBuilder(); // include all
		if (dependency.rules != null) {
			rules.append("[");
			for (DependencyComputer.AccessRule rule : dependency.rules) {
				if (rules.length() > 1) rules.append(ACCESS_RULE_SEPARATOR);
				rules.append(rule.discouraged? "~": "+");
				rules.append(rule.path);
			}
			if (rules.length() > 1) rules.append(ACCESS_RULE_SEPARATOR);
			rules.append(EXCLUDE_ALL_RULE);
			rules.append("]");
		}

		for (String entry : entries) {
			classpath.add(entry + rules);
		}
	}

	private List<String> getBundleEntries(BundleDescription bundle) {
		ArrayList<String> classpath = new ArrayList<String>(); 

		for (String cp : getBundleClasspath(bundle)) {
			File entry;
			if (".".equals(cp)) {
				entry = new File(bundle.getLocation());
			} else {
				entry = getNestedJar(bundle, cp);
			}
			if (entry != null) { 
				classpath.add(entry.getAbsolutePath());
			}
		}

		return classpath;
	}

	private File getNestedJar(BundleDescription bundle, String cp) {
		File bundleLocation = new File(bundle.getLocation());

		if (bundleLocation.isDirectory()) {
			return new File(bundleLocation, cp);
		}

		File file = new File(storage, bundle.getName() + "_" + bundle.getVersion() + "/" + cp);

		try {
			ZipFile zip = new ZipFile(bundleLocation);
			try {
				ZipEntry ze = zip.getEntry(cp);
				if (ze != null) {
					InputStream is = zip.getInputStream(ze);
					FileUtils.copyStreamToFile(new RawInputStreamFacade(is), file);
				} else {
					// TODO log
				}
			} finally {
				zip.close();
			}
		} catch (IOException e) {
			// XXX log
			return null;
		}

		return file;
	}

	/**
	 * Returns classpath entries of this maven project. Return list does not
	 * include any project dependencies. 
	 * @param bundleDescription 
	 */
	private List<String> getProjectEntries(BundleDescription bundle, MavenProject project) {
		ArrayList<String> classpath = new ArrayList<String>(); 

		EclipsePluginProject pdeProject = (EclipsePluginProject) project.getContextValue( TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT );

		Map<String, BuildOutputJar> outputJars = pdeProject.getOutputJarMap();
		for (String cp : getBundleClasspath(bundle)) {
			if (outputJars.containsKey(cp)) {
				classpath.add(outputJars.get(cp).getOutputDirectory().getAbsolutePath());
			} else {
				File jar = new File(project.getBasedir(), cp);
				if (jar.exists()) {
					classpath.add(jar.getAbsolutePath());
				}
			}
		}

		return classpath;
	}

	private String[] getBundleClasspath(BundleDescription bundle) {
		String[] result = new String[] {"."};
		String classpath = bundleResolutionState.getManifestAttribute(bundle, Constants.BUNDLE_CLASSPATH);
		if (classpath != null) {
			ManifestElement[] classpathEntries;
			try {
				classpathEntries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, classpath);
				result = new String[classpathEntries.length];
				for (int i = 0; i < classpathEntries.length; i++) {
					result[i] = classpathEntries[i].getValue();
				}
			} catch (BundleException e) {
				// ignore
			}
		}
		return result;
	}
	
}
