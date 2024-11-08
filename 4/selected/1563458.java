package de.jlab.lab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import de.jlab.GlobalsLocator;
import de.jlab.boards.ADAIOBoard;
import de.jlab.boards.Board;
import de.jlab.boards.DCGBoard;
import de.jlab.boards.DDSBoard;
import de.jlab.boards.DIVBoard;
import de.jlab.boards.EDLBoard;
import de.jlab.boards.FPGABoard;
import de.jlab.boards.UNICBoard;
import de.jlab.communication.BoardCommunication;
import de.jlab.communication.BoardReceiver;
import de.jlab.config.ConfigHandler;
import de.jlab.config.ConfigHelper;
import de.jlab.config.ConnectionChannelConfig;
import de.jlab.config.ConnectionConfig;
import de.jlab.config.JLabConfig;
import de.jlab.config.external.ExternalModelConfig;
import de.jlab.config.external.ExternalModuleConfig;
import de.jlab.config.runs.Run;
import de.jlab.config.runs.RunDefinition;
import de.jlab.external.measurement.model.ExternalModel;
import de.jlab.lab.runs.RunConfigReader;
import de.jlab.lab.runs.RunDefinitionAnalyzed;
import de.jlab.lab.runs.RunExecutorCallback;
import de.jlab.lab.runs.RunsExecutor;
import de.jlab.parameterbackup.config.BackupConfig;
import de.jlab.ui.connectionsetting.ConnectionSelectionDialog;
import de.jlab.ui.tools.Ordering;
import de.jlab.ui.tools.WeakObservable;
import de.jlab.util.CommandUtils;

public class Lab extends WeakObservable implements BoardReceiver {

    Map<String, BoardCommunication> commChannelById = new HashMap<String, BoardCommunication>();

    Timer valueWatchTimer = new Timer("Watch Timer");

    JLabConfig config = null;

    protected Map<Integer, Double> valuesOfSubchannels = new HashMap<Integer, Double>();

    protected Map<Board, String> presentModulesForBoard = new HashMap<Board, String>();

    static Logger stdlog = Logger.getLogger(Lab.class.getName());

    ConfigHandler configHandler = null;

    protected Map<String, ExternalModel> externalModels = new HashMap<String, ExternalModel>();

    private Map<String, Map<Integer, Board>> boardsInLabPerChannel = new HashMap<String, Map<Integer, Board>>();

    private List<Board> allBoardsFound = new ArrayList<Board>();

    private List<Board> possibleBoardList = new ArrayList<Board>();

    private HashMap<String, RunDefinitionAnalyzed> runSetsPerRunDefinition = new HashMap<String, RunDefinitionAnalyzed>();

    public Map<String, Map<Integer, Board>> getBoardsInLabPerChannel() {
        return boardsInLabPerChannel;
    }

    public List<Board> getAllBoardsFound() {
        return allBoardsFound;
    }

    public Map<String, BoardCommunication> getCommChannelByNameMap() {
        return commChannelById;
    }

    public BoardCommunication getAnyCommChannel() {
        return (BoardCommunication) commChannelById.values().toArray()[0];
    }

    public BoardCommunication getCommChannelByName(String channelName) {
        return commChannelById.get(channelName);
    }

    public List<Board> getPossibleBoardList() {
        return possibleBoardList;
    }

    public Lab(ConfigHandler configHandler) {
        this.configHandler = configHandler;
    }

    public void initLab(ConnectionConfig defaultConfig) {
        loadConfig(defaultConfig);
        prepareBoardCommunication();
        for (BoardCommunication currCommChannel : commChannelById.values()) {
            try {
                currCommChannel.connect();
            } catch (Exception e) {
                stdlog.log(Level.SEVERE, "Error in initializing Lab", e);
            }
            currCommChannel.setReceiver(this);
            currCommChannel.startReceiverThread();
        }
        analysePresentModules();
        stdlog.info("c't Lab Modules checked");
        initExternalModules();
        stdlog.info("External Lab Modules checked");
        analyseFeatures();
        analyzeRuns();
        int a = 0;
    }

    public void startLab() {
    }

    private void analyzeRuns() {
        if (config.getRunDefinitions() != null) {
            for (RunDefinition currDefinition : config.getRunDefinitions()) {
                runSetsPerRunDefinition.put(currDefinition.getName(), RunConfigReader.analyzeDefinition(currDefinition));
            }
        }
    }

