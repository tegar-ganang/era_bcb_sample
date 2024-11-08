package chipchat;

/**
 * @author MTY.
 */
public class ConnectionInfo {

    /** name */
    private String name;

    /** channel */
    private String channel;

    /** password of room */
    private String roompw;

    /** User id */
    private int userid;

    /** Room id */
    private Long roomid;

    /**
	 * @return
	 */
    public String getChannel() {
        return channel;
    }

    /**
	 * @return
	 */
    public String getName() {
        return name;
    }

    /**
	 * @return
	 */
    public Long getRoomid() {
        return roomid;
    }

    /**
	 * @return
	 */
    public String getRoompw() {
        return roompw;
    }

    /**
	 * @return
	 */
    public int getUserid() {
        return userid;
    }

    /**
	 * @param string
	 */
    public void setChannel(String string) {
        channel = string;
    }

    /**
	 * @param string
	 */
    public void setName(String string) {
        name = string;
    }

    /**
	 * @param l
	 */
    public void setRoomid(Long l) {
        roomid = l;
    }

    /**
	 * @param string
	 */
    public void setRoompw(String string) {
        roompw = string;
    }

    /**
	 * @param i
	 */
    public void setUserid(int i) {
        userid = i;
    }
}
