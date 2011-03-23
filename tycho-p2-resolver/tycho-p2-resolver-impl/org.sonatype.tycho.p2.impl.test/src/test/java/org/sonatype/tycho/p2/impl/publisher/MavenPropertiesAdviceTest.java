package org.sonatype.tycho.p2.impl.publisher;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.sonatype.tycho.p2.repository.RepositoryLayoutHelper;

public class MavenPropertiesAdviceTest
{

    @Test
    public void testIUPropertiesNullClassifier()
    {
        Map<String, String> iuProperties = createIUProperties( null );
        assertNull( iuProperties.get( RepositoryLayoutHelper.PROP_CLASSIFIER ) );
    }

    @Test
    public void testIUPropertiesEmptyClassifier()
    {
        Map<String, String> iuProperties = createIUProperties( "" );
        assertNull( iuProperties.get( RepositoryLayoutHelper.PROP_CLASSIFIER ) );
    }

    @Test
    public void testIUPropertiesNonEmptyClassifier()
    {
        Map<String, String> iuProperties = createIUProperties( "sources" );
        assertEquals( "sources", iuProperties.get( RepositoryLayoutHelper.PROP_CLASSIFIER ) );
    }

    private Map<String, String> createIUProperties( String classifier )
    {
        MavenPropertiesAdvice mavenPropertiesAdvice =
            new MavenPropertiesAdvice( "groupId", "artifactId", "1.0.0", classifier );
        Map<String, String> iuProperties = mavenPropertiesAdvice.getInstallableUnitProperties( null );
        return iuProperties;
    }

}
