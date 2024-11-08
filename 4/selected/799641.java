package org.eclipse.babel.build.core.languagepack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.eclipse.babel.build.core.Configuration;
import org.eclipse.babel.build.core.LocaleProxy;
import org.eclipse.babel.build.core.Messages;
import org.eclipse.babel.build.core.PluginProxy;
import org.eclipse.babel.build.core.ResourceProxy;
import org.eclipse.babel.build.core.coverage.PluginCoverageInformation;
import org.eclipse.babel.build.core.translationcatalogue.TranslationCatalogue;

/**
 * 
 * Responsible for generating an NL Fragment as a directory in the language pack.
 * May be removed in the near future if it is confirmed that all nl fragments should be jars.   
 *
 */
public class NonJarNLFragment implements NLFragment {

    private File directory;

    private final String BUNDLE_CLASSPATH = Messages.getString("Filename_bundle_classpath");

    private JarOutputStream bundleClasspathStream = null;

    private PluginProxy eclipseInstallPlugin;

    public NonJarNLFragment(File directory, PluginProxy eclipseInstallPlugin) {
        this.directory = directory;
        this.eclipseInstallPlugin = eclipseInstallPlugin;
    }

    public PluginCoverageInformation generateFragment(Configuration config) throws Exception {
        directory.mkdir();
        TranslationCatalogue translationCatalogue = config.translations();
        Set<LocaleProxy> locales = config.locales();
        if (locales.isEmpty() && config.localeGroups().isEmpty()) {
            locales.addAll(translationCatalogue.getAllLocales());
        }
        Map<String, PluginProxy> translationCataloguePluginMap = getPluginOrFeatureForSpecifiedLocales(config, eclipseInstallPlugin);
        boolean pluginIsMatchedToAnyLocale = false;
        PluginCoverageInformation coverage = new PluginCoverageInformation(eclipseInstallPlugin);
        try {
            for (LocaleProxy locale : locales) {
                boolean pluginIsMatchedToThisLocale = translationCataloguePluginMap.containsKey(locale.getName());
                if (pluginIsMatchedToThisLocale) {
                    PluginProxy translationCataloguePlugin = translationCataloguePluginMap.get(locale.getName());
                    pluginIsMatchedToAnyLocale = true;
                    for (ResourceProxy resource : eclipseInstallPlugin.getResources()) {
                        if (resource.getRelativePath().equalsIgnoreCase(MANIFEST_PATH)) {
                            FileOutputStream fileOut = new FileOutputStream(new File(directory, MANIFEST_PATH));
                            Manifest manifest = composeManifestContent(config.localeExtension());
                            manifest.write(fileOut);
                        } else {
                            ResourceProxy translationResource = translationCatalogue.getResourceTranslation(translationCataloguePlugin, resource);
                            if (translationResource != null) {
                                writeResource(eclipseInstallPlugin, translationResource, locale);
                                if (resource.getRelativePath().endsWith(Messages.getString("Extensions_properties"))) {
                                    int value = computePropertyCoverageForLocale(config, resource, translationResource);
                                    coverage.setResourceCoverageForLocale(locale, resource, true, value);
                                } else {
                                    coverage.setResourceCoverageForLocale(locale, resource, true);
                                }
                            } else {
                                coverage.setResourceCoverageForLocale(locale, resource, false);
                            }
                        }
                    }
                }
                coverage.setPluginMatchingForLocale(locale, pluginIsMatchedToThisLocale);
            }
            if (!pluginIsMatchedToAnyLocale) {
                directory.delete();
            }
            if (bundleClasspathStream != null) {
                bundleClasspathStream.close();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return coverage;
    }

    private int computePropertyCoverageForLocale(Configuration config, ResourceProxy eclipseTargetResource, ResourceProxy translationResource) throws Exception {
        InputStream in = getResourceAsStream(config, eclipseTargetResource);
        Properties property = new Properties();
        property.load(in);
        InputStream transIn = new FileInputStream(translationResource.getFileResource());
        Properties transProperty = new Properties();
        transProperty.load(transIn);
        Set<Object> properties = new HashSet<Object>(property.keySet());
        int initialSize = properties.size();
        initialSize = initialSize < 1 ? 1 : initialSize;
        properties.removeAll(transProperty.keySet());
        return (100 * (initialSize - properties.size())) / initialSize;
    }

    private InputStream getResourceAsStream(Configuration config, ResourceProxy resource) throws ZipException, IOException, FileNotFoundException {
        if (config.eclipseInstall().isArchive()) {
            ZipFile eclipseArchive = new ZipFile(config.eclipseInstall().getLocation());
            String pluginPath = Messages.getString("Paths_eclipse_directory_name") + SLASH + (eclipseInstallPlugin.isFeature() ? "features" : "plugins") + SLASH + eclipseInstallPlugin.getName() + Messages.getString("Characters_underscore") + eclipseInstallPlugin.getVersion() + SLASH + resource.getCanonicalPath();
            ZipEntry resourceFile = eclipseArchive.getEntry(pluginPath);
            return eclipseArchive.getInputStream(resourceFile);
        }
        return new FileInputStream(resource.getFileResource());
    }

    /**
	private void addBundleClasspathInManifest(Manifest manifest) throws Exception {
		Manifest manifest = new Manifest();
		
		Attributes attributes = manifest.getMainAttributes();
		
		attributes.putValue(Messages.getString("Manifest_entry_bundle_classpath"), Messages.getString("Filename_bundle_classpath")); //$NON-NLS-1$	 $NON-NLS-2$
	}*/
    private Manifest composeManifestContent(String localeExtension) throws Exception {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue(Messages.getString("Manifest_key_bundle_name"), eclipseInstallPlugin.getName() + " " + Messages.getString("Suffixes_fragment_name"));
        attributes.putValue(Messages.getString("Manifest_key_bundle_version"), eclipseInstallPlugin.getVersion());
        attributes.putValue(Messages.getString("Manifest_key_bundle_symbolic_name"), eclipseInstallPlugin.getName() + Messages.getString("Extensions_nl") + localeExtension + Messages.getString("Manifest_value_bundle_symbolic_name_suffix"));
        if (eclipseInstallPlugin.getName().contains((Messages.getString("Prefixes_eclipse_plugin")))) {
            attributes.putValue(Messages.getString("Manifest_key_bundle_vendor"), Messages.getString("Provider_name_eclipse"));
        } else if (eclipseInstallPlugin.getName().contains((Messages.getString("Prefixes_ibm_plugin")))) {
            attributes.putValue(Messages.getString("Manifest_key_bundle_vendor"), Messages.getString("Provider_name_ibm"));
        } else {
            attributes.putValue(Messages.getString("Manifest_key_bundle_vendor"), Messages.getString("Provider_name_unknown"));
        }
        attributes.putValue(Messages.getString("Manifest_key_fragment_host"), eclipseInstallPlugin.getName());
        return manifest;
    }

    private boolean writeResource(PluginProxy eclipseInstallPlugin, ResourceProxy translation, LocaleProxy locale) throws Exception {
        String translationResourceName = determineTranslatedResourceName(translation, locale);
        String pluginNameInDirFormat = eclipseInstallPlugin.getName().replace(Messages.getString("Characters_period"), File.separator);
        if (translation.getRelativePath().contains(pluginNameInDirFormat)) {
            return writeResourceToBundleClasspath(translation, locale);
        } else if (translationResourceName.contains(File.separator)) {
            String resourcePath = translationResourceName.substring(0, translationResourceName.lastIndexOf(File.separatorChar));
            File resourcePathDirectory = new File(directory.getPath() + File.separatorChar + resourcePath);
            resourcePathDirectory.mkdirs();
        }
        File fragmentResource = new File(directory.getPath() + File.separatorChar + translationResourceName);
        File translatedResource = new File(translation.getFileResource().getAbsolutePath());
        FileChannel inputChannel = new FileInputStream(translatedResource).getChannel();
        FileChannel outputChannel = new FileOutputStream(fragmentResource).getChannel();
        inputChannel.transferTo(0, inputChannel.size(), outputChannel);
        inputChannel.close();
        outputChannel.close();
        return true;
    }

    private boolean writeResourceToBundleClasspath(ResourceProxy translation, LocaleProxy locale) throws Exception {
        if (bundleClasspathStream == null) {
            bundleClasspathStream = new JarOutputStream(new FileOutputStream(directory.getPath() + File.separator + BUNDLE_CLASSPATH));
        }
        byte[] buf = new byte[1024];
        FileInputStream in = new FileInputStream(new File(translation.getFileResource().getAbsolutePath()));
        String temp = determineTranslatedResourceName(translation, locale);
        bundleClasspathStream.putNextEntry(new ZipEntry(temp));
        int len;
        while ((len = in.read(buf)) > 0) {
            bundleClasspathStream.write(buf, 0, len);
        }
        bundleClasspathStream.closeEntry();
        in.close();
        return true;
    }

    public String determineTranslatedResourceName(ResourceProxy resource, LocaleProxy locale) {
        String resourceName = resource.getRelativePath();
        String resourceExtension = resourceName.substring(resourceName.lastIndexOf(Messages.getString("Characters_period")));
        return resourceName.substring(0, resourceName.lastIndexOf(Messages.getString("Characters_period"))) + Messages.getString("Characters_underscore") + locale.getName() + resourceExtension;
    }

    private Map<String, PluginProxy> getPluginOrFeatureForSpecifiedLocales(Configuration config, PluginProxy plugin) {
        if (plugin.isFeature()) {
            return config.translations().getFeatureForSpecifiedLocales(plugin);
        }
        return config.translations().getPluginForSpecifiedLocales(plugin);
    }
}
