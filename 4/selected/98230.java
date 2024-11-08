package com.xentelco.asterisk.agi.command;

import java.util.HashMap;
import java.util.Map;
import com.xentelco.asterisk.agi.AgiCommandResponse;

/**
 * @author Ussama Baggili
 * 
 */
public class ChannelStatus extends AbstractCommand {

    private static final String command = "CHANNEL STATUS";

    private String channelName;

    static {
        Map<String, CommandResult> resultInfo = new HashMap<String, CommandResult>();
        resultInfo.put("-1", CommandResult.UNAVAILABLE);
        resultInfo.put("0", CommandResult.AVAILABLE);
        resultInfo.put("1", CommandResult.RESERVED);
        resultInfo.put("2", CommandResult.OFF_HOOK);
        resultInfo.put("3", CommandResult.DIALING_INITIATED);
        resultInfo.put("4", CommandResult.LINE_RINGING);
        resultInfo.put("5", CommandResult.REMOTE_END_RINGING);
        resultInfo.put("6", CommandResult.LINE_UP);
        resultInfo.put("7", CommandResult.LINE_BUSY);
        AgiCommandResponse.commandResultTypes.put(ChannelStatus.class, resultInfo);
    }

    /**
     * 
     * @return either "CHANNEL STATUS" or "CHANNEL STATUS channelName".
     */
    public String getCommand() {
        if (channelName != null) return new StringBuffer().append(command).append(' ').append(channelName).toString(); else return command;
    }

    /**
     * @return the channelName
     */
    public String getChannelName() {
        return this.channelName;
    }

    /**
     * @param channelName
     *            the channelName to set
     */
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
}
