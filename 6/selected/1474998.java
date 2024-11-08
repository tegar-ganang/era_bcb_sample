package net.sf.mailsomething.mail;

import java.io.*;
import net.sf.mailsomething.mail.parsers.*;
import java.util.Vector;

/**
 *  Class which encapsulates a smtpaccount, and take care of sending mails.
 *  Should use a thread for sending mails.
 * 
 * Todo: Needs a running thread and a stack possible, to send messages without
 * the rest of the eventhandling to lag.
 *
 *@author     Stig Tanggaard
 *@created    September 29, 2001
 */
public class SmtpAccount extends Account implements Serializable {

    static final long serialVersionUID = -3265154512807356955L;

    public static final int DEFAULT_PORT = 25;

    private boolean requiresLogin = false;

    private MailAddress mailAddress = null;

    transient SmtpSocketSession session = null;

    transient SmtpSessionHandler smtpSessionHandler = null;

    transient Vector messageBuffer;

    private int socketTimeout = 30000;

    public SmtpAccount() {
        smtpSessionHandler = new SmtpSessionHandler(this);
    }

    public SmtpAccount(String userName, String password, String host) {
        super(userName, password, host, DEFAULT_PORT);
        messageBuffer = new Vector();
        smtpSessionHandler = new SmtpSessionHandler(this);
    }

    /**
	 *  Constructor for the SmtpAccount object
	 *
	 *@param  name         Description of Parameter
	 *@param  host         Description of Parameter
	 *@param  port         Description of Parameter
	 *@param  mailaddress  Description of Parameter
	 */
    public SmtpAccount(String userName, String password, String host, int port) {
        super(userName, password, host, port);
        messageBuffer = new Vector();
    }

    /**
	 *  Gets the protocol attribute of the SmtpAccount object
	 *
	 *@return    The protocol value
	 */
    public String getProtocol() {
        return ("smtp");
    }

    public void sendMail(Message message) {
        sendMail(message, mailAddress);
    }

    /**
	 * For sending mails through javamail. Currently this makes use of the
	 * contenttype field in the message, so this have to be set (contrary to
	 * recieved messages, where I use MimeType).
	 * 
	 */
    public void sendMail(Message message, MailAddress address) {
        getSessionHandler();
        if (session == null) {
            session = new SmtpSocketSession();
        }
        if (!smtpSessionHandler.isConnected()) {
            smtpSessionHandler.connect(session, this);
            if (!smtpSessionHandler.isConnected()) return;
            if (requiresLogin()) {
                if (getUserName() == null || getPassword() == null) return;
                session.login(getUserName(), getPassword());
                if (smtpSessionHandler.isLoggedIn()) {
                } else {
                    session.quit();
                    return;
                }
            }
        }
        message.setField(RFC822.FROM, address.toString());
        try {
            session.mailFrom(getMailAddress().getEmail());
        } catch (IOException f) {
        }
        AddressList list = message.getTo();
        MailAddress[] recipients = list.getAddresses();
        for (int i = 0; i < recipients.length; i++) {
            session.rcptTo(recipients[i].getEmail());
        }
        session.data(message);
        session.sendMessage();
        message.setFlag(Message.SENT);
        smtpSessionHandler.disconnect();
    }

    /**
	 *  Sets the requiresLogin attribute of the SmtpAccount object
	 *
	 *@param  login  The new requiresLogin value
	 */
    public void setRequiresLogin(boolean login) {
        requiresLogin = login;
    }

    /**
	 *  Description of the Method
	 *
	 *@return    Description of the Return Value
	 */
    public boolean requiresLogin() {
        return requiresLogin;
    }

    /**
	 *  Gets the mailAddress attribute of the SmtpAccount object
	 *
	 *@return    The mailAddress value
	 */
    public MailAddress getMailAddress() {
        return mailAddress;
    }

    /**
	 *  Sets the mailAddress attribute of the SmtpAccount object
	 *
	 *@param  mailAddress  The new mailAddress value
	 */
    public void setMailAddress(MailAddress mailAddress) {
        this.mailAddress = mailAddress;
    }

    /**
	 * Method addSmtpListener.
	 * @param listener
	 */
    public void addSmtpListener(SmtpListener listener) {
    }

    /**
	 * Method getSessionHandler.
	 * @return SessionHandler
	 */
    public SessionHandler getSessionHandler() {
        if (smtpSessionHandler == null) smtpSessionHandler = new SmtpSessionHandler(this);
        return smtpSessionHandler;
    }

    public void setSocketTimeout(int msecs) {
        socketTimeout = msecs;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }
}
