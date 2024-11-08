package org.rascalli.framework.jabber;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket.ItemStatus;
import org.jivesoftware.smack.util.StringUtils;
import org.rascalli.framework.core.Agent;
import org.rascalli.framework.core.AgentConfiguration;
import org.rascalli.framework.core.CommunicationChannel;
import org.rascalli.framework.core.Gender;
import org.rascalli.framework.core.User;
import org.rascalli.framework.eca.UserPraise;
import org.rascalli.framework.eca.UserScolding;
import org.rascalli.framework.eca.UserUtterance;
import org.rascalli.framework.event.Event;
import org.rascalli.framework.rss.FeedEntry;

/**
 * <p>
 * JabberClient is a connector to the Jabber instant messaging network. It
 * permits the opening of a chat connection to a user, and the sending and
 * receiving of messages. Also, the online status of a user can be tracked.
 * </p>
 * 
 * <p>
 * Note: The Agent which is given as an argument to the constructor must contain
 * the following properties:
 * 
 * <ul>
 * <li>{@link #P_JABBER_SERVER}</li>
 * <li>{@link #P_JABBER_ID}</li>
 * <li>{@link #P_JABBER_PASSWORD}</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Usage:
 * 
 * <pre>
 * jc.connect();
 * jc.chatWith(&quot;jabber-id&quot;);
 * </pre>
 * 
 * </p>
 */
public class JabberClient implements RosterListener {

    public static final String SYSTEM_PROPERTY_PLATFORM_ID = "org.rascalli.platform.id";

    public static final String SYSTEM_PROPERTY_JABBER_SERVER = "org.rascalli.framework.jabber.server";

    public static final String SYSTEM_PROPERTY_JABBER_SERVER_DEFAULT_VALUE = "www2.ofai.at";

    public static final String P_JABBER_SERVER = "jabber.server";

    public static final String P_JABBER_ID = "jabber.id";

    public static final String P_JABBER_PASSWORD = "jabber.password";

    public static final String P_USER_JABBER_ID = "user.jabber.id";

    private static final String AGENT_JABBER_PASSWORD = "Cv2VhFQDtyrL";

    private final Log log = LogFactory.getLog(getClass());

    private XMPPConnection connection;

    private Chat chat;

    boolean userAvailable;

    private final Agent agent;

    private UserSubscriptionChecker userSubscriptionChecker;

    public JabberClient(Agent agent) {
        this.agent = agent;
    }

    public void connect() throws JabberException {
        String jabberServer = System.getProperty(SYSTEM_PROPERTY_JABBER_SERVER, SYSTEM_PROPERTY_JABBER_SERVER_DEFAULT_VALUE);
        final String agentJabberId = getAgentJabberId(agent);
        if (log.isDebugEnabled()) {
            log.debug("Connecting to jabber as " + agentJabberId);
        }
        try {
            try {
                login(jabberServer, agentJabberId, AGENT_JABBER_PASSWORD);
            } catch (XMPPException e) {
                if (log.isInfoEnabled()) {
                    log.info("jabber login failed for agent jabber id " + agentJabberId + ": trying to create account");
                }
                createAccount(jabberServer, agentJabberId);
                login(jabberServer, agentJabberId, AGENT_JABBER_PASSWORD);
            }
        } catch (XMPPException e) {
            throw new JabberException(e);
        }
    }

    /**
     * @param jabberServer
     * @param agentJabberId
     * @throws JabberException
     */
    private void createAccount(String jabberServer, final String agentJabberId) throws JabberException {
        JabberAccountManager jabberAccountManager = new JabberAccountManager();
        final boolean accountCreated = jabberAccountManager.registerAccount(jabberServer, agentJabberId, AGENT_JABBER_PASSWORD);
        if (!accountCreated) {
            throw new JabberException("Cannot create jabber account: " + agentJabberId);
        }
    }

    /**
     * @param jabberServer
     * @param agentJabberId
     * @param agentJabberPassword
     * @return
     * @throws XMPPException
     * @throws XMPPException
     */
    private void login(final String server, final String id, final String password) throws XMPPException {
        connection = new XMPPConnection(server);
        try {
            connection.connect();
            connection.login(id, password);
            final Roster roster = connection.getRoster();
            roster.addRosterListener(this);
            String userJabberId = agent.getUser().getJabberId();
            checkUserSubscription(roster, userJabberId);
            presenceChanged(roster.getPresence(userJabberId));
            chat = connection.getChatManager().createChat(userJabberId, new ChatMessageListener());
        } catch (XMPPException e) {
            if (connection.isConnected()) {
                connection.disconnect();
            }
            connection = null;
            throw e;
        }
    }

    /**
     * @param roster
     * @param userJabberId
     */
    private void checkUserSubscription(Roster roster, String userJabberId) {
        userSubscriptionChecker = new UserSubscriptionChecker(roster, userJabberId);
        userSubscriptionChecker.execute();
    }

    /**
     * @param agent
     * @return
     * @throws JabberException
     */
    private String getAgentJabberId(Agent agent) throws JabberException {
        String platformId = System.getProperty(SYSTEM_PROPERTY_PLATFORM_ID);
        if (platformId == null) {
            throw new JabberException(("system property '" + SYSTEM_PROPERTY_PLATFORM_ID + "' not set"));
        }
        return "rascalli-" + platformId + "-" + agent.getId();
    }

