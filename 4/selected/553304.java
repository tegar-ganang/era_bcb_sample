package com.quikj.ace.messages.vo.talk;

public class SetupRequestMessage implements TalkMessageInterface {

    private static final long serialVersionUID = 980467070567185954L;

    private CallingNameElement calling = null;

    private CalledNameElement called = null;

    private long sessionId = -1;

    private String transferId = null;

    private String transferFrom = null;

    private boolean userTransfer = false;

    private boolean userConference = false;

    private MediaElements media = null;

    public SetupRequestMessage() {
    }

    public CalledNameElement getCalledNameElement() {
        return called;
    }

    public CallingNameElement getCallingNameElement() {
        return calling;
    }

    /**
	 * Getter for property media.
	 * 
	 * @return Value of property media.
	 * 
	 */
    public MediaElements getMedia() {
        return media;
    }

    public long getSessionId() {
        return sessionId;
    }

    /**
	 * Getter for property transferFrom.
	 * 
	 * @return Value of property transferFrom.
	 * 
	 */
    public String getTransferFrom() {
        return transferFrom;
    }

    /**
	 * Getter for property transferId.
	 * 
	 * @return Value of property transferId.
	 */
    public String getTransferId() {
        return transferId;
    }

    public void setCalledNameElement(CalledNameElement called) {
        this.called = called;
    }

    public void setCallingNameElement(CallingNameElement calling) {
        this.calling = calling;
    }

    /**
	 * Setter for property media.
	 * 
	 * @param media
	 *            New value of property media.
	 * 
	 */
    public void setMedia(MediaElements media) {
        this.media = media;
    }

    public void setSessionId(long id) {
        sessionId = id;
    }

    /**
	 * Setter for property transferFrom.
	 * 
	 * @param transferFrom
	 *            New value of property transferFrom.
	 * 
	 */
    public void setTransferFrom(java.lang.String transferFrom) {
        this.transferFrom = transferFrom;
    }

    /**
	 * Setter for property transferId.
	 * 
	 * @param transferId
	 *            New value of property transferId.
	 */
    public void setTransferId(java.lang.String transferId) {
        this.transferId = transferId;
    }

    /**
	 * @return the userTransfer
	 */
    public boolean isUserTransfer() {
        return userTransfer;
    }

    /**
	 * @param userTransfer
	 *            the userTransfer to set
	 */
    public void setUserTransfer(boolean userTransfer) {
        this.userTransfer = userTransfer;
    }

    /**
	 * @return the userConference
	 */
    public boolean isUserConference() {
        return userConference;
    }

    /**
	 * @param userConference
	 *            the userConference to set
	 */
    public void setUserConference(boolean userConference) {
        this.userConference = userConference;
    }
}
