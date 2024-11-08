package org.manentia.webploy.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.manentia.webploy.Constants;
import org.manentia.webploy.Util;
import org.manentia.webploy.configuration.ConfigFile;
import org.manentia.webploy.configuration.CreateDatabase;
import org.manentia.webploy.configuration.DatabaseField;
import org.manentia.webploy.configuration.ExecuteScript;
import org.manentia.webploy.configuration.InstallPropertiesFile;
import org.manentia.webploy.configuration.SelectionField;
import org.manentia.webploy.configuration.TextField;
import com.manentia.commons.log.Log;

public class InstallFacade extends BaseFacade {

    public InstallFacade() {
    }

    public Map<String, InstallationStepResult> install(Map<String, String> installParameters) throws URISyntaxException, FileNotFoundException, IOException, ConfigurationException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        WebContext ctx = WebContextFactory.get();
        Map<String, InstallationStepResult> result = new LinkedHashMap<String, InstallationStepResult>();
        result.put("Generate zone key", new InstallationStepResult());
        result.put("Install properties files", new InstallationStepResult());
        result.put("Execute database scripts", new InstallationStepResult());
        ConfigFile configFile = (ConfigFile) ctx.getSession().getAttribute("configFile");
        String zoneKeyPath = installParameters.get("zoneKeyPath");
        if (configFile.getInstall().isRequiresZoneKey()) {
            if (StringUtils.isEmpty(zoneKeyPath)) {
                throw new ConfigurationException("You must specify a path for the application zone key file");
            }
            Util.generateZoneKey(zoneKeyPath);
            result.get("Generate zone key").setSuccessful(true);
        }
        result.remove("Install properties files");
        for (InstallPropertiesFile installPropertiesFile : configFile.getInstall().getInstallPropertiesFileList()) {
            result.put("Install " + installPropertiesFile.getInstalledFileName(), new InstallationStepResult());
            Map<String, String> parameters = new HashMap<String, String>();
            String configPath = installParameters.get(installPropertiesFile.getId() + "|configPath");
            if (StringUtils.isEmpty(configPath)) {
                configPath = installParameters.get(installPropertiesFile.getId() + "|otherConfigPath");
            }
            for (SelectionField selectionField : installPropertiesFile.getSelectionFields()) {
                String value = installParameters.get(installPropertiesFile.getId() + "|select" + selectionField.getId());
                if (selectionField.isEncrypted()) {
                    value = Util.encrypt(zoneKeyPath, value);
                }
                parameters.put(selectionField.getProperty(), value);
            }
            for (TextField textField : installPropertiesFile.getTextFields()) {
                String value = installParameters.get(installPropertiesFile.getId() + "|text" + textField.getId());
                String validation = validateTextField(textField, value);
                if (StringUtils.isNotEmpty(validation)) {
                    throw new ConfigurationException(validation);
                }
                if (textField.isEncrypted()) {
                    value = Util.encrypt(zoneKeyPath, value);
                }
                parameters.put(textField.getProperty(), value);
            }
            for (DatabaseField databaseField : installPropertiesFile.getDatabaseFields()) {
                String driver = installParameters.get(installPropertiesFile.getId() + "|db|driver" + databaseField.getId());
                String url = installParameters.get(installPropertiesFile.getId() + "|db|url" + databaseField.getId());
                String user = installParameters.get(installPropertiesFile.getId() + "|db|username" + databaseField.getId());
                String password = installParameters.get(installPropertiesFile.getId() + "|db|password" + databaseField.getId());
                if (databaseField.isEncryptPassword()) {
                    password = Util.encrypt(zoneKeyPath, password);
                }
                parameters.put(databaseField.getDriverProperty(), driver);
                parameters.put(databaseField.getUrlProperty(), url);
                parameters.put(databaseField.getUserProperty(), user);
                parameters.put(databaseField.getPasswordProperty(), password);
            }
            if (installPropertiesFile.isIncludeZoneKeyPath()) {
                parameters.put(Constants.ZONE_KEY_PATH_PROPERTY, zoneKeyPath);
            }
            if (installPropertiesFile.isIncludeConfigurationPassword()) {
                String configurationPassword = installParameters.get(installPropertiesFile.getId() + "|configurationPassword");
                if (StringUtils.isEmpty(configurationPassword)) {
                    throw new ConfigurationException("You must specify a configuration access password.");
                }
                parameters.put(Constants.CONFIGURATION_PASSWORD_PROPERTY, Util.encrypt(zoneKeyPath, configurationPassword));
            }
            deployPropertiesFile(installPropertiesFile, configPath, parameters);
            result.get("Install " + installPropertiesFile.getInstalledFileName()).setSuccessful(true);
        }
        result.remove("Execute database scripts");
        for (CreateDatabase createDatabase : configFile.getInstall().getCreateDatabaseList()) {
            String driver = installParameters.get(createDatabase.getPropertiesFileId() + "|db|driver" + createDatabase.getFieldId());
            String url = installParameters.get(createDatabase.getPropertiesFileId() + "|db|url" + createDatabase.getFieldId());
            String user = installParameters.get(createDatabase.getPropertiesFileId() + "|db|username" + createDatabase.getFieldId());
            String password = installParameters.get(createDatabase.getPropertiesFileId() + "|db|password" + createDatabase.getFieldId());
            if (StringUtils.isNotEmpty(installParameters.get("createDatabase|username" + createDatabase.getFieldId()))) {
                user = installParameters.get("createDatabase|username" + createDatabase.getFieldId());
            }
            if (StringUtils.isNotEmpty(installParameters.get("createDatabase|password" + createDatabase.getFieldId()))) {
                password = installParameters.get("createDatabase|password" + createDatabase.getFieldId());
            }
            for (ExecuteScript executeScript : createDatabase.getExecuteScriptList()) {
                result.put("Execute database script " + executeScript.getPath(), new InstallationStepResult());
                Util.runSQLScript(executeScript.getPath(), driver, url, user, password);
                result.get("Execute database script " + executeScript.getPath()).setSuccessful(true);
            }
        }
        return result;
    }

    private void deployPropertiesFile(InstallPropertiesFile installPropertiesFile, String configPath, Map<String, String> parameters) throws FileNotFoundException, IOException, ConfigurationException {
        Log.write("ENTER - Deploying " + installPropertiesFile.getInstalledFileName() + " to " + configPath, Log.INFO, "deployPropertiesFile", this.getClass());
        WebContext ctx = WebContextFactory.get();
        InputStream configStream = ctx.getServletContext().getResourceAsStream(installPropertiesFile.getTemplate());
        if (configStream != null) {
            Util.copyStreamToFile(configStream, configPath + "/" + installPropertiesFile.getInstalledFileName());
        } else {
            Log.write("Configuration file " + configPath + "/" + installPropertiesFile.getInstalledFileName() + " not found on WAR or cannot be read", Log.WARN, "deployPropertiesFile", this.getClass());
            throw new FileNotFoundException("Configuration file " + installPropertiesFile.getTemplate() + " not found on WAR or cannot be read");
        }
        PropertiesConfiguration propConfiguration = new PropertiesConfiguration(configPath + "/" + installPropertiesFile.getInstalledFileName());
        for (String property : parameters.keySet()) {
            String value = parameters.get(property);
            Log.write("Setting " + property + "=" + value, Log.INFO, "deployPropertiesFile", this.getClass());
            propConfiguration.setProperty(property, value);
        }
        propConfiguration.save();
        Log.write("EXIT", Log.INFO, "deployPropertiesFile", this.getClass());
    }

    public List<String> getPossibleConfigPaths() {
        Log.write("ENTER", Log.INFO, "getPossibleConfigPaths", this.getClass());
        ArrayList<String> result = new ArrayList<String>();
        Properties p = System.getProperties();
        String catalinaBase = p.getProperty("catalina.home");
        String sharedLoader = p.getProperty("shared.loader");
        Log.write("catalinaBase = " + catalinaBase, Log.INFO, "getPossibleConfigPaths", this.getClass());
        Log.write("sharedLoader = " + sharedLoader, Log.INFO, "getPossibleConfigPaths", this.getClass());
        if (p.getProperty("catalina.base") != null) {
            if (sharedLoader != null) {
                sharedLoader = StringUtils.replace(sharedLoader, "${catalina.base}", catalinaBase);
                sharedLoader = StringUtils.replace(sharedLoader, "${catalina.home}", catalinaBase);
                StringTokenizer tok = new StringTokenizer(sharedLoader, ",");
                while (tok.hasMoreTokens()) {
                    String entry = tok.nextToken();
                    Log.write("Evaluating " + entry, Log.INFO, "getPossibleConfigPaths", this.getClass());
                    File f = new File(entry);
                    if (f.isDirectory()) {
                        result.add(entry);
                        Log.write("Adding " + entry, Log.INFO, "getPossibleConfigPaths", this.getClass());
                    }
                }
            }
        }
        Log.write("EXIT", Log.INFO, "getPossibleConfigPaths", this.getClass());
        return result;
    }

    public Map<String, String> testTextField(String installPropertiesFileId, String textFieldId, String value, String itemId) {
        WebContext ctx = WebContextFactory.get();
        ConfigFile configFile = (ConfigFile) ctx.getSession().getAttribute("configFile");
        Map<String, String> result = new HashMap<String, String>();
        for (InstallPropertiesFile installPropertiesFile : configFile.getInstall().getInstallPropertiesFileList()) {
            if (installPropertiesFile.getId().equals(installPropertiesFileId)) {
                for (TextField textField : installPropertiesFile.getTextFields()) {
                    if (textField.getId().equals(textFieldId)) {
                        String validationResult = validateTextField(textField, value);
                        if (StringUtils.isEmpty(validationResult)) {
                            result.put("result", "true");
                            result.put("reason", "");
                        } else {
                            result.put("result", "false");
                            result.put("reason", validationResult);
                        }
                    }
                }
            }
        }
        result.put("itemId", itemId);
        result.put("installPropertiesFileId", installPropertiesFileId);
        return result;
    }
}
