package org.codehaus.groovy.maven.gossip.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.codehaus.groovy.maven.gossip.InternalLogger;
import org.codehaus.groovy.maven.gossip.model.Configuration;
import org.codehaus.groovy.maven.gossip.model.Filter;
import org.codehaus.groovy.maven.gossip.model.Logger;
import org.codehaus.groovy.maven.gossip.model.Profile;
import org.codehaus.groovy.maven.gossip.model.Source;
import org.codehaus.groovy.maven.gossip.model.Trigger;
import org.codehaus.groovy.maven.gossip.model.render.Renderer;

/**
 * ???
 *
 * @version $Id: ConfigurationFactory.java 2 2008-04-03 19:50:16Z user57 $
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class ConfigurationFactory {

    private final InternalLogger log = InternalLogger.getLogger(getClass());

    private ClassLoader classLoader;

    public ConfigurationFactory(ClassLoader cl) {
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        this.classLoader = cl;
    }

    public ConfigurationFactory() {
        this(null);
    }

    public Configuration create(final URL url) throws Exception {
        assert url != null;
        log.debug("Configuring from: {}", url);
        Configuration config = new Configuration();
        Context ctx = createContext(url);
        String tmp = ctx.get("version", (String) null);
        if (!config.getVersion().equals(tmp)) {
            throw new ConfigurationException("Invalid configuration version: " + tmp + ", expected: " + config.getVersion());
        }
        Properties props = createProperties(ctx.child("properties"));
        config.properties().putAll(props);
        configureSources(config, ctx);
        configureProfiles(config, ctx);
        log.debug("Configuration: {}", config);
        return config;
    }

    private void configureSources(final Configuration config, final Context ctx) {
        assert config != null;
        assert ctx != null;
        log.debug("Configuring sources: {}", ctx);
        if (!ctx.contains("sources")) {
            log.debug("Missing 'sources' property; skipping");
            return;
        }
        String[] names = ctx.get("sources", "").split(",");
        for (int i = 0; i < names.length; i++) {
            String name = names[i].trim();
            if (name.length() == 0) {
                throw new ConfigurationException("Source name must not be blank");
            }
            try {
                Source source = createSource(ctx.get("source." + name, (String) null), ctx.child("source." + name));
                log.debug("Created source: {}", source);
                config.addSource(source);
            } catch (Exception e) {
                log.error("Failed to create source: {}", name, e);
            }
        }
    }

    private Source createSource(final String className, final Context ctx) throws Exception {
        assert className != null;
        assert ctx != null;
        log.debug("Creating source: {}", className);
        Class type = loadClass(className);
        Source source = (Source) type.newInstance();
        for (Iterator iter = ctx.names().iterator(); iter.hasNext(); ) {
            String key = (String) iter.next();
            String value = ctx.get(key, (String) null);
            maybeSet(source, key, value);
        }
        return source;
    }

    private void configureProfiles(final Configuration config, final Context ctx) {
        assert config != null;
        assert ctx != null;
        log.debug("Configuring profiles: {}", ctx);
        if (!ctx.contains("profiles")) {
            log.debug("Missing 'profiles' property; skipping");
            return;
        }
        String[] names = ctx.get("profiles", "").split(",");
        for (int i = 0; i < names.length; i++) {
            String name = names[i].trim();
            if (name.length() == 0) {
                throw new ConfigurationException("Profile name must not be blank");
            }
            Profile profile = createProfile(name, ctx.child("profile." + name));
            log.debug("Created profile: {}", profile);
            config.addProfile(profile);
        }
    }

    private Profile createProfile(final String name, final Context ctx) {
        assert name != null;
        assert ctx != null;
        log.debug("Creating profile: {}");
        Profile profile = new Profile(name);
        Properties props = createProperties(ctx.child("properties"));
        profile.properties().putAll(props);
        configureLoggers(profile, ctx.child("logger"));
        configureTriggers(profile, ctx);
        configureFilters(profile, ctx);
        return profile;
    }

    private void configureLoggers(final Profile profile, final Context ctx) {
        assert profile != null;
        assert ctx != null;
        log.debug("Configuring loggers: {}", ctx);
        for (Iterator iter = ctx.names().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String value = ctx.get(name, (String) null);
            Logger logger = new Logger(name);
            logger.setLevel(value);
            log.debug("Created logger[{}]: {}", name, logger);
            profile.addLogger(logger);
        }
    }

    private void configureTriggers(final Profile profile, final Context ctx) {
        assert profile != null;
        assert ctx != null;
        log.debug("Configuring triggers: {}", ctx);
        if (!ctx.contains("triggers")) {
            log.debug("Missing 'triggers' property; skipping");
            return;
        }
        String[] names = ctx.get("triggers", "").split(",");
        for (int i = 0; i < names.length; i++) {
            String name = names[i].trim();
            if (name.length() == 0) {
                throw new ConfigurationException("Trigger name must not be blank");
            }
            try {
                Trigger trigger = createTrigger(ctx.get("trigger." + name, (String) null), ctx.child("trigger." + name));
                log.debug("Created trigger: {}", trigger);
                profile.addTrigger(trigger);
            } catch (Exception e) {
                log.error("Failed to create trigger: {}", name, e);
            }
        }
    }

    private Trigger createTrigger(final String className, final Context ctx) throws Exception {
        assert className != null;
        assert ctx != null;
        log.debug("Creating trigger: {}", className);
        Class type = loadClass(className);
        Trigger trigger = (Trigger) type.newInstance();
        for (Iterator iter = ctx.names().iterator(); iter.hasNext(); ) {
            String key = (String) iter.next();
            int i = key.indexOf(".");
            if (i != -1) {
                key = key.substring(0, i);
            }
            String value = ctx.get(key, (String) null);
            maybeSet(trigger, key, value);
        }
        return trigger;
    }

    private void configureFilters(final Profile profile, final Context ctx) {
        assert profile != null;
        assert ctx != null;
        log.debug("Configuring filters: {}", ctx);
        if (!ctx.contains("filters")) {
            log.debug("Missing 'filters' property; skipping");
            return;
        }
        String[] names = ctx.get("filters", "").split(",");
        for (int i = 0; i < names.length; i++) {
            String name = names[i].trim();
            if (name.length() == 0) {
                throw new ConfigurationException("Filter name must not be blank");
            }
            try {
                Filter filter = createFilter(ctx.get("filter." + name, (String) null), ctx.child("filter." + name));
                log.debug("Created filter: {}", filter);
                profile.addFilter(filter);
            } catch (Exception e) {
                log.error("Failed to create filter: {}", name, e);
            }
        }
    }

    private Filter createFilter(final String className, final Context ctx) throws Exception {
        assert className != null;
        assert ctx != null;
        log.debug("Creating filter: {}", className);
        Class type = loadClass(className);
        Filter filter = (Filter) type.newInstance();
        configureRenderer(filter, ctx);
        for (Iterator iter = ctx.names().iterator(); iter.hasNext(); ) {
            String key = (String) iter.next();
            int i = key.indexOf(".");
            if (i != -1) {
                key = key.substring(0, i);
            }
            if ("renderer".equals(key)) {
                continue;
            }
            Object value = ctx.get(key);
            maybeSet(filter, key, value);
        }
        return filter;
    }

    private void configureRenderer(final Filter filter, final Context ctx) throws Exception {
        assert filter != null;
        assert ctx != null;
        String className = ctx.get("renderer", (String) null);
        if (className != null) {
            Renderer renderer = createRenderer(className, ctx.child("renderer"));
            log.debug("Created renderer: {}", renderer);
            filter.setRenderer(renderer);
        }
    }

    private Renderer createRenderer(final String className, final Context ctx) throws Exception {
        assert className != null;
        assert ctx != null;
        log.debug("Creating renderer: {}", className);
        Class type = loadClass(className);
        Renderer renderer = (Renderer) type.newInstance();
        for (Iterator iter = ctx.names().iterator(); iter.hasNext(); ) {
            String key = (String) iter.next();
            int i = key.indexOf(".");
            if (i != -1) {
                key = key.substring(0, i);
            }
            String value = ctx.get(key, (String) null);
            maybeSet(renderer, key, value);
        }
        return renderer;
    }

    private Context createContext(final URL url) throws IOException {
        assert url != null;
        Properties props = new Properties();
        InputStream input = url.openStream();
        try {
            props.load(input);
        } finally {
            input.close();
        }
        if (log.isDebugEnabled()) {
            dumpProperties(props);
        }
        return new Context(props);
    }

    private Properties createProperties(final Context ctx) {
        assert ctx != null;
        Properties props = new Properties();
        for (Iterator iter = ctx.names().iterator(); iter.hasNext(); ) {
            String key = (String) iter.next();
            String value = ctx.get(key, (String) null);
            props.setProperty(key, value);
        }
        if (log.isDebugEnabled()) {
            dumpProperties(props);
        }
        return props;
    }

    private void dumpProperties(final Properties props) {
        if (!props.isEmpty()) {
            log.debug("Properties: ");
            List keys = new ArrayList();
            keys.addAll(props.keySet());
            Collections.sort(keys);
            for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
                String name = (String) iter.next();
                String value = props.getProperty(name);
                log.debug("    {} -> {}", name, value);
            }
        }
    }

    private Class loadClass(final String className) throws ClassNotFoundException {
        assert className != null;
        Class type = classLoader.loadClass(className);
        log.trace("Loaded class: {}", type);
        return type;
    }

    private String capitalize(final String string) {
        assert string != null;
        int length = string.length();
        if (length == 0) {
            return string;
        } else if (length == 1) {
            return string.toUpperCase();
        } else {
            return (Character.toUpperCase(string.charAt(0)) + string.substring(1));
        }
    }

    private void maybeSet(final Object target, final String name, final Object value) {
        assert target != null;
        assert name != null;
        assert value != null;
        String tmp = "set" + capitalize(name);
        log.trace("Looking for setter: {}", tmp);
        Class type = target.getClass();
        try {
            Method setter = type.getMethod(tmp, new Class[] { String.class });
            if (setter != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Setting '{}={}' via: {}", new Object[] { name, value, setter });
                }
                setter.invoke(target, new Object[] { value });
            }
        } catch (NoSuchMethodException e) {
            log.warn("No '{}(String)' found for: {} in: {}", new Object[] { tmp, name, type });
        } catch (Exception e) {
            log.error("Failed to set '{}={}'", new Object[] { name, value, e });
        }
    }
}
