package de.fuh.xpairtise.plugin.ui.xpviews.sessiongallery;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import de.fuh.xpairtise.common.Constants;
import de.fuh.xpairtise.common.LogConstants;
import de.fuh.xpairtise.common.XPLog;
import de.fuh.xpairtise.common.network.IServerCommandInterface;
import de.fuh.xpairtise.common.network.NetworkException;
import de.fuh.xpairtise.common.network.data.CreateXPSessionRequestReply;
import de.fuh.xpairtise.common.network.data.JoinXPSessionRequestReply;
import de.fuh.xpairtise.common.network.data.LeaveXPSessionRequestReply;
import de.fuh.xpairtise.common.network.data.RemoveXPSessionRequestReply;
import de.fuh.xpairtise.common.replication.IReplicatedListReceiver;
import de.fuh.xpairtise.common.replication.elements.ReplicatedXPSession;
import de.fuh.xpairtise.plugin.core.ClientApplication;
import de.fuh.xpairtise.plugin.core.IClientXPSessionChangeListener;
import de.fuh.xpairtise.plugin.network.ClientSideCommunicationFactory;
import de.fuh.xpairtise.plugin.network.ConnectionState;
import de.fuh.xpairtise.plugin.network.IConnectionStateListener;
import de.fuh.xpairtise.plugin.network.IRemoteEventListener;
import de.fuh.xpairtise.plugin.network.RemoteEventManager;
import de.fuh.xpairtise.plugin.ui.xpviews.sessiongallery.util.RemoteEventListener;
import de.fuh.xpairtise.plugin.ui.xpviews.sessiongallery.util.ReplicatedEventListener;
import de.fuh.xpairtise.plugin.util.ClientXPLog;
import de.fuh.xpairtise.plugin.util.MonitorTools;

/**
 * This class represents the controller part of the session gallery
 * implementation.
 */
public class XPSessionGalleryController implements IXPSessionGalleryController {

    private IServerCommandInterface commandInterface;

    private IReplicatedListReceiver<ReplicatedXPSession> xpSessionReceiver;

    private IXPSessionGalleryView sessionView;

    private String localUserId;

    private LeaveXPSessionRequestReply leaveReply;

    private JoinXPSessionRequestReply requestReply;

    private IConnectionStateListener csl;

    private IRemoteEventListener rel;

    private SessionListener sessionListener;

    private String localUserGroup;

    private String sessionId = null;

    /**
   * Creates a new <code>IXPSessionGalleryController</code> instance.
   * 
   * @param view
   *          the <code>IXPSessionGalleryView</code> instance to control
   * @throws Exception
   */
    public XPSessionGalleryController(IXPSessionGalleryView view) throws Exception {
        if (XPLog.isDebugEnabled()) {
            XPLog.printDebug(LogConstants.LOG_PREFIX_XPSESSIONGALLERY_CONTROLLER + "new instance for channel id: " + getChannelId());
        }
        this.sessionView = view;
        csl = new ConnectionStateListener();
        RemoteEventManager.getInstance().addConnectionStateListener(csl);
        rel = new RemoteEventListener(this.sessionView);
        RemoteEventManager.getInstance().addMessageListener(rel);
        sessionListener = new SessionListener();
        ClientApplication.getInstance().getXPSessionManager().addListener(sessionListener);
        ClientSideCommunicationFactory factory = ClientSideCommunicationFactory.getInstance();
        commandInterface = factory.getServerCommandInterface();
        localUserId = ClientApplication.getInstance().getUserManager().getUserId();
        localUserGroup = ClientApplication.getInstance().getUserManager().getUserGroup();
        sessionView.setLocalUserId(localUserId);
        sessionView.setLocalUserGroup(localUserGroup);
        if (XPLog.isDebugEnabled()) {
            XPLog.printDebug(LogConstants.LOG_PREFIX_XPSESSIONGALLERY_CONTROLLER + "local user name=" + localUserId);
        }
    }

    public CreateXPSessionRequestReply create(String xpSessionTopic, boolean uploadsPending, String user) throws Exception {
        if (XPLog.isDebugEnabled() && xpSessionReceiver != null) {
            XPLog.printDebug(LogConstants.LOG_PREFIX_XPSESSIONGALLERY_CONTROLLER + "sendXPSessionCreateMessage() call for replicated list: " + xpSessionReceiver.getListId() + ", topic: " + xpSessionTopic + ", user: " + user);
        }
        return commandInterface.sendXPSessionCreateRequest(xpSessionTopic, uploadsPending);
    }

