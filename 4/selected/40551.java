package de.sopra.controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.Properties;
import de.sopra.exceptions.ConfigFileIOException;
import de.sopra.exceptions.InvalidDefaultConfigFileException;

/**
 * This class handles the access to the configuration file of TOM. It uses the
 * singleton pattern so an instance may only be gotten via getInstance().
 *
 * @author Falko K&ouml;tter
 */
public class ConfigurationParser {

    /**
     * This char indicates that a line contains a comment.
     */
    private static final char COMMENT_CHAR = '#';

    /**
     * A <code>{@link String}</code> constant containing the path to the
     * config file.
     */
    public static final String CONFIG_PATH = "tom.cfg";

    /**
     * The version of the configuration file that will be accepted.
     */
    public static final int CONFIG_VERSION = 1;

    /**
     * A <code>{@link String}</code> constant containing the path to the
     * default config file.
     */
    public static final String DEFAULT_CONFIG_PATH = "/resources/config/std_tom.cfg";

    /**
     * A <code>{@link File}</code> constant containing the default config
     * file.
     */
    private static final InputStream DEFAULT_CONFIG = ConfigurationParser.class.getResourceAsStream(DEFAULT_CONFIG_PATH);

    /**
     * The syntax for the last opened folder.
     */
    private static final String LASTOPENED_FOLDER = "lastopened.folder=";

    /**
     * Due to singleton pattern only one <code>ConfigurationParser</code> may
     * exist at a time.
     */
    private static ConfigurationParser theInstance;

    /**
     * @return A configuration parser
     */
    public static ConfigurationParser getInstance() {
        if (theInstance == null) {
            theInstance = new ConfigurationParser();
        }
        return theInstance;
    }

    /**
     * The config file.
     */
    private File confFile;

    /**
     * The configuration.
     */
    private Properties configProperties;

    /**
     * Private constructor due to singleton pattern.
     */
    private ConfigurationParser() {
        initConfig();
    }

