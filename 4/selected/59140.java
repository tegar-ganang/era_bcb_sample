package de.fuh.xpairtise.plugin.ui.xpviews.chat;

import de.fuh.xpairtise.common.Constants;
import de.fuh.xpairtise.common.LogConstants;
import de.fuh.xpairtise.common.XPLog;
import de.fuh.xpairtise.common.network.IServerCommandInterface;
import de.fuh.xpairtise.common.network.NetworkException;
import de.fuh.xpairtise.common.replication.IReplicatedListEventListener;
import de.fuh.xpairtise.common.replication.IReplicatedListReceiver;
import de.fuh.xpairtise.common.replication.UnexpectedReplicationState;
import de.fuh.xpairtise.common.replication.elements.ReplicatedChatEntry;
import de.fuh.xpairtise.plugin.core.ClientApplication;
import de.fuh.xpairtise.plugin.network.ClientSideCommunicationFactory;
import de.fuh.xpairtise.plugin.network.ConnectionState;
import de.fuh.xpairtise.plugin.network.IConnectionStateListener;
import de.fuh.xpairtise.plugin.network.RemoteEventManager;
import de.fuh.xpairtise.plugin.util.ClientXPLog;

/**
 * This class represents the controller part of the chat implementation.
 */
public class ChatController implements IChatController {

    private IServerCommandInterface commandInterface;

    private IReplicatedListReceiver<ReplicatedChatEntry> chatLogReceiver;

    private IChatView chatView;

    private IConnectionStateListener csl;

    private IReplicatedListEventListener<ReplicatedChatEntry> rel;

    /**
   * 
   * @param view
   * @throws Exception
   */
    public ChatController(IChatView view) throws Exception {
        if (XPLog.isDebugEnabled()) {
            XPLog.printDebug(LogConstants.LOG_PREFIX_CHAT_CONTROLLER + "starting.");
        }
        this.chatView = view;
        csl = new RemoteEventListener();
        RemoteEventManager.getInstance().addConnectionStateListener(csl);
        ClientSideCommunicationFactory factory = ClientSideCommunicationFactory.getInstance();
        commandInterface = factory.getServerCommandInterface();
    }

    /**
   * attaches this <code>ChatController</code> to the replicated master list.
   */
    protected synchronized void attach() {
        if (chatLogReceiver == null) {
            try {
                ClientSideCommunicationFactory factory = ClientSideCommunicationFactory.getInstance();
                chatView.setLocalUserGroup(ClientApplication.getInstance().getUserManager().getUserGroup());
                rel = new ReplicatedEventListener();
                chatLogReceiver = factory.attachToList(getChannelId(), rel);
                chatView.activate();
            } catch (NetworkException e) {
                ClientXPLog.logException(0, "Chat Controller", "error when trying to attach", e, false);
            }
        }
    }

    /**
   * detaches this <code>ChatController</code> from the replicated master
   * list.
   */
    protected void detach() {
        if (chatLogReceiver != null) {
            try {
                chatLogReceiver.detach();
            } catch (NetworkException e) {
                ClientXPLog.logException(0, "Chat Controller", "error when trying to detach", e, false);
            }
            chatLogReceiver = null;
            if (ClientApplication.getInstance() != null && !ClientApplication.getInstance().getShutdownFlag()) {
                chatView.deactivate();
            }
        }
    }

    /**
   * dummy method; this would normally be an event from the view
   * 
   * @see de.fuh.xpairtise.plugin.ui.xpviews.chat.IChatController#enterChatMessage(java.lang.String)
   */
    public void enterChatMessage(String text) throws Exception {
        if (chatLogReceiver != null) {
            commandInterface.sendChatMessage(chatLogReceiver.getListId(), text);
        }
    }

    public void close() throws Exception {
        if (csl != null && RemoteEventManager.getInstance() != null) {
            RemoteEventManager.getInstance().removeConnectionStateListener(csl);
        }
        detach();
    }

    /**
   * returns the channel Id.
   * 
   * @return the channel Id.
   */
    protected String getChannelId() {
        return Constants.MASTER_LIST_NAME_GLOBAL_CHAT;
    }

    class ReplicatedEventListener implements IReplicatedListEventListener<ReplicatedChatEntry> {

        public void add(ReplicatedChatEntry element) throws UnexpectedReplicationState {
            chatView.addEntry(element);
        }

        public void remove(ReplicatedChatEntry element) throws UnexpectedReplicationState {
        }

        public void update(ReplicatedChatEntry element) throws UnexpectedReplicationState {
        }

        public void removeAll() throws UnexpectedReplicationState {
        }
    }

    /**
   * Delegates call to {@link #attach() attach()}. Called when the connection
   * to the server was established.
   */
    protected void attachOnConnect() {
        attach();
    }

    private class RemoteEventListener implements IConnectionStateListener {

        public void onConnected() {
            attachOnConnect();
        }

        public void onConnectionFailed(String message) {
        }

        public void onConnectionStateChanged(ConnectionState newState) {
        }

        public void onDisconnected() {
            detach();
        }

        public void onConnectionDesiredStateChanged(boolean desired) {
        }
    }
}
