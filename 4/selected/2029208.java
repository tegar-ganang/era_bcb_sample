package utils;

/**
 *
 */
public abstract class IRCParser {

    /*******************************************
     *                                         *
     *             Status messages             *
     *                                         *
     *******************************************/
    public static final int RPL_WELCOME = 001;

    public static final int RPL_AWAY = 301;

    public static final int RPL_TOPIC = 332;

    public static final int RPL_TOPICWHOTIME = 333;

    public static final int RPL_NAMREPLY = 353;

    public static final int RPL_ENDOFNAMES = 366;

    public static final int ERR_NICKNAMEINUSE = 433;

    /**
     * Checks if the message is a status-message.
     *
     * @param message the message to check
     * @return boolean true if the message is a status message
     */
    public static boolean isStatusMessage(final String message) {
        return getStatusCode(message) != -1;
    }

    /**
     * Returns the status code from the message.
     * 
     * @param message the status message
     * @return int the status code
     */
    public static int getStatusCode(final String message) {
        int index = message.indexOf(" ");
        String temp = message.substring(index).trim();
        index = temp.indexOf(" ");
        temp = temp.substring(0, index).trim();
        try {
            return Integer.parseInt(temp);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public static String getMessage(final String message) {
        int iterations = -1;
        String tmp = message;
        if (isStatusMessage(message)) {
            final int status = getStatusCode(message);
            switch(status) {
                case RPL_NAMREPLY:
                    iterations = 5;
                    break;
                case RPL_TOPIC:
                    iterations = 4;
                    break;
                default:
                    iterations = 3;
            }
        } else iterations = 3;
        for (int i = 0; i < iterations; ++i) tmp = tmp.substring(tmp.indexOf(" ") + 1);
        if (tmp.startsWith(":")) tmp = tmp.substring(1);
        return tmp;
    }

    public static boolean isCommandMessage(final String message) {
        String temp = getCommand(message);
        return temp.equals("PRIVMSG") || temp.equals("MODE") || temp.equals("JOIN") || temp.equals("PART") || temp.equals("QUIT") || temp.equals("NICK") || temp.equals("TOPIC") || temp.equals(("KICK"));
    }

    public static String getAuthor(final String message) {
        try {
            String author = message.substring(1).trim();
            author = author.substring(0, author.indexOf("!")).trim();
            return author;
        } catch (Exception ex) {
            System.out.println("ERROR: getAuthor while parsing: " + message);
            return "";
        }
    }

    public static String getAuthorHost(final String message) {
        String host = message.substring(message.indexOf("!") + 1);
        host = host.substring(0, host.indexOf(" "));
        return host;
    }

    public static String getCommand(final String message) {
        int index = message.indexOf(" ");
        String temp = message.substring(index).trim();
        index = temp.indexOf(" ");
        return temp.substring(0, index).trim();
    }

    public static String getChannel(final String message) {
        int iterations = -1;
        String tmp = message;
        if (isStatusMessage(message)) iterations = 3; else if (isCommandMessage(message)) iterations = 2;
        for (int i = 0; i < iterations; ++i) tmp = tmp.substring(tmp.indexOf(" ")).trim();
        int index = tmp.indexOf(" ");
        if (index == -1) {
            if (tmp.startsWith(":")) tmp = tmp.substring(1);
            return tmp.trim();
        }
        String ttmp = tmp.substring(0, index).trim();
        if (ttmp.equals("*") || ttmp.equals("@") || ttmp.equals("=")) {
            tmp = tmp.substring(index).trim();
            index = tmp.indexOf(" ");
        } else if (!ttmp.startsWith("#")) return getAuthor(message);
        return tmp.substring(0, index).trim();
    }

    /**
     * Checks if the message is a PING-message.
     * 
     * @param message the message to check
     * @return boolean true if it is a PING-message
     */
    public static boolean isPINGMessage(final String message) {
        if (message == null || message.isEmpty()) return false;
        return message.startsWith("PING :");
    }

    public static String createPONGMessage(final String ping) {
        String reply = ping.substring(ping.indexOf(":")).trim();
        return "PONG " + reply;
    }
}
