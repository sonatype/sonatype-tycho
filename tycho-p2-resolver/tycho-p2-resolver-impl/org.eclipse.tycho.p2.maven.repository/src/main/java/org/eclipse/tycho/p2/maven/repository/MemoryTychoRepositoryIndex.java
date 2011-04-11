package org.eclipse.tycho.p2.maven.repository;

import java.util.List;

import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class MemoryTychoRepositoryIndex
    implements TychoRepositoryIndex
{
    private List<GAV> projectGAVs;

    public MemoryTychoRepositoryIndex( List<GAV> projectGAVs )
    {
        this.projectGAVs = projectGAVs;
    }

    public List<GAV> getProjectGAVs()
    {
        return projectGAVs;
    }
}
