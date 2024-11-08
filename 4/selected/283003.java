package test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;

public class OsgiTestCase extends AbstractConfigurableBundleCreatorTests {

    String platformConfigFileLocation = "../target-platform/default.target";

    protected boolean createManifestOnlyFromTestClass() {
        return true;
    }

    public String getPlatformConfigFileLocation() {
        return platformConfigFileLocation;
    }

    public void setPlatformConfigFileLocation(String platformConfigFileLocation) {
        this.platformConfigFileLocation = platformConfigFileLocation;
    }

    protected String getRootPath() {
        return "file:./bin";
    }

    protected String getManifestLocation() {
        return "file:./META-INF/MANIFEST.MF";
    }

    protected String[] getConfigLocations() {
        return new String[] { "file:./META-INF/spring/test-context-osgi.xml" };
    }

    protected String[] getTestBundleLocations() {
        return null;
    }

    protected Resource[] getTestBundles() {
        String[] testLocations = getTestBundleLocations();
        if (testLocations == null) return null;
        Resource[] resources = new Resource[testLocations.length];
        int counter = 0;
        for (String bundleUrl : testLocations) {
            try {
                resources[counter++] = new UrlResource(bundleUrl);
            } catch (MalformedURLException err) {
                throw new RuntimeException(err);
            }
        }
        return resources;
    }

    protected String[] getFrameworkBundleLocations() {
        Reader reader = null;
        List<String> locationList = new ArrayList<String>();
        String config = null;
        try {
            reader = new FileReader(platformConfigFileLocation);
            StringWriter writer = new StringWriter();
            char[] buffer = new char[1024];
            int n = 0;
            while (-1 != (n = reader.read(buffer))) writer.write(buffer, 0, n);
            config = writer.toString();
        } catch (IOException err) {
            throw new RuntimeException(err);
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Throwable err) {
            }
        }
        int start = config.indexOf("<extraLocations>");
        if (start < 0) return null;
        int stop = start;
        String startTag = "<location path=\"";
        String stopTag = "\"/>";
        while ((start = config.indexOf(startTag, start)) > 0) {
            stop = config.indexOf(stopTag, start);
            if (stop <= 0) break;
            locationList.add(config.substring(start + startTag.length(), stop));
            start = stop + stopTag.length();
        }
        String[] locations = new String[locationList.size()];
        locationList.toArray(locations);
        return locations;
    }

    protected Resource[] getTestFrameworkBundles() {
        Set<String> addedSet = new HashSet<String>();
        String[] locations = getFrameworkBundleLocations();
        List<String> bundleList = new ArrayList<String>();
        for (String location : locations) {
            File target = new File(location);
            if (!target.exists()) continue;
            if (target.isDirectory()) {
                File[] children = target.listFiles(new FileFilter() {

                    public boolean accept(File file) {
                        if (file.isFile() && (file.getName().toLowerCase().endsWith(".jar") || file.getName().toLowerCase().endsWith(".war"))) return true; else return false;
                    }
                });
                for (File child : children) {
                    if (addedSet.contains(child.getName())) {
                        System.out.println(child.getPath() + " has been loaded!");
                        continue;
                    }
                    addedSet.add(child.getName());
                    bundleList.add("reference:file:" + child.getPath());
                }
            }
        }
        Resource[] resources = new Resource[bundleList.size()];
        int counter = 0;
        for (String bundleUrl : bundleList) {
            try {
                resources[counter++] = new UrlResource(bundleUrl);
            } catch (MalformedURLException err) {
                throw new RuntimeException(err);
            }
        }
        return resources;
    }
}
