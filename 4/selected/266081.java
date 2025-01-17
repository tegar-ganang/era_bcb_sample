package eu.isas.peptideshaker;

import com.compomics.util.gui.UtilitiesGUIDefaults;
import eu.isas.peptideshaker.preferences.UserPreferences;
import eu.isas.peptideshaker.utils.Properties;
import java.io.*;
import javax.swing.JOptionPane;

/**
 * A wrapper class used to start the jar file with parameters. The parameters
 * are read from the JavaOptions file in the Properties folder.
 *
 * @author Harald Barsnes
 */
public class PeptideShakerWrapper {

    /**
     * If set to true debug output will be written to the screen and to
     * startup.log.
     */
    private boolean useStartUpLog = true;

    /**
     * Writes the debug output to startup.log.
     */
    private BufferedWriter bw = null;

    /**
     * The name of the jar file. Must be equal to the name given in the pom
     * file.
     */
    private String jarFileName = "PeptideShaker-";

    /**
     * True if this the first time the wrapper tries to launch the application.
     * If the first launch failes, e.g., due to memory settings, it is set to
     * false.
     */
    private boolean firstTry = true;

    /**
     * Is set to true if proxy settings are found in the JavaOptions file.
     */
    private boolean proxySettingsFound = false;

    /**
     * User preferences file.
     */
    private final String USER_PREFERENCES_FILE = System.getProperty("user.home") + "/.peptideshaker/userpreferences.cpf";

