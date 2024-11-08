package org.manentia.webploy.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.manentia.webploy.Constants;
import org.manentia.webploy.configuration.CheckPropertiesFile;
import org.manentia.webploy.configuration.ConfigFile;
import org.manentia.webploy.configuration.DatabaseField;
import org.manentia.webploy.configuration.InstallPropertiesFile;
import org.manentia.webploy.configuration.SelectionField;
import org.manentia.webploy.configuration.TextField;
import org.xml.sax.SAXException;
import com.manentia.commons.log.Log;

public class VerifyInstallationServlet extends HttpServlet {

    private static final long serialVersionUID = -313446276923096465L;

    private static final int DO_NOTHING = 0;

    private static final int INSTALL = 1;

    private static final int UPDATE = 2;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doIt(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doIt(req, resp);
    }

    private void doIt(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Log.write("Verifying if a install is needed", Log.INFO, "doIt", this.getClass());
        ConfigFile configFile = null;
        try {
            configFile = loadVerificationConfiguration();
        } catch (SAXException e) {
            throw new ServletException("Cannot load webploy.xml", e);
        }
        int whatToDo = DO_NOTHING;
        for (CheckPropertiesFile checkProperties : configFile.getVerification().getCheckPropertiesFile()) {
            whatToDo = verifyCheckPropertiesFile(checkProperties);
            if (whatToDo == INSTALL || whatToDo == UPDATE) {
                break;
            }
        }
        if (whatToDo == INSTALL) {
            try {
                req.getSession().setAttribute("configFile", loadInstallConfiguration());
            } catch (SAXException e) {
                throw new ServletException(e);
            } catch (ConfigurationException e) {
                throw new ServletException(e);
            }
            resp.sendRedirect(req.getContextPath() + "/webploy/install.jsp");
        } else if (whatToDo == UPDATE) {
            try {
                req.getSession().setAttribute("configFile", loadUpdateConfiguration());
            } catch (SAXException e) {
                throw new ServletException(e);
            } catch (ConfigurationException e) {
                throw new ServletException(e);
            }
            resp.sendRedirect(req.getContextPath() + "/webploy/update.jsp");
        } else {
            RequestDispatcher dispatcher = req.getRequestDispatcher("/index.jsp");
            dispatcher.forward(req, resp);
        }
    }

