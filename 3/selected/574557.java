package se.sics.cooja.contikimote;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.log4j.Logger;
import org.jdom.Element;
import se.sics.cooja.AbstractionLevelDescription;
import se.sics.cooja.ClassDescription;
import se.sics.cooja.CoreComm;
import se.sics.cooja.GUI;
import se.sics.cooja.Mote;
import se.sics.cooja.MoteInterface;
import se.sics.cooja.MoteType;
import se.sics.cooja.ProjectConfig;
import se.sics.cooja.SectionMoteMemory;
import se.sics.cooja.Simulation;
import se.sics.cooja.dialogs.CompileContiki;
import se.sics.cooja.dialogs.ContikiMoteCompileDialog;
import se.sics.cooja.dialogs.MessageList;
import se.sics.cooja.dialogs.MessageList.MessageContainer;
import se.sics.cooja.util.StringUtils;

/**
 * The Contiki mote type holds the native library used to communicate with an
 * underlying Contiki system. All communication with that system should always
 * pass through this mote type.
 * <p>
 * This type also contains information about sensors and mote interfaces a mote
 * of this type has.
 * <p>
 * All core communication with the Contiki mote should be via this class. When a
 * mote type is created it allocates a CoreComm to be used with this type, and
 * loads the variable and segments addresses.
 * <p>
 * When a new mote type is created an initialization function is run on the
 * Contiki system in order to create the initial memory. When a new mote is
 * created the createInitialMemory() method should be called to get this initial
 * memory for the mote.
 *
 * @author Fredrik Osterlind
 */
@ClassDescription("Contiki Mote Type")
@AbstractionLevelDescription("OS level")
public class ContikiMoteType implements MoteType {

    private static Logger logger = Logger.getLogger(ContikiMoteType.class);

    public static final String ID_PREFIX = "mtype";

    /**
   * Library file suffix
   */
    public static final String librarySuffix = ".cooja";

    /**
   * Map file suffix
   */
    public static final String mapSuffix = ".map";

    /**
   * Make archive file suffix
   */
    public static final String dependSuffix = ".a";

    /**
   * Temporary output directory
   */
    public static final File tempOutputDirectory = new File("obj_cooja");

    /**
   * Communication stacks in Contiki.
   */
    public enum CommunicationStack {

        RIME, UIP, UIPV6;

        public String toString() {
            if (this == UIPV6) {
                return "uIPv6";
            }
            if (this == UIP) {
                return "uIPv4";
            }
            if (this == RIME) {
                return "Rime";
            }
            return "[unknown]";
        }

        public String getSourceFilenamesString() {
            if (this == UIPV6) {
                return " init-net-uipv6.c";
            }
            if (this == UIP) {
                return " init-net-uip.c";
            }
            if (this == RIME) {
                return " init-net-rime.c";
            }
            return " ";
        }

        public static CommunicationStack parse(String name) {
            if (name.equals("uIPv4") || name.equals("UIP")) {
                return UIP;
            }
            if (name.equals("uIPv6") || name.equals("UIPV6")) {
                return UIPV6;
            }
            if (name.equals("Rime") || name.equals("RIME")) {
                return RIME;
            }
            logger.warn("Can't parse communication stack name: " + name);
            return RIME;
        }
    }

    private final String[] sensors = { "button_sensor", "pir_sensor", "radio_sensor", "vib_sensor" };

    private String identifier = null;

    private String description = null;

    private File fileSource = null;

    private File fileFirmware = null;

    private String compileCommands = null;

    private File contikiApp = null;

    public File libSource = null;

    public File libFile = null;

    public File archiveFile = null;

    public File mapFile = null;

    public String javaClassName = null;

    private String[] coreInterfaces = null;

    private ArrayList<Class<? extends MoteInterface>> moteInterfacesClasses = null;

    private boolean hasSystemSymbols = false;

    private CommunicationStack commStack = CommunicationStack.RIME;

    private Simulation simulation = null;

    private ProjectConfig myConfig = null;

    private int relAddressOfReferenceVariable = 0;

    private CoreComm myCoreComm = null;

    private Properties varAddresses = new Properties();

    private SectionMoteMemory initialMemory = null;

    /**
   * Creates a new uninitialized Contiki mote type. This mote type needs to load
   * a library file and parse a map file before it can be used.
   */
    public ContikiMoteType() {
    }

    public Mote generateMote(Simulation simulation) {
        return new ContikiMote(this, simulation);
    }

