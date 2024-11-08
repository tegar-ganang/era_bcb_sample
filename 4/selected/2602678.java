package edu.rice.cs.cunit.record;

import edu.rice.cs.cunit.SyncPointBuffer2;
import edu.rice.cs.cunit.record.callTree.CallTree;
import edu.rice.cs.cunit.util.Debug;
import edu.rice.cs.cunit.util.ILambda;
import java.io.*;
import java.util.*;

/**
 * Processing for compact events with debug information.
 * @author Mathias Ricken
 */
public class CompactDebugProcessor2 {

    /**
     * Writer for output.
     */
    private PrintWriter _writer;

    /**
     * Data input stream with binary sync points.
     */
    private DataInputStream _input;

    /**
     * Method database, class index/method index pairs as keys.
     */
    private HashMap<Long, String> _methodDatabase;

    /**
     * Index of the sync point counts just for the method.
     */
    private static final int METHOD_COUNTS_INDIVIDUAL = 0;

    /**
     * Index of the sync point counts for the method and all methods it called.
     */
    private static final int METHOD_COUNTS_DEPENDENT = 1;

    /**
     * Number of columns in the method counts.
     */
    private static final int METHOD_COUNTS_COLUMNS = 2;

    /**
     * Method counts, class index/method index pairs as keys.
     */
    private HashMap<Long, long[]> _methodCounts;

    /**
     * Call tree.
     */
    private CallTree<Long> _callTree;

    /**
     * Number of user threads.
     */
    private int _userThreads = 1;

    /**
     * Total number of sync points processed.
     */
    private long _numTotalSyncPoints = 0;

    /**
     * Whether the user wants counts.
     */
    private boolean _enableCounts = true;

    /**
     * Whether the user wants sync points.
     */
    private boolean _enableSyncPoints = true;

    /**
     * Whether the user wants stack traces.
     */
    private boolean _enableStackTraces = true;

    /**
     * Whether the user wants call trees.
     */
    private boolean _enableCallTrees = true;

    /**
     * Whether the user wants to see progress indicators.
     */
    private boolean _enableProgress = false;

    /**
     * Debug log name.
     */
    public static final String DRV2 = "record.verbose2";

    /**
     * Create a new processor for verbose object events.
     * @param writer writer for output
     * @param input input stream with binary sync points
     * @param methodDatabase method database
     * @param enableCounts whether the user wants counts
     * @param enableSyncPoints whether the user wants sync points
     * @param enableStackTraces whether the user wants stack traces
     * @param enableProgress whether the user wants to see progress indicators
     */
    public CompactDebugProcessor2(PrintWriter writer, DataInputStream input, HashMap<Long, String> methodDatabase, boolean enableCounts, boolean enableSyncPoints, boolean enableStackTraces, boolean enableCallTrees, boolean enableProgress) {
        _writer = writer;
        _input = input;
        _methodDatabase = methodDatabase;
        if (enableCounts) {
            _methodCounts = new HashMap<Long, long[]>();
        }
        if (enableCallTrees) {
            _callTree = new CallTree<Long>(CallTree.Long0.ONLY, CallTree.LongCountTops.ONLY);
        }
        _enableCounts = enableCounts;
        _enableSyncPoints = enableSyncPoints;
        _enableStackTraces = enableStackTraces;
        _enableCallTrees = enableCallTrees;
        _enableProgress = enableProgress;
    }

