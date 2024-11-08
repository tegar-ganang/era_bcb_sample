package org.speakmon.babble;

/**
 *
 * @author  speakmon
 */
public final class WhoisInfo {

    /** Holds value of property userInfo. */
    private UserInfo userInfo;

    /** Holds value of property realName. */
    private String realName;

    /** Holds value of property server. */
    private String server;

    /** Holds value of property serverDescription. */
    private String serverDescription;

    /** Holds value of property idleTime. */
    private long idleTime;

    /** Holds value of property isOperator. */
    private boolean isOperator;

    /** Creates a new instance of WhoisInfo */
    protected WhoisInfo() {
    }

    /** Getter for property userInfo.
     * @return Value of property userInfo.
     *
     */
    public UserInfo getUserInfo() {
        return this.userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    /** Getter for property realName.
     * @return Value of property realName.
     *
     */
    public String getRealName() {
        return this.realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    /** Getter for property server.
     * @return Value of property server.
     *
     */
    public String getServer() {
        return this.server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    /** Getter for property serverDescription.
     * @return Value of property serverDescription.
     *
     */
    public String getServerDescription() {
        return this.serverDescription;
    }

    public void setServerDescription(String serverDescription) {
        this.serverDescription = serverDescription;
    }

    /** Getter for property idleTime.
     * @return Value of property idleTime.
     *
     */
    public long getIdleTime() {
        return this.idleTime;
    }

    public void setIdleTime(long idleTime) {
        this.idleTime = idleTime;
    }

    /** Getter for property isOperator.
     * @return Value of property isOperator.
     *
     */
    public boolean isOperator() {
        return this.isOperator;
    }

    public void setOperator(boolean isOperator) {
        this.isOperator = isOperator;
    }

    protected void setChannels(String[] channels) {
    }

    public String[] getChannels() {
        return new String[0];
    }
}
