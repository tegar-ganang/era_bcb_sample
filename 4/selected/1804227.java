package com.xentelco.asterisk.agi.command;

import java.util.HashMap;
import java.util.Map;
import com.xentelco.asterisk.agi.AgiCommandResponse;

/**
 * @author Ussama Baggili
 * 
 */
public class Hangup extends AbstractCommand {

    private static final String command = "HANGUP";

    private String channelName;

    static {
        Map<String, CommandResult> resultInfo = new HashMap<String, CommandResult>();
        resultInfo.put("1", CommandResult.SUCCESS);
        resultInfo.put("-1", CommandResult.FAILURE);
        AgiCommandResponse.commandResultTypes.put(Hangup.class, resultInfo);
    }

    /**
     * 
     * @return either "HANGUP" or "HANGUP channelName".
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
