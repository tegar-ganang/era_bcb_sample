package com.volantis.mcs.migrate.api.config;

import com.volantis.mcs.migrate.api.framework.ResourceMigrator;
import com.volantis.mcs.migrate.api.framework.ResourceMigrationException;
import com.volantis.mcs.migrate.api.framework.InputMetadata;
import com.volantis.mcs.migrate.api.framework.OutputCreator;
import com.volantis.mcs.migrate.api.notification.NotificationReporter;
import com.volantis.mcs.migrate.api.ExtensionClassLoader;
import com.volantis.synergetics.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Factory to create configuration related migration classes.
 */
public abstract class ConfigFactory {

    /**
     * The default instance.
     */
    private static final ConfigFactory defaultInstance;

    /**
     * Instantiate the default instance using reflection to prevent
     * dependencies between this and the implementation class.
     */
    static {
        defaultInstance = (ConfigFactory) ExtensionClassLoader.loadExtension("com.volantis.mcs.migrate.impl.config.DefaultConfigFactory", new NopConfigFactory(), null);
    }

    /**
     * Get the default instance of this factory.
     *
     * @return The default instance of this factory.
     */
    public static ConfigFactory getDefaultInstance() {
        return defaultInstance;
    }

    /**
     * Return a Entity Resolver that will resolve against the repository
     * @return an entity resolver
     */
    public abstract EntityResolver createRepositoryEntityResolver();

    /**
     * Create a resource migrator for migrating all relevant files to the
     * current version.
     *
     * @param reporter The notification reporter to use for feedback
     * @param strictMode The flag to indicate if the input validation failures should be informational only
     *          or should cause the migration to fail
     * @return a fully configured resource migrator
     * @throws ResourceMigrationException if a problem occurs when attempting
     * to migrate the resource.
     */
    public abstract ResourceMigrator createDefaultResourceMigrator(NotificationReporter reporter, boolean strictMode) throws ResourceMigrationException;

    /**
     * Create an object that can migrate a remote policy.
     *
     * @return A {@link RemotePolicyMigrator}.
     */
    public abstract RemotePolicyMigrator createRemotePolicyMigrator();

    /**
     * A simple config factory. This <b>MUST</b> be stateless as only one
     * is created to service all needs.
     */
    private static class NopConfigFactory extends ConfigFactory {

        public EntityResolver createRepositoryEntityResolver() {
            return new EntityResolver() {

                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    return null;
                }
            };
        }

        public ResourceMigrator createDefaultResourceMigrator(NotificationReporter reporter, boolean strictMode) throws ResourceMigrationException {
            return new ResourceMigrator() {

                public void migrate(InputMetadata meta, InputStream inputStream, OutputCreator outputCreator) throws IOException, ResourceMigrationException {
                    OutputStream outputStream = outputCreator.createOutputStream();
                    IOUtils.copy(inputStream, outputStream);
                }
            };
        }

        /**
         * Create a Remote Policy Manager that does nothing.
         *
         * @return a Remote Policy Manager that does nothing.
         */
        public RemotePolicyMigrator createRemotePolicyMigrator() {
            return new RemotePolicyMigrator() {

                public String migratePolicy(InputStream stream, String url) throws ResourceMigrationException, IOException {
                    ByteArrayOutputCreator oc = new ByteArrayOutputCreator();
                    IOUtils.copyAndClose(stream, oc.getOutputStream());
                    return oc.getOutputStream().toString();
                }
            };
        }
    }
}