    private void prepareBoardCommunication() {
        List<ConnectionChannelConfig> commChannels = config.getConnParameter().getCommChannels();
        for (ConnectionChannelConfig currChannel : commChannels) {
            try {
                Class<BoardCommunication> connectionClass = (Class<BoardCommunication>) Class.forName(currChannel.getClassname());
                BoardCommunication commChannel = connectionClass.newInstance();
                commChannel.initByParameters(currChannel.getParametersAsHashMap());
                commChannel.setChannelName(currChannel.getChannelname());
                this.commChannelById.put(currChannel.getChannelname(), commChannel);
            } catch (Throwable e) {
                stdlog.log(Level.SEVERE, "Error in preparing board communication with JLab ID " + currChannel.getChannelname() + " Class " + currChannel.getClassname(), e);
                int option = JOptionPane.showConfirmDialog(GlobalsLocator.getMainFrame(), GlobalsLocator.translate("connection-error-warning"), GlobalsLocator.translate("connection-error-header"), JOptionPane.YES_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    resetConnectionConfig();
                    System.exit(0);
                }
            }
        }
    }

    private void setBoard(String channelName, int address, Board board) {
        Map<Integer, Board> boardsInChannel = boardsInLabPerChannel.get(channelName);
        if (boardsInChannel == null) {
            boardsInChannel = new HashMap<Integer, Board>();
            boardsInLabPerChannel.put(channelName, boardsInChannel);
        }
        boardsInChannel.put(address, board);
    }

    private void analysePresentModules() {
        List<Board> possibleBoards = createBoardList();
        for (String currCommChannelName : commChannelById.keySet()) {
            System.err.println("\nCheck Channel " + currCommChannelName);
            BoardCommunication currCommChannel = commChannelById.get(currCommChannelName);
            for (int addressCount = 0; addressCount < 9; ++addressCount) {
                String status = this.queryStringValue(currCommChannel, addressCount, 254);
                if (status == null) {
                    System.err.println("No Board at address" + addressCount);
                    continue;
                }
                status = status.replace('\n', ' ');
                status = status.replace('\r', ' ');
                System.err.println("Status " + status);
                for (Board currBoard : possibleBoards) {
                    String boardName = currBoard.isBoardPresent(status);
                    if (boardName != null) {
                        System.err.println(" => Found Board " + boardName);
                        try {
                            presentModulesForBoard.put(currBoard, boardName);
                            Board boardInstance = (Board) currBoard.getClass().newInstance();
                            boardInstance.init(addressCount, status, this, currCommChannel);
                            setBoard(currCommChannelName, addressCount, boardInstance);
                            allBoardsFound.add(boardInstance);
                        } catch (Exception e) {
                            stdlog.log(Level.SEVERE, "Unable to instanciate already loaded Board " + currBoard.getClass().getName(), e);
                        }
                        break;
                    }
                }
            }
        }
    }

