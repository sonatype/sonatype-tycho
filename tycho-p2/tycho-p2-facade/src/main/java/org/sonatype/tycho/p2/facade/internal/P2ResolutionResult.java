package org.sonatype.tycho.p2.facade.internal;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

public class P2ResolutionResult
{

    private final Set<File> bundles = new LinkedHashSet<File>();

    private final Set<File> features = new LinkedHashSet<File>();

    public void addBundle( File bundle )
    {
        this.bundles.add( bundle );
    }

    public void addFeature( File feature )
    {
        this.features.add( feature );
    }

    public Set<File> getBundles()
    {
        return bundles;
    }
    
    public Set<File> getFeatures()
    {
        return features;
    }

}
