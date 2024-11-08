package org.yawlfoundation.yawl.procletService.persistence;

/**
 * @author Michael Adams
 * @date 2/02/12
 */
public class StoredPortConnection {

    private long pkey;

    private String input;

    private String channel;

    private String output;

    public StoredPortConnection() {
    }

    public StoredPortConnection(String input, String channel, String output) {
        this.input = input;
        this.channel = channel;
        this.output = output;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public long getPkey() {
        return pkey;
    }

    public void setPkey(long pkey) {
        this.pkey = pkey;
    }
}
