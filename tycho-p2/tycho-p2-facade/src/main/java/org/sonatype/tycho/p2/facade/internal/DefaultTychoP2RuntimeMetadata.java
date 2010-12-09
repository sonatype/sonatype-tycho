package org.sonatype.tycho.p2.facade.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.utils.TychoVersion;

@Component( role = TychoP2RuntimeMetadata.class, hint = TychoP2RuntimeMetadata.HINT_FRAMEWORK )
public class DefaultTychoP2RuntimeMetadata
    implements TychoP2RuntimeMetadata
{
    private static final List<Dependency> ARTIFACTS;

    static
    {
        ARTIFACTS = new ArrayList<Dependency>();

        String p2Version = TychoVersion.getTychoVersion();

        ARTIFACTS.add( newDependency( "org.sonatype.tycho", "tycho-p2-runtime", p2Version, "zip" ) );
        ARTIFACTS.add( newDependency( "org.sonatype.tycho", "org.sonatype.tycho.p2.impl", p2Version, "jar" ) );
        ARTIFACTS.add( newDependency( "org.sonatype.tycho", "org.sonatype.tycho.p2.maven.repository", p2Version, "jar" ) );
        ARTIFACTS.add( newDependency( "org.sonatype.tycho", "org.sonatype.tycho.p2.tools.impl", p2Version, "jar" ) );
    }

    public List<Dependency> getRuntimeArtifacts()
    {
        return ARTIFACTS;
    }

    private static Dependency newDependency( String groupId, String artifactId, String version, String type )
    {
        Dependency d = new Dependency();
        d.setGroupId( groupId );
        d.setArtifactId( artifactId );
        d.setVersion( version );
        d.setType( type );
        return d;
    }

}
