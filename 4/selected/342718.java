package org.iosgi.benchmark;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.service.command.CommandProcessor;
import org.iosgi.IsolationDirective;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sven Schulz
 */
@Component(immediate = true)
@Provides(specifications = BenchmarkManager.class)
public class BenchmarkManager {

    private static final Random RANDOM = new Random();

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkManager.class);

    @Requires(optional = true)
    private Benchmark[] benchmarks;

    @ServiceProperty(name = CommandProcessor.COMMAND_SCOPE)
    public String commandScope = "iosgi-benchmarks";

    @ServiceProperty(name = CommandProcessor.COMMAND_FUNCTION)
    public String[] commandFunction = new String[] { "benchmarks", "perform", "createBundle", "directive" };

    private String getName(final Benchmark b) {
        return b.getClass().getName();
    }

    public void perform(String pattern) {
        for (Benchmark b : benchmarks) {
            if (getName(b).matches(pattern)) {
                try {
                    b.perform();
                } catch (Exception e) {
                    LOGGER.error("benchmark execution failed", e);
                }
            }
        }
    }

    public String[] benchmarks() {
        String[] l = new String[benchmarks.length];
        for (int i = 0; i < benchmarks.length; i++) {
            l[i] = getName(benchmarks[i]);
        }
        return l;
    }

    public URL createBundle(int size, List<IsolationDirective> directives) throws IOException {
        String symbolicName = "org.iosgi.benchmark.bundle-" + Integer.toHexString(RANDOM.nextInt());
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.put(new Attributes.Name(Constants.BUNDLE_SYMBOLICNAME), symbolicName);
        attrs.put(new Attributes.Name(Constants.BUNDLE_VERSION), new Version(1, 0, 0).toString());
        if (!directives.isEmpty()) {
            StringBuilder b = new StringBuilder();
            for (IsolationDirective d : directives) {
                if (b.length() != 0) {
                    b.append(',');
                }
                b.append(d);
            }
            attrs.put(new Attributes.Name(org.iosgi.Constants.BUNDLE_ISOLATION), b.toString());
        }
        File f = new File(new File(System.getProperty("java.io.tmpdir")), symbolicName + "-1.0.0.jar");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(f), manifest);
        byte[] data = new byte[size];
        RANDOM.nextBytes(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        add("data", bais, jos);
        jos.close();
        return f.toURI().toURL();
    }

    private void add(String path, InputStream source, JarOutputStream target) throws IOException {
        BufferedInputStream in = null;
        try {
            JarEntry entry = new JarEntry(path);
            entry.setTime(System.currentTimeMillis());
            target.putNextEntry(entry);
            in = new BufferedInputStream(source);
            byte[] buf = new byte[1024];
            int read = 0;
            while ((read = in.read(buf)) != -1) {
                target.write(buf, 0, read);
            }
            target.closeEntry();
        } finally {
            if (in != null) in.close();
        }
    }

    public IsolationDirective directive(int level, String filter) throws InvalidSyntaxException {
        return new IsolationDirective(level, FrameworkUtil.createFilter(filter));
    }
}
