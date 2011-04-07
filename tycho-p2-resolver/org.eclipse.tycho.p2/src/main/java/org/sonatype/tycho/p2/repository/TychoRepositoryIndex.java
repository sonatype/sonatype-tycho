package org.sonatype.tycho.p2.repository;

import java.util.List;



public interface TychoRepositoryIndex
{

    List<GAV> getProjectGAVs();

}
