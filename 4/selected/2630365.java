package edu.rice.cs.cunit.record;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import edu.rice.cs.cunit.SyncPointBuffer;
import edu.rice.cs.cunit.classFile.ClassFileTools;
import edu.rice.cs.cunit.classFile.MethodInfo;
import edu.rice.cs.cunit.classFile.attributes.AAttributeInfo;
import edu.rice.cs.cunit.classFile.attributes.LineNumberTableAttributeInfo;
import edu.rice.cs.cunit.classFile.attributes.SourceFileAttributeInfo;
import edu.rice.cs.cunit.util.Debug;
import edu.rice.cs.cunit.util.StreamRedirectThread;
import edu.rice.cs.cunit.util.ExecJVM;
import javax.swing.table.AbstractTableModel;
import java.io.*;
import java.util.*;
import javax.swing.SwingUtilities;

/**
 * This program monitors thread starts, terminations, and states.
 *
 * @author Mathias Ricken
 */
public class Record {

    /**
     * Debug VM.
     */
    private VirtualMachine _vm;

    /**
     * Thread transferring remote error stream to our error stream
     */
    private Thread _errThread = null;

    /**
     * Thread transferring remote output stream to our output stream
     */
    private Thread _outThread = null;

    /**
     * Mode for tracing the Record program (default=0 off)
     */
    private int _debugTraceMode = 0;

    /**
     * View for the program.
     */
    private IRecordView _view;

    /**
     * Event thread
     */
    private EventThread _eventThread;

    /**
     * Automatic update mode?
     */
    private boolean _auto = false;

    /**
     * Delay in ms between automatic updates.
     */
    private int _autoDelay = 1000;

    /**
     * Writer for output.
     */
    private PrintWriter _writer;

    /**
     * Time the program was started.
     */
    private long _startTime;

    /**
     * Debug log name.
     */
    public static final String DRV = "record.verbose";

    /**
     * Custom table model for history list.
     */
    private static class HistoryTableModel extends AbstractTableModel {

        /**
         * Storage class for transfer records
         */
        private static class TransferRecord {

            public int count;

            public int compactCount;

            public long time;

            public String desc;

            public TransferRecord(int count, int compactCount, long time, String desc) {
                this.count = count;
                this.compactCount = compactCount;
                this.time = time;
                this.desc = desc;
            }
        }

        /**
         * List with history.
         */
        private ArrayList<TransferRecord> _list = new ArrayList<TransferRecord>();

        private final String[] _columnNames = { "No", "Object #", "Compact #", "Time", "Comment" };

        public String getColumnName(int col) {
            return _columnNames[col];
        }

        public int getRowCount() {
            return _list.size();
        }

        public int getColumnCount() {
            return _columnNames.length;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            final int i = _list.size() - rowIndex - 1;
            switch(columnIndex) {
                case 0:
                    return i + 1;
                case 1:
                    return _list.get(i).count;
                case 2:
                    return _list.get(i).compactCount;
                case 3:
                    return _list.get(i).time;
                case 4:
                    return _list.get(i).desc;
            }
            return null;
        }

