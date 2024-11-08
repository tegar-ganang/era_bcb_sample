import borknet_services.core.*;

/**
 * Class to load configuration files.
 * @author Ozafy - ozafy@borknet.org - http://www.borknet.org
 */
public class UserTicket {

    private String user;

    private Long time;

    private String channel;

    /**
     * Constructs a Loader
     * @param debug		If we're running in debug.
     */
    public UserTicket(String user, long time, String channel) {
        this.user = user;
        this.time = time;
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
