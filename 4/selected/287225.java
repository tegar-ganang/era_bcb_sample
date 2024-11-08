package de.fuh.xpairtise.plugin.ui.xpviews.chat;

import de.fuh.xpairtise.common.Constants;
import de.fuh.xpairtise.common.LogConstants;
import de.fuh.xpairtise.common.XPLog;
import de.fuh.xpairtise.plugin.core.ClientApplication;
import de.fuh.xpairtise.plugin.core.IClientUserChangeListener;
import de.fuh.xpairtise.plugin.core.IClientUserManager;

/**
 * This is the session chat controller part.
 */
public class SessionChatController extends ChatController {

    private IClientUserManager clientUserManager;

    private IClientUserChangeListener cucl;

    /**
   * Constructs a new session chat controller and attaches it to the given view
   * part.
   * 
   * @param view 
   *          the chat view attached to this controller
   * @throws Exception
   */
    public SessionChatController(IChatView view) throws Exception {
        super(view);
        if (XPLog.isDebugEnabled()) {
            XPLog.printDebug(LogConstants.LOG_PREFIX_SESSIONCHAT_CONTROLLER + "starting.");
        }
        clientUserManager = ClientApplication.getInstance().getUserManager();
        cucl = new ClientUserChangeListener();
        if (clientUserManager != null) {
            clientUserManager.addListener(cucl);
        }
    }

    public void close() throws Exception {
        super.close();
        if (clientUserManager != null && cucl != null) {
            clientUserManager.removeListener(cucl);
        }
    }

    protected void attachOnConnect() {
    }

    protected String getChannelId() {
        return Constants.MASTER_LIST_NAME_SESSION_CHAT + clientUserManager.getSessionId();
    }

    private class ClientUserChangeListener implements IClientUserChangeListener {

        public void notifyUserChange() {
            if (clientUserManager != null && clientUserManager.isInSession()) {
                attach();
            } else {
                detach();
            }
        }

        public void notificationFinished() {
        }
    }
}