    private ConfigFile loadInstallConfiguration() throws IOException, SAXException, ConfigurationException {
        Log.write("Loading installation information from webploy.xml", Log.INFO, "<init>", this.getClass());
        InputStream input = this.getServletContext().getResourceAsStream("/WEB-INF/webploy.xml");
        Digester digester = new Digester();
        digester.setNamespaceAware(false);
        digester.addObjectCreate("webploy", "org.manentia.webploy.configuration.ConfigFile");
        digester.addSetProperties("webploy");
        digester.addObjectCreate("webploy/install", "org.manentia.webploy.configuration.Install");
        digester.addSetProperties("webploy/install");
        digester.addSetNext("webploy/install", "setInstall", "org.manentia.webploy.configuration.Install");
        digester.addObjectCreate("webploy/install/installPropertiesFile", "org.manentia.webploy.configuration.InstallPropertiesFile");
        digester.addSetProperties("webploy/install/installPropertiesFile");
        digester.addSetNext("webploy/install/installPropertiesFile", "addInstallPropertiesFile", "org.manentia.webploy.configuration.InstallPropertiesFile");
        digester.addObjectCreate("webploy/install/installPropertiesFile/text", "org.manentia.webploy.configuration.TextField");
        digester.addSetProperties("webploy/install/installPropertiesFile/text");
        digester.addSetNext("webploy/install/installPropertiesFile/text", "addTextField", "org.manentia.webploy.configuration.TextField");
        digester.addObjectCreate("webploy/install/installPropertiesFile/text/regexpValidation", "org.manentia.webploy.configuration.RegexpValidation");
        digester.addSetProperties("webploy/install/installPropertiesFile/text/regexpValidation");
        digester.addSetNext("webploy/install/installPropertiesFile/text/regexpValidation", "addRegexpValidation", "org.manentia.webploy.configuration.RegexpValidation");
        digester.addObjectCreate("webploy/install/installPropertiesFile/database", "org.manentia.webploy.configuration.DatabaseField");
        digester.addSetProperties("webploy/install/installPropertiesFile/database");
        digester.addSetNext("webploy/install/installPropertiesFile/database", "addDatabaseField", "org.manentia.webploy.configuration.DatabaseField");
        digester.addObjectCreate("webploy/install/installPropertiesFile/selection", "org.manentia.webploy.configuration.SelectionField");
        digester.addSetProperties("webploy/install/installPropertiesFile/selection");
        digester.addSetNext("webploy/install/installPropertiesFile/selection", "addSelectionField", "org.manentia.webploy.configuration.SelectionField");
        digester.addObjectCreate("webploy/install/installPropertiesFile/selection/option", "org.manentia.webploy.configuration.SelectionOption");
        digester.addSetProperties("webploy/install/installPropertiesFile/selection/option");
        digester.addSetNext("webploy/install/installPropertiesFile/selection/option", "addOption", "org.manentia.webploy.configuration.SelectionOption");
        digester.addObjectCreate("webploy/install/createDatabase", "org.manentia.webploy.configuration.CreateDatabase");
        digester.addSetProperties("webploy/install/createDatabase");
        digester.addSetNext("webploy/install/createDatabase", "addCreateDatabase", "org.manentia.webploy.configuration.CreateDatabase");
        digester.addObjectCreate("webploy/install/createDatabase/executeScript", "org.manentia.webploy.configuration.ExecuteScript");
        digester.addSetProperties("webploy/install/createDatabase/executeScript");
        digester.addSetNext("webploy/install/createDatabase/executeScript", "addExecuteScript", "org.manentia.webploy.configuration.ExecuteScript");
        digester.addObjectCreate("webploy/install/license", "org.manentia.webploy.configuration.License");
        digester.addSetProperties("webploy/install/license");
        digester.addSetNext("webploy/install/license", "setLicense", "org.manentia.webploy.configuration.License");
        ConfigFile configFile = (ConfigFile) digester.parse(input);
        input.close();
        Log.write("Verification installation loaded successfully from webploy.xml", Log.INFO, "<init>", this.getClass());
        for (InstallPropertiesFile installPropertiesFile : configFile.getInstall().getInstallPropertiesFileList()) {
            loadDefaultValues(installPropertiesFile);
        }
        Log.write("Installation info:" + configFile.toString(), Log.INFO, "<init>", this.getClass());
        return configFile;
    }