    public boolean configureAndInit(Container parentContainer, Simulation simulation, boolean visAvailable) throws MoteTypeCreationException {
        this.simulation = simulation;
        myConfig = simulation.getGUI().getProjectConfig().clone();
        if (visAvailable) {
            if (getDescription() == null) {
                setDescription("Contiki Mote Type #" + (simulation.getMoteTypes().length + 1));
            }
            boolean compileOK = ContikiMoteCompileDialog.showDialog(parentContainer, simulation, this);
            if (!compileOK) {
                return false;
            }
        } else {
            if (getIdentifier() == null) {
                throw new MoteTypeCreationException("No identifier specified");
            }
            if (getContikiSourceFile() == null) {
                throw new MoteTypeCreationException("No Contiki application specified");
            }
            contikiApp = getContikiSourceFile();
            libSource = new File(contikiApp.getParentFile(), "obj_cooja/" + getIdentifier() + ".c");
            libFile = new File(contikiApp.getParentFile(), "obj_cooja/" + getIdentifier() + librarySuffix);
            archiveFile = new File(contikiApp.getParentFile(), "obj_cooja/" + getIdentifier() + dependSuffix);
            mapFile = new File(contikiApp.getParentFile(), "obj_cooja/" + getIdentifier() + mapSuffix);
            javaClassName = CoreComm.getAvailableClassName();
            if (javaClassName == null) {
                throw new MoteTypeCreationException("Could not allocate a core communicator!");
            }
            libSource.delete();
            libFile.delete();
            archiveFile.delete();
            mapFile.delete();
            try {
                CompileContiki.generateSourceFile(libSource, javaClassName, getSensors(), getCoreInterfaces());
            } catch (Exception e) {
                throw (MoteTypeCreationException) new MoteTypeCreationException("Error when generating Contiki main source").initCause(e);
            }
            String[][] env;
            try {
                env = CompileContiki.createCompilationEnvironment(getIdentifier(), contikiApp, mapFile, libFile, archiveFile, commStack);
            } catch (Exception e) {
                throw (MoteTypeCreationException) new MoteTypeCreationException("Error when creating environment: " + e.getMessage()).initCause(e);
            }
            String[] envOneDimension = new String[env.length];
            for (int i = 0; i < env.length; i++) {
                envOneDimension[i] = env[i][0] + "=" + env[i][1];
            }
            if (getCompileCommands() == null) {
                throw new MoteTypeCreationException("No compile commands specified");
            }
            final MessageList compilationOutput = new MessageList();
            String[] arr = getCompileCommands().split("\n");
            for (String cmd : arr) {
                if (cmd.trim().isEmpty()) {
                    continue;
                }
                try {
                    CompileContiki.compile(cmd, envOneDimension, null, getContikiSourceFile().getParentFile(), null, null, compilationOutput, true);
                } catch (Exception e) {
                    MoteTypeCreationException newException = new MoteTypeCreationException("Mote type creation failed: " + e.getMessage());
                    newException = (MoteTypeCreationException) newException.initCause(e);
                    newException.setCompilationOutput(compilationOutput);
                    MessageContainer[] messages = compilationOutput.getMessages();
                    for (int i = messages.length - 10; i < messages.length; i++) {
                        if (i < 0) {
                            continue;
                        }
                        logger.fatal(">> " + messages[i]);
                    }
                    logger.fatal("Compilation error: " + e.getMessage());
                    throw newException;
                }
            }
            if (getContikiFirmwareFile() == null || !getContikiFirmwareFile().exists()) {
                throw new MoteTypeCreationException("Contiki firmware file does not exist: " + getContikiFirmwareFile());
            }
        }
        doInit();
        return true;
    }

    public static File getExpectedFirmwareFile(File source) {
        File parentDir = source.getParentFile();
        String sourceNoExtension = source.getName().substring(0, source.getName().length() - 2);
        return new File(parentDir, sourceNoExtension + librarySuffix);
    }

