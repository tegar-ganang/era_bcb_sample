package org.cilogon.config.cli;

import edu.uiuc.ncsa.security.rdf.MyThing;
import edu.uiuc.ncsa.security.rdf.storage.*;
import edu.uiuc.ncsa.security.util.configuration.CLITools;
import edu.uiuc.ncsa.security.core.exceptions.NotImplementedException;
import org.cilogon.rdf.CILogonConfiguration;
import org.tupeloproject.kernel.OperatorException;
import org.tupeloproject.rdf.Resource;
import static edu.uiuc.ncsa.security.util.configuration.CLITools.prompt;
import static edu.uiuc.ncsa.security.util.configuration.CLITools.say;

/**
 * Command line tool to make/manage configurations.
 * <p>Created by Jeff Gaynor<br>
 * on Jul 9, 2010 at  3:40:10 PM
 */
public abstract class AbstractConfigTool {

    /**
     * For use when no choice made
     */
    public static final String NONE = "none";

    public static final String HELP = "-help";

    public static int MAX_TRIES = 5;

    protected static boolean askedForHelp(String[] args) {
        if (args.length != 0) {
            for (String arg : args) {
                if (arg.toLowerCase().contains(HELP)) {
                    printHelp();
                    return true;
                }
            }
        }
        return false;
    }

    protected static void printHelp() {
        say("A tool for writing the configuration files. Command line options are:");
        say("  -help = prints this message");
        say("  filename = complete path to an existing configuration file. This will be loaded (optional)");
        say("  targetfile = file to which the configuration will be written. If omitted, the configuration will end up in 'filename'");
        say("Exiting...");
    }

    /**
     * Convenience. Don't set the value unless it is not null.
     *
     * @param myThing
     * @param predicate
     * @param promptString
     * @throws OperatorException
     */
    protected void setModelValue(MyThing myThing, Resource predicate, String promptString) throws OperatorException {
        String temp = prompt(promptString, myThing.getValue(predicate));
        if (temp != null && temp.length() != 0) {
            myThing.setValue(predicate, temp);
        }
    }

    protected void doIt(String[] args, CILogonConfigurationDepot configuration) throws Exception {
        String source = null, target = null;
        switch(args.length) {
            case 2:
                target = args[1];
            case 1:
                source = args[0];
            default:
        }
        if (source == null) {
            source = CLITools.prompt("Enter the full path to your configuration file", NONE);
            if (!source.equals(NONE)) {
                target = source;
            }
        }
        boolean isConfigured = false;
        if (source != null && 0 < source.length() && !source.equals("none")) {
            configuration.deserialize(source);
            isConfigured = true;
        }
        doConfig(configuration, isConfigured);
        String doAgain = "y";
        if (target == null || target.length() == 0) {
            target = CLITools.prompt("Enter the name of the file to which I should write the configuration", NONE);
        } else {
            doAgain = CLITools.prompt("I'm getting ready to write the configuration file. Proceed? (y/n)", "y");
        }
        while (doAgain.equals("y")) {
            configuration.save();
            try {
                configuration.serialize(target);
                doAgain = "n";
            } catch (Throwable t) {
                doAgain = CLITools.prompt("Well that didn't work:\"" + t.getMessage() + "\". Try again? (y/n)", "n");
                if (doAgain.equals("y")) {
                    target = CLITools.prompt("Enter the name of the file to which I should write the configuration", target);
                }
            }
            say("done!");
        }
    }

    protected void doConfig(CILogonConfigurationDepot configuration, boolean isConfigured) throws Exception {
        CILogonConfiguration root = null;
        if (isConfigured) {
            root = configuration.getCurrentConfiguration();
        } else {
            root = configuration.createRoot(CLITools.prompt("Enter a name for this configuration", "default"));
            configuration.setRoot(root);
        }
        configureStore(configuration, root, isConfigured);
    }

    protected abstract void configureStore(CILogonConfigurationDepot configuration, CILogonConfiguration root, boolean isConfigured) throws OperatorException;

    protected void configureTable(TableModel tableModel, String displayName) throws OperatorException {
        tableModel.setName(CLITools.prompt("Name of the " + displayName + " table", tableModel.getName()));
        tableModel.setPrefix(CLITools.prompt("prefix for the " + displayName + " table", tableModel.getPrefix()));
        if (tableModel.getColumns() != null && CLITools.prompt("Do you want to edit the columns (advanced!!)? (y/n/)", "n").equals("y")) {
            for (ColumnModel c : tableModel.getColumns()) {
                c.setName(CLITools.prompt("current name", c.getName()));
            }
        }
    }

    /**
     * Allows you to edit, delete or create a configuration.
     */
    protected org.cilogon.rdf.CILogonConfiguration manageConfigurations(CILogonConfigurationDepot configuration) throws OperatorException {
        throw new NotImplementedException("Implement me!");
    }

    protected void setupDatabaseConnections(CILogonConfiguration configuration, SQLStoreModel sqlStoreModel, boolean isConfigured) throws OperatorException {
        say("We now are going to set up the connection arguments for the user who runs the server.");
        say("NOTE: All passwords will be displayed in clear text on the screen!");
        ConnectionParametersModel cpm;
        if (!isConfigured) {
            cpm = configuration.getConnectionParametersModel();
            sqlStoreModel.setConnectionParameters(cpm);
        } else {
            cpm = sqlStoreModel.getConnectionParametersModel();
        }
        cpm.setUserName(CLITools.prompt("Set the user name", cpm.getUserName()));
        cpm.setPassword(CLITools.prompt("Set the password", cpm.getPassword()));
        cpm.setHost(CLITools.prompt("Set the host:", cpm.getHost()));
        cpm.setDriver(CLITools.prompt("Set the JDBC driver", cpm.getDriver()));
        boolean setPort = false;
        String rv = null;
        for (int i = 0; i < MAX_TRIES; i++) {
            try {
                rv = CLITools.prompt("Set the port number", Integer.toString(cpm.getPort()));
                cpm.setPort(Integer.parseInt(rv));
                setPort = true;
                break;
            } catch (NumberFormatException nx) {
                say("Sorry, but \"" + rv + "\" is not a number. Try again");
            }
        }
        if (!setPort) {
            say("Using default port value of 5432");
            cpm.setPort(5432);
        }
        setPort = false;
        rv = null;
        say("\n\nNow it is time to set up the administrator's connection information.");
        AdminConnectionParametersModel apm = null;
        if (!isConfigured) {
            apm = configuration.getAdminConnectionParametersModel();
            sqlStoreModel.setAdminConnectionParameters(apm);
        } else {
            apm = sqlStoreModel.getAdminConnectionParametersModel();
        }
        apm.setAdminUserName(CLITools.prompt("Set the user name", apm.getAdminUserName()));
        apm.setAdminPassword(CLITools.prompt("Set the password", apm.getAdminPassword()));
        apm.setAdminHost(CLITools.prompt("Set the host:", apm.getAdminHost()));
        apm.setDriver(CLITools.prompt("Set the JDBC driver", apm.getDriver()));
        apm.setUserName(cpm.getUserName());
        for (int i = 0; i < MAX_TRIES; i++) {
            try {
                rv = CLITools.prompt("Set the port number", Integer.toString(apm.getAdminPort()));
                apm.setAdminPort(Integer.parseInt(rv));
                setPort = true;
                break;
            } catch (NumberFormatException nx) {
                say("Sorry, but \"" + rv + "\" is not a number. Try again");
                apm.setAdminPort(5432);
            }
        }
        if (!setPort) {
            say("Using default admin port value of 5432");
            apm.setAdminPort(5432);
        }
    }
}
