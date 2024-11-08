package papertoolkit.external;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import papertoolkit.application.config.Constants;
import papertoolkit.util.DebugUtils;
import papertoolkit.util.files.FileUtils;

/**
 * <p>
 * A messaging server that will relay information objects to one or more Flash GUIs, which will listen for
 * them. It's a two way pipe, so the Flash GUIs can also send messages back! It can ask the GUI to do
 * different things, such as going to a named frame.
 * 
 * This is an early implementation. Later on, we may allow our event handlers to live in the world of Flash,
 * for faster UI prototyping.
 * 
 * By default, we accept two sorts of commands. Commands without arguments come across the wire looking like
 * this: <br>
 * commandName <br>
 * We also accept commands with arguments, that look like this:<br>
 * [[commandName]]{{arg1}}{{arg2}}{{arg3}}<br>
 * We simply assume that the input strings do not have double brackets or braces... If they do, then you
 * should set your own delimiters...
 * </p>
 * <p>
 * TODO: Clearly, this functionality would be useful outside of Flash too. Should we rename this class to
 * ExternalCommunicationServer and ExternalCommand?
 * </p>
 * <p>
 * <span class="BSDLicense"> This software is distributed under the <a
 * href="http://hci.stanford.edu/research/copyright.txt">BSD License</a>. </span>
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 */
public class ExternalCommunicationServer {

    /**
	 * 
	 */
    private String argDelimiter;

    /**
	 * 
	 */
    private Pattern argRegExp;

    /**
	 * Only ever counts up, so IDs are unique.
	 */
    private int clientID = 0;

    /**
	 * 
	 */
    private String commandDelimiter;

    /**
	 * 
	 */
    private Pattern commandRegExp;

    /**
	 * 
	 */
    private Map<String, ExternalCommand> commands = new HashMap<String, ExternalCommand>();

    /**
	 * All the clients that have connected to us! You can test this by telnetting in to this server and port.
	 */
    private List<ExternalClient> clients = new ArrayList<ExternalClient>();

    /**
	 * Send messages to these Java listeners.
	 */
    private List<ExternalListener> listeners = new ArrayList<ExternalListener>();

    /**
	 * -1 means unlimited. Any number > 0 implies a limit on the number of flash clients....
	 */
    private int maxNumClients = -1;

    /**
	 * Replaces OTHER_PARAMS in the HTML/Flash template with other query parameters.
	 */
    private String queryParameters = "";

    /**
	 * 
	 */
    private int serverPort;

    /**
	 * 
	 */
    private Thread serverThread;

    /**
	 * 
	 */
    private ServerSocket socket;

    /**
	 * Useful for debugging Flash communcations...
	 */
    private boolean verbose = false;

    /**
	 * Allows us to send messages to the Flash GUI.
	 */
    public ExternalCommunicationServer() {
        this(Constants.Ports.EXTERNAL_COMMUNICATION_SERVER);
    }

    /**
	 * Customize the port for the Flash Communication Server.
	 * 
	 * @param port
	 */
    public ExternalCommunicationServer(int port) {
        setCommandDelimiter("%%*%%");
        setArgumentsDelimiter("@_*_@");
        serverPort = port;
        serverThread = new Thread(getServer());
        serverThread.start();
    }

    /**
	 * @param cmd
	 */
    public void addCommand(ExternalCommand cmd) {
        addCommand(cmd.getName(), cmd);
    }

    /**
	 * Adds a handler for this function name...
	 * 
	 * @param cmdName
	 * @param flashCommand
	 */
    public void addCommand(String cmdName, ExternalCommand flashCommand) {
        commands.put(cmdName, flashCommand);
    }

    /**
	 * @param flashListener
	 */
    public void addFlashClientListener(ExternalListener flashListener) {
        listeners.add(flashListener);
    }

    /**
	 * @param params
	 */
    public void addQueryParameter(String params) {
        if (queryParameters.equals("")) {
            queryParameters = params;
        } else {
            queryParameters = queryParameters + "&" + params;
        }
    }

