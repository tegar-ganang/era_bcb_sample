package org.rascalli.mbe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.model.URI;
import org.rascalli.framework.concurrent.Executor;
import org.rascalli.framework.config.ConfigService;
import org.rascalli.framework.core.Agent;
import org.rascalli.framework.core.AgentConfiguration;
import org.rascalli.framework.core.AgentManager;
import org.rascalli.framework.core.CommunicationChannel;
import org.rascalli.framework.core.User;
import org.rascalli.framework.eca.NebulaCommunicationProtocol;
import org.rascalli.framework.eca.UserConnected;
import org.rascalli.framework.eca.UserDisconnected;
import org.rascalli.framework.eca.UserPraise;
import org.rascalli.framework.eca.UserScolding;
import org.rascalli.framework.eca.UserUtterance;
import org.rascalli.framework.event.Event;
import org.rascalli.framework.event.EventListener;
import org.rascalli.framework.jabber.JabberClient;
import org.rascalli.framework.jabber.JabberException;
import org.rascalli.framework.jabber.JabberPresenceChanged;
import org.rascalli.framework.net.tcp.ClosedConnectionException;
import org.rascalli.framework.properties.UrlList;
import org.rascalli.framework.rss.FeedEntry;
import org.rascalli.framework.rss.RssManager;
import org.rascalli.mbe.adaptivemind.AMInputProcessorOutput;
import org.rascalli.webui.ws.RascalliWSImpl;

/**
 * <p>
 * This class is a compatibility wrapper for running Platform V1
 * Mind-Body-Environment style agents within the later versions of the platform.
 * It is currently used to translate between the old and new Nebula-Platform
 * communication protocols.
 * </p>
 * 
 * <p>
 * <b>Company:&nbsp;</b> SAT, Research Studios Austria
 * </p>
 * 
 * <p>
 * <b>Copyright:&nbsp;</b> (c) 2007
 * </p>
 * 
 * <p>
 * <b>last modified:</b><br/> $Author: christian $<br/> $Date: 2007-12-04
 * 19:22:17 +0100 (Di, 04 Dez 2007) $<br/> $Revision: 2447 $
 * </p>
 * 
 * @author Christian Schollum
 */
public class MBEAgentImpl implements MBEAgent {

    private final Log log = LogFactory.getLog(getClass());

    private final AtomicReference<NebulaCommunicationProtocol> protocolRef = new AtomicReference<NebulaCommunicationProtocol>();

    private final ActionDispatcherImpl actionDispatcher = new ActionDispatcherImpl();

    private final List<Object> components = new LinkedList<Object>();

    private final User user;

    private AgentConfiguration spec;

    private final Mind mind;

    private final Executor executor;

    private final ConfigService configService;

    private final InputProcessor inputProcessor;

    private boolean useJabber = false;

    private JabberClient jabberClient = null;

    private final DialogueHistoryImpl dialogueHistory = new DialogueHistoryImpl();

    private final SimpleDialogueManager simpleDialogueManager = new SimpleDialogueManager(this, actionDispatcher);

    private final RascalliWSImpl rascalliWS;

    private final List<EventListener> eventListeners = new ArrayList<EventListener>();

    private final Map<CommunicationChannel, AtomicBoolean> onlineStatus = new EnumMap<CommunicationChannel, AtomicBoolean>(CommunicationChannel.class);

    private final RssManager rssManager;

    private Event lastUserEvent;

    private CommunicationChannel lastChannel;

    private AtomicInteger agentUtteranceCounter = new AtomicInteger(0);

    private final String factoryId;

    private final AgentManager agentManager;

    private String lastQuestion;

    public MBEAgentImpl(User user, AgentConfiguration spec, Mind mind, InputProcessor inputProcessor, Executor executor, ConfigService configService, RascalliWSImpl rascalliWS, RssManager rssManager, AgentManager agentManager, String factoryId) {
        this.user = user;
        this.spec = spec;
        this.mind = mind;
        this.inputProcessor = inputProcessor;
        this.executor = executor;
        this.configService = configService;
        this.rascalliWS = rascalliWS;
        this.rssManager = rssManager;
        this.agentManager = agentManager;
        this.factoryId = factoryId;
        addEventListener(simpleDialogueManager);
        lastQuestion = "";
        onlineStatus.put(CommunicationChannel.ECA, new AtomicBoolean(false));
        onlineStatus.put(CommunicationChannel.JABBER, new AtomicBoolean(false));
    }

