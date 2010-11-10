package org.sonatype.tycho.p2.tools.publisher;

import java.io.File;
import java.util.Collection;

import org.sonatype.tycho.p2.tools.FacadeException;

public interface PublisherService
{
    /**
     * Publishes given category definitions.
     * 
     * @param categoryDefinition A category.xml file as defined by the Eclipse PDE
     * @throws IllegalStateException if this service instance has already been stopped
     * @throws FacadeException if a checked exception occurs during publishing
     * @return the root IUs in the publisher result
     */
    Collection</* IInstallableUnit */?> publishCategories( File categoryDefinition )
        throws FacadeException, IllegalStateException;

    /**
     * Publishes the given product definition.
     * 
     * @param productDefinition A .product file as defined by the Eclipse PDE
     * @param launcherBinaries A folder that contains the native Eclipse launcher binaries
     * @param flavor The installation flavor the product shall be published for
     * @throws IllegalStateException if this service instance has already been stopped
     * @throws FacadeException if a checked exception occurs during publishing
     * @return the root IUs in the publisher result
     */
    Collection</* IInstallableUnit */?> publishProduct( File productDefinition, File launcherBinaries, String flavor )
        throws FacadeException, IllegalStateException;

    /**
     * Stops this PublisherService instance. This shuts down and unregisters internally used services.
     */
    void stop();
}
