package org.kwantu.zakwantu;

import org.apache.commons.cli.*;
import org.hibernate.Session;
import org.kwantu.app.CurrentSessionProvider;
import org.kwantu.m2.KwantuContingencyException;
import org.kwantu.m2.model.*;
import org.kwantu.m2generator.M2Generator;
import org.kwantu.m2generator.M2GeneratorBuildFailedException;
import org.kwantu.m2generator.VersionManager;
import org.kwantu.m2generator.VersionManagerMergeFailedException;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.MovedContextHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.netbeans.api.visual.graph.layout.GridGraphLayout;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.layout.SceneLayout;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

/** Implements a command line interface for zakwantu.
 * 
 * The command line interface can be used in various ways, one easy way is via
 * maven, e.g. by issueing the following in the zakwantu directory:
 * 
 * <pre>
 * mvn -Dexec.args="-overwrite -file ../m2/target/ConfigurationModel1.model upload" exec:java"
 * </pre>
 * 
 * The command line has got the following options available:
 * 
 * <ul>
 *  <li>upload: upload a model file into the zakwantu environment.</li>
 *  <li>download: download a model into a file from the zakwantu environment.</li>
 *  <li>import: import modifications in previously generated Java files into an existing model.</li>
 *  <li>export: export a model to Java files.</li>
 * </ul>
 * 
 * The command line options can be investigated in detail by calling the command line without 
 * any command line argument, e.g. (in the zakwantu directory):
 * 
 * <pre>
 * mvn exec:java
 * </pre>
 * 
 * Note: The usage description is typically surrounded by some log messages.
 * 
 * At the time of this writing, the help output looked as follows:
 * 
 * <pre>
 * usage: <command> <options> { upload, download, import, export }
 * -file         File from which upload is done (to which download is done,
 *               resp.).
 * -modelName    Name of the Kwantu model to operate on (for
 *               exporting/importing java source).
 * -overwrite    If specified, the destination (file or model in the
 *               database) will be overwritten.
 * </pre>
 * 
 * @author chris
 */
public class ZakwantuCommandline {

    private static final Options options;

    private static final Zakwantu zakwantu;

    /** we record error messages, such that in the unit tests it can be checked
     * if an error occured.
     */
    private static String lastErrorMessage;

    private static Throwable lastError;

    public static String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public static Throwable getLastError() {
        return lastError;
    }

    public abstract static class CommandImplementation {

        public abstract void run(CommandLine commandLine);
    }

    public static enum Command {