        public void add(final int count, final int compactCount, final long time, final String desc) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    _list.add(new TransferRecord(count, compactCount, time, desc));
                    fireTableRowsInserted(0, 0);
                }
            });
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
    }

    /**
     * History of the synchronization point transfers.
     */
    private HistoryTableModel _transferHistoryModel = new HistoryTableModel();

    /**
     * Custom table model for info table.
     */
    private static class InfoTableModel extends AbstractTableModel {

        /**
         * List of name-value pairs.
         */
        private HashMap<String, Object> _properties = new HashMap<String, Object>();

        /**
         * Column names.
         */
        private final String[] _columnNames = { "Name", "Value" };

        public String getColumnName(int col) {
            return _columnNames[col];
        }

        public int getRowCount() {
            return _properties.size();
        }

        public int getColumnCount() {
            return _columnNames.length;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return _properties.keySet().toArray(new String[] {})[rowIndex];
                case 1:
                    return _properties.values().toArray(new Object[] {})[rowIndex];
            }
            return null;
        }

        public void setProperty(String name, Object value) {
            _properties.put(name, value);
        }

        public Object getProperty(String name) {
            return _properties.get(name);
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        /**
         * Create a new info table model with start values for the properties.
         */
        public InfoTableModel() {
            setProperty("nextObjectID", -1);
            setProperty("nextThreadID", -1);
            setProperty("numSyncPointsInList", -1);
            setProperty("numSyncPointsInCompactList", -1);
            setProperty("numTotalSyncPoints", 0);
            setProperty("numTotalCompactSyncPoints", 0);
            setProperty("numUpdates", 0);
            setProperty("numDelayedUpdates", 0);
            setProperty("isUpdateDelayed", false);
            setProperty("mainEntered", false);
            setProperty("transferImmediately", false);
            setProperty("recording", false);
            setProperty("numCompactSyncPointsBeforeMain", -1);
            setProperty("userThreads", 1);
            setProperty("allUserThreadsDied", false);
        }
    }

    /**
     * Table model for the info table.
     */
    private InfoTableModel _infoTableModel = new InfoTableModel();

    /**
     * Sync point processor.
     */
    private ISyncPointProcessor _processor;

    /**
     * Object sync point processor, or null if not used
     */
    private ISyncPointProcessor _objectProcessor = null;

    /**
     * Enable processing of object sync points?
     */
    private boolean _processObjectSyncPoints = false;

    /**
     * Enable processing of compact debug sync points?
     */
    private boolean _processCompactDebugSyncPoints = false;

    /**
     * Enable processing of sync points during init?
     */
    private boolean _includeInitSyncPoints = false;

    /**
     * Enable processing of sync points during termination?
     */
    private boolean _includeTermSyncPoints = false;

    /**
     * True if no GUI should be shown.
     */
    private boolean _headless = false;

    /**
     * Decypher class index/method index pairs?
     */
    private HashMap<String, String> _methodDatabase;

    /**
     * DrJava .jar file.
     */
    private File _drJavaJarFile = new File("./drjava.jar");

    /**
     * Path used to find source files.
     */
    private String _sourcePath = ".";

    /**
     * Debug VM class path.
     */
    private String _classPath = ".";

    /**
     * JVM Entry Point.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        new Record(args, true);
    }

    /**
     * Constructor.
     *
     * @param args command line arguments.
     * @param exitOnClose true if application should exit on window close
     *
     */
    Record(String[] args, boolean exitOnClose) {
        String jvmWorkingDir = null;
        boolean closeWriter = false;
        _methodDatabase = new HashMap<String, String>();
        _writer = new PrintWriter(System.out);
        int inx;
        for (inx = 0; inx < args.length; ++inx) {
            String arg = args[inx];
            if (arg.charAt(0) != '-') {
                break;
            }
            Debug.out.println(DRV, "Argument " + inx + ": " + arg);
            if (arg.equals("-quiet")) {
                _writer = new PrintWriter(new OutputStream() {

                    public void write(int b) {
                    }
                });
            } else if (arg.equals("-headless")) {
                _headless = true;
            } else if (arg.equals("-verbose")) {
                Debug.out.setDebug(true);
                if ((inx < args.length - 1) && (!args[inx + 1].startsWith("-"))) {
                    String logs = args[++inx].toLowerCase();
                    for (int c = 0; c < logs.length(); ++c) {
                        switch(logs.charAt(c)) {
                            case 'm':
                                {
                                    Debug.out.setLogTarget(DRV, Debug.LogTarget.MAIN);
                                    break;
                                }
                            case 'e':
                                {
                                    Debug.out.setLogTarget(EventThread.DREV, Debug.LogTarget.MAIN);
                                    break;
                                }
                            default:
                                {
                                    System.err.println("Unknown log character. Usage: -verbose [m][e].");
                                    System.exit(-1);
                                    break;
                                }
                        }
                    }
                } else {
                    Debug.out.setLogTarget(DRV, Debug.LogTarget.MAIN);
                    Debug.out.setLogTarget(EventThread.DREV, Debug.LogTarget.MAIN);
                }
            } else if (arg.equals("-output")) {
                try {
                    _writer = new PrintWriter(new FileWriter(args[++inx]));
                    closeWriter = true;
                } catch (IOException exc) {
                    System.err.println("Cannot open output file: " + args[inx] + " - " + exc);
                    System.exit(1);
                }
            } else if (arg.equals("-dbgtrace")) {
                _debugTraceMode = Integer.parseInt(args[++inx]);
            } else if (arg.equals("-auto")) {
                _auto = true;
                try {
                    int n = Integer.parseInt(args[inx + 1]);
                    if (n >= 100) {
                        _autoDelay = n;
                    }
                    ++inx;
                } catch (NumberFormatException e) {
                    System.err.println("Error in number format for -auto.");
                }
            } else if (arg.equals("-obj")) {
                _processObjectSyncPoints = true;
            } else if (arg.equals("-debug")) {
                _processCompactDebugSyncPoints = true;
            } else if (arg.equals("-methoddb")) {
                try {
                    File db = new File(args[++inx]);
                    BufferedReader r = new BufferedReader(new FileReader(db));
                    String line;
                    while ((line = r.readLine()) != null) {
                        String key = line.substring(0, line.indexOf(":"));
                        String value = line.substring(line.indexOf(":") + 2);
                        _methodDatabase.put(key, value);
                    }
                    r.close();
                } catch (IOException exc) {
                    System.err.println("Cannot open method database file: " + args[inx] + " - " + exc);
                    System.exit(1);
                }
            } else if (arg.equals("-initsp")) {
                _includeInitSyncPoints = true;
            } else if (arg.equals("-termsp")) {
                _includeTermSyncPoints = true;
            } else if (arg.equals("-D")) {
                jvmWorkingDir = args[++inx];
            } else if (arg.equals("-help")) {
                help();
                System.exit(0);
            } else if (arg.startsWith("-J")) {
                ++inx;
                break;
            } else if (arg.equals("-drj")) {
                _drJavaJarFile = new File(args[++inx]);
                if (!_drJavaJarFile.exists() || !_drJavaJarFile.canRead()) {
                    System.err.println("Cannot find/read drjava.jar file: " + _drJavaJarFile);
                    System.exit(1);
                }
            } else if (arg.equals("-sp")) {
                _sourcePath = args[++inx];
            } else if (arg.equals("-cp")) {
                _classPath = args[++inx];
            } else {
                System.err.println("No option: " + arg);
                help();
                System.exit(1);
            }
        }
        if (inx >= args.length) {
            System.err.println("<class> missing");
            help();
            System.exit(1);
        }
        StringBuilder sb = new StringBuilder();
        if (jvmWorkingDir != null) {
            sb.append("-Duser.dir=\"");
            sb.append(jvmWorkingDir);
            sb.append("\" ");
        }
        sb.append("-cp \"");
        sb.append(_classPath);
        sb.append("\" ");
        sb.append(args[inx]);
        for (++inx; inx < args.length; ++inx) {
            sb.append(' ');
            sb.append(args[inx]);
        }
        if (_processObjectSyncPoints) {
            _objectProcessor = new ObjectProcessor(_writer, this);
        }
        if (_processCompactDebugSyncPoints) {
            _processor = new CompactDebugProcessor(_writer, this, _methodDatabase);
        } else {
            _processor = new CompactProcessor(_writer, this, _methodDatabase);
        }
        Debug.out.println(DRV, "Command line: " + sb.toString());
        run(sb.toString(), _writer, exitOnClose, !_auto, closeWriter);
    }

    /**
     * Returns the reference to the slave VM.
     * @return slave VM
     */
    public VirtualMachine getVM() {
        return _vm;
    }

    /**
     * Runs the tracer.
     *
     * @param cmdLine command line
     * @param writer  writer for debug output
     * @param exitOnClose true if application should exit on window close
     * @param showSyncButton true if sync button should be displayed (manual update)
     * @param closeWriter true if the writer should be closed at the end
     */
    private void run(String cmdLine, PrintWriter writer, boolean exitOnClose, boolean showSyncButton, boolean closeWriter) {
        if (!_headless) {
            Debug.out.println(DRV, "Creating view...");
            _view = new RecordView(new IMonitorAdapter() {

                public IThreadInfo[] getThreadList() {
                    return Record.this.getThreadList();
                }

                public void shutDown() {
                    if (_eventThread != null) {
                        _eventThread.shutDown();
                    }
                }

                public List<List<IThreadInfo>> getCycles() {
                    return Record.this.getCycles();
                }

                public void processSynchronizationPoints(String desc) {
                    Record.this.processSynchronizationPoints(false, desc);
                }

                public int getNumDelayedUpdates() {
                    return Record.this.getNumDelayedUpdates();
                }

                public int getNumUpdates() {
                    return Record.this.getNumUpdates();
                }

                public int getNumSyncPointsInList() {
                    return Record.this.getNumSyncPointsInList();
                }

                public boolean isUpdateDelayed() {
                    return Record.this.isUpdateDelayed();
                }

                public long getNextObjectID() {
                    return Record.this.getNextObjectID();
                }

                public long getNextThreadID() {
                    return Record.this.getNextThreadID();
                }

                public AbstractTableModel getTransferHistoryModel() {
                    return Record.this._transferHistoryModel;
                }

                public AbstractTableModel getInfoTableModel() {
                    return Record.this._infoTableModel;
                }

                public boolean openSource(String link) {
                    return Record.this.openSource(link);
                }
            }, exitOnClose, showSyncButton);
            _view.setProjectInWindowTitle("[running]");
        } else {
            Debug.out.println(DRV, "Headless operation...");
            _view = new IRecordView() {

                public void setProjectInWindowTitle(String project) {
                    Debug.out.println(DRV, project);
                }

                public void selectThreadWithId(long id) {
                }

                public void updateList(IThreadInfo[] threads) {
                    for (IThreadInfo ti : threads) {
                        Debug.out.println(DRV, ti.toStringVerbose());
                    }
                }

                public void detectDeadlock() {
                }
            };
        }
        Debug.out.println(DRV, "Launching debug VM...");
        _vm = launchDebugVM(cmdLine);
        Debug.out.println(DRV, "Recording...");
        generateTrace(writer, closeWriter);
    }

    /**
     * Generates trace.
     *
     * @param writer writer for debug output
     * @param closeWriter true if the writer should be closed at the end
     */
    void generateTrace(PrintWriter writer, boolean closeWriter) {
        _vm.setDebugTraceMode(_debugTraceMode);
        _eventThread = new EventThread(_vm, writer, new IViewAdapter() {

            public void updateThreadList() {
                if (_view != null) {
                    _view.updateList(getThreadList());
                }
            }

            public void updateSynchronizationPoints(boolean force, String desc) {
                if (_view != null) {
                    processSynchronizationPoints(force, desc);
                    _view.updateList(getThreadList());
                }
            }

            public void setMainEntered() {
                Record.this.setMainEntered();
            }

            public void vmStartEvent() {
                enableRecording(_includeInitSyncPoints);
            }

            public void updateSynchronizationPointsImmediately(boolean force, String desc) {
                if (_view != null) {
                    processSynchronizationPointsImmediately(force, desc);
                }
            }
        });
        _eventThread.setEventRequests();
        _eventThread.start();
        redirectOutput();
        _vm.resume();
        _startTime = System.currentTimeMillis();
        if (_auto) {
            Thread updaterThread = new Thread(new Runnable() {

                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(_autoDelay);
                            if (!isUpdateDelayed()) {
                                processSynchronizationPoints(false, "timed");
                                if (_view != null) {
                                    _view.updateList(getThreadList());
                                }
                            }
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
            updaterThread.setDaemon(true);
            updaterThread.start();
        }
        try {
            _eventThread.join();
            _errThread.join();
            _outThread.join();
        } catch (InterruptedException exc) {
        }
        if (_view != null) {
            _view.setProjectInWindowTitle("[stopped]");
        }
        if (_processObjectSyncPoints) {
            writer.println("// Total number of object sync points: " + getTotalNumSyncPoints());
        }
        writer.println("// Total number of compact sync points: " + getTotalNumCompactSyncPoints());
        writer.println("0 " + SyncPointBuffer.SP.END.intValue() + " // End of schedule");
        writer.flush();
        if (closeWriter) {
            writer.close();
        }
        _methodDatabase = null;
    }

    /**
     * Launches the debug VM.
     *
     * @param cmdLine command line
     * @return VirtualMachine interface
     */
    VirtualMachine launchDebugVM(String cmdLine) {
        LaunchingConnector connector = findLaunchingConnector();
        Debug.out.println(DRV, "Connector = " + connector.name() + ": " + connector.description());
        Map<String, Connector.Argument> arguments = connectorArguments(connector, cmdLine);
        Debug.out.println(DRV, "Arguments:");
        for (String k : arguments.keySet()) {
            Connector.Argument a = arguments.get(k);
            Debug.out.println(DRV, "Name = " + a.name() + ": " + a.description());
            Debug.out.println(DRV, "Value = " + a.value());
        }
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

    /**
     * Redirects output from debugged program to console.
     */
    void redirectOutput() {
        Process process = _vm.process();
        _errThread = new StreamRedirectThread("error reader", process.getErrorStream(), System.err);
        _outThread = new StreamRedirectThread("output reader", process.getInputStream(), System.out);
        _errThread.start();
        _outThread.start();
    }

    /**
     * Finds a com.sun.jdi.CommandLineLaunch connector.
     *
     * @return LaunchingConnector interface
     */
    LaunchingConnector findLaunchingConnector() {
        List<Connector> connectors = Bootstrap.virtualMachineManager().allConnectors();
        for (Connector connector : connectors) {
            if (connector.name().equals("com.sun.jdi.CommandLineLaunch")) {
                return (LaunchingConnector) connector;
            }
        }
        throw new Error("No launching connector");
    }

    /**
     * Returns the launching connector's arguments.
     *
     * @param connector launching connector interface
     * @param cmdLine   command line
     * @return argument map
     */
    Map<String, Connector.Argument> connectorArguments(LaunchingConnector connector, String cmdLine) {
        Map<String, Connector.Argument> arguments = connector.defaultArguments();
        Connector.Argument mainArg = arguments.get("main");
        if (mainArg == null) {
            throw new Error("Bad launching connector");
        }
        mainArg.setValue(cmdLine);
        return arguments;
    }

    /**
     * Prints command line help message.
     */
    void help() {
        System.err.println("Usage: java edu.rice.cs.cunit.record.Record [options] <class> [args]");
        System.err.println("[options] are:");
        System.err.println("  -quiet               No trace output");
        System.err.println("  -headless            No GUI");
        System.err.println("  -output <filename>   Output trace to <filename>");
        System.err.println("  -auto [n]            Automatically update on thread starts/stops.");
        System.err.println("                          Optional: n=delay in ms, n>=100. Default: 1000");
        System.err.println("  -obj                 Also process object sync points");
        System.err.println("  -debug               Process compact sync points with debug information");
        System.err.println("  -methoddb <filename> Specify <filename> as method database");
        System.err.println("  -initsp              Process sync points during VM initialization");
        System.err.println("  -termsp              Process sync points during VM termination");
        System.err.println("  -D <dir>             Set current directory (\"user.dir\") for debug JVM");
        System.err.println("  -cp <classpath>      Set classpath (\"java.class.path\") for debug JVM");
        System.err.println("  -drj <filename>      Set jar file used to start DrJava");
        System.err.println("  -sp <sourcepath>     Set source path to find source files");
        System.err.println("  -help                Print this help message");
        System.err.println("  -J                   Pass all following options to debug JVM");
        System.err.println("<class> is the program to trace");
        System.err.println("[args] are the arguments to <class>");
    }

    /**
     * Returns an array of thread list entries that the monitor knows about.
     * @return array of thread list entries.
     */
    private IThreadInfo[] getThreadList() {
        List<IThreadInfo> list = new ArrayList<IThreadInfo>((_objectProcessor != null ? _objectProcessor : _processor).getThreads().values());
        Collections.sort(list, new Comparator<IThreadInfo>() {

            public int compare(IThreadInfo o1, IThreadInfo o2) {
                return (int) (o1.getThreadID() - o2.getThreadID());
            }
        });
        return list.toArray(new IThreadInfo[0]);
    }

    /**
     * Detects the cycles in the wait graph.
     * @return list with cycles.
     */
    public List<List<IThreadInfo>> getCycles() {
        return (_objectProcessor != null ? _objectProcessor : _processor).getCycles();
    }

    /**
     * Processes the synchronization points since the last request and then clears the list in the
     * debugged VM.
     * @param force true if processing should be forced, even if debugged VM is in the process of changing the list
     * @param desc description of the request
     */
    private void processSynchronizationPoints(boolean force, String desc) {
        if (_vm == null || _eventThread == null || !_eventThread.isConnected()) {
            return;
        }
        _writer.println("// Note: Attempting to process sync points - " + desc);
        _vm.suspend();
        try {
            List<ReferenceType> classes = _vm.classesByName("edu.rice.cs.cunit.SyncPointBuffer");
            ClassType bufferClass = null;
            ClassObjectReference bufferClassObject = null;
            for (ReferenceType cl : classes) {
                if (cl.name().equals("edu.rice.cs.cunit.SyncPointBuffer")) {
                    if (cl instanceof ClassType) {
                        bufferClass = (ClassType) cl;
                        bufferClassObject = bufferClass.classObject();
                        break;
                    }
                }
            }
            if (null == bufferClass) {
                throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer class.");
            }
            Field nextObjectIDField = bufferClass.fieldByName("_nextObjectID");
            if (null == nextObjectIDField) {
                throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._nextObjectID field.");
            }
            Value nextObjectIDValue = bufferClass.getValue(nextObjectIDField);
            if (!(nextObjectIDValue instanceof LongValue)) {
                throw new Error("Unexpected type for edu.rice.cs.cunit.SyncPointBuffer._nextObjectID.");
            }
            LongValue nextObjectID = (LongValue) nextObjectIDValue;
            setProperty("nextObjectID", nextObjectID.longValue());
            Field nextThreadIDField = bufferClass.fieldByName("_nextThreadID");
            if (null == nextThreadIDField) {
                throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._nextThreadID field.");
            }
            Value nextThreadIDValue = bufferClass.getValue(nextThreadIDField);
            if (!(nextThreadIDValue instanceof LongValue)) {
                throw new Error("Unexpected type for edu.rice.cs.cunit.SyncPointBuffer._nextThreadID.");
            }
            LongValue nextThreadID = (LongValue) nextThreadIDValue;
            setProperty("nextThreadID", nextThreadID.longValue());
            boolean success = _processor.process(force, bufferClass, bufferClassObject, _methodDatabase);
            if (_objectProcessor != null) {
                success &= _objectProcessor.process(force, bufferClass, bufferClassObject, _methodDatabase);
            }
            if (success) {
                setProperty("numUpdates", getNumUpdates() + 1);
                setProperty("isUpdateDelayed", false);
                _transferHistoryModel.add(((Integer) getProperty("numSyncPointsInList")), ((Integer) getProperty("numSyncPointsInCompactList")), System.currentTimeMillis() - _startTime, desc);
            } else {
                _writer.println("// Note: Postponing update. Slave VM in edu.rice.cs.cunit.SyncPointBuffer.add or compactAdd.");
                _writer.flush();
                _eventThread.setSyncPointBufferExitRequestEnable(true, desc);
                setProperty("numDelayedUpdates", getNumDelayedUpdates() + 1);
                setProperty("isUpdateDelayed", true);
            }
        } finally {
            _writer.println("// Note: Finished processing sync points - " + desc);
            int userThreads = (Integer) getProperty("userThreads");
            boolean transferImmediately = (Boolean) getProperty("transferImmediately");
            if ((userThreads == 0) && _includeTermSyncPoints && !transferImmediately) {
                enableImmediateTransfers();
            }
            _vm.resume();
            _infoTableModel.fireTableDataChanged();
        }
    }

    /**
     * Processes the current synchronization point immediately.
     * @param force true if processing should be forced, even if debugged VM is in the process of changing the list
     * @param desc description of the request
     */
    private void processSynchronizationPointsImmediately(boolean force, String desc) {
        if (_vm == null || _eventThread == null || !_eventThread.isConnected()) {
            return;
        }
        _writer.println("// Note: Attempting to process sync point immediately - " + desc);
        _vm.suspend();
        try {
            List<ReferenceType> classes = _vm.classesByName("edu.rice.cs.cunit.SyncPointBuffer");
            ClassType bufferClass = null;
            ClassObjectReference bufferClassObject = null;
            for (ReferenceType cl : classes) {
                if (cl.name().equals("edu.rice.cs.cunit.SyncPointBuffer")) {
                    if (cl instanceof ClassType) {
                        bufferClass = (ClassType) cl;
                        bufferClassObject = bufferClass.classObject();
                        break;
                    }
                }
            }
            if (null == bufferClass) {
                throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer class.");
            }
            Field nextObjectIDField = bufferClass.fieldByName("_nextObjectID");
            if (null == nextObjectIDField) {
                throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._nextObjectID field.");
            }
            Value nextObjectIDValue = bufferClass.getValue(nextObjectIDField);
            if (!(nextObjectIDValue instanceof LongValue)) {
                throw new Error("Unexpected type for edu.rice.cs.cunit.SyncPointBuffer._nextObjectID.");
            }
            LongValue nextObjectID = (LongValue) nextObjectIDValue;
            setProperty("nextObjectID", nextObjectID.longValue());
            Field nextThreadIDField = bufferClass.fieldByName("_nextThreadID");
            if (null == nextThreadIDField) {
                throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._nextThreadID field.");
            }
            Value nextThreadIDValue = bufferClass.getValue(nextThreadIDField);
            if (!(nextThreadIDValue instanceof LongValue)) {
                throw new Error("Unexpected type for edu.rice.cs.cunit.SyncPointBuffer._nextThreadID.");
            }
            LongValue nextThreadID = (LongValue) nextThreadIDValue;
            setProperty("nextThreadID", nextThreadID.longValue());
            boolean success = _processor.processImmediately(force, bufferClass, bufferClassObject, _methodDatabase);
            if (_objectProcessor != null) {
                success &= _objectProcessor.processImmediately(force, bufferClass, bufferClassObject, _methodDatabase);
            }
            if (success) {
                setProperty("numUpdates", getNumUpdates() + 1);
                setProperty("isUpdateDelayed", false);
                _transferHistoryModel.add(((Integer) getProperty("numSyncPointsInList")), ((Integer) getProperty("numSyncPointsInCompactList")), System.currentTimeMillis() - _startTime, desc);
            } else {
                _writer.println("// Note: Postponing update. Slave VM in edu.rice.cs.cunit.SyncPointBuffer.add or compactAdd.");
                _writer.flush();
                _eventThread.setSyncPointBufferExitRequestEnable(true, desc);
                setProperty("numDelayedUpdates", getNumDelayedUpdates() + 1);
                setProperty("isUpdateDelayed", true);
            }
        } finally {
            _writer.println("// Note: Finished processing sync points - " + desc);
            _vm.resume();
            _infoTableModel.fireTableDataChanged();
        }
    }

    /**
     * Returns the number of delayed updates.
     * @return number of delayed updates.
     */
    public int getNumDelayedUpdates() {
        return (Integer) getProperty("numDelayedUpdates");
    }

    /**
     * Returns the number of successful updates.
     * @return number of updates.
     */
    public int getNumUpdates() {
        return (Integer) getProperty("numUpdates");
    }

    /**
     * Number of sync points in the list at the last update attempt.
     * @return number of sync points
     */
    public int getNumSyncPointsInList() {
        return (Integer) getProperty("numSyncPointsInList");
    }

    /**
     * Number of sync points in the compact list at the last update attempt.
     * @return number of compact sync points
     */
    public int getNumSyncPointsInCompactList() {
        return (Integer) getProperty("numSyncPointsInCompactList");
    }

    /**
     * Number of object sync points written to trace file.
     * @return number of object sync points written to trace file
     */
    public int getTotalNumSyncPoints() {
        return (Integer) getProperty("numTotalSyncPoints");
    }

    /**
     * Number of compact sync points written to trace file.
     * @return number of compact sync points written to trace file
     */
    public int getTotalNumCompactSyncPoints() {
        return (Integer) getProperty("numTotalCompactSyncPoints");
    }

    /**
     * Returns the next object ID to be assigned.
     * @return next object ID
     */
    public long getNextObjectID() {
        return (Long) getProperty("nextObjectID");
    }

    /**
     * Returns the next thread ID to be assigned.
     * @return next object ID
     */
    public long getNextThreadID() {
        return (Long) getProperty("nextThreadID");
    }

    /**
     * Returns true if the update has been delayed.
     * @return true if update is delayed.
     */
    public boolean isUpdateDelayed() {
        return (Boolean) getProperty("isUpdateDelayed");
    }

    /**
     * Returns the value of the property with the given name.
     * @param propName name of the property
     * @return property value
     */
    public Object getProperty(String propName) {
        return _infoTableModel.getProperty(propName);
    }

    /**
     * Sets the property with the given name to the specified value.
     * @param propName name of the property
     * @param newValue new value of the property
     */
    public void setProperty(String propName, Object newValue) {
        _infoTableModel.setProperty(propName, newValue);
    }

    /**
     * Sets the "mainEntered" property to enable recording. VM must be suspended and will not be resumed.
     */
    public void setMainEntered() {
        setProperty("mainEntered", true);
        _writer.println("// Note: main method entered");
        try {
            List<ReferenceType> classes = _vm.classesByName("edu.rice.cs.cunit.SyncPointBuffer");
            ClassType bufferClass = null;
            for (ReferenceType cl : classes) {
                if (cl.name().equals("edu.rice.cs.cunit.SyncPointBuffer")) {
                    if (cl instanceof ClassType) {
                        bufferClass = (ClassType) cl;
                        break;
                    }
                }
            }
            if (null == bufferClass) {
                throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer class.");
            }
            Field compactIndexField = bufferClass.fieldByName("_compactIndex");
            if (null == compactIndexField) {
                throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._compactIndex field.");
            }
            Value compactIndexValue = bufferClass.getValue(compactIndexField);
            if (!(compactIndexValue instanceof IntegerValue)) {
                throw new Error("Unexpected type for edu.rice.cs.cunit.SyncPointBuffer._compactIndex.");
            }
            IntegerValue compactIndex = (IntegerValue) compactIndexValue;
            setProperty("numCompactSyncPointsBeforeMain", getTotalNumCompactSyncPoints() + (compactIndex.intValue() / SyncPointBuffer.COMPACT_DEBUG_RECORD_SIZE));
            _writer.println("// Note: " + getProperty("numCompactSyncPointsBeforeMain") + " compact sync points before main");
        } finally {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    _infoTableModel.fireTableDataChanged();
                }
            });
        }
        enableRecording(true);
    }

    /**
     * Sets the "allUserThreadsDied" property; if sync points in termination are not desired, then recording will be
     * disabled. VM must be suspended and will not be resumed.
     */
    public void setAllUserThreadsDied() {
        setProperty("allUserThreadsDied", true);
        _writer.println("// Note: all user threads died");
        enableRecording(_includeTermSyncPoints);
    }

    /**
     * Enable or disable recording by changing SyncPointBuffer._recording. VM must be suspended and will not be resumed.
     * @param state true to enable recording, false to disable recording.
     */
    private void enableRecording(boolean state) {
        if (getProperty("recording").equals(state)) {
            return;
        }
        _writer.println("// Note: Setting recording to " + state);
        try {
            List<ReferenceType> classes = _vm.classesByName("edu.rice.cs.cunit.SyncPointBuffer");
            ClassType bufferClass = null;
            for (ReferenceType cl : classes) {
                if (cl.name().equals("edu.rice.cs.cunit.SyncPointBuffer")) {
                    if (cl instanceof ClassType) {
                        bufferClass = (ClassType) cl;
                        break;
                    }
                }
            }
            if (null == bufferClass) {
                throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer class.");
            }
            Field replayingField = bufferClass.fieldByName("_recording");
            if (null == replayingField) {
                throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._recording field.");
            }
            bufferClass.setValue(replayingField, _vm.mirrorOf(state));
        } catch (InvalidTypeException e) {
            throw new Error("Could not set SyncPointBuffer._recording.", e);
        } catch (ClassNotLoadedException e) {
            throw new Error("Could not set SyncPointBuffer._recording.", e);
        } catch (Error error) {
            throw new Error("Could not set SyncPointBuffer._recording.", error);
        } finally {
            _writer.flush();
            setProperty("recording", state);
        }
    }

    /**
     * Sets the "transferImmediately" property to enable immediate transfers. VM must be suspended and will not be resumed.
     */
    public void enableImmediateTransfers() {
        setProperty("transferImmediately", true);
        _writer.println("// Note: enabling immediate transfers");
        try {
            List<ReferenceType> classes = _vm.classesByName("edu.rice.cs.cunit.SyncPointBuffer");
            ClassType bufferClass = null;
            for (ReferenceType cl : classes) {
                if (cl.name().equals("edu.rice.cs.cunit.SyncPointBuffer")) {
                    if (cl instanceof ClassType) {
                        bufferClass = (ClassType) cl;
                        break;
                    }
                }
            }
            if (null == bufferClass) {
                throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer class.");
            }
            Field transferImmediatelyField = bufferClass.fieldByName("_transferImmediately");
            if (null == transferImmediatelyField) {
                throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._transferImmediately field.");
            }
            try {
                bufferClass.setValue(transferImmediatelyField, _vm.mirrorOf(true));
            } catch (InvalidTypeException e) {
                throw new Error("Could not set edu.rice.cs.cunit._transferImmediately to true.", e);
            } catch (ClassNotLoadedException e) {
                throw new Error("Could not set edu.rice.cs.cunit._transferImmediately to true.", e);
            }
        } finally {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    _infoTableModel.fireTableDataChanged();
                }
            });
        }
        enableRecording(true);
    }

    /**
     * Open the source specified by class, method and PC.
     * @param link string with class, method and PC, formatted as "<class>.<method><descriptor> PC=<pc>"
     * @return true if source could be opened
     */
    public boolean openSource(String link) {
        final String str = " PC=";
        final int pcPos = link.indexOf(str);
        String classAndMethods = link.substring(0, pcPos);
        int dotIndex = classAndMethods.lastIndexOf('.');
        final String className = classAndMethods.substring(0, dotIndex);
        final String methodName = classAndMethods.substring(dotIndex + 1);
        int pc;
        try {
            pc = Integer.parseInt(link.substring(pcPos + str.length()));
        } catch (NumberFormatException e) {
            return false;
        }
        String[] ps = _classPath.split(java.io.File.pathSeparator);
        ArrayList<String> list = new ArrayList<String>();
        for (String s : ps) {
            list.add(s);
        }
        ps = System.getProperty("sun.boot.class.path").split(java.io.File.pathSeparator);
        for (String s : ps) {
            list.add(s);
        }
        ClassFileTools.ClassLocation cl = null;
        try {
            cl = ClassFileTools.findClassFile(className, list);
            if (cl == null) {
                return false;
            }
            SourceFileAttributeInfo sf = (SourceFileAttributeInfo) cl.getClassFile().getAttribute(SourceFileAttributeInfo.getAttributeName());
            String fileName = sf.getSourceFileName().toString();
            String packageName = cl.getClassFile().getThisClassName();
            dotIndex = packageName.lastIndexOf('.');
            if (dotIndex >= 0) {
                packageName = packageName.substring(0, dotIndex).replace('.', File.separatorChar);
                fileName = packageName + File.separatorChar + fileName;
            } else {
                packageName = "";
            }
            String[] paths = _sourcePath.split(File.pathSeparator);
            File sourceFile = null;
            for (String s : paths) {
                File f = new File(s, fileName);
                if (f.exists()) {
                    sourceFile = f;
                    break;
                }
            }
            if (sourceFile == null) {
                return false;
            }
            int lineNo = -1;
            for (MethodInfo mi : cl.getClassFile().getMethods()) {
                if ((mi.getName().toString() + mi.getDescriptor().toString()).equals(methodName)) {
                    for (AAttributeInfo ai : mi.getCodeAttributeInfo().getAttributes()) {
                        if (ai.getName().toString().equals(LineNumberTableAttributeInfo.getAttributeName())) {
                            LineNumberTableAttributeInfo lntai = (LineNumberTableAttributeInfo) ai;
                            LineNumberTableAttributeInfo.LineNumberRecord[] lns = lntai.getLineNumbers();
                            for (LineNumberTableAttributeInfo.LineNumberRecord l : lns) {
                                if (l.startPC >= pc) {
                                    lineNo = l.lineNo;
                                    break;
                                }
                            }
                        }
                        if (lineNo >= 0) {
                            break;
                        }
                    }
                }
                if (lineNo >= 0) {
                    break;
                }
            }
            if (lineNo == -1) {
                return false;
            }
            try {
                Process p = ExecJVM.runJVM("edu.rice.cs.drjava.DrJava", new String[] { sourceFile.toString() + File.pathSeparator + lineNo }, _drJavaJarFile.getAbsolutePath() + File.pathSeparator + System.getProperty("java.class.path"), new String[] {}, new File("."));
                return (p != null);
            } catch (IOException ioe) {
                return false;
            }
        } finally {
            try {
                if (cl != null) {
                    cl.close();
                }
            } catch (IOException e) {
                return false;
            }
        }
    }
}
