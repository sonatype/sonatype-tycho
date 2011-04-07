package org.sonatype.tycho.p2.tools;

import java.util.HashMap;

public class TargetEnvironment
{
    private final String ws;

    private final String os;

    private final String arch;

    public TargetEnvironment( String ws, String os, String arch )
    {
        this.ws = ws;
        this.os = os;
        this.arch = arch;
    }

    /**
     * Returns the windowing system of the represented target environment.
     */
    public String getWs()
    {
        return ws;
    }

    /**
     * Returns the operating system of the represented target environment.
     */
    public String getOs()
    {
        return os;
    }

    /**
     * Returns the architecture of the represented target environment.
     */
    public String getArch()
    {
        return arch;
    }

    /**
     * Returns the target environment as string of the form <code>ws.os.arch</code>. This format is
     * used by the p2 publishers and in that context called "configuration" or "config spec".
     */
    public String toConfigSpec()
    {
        return ws + '.' + os + '.' + arch;
    }

    /**
     * Returns the target environment as map. The keys are "osgi.ws", "osgi.os", and "osgi.arch".
     * This format is used by the p2 slicer to filter installable units by environments.
     * 
     * @return a new instance of {@link HashMap} with the target environment set
     */
    public HashMap<String, String> toFilter()
    {
        HashMap<String, String> result = new HashMap<String, String>();
        result.put( "osgi.ws", ws );
        result.put( "osgi.os", os );
        result.put( "osgi.arch", arch );
        return result;
    }
}
