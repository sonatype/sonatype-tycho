package org.sonatype.tycho.plugins.p2;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.sonatype.tycho.p2.facade.internal.P2ApplicationLauncher;

/**
 * Adds category IUs to existing metadata repository.
 * http://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/p2_publisher.html
 * 
 * @goal category-p2-metadata
 */
public class CategoryP2MetadataMojo
    extends AbstractP2MetadataMojo
{
    /**
     * @parameter default-value="${project.basedir}/category.xml"
     */
    private File categoryDefinition;

    @Override
    protected String getPublisherApplication()
    {
        return "org.eclipse.equinox.p2.publisher.CategoryPublisher";
    }

    @Override
    protected void addArguments( P2ApplicationLauncher cli )
        throws IOException, MalformedURLException
    {
        cli.addArguments( "-metadataRepository", getUpdateSiteLocation().toURL().toExternalForm(), //
                          "-categoryDefinition", categoryDefinition.toURL().toExternalForm() );
    }
}
