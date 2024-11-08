package edu.clemson.cs.nestbed.server.management.instrumentation;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.clemson.cs.nestbed.common.management.instrumentation.ProgramProbeManager;
import edu.clemson.cs.nestbed.common.management.instrumentation.ProgramWeaverManager;
import edu.clemson.cs.nestbed.common.management.configuration.ProgramManager;
import edu.clemson.cs.nestbed.common.model.Mote;
import edu.clemson.cs.nestbed.common.model.MoteTestbedAssignment;
import edu.clemson.cs.nestbed.common.model.Program;
import edu.clemson.cs.nestbed.common.trace.StaticProgramData;
import edu.clemson.cs.nestbed.common.trace.TraceEntry;
import edu.clemson.cs.nestbed.common.trace.TraceMessage;
import edu.clemson.cs.nestbed.server.management.configuration.MoteManagerImpl;
import edu.clemson.cs.nestbed.server.management.configuration.ProgramManagerImpl;
import edu.clemson.cs.nestbed.server.management.instrumentation.ProgramWeaverManagerImpl;
import edu.clemson.cs.nesctk.NescToolkit;
import edu.clemson.cs.nesctk.SourceFile;
import edu.clemson.cs.nesctk.records.FunctionCalleeRecord;
import edu.clemson.cs.nesctk.records.FunctionRecord;
import edu.clemson.cs.nesctk.records.TraceFunctionRecord;
import edu.clemson.cs.nesctk.records.TraceModuleRecord;
import edu.clemson.cs.nesctk.records.WiringRecord;
import edu.clemson.cs.nesctk.tools.trace.instrumentor.TraceInstrumentor;
import edu.clemson.cs.nesctk.tools.trace.FunctionTraceData;
import edu.clemson.cs.nesctk.tools.trace.RadioTraceData;
import edu.clemson.cs.nesctk.tools.trace.TraceData;
import edu.clemson.cs.nesctk.util.AstUtils;
import edu.clemson.cs.nesctk.util.FileUtils;
import net.tinyos.message.Message;
import net.tinyos.message.MessageListener;
import net.tinyos.message.MoteIF;
import net.tinyos.packet.BuildSource;
import net.tinyos.packet.PacketSource;
import net.tinyos.packet.PhoenixSource;

public class ProgramProbeManagerImpl extends UnicastRemoteObject implements ProgramProbeManager {

    private static final ProgramProbeManagerImpl instance;

    private static final Log log = LogFactory.getLog(ProgramProbeManagerImpl.class);

    private static final String NESTBED_NESC_ROOT;

    private static final TraceEntry TRACE_ENTRY = new TraceEntry();

    private static final TraceMessage TRACE_MESSAGE = new TraceMessage();

    static {
        String property;
        property = "nestbed.dir.lib.nesc";
        String rootStr = System.getProperty(property);
        File root = null;
        if (rootStr == null) {
            log.fatal("Property '" + property + "' is not set");
            System.exit(1);
        } else {
            root = new File(rootStr);
            if (!root.exists()) {
                log.fatal("Directory " + rootStr + " does not exist!");
                System.exit(1);
            }
        }
        NESTBED_NESC_ROOT = root.getAbsolutePath();
        log.info(property + " = " + NESTBED_NESC_ROOT);
        ProgramProbeManagerImpl impl = null;
        try {
            impl = new ProgramProbeManagerImpl();
        } catch (Exception ex) {
            log.fatal("Unable to create singleton instance", ex);
            System.exit(1);
        } finally {
            instance = impl;
        }
    }

    private ProgramManager programManager = ProgramManagerImpl.getInstance();

    private ProgramWeaverManager programWeaverManager = ProgramWeaverManagerImpl.getInstance();

    public static ProgramProbeManagerImpl getInstance() {
        return instance;
    }

    private class TraceMessageListener implements MessageListener {

        private MoteIF moteIf;

        private List<TraceData> traceData = new ArrayList<TraceData>();

        public TraceMessageListener(MoteIF moteIf) {
            this.moteIf = moteIf;
        }