    private void addEventListener(EventListener eventListener) {
        eventListeners.add(eventListener);
    }

    public void enableJabber() {
        useJabber = true;
    }

    public Object getProperty(String key) {
        return spec.getAgentProperties().get(key);
    }

    public void start() {
        try {
            actionDispatcher.setExecutor(executor);
            actionDispatcher.setMind(mind);
            mind.setActionDispatcher(actionDispatcher);
            inject(mind, Agent.class, this);
            inject(mind, MBEAgent.class, this);
            inject(mind, ConfigService.class, configService);
            inject(mind, RascalliWSImpl.class, rascalliWS);
            if (mind instanceof EventListener) {
                addEventListener((EventListener) mind);
            }
            for (Object component : components) {
                if (component instanceof Effector) {
                    Effector effector = (Effector) component;
                    actionDispatcher.registerTool(effector);
                }
                if (component instanceof EventListener) {
                    addEventListener((EventListener) component);
                }
                inject(component, Agent.class, this);
                inject(component, MBEAgent.class, this);
                inject(component, ActionDispatcher.class, actionDispatcher);
                inject(component, Mind.class, mind);
                inject(component, DialogueHistory.class, dialogueHistory);
                inject(component, RascalliWSImpl.class, rascalliWS);
                inject(component, ConfigService.class, configService);
                inject(component, AgentManager.class, agentManager);
            }
            if (useJabber) {
                initJabber();
            }
            startComponent(mind);
            for (Object component : components) {
                startComponent(component);
            }
            rssManager.subscribe(this, ((UrlList) getProperty(P_AGENT_RSS_URLS)).asArray());
        } catch (Throwable t) {
            log.debug("error in init", t);
        }
    }

