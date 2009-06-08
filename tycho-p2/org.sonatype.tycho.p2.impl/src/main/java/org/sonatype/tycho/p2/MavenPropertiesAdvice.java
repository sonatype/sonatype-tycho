package org.sonatype.tycho.p2;

import java.util.Map;
import java.util.Properties;

import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;

@SuppressWarnings( "restriction" )
public class MavenPropertiesAdvice
    implements IPropertyAdvice
{

    private final Properties properties;

    public MavenPropertiesAdvice( String groupId, String artifactId, String version )
    {
        properties = new Properties();

        properties.put( RepositoryLayoutHelper.PROP_GROUP_ID, groupId );
        properties.put( RepositoryLayoutHelper.PROP_ARTIFACT_ID, artifactId );
        properties.put( RepositoryLayoutHelper.PROP_VERSION, version );
    }

    @SuppressWarnings("unchecked")
    public Properties getArtifactProperties( IInstallableUnit iu, IArtifactDescriptor descriptor )
    {
        for (Map.Entry entry : properties.entrySet())
        {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            ((ArtifactDescriptor) descriptor).setProperty( key, value );
        }
        return null;
    }

    public Properties getInstallableUnitProperties( InstallableUnitDescription iu )
    {
        return properties;
    }

    public boolean isApplicable( String configSpec, boolean includeDefault, String id, Version version )
    {
        return true;
    }

}