    /**
	 * Exits the Flash communication server.
	 */
    public void exitServer() {
        for (ExternalClient client : clients) {
            client.exitClient();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DebugUtils.println("Exiting ExternalCommunications Server.... " + "If this is the last thread, the program should exit.");
        for (ExternalListener listener : listeners) {
            listener.messageReceived("exitServer");
        }
    }

    /**
	 * @return
	 */
    private Runnable getServer() {
        return new Runnable() {

            public void run() {
                DebugUtils.println("Starting ExternalCommunications Server at port: " + serverPort);
                try {
                    socket = new ServerSocket(serverPort);
                    while (true) {
                        final Socket incoming = socket.accept();
                        final BufferedReader readerIn = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
                        final PrintStream writerOut = new PrintStream(incoming.getOutputStream());
                        if ((maxNumClients > 0) && (clients.size() == maxNumClients)) {
                            ExternalClient removedClient = clients.remove(0);
                            removedClient.exitClient();
                        }
                        clients.add(new ExternalClient(ExternalCommunicationServer.this, clientID++, incoming, readerIn, writerOut));
                    }
                } catch (SocketException e) {
                    DebugUtils.println("Server Socket was Closed");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                DebugUtils.println("Stopping Flash Communications Server");
            }
        };
    }

    /**
	 * @param client
	 * @param clientID
	 * @param command
	 *            case sensitive commands...
	 */
    public void handleCommand(ExternalClient client, String command) {
        String commandName = "";
        Matcher cmdMatcher = commandRegExp.matcher(command);
        if (cmdMatcher.find()) {
            commandName = cmdMatcher.group(1);
        } else {
            commandName = command;
        }
        ArrayList<String> arguments = new ArrayList<String>();
        Matcher argMatcher = argRegExp.matcher(command);
        while (argMatcher.find()) {
            arguments.add(argMatcher.group(1));
        }
        String[] args = {};
        if (arguments.size() > 0) {
            args = arguments.toArray(args);
        }
        if (commandName.equals("exit")) {
            if (client == null) {
                return;
            }
            client.exitClient();
        } else if (commandName.equals("exitClients")) {
            ArrayList<ExternalClient> toBeRemoved = new ArrayList<ExternalClient>();
            for (ExternalClient cl : clients) {
                if (cl.getName().equals(args[0])) {
                    cl.exitClient();
                    toBeRemoved.add(cl);
                }
            }
            clients.removeAll(toBeRemoved);
            DebugUtils.println(clients.size() + " clients remain:");
            for (ExternalClient cl : clients) {
                DebugUtils.println(cl.getID() + ": " + cl.getName());
            }
        } else if (commandName.equals("exitServer")) {
            if (client == null) {
                return;
            }
            client.exitClient();
            exitServer();
        } else if (commandName.equals("exitApplication")) {
            if (client == null) {
                return;
            }
            DebugUtils.println("Got exitApplicaton command...");
            client.exitClient();
            exitServer();
            System.exit(0);
        } else if (commandName.equals("setName")) {
            if (client == null) {
                return;
            }
            client.setName(arguments.get(0));
        } else if (commands.containsKey(commandName)) {
            DebugUtils.println("Server handling command: " + commandName);
            commands.get(commandName).invoke(args);
        } else {
            if (client != null) {
                DebugUtils.println("Server got command [" + commandName + "] from client " + client.getID());
            } else {
                DebugUtils.println("Server got Message: " + commandName);
            }
            for (ExternalListener listener : listeners) {
                boolean consumed = listener.messageReceived(commandName, args);
                if (consumed) {
                    break;
                }
            }
        }
    }

    /**
	 * You can manually trigger commands without any client in particular.
	 * 
	 * @param commandWithArgs
	 */
    public void handleCommand(String commandWithArgs) {
        this.handleCommand(null, commandWithArgs);
    }

    /**
	 * Reads in a Template HTML file, and generates the final HTML file on the fly, to contain our SWF,
	 * passing in the port as a parameter.
	 * 
	 * @param htmlFileContainingSWF
	 *            Or perhaps this should be a URL in the future, as the GUI can live online? Launches the
	 *            flash GUI in a browser.
	 */
    public void openFlashHTMLGUI(File htmlFileContainingSWF) {
        try {
            String fileStr = FileUtils.readFileIntoStringBuffer(htmlFileContainingSWF, true).toString();
            fileStr = fileStr.replace("PORT_NUM", Integer.toString(serverPort));
            fileStr = fileStr.replace("OTHER_PARAMS", queryParameters);
            final File outputTempHTML = new File(htmlFileContainingSWF.getParentFile(), htmlFileContainingSWF.getName() + "_" + serverPort + ".html");
            FileUtils.writeStringToFile(fileStr, outputTempHTML);
            final URI uri = outputTempHTML.toURI();
            Desktop.getDesktop().browse(uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 */
    public void removeAllFlashClientListeners() {
        listeners.clear();
    }

    /**
	 * Basically a println.
	 * 
	 * @param msg
	 */
    public void sendLine(String msg) {
        sendMessage(msg + "\r\n");
    }

    /**
	 * @param msg
	 */
    public void sendMessage(String msg) {
        if (verbose) {
            if (msg.length() < 100) {
                DebugUtils.println("Sending message: " + msg.replace("\n", "") + " to all " + clients.size() + " clients");
            } else {
                DebugUtils.println("Sending message: " + msg.substring(0, 100).replace("\n", "") + "... to all " + clients.size() + " clients");
            }
        }
        for (ExternalClient client : clients) {
            client.sendMessage(msg);
        }
    }

    /**
	 * server.setArgumentsDelimiter("{{*}}");
	 * 
	 * @param argumentPatternRegExp
	 */
    public void setArgumentsDelimiter(String argumentPattern) {
        argDelimiter = argumentPattern.replace("*", "(.*?)");
        argRegExp = Pattern.compile(argDelimiter);
    }

    /**
	 * server.setCommandDelimiter("[[*]]"); Place an asterisk where the command should go...
	 * 
	 * @param commandPattern
	 */
    public void setCommandDelimiter(String commandPattern) {
        commandDelimiter = commandPattern.replace("*", "(.*?)");
        commandRegExp = Pattern.compile(commandDelimiter);
    }

    /**
	 * @param num
	 */
    public void setMaxNumClients(int num) {
        maxNumClients = num;
    }

    /**
	 * @param v
	 */
    public void setVerbose(boolean v) {
        verbose = v;
    }
}
