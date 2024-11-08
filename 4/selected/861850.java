package com.xentelco.asterisk.agi.command;

import java.util.HashMap;
import java.util.Map;
import com.xentelco.asterisk.agi.AgiCommandResponse;
import com.xentelco.asterisk.agi.codec.command.response.AgiCommandResponseUtils;

/**
 * This is basically GET VARIABLE for "full" $-expressions:
 * 
 * GET FULL VARIABLE ${foo} is equivalent to GET VARIABLE foo
 * 
 * GET FULL VARIABLE ${func(bar)} is equivalent to GET VARIABLE func(bar)
 * 
 * GET FULL VARIABLE $[expr] has no GET VARIABLE equivalent
 * 
 * Note that in some bugged version of Asterisk 1.2, GET FULL VARIABLE was the
 * only way to retrieve global dialplan variables.
 * 
 * Returns:
 * 
 * failure or not set: 200 result=0
 * 
 * success: 200 result=1 <value>
 * 
 * @author Ussama Baggili
 * 
 */
public class GetFullVariable extends AbstractCommand {

    private static final String command = "GET FULL VARIABLE";

    private String variableName;

    private String channelName;

    private String responseValue;

    static {
        Map<String, CommandResult> resultInfo = new HashMap<String, CommandResult>();
        resultInfo.put("1", CommandResult.SUCCESS);
        resultInfo.put("0", CommandResult.FAILURE);
        AgiCommandResponse.commandResultTypes.put(GetFullVariable.class, resultInfo);
    }

    /**
     * 
     * @return "DATABASE DELTREE \<family\> \<keytree\> "
     */
    public String getCommand() {
        if (channelName != null) return new StringBuffer().append(command).append(' ').append(variableName).append(' ').append(channelName).toString(); else return new StringBuffer().append(command).append(' ').append(variableName).append(' ').toString();
    }

    @Override
    public void processResult(String unprocessedResult) {
        super.processResult(unprocessedResult);
        responseValue = AgiCommandResponseUtils.getRequestVariableValue(unprocessedResult);
    }

    /**
     * @return the variableName
     */
    public String getVariableName() {
        return this.variableName;
    }

    /**
     * @param variableName
     *            the variableName to set
     */
    public void setVariableName(String variableName) {
        this.variableName = variableName;
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
