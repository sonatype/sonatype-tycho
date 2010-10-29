package org.sonatype.tycho;

/**
 * @author igor
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ArtifactKey
{

    public static final String TYPE_ECLIPSE_PLUGIN = "eclipse-plugin";

    public static final String TYPE_ECLIPSE_TEST_PLUGIN = "eclipse-test-plugin";

    public static final String TYPE_ECLIPSE_FEATURE = "eclipse-feature";

    public static final String TYPE_ECLIPSE_UPDATE_SITE = "eclipse-update-site";

    public static final String TYPE_ECLIPSE_APPLICATION = "eclipse-application";

    public static final String TYPE_ECLIPSE_REPOSITORY = "eclipse-repository";

    public static final String[] PROJECT_TYPES = {
        TYPE_ECLIPSE_PLUGIN,
        TYPE_ECLIPSE_TEST_PLUGIN,
        TYPE_ECLIPSE_FEATURE,
        TYPE_ECLIPSE_UPDATE_SITE,
        TYPE_ECLIPSE_APPLICATION,
        TYPE_ECLIPSE_REPOSITORY
    };

    /**
     * Artifact type. One of TYPE_* constants above.
     */
    public String getType();

    /**
     * Eclipse/OSGi artifact id (bundle symbolic name, feature id, etc). Can differ from Maven artifactId.
     */
    public String getId();

    /**
     * Eclipse/OSGi artifact version. Can differ from Maven version. For maven projects, this version corresponds to
     * version specified in the project sources and does not reflect qualifier expansion.
     */
    public String getVersion();

}
