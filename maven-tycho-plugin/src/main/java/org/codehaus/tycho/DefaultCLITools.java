package org.codehaus.tycho;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * @plexus.component role="org.codehaus.tycho.CLITools"
 *                   role-hint="default"
 * @author tom
 */
public class DefaultCLITools implements Contextualizable, CLITools
{

	static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern
			.compile("(.+)::(.+)::(.+)");

	private PlexusContainer container;

	/**
	 * @plexus.requirement
	 */
	private ArtifactFactory artifactFactory;

	public List createRemoteRepositories(String repositories)
			throws TychoException
	{
		String[] split = repositories.split(",");
		List result = new ArrayList(split.length);
		for (int i = 0; i < split.length; i++) {
			result.add(createRemoteRepository(split[i]));
			
		}
		return result;
	}

	public ArtifactRepository createRemoteRepository(String repository)
			throws TychoException
	{
		Matcher matcher = ALT_REPO_SYNTAX_PATTERN.matcher(repository);

		if (!matcher.matches())
		{
			throw new TychoException(
					"Invalid syntax for repository. Use \"id::layout::url\".");
		} else
		{
			String id = matcher.group(1).trim();
			String layout = matcher.group(2).trim();
			String url = matcher.group(3).trim();

			ArtifactRepositoryLayout repoLayout;
			try
			{
				repoLayout = (ArtifactRepositoryLayout) container.lookup(
						ArtifactRepositoryLayout.ROLE, layout);
			}
			catch (ComponentLookupException e)
			{
				throw new TychoException(
						"Cannot find repository layout: " + layout, e);
			}

			ArtifactRepository repo = new DefaultArtifactRepository(id, url,
					repoLayout);

			return repo;
		}

	}


	public ArtifactRepository createLocalRepository(File location)
	{
		try
		{
			ArtifactRepositoryLayout layout = new DefaultRepositoryLayout();
			ArtifactRepository local = new DefaultArtifactRepository("local", location.toURI().toURL().toString(), layout);
			return local;
		}
		catch (MalformedURLException e)
		{
			// shouldn't happen
			throw new IllegalArgumentException("File " + location + " could not be converted to a URL");
		}
	}

	public static final Pattern ARTIFACT_PATTERN = Pattern
			.compile("(.*):(.*):(.*)");

	public Artifact createArtifact(String id, String type)
			throws TychoException
	{
		Matcher m = ARTIFACT_PATTERN.matcher(id);
		if (!m.matches())
		{
			throw new TychoException(
					"Paramater artifact does not match groupId:artifactId:version");
		}

		String groupId = m.group(1);
		String artifactId = m.group(2);
		String version = m.group(3);

		Artifact result = artifactFactory.createArtifactWithClassifier(groupId,
				artifactId, version, type, null);

		return result;
	}

	public void contextualize(Context context) throws ContextException
	{
		this.container = (PlexusContainer) context
				.get(PlexusConstants.PLEXUS_KEY);
	}

}
