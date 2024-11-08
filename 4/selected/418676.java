package de.jlab.boards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.jlab.communication.BoardCommunication;
import de.jlab.config.CTModuleParameterConfig;
import de.jlab.lab.Lab;
import de.jlab.parameterbackup.config.BackupConfig;
import de.jlab.ui.modules.UILabModule;

/**
 * Board Class to provide a basic Class for the c't Lab Boards. All specific boards are derived from this one.
 * 
 * 
 */
public abstract class Board {

    protected Lab lab = null;

    protected int address = 0;

    protected String labIdentifier = null;

    BoardCommunication commChannel = null;

    public BoardCommunication getCommChannel() {
        return commChannel;
    }

    /**
    * Any UI Modules automatically included with the board?
    * @param moduleIdentifierString IDN? response
    * @return
    */
    public abstract List<UILabModule> getUILabModules(String moduleIdentifierString);

    /**
    * Check if this board is present.
    * @param statusMessage IDN? string
    * @return Board Identifier
    */
    public abstract String isBoardPresent(String statusMessage);

    /**
    * Get a List of Channels used for Snapshots AND Value Watch.
    * @return
    */
    public abstract List<BoardSubchannelInfo> getSnapshotChannels();

    /**
    * Get Channels for a parameterbackup
    * @return
    */
    public abstract List<BackupConfig> getBackupChannels();

    /**
    * Get the Boards Identifier
    * @return
    */
    public abstract String getBoardIdentifier();

    /**
    * Get a description for the board.
    * @return
    */
    public abstract String getBoardDescription();

    public void init(int address, String labIdentifier, Lab aLab, BoardCommunication commChannel) {
        this.labIdentifier = labIdentifier;
        this.address = address;
        this.commChannel = commChannel;
        lab = aLab;
    }

    public int getAddress() {
        return address;
    }

    public String getLabIdentifier() {
        return labIdentifier;
    }

    protected Map<String, BoardFeature> features = new HashMap<String, BoardFeature>();

    public static final int CHANNEL_STATUS = 255;

    public static final int CHANNEL_IDENTIFICATION = 254;

    public static final int CHANNEL_EEPROM_ENABLE = 250;

    public static final int CHANNEL_ERRORS = 251;

    public abstract void setFeatures(String boardIndentifier, List<CTModuleParameterConfig> ctModuleParams);

    public void addFeature(BoardFeature newFeature) {
        features.put(newFeature.getFeaturename(), newFeature);
    }

    public double getDoubleValueOfFeature(String featureName) {
        double result = 0;
        BoardFeature feature = features.get(featureName);
        result = feature.getValueAsDouble();
        return result;
    }

    public double getIntegerValueOfFeature(String featureName) {
        int result = 0;
        BoardFeature feature = features.get(featureName);
        result = feature.getValueAsInt();
        return result;
    }

    public boolean isFeaturePresent(String featureName) {
        return features.get(featureName) != null;
    }

    public String getBoardInfo() {
        return lab.queryStringValue(commChannel, address, CHANNEL_IDENTIFICATION);
    }

    public int getBoardErrors() {
        int errors = lab.queryIntegerValue(commChannel, address, CHANNEL_ERRORS);
        return errors;
    }

    public Status getBoardStatus() {
        BoardCommunication commChannel = lab.getCommChannelByNameMap().get(labIdentifier);
        int statusCode = lab.queryIntegerValue(commChannel, address, CHANNEL_STATUS);
        Status state = null;
        boolean busy = (statusCode & 128) == 1;
        boolean userSRQ = (statusCode & 64) == 1;
        boolean overLoad = (statusCode & 32) == 1;
        boolean bit4 = (statusCode & 16) == 1;
        int errorCodeOrButtons = (statusCode & 15);
        state = new Status(busy, userSRQ, overLoad, bit4, errorCodeOrButtons);
        return state;
    }

    public void enableEEPromWrite(boolean enable) {
        if (enable) lab.sendCommand(commChannel, address, CHANNEL_EEPROM_ENABLE, 1); else lab.sendCommand(commChannel, address, CHANNEL_EEPROM_ENABLE, 0);
    }

    public boolean isEEPromWriteEnabled() {
        int enabled = lab.queryIntegerValue(commChannel, address, CHANNEL_EEPROM_ENABLE);
        if (enabled == 1) return true; else return false;
    }

    public void sendCommand(String command) {
        lab.sendCommand(commChannel, address, command);
    }

    public void sendCommand(int subchannel, double value) {
        lab.sendCommand(commChannel, address, subchannel, value);
    }

    public void sendCommand(int subchannel, String value) {
        lab.sendCommand(commChannel, address, subchannel, value);
    }

    public void sendCommand(int subchannel, int value) {
        lab.sendCommand(commChannel, address, subchannel, value);
    }

    public void sendCommand(int subchannel, long value) {
        lab.sendCommand(commChannel, address, subchannel, value);
    }

    public void queryValueAsynchronously(int subchannel) {
        lab.queryValueAsynchronously(commChannel, address, subchannel);
    }

    public double queryDoubleValue(int subchannel) {
        return lab.queryValue(commChannel, address, subchannel);
    }

    public int queryIntegerValue(int subchannel) {
        return lab.queryIntegerValue(commChannel, address, subchannel);
    }

    public long queryLongValue(int subchannel) {
        return lab.queryLongValue(commChannel, address, subchannel);
    }

    public String queryStringValue(int subchannel) {
        return lab.queryStringValue(commChannel, address, subchannel);
    }

    public String getBoardInstanceIdentifier() {
        return this.commChannel.getChannelName() + "_" + this.getBoardIdentifier() + "(" + this.getAddress() + ")";
    }

    public String getBoardInstanceIdentifierForScripting() {
        return this.commChannel.getChannelName() + "_" + this.getBoardIdentifier() + "_" + this.getAddress();
    }

    public String getBoardInstanceIdentifierForMenu() {
        return this.commChannel.getChannelName() + "/" + this.getBoardIdentifier() + "(" + this.getAddress() + ")";
    }

    public boolean equals(Object other) {
        boolean equal = true;
        if (!(other instanceof Board)) return false;
        Board otherBoard = (Board) other;
        boolean channelFits = this.getCommChannel().getChannelName().equals(otherBoard.getCommChannel().getChannelName());
        boolean addressFits = this.getAddress() == otherBoard.getAddress();
        boolean typeFits = this.getBoardIdentifier().equals(otherBoard.getBoardIdentifier());
        return channelFits && addressFits && typeFits;
    }

    public int hashCode() {
        if (getCommChannel() != null) return (getCommChannel().getChannelName() + getAddress() + getBoardIdentifier()).hashCode(); else return (getAddress() + getBoardIdentifier()).hashCode();
    }

    public String toString() {
        return getBoardIdentifier() + "-" + getCommChannel().getChannelName() + "(" + this.getAddress() + ")";
    }
}