        UPLOAD("upload", new CommandImplementation() {

            @Override
            public void run(CommandLine commandLine) {
                try {
                    String fileName = commandLine.getOptionValue("file");
                    if (StringUtil.isEmpty(fileName)) {
                        error("file not specified");
                        return;
                    }
                    System.out.println("About to upload from file " + fileName);
                    ArrayList<KwantuModel> importedDependencies = new ArrayList<KwantuModel>();
                    KwantuModel model = KwantuModel.importKwantuModel(new BufferedReader(new FileReader(fileName)), zakwantu.getController().getKwantuModelResolver(), importedDependencies);
                    zakwantu.saveModel(model, commandLine.hasOption("overwrite"));
                    zakwantu.saveModelDependencies(model, importedDependencies);
                    System.out.println("Model " + model.getName() + " successfully uploaded.");
                } catch (FileNotFoundException ex) {
                    error("File not found: " + ex.getMessage(), ex);
                } catch (KwantuContingencyException ex) {
                    error("Error: " + ex.getMessage(), ex);
                }
            }
        }), DOWNLOAD("download", new CommandImplementation() {

            @Override
            public void run(CommandLine commandLine) {
                try {
                    String fileName = commandLine.getOptionValue("file");
                    if (StringUtil.isEmpty(fileName)) {
                        error("file not specified");
                        help();
                        return;
                    }
                    File file = new File(fileName);
                    if (file.exists() && !commandLine.hasOption("overwrite")) {
                        error("File " + fileName + " exists already and '-overwrite' isn't specified.");
                        return;
                    }
                    System.out.println("About to download into file " + fileName);
                    KwantuModel model = getModel(commandLine);
                    if (model == null) {
                        return;
                    }
                    ModelExporter.export(file, model);
                    System.out.println("Downloaded model " + model.getName() + " into file " + fileName);
                } catch (IOException ex) {
                    error("Unable to download into file: " + ex.getMessage());
                }
            }
        }), IMPORT("import", new CommandImplementation() {

            @Override
            public void run(CommandLine commandLine) {
                String dirName = commandLine.getOptionValue("dir");
                if (StringUtil.isEmpty(dirName)) {
                    error("dir not specified");
                    help();
                    return;
                }
                KwantuModel model = getModel(commandLine);
                if (model == null) {
                    error("Cannot import for a model which doesn't exist yet.");
                    return;
                }
                File path = new File(dirName + model.getName() + "/src/main/java/org/kwantu/" + model.getName() + "/model");
                VersionManager versionManager = new VersionManager();
                Session session = zakwantu.getController().getM2Context().getDbSession();
                try {
                    versionManager.update(model, path, session);
                    session.getTransaction().commit();
                    session.createSQLQuery("SHUTDOWN;").executeUpdate();
                } catch (VersionManagerMergeFailedException ex) {
                    error("Import failed", ex);
                }
            }
        }), EXPORT("export", new CommandImplementation() {

            @Override
            public void run(CommandLine commandLine) {
                String dirName = commandLine.getOptionValue("dir");
                if (StringUtil.isEmpty(dirName)) {
                    error("dir not specified");
                    help();
                    return;
                }
                KwantuModel model = getModel(commandLine);
                if (model == null) {
                    return;
                }
                M2Generator generator = new M2Generator();
                generator.setBuildDirectory(dirName);
                generator.setKwantuModel(model);
                generator.generateClasses(null);
                generator.writePom();
            }
        }), BUILD("build", new CommandImplementation() {

            @Override
            public void run(CommandLine commandLine) {
                KwantuModel model = getModel(commandLine);
                if (model == null) {
                    return;
                }
                M2Generator generator = new M2Generator();
                generator.setKwantuModel(model);
                try {
                    generator.buildModel();
                } catch (M2GeneratorBuildFailedException ex) {
                    error("build failed: " + ex.getMessage(), ex);
                    return;
                }
            }
        }), RUN("run", new CommandImplementation() {

            @Override
            public void run(CommandLine commandLine) {
                int port = getHttpPort(commandLine);
                if (port == 0) {
                    return;
                }
                Server server = setupJettyServer(port);
                try {
                    server.start();
                    server.join();
                } catch (InterruptedException ex) {
                    error("interrupted", ex);
                } catch (Exception ex) {
                    error("exception in server: " + ex.getMessage(), ex);
                }
            }
        }), DISPLAY("display", new CommandImplementation() {

            @Override
            public void run(CommandLine commandLine) {
                KwantuModel model = getModel(commandLine);
                if (model == null) {
                    return;
                }
                KwantuImageExporter exporter = new KwantuImageExporter();
                KwantuModelGraphScene scene = exporter.createSceneFromModel(model.getKwantuBusinessObjectModel());
                JComponent sceneView = scene.createView();
                JScrollPane panel = new JScrollPane(sceneView);
                JDialog dialog = new JDialog((JDialog) null, true);
                dialog.add(panel, BorderLayout.CENTER);
                dialog.setSize(800, 600);
                GridGraphLayout<KwantuClass, KwantuEdge> graphLayout = new GridGraphLayout<KwantuClass, KwantuEdge>();
                graphLayout.setGaps(50, 50);
                SceneLayout sceneGraphLayout = LayoutFactory.createSceneGraphLayout(scene, graphLayout);
                sceneGraphLayout.invokeLayout();
                dialog.setVisible(true);
                dialog.dispose();
            }
        });

        String command;

        CommandImplementation implementation;

        Command(String command, CommandImplementation implementation) {
            this.command = command;
            this.implementation = implementation;
        }
    }