        public synchronized void messageReceived(int toAddr, Message message) {
            if (message instanceof TraceEntry) {
                TraceEntry entry = (TraceEntry) message;
                if (entry.get_type() == 0 || entry.get_type() == 1) {
                    traceData.add(new FunctionTraceData(entry.get_type(), entry.get_u_callTrace_moduleId(), entry.get_u_callTrace_functionId()));
                } else {
                    traceData.add(new RadioTraceData(entry.get_type(), entry.get_u_radioTrace_address(), entry.get_u_radioTrace_magic()));
                }
            } else if (message instanceof TraceMessage) {
                moteIf.deregisterListener(TRACE_ENTRY, this);
                moteIf.deregisterListener(TRACE_MESSAGE, this);
                notifyAll();
            }
        }

        public List<TraceData> getTraceData() {
            return traceData;
        }
    }

    public Map<Integer, List<TraceData>> collectData(List<MoteTestbedAssignment> interestingMotes) throws RemoteException {
        Map<Integer, List<TraceData>> map = new TreeMap<Integer, List<TraceData>>();
        for (MoteTestbedAssignment mta : interestingMotes) {
            try {
                Mote mote = MoteManagerImpl.getInstance().getMote(mta.getMoteID());
                int address = mta.getMoteAddress();
                String moteSerialID = mote.getMoteSerialID();
                String tosPlatform = "telosb";
                String commPort = "/dev/motes/" + moteSerialID;
                PacketSource packetSource;
                PhoenixSource phoenixSource;
                MoteIF moteIf;
                TraceMessageListener listener;
                packetSource = BuildSource.makePacketSource("serial@" + commPort + ":telos");
                phoenixSource = BuildSource.makePhoenix(packetSource, null);
                moteIf = new MoteIF(phoenixSource);
                listener = new TraceMessageListener(moteIf);
                moteIf.registerListener(TRACE_ENTRY, listener);
                moteIf.registerListener(TRACE_MESSAGE, listener);
                moteIf.send(address, new TraceMessage());
                synchronized (listener) {
                    try {
                        listener.wait();
                    } catch (Exception ex) {
                    }
                }
                phoenixSource.shutdown();
                packetSource.close();
                map.put(address, listener.getTraceData());
            } catch (IOException ex) {
                log.error("Exception while collecting trace data", ex);
            }
        }
        return map;
    }

    public Map<String, List<String>> getModuleFunctionListMap(int programID) throws RemoteException {
        log.debug("getModuleFunctionListMap called");
        try {
            Program program = programManager.getProgram(programID);
            File makefile = new File(program.getSourcePath(), "Makefile");
            String topLevelConfiguration = programWeaverManager.findComponentFromMakefile(makefile);
            log.info("Top-level config: " + topLevelConfiguration);
            File topLevelFile = new File(program.getSourcePath(), topLevelConfiguration + ".nc").getAbsoluteFile();
            File analysisDir = new File(program.getSourcePath()).getAbsoluteFile();
            log.info("Top-Level file:    " + topLevelFile.getAbsolutePath());
            log.info("Basedir directory: " + analysisDir.getAbsolutePath());
            NescToolkit toolkit = new NescToolkit(topLevelFile, analysisDir);
            toolkit.prependIncludePath(NESTBED_NESC_ROOT + "/ModifiedTinyOS");
            toolkit.addIncludePath(NESTBED_NESC_ROOT + "/TraceRecorder");
            toolkit.addIncludePath(NESTBED_NESC_ROOT + "/MemoryProfiler");
            toolkit.addIncludePath(NESTBED_NESC_ROOT + "/NestbedControl");
            toolkit.appendGccArgument("-I");
            toolkit.appendGccArgument(NESTBED_NESC_ROOT + "/ModifiedTinyOS");
            toolkit.appendGccArgument("-I");
            toolkit.appendGccArgument(NESTBED_NESC_ROOT + "/TraceRecorder");
            toolkit.appendGccArgument("-I");
            toolkit.appendGccArgument(NESTBED_NESC_ROOT + "/MemoryProfiler");
            toolkit.appendGccArgument("-I");
            toolkit.appendGccArgument(NESTBED_NESC_ROOT + "/NestbedControl");
            toolkit.load();
            Map<String, SourceFile> sourceFiles = toolkit.getSourceFileMap();
            return AstUtils.buildModuleFunctionListMap(sourceFiles);
        } catch (Exception ex) {
            throw new RemoteException("Exception:", ex);
        }
    }

