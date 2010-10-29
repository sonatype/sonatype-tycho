package org.sonatype.tycho.p2runtime;

import java.io.File;
import java.util.List;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;

public interface TychoP2RuntimeResolver
{
    public List<File> getRuntimeLocations( MavenSession session )
        throws MavenExecutionException;

}