    public void sendMessage(String message) throws JabberException {
        try {
            chat.sendMessage(message);
        } catch (XMPPException e) {
            throw new JabberException(e);
        }
    }

    public void disconnect() {
        userSubscriptionChecker.stop();
        connection.disconnect();
    }

    public String getRoster() {
        StringBuffer s = new StringBuffer();
        if ((connection != null) && connection.isConnected()) {
            Iterator<?> iter = connection.getRoster().getEntries().iterator();
            while (iter.hasNext()) {
                RosterEntry entry = (RosterEntry) iter.next();
                s.append(entry.getName() + " (" + entry.getUser() + ")" + "\n");
            }
        }
        return s.toString();
    }

    public void entriesAdded(Collection<String> arg0) {
        log.debug("added " + arg0.toString());
    }

    public void entriesDeleted(Collection<String> arg0) {
        log.debug("deleted " + arg0.toString());
    }

    public void entriesUpdated(Collection<String> arg0) {
        log.debug("Update for " + arg0.toString());
        for (String str : arg0) {
            log.debug("  " + str);
        }
    }

    public void presenceChanged(Presence p) {
        log.debug("presence changed " + p.getFrom());
        final String jabberId = StringUtils.parseBareAddress(p.getFrom());
        if (p.isAvailable()) {
            agent.handleEvent(new JabberPresenceChanged(jabberId, JabberPresenceChanged.Status.ONLINE));
        } else {
            agent.handleEvent(new JabberPresenceChanged(jabberId, JabberPresenceChanged.Status.OFFLINE));
        }
    }

    private class ChatMessageListener implements MessageListener {

        public void processMessage(Chat chat, Message message) {
            final String utterance = message.getBody();
            if (log.isDebugEnabled()) {
                log.debug("received jabber user input: " + utterance);
            }
            if (null != utterance) {
                if (utterance.equals("p")) {
                    agent.handleEvent(new UserPraise(CommunicationChannel.JABBER));
                } else if (utterance.equals("s")) {
                    if (log.isDebugEnabled()) {
                        log.debug("registered scolding event through jabber");
                    }
                    agent.handleEvent(new UserScolding(CommunicationChannel.JABBER));
                } else {
                    agent.handleEvent(new UserUtterance(CommunicationChannel.JABBER, utterance));
                }
            }
        }
    }

    /**
     * @param jabberId
     * @return
     */
    public static boolean isValidJabberId(String jabberId) {
        if (jabberId == null) {
            return false;
        }
        if (jabberId.length() == 0) {
            return false;
        }
        if (StringUtils.parseName(jabberId).length() == 0) {
            return false;
        }
        if (StringUtils.parseServer(jabberId).length() == 0) {
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws JabberException {
        Agent agent = new Agent() {

            public int getId() {
                return 6;
            }

            public String getName() {
                return null;
            }

            public Object getProperty(String key) {
                return null;
            }

            public User getUser() {
                return new User(1, "Micky", Gender.MALE, "cbs@jabber.ccc.de");
            }

            public void start() {
            }

            public void stop() {
            }

            public void update(AgentConfiguration newSpec) {
            }

            public void handleEvent(Event event) {
            }

            public void feedEntryReceived(FeedEntry entry) {
            }

            public String getAgentFactoryId() {
                return null;
            }
        };
        System.setProperty("org.rascalli.platform.id", "jabbertest");
        JabberClient jc = new JabberClient(agent);
        jc.connect();
    }

    private class UserSubscriptionChecker implements Runnable {

        private boolean subscribed = false;

        private final Roster roster;

        private final String userJabberId;

        private ScheduledExecutorService scheduler;

        /**
         * @param roster
         * @param userJabberId
         */
        public UserSubscriptionChecker(Roster roster, String userJabberId) {
            this.roster = roster;
            this.userJabberId = userJabberId;
        }

        /**
         * 
         */
        public void execute() {
            checkSubscription();
            if (!subscribed) {
                scheduler = Executors.newScheduledThreadPool(1);
                scheduler.scheduleWithFixedDelay(this, 1, 1, TimeUnit.MINUTES);
            }
        }

        public void run() {
            checkSubscription();
            if (subscribed) {
                stop();
            }
        }

        public void stop() {
            if (scheduler != null) {
                scheduler.shutdown();
            }
        }

        public void checkSubscription() {
            try {
                final RosterEntry entry = roster.getEntry(userJabberId);
                if (entry != null) {
                    final ItemStatus status = entry.getStatus();
                    if (log.isDebugEnabled()) {
                        log.debug("subscription status for " + userJabberId + ": " + status);
                    }
                    if (status == ItemStatus.SUBSCRIPTION_PENDING) {
                        roster.removeEntry(entry);
                    } else {
                        subscribed = true;
                    }
                }
                if (!roster.contains(userJabberId)) {
                    roster.createEntry(userJabberId, userJabberId, null);
                    if (log.isDebugEnabled()) {
                        log.debug("added user " + userJabberId + " to roster");
                    }
                }
            } catch (XMPPException e) {
                if (log.isWarnEnabled()) {
                    log.warn("user subscription check failed", e);
                }
            }
        }
    }
}