    /**
     * Processes the compact synchronization points since the last request and then clears the list in the
     * debugged VM.
     * @return true if update was successful, false if it was delayed
     */
    public boolean process() {
        try {
            long read = 0;
            long lastTime = System.currentTimeMillis();
            long firstTime = lastTime;
            while (_input.available() > 0) {
                long tid = _input.readLong();
                long code = _input.readLong();
                long classIndex = _input.readLong();
                long methodAndPC = _input.readLong();
                long stackTraceLength = _input.readLong();
                long[] stackTrace = new long[(int) stackTraceLength];
                for (int i = 0; i < stackTraceLength; ++i) {
                    stackTrace[i] = _input.readLong();
                }
                read += 8 * (5 + stackTraceLength);
                processSyncPoint(tid, code, classIndex, methodAndPC, 0, stackTrace);
                if (System.currentTimeMillis() - 5000 > lastTime) {
                    if (System.in.available() > 0) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                        String line = br.readLine();
                        if (line.equals("quit")) {
                            lastTime = System.currentTimeMillis();
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            float percent = ((float) read) / (read + _input.available());
                            float timeLeft = (lastTime - firstTime) / percent * (100 - percent);
                            pw.printf("%5.1f", percent);
                            _writer.println("// User aborted, " + sw.toString() + "%, " + _numTotalSyncPoints + " sync points, remaining: " + edu.rice.cs.cunit.util.StringOps.toStringMillis((long) timeLeft));
                            _writer.flush();
                            break;
                        }
                    }
                }
                if (_enableProgress && (System.currentTimeMillis() - 5000 > lastTime)) {
                    lastTime = System.currentTimeMillis();
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    float percent = 100 * ((float) read) / (read + _input.available());
                    float timeLeft = (lastTime - firstTime) / percent * (100 - percent);
                    pw.printf("%5.1f", percent);
                    System.out.println("// " + sw.toString() + "%, " + _numTotalSyncPoints + " sync points, remaining: " + edu.rice.cs.cunit.util.StringOps.toStringMillis((long) timeLeft));
                    System.out.flush();
                }
            }
            _writer.flush();
        } catch (IOException e) {
            throw new Error("Could not process sync points:", e);
        }
        int methodCountSortIndex = METHOD_COUNTS_INDIVIDUAL;
        if (_enableCallTrees) {
            _writer.println("Legend: own/dependent location");
            _callTree.print(_writer, new ILambda<String, CallTree.Node<Long>>() {

                public String apply(CallTree.Node<Long> node) {
                    long v = node.getValue();
                    long sum = 0;
                    Stack<CallTree.Node<Long>> toProcess = new Stack<CallTree.Node<Long>>();
                    toProcess.add(node);
                    while (!toProcess.empty()) {
                        CallTree.Node<Long> top = toProcess.pop();
                        toProcess.addAll(top.getChildren().values());
                        sum += top.getValue();
                    }
                    if (_enableCounts) {
                        long[] counts = _methodCounts.get(node.getStackTraceElement().getHash());
                        if (counts != null) {
                            counts[METHOD_COUNTS_DEPENDENT] += sum;
                            _methodCounts.put(node.getStackTraceElement().getHash(), counts);
                        }
                    }
                    return String.valueOf(v) + "/" + sum;
                }
            });
            methodCountSortIndex = METHOD_COUNTS_DEPENDENT;
        }
        final int finalMethodCountSortIndex = methodCountSortIndex;
        if (_enableCounts) {
            _writer.println("Legend: dependent (percentage)/own (percentage) location");
            List<Map.Entry<Long, long[]>> sorted = new ArrayList<Map.Entry<Long, long[]>>(_methodCounts.entrySet());
            Collections.sort(sorted, new Comparator<Map.Entry<Long, long[]>>() {

                public int compare(Map.Entry<Long, long[]> o1, Map.Entry<Long, long[]> o2) {
                    long[] v1 = o1.getValue();
                    long[] v2 = o2.getValue();
                    return v1[finalMethodCountSortIndex] < v2[finalMethodCountSortIndex] ? +1 : (v1[finalMethodCountSortIndex] > v2[finalMethodCountSortIndex] ? -1 : 0);
                }

                public boolean equals(Object obj) {
                    return (this == obj);
                }
            });
            for (Map.Entry<Long, long[]> e : sorted) {
                String methodString = _methodDatabase.get(e.getKey());
                if (methodString != null) {
                    _writer.printf("%10d (%5.1f)/%10d (%5.1f) %s\n", e.getValue()[METHOD_COUNTS_DEPENDENT], 100.0 / _numTotalSyncPoints * e.getValue()[METHOD_COUNTS_DEPENDENT], e.getValue()[METHOD_COUNTS_INDIVIDUAL], 100.0 / _numTotalSyncPoints * e.getValue()[METHOD_COUNTS_INDIVIDUAL], methodString);
                }
            }
        }
        _writer.flush();
        return true;
    }

    /**
     * Process a synchronization point.
     * @param tid thread ID
     * @param code code for the sync point
     * @param classIndex class index
     * @param methodAndPC method index and PC
     * @param oid object ID
     * @param stackTrace array with methodAndPC indices representing the stack trace
     */
    private void processSyncPoint(long tid, long code, long classIndex, long methodAndPC, long oid, long[] stackTrace) {
        long methodIndex = (methodAndPC >>> 32);
        long methodPC = (methodAndPC & 0xFFFF);
        String methodAndPCString = "";
        if ((classIndex != 0) && (methodIndex != 0)) {
            long key = (classIndex << 32) | methodIndex;
            methodAndPCString = _methodDatabase.get(key);
            if (methodAndPCString == null) {
                methodAndPCString = "";
            } else {
                methodAndPCString = " " + methodAndPCString + " PC=" + methodPC;
            }
            if (_enableCounts) {
                long[] count = _methodCounts.get(key);
                if (count == null) {
                    count = new long[METHOD_COUNTS_COLUMNS];
                }
                count[METHOD_COUNTS_INDIVIDUAL] += 1;
                _methodCounts.put(key, count);
            }
        }
        if (_enableSyncPoints) {
            _writer.printf("%d %d // %08x %04x %04x %10d%s%s", tid, code, classIndex, methodIndex, methodPC, oid, methodAndPCString, System.getProperty("line.separator"));
        }
        if (_enableStackTraces || _enableCallTrees) {
            CallTree.StackTraceElement[] stes = new CallTree.StackTraceElement[stackTrace.length];
            int index = 0;
            for (long classAndMethodIdx : stackTrace) {
                long classIdx = classAndMethodIdx >>> 32;
                long methodIdx = classAndMethodIdx & 0x00000000ffffffffL;
                String methodString = _methodDatabase.get(classAndMethodIdx);
                if (methodString == null) {
                    methodString = "???";
                    stes[index] = new CallTree.StackTraceElement("???", "???", "", -1, classAndMethodIdx);
                } else {
                    try {
                        int lastDot = methodString.lastIndexOf('.');
                        String cname = methodString.substring(0, lastDot);
                        String mname = methodString.substring(lastDot + 1);
                        stes[index] = new CallTree.StackTraceElement(cname, mname, "", -1, classAndMethodIdx);
                    } catch (StringIndexOutOfBoundsException e) {
                        System.out.println(methodString);
                    }
                }
                if (_enableStackTraces) {
                    _writer.printf("\t// at %08x %04x %s%s", classIdx, methodIdx, methodString, System.getProperty("line.separator"));
                }
                ++index;
            }
            if (_enableCallTrees) {
                _callTree.addStackTrace(stes);
            }
        }
        if (code == SyncPointBuffer2.SP.THREADSTART.intValue()) {
            ++_userThreads;
            if (_enableSyncPoints) {
                _writer.println("// user thread with id " + tid + " started; " + _userThreads + " remaining");
            }
        } else if (code == SyncPointBuffer2.SP.THREADEXIT.intValue()) {
            --_userThreads;
            if (_enableSyncPoints) {
                _writer.println("// user thread with id " + tid + " exited; " + _userThreads + " remaining");
            }
        }
        ++_numTotalSyncPoints;
    }

    /**
     * Main method to run the processor.
     */
    public static void main(String[] args) {
        HashMap<Long, String> methodDatabase = new HashMap<Long, String>();
        String inputName = SyncPointBuffer2.FILE_NAME;
        PrintWriter writer = new PrintWriter(System.out);
        boolean useDefaults = true;
        boolean enableCounts = false;
        boolean enableSyncPoints = false;
        boolean enableStackTraces = false;
        boolean enableCallTrees = false;
        boolean enableProgress = false;
        int inx;
        for (inx = 0; inx < args.length; ++inx) {
            String arg = args[inx];
            if ((arg.charAt(0) != '-') && (arg.charAt(0) != '+')) {
                break;
            }
            Debug.out.println(DRV2, "Argument " + inx + ": " + arg);
            if (arg.equals("-quiet")) {
                writer = new PrintWriter(new OutputStream() {

                    public void write(int b) {
                    }
                });
            } else if (arg.equals("-verbose")) {
                Debug.out.setDebug(true);
                if ((inx < args.length - 1) && (!args[inx + 1].startsWith("-"))) {
                    String logs = args[++inx].toLowerCase();
                    for (int c = 0; c < logs.length(); ++c) {
                        switch(logs.charAt(c)) {
                            case 'm':
                                {
                                    Debug.out.setLogTarget(DRV2, Debug.LogTarget.MAIN);
                                    break;
                                }
                            default:
                                {
                                    System.err.println("Unknown log character. Usage: -verbose [m].");
                                    System.exit(-1);
                                    break;
                                }
                        }
                    }
                } else {
                    Debug.out.setLogTarget(DRV2, Debug.LogTarget.MAIN);
                }
            } else if (arg.equals("-input")) {
                inputName = args[++inx];
            } else if (arg.equals("-output")) {
                try {
                    writer = new PrintWriter(new BufferedWriter(new FileWriter(args[++inx]), 1024 * 1024));
                } catch (IOException exc) {
                    System.err.println("Cannot open output file: " + args[inx] + " - " + exc);
                    System.exit(1);
                }
            } else if (arg.equals("-methoddb")) {
                try {
                    File db = new File(args[++inx]);
                    BufferedReader r = new BufferedReader(new FileReader(db));
                    String line;
                    while ((line = r.readLine()) != null) {
                        long classIdx = Long.valueOf(line.substring(0, line.indexOf(' ')), 16);
                        long methodIdx = Long.valueOf(line.substring(line.indexOf(' ') + 1, line.indexOf(':')), 16);
                        String classMethodName = line.substring(line.indexOf(':') + 2);
                        long classMethodIdx = (classIdx << 32) | methodIdx;
                        methodDatabase.put(classMethodIdx, classMethodName);
                    }
                    r.close();
                } catch (IOException exc) {
                    System.err.println("Cannot open method database file: " + args[inx] + " - " + exc);
                    System.exit(1);
                }
            } else if (arg.equals("+c")) {
                useDefaults = false;
                enableCounts = true;
            } else if (arg.equals("+s")) {
                useDefaults = false;
                enableSyncPoints = true;
            } else if (arg.equals("+st")) {
                useDefaults = false;
                enableStackTraces = true;
                enableSyncPoints = true;
            } else if (arg.equals("+p")) {
                useDefaults = false;
                enableProgress = true;
            } else if (arg.equals("+ct")) {
                useDefaults = false;
                enableCallTrees = true;
            } else if (arg.equals("-help")) {
                help();
                System.exit(0);
            } else {
                System.err.println("No option: " + arg);
                help();
                System.exit(1);
            }
        }
        try {
            if (useDefaults) {
                enableCounts = true;
                enableSyncPoints = true;
                enableStackTraces = true;
                enableCallTrees = true;
                enableProgress = false;
            }
            DataInputStream input = new DataInputStream(new FileInputStream(inputName));
            CompactDebugProcessor2 processor = new CompactDebugProcessor2(writer, input, methodDatabase, enableCounts, enableSyncPoints, enableStackTraces, enableCallTrees, enableProgress);
            processor.process();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * Prints command line help message.
     */
    static void help() {
        System.err.println("Usage: java edu.rice.cs.cunit.record.CompactDebugProcessor2 [options]");
        System.err.println("[options] are:");
        System.err.println("  -quiet               No trace output");
        System.err.println("  -input <filename>    Input trace from <filename> (default: " + SyncPointBuffer2.FILE_NAME + ")");
        System.err.println("  -output <filename>   Output trace to <filename>");
        System.err.println("  -methoddb <filename> Specify <filename> as method database");
        System.err.println("  +c                   Enable counts.");
        System.err.println("  +ct                  Enable call trees.");
        System.err.println("  +st                  Enable sync points and stack traces.");
        System.err.println("  +s                   Enable sync points.");
        System.err.println("  +p                   Enable progress indicators.");
        System.err.println("  -help                Print this help message");
        System.err.println("");
        System.err.println("Default is +c +st +ct. Using any of the + options disables the + options");
        System.err.println("that are not named.");
        System.err.println("Processing can be aborted by entering 'quit' (without the quotes) followed");
        System.err.println("by a newline on stdin.");
    }
}
