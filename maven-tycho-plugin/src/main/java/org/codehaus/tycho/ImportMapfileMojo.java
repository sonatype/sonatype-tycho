package org.codehaus.tycho;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.tycho.mapfile.MapEntry;
import org.codehaus.tycho.mapfile.MapfileUtils;

/**
 * @goal import-mapfile
 * @requiresProject false
 * @author marvin
 * 
 */
public class ImportMapfileMojo extends AbstractMojo implements Contextualizable {

	private PlexusContainer plexus;

	/**
	 * @parameter expression="${mapfile}"
	 * @required
	 */
	private File mapfile;

	/**
	 * @parameter expression="${scmSystem}" default-value="cvs"
	 * @required
	 */
	private String scmSystem;

	private File baseFolder;

	private ScmManager scmManager;

	public void contextualize(Context ctx) throws ContextException {
		plexus = (PlexusContainer) ctx.get(PlexusConstants.PLEXUS_KEY);
	}

	@SuppressWarnings("unchecked")
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (mapfile == null) {
			throw new MojoExecutionException("Mapfile not defined!");
		}
		if (!mapfile.exists()) {
			throw new MojoExecutionException("Mapfile not found "
					+ mapfile.getAbsolutePath());
		}

		try {
			scmManager = (ScmManager) plexus.lookup(ScmManager.ROLE);
		} catch (ComponentLookupException e) {
			throw new MojoFailureException("Unable to access scm manager", e);
		}

		baseFolder = new File(mapfile.getParentFile(), FilenameUtils
				.getBaseName(mapfile.getName())
				+ ".src");

		List<String> lines;
		try {
			lines = FileUtils.readLines(mapfile);
		} catch (IOException e) {
			throw new MojoExecutionException("Error reading mapfile", e);
		}

		for (String line : lines) {
			MapEntry entry = MapfileUtils.parse(line);
			if (entry == null) {
				continue;
			}
			try {
				checkout(entry);
			} catch (ScmException e) {
				throw new MojoExecutionException("Error fetching SCM ", e);
			}
		}
	}

	private void checkout(MapEntry entry) throws ScmException,
			MojoExecutionException {
		String scmModule = entry.getScmPath();
		if (scmModule == null) {
			scmModule = entry.getName();
		}

		String childName;
		if (scmModule.contains("/")) {
			childName = scmModule.substring(scmModule.lastIndexOf('/') + 1);
		} else {
			childName = scmModule;
		}

		File workingDirectory = new File(baseFolder, childName);

		ScmRepository scmRepository = scmManager.makeScmRepository("scm:"
				+ scmSystem + entry.getScmUrl() + ":" + scmModule);

		ScmVersion version = new ScmTag(entry.getVersion());

		ScmResult result;
		if (workingDirectory.exists()) {
			result = scmManager.update(scmRepository, new ScmFileSet(
					workingDirectory), version);
		} else if (workingDirectory.mkdirs()) {
			result = scmManager.checkOut(scmRepository, new ScmFileSet(
					workingDirectory), version);
		} else {
			throw new MojoExecutionException(
					"Unable to create output folder for "
							+ workingDirectory.getAbsolutePath());
		}

		if (!result.isSuccess()) {
			throw new MojoExecutionException("Command failed."
					+ StringUtils.defaultString(result.getProviderMessage()));
		}

	}

}