    /**
     * Overwrites the config file with the standard config file.
     */
    private void copyConfFromStandard() {
        BufferedWriter writeStream = null;
        BufferedReader stdStream = null;
        try {
            String readLine;
            confFile.createNewFile();
            stdStream = new BufferedReader(new InputStreamReader(ConfigurationParser.class.getResourceAsStream(DEFAULT_CONFIG_PATH)));
            writeStream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(confFile)));
            readLine = stdStream.readLine();
            while (readLine != null) {
                if (readLine.length() > 0 && readLine.charAt(0) != COMMENT_CHAR) {
                    writeStream.write(readLine);
                    writeStream.newLine();
                }
                readLine = stdStream.readLine();
            }
        } catch (IOException e) {
            throw new ConfigFileIOException("Config file cannot be written.", e);
        } catch (NullPointerException e) {
            throw new InvalidDefaultConfigFileException("The default configuration file is missing" + "\nYour version of TOM is corrupt.", e);
        } finally {
            try {
                stdStream.close();
            } catch (Exception e) {
            }
            try {
                writeStream.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * @param key
     *            The name of the desired property.
     * @return The property of the config file. If this property doesn't exist
     *         in the file it will be taken from default file instead.
     */
    public String getProperty(String key) {
        return configProperties.getProperty(key);
    }

    /**
     * Reads the config file and the default config file. If no config file
     * exists a new config file with the attributes of the default config file
     * will be created.
     */
    private void initConfig() {
        Properties defaultProperty = new Properties();
        InputStream config = null;
        boolean goodVersion;
        confFile = new File(CONFIG_PATH);
        if (!confFile.exists()) {
            copyConfFromStandard();
        }
        configProperties = new Properties(defaultProperty);
        try {
            defaultProperty.load(DEFAULT_CONFIG);
        } catch (IOException e) {
            throw new InvalidDefaultConfigFileException();
        } catch (NullPointerException e) {
            throw new InvalidDefaultConfigFileException();
        }
        try {
            config = new FileInputStream(confFile);
        } catch (FileNotFoundException e) {
            throw new ConfigFileIOException("Config file cannot be opened.", e);
        }
        goodVersion = checkVersion();
        try {
            configProperties.load(config);
        } catch (IOException e) {
            throw new ConfigFileIOException("Illegal config file.", e);
        } finally {
            try {
                config.close();
            } catch (Exception ex) {
            }
        }
        if (!goodVersion) {
            throw new ConfigFileIOException("There was a severe problem in the current config file," + " so we replaced it with the default.");
        }
    }

    /**
     * Makes sure the config file version is correct.
     *
     * @return <code>true</code>, if the version is fine or appropriate
     *         measures could be taken, <code>false</code> if something is
     *         seriously wrong.
     */
    private boolean checkVersion() {
        Properties checkProperty = new Properties();
        FileInputStream checkedStream = null;
        try {
            checkedStream = new FileInputStream(confFile);
            checkProperty.load(checkedStream);
        } catch (FileNotFoundException e) {
            throw new ConfigFileIOException("Config file cannot be opened.", e);
        } catch (IllegalArgumentException e) {
            copyConfFromStandard();
            return false;
        } catch (IOException e) {
            throw new ConfigFileIOException("Illegal config file.", e);
        } finally {
            try {
                checkedStream.close();
            } catch (Exception e) {
            }
        }
        if (checkProperty.getProperty("version") == null) {
            copyConfFromStandard();
            return false;
        }
        try {
            int version = new Integer(checkProperty.getProperty("version"));
            if (version != CONFIG_VERSION) {
                if (version > CONFIG_VERSION) {
                    System.err.println("WARNING: Your configuration file is from " + "a version of TOM newer that this version." + "\n\tIf you want all options included, please" + " remove/rename the current configuration file.");
                } else {
                    System.err.println("WARNING: Your configuration file is outdated." + "\n\tIf you want all options included, please" + " remove/rename the current configuration file.");
                }
            }
        } catch (NumberFormatException e) {
            throw new ConfigFileIOException("Config file version is not an integer.", e);
        }
        return true;
    }

    /**
     * Saves alle changes to the configuration in the configuration file. The
     * layout of the file will be preserved. Missing attributes which have been
     * set will be appended to the end of the file.
     */
    public void saveConfig() {
        LinkedList<String> stringList = new LinkedList<String>();
        BufferedReader inStream = null;
        BufferedWriter writeStream = null;
        String currLine;
        if (!confFile.exists()) {
            copyConfFromStandard();
        }
        try {
            inStream = new BufferedReader(new FileReader(confFile));
        } catch (FileNotFoundException e) {
            throw new ConfigFileIOException("Config file cannot be opened.", e);
        }
        try {
            currLine = inStream.readLine();
            while (currLine != null) {
                stringList.add(currLine);
                currLine = inStream.readLine();
            }
        } catch (IOException o) {
            throw new ConfigFileIOException("Config file cannot be read.", o);
        } finally {
            try {
                inStream.close();
            } catch (Exception e) {
            }
        }
        try {
            writeStream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(confFile)));
        } catch (FileNotFoundException e) {
            throw new ConfigFileIOException("Config file cannot be written.", e);
        }
        try {
            boolean found = false;
            for (String string : stringList) {
                if (!found && string.startsWith(LASTOPENED_FOLDER)) {
                    writeStream.write(LASTOPENED_FOLDER + Controller.getInstance().lastOpenedFolder.replace("\\", "\\\\"));
                    writeStream.newLine();
                    found = true;
                } else {
                    writeStream.write(string);
                    writeStream.newLine();
                }
            }
            if (!found) {
                writeStream.write(LASTOPENED_FOLDER + Controller.getInstance().lastOpenedFolder);
                writeStream.newLine();
            }
        } catch (IOException e) {
            throw new ConfigFileIOException("Config file cannot be written.", e);
        } finally {
            try {
                writeStream.close();
            } catch (Exception e) {
            }
        }
    }
}
