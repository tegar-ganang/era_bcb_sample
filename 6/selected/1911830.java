package plugins.collaboration.jabber.mindmap;

import java.util.LinkedList;
import java.util.logging.Level;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import com.echomine.jabber.Jabber;
import com.echomine.jabber.JabberChatMessage;
import com.echomine.jabber.JabberCode;
import com.echomine.jabber.JabberContext;
import com.echomine.jabber.JabberMessageEvent;
import com.echomine.jabber.JabberMessageException;
import com.echomine.jabber.JabberMessageListener;
import com.echomine.jabber.JabberSession;
import freemind.controller.actions.generated.instance.CollaborationAction;
import freemind.controller.actions.generated.instance.CompoundAction;
import freemind.controller.actions.generated.instance.XmlAction;
import freemind.main.Tools;
import freemind.modes.mindmapmode.MindMapController;
import freemind.modes.mindmapmode.actions.xml.ActionPair;

/**
 * @author RReppel - Connects to a jabber server. - Establishes a private chat
 *         with another user. - Listens to a limited number of FreeMind commands
 *         sent by the other user. - Performs the FreeMind actions corresponding
 *         to the commands sent.
 *  
 */
public class JabberListener {

    private static java.util.logging.Logger logger;

    MindMapController controller;

    LinkedList commandQueue;

    JabberSession session;

    public JabberListener(MindMapController c, MapSharingController sharingWizardController, String jabberServer, int port, String userName, String password) {
        controller = c;
        if (logger == null) {
            logger = controller.getController().getFrame().getLogger(this.getClass().getName());
        }
        commandQueue = new LinkedList();
        JabberContext context = new JabberContext(userName, password, jabberServer);
        Jabber jabber = new Jabber();
        session = jabber.createSession(context);
        try {
            session.connect(jabberServer, port);
            session.getUserService().login();
            logger.info("User logged in.\n");
            session.getPresenceService().setToAvailable("FreeMind Session", null, false);
            session.addMessageListener(new FreeMindJabberMessageListener(sharingWizardController));
        } catch (Exception ex) {
            freemind.main.Resources.getInstance().logException(ex);
            String message;
            if (ex.getClass().getName().compareTo("com.echomine.jabber.JabberMessageException") == 0) {
                JabberMessageException jabberMessageException = (JabberMessageException) ex;
                message = jabberMessageException.getErrorMessage();
            } else {
                message = ex.getClass().getName() + "\n\n" + ex.getMessage();
            }
            JFrame frame = new JFrame();
            JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * @return
     */
    public JabberSession getSession() {
        return session;
    }

    /**
     * 
     * @author RReppel
     * 
     * Listens to received Jabber messages and initiates the appropriate
     * FreeMind actions.
     *  
     */
    private class FreeMindJabberMessageListener implements JabberMessageListener {

        MapSharingController sharingWizardController;

        public FreeMindJabberMessageListener(MapSharingController sharingWizardController) {
            super();
            this.sharingWizardController = sharingWizardController;
        }

        public void messageReceived(JabberMessageEvent event) {
            if (event.getMessageType() != JabberCode.MSG_CHAT) return;
            JabberChatMessage latestMsg = (JabberChatMessage) event.getMessage();
            if (latestMsg.getType().equals(JabberChatMessage.TYPE_CHAT) || latestMsg.getType().equals(JabberChatMessage.TYPE_NORMAL)) {
                commandQueue.addLast(latestMsg);
                logger.info("Queue has " + commandQueue.size() + " items.");
                JabberChatMessage msg = (JabberChatMessage) commandQueue.removeFirst();
                String msgString = Tools.decompress(msg.getBody());
                if (logger.isLoggable(Level.INFO)) {
                    String displayMessage = ("Sending message:" + ((msgString.length() < 100) ? msgString : (msgString.substring(0, 50) + "..." + msgString.substring(msgString.length() - 50))));
                    logger.info("message " + displayMessage + " from " + msg.getFrom().getUsername() + " is reply required:" + msg.isReplyRequired());
                }
                XmlAction action = controller.unMarshall(msgString);
                if (action instanceof CollaborationAction) {
                    CollaborationAction xml = (CollaborationAction) action;
                    String cmd = xml.getCmd();
                    String username = xml.getUser();
                    try {
                        if (cmd.compareTo(JabberSender.REQUEST_MAP_SHARING) == 0) {
                            sharingWizardController.setMapSharingRequested(username, xml.getMap(), xml.getFilename());
                        } else if (cmd.compareTo(JabberSender.ACCEPT_MAP_SHARING) == 0) {
                            sharingWizardController.setMapShareRequestAccepted(username, true);
                        } else if (cmd.compareTo(JabberSender.DECLINE_MAP_SHARING) == 0) {
                            sharingWizardController.setMapShareRequestAccepted(username, false);
                        } else if (cmd.compareTo(JabberSender.STOP_MAP_SHARING) == 0) {
                            sharingWizardController.setSharingStopped(username);
                        } else {
                            logger.warning("Unknown command:" + cmd);
                        }
                    } catch (Exception e) {
                        freemind.main.Resources.getInstance().logException(e);
                    }
                } else if (action instanceof CompoundAction) {
                    CompoundAction pair = (CompoundAction) action;
                    if (pair.getListChoiceList().size() != 2) {
                        logger.warning("Cannot process the message " + msgString);
                        return;
                    }
                    executeRemoteCommand(pair);
                } else {
                    logger.warning("Unknown collaboration message:" + msgString);
                }
            }
        }

        /** Executes a command that was received via the jabber channel.
         * @param pair
         */
        private void executeRemoteCommand(CompoundAction pair) {
            XmlAction doAction = (XmlAction) pair.getListChoiceList().get(0);
            XmlAction undoAction = (XmlAction) pair.getListChoiceList().get(1);
            final ActionPair ePair = new ActionPair(doAction, undoAction);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    sharingWizardController.setSendingEnabled(false);
                    try {
                        sharingWizardController.getController().getActionFactory().executeAction(ePair);
                    } catch (Exception e) {
                        freemind.main.Resources.getInstance().logException(e);
                    }
                    sharingWizardController.setSendingEnabled(true);
                }
            });
        }
    }
}
