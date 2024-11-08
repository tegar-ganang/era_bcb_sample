package org.asteriskjava.manager.event;

/**
 * A NewAccountCodeEvent indicates that the account code used for CDR has
 * changed.<p>
 * Available since Asterisk 1.6.<p>
 * It is implemented in <code>main/cdr.c</code>
 *
 * @author srt
 * @version $Id: NewAccountCodeEvent.java 1095 2008-08-09 01:49:43Z sprior $
 * @since 1.0.0
 */
public class NewAccountCodeEvent extends ManagerEvent {

    private static final long serialVersionUID = -1786512014173534223L;

    private String channel;

    private String uniqueId;

    private String accountCode;

    private String oldAccountCode;

    /**
     * @param source
     */
    public NewAccountCodeEvent(Object source) {
        super(source);
    }

    /**
     * Returns the name of the channel.
     *
     * @return the name of the channel.
     */
    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the unique id of the channel.
     *
     * @return the unique id of the channel.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Returns the new account code.
     *
     * @return the new account code.
     */
    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    /**
     * Returns the old account code.
     *
     * @return the old account code.
     */
    public String getOldAccountCode() {
        return oldAccountCode;
    }

    public void setOldAccountCode(String oldAccountCode) {
        this.oldAccountCode = oldAccountCode;
    }
}
