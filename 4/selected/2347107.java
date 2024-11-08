package debugEngine;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.connect.*;
import dataTree.DataTree;
import dataTree.MethodCallNode;
import dataTree.VariableNode;
import gui.MainGUI;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Jdd {

    private VirtualMachine _vm = null;

    private Thread errThread = null;

    private Thread outThread = null;

    private int _debugTraceMode = 0;

    private String[] _excludes = { "java.*", "javax.*", "sun.*", "org.*", "com.sun.*" };

    private String _classpath = "\".\"";

    private String _classToDebug = null;

    private String _classArguments = "";

    private String _outputFile = null;

    private boolean _showGUI = true;

    private PrintWriter _writer = null;

    private BufferedReader _in = null;

    private MainGUI mGUI = null;

    /**
	 * main
	 */
    public static void main(String[] args) {
        new Jdd(args);
    }

    /**
	 * Parse the command line arguments.  
	 * Launch target VM.
	 * Generate the trace.
	 */
    Jdd(String[] args) {
        try {
            _writer = new PrintWriter(System.out);
            _in = new BufferedReader(new InputStreamReader(System.in));
            boolean hasClass = false;
            int inx;
            for (inx = 0; inx < args.length; ++inx) {
                String arg = args[inx];
                if (arg.charAt(0) != '-') {
                    break;
                }
                if (arg.equals("-output")) {
                } else if (arg.equals("-nogui")) {
                    _showGUI = false;
                } else if (arg.equals("-classpath")) {
                    _classpath = args[++inx];
                } else if (arg.equals("-dbgtrace")) {
                    _debugTraceMode = Integer.parseInt(args[++inx]);
                } else if (arg.equals("-excludes")) {
                    _excludes = getExcludes(args[++inx]);
                } else if (arg.equals("-version")) {
                    _writer.print(showVersion());
                } else if (arg.equals("-help")) {
                    usage();
                    System.exit(0);
                } else {
                    System.err.println("No option: " + arg);
                    usage();
                    System.exit(1);
                }
            }
            if (inx < args.length) {
                _classToDebug = args[inx];
                _classArguments = "";
                for (++inx; inx < args.length; ++inx) {
                    _classArguments = " " + args[inx];
                }
                this.run();
                hasClass = true;
            }
            if (_showGUI) {
                mGUI = new MainGUI();
                if (!hasClass) mGUI.loadDebugTreeWindow(this); else {
                    MethodCallNode mcn = DataTree.getRoot();
                    mGUI.loadDebugTreeWindow(mcn.getNodeCount(), mcn.getDepth() + 1, mcn, this);
                }
            } else {
                debugerShell myShell = new debugerShell(this, _writer, _in);
                myShell.start();
                try {
                    myShell.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String showVersion() {
        return " -- Java Declarative Debugger -- Version RC 1.0  --\n" + " -- Developed by: Reyes de Miguel Roses - Susana Serrano Soria - Francisco Gonzalez-Blanch Rodriguez --\n" + " -- Universidad Complutense de Madrid - 2006 --\n";
    }

    private String[] getExcludes(String string) {
        return null;
    }

    /**
	 *  This class implements the debugger shell
	 * 
	 */
    public class debugerShell extends Thread {

        private PrintWriter _writer = null;

        private Jdd _engine = null;

        private BufferedReader _in = null;

        public debugerShell(Jdd engine, PrintWriter writer, BufferedReader in) {
            super("debugerShell");
            this._engine = engine;
            this._writer = writer;
            _in = in;
        }

        public void printPrompt() {
            this._writer.print("JDD:>");
            this._writer.flush();
        }

        public void run() {
            _writer.println("Initializing Java Declarative Debugger version 1.0 ....");
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            printPrompt();
            while (true) {
                String ln;
                try {
                    ln = _in.readLine();
                    if (ln == null) {
                        _writer.println("Input stream closed.");
                        return;
                    }
                    if (ln.startsWith("!!")) {
                        _writer.println(ln);
                    }
                    StringTokenizer t = new StringTokenizer(ln);
                    if (t.hasMoreTokens()) {
                        executeCommand(t);
                    }
                    printPrompt();
                } catch (IOException e) {
                    System.out.println("Error Processing I/O in Debugger Shell");
                    e.printStackTrace();
                } catch (Exception e) {
                    System.out.println("Error executing cquitommand");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	 * Generate the trace.
	 * Enable events, start thread to display events, 
	 * start threads to forward remote error and output streams,
	 * resume the remote VM, wait for the final event, and shutdown.
	 */
    void generateTrace(PrintWriter writer) {
        _vm.setDebugTraceMode(_debugTraceMode);
        DataTree.Reset();
        DataTree.ResetObjectTable();
        DataTree.Init();
        EventThread eventThread = new EventThread(_vm, _excludes, writer);
        eventThread.setEventRequests();
        eventThread.start();
        redirectOutput();
        _vm.resume();
        try {
            eventThread.join();
            errThread.join();
            outThread.join();
        } catch (InterruptedException exc) {
        }
        DataTree.StartDebugging();
    }

    /**
	 * Launch target VM.
	 * Forward target's output and error.
	 */
    VirtualMachine launchTarget(String mainArgs) {
        LaunchingConnector connector = findLaunchingConnector();
        Map arguments = connectorArguments(connector, mainArgs);
        try {
            return connector.launch(arguments);
        } catch (IOException exc) {
            throw new Error("Unable to launch target VM: " + exc);
        } catch (IllegalConnectorArgumentsException exc) {
            throw new Error("Internal error: " + exc);
        } catch (VMStartException exc) {
            throw new Error("Target VM failed to initialize: " + exc.getMessage());
        }
    }

    void redirectOutput() {
        Process process = _vm.process();
        errThread = new StreamRedirectThread("error reader", process.getErrorStream(), System.err);
        outThread = new StreamRedirectThread("output reader", process.getInputStream(), System.out);
        errThread.start();
        outThread.start();
    }

    /**
	 * Find a com.sun.jdi.CommandLineLaunch connector
	 */
    LaunchingConnector findLaunchingConnector() {
        List connectors = Bootstrap.virtualMachineManager().allConnectors();
        Iterator iter = connectors.iterator();
        while (iter.hasNext()) {
            Connector connector = (Connector) iter.next();
            if (connector.name().equals("com.sun.jdi.CommandLineLaunch")) {
                return (LaunchingConnector) connector;
            }
        }
        throw new Error("No launching connector");
    }

    /**
	 * Return the launching connector's arguments.
	 */
    Map connectorArguments(LaunchingConnector connector, String mainArgs) {
        Map arguments = connector.defaultArguments();
        Connector.Argument mainArg = (Connector.Argument) arguments.get("main");
        if (mainArg == null) {
            throw new Error("Bad launching connector");
        }
        mainArg.setValue(mainArgs);
        Connector.Argument optionArg = (Connector.Argument) arguments.get("options");
        if (optionArg == null) {
            throw new Error("Bad launching connector");
        }
        optionArg.setValue("-classic");
        return arguments;
    }

    public void executeCommand(StringTokenizer t) throws Exception {
        String command = t.nextToken();
        if (command.compareTo("run") == 0) {
        } else if (command.compareTo("where") == 0) {
            if (t.hasMoreElements()) _writer.println(where(Integer.parseInt(t.nextToken()))); else _writer.println(where(0));
        } else if (command.compareTo("dumpTree") == 0) {
            _writer.println(dumpTree());
        } else if (command.compareTo("list") == 0) {
            _writer.println(list());
        } else if (command.compareTo("callerObject") == 0) {
            _writer.println(callerObject());
        } else if (command.compareTo("returnValue") == 0) {
            _writer.println(returnValue());
        } else if (command.compareTo("arguments") == 0) {
            _writer.println(arguments());
        } else if (command.compareTo("set") == 0) {
            setState(Integer.parseInt(t.nextToken()));
        } else if (command.compareTo("next") == 0) {
            next();
            _writer.println(where(0));
        } else if (command.compareTo("prev") == 0) {
            prev();
            _writer.println(where(0));
        } else if (command.compareTo("help") == 0) {
            _writer.println(help());
        } else if (command.compareTo("version") == 0) {
            _writer.println(showVersion());
        } else if (command.compareTo("quit") == 0) {
            quit();
        } else if (command.compareTo("exit") == 0) {
            quit();
        } else {
            _writer.println("Command not valid; Use \"help\" to view possible commands ");
        }
    }

    private Object list() throws Exception {
        return DataTree.getCurrentNode().getMethodCode();
    }

    private String help() {
        String aux = "This is the JDD Console Options  \n";
        aux += "   run  <class> <args> -- Run the specified class \n";
        aux += "   classpath           -- Shows the classpath \n";
        aux += "   list                -- Shows the code of the current node \n";
        aux += "   where               -- Shows the current node \n";
        aux += "   where  <levels>     -- Shows the tree from the current node until \n";
        aux += "     					      the depth given <levels>\n";
        aux += "   dumpTree            -- Shows the debugging tree \n";
        aux += "   callerObject        -- Shows the caller object of the current node \n";
        aux += "   arguments           -- Shows the arguments of the current node \n";
        aux += "   returnValue         -- Shows the return value of the current node \n";
        aux += "   set <state>         -- Sets a debugging <state> for the current node \n";
        aux += "   next                -- Next node to debug\n";
        aux += "   prev                -- Prev node to debug\n";
        aux += "   version             -- Shows the JDD Version \n";
        aux += "   exit/quit           -- Quits the application\n";
        return aux;
    }

    public void run() {
        StringBuffer sb = new StringBuffer();
        sb.append("-classpath \"" + _classpath + "\" ");
        sb.append(_classToDebug);
        sb.append(" " + _classArguments);
        _vm = launchTarget(sb.toString());
        generateTrace(_writer);
    }

    public void next() {
        DataTree.NextNode();
    }

    public void prev() {
        DataTree.PrevNode();
    }

    public String where(int i) throws Exception {
        return DataTree.getCurrentNode().print(i, 0);
    }

    public String dumpTree() throws Exception {
        return DataTree.getRoot().print(DataTree.getRoot().getDepth(), 0);
    }

    public String callerObject() throws Exception {
        return DataTree.getCurrentNode().getCallerObject().toString();
    }

    public String returnValue() throws Exception {
        return DataTree.getCurrentNode().getReturnValue().toString();
    }

    public String arguments() throws Exception {
        Vector arguments = DataTree.getCurrentNode().getArguments();
        String aux = "";
        for (int i = 0; i < arguments.size(); i++) {
            aux += ((VariableNode) arguments.elementAt(i)).toString() + "\n";
        }
        return aux;
    }

    public void setState(int state) throws Exception {
        DataTree.getCurrentNode().setState(state);
    }

    public void quit() {
        System.exit(0);
    }

    /**
	 * Print command line usage help
	 */
    void usage() {
        System.err.println("Usage: java JDD <options> <class> <args>");
        System.err.println("<options> are:");
        System.err.println("  -output <filename>      Log debug Session to an output file : <filename>");
        System.err.println("  -nogui 					    Don't Launch GUI (only text mode)");
        System.err.println("  -classpath  <classpath> Set the classpath");
        System.err.println("  -excludes  <class>      don't trace <classes> classes or packages");
        System.err.println("  -help                   Print this help message");
        System.err.println("  -version                Print Debugger version");
        System.err.println("  -dbgtrace               Show JDI debugger Information");
        System.err.println("<class> is the program to debug");
        System.err.println("<args> are the arguments to <class>");
    }

    public String getClassToDebug() {
        return _classToDebug;
    }

    public String getClassPath() {
        return _classpath;
    }

    public void setClassPath(String classpath) {
        _classpath = classpath;
    }

    public void setClassToDebug(String toDebug) {
        _classToDebug = toDebug;
    }

    public String getClassArguments() {
        return _classArguments;
    }

    public void setClassArguments(String arguments) {
        _classArguments = arguments;
    }
}