    /**
     * The user preferences.
     */
    private UserPreferences userPreferences;

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     */
    public PeptideShakerWrapper() {
        jarFileName = jarFileName + new Properties().getVersion() + ".jar";
        UtilitiesGUIDefaults.setLookAndFeel();
        try {
            loadUserPreferences();
            if (useStartUpLog) {
                String path = this.getClass().getResource("PeptideShakerWrapper.class").getPath();
                path = path.substring(5, path.indexOf(jarFileName));
                path = path.replace("%20", " ");
                path = path.replace("%5b", "[");
                path = path.replace("%5d", "]");
                File debugOutput = new File(path + "resources/conf/startup.log");
                bw = new BufferedWriter(new FileWriter(debugOutput));
                bw.write("Memory settings read from the user preferences: " + userPreferences.getMemoryPreference() + "\n");
            }
            launch();
            if (useStartUpLog) {
                bw.flush();
                bw.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Launches the jar file with parameters to the jvm.
     *
     * @throws java.lang.Exception
     */
    private void launch() throws Exception {
        String temp = "", cmdLine, path;
        String options = "", currentOption;
        path = this.getClass().getResource("PeptideShakerWrapper.class").getPath();
        path = path.substring(5, path.indexOf(jarFileName));
        path = path.replace("%20", " ");
        path = path.replace("%5b", "[");
        path = path.replace("%5d", "]");
        File javaOptions = new File(path + "resources/conf/JavaOptions.txt");
        File nonStandardJavaHome = new File(path + "resources/conf/JavaHome.txt");
        File uniprotApiPropertiesFile = new File(path + "resources/conf/proxy/uniprotjapi.properties");
        String uniprotApiProperties = "";
        if (javaOptions.exists()) {
            try {
                FileReader f = new FileReader(javaOptions);
                BufferedReader b = new BufferedReader(f);
                currentOption = b.readLine();
                while (currentOption != null) {
                    if (currentOption.startsWith("-Xmx")) {
                        if (firstTry) {
                            currentOption = currentOption.substring(4, currentOption.length() - 1);
                            boolean input = false;
                            for (char c : currentOption.toCharArray()) {
                                if (c != '*') {
                                    input = true;
                                    break;
                                }
                            }
                            if (input) {
                                try {
                                    userPreferences.setMemoryPreference(new Integer(currentOption));
                                    saveNewSettings();
                                    if (useStartUpLog) {
                                        bw.write("New memory setting saved: " + userPreferences.getMemoryPreference() + "\n");
                                    }
                                } catch (Exception e) {
                                    javax.swing.JOptionPane.showMessageDialog(null, "PeptideShaker could not parse the memory setting:" + currentOption + ". The value was reset to" + userPreferences.getMemoryPreference() + ".", "PeptideShaker - Startup Failed", JOptionPane.WARNING_MESSAGE);
                                }
                            }
                        }
                    } else if (!currentOption.startsWith("#")) {
                        if (currentOption.startsWith("-Dhttp")) {
                            proxySettingsFound = true;
                            String[] tempProxySetting = currentOption.split("=");
                            if (tempProxySetting[0].equalsIgnoreCase("-Dhttp.proxyHost")) {
                                uniprotApiProperties += "proxy.host=" + tempProxySetting[1] + "\n";
                            } else if (tempProxySetting[0].equalsIgnoreCase("-Dhttp.proxyPort")) {
                                uniprotApiProperties += "proxy.port=" + tempProxySetting[1] + "\n";
                            } else if (tempProxySetting[0].equalsIgnoreCase("-Dhttp.proxyUser")) {
                                uniprotApiProperties += "username=" + tempProxySetting[1] + "\n";
                            } else if (tempProxySetting[0].equalsIgnoreCase("-Dhttp.proxyPassword")) {
                                uniprotApiProperties += "password=" + tempProxySetting[1] + "\n";
                            }
                        }
                        options += currentOption + " ";
                    }
                    currentOption = b.readLine();
                }
                if (proxySettingsFound) {
                    FileWriter uniprotProxyWriter = new FileWriter(uniprotApiPropertiesFile);
                    BufferedWriter uniprotProxyBufferedWriter = new BufferedWriter(uniprotProxyWriter);
                    uniprotProxyBufferedWriter.write(uniprotApiProperties);
                    uniprotProxyBufferedWriter.close();
                    uniprotProxyWriter.close();
                }
                b.close();
                f.close();
                options += "-Xmx" + userPreferences.getMemoryPreference() + "M";
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
                if (useStartUpLog) {
                    bw.write(ex.getMessage());
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                if (useStartUpLog) {
                    bw.write(ex.getMessage());
                }
            }
        } else {
            options = "-Xms128M -Xmx1024M";
        }
        String javaHome = System.getProperty("java.home") + File.separator + "bin" + File.separator;
        boolean usingStandardJavaHome = true;
        if (nonStandardJavaHome.exists()) {
            try {
                FileReader f = new FileReader(nonStandardJavaHome);
                BufferedReader b = new BufferedReader(f);
                String tempLocation = b.readLine();
                if (new File(tempLocation).exists() && (new File(tempLocation, "java.exe").exists() || new File(tempLocation, "java").exists())) {
                    javaHome = tempLocation;
                    usingStandardJavaHome = false;
                } else {
                    if (firstTry) {
                        JOptionPane.showMessageDialog(null, "Non-standard Java home location not found.\n" + "Using default Java home.", "Java Home Not Found!", JOptionPane.WARNING_MESSAGE);
                    }
                }
                b.close();
                f.close();
            } catch (FileNotFoundException ex) {
                if (firstTry) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Non-standard Java home location not found.\n" + "Using default Java home", "Java Home Not Found!", JOptionPane.WARNING_MESSAGE);
                }
            } catch (IOException ex) {
                if (firstTry) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error when reading non-standard Java home location.\n" + "Using default Java home.", "Java Home Error", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
        String quote = "";
        if (System.getProperty("os.name").lastIndexOf("Windows") != -1) {
            quote = "\"";
        }
        if (useStartUpLog) {
            bw.write("original java.home: " + javaHome + "\n");
        }
        if (usingStandardJavaHome && javaHome.lastIndexOf(" (x86)") != -1 && System.getProperty("os.name").lastIndexOf("Windows") != -1) {
            String tempJavaHome = javaHome.replaceAll(" \\(x86\\)", "");
            if (useStartUpLog) {
                bw.write("temp java.home: " + tempJavaHome + "\n");
            }
            if (new File(tempJavaHome).exists()) {
                javaHome = tempJavaHome;
            }
        }
        if (useStartUpLog) {
            bw.write("new java.home: " + javaHome + "\n");
        }
        String splashPath = path + "resources/conf/peptide-shaker-splash.png";
        if (System.getProperty("os.name").lastIndexOf("Windows") != -1) {
            splashPath = splashPath.replace("/", "\\");
            if (splashPath.startsWith("\\") && !splashPath.startsWith("\\\\")) {
                splashPath = splashPath.substring(1);
            }
        }
        String uniprotProxyClassPath = "";
        if (proxySettingsFound) {
            uniprotProxyClassPath = path + "resources/conf/proxy";
            if (System.getProperty("os.name").lastIndexOf("Windows") != -1) {
                uniprotProxyClassPath = uniprotProxyClassPath.replace("/", "\\");
                if (uniprotProxyClassPath.startsWith("\\") && !uniprotProxyClassPath.startsWith("\\\\")) {
                    uniprotProxyClassPath = uniprotProxyClassPath.substring(1);
                }
            }
            uniprotProxyClassPath = ";" + quote + uniprotProxyClassPath + quote;
        }
        cmdLine = javaHome + "java -splash:" + quote + splashPath + quote + " " + options + " -cp " + quote + new File(path, jarFileName).getAbsolutePath() + quote + uniprotProxyClassPath + " eu.isas.peptideshaker.gui.PeptideShakerGUI";
        if (useStartUpLog) {
            System.out.println("\n" + cmdLine + "\n\n");
            bw.write("\nCommand line: " + cmdLine + "\n\n");
        }
        try {
            Process p = Runtime.getRuntime().exec(cmdLine);
            InputStream stderr = p.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            String line = br.readLine();
            boolean error = false;
            while (line != null) {
                if (useStartUpLog) {
                    System.out.println(line);
                    bw.write(line + "\n");
                }
                temp += line + "\n";
                line = br.readLine();
                error = true;
            }
            int exitVal = p.waitFor();
            if (useStartUpLog) {
                System.out.println("Process exitValue: " + exitVal);
                bw.write("Process exitValue: " + exitVal + "\n");
            }
            if (error) {
                firstTry = false;
                if (temp.contains("Could not create the Java virtual machine")) {
                    if (userPreferences.getMemoryPreference() > 3 * 1024) {
                        userPreferences.setMemoryPreference(userPreferences.getMemoryPreference() - 1024);
                        saveNewSettings();
                        launch();
                    } else if (userPreferences.getMemoryPreference() > 1024) {
                        userPreferences.setMemoryPreference(userPreferences.getMemoryPreference() - 512);
                        saveNewSettings();
                        launch();
                    } else {
                        if (useStartUpLog) {
                            bw.write("Memory Limit:" + userPreferences.getMemoryPreference() + "\n");
                            bw.flush();
                            bw.close();
                        }
                        javax.swing.JOptionPane.showMessageDialog(null, "Failed to create the Java virtual machine.\n\n" + "Inspect the log file for details: resources/conf/startup.log.\n\n" + "Then go to Troubleshooting at http://peptide-shaker.googlecode.com.", "PeptideShaker - Startup Failed", JOptionPane.ERROR_MESSAGE);
                        System.exit(0);
                    }
                } else {
                    if (useStartUpLog) {
                        bw.flush();
                        bw.close();
                    }
                    javax.swing.JOptionPane.showMessageDialog(null, "An error occurred when starting PeptideShaker.\n\n" + "Inspect the log file for details: resources/conf/startup.log.\n\n" + "Then go to Troubleshooting at http://peptide-shaker.googlecode.com.", "PeptideShaker - Startup Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Loads the user preferences.
     */
    private void loadUserPreferences() {
        try {
            File file = new File(USER_PREFERENCES_FILE);
            if (!file.exists()) {
                userPreferences = new UserPreferences();
                saveUserPreferences();
            } else {
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream in = new ObjectInputStream(bis);
                Object inObject = in.readObject();
                fis.close();
                bis.close();
                in.close();
                userPreferences = (UserPreferences) inObject;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the new memory settings.
     */
    private void saveNewSettings() {
        saveUserPreferences();
        saveJavaOptions();
    }

    /**
     * Saves the user preferences.
     */
    private void saveUserPreferences() {
        try {
            File file = new File(USER_PREFERENCES_FILE);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdir();
            }
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(userPreferences);
            oos.close();
            bos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new javaOptions text file with the new settings.
     */
    private void saveJavaOptions() {
        String path = this.getClass().getResource("PeptideShakerWrapper.class").getPath();
        String currentLine, lines = "";
        path = path.substring(5, path.indexOf(jarFileName));
        path = path.replace("%20", " ");
        path = path.replace("%5b", "[");
        path = path.replace("%5d", "]");
        File javaOptions = new File(path + "resources/conf/JavaOptions.txt");
        if (javaOptions.exists()) {
            try {
                FileReader f = new FileReader(javaOptions);
                BufferedReader b = new BufferedReader(f);
                while ((currentLine = b.readLine()) != null) {
                    if (!currentLine.startsWith("-Xmx")) {
                        lines += currentLine + "\n";
                    }
                }
                b.close();
                f.close();
                FileWriter fw = new FileWriter(javaOptions);
                BufferedWriter bow = new BufferedWriter(fw);
                bow.write(lines);
                bow.write("-Xmx" + userPreferences.getMemoryPreference() + "M\n");
                bow.close();
                fw.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args
     */
    public static void main(String[] args) {
        new PeptideShakerWrapper();
    }
}