    public void addProject(String xpSessionId, final String project, boolean uploadsPending, IProgressMonitor monitor) throws Exception {
        ClientApplication.getInstance().getResourceManager().getUploader().uploadProject(xpSessionId, project, uploadsPending, monitor);
    }

    public void removeProjects(String xpSessionId, List<String> projects) throws Exception {
        commandInterface.sendRemoveProjectsMessage(xpSessionId, projects);
    }

    public JoinXPSessionRequestReply join(String xpSessionId, String user, int preferredRole, IProgressMonitor monitor) throws Exception {
        try {
            monitor.beginTask("Joining session: " + xpSessionId, 1000);
            if (XPLog.isDebugEnabled()) {
                XPLog.printDebug(LogConstants.LOG_PREFIX_XPSESSIONGALLERY_CONTROLLER + "sendXPSessionJoinMessage() call for replicated list: " + xpSessionReceiver.getListId() + ", xpSessionId: " + xpSessionId + ", user: " + user + ", preferred role: " + preferredRole);
            }
            try {
                requestReply = commandInterface.sendXPSessionJoinRequest(xpSessionId, user, preferredRole);
                if (requestReply.getResult().equals(Constants.REQUEST_REPLY_OK)) {
                    boolean b = false;
                    synchronized (XPSessionGalleryController.this) {
                        while (xpSessionReceiver != null && sessionId == null) {
                            if (XPLog.isDebugEnabled()) {
                                XPLog.printDebug(LogConstants.LOG_PREFIX_XPSESSIONGALLERY_CONTROLLER + "waiting an update of the session manager.");
                            }
                            XPSessionGalleryController.this.wait();
                        }
                        b = sessionId != null && sessionId.equals(xpSessionId) && xpSessionReceiver != null;
                    }
                    if (b) {
                        ClientApplication.getInstance().attachResourceManagement(xpSessionId, MonitorTools.subMonitorFor(monitor, 1000));
                    } else {
                        throw new Exception("An error occurred during join");
                    }
                }
            } catch (Exception e) {
                requestReply.setResult(Constants.REQUEST_REPLY_ERROR);
                try {
                    leaveReply = commandInterface.sendXPSessionLeaveRequest(xpSessionId, user);
                } catch (NetworkException e1) {
                    ClientXPLog.logException(0, "Session Gallery Controller", "error when trying to leave session '" + xpSessionId + "' after failed join request", e1, false);
                }
                throw e;
            }
            return requestReply;
        } finally {
            monitor.done();
        }
    }

    public LeaveXPSessionRequestReply leave(String xpSessionId, String user, IProgressMonitor monitor) throws Exception {
        monitor.beginTask("Leaving session", 100);
        if (XPLog.isDebugEnabled()) {
            XPLog.printDebug(LogConstants.LOG_PREFIX_XPSESSIONGALLERY_CONTROLLER + "sendXPSessionLeaveMessage() call for replicated list: " + xpSessionReceiver.getListId() + ", xpSessionId: " + xpSessionId + ", user: " + user);
        }
        final String id = xpSessionId;
        final String u = user;
        synchronized (XPSessionGalleryController.this) {
            try {
                monitor.beginTask("Leaving Session", IProgressMonitor.UNKNOWN);
                leaveReply = commandInterface.sendXPSessionLeaveRequest(id, u);
                while (sessionId != null) {
                    XPSessionGalleryController.this.wait();
                }
                monitor.worked(15);
            } catch (Exception e) {
                if (leaveReply != null) {
                    leaveReply.setResult(Constants.REQUEST_REPLY_ERROR);
                }
                ClientXPLog.logException(0, "Session Gallery Controller", "error when trying to leave session '" + id + "'", e, false);
            } finally {
                ClientApplication.getInstance().detachResourceManagement(MonitorTools.subMonitorFor(monitor, 85));
                monitor.done();
            }
        }
        return leaveReply;
    }

    public RemoveXPSessionRequestReply remove(String xpSessionId, String user) throws Exception {
        if (XPLog.isDebugEnabled()) {
            XPLog.printDebug(LogConstants.LOG_PREFIX_XPSESSIONGALLERY_CONTROLLER + "sendXPSessionRemoveMessage() call for replicated list: " + xpSessionReceiver.getListId() + ", xpSessionId: " + xpSessionId + ", user: " + user);
        }
        return commandInterface.sendXPSessionRemoveRequest(xpSessionId, user);
    }