    /**
   * For internal use.
   *
   * This method creates a core communicator linking a Contiki library and a Java class.
   * It furthermore parses library Contiki memory addresses and creates the initial memory.
   *
   * @throws MoteTypeCreationException
   */
    private void doInit() throws MoteTypeCreationException {
        if (myCoreComm != null) {
            throw new MoteTypeCreationException("Core communicator already used: " + myCoreComm.getClass().getName());
        }
        if (getContikiFirmwareFile() == null || !getContikiFirmwareFile().exists()) {
            throw new MoteTypeCreationException("Library file could not be found: " + getContikiFirmwareFile());
        }
        if (this.javaClassName == null) {
            throw new MoteTypeCreationException("Unknown Java class library: " + this.javaClassName);
        }
        logger.info("Creating core communicator between Java class '" + javaClassName + "' and Contiki library '" + getContikiFirmwareFile().getName() + "'");
        myCoreComm = CoreComm.createCoreComm(this.javaClassName, getContikiFirmwareFile());
        boolean useCommand = Boolean.parseBoolean(GUI.getExternalToolsSetting("PARSE_WITH_COMMAND", "false"));
        int dataSectionAddr = -1, dataSectionSize = -1;
        int bssSectionAddr = -1, bssSectionSize = -1;
        int commonSectionAddr = -1, commonSectionSize = -1;
        if (useCommand) {
            String[] output = loadCommandData(getContikiFirmwareFile());
            if (output == null) {
                throw new MoteTypeCreationException("No parse command output loaded");
            }
            boolean parseOK = parseCommandData(output, varAddresses);
            if (!parseOK) {
                logger.fatal("Command output parsing failed");
                throw new MoteTypeCreationException("Command output parsing failed");
            }
            dataSectionAddr = parseCommandDataSectionAddr(output);
            dataSectionSize = parseCommandDataSectionSize(output);
            bssSectionAddr = parseCommandBssSectionAddr(output);
            bssSectionSize = parseCommandBssSectionSize(output);
            commonSectionAddr = parseCommandCommonSectionAddr(output);
            commonSectionSize = parseCommandCommonSectionSize(output);
        } else {
            if (mapFile == null || !mapFile.exists()) {
                throw new MoteTypeCreationException("Map file " + mapFile + " could not be found!");
            }
            String[] mapData = loadMapFile(mapFile);
            if (mapData == null) {
                logger.fatal("No map data could be loaded");
                throw new MoteTypeCreationException("No map data could be loaded: " + mapFile);
            }
            boolean parseOK = parseMapFileData(mapData, varAddresses);
            if (!parseOK) {
                logger.fatal("Map data parsing failed");
                throw new MoteTypeCreationException("Map data parsing failed: " + mapFile);
            }
            dataSectionAddr = parseMapDataSectionAddr(mapData);
            dataSectionSize = parseMapDataSectionSize(mapData);
            bssSectionAddr = parseMapBssSectionAddr(mapData);
            bssSectionSize = parseMapBssSectionSize(mapData);
            commonSectionAddr = parseMapCommonSectionAddr(mapData);
            commonSectionSize = parseMapCommonSectionSize(mapData);
        }
        if (varAddresses.size() == 0) {
            throw new MoteTypeCreationException("Library variables parsing failed");
        }
        try {
            relAddressOfReferenceVariable = (Integer) varAddresses.get("referenceVar");
        } catch (Exception e) {
            throw (MoteTypeCreationException) new MoteTypeCreationException("JNI call error: " + e.getMessage()).initCause(e);
        }
        if (dataSectionAddr <= 0 || dataSectionSize <= 0 || bssSectionAddr <= 0 || bssSectionSize <= 0) {
            throw new MoteTypeCreationException("Library section addresses parsing failed");
        }
        myCoreComm.setReferenceAddress(relAddressOfReferenceVariable);
        initialMemory = new SectionMoteMemory(varAddresses);
        byte[] initialDataSection = new byte[dataSectionSize];
        getCoreMemory(dataSectionAddr, dataSectionSize, initialDataSection);
        initialMemory.setMemorySegment(dataSectionAddr, initialDataSection);
        logger.info(getContikiFirmwareFile().getName() + ": data section at 0x" + Integer.toHexString(dataSectionAddr) + " (0x" + dataSectionSize + " bytes)");
        byte[] initialBssSection = new byte[bssSectionSize];
        getCoreMemory(bssSectionAddr, bssSectionSize, initialBssSection);
        initialMemory.setMemorySegment(bssSectionAddr, initialBssSection);
        logger.info(getContikiFirmwareFile().getName() + ": BSS section at 0x" + Integer.toHexString(bssSectionAddr) + " (0x" + bssSectionSize + " bytes)");
        if (commonSectionAddr > 0 && commonSectionSize > 0) {
            byte[] initialCommonSection = new byte[commonSectionSize];
            getCoreMemory(commonSectionAddr, commonSectionSize, initialCommonSection);
            initialMemory.setMemorySegment(commonSectionAddr, initialCommonSection);
            logger.info(getContikiFirmwareFile().getName() + ": common section at 0x" + Integer.toHexString(commonSectionAddr) + " (0x" + commonSectionSize + " bytes)");
        }
    }

    /**
   * Ticks the currently loaded mote. This should not be used directly, but
   * rather via {@link ContikiMote#execute(long)}.
   */
    public void tick() {
        myCoreComm.tick();
    }

    /**
   * Creates and returns a copy of this mote type's initial memory (just after
   * the init function has been run). When a new mote is created it should get
   * it's memory from here.
   *
   * @return Initial memory of a mote type
   */
    public SectionMoteMemory createInitialMemory() {
        return initialMemory.clone();
    }

    /**
   * Copy given memory to the Contiki system. This should not be used directly,
   * but instead via ContikiMote.setMemory().
   *
   * @param mem
   *          New memory
   */
    public void setCoreMemory(SectionMoteMemory mem) {
        for (int i = 0; i < mem.getNumberOfSections(); i++) {
            setCoreMemory(mem.getStartAddrOfSection(i), mem.getSizeOfSection(i), mem.getDataOfSection(i));
        }
    }