    public void insertProbes(int programID, Map<String, List<String>> moduleIncludeMap) throws RemoteException {
        log.debug("insertProbes called");
        try {
            Program program = programManager.getProgram(programID);
            File makefile = new File(program.getSourcePath(), "Makefile");
            String topLevelConfiguration = programWeaverManager.findComponentFromMakefile(makefile);
            String topLevelPath = new File(program.getSourcePath(), topLevelConfiguration + ".nc").getAbsolutePath();
            backup(program.getSourcePath());
            log.info("Top-level config: " + topLevelConfiguration);
            List<String> prependList = new ArrayList<String>(1);
            List<String> appendList = new ArrayList<String>(3);
            List<String> gccArgs = new ArrayList<String>(2);
            prependList.add(NESTBED_NESC_ROOT + "/ModifiedTinyOS");
            appendList.add(NESTBED_NESC_ROOT + "/TraceRecorder");
            appendList.add(NESTBED_NESC_ROOT + "/MemoryProfiler");
            appendList.add(NESTBED_NESC_ROOT + "/NestbedControl");
            gccArgs.add("-I");
            gccArgs.add(NESTBED_NESC_ROOT + "/TraceRecorder");
            gccArgs.add("-I");
            gccArgs.add(NESTBED_NESC_ROOT + "/ModifiedTinyOS");
            gccArgs.add("-I");
            gccArgs.add(NESTBED_NESC_ROOT + "/MemoryProfiler");
            gccArgs.add("-I");
            gccArgs.add(NESTBED_NESC_ROOT + "/NestbedControl");
            log.debug("Prepend list: " + prependList);
            log.debug("Append list:  " + appendList);
            File topLevelFile = new File(topLevelPath).getAbsoluteFile();
            File analysisDir = topLevelFile.getParentFile();
            log.info("Top-Level file:    " + topLevelFile.getAbsolutePath());
            log.info("Basedir directory: " + analysisDir.getAbsolutePath());
            TraceInstrumentor traceInstrumentor;
            traceInstrumentor = new TraceInstrumentor(prependList, appendList, gccArgs, topLevelFile.getAbsolutePath(), analysisDir);
            traceInstrumentor.setModuleIncludeMap(moduleIncludeMap);
            traceInstrumentor.enableTrace();
            traceInstrumentor.handleCommit();
            File source = new File(NESTBED_NESC_ROOT + "/TraceRecorder/TraceRecorder.h");
            File dest = new File(program.getSourcePath() + "/analysis/TraceRecorder.h");
            FileUtils.copyFile(source, dest);
            log.info("Moving source files from analysis directory down...");
            moveFiles(program.getSourcePath());
            traceInstrumentor.generateRadioHeader(program.getSourcePath());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public StaticProgramData getStaticData(Program program) throws RemoteException {
        log.debug("getStaticData called");
        StaticProgramData data = null;
        try {
            data = readStaticData(new File(program.getSourcePath() + "/staticTraceData.dat"));
        } catch (Exception ex) {
            log.error("Exception in getStaticData", ex);
            throw new RemoteException("Exception in getStaticData", ex);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private StaticProgramData readStaticData(File file) throws IOException, ClassNotFoundException {
        StaticProgramData data;
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
        data = new StaticProgramData((List<TraceModuleRecord>) in.readObject(), (List<TraceFunctionRecord>) in.readObject(), (Map<FunctionRecord, List<FunctionCalleeRecord>>) in.readObject(), (List<WiringRecord>) in.readObject());
        in.close();
        return data;
    }

    private void moveFiles(String path) throws IOException {
        File dest = new File(path).getAbsoluteFile();
        File src = new File(dest, "analysis");
        log.info("Moving " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
        FileUtils.copyDirectory(src, dest);
        FileUtils.deleteRecursive(src);
    }

    private void backup(String path) throws IOException {
        File directory = new File(path).getAbsoluteFile();
        File backup = new File(directory, "nb_backup");
        FileUtils.copyDirectory(directory, backup);
    }

    private ProgramProbeManagerImpl() throws RemoteException {
        super();
    }
}
