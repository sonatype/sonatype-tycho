package org.sonatype.tycho.p2.tools.publisher;

import java.io.File;
import java.util.Collection;

import org.sonatype.tycho.p2.tools.FacadeException;

public interface PublisherServiceFactory
{
    /**
     * Flag to indicate the repository metadata files (content.xml, artifacts.xml) shall be
     * compressed.
     */
    public static final int REPOSITORY_COMPRESS = 1;

    /**
     * Creates a {@link PublisherService} instance that can be used to publish artifacts. The
     * results are stored as metadata and artifacts repository at the given location.
     * 
     * @param targetRepository The location of the output repository; if the output repository
     *            exists, new content will be appended
     * @param contextMetadataRepositories List of context metadata repositories that may be
     *            consulted by the publishers
     * @param contextArtifactRepositories List of context artifact repositories that may be
     *            consulted by the publishers
     * @param context Context information about the current build
     * @param flags Additional flags. The only supported flag is <tt>REPOSITORY_COMPRESS</tt>
     * @return A new {@link PublisherService} instance. The caller is responsible to call
     *         <tt>stop</tt> on the instance after use
     * @throws FacadeException if a checked exception occurs internally
     */
    PublisherService createPublisher( File targetRepository, Collection<File> contextMetadataRepositories,
                                      Collection<File> contextArtifactRepositories, BuildContext context, int flags )
        throws FacadeException;
}