    private ConfigFile loadUpdateConfiguration() throws IOException, SAXException, ConfigurationException {
        Log.write("Loading update information from webploy.xml", Log.INFO, "<init>", this.getClass());
        InputStream input = this.getServletContext().getResourceAsStream("/WEB-INF/webploy.xml");
        Digester digester = new Digester();
        digester.setNamespaceAware(false);
        digester.addObjectCreate("webploy", "org.manentia.webploy.configuration.ConfigFile");
        digester.addSetProperties("webploy");
        digester.addObjectCreate("webploy/update", "org.manentia.webploy.configuration.Update");
        digester.addSetProperties("webploy/update");
        digester.addSetNext("webploy/update", "setUpdate", "org.manentia.webploy.configuration.Update");
        digester.addObjectCreate("webploy/update/installPropertiesFile", "org.manentia.webploy.configuration.InstallPropertiesFile");
        digester.addSetProperties("webploy/update/installPropertiesFile");
        digester.addSetNext("webploy/update/installPropertiesFile", "addInstallPropertiesFile", "org.manentia.webploy.configuration.InstallPropertiesFile");
        digester.addObjectCreate("webploy/update/installPropertiesFile/text", "org.manentia.webploy.configuration.TextField");
        digester.addSetProperties("webploy/update/installPropertiesFile/text");
        digester.addSetNext("webploy/update/installPropertiesFile/text", "addTextField", "org.manentia.webploy.configuration.TextField");
        digester.addObjectCreate("webploy/update/installPropertiesFile/text/regexpValidation", "org.manentia.webploy.configuration.RegexpValidation");
        digester.addSetProperties("webploy/update/installPropertiesFile/text/regexpValidation");
        digester.addSetNext("webploy/update/installPropertiesFile/text/regexpValidation", "addRegexpValidation", "org.manentia.webploy.configuration.RegexpValidation");
        digester.addObjectCreate("webploy/update/installPropertiesFile/database", "org.manentia.webploy.configuration.DatabaseField");
        digester.addSetProperties("webploy/update/installPropertiesFile/database");
        digester.addSetNext("webploy/update/installPropertiesFile/database", "addDatabaseField", "org.manentia.webploy.configuration.DatabaseField");
        digester.addObjectCreate("webploy/update/installPropertiesFile/selection", "org.manentia.webploy.configuration.SelectionField");
        digester.addSetProperties("webploy/update/installPropertiesFile/selection");
        digester.addSetNext("webploy/update/installPropertiesFile/selection", "addSelectionField", "org.manentia.webploy.configuration.SelectionField");
        digester.addObjectCreate("webploy/update/installPropertiesFile/selection/option", "org.manentia.webploy.configuration.SelectionOption");
        digester.addSetProperties("webploy/update/installPropertiesFile/selection/option");
        digester.addSetNext("webploy/update/installPropertiesFile/selection/option", "addOption", "org.manentia.webploy.configuration.SelectionOption");
        digester.addObjectCreate("webploy/update/updateDatabase", "org.manentia.webploy.configuration.UpdateDatabase");
        digester.addSetProperties("webploy/update/updateDatabase");
        digester.addSetNext("webploy/update/updateDatabase", "addUpdateDatabase", "org.manentia.webploy.configuration.UpdateDatabase");
        digester.addObjectCreate("webploy/update/updateDatabase/executeScript", "org.manentia.webploy.configuration.ExecuteScript");
        digester.addSetProperties("webploy/update/updateDatabase/executeScript");
        digester.addSetNext("webploy/update/updateDatabase/executeScript", "addExecuteScript", "org.manentia.webploy.configuration.ExecuteScript");
        digester.addObjectCreate("webploy/update/license", "org.manentia.webploy.configuration.License");
        digester.addSetProperties("webploy/update/license");
        digester.addSetNext("webploy/update/license", "setLicense", "org.manentia.webploy.configuration.License");
        ConfigFile configFile = (ConfigFile) digester.parse(input);
        input.close();
        Log.write("Update info loaded successfully from webploy.xml", Log.INFO, "<init>", this.getClass());
        for (InstallPropertiesFile installPropertiesFile : configFile.getUpdate().getInstallPropertiesFileList()) {
            loadDefaultValues(installPropertiesFile);
        }
        Log.write("Installation info:" + configFile.toString(), Log.INFO, "<init>", this.getClass());
        return configFile;
    }

    /**
	 * Loads the default values, contained in the template file, onto the installPropertiesFile object
	 * 
	 * @param installPropertiesFile
	 * @throws ConfigurationException
	 * @throws MalformedURLException
	 */
    private void loadDefaultValues(InstallPropertiesFile installPropertiesFile) throws ConfigurationException, MalformedURLException {
        PropertiesConfiguration prop = new PropertiesConfiguration(this.getServletContext().getResource(installPropertiesFile.getTemplate()));
        for (TextField textField : installPropertiesFile.getTextFields()) {
            textField.setDefaultValue(prop.getString(textField.getProperty()));
        }
        for (SelectionField selectionField : installPropertiesFile.getSelectionFields()) {
            selectionField.setDefaultValue(prop.getString(selectionField.getProperty()));
        }
        for (DatabaseField databaseField : installPropertiesFile.getDatabaseFields()) {
            databaseField.setDriverDefaultValue(prop.getString(databaseField.getDriverProperty()));
            databaseField.setUrlDefaultValue(prop.getString(databaseField.getUrlProperty()));
            databaseField.setUserDefaultValue(prop.getString(databaseField.getUserProperty()));
            databaseField.setPasswordDefaultValue(prop.getString(databaseField.getPasswordProperty()));
        }
    }

