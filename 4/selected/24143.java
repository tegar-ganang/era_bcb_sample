package com.shimari.bot;

/**
 * Part of the implementation of ConsoleConnection.
 */
public class ConsoleMessage implements Message {

    private final String my_message;

    private final Connection conn;

    /**
     * Return debug data
     */
    public String toString() {
        return "ConsoleMessage(" + my_message + ")";
    }

    /**
     * Create a new message
     */
    public ConsoleMessage(String message, Connection conn) {
        this.my_message = message;
        this.conn = conn;
    }

    /**
     * Never public -- return false
     */
    public boolean isPublic() {
        return false;
    }

    /**
     * Always addressed -- return true
     */
    public boolean isAddressed() {
        return true;
    }

    /**
     * Nick is "console"
     */
    public String getFromNick() {
        return "console";
    }

    /**
     * User is "console"
     */
    public String getFromUser() {
        return "console";
    }

    /**
     * Host is "localhost"
     */
    public String getFromHost() {
        return "localhost";
    }

    /**
     * Get the message
     */
    public String getMessage() {
        return my_message;
    }

    /**
     * Channel is #console
     */
    public String getChannel() {
        return "#console";
    }

    /**
     * Nick is "console"
     */
    public String getToNick() {
        return "console";
    }

    /**
     * Server is "localhost"
     */
    public String getToServer() {
        return "localhost";
    }

    /**
     * Print to the console
     */
    public void sendReply(String message) {
        System.out.println(message);
    }

    /**
     * Print to the console
     */
    public void sendReply(String[] message) {
        for (int i = 0; i < message.length; i++) {
            sendReply(message[i]);
        }
    }

    public Connection getConnection() {
        return conn;
    }
}
