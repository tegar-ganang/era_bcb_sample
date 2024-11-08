package org.mobicents.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import org.jboss.system.ServiceMBeanSupport;

public class SimpleGlobalLoggingConfiguration extends ServiceMBeanSupport implements SimpleGlobalLoggingConfigurationMBean {

    private static final String DEFAULT_LOG4J_XML_FILE_NAME = "jboss-log4j.xml.";

    private static String log4jConfigFilePath = System.getProperty("jboss.server.home.dir") + File.separator + "conf" + File.separator + "jboss-log4j.xml";

    private static String log4jTemplatesPath = "";

    private static String log4jTemplatesCompletePath = "";

    private String currentProfile = "default";

    public SimpleGlobalLoggingConfiguration(String templatePath) {
        log4jTemplatesPath = System.getProperty("jboss.server.home.dir") + File.separator + templatePath + File.separator;
        log4jTemplatesCompletePath = log4jTemplatesPath + DEFAULT_LOG4J_XML_FILE_NAME;
        if (log.isTraceEnabled()) {
            log.trace("Mobicents Simple Global Logging Configuration loaded. Using path: ./" + templatePath);
        }
    }

    public String getCurrentProfile() {
        return currentProfile;
    }

    public String getLoggingConfiguration(String profile) throws IOException {
        try {
            return readFile(getLog4jPath(profile));
        } catch (IOException ioe) {
            throw new IOException("Failed to read log4j configuration file for profile '" + profile + "'. Does it exists?", ioe);
        }
    }

    public void setLoggingConfiguration(String profile, String contents) throws IOException {
        try {
            currentProfile = profile;
            writeFile(getLog4jPath(profile), contents);
        } catch (IOException ioe) {
            throw new IOException("Failed to update log4j configuration file for profile '" + profile + "'.", ioe);
        }
    }

    public Set<String> listProfiles() {
        File templatePath = new File(log4jTemplatesPath);
        Set<String> profiles = new HashSet<String>();
        if (templatePath.exists() && templatePath.isDirectory()) {
            File[] files = templatePath.listFiles();
            for (File file : files) {
                int indexOfFileName = file.getName().indexOf(DEFAULT_LOG4J_XML_FILE_NAME);
                if (indexOfFileName != -1) {
                    String profile = file.getName().substring(DEFAULT_LOG4J_XML_FILE_NAME.length());
                    profiles.add(profile);
                }
            }
        }
        return profiles;
    }

    public void switchLoggingConfiguration(String newProfile) throws IOException {
        try {
            currentProfile = newProfile;
            String srcPath = getLog4jPath(newProfile);
            String destPath = getLog4jPath("");
            writeFile(destPath, readFile(srcPath));
        } catch (IOException ioe) {
            throw new IOException("Failed to update log4j configuration file for profile '" + newProfile + "'.", ioe);
        }
    }

    private String getLog4jPath(String profile) {
        return (profile == null || profile.equals("") || profile.equalsIgnoreCase("current")) ? log4jConfigFilePath : log4jTemplatesCompletePath + profile;
    }

    private String readFile(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private void writeFile(String path, String contents) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(path));
        try {
            out.write(contents);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
