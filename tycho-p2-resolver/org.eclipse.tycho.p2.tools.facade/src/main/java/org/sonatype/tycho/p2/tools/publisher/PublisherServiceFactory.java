package org.sonatype.tycho.p2.tools.publisher;

import java.io.File;

import org.sonatype.tycho.p2.tools.BuildContext;
import org.sonatype.tycho.p2.tools.FacadeException;
import org.sonatype.tycho.p2.tools.RepositoryReferences;

public interface PublisherServiceFactory
{
    /**
     * Creates a {@link PublisherService} instance that can be used to publish artifacts. The
     * results are stored as metadata and artifacts repository at the given location.
     * 
     * @param targetRepository The location of the output repository; if the output repository
     *            exists, new content will be appended
     * @param contextRepositories Context metadata repositories that may be consulted by the
     *            publishers; note that artifact repository references in the argument are ignored
     * @param context Context information about the current build
     * @return A new {@link PublisherService} instance. The caller is responsible to call
     *         <tt>stop</tt> on the instance after use
     * @throws FacadeException if a checked exception occurs internally
     */
    PublisherService createPublisher( File targetRepository, RepositoryReferences contextRepositories,
                                      BuildContext context )
        throws FacadeException;
}