    private void startComponent(Object component) {
        Class<?> componentClass = component.getClass();
        log.debug("trying to start component " + componentClass.getName());
        try {
            Method setter = componentClass.getMethod("start", new Class[] {});
            log.debug("invoking start()");
            setter.invoke(component, new Object[] {});
            log.debug("after start()");
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("unexpected exception", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("unexpected exception", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("unexpected exception", e);
        }
    }

    private void stopComponent(Object component) {
        Class<?> componentClass = component.getClass();
        try {
            Method setter = componentClass.getMethod("stop", new Class[] {});
            setter.invoke(component, new Object[] {});
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("unexpected exception", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("unexpected exception", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("unexpected exception", e);
        }
    }

    private void initJabber() {
        if (JabberClient.isValidJabberId(user.getJabberId())) {
            if (log.isInfoEnabled()) {
                log.info("enabling jabber communication");
            }
            jabberClient = new JabberClient(this);
            try {
                jabberClient.connect();
            } catch (JabberException e) {
                if (log.isWarnEnabled()) {
                    log.warn("error setting up jabber client", e);
                }
                jabberClient = null;
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info("jabber communication not enabled: user has no or invalid jabber ID: " + user.getJabberId());
            }
        }
    }

    public void stop() {
        rssManager.unsubscribeAll(this);
        stopComponent(mind);
        for (Object component : components) {
            stopComponent(component);
        }
        if (jabberClient != null) {
            jabberClient.disconnect();
            jabberClient = null;
        }
    }

    /**
     * @param component
     * @param dependencyClass
     * @param actionDispatcher2
     */
    private void inject(Object component, Class<?> dependencyClass, Object dependency) {
        String dependencyClassName = dependencyClass.getSimpleName();
        String setterName = "set" + dependencyClassName;
        Class<?> componentClass = component.getClass();
        try {
            Method setter = componentClass.getMethod(setterName, new Class[] { dependencyClass });
            setter.invoke(component, new Object[] { dependency });
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("unexpected exception", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("unexpected exception", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("unexpected exception", e);
        }
    }

    public void addComponent(Object component) {
        if (component == null) {
            if (log.isErrorEnabled()) {
                log.error("trying to add null component");
            }
            return;
        }
        components.add(component);
    }

    public void handleEvent(Event event) {
        if (UserUtterance.class == event.getClass()) {
            if (((UserUtterance) event).getChannel() == CommunicationChannel.JABBER) {
                setUserIsOnline(CommunicationChannel.JABBER, true);
            }
            lastUserEvent = event;
            dispatchUserUtterance((UserUtterance) event);
            final String text = ((UserUtterance) event).getText();
            dialogueHistory.add(new DialogueAct(DialogueActType.USER_UTTERANCE, text));
        } else if (UserPraise.class == event.getClass()) {
            if (log.isDebugEnabled()) {
                log.debug("got praise event");
            }
            setUserIsOnline(((UserPraise) event).getChannel(), true);
            dispatchUserInputWithUtteranceClass(UtteranceClass.PRAISE);
            dialogueHistory.add(new DialogueAct(DialogueActType.USER_PRAISE));
        } else if (UserScolding.class == event.getClass()) {
            if (log.isDebugEnabled()) {
                log.debug("got scolding event");
            }
            setUserIsOnline(((UserScolding) event).getChannel(), true);
            dispatchUserInputWithUtteranceClass(UtteranceClass.SCOLDING);
            dialogueHistory.add(new DialogueAct(DialogueActType.USER_SCOLDING));
        } else if (UserConnected.class == event.getClass()) {
            setNebulaCommunicationProtocol(((UserConnected) event).getNebulaCommunicationProtocol());
            setUserIsOnline(CommunicationChannel.ECA, true);
            rascalliWS.setActiveRascalloIdForUser(getUser().getId(), getId());
        } else if (UserDisconnected.class == event.getClass()) {
            setUserIsOnline(CommunicationChannel.ECA, false);
            setNebulaCommunicationProtocol(null);
        } else if (JabberPresenceChanged.class == event.getClass()) {
            JabberPresenceChanged jpc = (JabberPresenceChanged) event;
            log.debug("JabberPresenceChanged: " + jpc.getJabberId() + " is " + (jpc.isOnline() ? "online" : "offline"));
            if (jpc.getJabberId().equals(user.getJabberId())) {
                final boolean isOnline = jpc.isOnline();
                setUserIsOnline(CommunicationChannel.JABBER, isOnline);
            }
        }
        for (EventListener eventListener : eventListeners) {
            eventListener.handleEvent(event);
        }
    }

    private void dispatchUserInputWithUtteranceClass(UtteranceClass utteranceClass) {
        try {
            final AMInputProcessorOutput userInput = new AMInputProcessorOutput(ToolID.IP, utteranceClass);
            if (useJabber) {
                final URI commChannel = userInput.toolHas("hasOutput", "CommunicationChannel");
                userInput.hasTextValue(commChannel, lastChannel.toString());
            }
            if (log.isDebugEnabled()) {
                log.debug("dispatching utterance class " + utteranceClass);
            }
            mind.processInput(userInput);
        } catch (RdfException e) {
            if (log.isErrorEnabled()) {
                log.error("cannot create user input for " + utteranceClass, e);
            }
        }
    }

    private void dispatchUserUtterance(UserUtterance userUtterance) {
        try {
            ToolOutput input = inputProcessor.processInput(userUtterance.getText(), getId(), getUser().getId());
            if (input != null) {
                if (useJabber) {
                    final URI commChannel = input.toolHas("hasOutput", "CommunicationChannel");
                    input.hasTextValue(commChannel, userUtterance.getChannel().toString());
                }
                mind.processInput(input);
            }
        } catch (RdfException e) {
            if (log.isErrorEnabled()) {
                log.error("cannot add channel information to user input", e);
            }
        }
    }

    public void sendMultimodalOutput(String sessionData, CommunicationChannel channel) throws CommunicationException {
        log.debug("sendMultimodalOutput(" + channel + ")");
        switch(channel) {
            case ECA:
                {
                    NebulaCommunicationProtocol protocol = protocolRef.get();
                    if (protocol == null) {
                        throw new CommunicationException("user not connected");
                    }
                    try {
                        protocol.sendMultimodalOutput(sessionData);
                    } catch (ClosedConnectionException e) {
                        setNebulaCommunicationProtocol(null);
                        throw new CommunicationException("user not connected");
                    }
                    break;
                }
            case JABBER:
                {
                    if (jabberClient == null) {
                        throw new CommunicationException("not connected to jabber");
                    }
                    if (!userIsOnline(CommunicationChannel.JABBER)) {
                        throw new CommunicationException("user not online");
                    }
                    try {
                        jabberClient.sendMessage(sessionData);
                    } catch (JabberException e) {
                        throw new CommunicationException("error sending jabber message", e);
                    }
                    break;
                }
        }
    }

    private void setNebulaCommunicationProtocol(NebulaCommunicationProtocol protocol) {
        protocolRef.set(protocol);
    }

    /**
     * Check whether the client is currently connected. This method should
     * always be called prior to an invocation of
     * {@link #sendMultimodalOutput(String, CommunicationChannel)}.
     * 
     * @return {@code true} if the client is currently connected, {@code false}
     *         otherwise.
     */
    public boolean isClientConnected() {
        return protocolRef.get() != null;
    }

    public int getId() {
        return spec.getAgentId();
    }

    public String getName() {
        return (String) getProperty(P_AGENT_NAME);
    }

    public User getUser() {
        return user;
    }

    public String getLastToolUsed() {
        return actionDispatcher.getLastToolUsed();
    }

    public DialogueHistory getDialogueHistory() {
        return dialogueHistory;
    }

    private void setUserIsOnline(CommunicationChannel channel, boolean value) {
        onlineStatus.get(channel).set(value);
        lastChannel = channel;
    }

    public boolean userIsOnline(CommunicationChannel channel) {
        return onlineStatus.get(channel).get();
    }

    public void feedEntryReceived(FeedEntry entry) {
        StringBuffer buf = new StringBuffer();
        buf.append("<entry>\n");
        buf.append("<feedId>").append(entry.getFeedUrl().id).append("</feedId>\n");
        buf.append("<feedUrl>").append(entry.getFeedUrl().url).append("</feedUrl>\n");
        buf.append("<h2>" + entry.getTitle() + "</h2>\n\n");
        buf.append("<a href=\"" + entry.getLink() + "\"/>\n");
        if (entry.getContents().isEmpty()) {
            buf.append("<p>" + entry.getDescription() + "</p>");
        } else {
            for (String content : entry.getContents()) {
                content = content.replaceAll("(\\s|\\n)+", " ");
                content = content.replaceAll("\\<.*?\\>", "");
                content = content.replaceAll("\\&\\#\\d+", " ");
                buf.append("<p>" + content + "</p>\n");
            }
        }
        buf.append("</entry>\n");
        String entryString = buf.toString();
        try {
            ToolOutput output = new ToolOutput(ToolID.RSS);
            final URI content = output.toolHas("hasOutput", "String");
            output.hasTextValue(content, entryString);
            mind.processInput(output);
        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error(ex);
            }
        }
    }

    public void update(AgentConfiguration newSpec) {
        if (log.isDebugEnabled()) {
            log.debug("agent " + getId() + " updated");
        }
        AgentConfiguration oldSpec = spec;
        spec = newSpec;
        UrlList oldUrls = (UrlList) oldSpec.getProperty(P_AGENT_RSS_URLS);
        UrlList newUrls = (UrlList) newSpec.getProperty(P_AGENT_RSS_URLS);
        UrlList deletedUrls = oldUrls.minus(newUrls);
        UrlList addedUrls = newUrls.minus(oldUrls);
        if (log.isDebugEnabled()) {
            if (deletedUrls.asList().size() > 0) {
                log.debug("deleted RSS feeds: " + deletedUrls);
            }
            if (addedUrls.asList().size() > 0) {
                log.debug("added RSS feeds: " + addedUrls);
            }
        }
        rssManager.unsubscribe(this, deletedUrls.asArray());
        rssManager.subscribe(this, addedUrls.asArray());
        if (log.isDebugEnabled()) {
            log.debug("agent update finished");
        }
    }

    public void setBusy(boolean value) {
        simpleDialogueManager.setBusy(value);
    }

    public int getAgentUtteranceCounter() {
        return agentUtteranceCounter.get();
    }

    public void incAgentUtteranceCounter() {
        agentUtteranceCounter.incrementAndGet();
    }

    public Event getLastUserEvent() {
        return lastUserEvent;
    }

    public String getAgentFactoryId() {
        return factoryId;
    }

    public AgentManager getAgentManager() {
        return agentManager;
    }

    public void setLastQuestion(String lastQuestion) {
        this.lastQuestion = lastQuestion;
    }

    public String getLastQuestion() {
        return lastQuestion;
    }
}
