package org.sonatype.tycho.resolver;

import java.io.File;

public interface DependentMavenProjectProxy
{

    public String getGroupId();

    public String getArtifactId();

    public String getVersion();

    public File getBasedir();

    public String getPackaging();

    public Object getContextValue( String key );

    public void setContextValue( String key, Object value );

    /**
     * Returns main project artifact file or null, if the project has not been packaged yet.
     */
    public File getArtifact();

    /**
     * returns attached artifact file or null if no such attached artifact.
     */
    public File getArtifact( String artifactClassifier );

}
