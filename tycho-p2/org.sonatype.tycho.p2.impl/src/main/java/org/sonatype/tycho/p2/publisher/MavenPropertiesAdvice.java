package org.sonatype.tycho.p2.publisher;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;

@SuppressWarnings( "restriction" )
public class MavenPropertiesAdvice
    implements IPropertyAdvice
{

    private final Map<String, String> properties = new LinkedHashMap<String, String>();

    public MavenPropertiesAdvice( String groupId, String artifactId, String version )
    {
        properties.put( RepositoryLayoutHelper.PROP_GROUP_ID, groupId );
        properties.put( RepositoryLayoutHelper.PROP_ARTIFACT_ID, artifactId );
        properties.put( RepositoryLayoutHelper.PROP_VERSION, version );
    }

    public MavenPropertiesAdvice( String groupId, String artifactId, String version, String classifier ) {
    	this(groupId, artifactId, version);
		properties.put(RepositoryLayoutHelper.PROP_CLASSIFIER, classifier);
    }

    public Map<String, String> getArtifactProperties( IInstallableUnit iu, IArtifactDescriptor descriptor )
    {
        for (Map.Entry<String, String> entry : properties.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue();
            ((ArtifactDescriptor) descriptor).setProperty( key, value );
        }
        return null;
    }

    public Map<String, String> getInstallableUnitProperties( InstallableUnitDescription iu )
    {
        return properties;
    }

    public boolean isApplicable( String configSpec, boolean includeDefault, String id, Version version )
    {
        return true;
    }

}
