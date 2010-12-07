package org.sonatype.tycho.p2.tools.mirroring;

import java.io.File;
import java.util.Collection;

import org.sonatype.tycho.p2.tools.BuildContext;
import org.sonatype.tycho.p2.tools.FacadeException;
import org.sonatype.tycho.p2.tools.RepositoryReferences;

public interface MirrorApplicationService
{
    /**
     * Flag to indicate the repository metadata files (content.xml, artifacts.xml) of newly created
     * p2 repositories shall be compressed.
     */
    public static final int REPOSITORY_COMPRESS = 1;

    /**
     * Copies the given installable units, their dependencies with strict versions (i.e. included
     * content) and the associated artifacts into the destination p2 repository.
     * 
     * @param sources The p2 repositories from which dependencies and artifacts are copied
     * @param destination The location of the p2 repository that shall be written to. The location
     *            must be a directory, which may be empty. Existing content is not overwritten but
     *            is appended to.
     * @param rootUnits A set of installable units that span the content to be copied. Note that the
     *            given installable units are written into the destination p2 repository without
     *            checking if they are actually present in the source repositories. Therefore only
     *            units from the source repositories should be passed via this parameter.
     * @param context Build context information; in particular this parameter defines a filter for
     *            environment specific installable units
     * @param flags Additional options. The only supported flag is <tt>REPOSITORY_COMPRESS</tt>
     * @throws FacadeException if a checked exception occurs while mirroring
     */
    public void mirror( RepositoryReferences sources, File destination, Collection<?/* IInstallableUnit */> rootUnits,
                        BuildContext context, int flags )
        throws FacadeException;
}