    public void stopLab() {
        stdlog.finest("Stop The Lab");
        storeConfig();
        valueWatchTimer.cancel();
        for (BoardCommunication currCommChannel : commChannelById.values()) {
            currCommChannel.stopReceiver();
            currCommChannel.queryValueAsynchronously(presentModulesForBoard.keySet().iterator().next().getAddress(), 254);
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
        for (BoardCommunication currCommChannel : commChannelById.values()) {
            currCommChannel.disconnect();
        }
    }

    public void decodeLabReply(String commChannel, String labReply) {
        stdlog.finest("Received Lab Reply : " + labReply);
        int address = labReply.charAt(1) - 48;
        int subchannel = CommandUtils.getSubchannelFromReply(labReply);
        SubChannelUpdatedNotification newNotification = null;
        if (CommandUtils.isReplyText(labReply)) {
            newNotification = new SubChannelUpdatedNotification(commChannel, address, subchannel, 0, CommandUtils.getStringValueFromReply(labReply), SubChannelUpdatedNotification.SUBCHANNEL_VALUE_TYPE.TEXT);
        } else {
            double value = CommandUtils.getDoubleValueFromReply(labReply);
            valuesOfSubchannels.put(subchannel + address * 1000, value);
            newNotification = new SubChannelUpdatedNotification(commChannel, address, subchannel, value, null, SubChannelUpdatedNotification.SUBCHANNEL_VALUE_TYPE.NUMBER);
        }
        newNotification.setOriginalReply(labReply);
        this.setChanged();
        try {
            this.notifyObservers(newNotification);
        } catch (Throwable e) {
        }
    }

    public void sendCommand(BoardCommunication commChannel, int address, String command) {
        commChannel.sendCommand(address, command);
    }

    public void sendCommand(BoardCommunication commChannel, int address, int subchannel, double value) {
        commChannel.sendCommand(address, subchannel, value);
    }

    public void sendCommand(BoardCommunication commChannel, int address, int subchannel, String value) {
        commChannel.sendCommand(address, subchannel, value);
    }

    public void sendCommand(BoardCommunication commChannel, int address, int subchannel, int value) {
        commChannel.sendCommand(address, subchannel, value);
    }

    public void sendCommand(BoardCommunication commChannel, int address, int subchannel, long value) {
        commChannel.sendCommand(address, subchannel, value);
    }

    public void queryValueAsynchronously(BoardCommunication commChannel, int address, int subchannel) {
        commChannel.queryValueAsynchronously(address, subchannel);
    }

    public double queryValue(BoardCommunication commChannel, int address, int subchannel) {
        return commChannel.queryDoubleValue(address, subchannel);
    }

    public int queryIntegerValue(BoardCommunication commChannel, int address, int subchannel) {
        return commChannel.queryIntegerValue(address, subchannel);
    }

    public long queryLongValue(BoardCommunication commChannel, int address, int subchannel) {
        return commChannel.queryLongValue(address, subchannel);
    }

    public String queryStringValue(BoardCommunication commChannel, int address, int subchannel) {
        return commChannel.queryStringValue(address, subchannel);
    }

    public void disconnect() {
        valueWatchTimer.cancel();
        for (BoardCommunication currCommChannel : commChannelById.values()) {
            currCommChannel.disconnect();
        }
    }

    public Map<Board, String> getPresentModulesForBoard() {
        return presentModulesForBoard;
    }

    public void storeConfig() {
        configHandler.storeConfig(config);
    }

    private void loadConfig(ConnectionConfig defaultConfig) {
        this.config = configHandler.loadConfig();
        if (config == null) {
            config = new JLabConfig();
        }
        if (config.getConnParameter() == null) {
            ConnectionSelectionDialog dlg = new ConnectionSelectionDialog(null, GlobalsLocator.translate("connection-dialog-header"));
            if (defaultConfig != null) dlg.setConnectionConfig(defaultConfig);
            dlg.pack();
            Ordering.centerDlgInScreen(dlg);
            dlg.setVisible(true);
            if (!dlg.isOkPressed()) {
                JOptionPane.showMessageDialog(null, GlobalsLocator.translate("connection-no-channel-warning"));
                System.exit(0);
            }
            config.setConnParameter(dlg.getConnectionConfig());
            storeConfig();
        }
    }

    private void initExternalModules() {
        List<ExternalModuleConfig> externalModules = config.getExternalModules();
        if (externalModules != null) {
            for (ExternalModuleConfig currModule : externalModules) {
                ExternalModelConfig modelConfig = currModule.getModel();
                String modelClassname = modelConfig.getClassname();
                try {
                    Class modelClass = Class.forName(modelClassname);
                    ExternalModel model = (ExternalModel) modelClass.newInstance();
                    model.setIdentity(currModule.getIdentifier());
                    model.startModel(ConfigHelper.getMapFromParameterList(modelConfig.getParameters()));
                    this.externalModels.put(currModule.getIdentifier(), model);
                } catch (ClassNotFoundException e) {
                    stdlog.warning("Modelclass " + modelClassname + " for Module " + currModule.getIdentifier() + " not found in Classpath, omitting!");
                } catch (Throwable le) {
                    stdlog.log(Level.SEVERE, "Error initing Module " + currModule.getIdentifier(), le);
                }
            }
        }
    }

    private void analyseFeatures() {
        for (String currCommChannelName : commChannelById.keySet()) {
            BoardCommunication currCommChannel = commChannelById.get(currCommChannelName);
            if (boardsInLabPerChannel.get(currCommChannelName) == null) continue;
            for (Integer address : boardsInLabPerChannel.get(currCommChannelName).keySet()) {
                Board currBoard = boardsInLabPerChannel.get(currCommChannelName).get(address);
                String identifier = currBoard.getLabIdentifier();
                currBoard.setFeatures(identifier, this.getConfig().getCtModuleParams());
            }
        }
    }

    public JLabConfig getConfig() {
        return config;
    }

    public void setConfig(JLabConfig config) {
        this.config = config;
    }

    public void resetConnectionConfig() {
        this.config.setConnParameter(null);
        storeConfig();
    }

    public void setEnableCommandConfirmation(boolean enable) {
        for (BoardCommunication currCommChannel : commChannelById.values()) {
            currCommChannel.setCommandConfirmation(enable);
        }
    }

    public boolean isEnabledCommandConfirmation() {
        return commChannelById.values().iterator().next().isCommandConfirmation();
    }

    public void setEnableCommandProtocol(boolean enable) {
        for (BoardCommunication currCommChannel : commChannelById.values()) {
            currCommChannel.setCommandProtocol(enable);
        }
    }

    public boolean isEnabledCommandDebug() {
        return commChannelById.values().iterator().next().isCommandConfirmation();
    }

    public void setEnableChecksum(boolean enable) {
        for (BoardCommunication currCommChannel : commChannelById.values()) {
            currCommChannel.setSendCheckSum(enable);
        }
    }

    public boolean isEnabledChecksum() {
        return commChannelById.values().iterator().next().isSendCheckSum();
    }

    public Map<String, ExternalModel> getExternalModels() {
        return externalModels;
    }

    public ExternalModel getExternalModelByIdentifier(String identifier) {
        return externalModels.get(identifier);
    }

    private List<Board> createBoardList() {
        possibleBoardList.clear();
        possibleBoardList.add(new ADAIOBoard());
        possibleBoardList.add(new DCGBoard());
        possibleBoardList.add(new EDLBoard());
        possibleBoardList.add(new DDSBoard());
        possibleBoardList.add(new DIVBoard());
        possibleBoardList.add(new FPGABoard());
        possibleBoardList.add(new UNICBoard());
        ArrayList<String> boardClasses = this.getConfig().getBoardDefinitions();
        if (boardClasses != null && !boardClasses.isEmpty()) {
            for (String currBoardClass : boardClasses) {
                try {
                    Board userBoard = (Board) Class.forName(currBoardClass).newInstance();
                    possibleBoardList.add(userBoard);
                } catch (Exception e) {
                    stdlog.log(Level.WARNING, "Could not load user board from class " + currBoardClass, e);
                    e.printStackTrace();
                }
            }
        }
        return possibleBoardList;
    }

    public Map<String, List<BackupConfig>> createBackupConfig() {
        HashMap<String, List<BackupConfig>> backupConfigs = new HashMap();
        for (Board currBoard : allBoardsFound) {
            if (currBoard.getBackupChannels() != null) backupConfigs.put(currBoard.getBoardIdentifier(), currBoard.getBackupChannels());
        }
        return backupConfigs;
    }

    public Board getBoardForCommChannelNameAndAddress(String channelName, int address) {
        Board foundBoard = null;
        for (Board currBoard : this.allBoardsFound) {
            if (currBoard.getCommChannel().getChannelName().equals(channelName) && currBoard.getAddress() == address) {
                foundBoard = currBoard;
                break;
            }
        }
        return foundBoard;
    }

    public HashMap<String, RunDefinitionAnalyzed> getRunSetsPerRunDefinition() {
        return runSetsPerRunDefinition;
    }

    public void executeRunConfig(RunExecutorCallback callback) {
        RunsExecutor executor = new RunsExecutor(this, callback);
        executor.start();
    }

    public void removeRunDefinition(String name) {
        runSetsPerRunDefinition.remove(name);
        for (Iterator<RunDefinition> runDefIter = getConfig().getRunDefinitions().iterator(); runDefIter.hasNext(); ) {
            RunDefinition runDef = runDefIter.next();
            if (runDef.getName().equals(name)) {
                runDefIter.remove();
            }
        }
        List<Run> runs = getConfig().getRunConfiguration().getRuns();
        for (Iterator<Run> runIter = runs.iterator(); runIter.hasNext(); ) {
            Run currRun = runIter.next();
            if (currRun.getName().equals(name)) {
                runIter.remove();
            }
        }
        this.setChanged();
        this.notifyObservers(NotificationDefinitions.RUNCONFIG_CHANGED);
    }

    public void addRunDefinition(RunDefinition runDef) {
        runSetsPerRunDefinition.put(runDef.getName(), RunConfigReader.analyzeDefinition(runDef));
        getConfig().getRunDefinitions().add(runDef);
        this.setChanged();
        this.notifyObservers(NotificationDefinitions.RUNCONFIG_CHANGED);
    }
}
