package biz.xsoftware.impl.nio.cm.basic;

public class IdObject {

    private String id;

    private String cmId;

    private String channelId;

    private String name;

    private static final String LB = "[";

    private static final String RB = "]";

    private static final String S = " ";

    public IdObject(String cmId, String channelId) {
        if (cmId == null || cmId.length() == 0) this.id = LB + channelId + RB + S; else this.id = LB + cmId + RB + LB + channelId + RB + S;
        this.cmId = cmId;
        this.channelId = channelId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getCmId() {
        return cmId;
    }

    public String toString() {
        return id;
    }

    public void setChannelId(String o) {
        this.channelId = o;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name;
        this.id = LB + cmId + RB + LB + channelId + RB + LB + name + RB + S;
    }

    public String getName() {
        return name;
    }
}