    private ConfigFile loadVerificationConfiguration() throws IOException, SAXException {
        Log.write("Loading verification information from webploy.xml", Log.INFO, "loadVerificationConfiguration", this.getClass());
        InputStream input = this.getServletContext().getResourceAsStream("/WEB-INF/webploy.xml");
        Digester digester = new Digester();
        digester.setNamespaceAware(false);
        digester.addObjectCreate("webploy", "org.manentia.webploy.configuration.ConfigFile");
        digester.addSetProperties("webploy");
        digester.addObjectCreate("webploy/verification", "org.manentia.webploy.configuration.Verification");
        digester.addSetProperties("webploy/verification");
        digester.addSetNext("webploy/verification", "setVerification", "org.manentia.webploy.configuration.Verification");
        digester.addObjectCreate("webploy/verification/checkPropertiesFile", "org.manentia.webploy.configuration.CheckPropertiesFile");
        digester.addSetProperties("webploy/verification/checkPropertiesFile");
        digester.addSetNext("webploy/verification/checkPropertiesFile", "addCheckPropertiesFile", "org.manentia.webploy.configuration.CheckPropertiesFile");
        ConfigFile configFile = (ConfigFile) digester.parse(input);
        input.close();
        Log.write("Verification information loaded successfully from webploy.xml", Log.INFO, "loadVerificationConfiguration", this.getClass());
        return configFile;
    }

    /**
	 * Determines if a installation or upgrade is needed, depending on the validation configuration and installed version
	 * 
	 * @param checkPropertiesFile
	 * @return VerifyInstallationServlet.INSTALL if a install is needed, VerifyInstallationServlet.UPDATE is an upgrade is needed, or VerifyInstallationServlet.DO_NOTHING in other case 
	 */
    private int verifyCheckPropertiesFile(CheckPropertiesFile checkPropertiesFile) {
        int result = INSTALL;
        try {
            URL propURL = ConfigurationUtils.locate(checkPropertiesFile.getResourceName());
            PropertiesConfiguration prop = new PropertiesConfiguration(propURL);
            Log.write("There is a configuration file", Log.INFO, "verifyCheckPropertiesFile", this.getClass());
            if (StringUtils.isNotEmpty(checkPropertiesFile.getProperty()) && StringUtils.isNotEmpty(checkPropertiesFile.getValueToReinstall())) {
                Log.write(checkPropertiesFile.getProperty() + "=" + prop.getString(checkPropertiesFile.getProperty()), Log.INFO, "verifyCheckPropertiesFile", this.getClass());
            }
            if (!prop.getString(checkPropertiesFile.getProperty()).equalsIgnoreCase(checkPropertiesFile.getValueToReinstall())) {
                Log.write("The " + checkPropertiesFile.getResourceName() + " properties file doesn't want to reinstall", Log.INFO, "verifyCheckPropertiesFile", this.getClass());
                result = DO_NOTHING;
                VersionNumber installedVersion = new VersionNumber(prop.getString(Constants.INSTALLED_VERSION_PROPERTY, "0.0.0"));
                PropertiesConfiguration templateProp = new PropertiesConfiguration(this.getServletContext().getResource(checkPropertiesFile.getUpdateTemplate()));
                VersionNumber updateVersion = new VersionNumber(templateProp.getString(Constants.INSTALLED_VERSION_PROPERTY, "0.0.0"));
                if (installedVersion.toLong() < updateVersion.toLong()) {
                    result = UPDATE;
                }
            }
        } catch (Exception e) {
            Log.write("No properties file, or it cannot be read (" + ExceptionUtils.getFullStackTrace(e) + ")", Log.INFO, "verifyCheckPropertiesFile", this.getClass());
        }
        return result;
    }

    /**
	 * @author Ricardo Zuasti
	 * Represents a three part version number (ie 1.0.0, optionally with sub-minor, ie 1.0.0_001) and implements a decimalization 
	 * for it that allows int based comparisons
	 */
    private class VersionNumber {

        int major;

        int mid;

        int minor;

        int subMinor;

        public VersionNumber(String versionText) {
            if (versionText.indexOf("_") != -1) {
                subMinor = Integer.parseInt(versionText.substring(versionText.indexOf("_") + 1));
                versionText = versionText.substring(0, versionText.indexOf("_"));
            }
            StringTokenizer tok = new StringTokenizer(versionText, ".");
            major = Integer.parseInt(tok.nextToken());
            mid = Integer.parseInt(tok.nextToken());
            minor = Integer.parseInt(tok.nextToken());
        }

        public long toLong() {
            return (major * 1000000000L) + (mid * 1000000L) + (minor * 1000L) + subMinor;
        }
    }
}