    static {
        options = new Options();
        options.addOption("overwrite", false, "If specified, the destination " + "(file or model in the database) will be overwritten.");
        options.addOption("file", true, "File from which upload is done " + "(to which download is done, resp.).");
        options.addOption("modelName", true, "Name of the Kwantu model " + "to operate on (for exporting/importing java source).");
        options.addOption("dir", true, "Directory from which/to which Java files " + "are read/written.");
        options.addOption("httpPort", true, "Specify the http port to use with the " + "'run' command. The default is 8080.");
        zakwantu = new Zakwantu(new CurrentSessionProvider() {

            @Override
            public Session getCurrentSession(String key) {
                return ZakwantuSessionProvider.getCurrentSession(zakwantu, key);
            }

            @Override
            public AbstractApplicationController getController() {
                return zakwantu.getController();
            }
        });
    }

    public static void main(String[] args) {
        GnuParser parser = new GnuParser();
        try {
            CommandLine line = parser.parse(options, args);
            if (line.getArgs().length > 1) {
                error("Too many commands or unrecognized options specified: " + line.getArgs());
                help();
                return;
            }
            if (line.getArgs().length == 0) {
                error("No command specified, should be one of " + Command.values());
                help();
                return;
            }
            String commandAsString = line.getArgs()[0];
            Command command = findCommand(commandAsString);
            if (command == null) {
                error("Command '" + commandAsString + "' is not recognized.");
                help();
                return;
            }
            try {
                command.implementation.run(line);
            } catch (Throwable t) {
                error("exception during command execution: " + t.getMessage(), t);
            }
        } catch (ParseException ex) {
            error("Parsing of command line failed. Reason: " + ex.getMessage());
        } catch (Throwable t) {
            error("Exception caught: " + t.getMessage(), t);
        }
    }

    /** set up a new jetty server for the zakwantu web application. This method
     * is public, because it is used/can be used by e.g. unit tests as well.
     * 
     * @return a newly instantiated jetty server running the zakwantu servlet.
     * 
     */
    public static Server setupJettyServer(int port) {
        Server server = new Server();
        WebAppContext web = new WebAppContext();
        String contextPath = "/zakwantu";
        web.setContextPath(contextPath);
        web.setWar("src/main/webapp");
        server.addHandler(web);
        server.addHandler(new MovedContextHandler(server, "/", contextPath));
        SelectChannelConnector httpConn = new SelectChannelConnector();
        httpConn.setPort(port);
        server.setConnectors(new Connector[] { httpConn });
        return server;
    }

    protected static int getHttpPort(CommandLine commandLine) {
        String s = commandLine.getOptionValue("httpPort");
        if (StringUtil.isEmpty(s)) {
            return 8080;
        }
        int i;
        try {
            i = Integer.valueOf(s).intValue();
        } catch (NumberFormatException ex) {
            error("Invalid value for http port: " + s);
            i = 0;
        }
        return i;
    }

    protected static KwantuModel getModel(CommandLine commandLine) {
        String modelName = commandLine.getOptionValue("modelName");
        if (StringUtil.isEmpty(modelName)) {
            error("No modelName specified, can't export.");
            help();
            return null;
        }
        KwantuModel model = zakwantu.findModel(modelName);
        if (model == null) {
            error("Model " + modelName + " not found.");
        }
        return model;
    }

    private static void error(String message) {
        error(message, null);
    }

    private static void error(String message, Throwable t) {
        System.err.println(message);
        if (t != null) {
            t.printStackTrace();
        }
        System.err.flush();
        lastErrorMessage = message;
        lastError = t;
    }

    private static void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("<command> <options> { " + listCommands() + " }", options);
    }

    private static Command findCommand(String commandAsString) {
        for (Command c : Command.values()) {
            if (c.command.equals(commandAsString)) {
                return c;
            }
        }
        return null;
    }

    private static String listCommands() {
        StringBuffer s = new StringBuffer();
        boolean first = true;
        for (Command c : Command.values()) {
            if (first) {
                first = false;
            } else {
                s.append(", ");
            }
            s.append(c.command);
        }
        return s.toString();
    }
}
