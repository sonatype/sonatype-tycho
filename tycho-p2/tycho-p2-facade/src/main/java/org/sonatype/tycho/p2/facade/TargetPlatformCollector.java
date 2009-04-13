package org.sonatype.tycho.p2.facade;

import java.io.File;

public interface TargetPlatformCollector
{
    public void addBundle( File location );

    public void addFeature( File location );
}
