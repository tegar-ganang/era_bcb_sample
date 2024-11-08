package conga.io;

import conga.param.spec.ByName;
import java.util.Set;

/**
 * Sources parameters from the JVM's system properties.  Can load all system
 * properties or only a subset, according to a paramter name specification.
 * Relies on {@link System#getProperties()}, so security manager policies
 * apply.
 *
 * @author Justin Caballero
 */
public class EnvironmentRepo extends SystemRepo {

    public EnvironmentRepo() {
        super();
    }

    public EnvironmentRepo(ByName nameSpec) {
        super(nameSpec);
    }

    protected String getProperty(String name) {
        return System.getenv(name);
    }

    protected Set<? extends Object> getSystemKeys() {
        return System.getenv().keySet();
    }

    protected Sink getSink() {
        throw new UnsupportedOperationException("Cannot write paramters to read-only environment");
    }
}
