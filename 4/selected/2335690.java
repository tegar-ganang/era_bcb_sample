package org.jdna.sagetv.networkencoder;

/**
 * START command implemenation
 * @author seans
 *
 */
public class StartCommand implements ICommand {

    public static final String CMD_NAME = "START";

    private String channel;

    private String filename;

    public StartCommand(String msgBody) {
        String fields[] = msgBody.split("\\|");
        channel = fields[2];
        filename = fields[4];
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