    public void sendMessageToUser(String sender, String recipient, String message) throws Exception {
        commandInterface.sendMessage(sender, recipient, message);
    }

    private String getChannelId() {
        return Constants.MASTER_LIST_NAME_XPSESSION_GALLERY;
    }

    private synchronized void attach() {
        if (xpSessionReceiver == null) {
            try {
                ClientSideCommunicationFactory factory = ClientSideCommunicationFactory.getInstance();
                localUserId = ClientApplication.getInstance().getUserManager().getUserId();
                localUserGroup = ClientApplication.getInstance().getUserManager().getUserGroup();
                sessionView.setLocalUserId(localUserId);
                sessionView.setLocalUserGroup(localUserGroup);
                xpSessionReceiver = factory.attachToList(getChannelId(), new ReplicatedEventListener(sessionView));
            } catch (NetworkException e) {
                ClientXPLog.logException(0, "Session Gallery Controller", "error when trying to attach", e, false);
            }
            try {
                sessionView.activate();
            } catch (Exception e) {
                ClientXPLog.logException(0, "Session Gallery Controller", "error when trying to activate the view", e, false);
            } finally {
                notifyAll();
            }
        }
    }

    public synchronized void detach() {
        if (xpSessionReceiver != null) {
            try {
                xpSessionReceiver.detach();
            } catch (NetworkException e) {
                ClientXPLog.logException(0, "Session Gallery Controller", "error when trying to detach", e, false);
            }
            try {
                if (sessionView != null) {
                    sessionView.deactivate();
                }
            } catch (Exception e) {
                ClientXPLog.logException(0, "Session Gallery Controller", "error when trying to deactivate the view", e, false);
            } finally {
                xpSessionReceiver = null;
                notifyAll();
            }
        }
    }

    public void close() {
        try {
            if (sessionListener != null) {
                ClientApplication.getInstance().getXPSessionManager().removeListener(sessionListener);
            }
            if (csl != null) {
                RemoteEventManager.getInstance().removeConnectionStateListener(csl);
            }
            if (rel != null) {
                RemoteEventManager.getInstance().removeMessageListener(rel);
            }
        } catch (Exception e) {
            ClientXPLog.logException(0, "Session Gallery Controller", "error when trying to close", e, false);
        } finally {
            detach();
        }
    }

    /**
   * Delegates call to {@link #attach() attach()}. Called when the connection to
   * the server was established.
   */
    protected void attachOnConnect() {
        attach();
    }

    private class SessionListener implements IClientXPSessionChangeListener {

        public void notificationFinished() {
            synchronized (XPSessionGalleryController.this) {
                sessionId = ClientApplication.getInstance().getXPSessionManager().getId();
                XPSessionGalleryController.this.notifyAll();
            }
        }

        public void notifyXPSessionChanged() {
        }
    }

    private class ConnectionStateListener implements IConnectionStateListener {

        public void onConnected() {
            if (XPLog.isDebugEnabled()) {
                XPLog.printDebug(LogConstants.LOG_PREFIX_XPSESSIONGALLERY_CONTROLLER + "onConnected() call for local user: " + localUserId);
            }
            attachOnConnect();
        }

        public void onConnectionFailed(String message) {
            if (XPLog.isDebugEnabled()) {
                XPLog.printDebug(LogConstants.LOG_PREFIX_XPSESSIONGALLERY_CONTROLLER + "onConnectionFailed() call for local user \"" + localUserId + "\", reason: \"" + message + "\"");
            }
        }

        public void onDisconnected() {
            if (XPLog.isDebugEnabled()) {
                XPLog.printDebug(LogConstants.LOG_PREFIX_XPSESSIONGALLERY_CONTROLLER + "onDisconnected() call for local user: " + localUserId);
            }
            detach();
        }

        public void onConnectionStateChanged(ConnectionState newState) {
            if (XPLog.isDebugEnabled()) {
                XPLog.printDebug(LogConstants.LOG_PREFIX_XPSESSIONGALLERY_CONTROLLER + "onConnectionStateChanged() call for local user: " + localUserId + ", new connection state: " + newState);
            }
        }

        public void onConnectionDesiredStateChanged(boolean desired) {
            if (XPLog.isDebugEnabled()) {
                XPLog.printDebug(LogConstants.LOG_PREFIX_XPSESSIONGALLERY_CONTROLLER + "onConnectionDesiredStateChanged() call for local user: " + localUserId + ", state change desired: " + desired);
            }
        }
    }
}
