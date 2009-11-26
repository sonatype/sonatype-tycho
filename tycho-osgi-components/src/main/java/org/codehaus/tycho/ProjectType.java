package org.codehaus.tycho;

public interface ProjectType
{
    public static final String ECLIPSE_PLUGIN = "eclipse-plugin";

    public static final String ECLIPSE_TEST_PLUGIN = "eclipse-test-plugin";

    public static final String ECLIPSE_FEATURE = "eclipse-feature";

    public static final String ECLIPSE_UPDATE_SITE = "eclipse-update-site";

    public static final String ECLIPSE_APPLICATION = "eclipse-application";
    
    /**
     * Used in dependencyManagement section to mark local eclipse installation.
     */
    public static final String ECLIPSE_INSTALLATION = "eclipse-installation";

    /**
     * Used in dependencyManagement section to mark local eclipse extension locations. 
     */
    public static final String ECLIPSE_EXTENSION_LOCATION = "eclipse-extension-location";

    public static final String[] PROJECT_TYPES = {
        ECLIPSE_PLUGIN,
        ECLIPSE_TEST_PLUGIN,
        ECLIPSE_FEATURE,
        ECLIPSE_UPDATE_SITE,
        ECLIPSE_APPLICATION
    };
}
