package com.volantis.mcs.migrate.api.framework;

import com.volantis.mcs.migrate.api.ExtensionClassLoader;
import com.volantis.mcs.migrate.api.notification.NotificationReporter;
import com.volantis.synergetics.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A factory for creating the major API classes of the migration framework.
 */
public abstract class FrameworkFactory {

    /**
     * The default instance.
     */
    private static final FrameworkFactory defaultInstance;

    /**
     * Instantiate the default instance using reflection to prevent
     * dependencies between this and the implementation class.
     */
    static {
        defaultInstance = (FrameworkFactory) ExtensionClassLoader.loadExtension("com.volantis.mcs.migrate.impl.framework.DefaultFrameworkFactory", new NoopFrameworkFactory(), null);
    }

    /**
     * Get the default instance of this factory.
     *
     * @return The default instance of this factory.
     */
    public static FrameworkFactory getDefaultInstance() {
        return defaultInstance;
    }

    /**
     * Create a builder which can be used to build {@link ResourceMigrator}
     * instances.
     *
     * @param reporter used to report notifications to the user.
     * @return the builder created.
     */
    public abstract ResourceMigratorBuilder createResourceMigratorBuilder(NotificationReporter reporter);

    /**
     * Create a version object from a string representation of it's name.
     *
     * @param name the name of the version.
     * @return the version object corresponding to the name supplied.
     */
    public abstract Version createVersion(String name);

    /**
     * Create meta data to describe input data for migration.
     *
     * @param uri the URI of the input data.
     * @param requiresPathRecognition true if we can perform path recognition
     *      on the URI, false otherwise.
     * @return
     */
    public abstract InputMetadata createInputMetadata(String uri, boolean requiresPathRecognition);

    /**
     * A factory that does not perform migration
     */
    private static class NoopFrameworkFactory extends FrameworkFactory {

        public ResourceMigratorBuilder createResourceMigratorBuilder(NotificationReporter reporter) {
            return new ResourceMigratorBuilder() {

                public ResourceMigrator getCompletedResourceMigrator() {
                    return new ResourceMigrator() {

                        public void migrate(InputMetadata meta, InputStream inputStream, OutputCreator outputCreator) throws IOException, ResourceMigrationException {
                            OutputStream outputStream = outputCreator.createOutputStream();
                            IOUtils.copy(inputStream, outputStream);
                        }
                    };
                }

                public void setTarget(Version version) {
                }

                public void startType(String typeName) {
                }

                public void setRegexpPathRecogniser(String re) {
                }

                public void setCustomPathRecogniser(PathRecogniser pathRecogniser) {
                }

                public void addRegexpContentRecogniser(Version version, String re) {
                }

                public void addCustomContentRecogniser(Version version, ContentRecogniser contentRecogniser) {
                }

                public XSLStreamMigratorBuilder createXSLStreamMigratorBuilder() {
                    return null;
                }

                public void addStep(Version inputVersion, Version outputVersion, StreamMigrator streamMigrator) {
                }

                public void endType() {
                }
            };
        }

        /**
         * Always return the current build version for the specified string
         *
         * @param name the string to create a version from
         * @return a Version representing the supplied string
         */
        public Version createVersion(final String name) {
            return new Version() {

                public String getName() {
                    return name;
                }
            };
        }

        public InputMetadata createInputMetadata(final String uri, final boolean requiresPathRecognition) {
            return new InputMetadata() {

                public String getURI() {
                    return uri;
                }

                public boolean applyPathRecognition() {
                    return requiresPathRecognition;
                }
            };
        }
    }
}
