package org.sonatype.tycho.p2.facade.internal;

import java.io.File;
import java.util.Set;

import org.sonatype.tycho.ReactorProject;
import org.sonatype.tycho.p2.IReactorArtifactFacade;

public class ReactorArtifactFacade
    implements IReactorArtifactFacade
{
    private final ReactorProject wrappedProject;

    private final String classifier;

    public ReactorArtifactFacade( ReactorProject otherProject, String classifier )
    {
        this.wrappedProject = otherProject;
        this.classifier = classifier;
    }

    public File getLocation()
    {
        return wrappedProject.getBasedir();
    }

    public String getGroupId()
    {
        return wrappedProject.getGroupId();
    }

    public String getArtifactId()
    {
        return wrappedProject.getArtifactId();
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getVersion()
    {
        return wrappedProject.getVersion();
    }

    public String getPackagingType()
    {
        return wrappedProject.getPackaging();
    }

    public Set<Object/* IInstallableUnit */> getDependencyMetadata()
    {
        return wrappedProject.getDependencyMetadata( classifier );
    }

    public String getClassidier()
    {
        return classifier;
    }
}