    /**
   * Parses specified map file data for variable name to addresses mappings. The
   * mappings are added to the given properties object.
   *
   * @param mapFileData
   *          Contents of entire map file
   * @param varAddresses
   *          Properties that should contain the name to addresses mappings.
   */
    public static boolean parseMapFileData(String[] mapFileData, Properties varAddresses) {
        String[] varNames = getMapFileVarNames(mapFileData);
        if (varNames == null || varNames.length == 0) {
            return false;
        }
        for (String varName : varNames) {
            int varAddress = getMapFileVarAddress(mapFileData, varName, varAddresses);
            if (varAddress > 0) {
                varAddresses.put(varName, new Integer(varAddress));
            } else {
                logger.warn("Parsed Contiki variable '" + varName + "' but could not find address");
            }
        }
        return true;
    }

    /**
   * Parses parse command output for variable name to addresses mappings.
   * The mappings are written to the given properties object.
   *
   * @param output Command output
   * @param addresses Variable addresses mappings
   */
    public static boolean parseCommandData(String[] output, Properties addresses) {
        int nrNew = 0, nrOld = 0, nrMismatch = 0;
        Pattern pattern = Pattern.compile(GUI.getExternalToolsSetting("COMMAND_VAR_NAME_ADDRESS"));
        for (String line : output) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String symbol = matcher.group(2);
                int address = Integer.parseInt(matcher.group(1), 16);
                if (!addresses.containsKey(symbol)) {
                    nrNew++;
                    addresses.put(symbol, new Integer(address));
                } else {
                    int oldAddress = (Integer) addresses.get(symbol);
                    if (oldAddress != address) {
                        nrMismatch++;
                    }
                    nrOld++;
                }
            }
        }
        return (nrNew + nrOld) > 0;
    }

    /**
   * Copy core memory to given memory. This should not be used directly, but
   * instead via ContikiMote.getMemory().
   *
   * @param mem
   *          Memory to set
   */
    public void getCoreMemory(SectionMoteMemory mem) {
        for (int i = 0; i < mem.getNumberOfSections(); i++) {
            int startAddr = mem.getStartAddrOfSection(i);
            int size = mem.getSizeOfSection(i);
            byte[] data = mem.getDataOfSection(i);
            getCoreMemory(startAddr, size, data);
        }
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public File getContikiSourceFile() {
        return fileSource;
    }

    public void setContikiSourceFile(File file) {
        fileSource = file;
    }

    public File getContikiFirmwareFile() {
        return fileFirmware;
    }

    public void setContikiFirmwareFile(File file) {
        fileFirmware = file;
    }

    public String getCompileCommands() {
        return compileCommands;
    }

    public void setCompileCommands(String commands) {
        this.compileCommands = commands;
    }

    /**
   * @param symbols Core library has system symbols information
   */
    public void setHasSystemSymbols(boolean symbols) {
        hasSystemSymbols = symbols;
    }

    /**
   * @return Whether core library has system symbols information
   */
    public boolean hasSystemSymbols() {
        return hasSystemSymbols;
    }

    /**
   * @param commStack Communication stack
   */
    public void setCommunicationStack(CommunicationStack commStack) {
        this.commStack = commStack;
    }

    /**
   * @return Contiki communication stack
   */
    public CommunicationStack getCommunicationStack() {
        return commStack;
    }

    /**
   * Get relative address of variable with given name.
   *
   * @param varName Name of variable
   * @return Relative memory address of variable or -1 if not found
   */
    public static int getMapFileVarAddress(String[] mapFileData, String varName, Properties varAddresses) {
        int varAddr;
        String varAddrString;
        if ((varAddrString = varAddresses.getProperty(varName)) != null) {
            varAddr = Integer.parseInt(varAddrString);
            return varAddr;
        }
        String regExp = GUI.getExternalToolsSetting("MAPFILE_VAR_ADDRESS_1") + varName + GUI.getExternalToolsSetting("MAPFILE_VAR_ADDRESS_2");
        String retString = getFirstMatchGroup(mapFileData, regExp, 1);
        if (retString != null) {
            varAddresses.setProperty(varName, Integer.toString(Integer.parseInt(retString.trim(), 16)));
            return Integer.parseInt(retString.trim(), 16);
        } else {
            return -1;
        }
    }

    private void getCoreMemory(int relAddr, int length, byte[] data) {
        myCoreComm.getMemory(relAddr, length, data);
    }

    private void setCoreMemory(int relAddr, int length, byte[] mem) {
        myCoreComm.setMemory(relAddr, length, mem);
    }

    private static String getFirstMatchGroup(String[] lines, String regexp, int groupNr) {
        Pattern pattern = Pattern.compile(regexp);
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(groupNr);
            }
        }
        return null;
    }

    /**
   * Returns all variable names in both data and BSS section by parsing the map
   * file. These values should not be trusted completely as the parsing may
   * fail.
   *
   * @return Variable names found in the data and bss section
   */
    public static String[] getMapFileVarNames(String[] mapFileData) {
        ArrayList<String> varNames = new ArrayList<String>();
        String[] dataVariables = getAllVariableNames(mapFileData, parseMapDataSectionAddr(mapFileData), parseMapDataSectionAddr(mapFileData) + parseMapDataSectionSize(mapFileData));
        for (String v : dataVariables) {
            varNames.add(v);
        }
        String[] bssVariables = getAllVariableNames(mapFileData, parseMapBssSectionAddr(mapFileData), parseMapBssSectionAddr(mapFileData) + parseMapBssSectionSize(mapFileData));
        for (String v : bssVariables) {
            varNames.add(v);
        }
        return varNames.toArray(new String[0]);
    }

    public static String[] getAllVariableNames(String[] lines, int startAddress, int endAddress) {
        ArrayList<String> varNames = new ArrayList<String>();
        Pattern pattern = Pattern.compile(GUI.getExternalToolsSetting("MAPFILE_VAR_NAME"));
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                if (Integer.decode(matcher.group(1)).intValue() >= startAddress && Integer.decode(matcher.group(1)).intValue() <= endAddress) {
                    varNames.add(matcher.group(2));
                }
            }
        }
        return varNames.toArray(new String[0]);
    }

    protected int getVariableSize(Vector<String> lines, String varName) {
        Pattern pattern = Pattern.compile(GUI.getExternalToolsSetting("MAPFILE_VAR_SIZE_1") + varName + GUI.getExternalToolsSetting("MAPFILE_VAR_SIZE_2"));
        for (int i = 0; i < lines.size(); i++) {
            Matcher matcher = pattern.matcher(lines.get(i));
            if (matcher.find()) {
                return Integer.decode(matcher.group(1));
            }
        }
        return -1;
    }

    private static int parseFirstHexInt(String regexp, String[] data) {
        String retString = getFirstMatchGroup(data, regexp, 1);
        if (retString != null) {
            return Integer.parseInt(retString.trim(), 16);
        } else {
            return -1;
        }
    }

    public static int parseMapDataSectionAddr(String[] mapFileData) {
        String regexp = GUI.getExternalToolsSetting("MAPFILE_DATA_START", "");
        if (regexp.equals("")) {
            return -1;
        }
        return parseFirstHexInt(regexp, mapFileData);
    }

    public static int parseMapDataSectionSize(String[] mapFileData) {
        String regexp = GUI.getExternalToolsSetting("MAPFILE_DATA_SIZE", "");
        if (regexp.equals("")) {
            return -1;
        }
        return parseFirstHexInt(regexp, mapFileData);
    }

    public static int parseMapBssSectionAddr(String[] mapFileData) {
        String regexp = GUI.getExternalToolsSetting("MAPFILE_BSS_START", "");
        if (regexp.equals("")) {
            return -1;
        }
        return parseFirstHexInt(regexp, mapFileData);
    }

    public static int parseMapBssSectionSize(String[] mapFileData) {
        String regexp = GUI.getExternalToolsSetting("MAPFILE_BSS_SIZE", "");
        if (regexp.equals("")) {
            return -1;
        }
        return parseFirstHexInt(regexp, mapFileData);
    }

    public static int parseMapCommonSectionAddr(String[] mapFileData) {
        String regexp = GUI.getExternalToolsSetting("MAPFILE_COMMON_START", "");
        if (regexp.equals("")) {
            return -1;
        }
        return parseFirstHexInt(regexp, mapFileData);
    }

    public static int parseMapCommonSectionSize(String[] mapFileData) {
        String regexp = GUI.getExternalToolsSetting("MAPFILE_COMMON_SIZE", "");
        if (regexp.equals("")) {
            return -1;
        }
        return parseFirstHexInt(regexp, mapFileData);
    }

    public static int parseCommandDataSectionAddr(String[] output) {
        String regexp = GUI.getExternalToolsSetting("COMMAND_DATA_START", "");
        if (regexp.equals("")) {
            return -1;
        }
        return parseFirstHexInt(regexp, output);
    }

    public static int parseCommandDataSectionSize(String[] output) {
        String regexp = GUI.getExternalToolsSetting("COMMAND_DATA_END", "");
        if (regexp.equals("")) {
            return -1;
        }
        int start = parseCommandDataSectionAddr(output);
        if (start < 0) {
            return -1;
        }
        int end = parseFirstHexInt(regexp, output);
        if (end < 0) {
            return -1;
        }
        return end - start;
    }

    public static int parseCommandBssSectionAddr(String[] output) {
        String regexp = GUI.getExternalToolsSetting("COMMAND_BSS_START", "");
        if (regexp.equals("")) {
            return -1;
        }
        return parseFirstHexInt(regexp, output);
    }

    public static int parseCommandBssSectionSize(String[] output) {
        String regexp = GUI.getExternalToolsSetting("COMMAND_BSS_END", "");
        if (regexp.equals("")) {
            return -1;
        }
        int start = parseCommandBssSectionAddr(output);
        if (start < 0) {
            return -1;
        }
        int end = parseFirstHexInt(regexp, output);
        if (end < 0) {
            return -1;
        }
        return end - start;
    }

    public static int parseCommandCommonSectionAddr(String[] output) {
        String regexp = GUI.getExternalToolsSetting("COMMAND_COMMON_START", "");
        if (regexp.equals("")) {
            return -1;
        }
        return parseFirstHexInt(regexp, output);
    }

    public static int parseCommandCommonSectionSize(String[] output) {
        String regexp = GUI.getExternalToolsSetting("COMMAND_COMMON_END", "");
        if (regexp.equals("")) {
            return -1;
        }
        int start = parseCommandCommonSectionAddr(output);
        if (start < 0) {
            return -1;
        }
        int end = parseFirstHexInt(regexp, output);
        if (end < 0) {
            return -1;
        }
        return end - start;
    }

    private static int getRelVarAddr(String mapFileData[], String varName) {
        String regExp = GUI.getExternalToolsSetting("MAPFILE_VAR_ADDRESS_1") + varName + GUI.getExternalToolsSetting("MAPFILE_VAR_ADDRESS_2");
        String retString = getFirstMatchGroup(mapFileData, regExp, 1);
        if (retString != null) {
            return Integer.parseInt(retString.trim(), 16);
        } else {
            return -1;
        }
    }

    public static String[] loadMapFile(File mapFile) {
        String contents = StringUtils.loadFromFile(mapFile);
        if (contents == null) {
            return null;
        }
        return contents.split("\n");
    }

    /**
   * Executes configured command on given file and returns the result.
   *
   * @param libraryFile Contiki library
   * @return Execution response, or null at failure
   */
    public static String[] loadCommandData(File libraryFile) {
        ArrayList<String> output = new ArrayList<String>();
        try {
            String command = GUI.getExternalToolsSetting("PARSE_COMMAND");
            if (command == null) {
                return null;
            }
            command = command.replace("$(LIBFILE)", libraryFile.getName().replace(File.separatorChar, '/'));
            String line;
            Process p = Runtime.getRuntime().exec(command.split(" "), null, libraryFile.getParentFile());
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.getErrorStream().close();
            while ((line = input.readLine()) != null) {
                output.add(line);
            }
            input.close();
            if (output == null || output.size() == 0) {
                return null;
            }
            return output.toArray(new String[0]);
        } catch (Exception err) {
            logger.fatal("Command error: " + err.getMessage(), err);
            return null;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String newDescription) {
        description = newDescription;
    }

    public ProjectConfig getConfig() {
        return myConfig;
    }

    /**
   * Sets mote type project configuration. This may differ from the general
   * simulator project configuration.
   *
   * @param moteTypeConfig
   *          Project configuration
   */
    public void setConfig(ProjectConfig moteTypeConfig) {
        myConfig = moteTypeConfig;
    }

    /**
   * Returns all sensors of this mote type
   *
   * @return All sensors
   */
    public String[] getSensors() {
        return sensors;
    }

    /**
   * Returns all core interfaces of this mote type
   *
   * @return All core interfaces
   */
    public String[] getCoreInterfaces() {
        return coreInterfaces;
    }

    /**
   * Set core interfaces
   *
   * @param coreInterfaces
   *          New core interfaces
   */
    public void setCoreInterfaces(String[] coreInterfaces) {
        this.coreInterfaces = coreInterfaces;
    }

    public Class<? extends MoteInterface>[] getMoteInterfaceClasses() {
        if (moteInterfacesClasses == null) {
            return null;
        }
        Class<? extends MoteInterface>[] arr = new Class[moteInterfacesClasses.size()];
        moteInterfacesClasses.toArray(arr);
        return arr;
    }

    public void setMoteInterfaceClasses(Class<? extends MoteInterface>[] moteInterfaces) {
        this.moteInterfacesClasses = new ArrayList<Class<? extends MoteInterface>>();
        for (Class<? extends MoteInterface> intf : moteInterfaces) {
            this.moteInterfacesClasses.add(intf);
        }
    }

    /**
   * Create a checksum of file. Used for checking if needed files are unchanged
   * when loading a saved simulation.
   *
   * @param file
   *          File containg data to checksum
   * @return Checksum
   */
    protected byte[] createChecksum(File file) {
        int bytesRead = 1;
        byte[] readBytes = new byte[128];
        MessageDigest messageDigest;
        try {
            InputStream fileInputStream = new FileInputStream(file);
            messageDigest = MessageDigest.getInstance("MD5");
            while (bytesRead > 0) {
                bytesRead = fileInputStream.read(readBytes);
                if (bytesRead > 0) {
                    messageDigest.update(readBytes, 0, bytesRead);
                }
            }
            fileInputStream.close();
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        return messageDigest.digest();
    }

    /**
   * Generates a unique Contiki mote type ID.
   *
   * @param existingTypes Already existing mote types, may be null
   * @param reservedIdentifiers Already reserved identifiers, may be null
   * @return Unique mote type ID.
   */
    public static String generateUniqueMoteTypeID(MoteType[] existingTypes, Collection reservedIdentifiers) {
        String testID = "";
        boolean okID = false;
        while (!okID) {
            testID = ID_PREFIX + (new Random().nextInt(1000));
            okID = true;
            if (reservedIdentifiers != null && reservedIdentifiers.contains(testID)) {
                okID = false;
            }
            if (!okID) {
                continue;
            }
            if (existingTypes != null) {
                for (MoteType existingMoteType : existingTypes) {
                    if (existingMoteType.getIdentifier().equals(testID)) {
                        okID = false;
                        break;
                    }
                }
            }
            if (!okID) {
                continue;
            }
            File libraryFile = new File(ContikiMoteType.tempOutputDirectory, testID + ContikiMoteType.librarySuffix);
            if (libraryFile.exists() || CoreComm.hasLibraryFileBeenLoaded(libraryFile)) {
                okID = false;
            }
        }
        return testID;
    }

    /**
   * Returns a panel with interesting data for this mote type.
   *
   * @return Mote type visualizer
   */
    public JPanel getTypeVisualizer() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel();
        JPanel smallPane;
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        smallPane = new JPanel(new BorderLayout());
        label = new JLabel("Identifier");
        smallPane.add(BorderLayout.WEST, label);
        label = new JLabel(identifier);
        smallPane.add(BorderLayout.EAST, label);
        panel.add(smallPane);
        smallPane = new JPanel(new BorderLayout());
        label = new JLabel("Description");
        smallPane.add(BorderLayout.WEST, label);
        label = new JLabel(description);
        smallPane.add(BorderLayout.EAST, label);
        panel.add(smallPane);
        smallPane = new JPanel(new BorderLayout());
        label = new JLabel("Contiki application");
        smallPane.add(BorderLayout.WEST, label);
        label = new JLabel(getContikiSourceFile().getName());
        label.setToolTipText(getContikiSourceFile().getAbsolutePath());
        smallPane.add(BorderLayout.EAST, label);
        panel.add(smallPane);
        panel.add(Box.createVerticalStrut(15));
        smallPane = new JPanel(new BorderLayout());
        label = new JLabel("Contiki firmware");
        smallPane.add(BorderLayout.WEST, label);
        label = new JLabel(getContikiFirmwareFile().getName());
        label.setToolTipText(getContikiFirmwareFile().getAbsolutePath());
        smallPane.add(BorderLayout.EAST, label);
        panel.add(smallPane);
        smallPane = new JPanel(new BorderLayout());
        label = new JLabel("Java class (JNI)");
        smallPane.add(BorderLayout.WEST, label);
        label = new JLabel(this.javaClassName);
        smallPane.add(BorderLayout.EAST, label);
        panel.add(smallPane);
        smallPane = new JPanel(new BorderLayout());
        label = new JLabel("Sensors");
        smallPane.add(BorderLayout.WEST, label);
        panel.add(smallPane);
        for (String sensor : sensors) {
            smallPane = new JPanel(new BorderLayout());
            label = new JLabel(sensor);
            smallPane.add(BorderLayout.EAST, label);
            panel.add(smallPane);
        }
        smallPane = new JPanel(new BorderLayout());
        label = new JLabel("Mote interfaces");
        smallPane.add(BorderLayout.WEST, label);
        panel.add(smallPane);
        for (Class intf : moteInterfacesClasses) {
            smallPane = new JPanel(new BorderLayout());
            label = new JLabel(intf.getSimpleName());
            smallPane.add(BorderLayout.EAST, label);
            panel.add(smallPane);
        }
        smallPane = new JPanel(new BorderLayout());
        label = new JLabel("Core interfaces");
        smallPane.add(BorderLayout.WEST, label);
        panel.add(smallPane);
        for (String intf : getCoreInterfaces()) {
            smallPane = new JPanel(new BorderLayout());
            label = new JLabel(intf);
            smallPane.add(BorderLayout.EAST, label);
            panel.add(smallPane);
        }
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        return panel;
    }

    public Collection<Element> getConfigXML() {
        Vector<Element> config = new Vector<Element>();
        Element element;
        element = new Element("identifier");
        element.setText(getIdentifier());
        config.add(element);
        element = new Element("description");
        element.setText(getDescription());
        config.add(element);
        element = new Element("contikiapp");
        File file = simulation.getGUI().createPortablePath(getContikiSourceFile());
        element.setText(file.getPath().replaceAll("\\\\", "/"));
        config.add(element);
        element = new Element("commands");
        element.setText(compileCommands);
        config.add(element);
        for (Class<? extends MoteInterface> moteInterface : getMoteInterfaceClasses()) {
            element = new Element("moteinterface");
            element.setText(moteInterface.getName());
            config.add(element);
        }
        element = new Element("symbols");
        element.setText(new Boolean(hasSystemSymbols()).toString());
        config.add(element);
        element = new Element("commstack");
        element.setText(getCommunicationStack().toString());
        config.add(element);
        return config;
    }

    public boolean setConfigXML(Simulation simulation, Collection<Element> configXML, boolean visAvailable) throws MoteTypeCreationException {
        boolean warnedOldVersion = false;
        File oldVersionSource = null;
        moteInterfacesClasses = new ArrayList<Class<? extends MoteInterface>>();
        this.simulation = simulation;
        for (Element element : configXML) {
            String name = element.getName();
            if (name.equals("identifier")) {
                identifier = element.getText();
            } else if (name.equals("description")) {
                description = element.getText();
            } else if (name.equals("contikiapp")) {
                File file = new File(element.getText());
                if (!file.exists()) {
                    file = simulation.getGUI().restorePortablePath(file);
                }
                setContikiSourceFile(file);
                File contikiFirmware = new File(getContikiSourceFile().getParentFile(), "obj_cooja/" + getIdentifier() + librarySuffix);
                setContikiFirmwareFile(contikiFirmware);
            } else if (name.equals("commands")) {
                compileCommands = element.getText();
            } else if (name.equals("symbols")) {
                hasSystemSymbols = Boolean.parseBoolean(element.getText());
            } else if (name.equals("commstack")) {
                commStack = CommunicationStack.parse(element.getText());
            } else if (name.equals("moteinterface")) {
                if (element.getText().trim().equals("se.sics.cooja.contikimote.interfaces.ContikiLog")) {
                } else {
                    Class<? extends MoteInterface> moteInterfaceClass = simulation.getGUI().tryLoadClass(this, MoteInterface.class, element.getText().trim());
                    if (moteInterfaceClass == null) {
                        logger.warn("Can't find mote interface class: " + element.getText());
                    } else {
                        moteInterfacesClasses.add(moteInterfaceClass);
                    }
                }
            } else if (name.equals("contikibasedir") || name.equals("contikicoredir") || name.equals("projectdir") || name.equals("compilefile") || name.equals("process") || name.equals("sensor") || name.equals("coreinterface")) {
                if (!warnedOldVersion) {
                    warnedOldVersion = true;
                    logger.warn("Old simulation config detected: contiki mote types may not load correctly");
                }
                if (name.equals("compilefile")) {
                    if (element.getText().endsWith(".c")) {
                        File potentialFile = new File(element.getText());
                        if (potentialFile.exists()) {
                            oldVersionSource = potentialFile;
                        }
                    }
                }
            } else {
                logger.fatal("Unrecognized entry in loaded configuration: " + name);
            }
        }
        Class<? extends MoteInterface>[] arr = new Class[moteInterfacesClasses.size()];
        moteInterfacesClasses.toArray(arr);
        setCoreInterfaces(ContikiMoteType.getRequiredCoreInterfaces(arr));
        if (getContikiSourceFile() == null && warnedOldVersion && oldVersionSource != null) {
            setContikiSourceFile(oldVersionSource);
            logger.info("Guessing Contiki source: " + oldVersionSource.getAbsolutePath());
            setContikiFirmwareFile(getExpectedFirmwareFile(oldVersionSource));
            logger.info("Guessing Contiki firmware: " + getContikiFirmwareFile().getAbsolutePath());
            String compileCommands = "make " + getExpectedFirmwareFile(oldVersionSource).getName() + " TARGET=cooja";
            logger.info("Guessing compile commands: " + compileCommands);
            setCompileCommands(compileCommands);
        }
        boolean createdOK = configureAndInit(GUI.getTopParentContainer(), simulation, visAvailable);
        return createdOK;
    }

    public static String[] getRequiredCoreInterfaces(Class<? extends MoteInterface>[] moteInterfaces) {
        ArrayList<String> coreInterfacesList = new ArrayList<String>();
        for (Class<? extends MoteInterface> intf : moteInterfaces) {
            if (!ContikiMoteInterface.class.isAssignableFrom(intf)) {
                continue;
            }
            String[] deps;
            try {
                Method m = intf.getDeclaredMethod("getCoreInterfaceDependencies", (Class[]) null);
                deps = (String[]) m.invoke(null, (Object[]) null);
            } catch (Exception e) {
                logger.warn("Could not extract Contiki dependencies of mote interface: " + intf.getName());
                e.printStackTrace();
                continue;
            }
            if (deps == null || deps.length == 0) {
                continue;
            }
            for (String dep : deps) {
                coreInterfacesList.add(dep);
            }
        }
        String[] coreInterfaces = new String[coreInterfacesList.size()];
        coreInterfacesList.toArray(coreInterfaces);
        return coreInterfaces;
    }
}
