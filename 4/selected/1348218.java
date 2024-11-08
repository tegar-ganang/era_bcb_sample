package fi.pyramus.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.resolution.ArtifactDescriptorException;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import sun.misc.Service;
import fi.internetix.smvc.logging.Logging;
import fi.pyramus.plugin.maven.MavenClient;

@SuppressWarnings("restriction")
public class PluginManager {

    public static final synchronized PluginManager getInstance() {
        return INSTANCE;
    }

    public static final synchronized PluginManager initialize(ClassLoader parentClassLoader, List<String> repositories) {
        if (INSTANCE != null) throw new PluginManagerException("Plugin manger is already initialized");
        INSTANCE = new PluginManager(parentClassLoader, repositories);
        return INSTANCE;
    }

    private static PluginManager INSTANCE = null;

    PluginManager(ClassLoader parentClassLoader, List<String> repositories) {
        this.jarLoader = new JarLoader(parentClassLoader);
        mavenClient = new MavenClient(getPluginDirectory());
        for (String repository : repositories) {
            mavenClient.addRepository(repository);
        }
    }

    public void addRepository(String url) {
        mavenClient.addRepository(url);
    }

    public void removeRepository(String url) {
        mavenClient.removeRepository(url);
    }

    private File getPluginDirectory() {
        String absoluteParent = new File(".").getAbsolutePath();
        absoluteParent = absoluteParent.substring(0, absoluteParent.length() - 1);
        File parentDirectory = new File(absoluteParent);
        if (parentDirectory.exists()) {
            File pluginDirectory = new File(parentDirectory, "PyramusPlugins");
            if (pluginDirectory.exists()) {
                if (pluginDirectory.canRead() && pluginDirectory.canWrite()) {
                    return pluginDirectory;
                } else {
                    throw new PluginManagerException("Cannot read or write into plugin directory");
                }
            } else {
                if (parentDirectory.canWrite()) {
                    if (!pluginDirectory.mkdir()) {
                        throw new PluginManagerException("Failed to create new plugin directory");
                    } else {
                        return pluginDirectory;
                    }
                } else {
                    throw new PluginManagerException("Unable to create new plugin directory. Parent folder is write protected");
                }
            }
        } else {
            throw new PluginManagerException("Plugins parent directory does not exist");
        }
    }

    public void loadPlugin(String groupId, String artifactId, String version) {
        try {
            ArtifactDescriptorResult descriptorResult = mavenClient.describeArtifact(groupId, artifactId, version);
            for (Dependency dependency : descriptorResult.getDependencies()) {
                if ("compile".equals(dependency.getScope())) {
                    File file = mavenClient.getArtifactJarFile(dependency.getArtifact());
                    Logging.logInfo("Loading " + groupId + "." + artifactId + ":" + version + " dependecy: " + file);
                    jarLoader.loadJar(file);
                }
            }
            File jarFile = mavenClient.getArtifactJarFile(descriptorResult.getArtifact());
            Logging.logInfo("Loading " + groupId + "." + artifactId + ":" + version + " plugin jar: " + jarFile);
            jarLoader.loadJar(jarFile);
        } catch (ArtifactResolutionException e) {
            throw new PluginManagerException(e);
        } catch (ArtifactDescriptorException e) {
            throw new PluginManagerException(e);
        }
    }

    public boolean isLoaded(String groupId, String artifactId, String version) {
        try {
            ArtifactDescriptorResult descriptorResult = mavenClient.describeArtifact(groupId, artifactId, version);
            File jarFile = mavenClient.getArtifactJarFile(descriptorResult.getArtifact());
            return jarLoader.isJarLoaded(jarFile);
        } catch (Exception e) {
            return false;
        }
    }

    public void registerPlugins() {
        @SuppressWarnings("unchecked") Iterator<PluginDescriptor> pluginDescriptors = Service.providers(PluginDescriptor.class, jarLoader.getPluginsClassLoader());
        while (pluginDescriptors.hasNext()) {
            PluginDescriptor pluginDescriptor = pluginDescriptors.next();
            registerPlugin(pluginDescriptor);
        }
    }

    public ClassLoader getPluginsClassLoader() {
        return jarLoader.getPluginsClassLoader();
    }

    public synchronized List<PluginDescriptor> getPlugins() {
        return plugins;
    }

    public synchronized void registerPlugin(PluginDescriptor plugin) {
        for (PluginDescriptor pluginDescriptor : plugins) {
            if (pluginDescriptor.getName().equals(plugin.getName())) return;
        }
        plugins.add(plugin);
    }

    private JarLoader jarLoader;

    private MavenClient mavenClient;

    private List<PluginDescriptor> plugins = new ArrayList<PluginDescriptor>();
}
