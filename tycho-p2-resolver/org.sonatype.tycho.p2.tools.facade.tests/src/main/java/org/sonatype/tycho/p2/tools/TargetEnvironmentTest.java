package org.sonatype.tycho.p2.tools;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class TargetEnvironmentTest
{
    private static final String WS = "cocoa";

    private static final String OS = "mac";

    private static final String ARCH = "ppc";

    private TargetEnvironment subject;

    @Before
    public void initSubject()
    {
        subject = new TargetEnvironment( WS, OS, ARCH );
    }

    @Test
    public void testGetters()
    {
        assertEquals( WS, subject.getWs() );
        assertEquals( OS, subject.getOs() );
        assertEquals( ARCH, subject.getArch() );
    }

    @Test
    public void testToConfigSpec()
    {
        assertEquals( "cocoa.mac.ppc", subject.toConfigSpec() );
    }

    @Test
    public void testToFilter()
    {
        Map<String, String> filterMap = subject.toFilter();

        assertEquals( 3, filterMap.size() );
        assertEquals( WS, filterMap.get( "osgi.ws" ) );
        assertEquals( OS, filterMap.get( "osgi.os" ) );
        assertEquals( ARCH, filterMap.get( "osgi.arch" ) );
    }
}
