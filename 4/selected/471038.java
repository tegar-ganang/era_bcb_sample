package org.asteriskjava.fastagi.command;

/**
 * Returns the value of the given channel varible and understands complex
 * variable names and builtin variables, unlike the GetVariableCommand.<p>
 * You can also use this command to use custom Asterisk functions. Syntax is
 * "func(args)".<p>
 * Returns 0 if the variable is not set or channel does not exist. Returns 1 if
 * the variable is set and returns the variable in parenthesis.<p>
 * Available since Asterisk 1.2<p>
 * Example return code: 200 result=1 (testvariable)
 * 
 * @since 0.2
 * @author srt
 * @version $Id: GetFullVariableCommand.java 938 2007-12-31 03:23:38Z srt $
 * @see org.asteriskjava.fastagi.command.GetVariableCommand
 */
public class GetFullVariableCommand extends AbstractAgiCommand {

    /**
     * Serial version identifier.
     */
    private static final long serialVersionUID = 3256719598056387384L;

    /**
     * The name of the variable to retrieve.
     */
    private String variable;

    private String channel;

    /**
     * Creates a new GetFullVariableCommand.
     * 
     * @param variable the name of the variable to retrieve.
     */
    public GetFullVariableCommand(String variable) {
        super();
        this.variable = variable;
    }

    /**
     * Creates a new GetFullVariableCommand.
     * 
     * @param variable the name of the variable to retrieve.
     * @param channel the name of the channel.
     */
    public GetFullVariableCommand(String variable, String channel) {
        super();
        this.variable = variable;
        this.channel = channel;
    }

    /**
     * Returns the name of the variable to retrieve.
     * 
     * @return the the name of the variable to retrieve.
     */
    public String getVariable() {
        return variable;
    }

    /**
     * Sets the name of the variable to retrieve.<p>
     * You can also use custom dialplan functions (like "func(args)") as
     * variable.
     * 
     * @param variable the name of the variable to retrieve.
     */
    public void setVariable(String variable) {
        this.variable = variable;
    }

    /**
     * Returns the the name of the channel.
     * 
     * @return the name of the channel.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Sets the name of the channel.
     * 
     * @param channel the name of the channel.
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    @Override
    public String buildCommand() {
        StringBuffer sb;
        sb = new StringBuffer("GET FULL VARIABLE ");
        sb.append(escapeAndQuote(variable));
        if (channel != null) {
            sb.append(" ");
            sb.append(escapeAndQuote(channel));
        }
        return sb.toString();
    }
}
