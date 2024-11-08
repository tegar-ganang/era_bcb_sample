package org.aacc.asterisk;

import org.aacc.main.User;
import java.util.Observable;
import org.aacc.exceptions.*;
import org.aacc.campaigns.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.asteriskjava.manager.*;
import org.asteriskjava.manager.action.*;
import org.asteriskjava.manager.event.*;
import org.asteriskjava.manager.response.*;
import org.aacc.asterisk.userevents.*;
import org.aacc.toolbarserver.ToolbarServerRequestHandler;
import java.sql.*;
import java.util.Iterator;
import java.util.Observer;
import org.aacc.utils.comm.*;
import org.aacc.utils.xml.XMLHelper;
import org.aacc.main.MainClass;
import org.aacc.utils.TokenReplacer;

/**
 * Handles all communications between the client ant the asterisk-java layer
 * @author Fernando
 */
public class Asterisk implements Observer {

    /**
     * The maker of manager connections
     */
    private ManagerConnectionFactory managerFactory;

    /**
     * This connection is used to send agent actions to the manager
     */
    private List<ManagerConnection> agentManagerPool = Collections.synchronizedList(new ArrayList());

    /**
     * This connection is used to send other actions to the manager (mainly for dialing)
     */
    private List<ManagerConnection> dialerManagerPool = Collections.synchronizedList(new ArrayList());

    /**
     * Queues associated to this server
     */
    private Map<String, Queue> queues = Collections.synchronizedMap(new HashMap());

    /**
     * Persons collection, composed of those persons who are or have logged into the system
     */
    private Map<String, User> persons = Collections.synchronizedMap(new HashMap());

    /**
     * Agents collection. Composed of those agents who are or have logged into the system
     */
    private Map<String, Agent> agents = Collections.synchronizedMap(new HashMap());

    /**
     * Campaigns currently registered with the system
     */
    private Map<String, AbstractCampaign> campaigns = Collections.synchronizedMap(new HashMap());

    /**
     * Name or IP address of the asterisk host
     */
    private String asteriskHost;

    private String managerUser;

    private String managerPassword;

    /**
     * Is this host active?
     * @deprecated
     */
    private boolean active;

    /**
     * Creates a connection to an asterisk server. It also gets from a database the 
     * campaigns and the queues associated to this server. <br>
     * <B>NOTE</B> The campaign names <i>must</i> match their corresponding queue name
     * @param host
     * @param user
     * @param password
     * @throws IllegalStateException
     * @throws IOException
     * @throws AuthenticationFailedException
     * @throws TimeoutException
     * @see ManagerConnectionFactory
     */
    public Asterisk(String host, String user, String password) throws IllegalStateException, IOException, AuthenticationFailedException, TimeoutException {
        try {
            this.asteriskHost = host;
            this.managerUser = user;
            this.managerPassword = password;
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Logging into Asterisk host " + host + " with user " + user);
            new AsteriskEventsListener(this, host, user, password).run();
            managerFactory = new ManagerConnectionFactory(host, user, password);
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Creating a pool of agent manager connections: " + MainClass.getAgentMgrPoolSize());
            for (int i = 1; i <= MainClass.getAgentMgrPoolSize(); i++) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Creating a manager connection: " + i + "/" + MainClass.getAgentMgrPoolSize());
                ManagerConnection c = managerFactory.createManagerConnection();
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Logging into manager connection: " + i + "/" + MainClass.getAgentMgrPoolSize());
                c.registerUserEventClass(IVRChannelEvent.class);
                c.registerUserEventClass(NotifyAgentCallEvent.class);
                c.registerUserEventClass(NotifyCallDroppedEvent.class);
                c.login();
                this.agentManagerPool.add(c);
                Thread.sleep(150);
            }
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Creating a pool of dialer manager connections: " + MainClass.getDialerMgrPoolSize());
            for (int i = 1; i <= MainClass.getDialerMgrPoolSize(); i++) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Creating a manager connection: " + i + "/" + MainClass.getDialerMgrPoolSize());
                ManagerConnection c = managerFactory.createManagerConnection();
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Logging into manager connection: " + i + "/" + MainClass.getDialerMgrPoolSize());
                c.registerUserEventClass(IVRChannelEvent.class);
                c.registerUserEventClass(NotifyAgentCallEvent.class);
                c.registerUserEventClass(NotifyCallDroppedEvent.class);
                c.login();
                this.dialerManagerPool.add(c);
                Thread.sleep(150);
            }
            loadCampaigns();
        } catch (InterruptedException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Adds an agent into the campaign and into the campaign's queue
     * @param campaign The queue is taken from the campaign's properties
     * @param agent The agent that will be added to the campaign
     * @throws java.lang.IllegalArgumentException
     * @throws java.io.IOException
     * @throws org.asteriskjava.manager.TimeoutException
     * @throws java.lang.IllegalStateException
     */
    public void addAgentToCampaign(AgentCampaign campaign, Agent agent) throws IllegalArgumentException, IOException, TimeoutException, IllegalStateException {
        ToolbarServerRequestHandler padServerRequestHandler = agent.getPadServerRequestHandler();
        campaign.addAgent(agent);
        QueueAddAction queueAction = new QueueAddAction();
        queueAction.setInterface("Agent/" + agent.getAgentID());
        queueAction.setQueue(campaign.getQueue());
        ManagerResponse queueResponse = padServerRequestHandler.getManagerConnection().sendAction(queueAction);
    }

    /**
     * Disconnects from the Asterisk Manager
     * @deprecated Now we use a manager pool
     */
    public void disconnect() {
        try {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Disconnecting from manager");
        } catch (Exception ex) {
        }
    }

    /**
     * Is server active?
     * @return
     * @deprecated Don't think we'll use this...
     */
    public boolean getActive() {
        return active;
    }

    public void pauseAgentInAsterisk(Agent agent, boolean paused) throws IllegalArgumentException, IOException, IllegalStateException, TimeoutException {
        QueuePauseAction action = new QueuePauseAction();
        action.setInterface("Agent/" + agent.getUserName());
        action.setPaused(paused);
        ManagerResponse response = agent.getPadServerRequestHandler().getManagerConnection().sendAction(action);
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).fine(response.toString());
    }

    /**
     * Removes an agent from an asterisk queue.
     * @param campaign We'll obtain the queue from the campaign's definition
     * @param agent The agent that needs to be removed from the queue
     * @throws org.asteriskjava.manager.TimeoutException
     * @throws java.io.IOException
     * @throws java.lang.IllegalStateException
     * @throws java.lang.IllegalArgumentException
     */
    public void removeAgentFromCampaign(AgentCampaign campaign, Agent agent) throws TimeoutException, IOException, IllegalStateException, IllegalArgumentException {
        ToolbarServerRequestHandler padServerRequestHandler = agent.getPadServerRequestHandler();
        QueueRemoveAction queueAction = new QueueRemoveAction();
        queueAction.setInterface("Agent/" + agent.getAgentID());
        queueAction.setQueue(campaign.getQueue());
        ManagerResponse queueResponse = padServerRequestHandler.getManagerConnection().sendAction(queueAction);
        campaign.removeAgent(agent);
    }

    /**
     * Mark server as active
     * @param val
     * @deprecated Don't think we'll use this...
     */
    public void setActive(boolean val) {
        this.active = val;
    }

    /**
     * Get the name or IP address of the host (i.e. the asterisk server)
     * @return
     */
    public String getHost() {
        return asteriskHost;
    }

    /**
     * Gives the requestor a random manager agent connection from a pool of manager connections
     * @return
     */
    public ManagerConnection getOneAgentManagerConnection() {
        Random generator = new Random();
        ManagerConnection mgr = null;
        synchronized (agentManagerPool) {
            while (mgr == null) {
                int i = generator.nextInt(MainClass.getAgentMgrPoolSize());
                if (agentManagerPool.get(i).getState() == ManagerConnectionState.CONNECTED) {
                    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Selected agent manager connection number: " + i + "/" + MainClass.getAgentMgrPoolSize());
                    mgr = agentManagerPool.get(i);
                }
            }
        }
        return mgr;
    }

    /**
     * Gives the requestor a random manager agent connection from a pool of manager connections
     * @return
     */
    public ManagerConnection getOneDialerManagerConnection() {
        Random generator = new Random();
        ManagerConnection mgr = null;
        synchronized (dialerManagerPool) {
            while (mgr == null) {
                int i = generator.nextInt(MainClass.getDialerMgrPoolSize());
                if (dialerManagerPool.get(i).getState() == ManagerConnectionState.CONNECTED) {
                    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Selected dialer manager connection number: " + i + "/" + MainClass.getDialerMgrPoolSize());
                    mgr = dialerManagerPool.get(i);
                }
            }
        }
        return mgr;
    }

    /**
     * Place a call to origin, then bridge to destination
     * 
     * @param origin
     * @param destination
     */
    public ManagerResponse call(String destination, String queueContext, String queueExtension, String campaign) throws NoAnswerException {
        OriginateAction originateAction;
        ManagerResponse originateResponse = null;
        ManagerConnection mgr = getOneDialerManagerConnection();
        long start = Calendar.getInstance().getTime().getTime();
        String dialPrefix = getCampaigns().get(campaign).getDialPrefix();
        String dialContext = getCampaigns().get(campaign).getDialContext();
        long originateTimeout = getCampaigns().get(campaign).getDialTimeout();
        try {
            if (campaigns.get(campaign) != null) {
                campaigns.get(campaign).getStats().notifyCall();
            }
            originateAction = new OriginateAction();
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).fine("About to call " + destination);
            originateAction.setChannel("Local/" + dialPrefix + destination + "@" + dialContext);
            originateAction.setContext(queueContext);
            originateAction.setExten(queueExtension);
            originateAction.setPriority(1);
            originateAction.setTimeout((long) originateTimeout);
            originateAction.setCallerId(destination);
            originateAction.setAsync(false);
            originateAction.setVariable("__campaign", campaign);
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).finer("Manager action: " + originateAction);
            originateResponse = mgr.sendAction(originateAction, originateTimeout);
            if (campaigns.get(campaign) != null) {
                if (originateResponse.getResponse().equalsIgnoreCase("Success")) {
                    campaigns.get(campaign).getStats().notifyTryTime((Calendar.getInstance().getTime().getTime() - start) / 1000);
                } else {
                    campaigns.get(campaign).getStats().notifyNonAnsweredHangup((Calendar.getInstance().getTime().getTime() - start) / 1000);
                }
            }
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).fine("Manager response: " + originateResponse.getResponse());
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).finer("Manager response: " + originateResponse);
        } catch (IllegalStateException ex) {
            if (campaigns.get(campaign) != null) {
                campaigns.get(campaign).getStats().notifyNonAnsweredHangup((Calendar.getInstance().getTime().getTime() - start) / 1000);
            }
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (IOException ex) {
            if (campaigns.get(campaign) != null) {
                campaigns.get(campaign).getStats().notifyNonAnsweredHangup((Calendar.getInstance().getTime().getTime() - start) / 1000);
            }
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex);
        } catch (TimeoutException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Call attempt timed out");
            if (campaigns.get(campaign) != null) {
                campaigns.get(campaign).getStats().notifyNonAnsweredHangup((Calendar.getInstance().getTime().getTime() - start) / 1000);
            }
            throw new NoAnswerException(" ");
        }
        return originateResponse;
    }

    /**
     * Logs agent into asterisk
     * @param agent
     * @param password
     * @param station
     * @see AgentCallbackLoginAction
     * @see ManagerResponse
     * @see ToolbarServerRequestHandler#push
     * @see ToolbarServerRequestHandler#sendKVP
     * @see ToolbarServerRequestHandler#sendInformation
     */
    public Agent agentLogin(ToolbarServerRequestHandler padServerRequestHandler, String agent, String password, String extension) {
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).fine("Starting logging agent " + agent);
        Agent myAgent = getAgent(agent, true);
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).finer(myAgent.toString());
        try {
            if (!myAgent.getLoggedIn()) {
                if (myAgent.login(agent, password)) {
                    myAgent.setPadServerRequestHandler(padServerRequestHandler);
                    if (padServerRequestHandler.getManagerConnection() == null) {
                        padServerRequestHandler.setManagerConnection(this.getOneAgentManagerConnection());
                    }
                    ExtensionStateResponse extensionStateResponse = null;
                    ExtensionStateAction extensionStateAction = new ExtensionStateAction();
                    extensionStateAction.setExten(extension);
                    extensionStateAction.setContext(MainClass.getAgentsContext());
                    ManagerResponse response = padServerRequestHandler.getManagerConnection().sendAction(extensionStateAction);
                    if (response instanceof ManagerError) {
                        throw new AgentLoginException();
                    } else {
                        extensionStateResponse = (ExtensionStateResponse) response;
                    }
                    if (extensionStateResponse != null && extensionStateResponse.getResponse().equalsIgnoreCase("success") && extensionStateResponse.getStatus() != -1 && extensionStateResponse.getStatus() != 4) {
                        myAgent.setContext(MainClass.getAgentsContext());
                        myAgent.setExtension(extension);
                        myAgent.setDeviceType(extensionStateResponse.getHint().split("/")[0]);
                        AgentCallbackLoginAction agentLoginAction = new AgentCallbackLoginAction();
                        agentLoginAction.setAgent(agent);
                        agentLoginAction.setAckCall(false);
                        agentLoginAction.setContext(MainClass.getAgentsContext());
                        agentLoginAction.setExten(extension);
                        ManagerResponse loginResponse = padServerRequestHandler.getManagerConnection().sendAction(agentLoginAction);
                        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).finer(loginResponse.toString());
                        if (!loginResponse.getResponse().equalsIgnoreCase("error")) {
                            padServerRequestHandler.sendOK("Agent " + agent + " successful login");
                            padServerRequestHandler.push(new LoginMessage());
                            padServerRequestHandler.sendKVP("agentName", myAgent.getRealName());
                            padServerRequestHandler.sendKVP("userName", agent);
                            padServerRequestHandler.sendKVP("campaign", "n/a");
                            padServerRequestHandler.sendKVP("speech", "Welcome!");
                            Iterator it = campaigns.entrySet().iterator();
                            while (it.hasNext()) {
                                Map.Entry pairs = (Map.Entry) it.next();
                                if (pairs.getValue().getClass() == AgentCampaign.class) {
                                    AgentCampaign campaign = (AgentCampaign) pairs.getValue();
                                    if (myAgent.getCampaignRole(campaign).equals("agent") && !campaign.getQueue().isEmpty()) {
                                        addAgentToCampaign(campaign, myAgent);
                                    }
                                }
                            }
                        } else {
                            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Unauthorized agent " + agent);
                            padServerRequestHandler.sendUnauthorized(loginResponse.getMessage());
                        }
                    } else {
                        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Agent " + agent + " tried to log in inexistent extension " + extension);
                        padServerRequestHandler.sendNotAcceptable("Extension " + extension + ((extensionStateResponse.getStatus() == -1) ? " is invalid" : " has not been registered"));
                        myAgent.logout();
                    }
                } else {
                    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Unauthorized agent " + agent);
                    padServerRequestHandler.sendUnauthorized("User or password incorrect");
                }
            } else {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Agent " + agent + " already logged in.");
                padServerRequestHandler.sendConflict("You are already logged in.");
            }
        } catch (AgentLoginException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Unauthorized agent " + agent);
            padServerRequestHandler.sendUnauthorized("User or password incorrect");
        } catch (SQLException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (IOException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (TimeoutException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (IllegalStateException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        }
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).fine("Finished logging agent " + agent);
        return getAgent(agent, false);
    }

    /**
     * Logs agent out of asterisk
     * @param agent The id of the agent to be logged off
     * @param force Logoff, even if agent isn't logged into the system (maybe user 
     * is still logged in asterisk...)
     * @see AgentLogoffAction
     * @see ManagerResponse
     * @see ToolbarServerRequestHandler#push
     */
    public void agentLogout(String agent, boolean forced) {
        try {
            Agent myAgent = this.getAgent(agent, forced);
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).fine("Will logout " + agent);
            if (myAgent.getLoggedIn()) {
                myAgent.logout();
                AgentLogoffAction action = new AgentLogoffAction();
                action.setAgent(agent);
                action.setSoft(false);
                ManagerResponse response = myAgent.getPadServerRequestHandler().getManagerConnection().sendAction(action);
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).finer(response.toString());
                myAgent.getPadServerRequestHandler().sendOK("Agent " + agent + " successful logoff");
                myAgent.getPadServerRequestHandler().push(new LogoffMessage());
                Iterator it = campaigns.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry) it.next();
                    if (pairs.getValue().getClass() == AgentCampaign.class) {
                        removeAgentFromCampaign((AgentCampaign) pairs.getValue(), myAgent);
                    }
                }
                agents.remove(myAgent.getAgentID());
            } else {
                myAgent.getPadServerRequestHandler().sendInformation("Agent " + agent + " was not logged in");
                myAgent.getPadServerRequestHandler().sendOK("Agent logoff");
                myAgent.getPadServerRequestHandler().push(new LogoffMessage());
            }
        } catch (IOException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (TimeoutException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (IllegalStateException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        }
    }

    /**
     * Logs out an agent that is already logged into the system and into asterisk
     * @param agent The agent id to log off the system
     * @see #agentLogout(String,boolean)
     */
    public void agentLogout(String agent) {
        agentLogout(agent, false);
    }

    /**
     * Requests asterisk to pause or unpause an agent
     * @see agentPause(String,boolean,boolean)
     * @param agent The agent that will be (un)paused
     * @param paused Set paused if true, or unpause if false
     */
    public void agentPause(String agent, String reason, boolean paused) {
        agentPause(agent, reason, paused, false);
    }

    /**
     * Requests asterisk to pause or unpause an agent
     * @see agentPause(String,boolean,boolean)
     * @param agent The agent that will be (un)paused
     * @param paused Set paused if true, or unpause if false
     * @param force Pay no attention to agent being paused or unpaused already (used mainly by login, in order not to get an error that could be displayed to the client)
     */
    public void agentPause(String agent, String reason, boolean paused, boolean force) {
        try {
            Agent myAgent = this.getAgent(agent, false);
            if (force || (myAgent.isPaused() != paused)) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info((paused ? "Pausing" : "Unpausing") + " agent " + agent);
                myAgent.setPaused(paused);
                pauseAgentInAsterisk(myAgent, paused);
                myAgent.getPadServerRequestHandler().sendOK("Agent " + agent + " " + (paused ? "paused" : "unpaused"));
                myAgent.getPadServerRequestHandler().push(paused ? new BreakMessage(Integer.parseInt(reason)) : new UnbreakMessage());
                if (!paused) {
                    myAgent.setPaused(false);
                } else {
                    myAgent.setPauseReason(reason);
                }
            } else if (force) {
                myAgent.getPadServerRequestHandler().sendConflict("You are alredy in " + (paused ? "paused" : "ready") + "state");
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Agent " + agent + " was paused already.");
            }
        } catch (IOException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (TimeoutException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (IllegalStateException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        }
    }

    public void agentACW(String agent, boolean acw) {
        try {
            Agent myAgent = this.getAgent(agent, false);
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("ACW: " + (acw ? "on" : "off") + " for agent " + agent);
            myAgent.setAcw(acw);
            pauseAgentInAsterisk(myAgent, acw);
            myAgent.getPadServerRequestHandler().sendOK("Agent " + agent + " " + (acw ? "now in acw" : "now out of acw"));
            myAgent.getPadServerRequestHandler().push(new AfterCallWorkMessage(acw ? AfterCallWorkMessage.ACW_ON : AfterCallWorkMessage.ACW_OFF));
        } catch (IOException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (TimeoutException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (IllegalStateException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        }
    }

    public void agentManualMode(String agent, boolean manual) {
        try {
            Agent myAgent = this.getAgent(agent, false);
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Manual mode: " + (manual ? "on" : "off") + " for agent " + agent);
            myAgent.setManual(manual);
            pauseAgentInAsterisk(myAgent, manual);
            myAgent.getPadServerRequestHandler().sendOK("Agent " + agent + " " + (manual ? "in manual mode" : "out of manual mode"));
            myAgent.getPadServerRequestHandler().push(new ManualModeMessage(manual ? ManualModeMessage.MANUAL_ON : ManualModeMessage.MANUAL_OFF));
        } catch (IOException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (TimeoutException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        } catch (IllegalStateException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        }
    }

    /**
     * Pushes a message to the client informing that a call has arrived. The call command also will include the ANI
     * Some additional KVP's might be included along with the command
     * @param event The call event
     * @see AgentCalledEvent
     * @see ToolbarServerRequestHandler#push
     * @see ToolbarServerRequestHandler#sendKVP
     */
    public void notifyCallToAgent(Call call) {
        try {
            String agentName = "";
            String callId = "n/a";
            callId = call.getANI();
            Agent agent = call.getAgent();
            if (agent != null) {
                agent.setInCall(true);
                call.setTalkStart();
                CallMessage callMessage = new CallMessage(callId);
                if (call.getContact() != null) {
                    callMessage.setXml(call.getContact().xml());
                }
                agent.getPadServerRequestHandler().push(callMessage);
            }
        } catch (Exception ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).warning("notifyCallToAgent: " + ex.toString());
        }
    }

    /**
     * Sends campaign information to the agent
     * @param call
     */
    public void notifyCampaignToAgent(Call call) {
        try {
            String agentName = call.getAgent().getAgentID();
            String campaignName = call.getCampaign().getName();
            Agent agent = getAgent(agentName);
            if (agent != null) {
                agent.setInCall(campaignName);
                AgentCampaign campaign = (AgentCampaign) (getCampaigns().get(campaignName));
                agent.getPadServerRequestHandler().sendKVP("campaign", campaignName);
                TokenReplacer replacer = new TokenReplacer(call);
                agent.getPadServerRequestHandler().sendKVP("speech", replacer.replace(campaign.getScript()));
                String URL = campaign.getPopUpURL();
                if (URL != null && !URL.isEmpty()) {
                    agent.getPadServerRequestHandler().push(new PopupMessage(replacer.replace(URL)));
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).warning("notifyCampaignToAgent: " + ex.toString());
        }
    }

    /**
     * Tells an IVR campaign when a new channel has been created for it and
     * when a channel is released.
     * <p>
     * This helps IVR campaigns keep count of how many channels are being use at
     * any given time, to keep statistics and to be used by the dialing algorithm
     * @param event
     */
    public void NotifyIVRChannel(IVRChannelEvent event) {
        if (campaigns.containsKey(event.getCampaign()) && campaigns.get(event.getCampaign()).getClass() == IVRCampaign.class) {
            IVRCampaign campaign = (IVRCampaign) campaigns.get(event.getCampaign());
            if (campaign != null) {
                campaign.setIvrChannels(Integer.parseInt(event.getCount()));
                if (event.getStage().equalsIgnoreCase("join")) {
                    campaign.getStats().notifyAnsweredCall();
                } else if (event.getStage().equalsIgnoreCase("leave")) {
                    campaign.getStats().notifyAnsweredHangup(Long.valueOf(event.getDuration()));
                }
            }
        }
    }

    /**
     * Notifies the client that the call has been terminated
     * @param event The event received
     * @see AgentCompleteEvent
     * @see Agent
     * @see ToolbarServerRequestHandler#push
     */
    public void notifyHangupToAgent(ManagerEvent event) {
        String agentName;
        long talkTime = 0;
        Agent agent = null;
        Call call = null;
        try {
            if (event instanceof AgentCompleteEvent) {
                agentName = ((AgentCompleteEvent) event).getMemberName().split("/")[1];
                talkTime = ((AgentCompleteEvent) event).getTalkTime();
                agent = getAgent(agentName);
                call = agent.getCurrentCall();
            } else {
                NotifyAgentEndEvent evt = (NotifyAgentEndEvent) event;
                call = Call.get(evt.getUniqueId());
                agent = call.getAgent();
            }
            call.setTalkEnd();
            talkTime = call.getTalkEnd().getTime() - call.getTalkStart().getTime();
            if (agent != null) {
                agent.getPadServerRequestHandler().push(new HangupMessage());
                if (agent.getPauseRequested() != -1) {
                    agentPause(agent.getAgentID(), Integer.toString(agent.getPauseRequested()), true, true);
                }
                AbstractCampaign camp = campaigns.get(agent.getRealtime().getCampaign());
                if (camp != null) {
                    try {
                        camp.getStats().notifyAnsweredHangup(talkTime);
                    } catch (Exception ex) {
                        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).warning("notifyHangupToAgent: " + ex.toString());
                    }
                }
                agent.setInCall(false);
            }
        } catch (Exception ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).warning("notifyHangupToAgent: " + ex.toString());
        }
    }

    /**
     * Gets an agent object from the agents collection (if it exists)
     * @param agentKey The agent name
     * @return null if agent not found
     * @see #getAgent(String,boolean)
     */
    public Agent getAgent(String agentKey) {
        return getAgent(agentKey, false);
    }

    /**
     * Gets an agent object from the agents collection. If it doesn't exist,
     * optionally adds the agent to the collection.
     * @param agentKey The agent name
     * @param addToCollection Adds the agent to the collection, if set to true
     * @return null if agent not found
     * @see #getAgent
     */
    private Agent getAgent(String agentKey, boolean addToCollection) {
        if (agents.containsKey(agentKey)) {
            return agents.get(agentKey);
        } else if (addToCollection) {
            Agent a = new Agent(this);
            a.setAgentID(agentKey);
            a.setAsterisk(this);
            synchronized (agents) {
                agents.put(agentKey, a);
            }
            return agents.get(agentKey);
        } else {
            return null;
        }
    }

    /**
     * Returns a list of all available campaigns for this server
     * @return
     */
    public Map<String, AbstractCampaign> getCampaigns() {
        return campaigns;
    }

    /**
     * Loads campaigns from the database
     */
    private void loadCampaigns() {
        if (campaigns.isEmpty()) {
            try {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Loading campaigns");
                java.sql.Statement stmt;
                java.sql.ResultSet rs;
                String qry = String.format("SELECT id, type FROM campaigns WHERE active = 1");
                stmt = MainClass.getDbConnection().createStatement();
                rs = stmt.executeQuery(qry);
                rsToCampaigns(rs);
            } catch (SQLException ex) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
            }
        }
    }

    /**
     * Given a result set from CAMPAIGNS table, it will iterate it and create
     * (or update) campaigns
     * @param rs - A result set from CAMPAIGNS table
     */
    public void rsToCampaigns(ResultSet rs) {
        try {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (rs.getByte("type") == AbstractCampaign.TYPE_IVR) {
                    IVRCampaign ivrCampaign = new IVRCampaign(this, id);
                    ivrCampaign.get();
                    campaigns.put(ivrCampaign.getName(), ivrCampaign);
                } else {
                    AgentCampaign agentCampaign = new AgentCampaign(this, id);
                    agentCampaign.get();
                    campaigns.put(agentCampaign.getName(), agentCampaign);
                    if (agentCampaign.getQueue() != null && !agentCampaign.getQueue().isEmpty()) {
                        Queue q = new Queue();
                        q.setName(agentCampaign.getQueue());
                        q.setCampaign(agentCampaign);
                        queues.put(q.getName(), q);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(ex.toString());
        }
    }

    public Map<String, Queue> getQueues() {
        return queues;
    }

    public void setQueues(Map<String, Queue> queues) {
        this.queues = queues;
    }

    /**
     * Handle events that come from the Asterisk manager
     * @param event
     */
    private void handleManagerEvents(Object event) {
        if (event instanceof NewChannelEvent) {
            NewChannelEvent newChannelEvent = (NewChannelEvent) event;
            if (newChannelEvent.getChannelState() == 4 && Call.get(newChannelEvent.getUniqueId()) == null) {
                Call.put(newChannelEvent.getUniqueId(), new Call(newChannelEvent));
            }
        } else if (event instanceof LinkEvent) {
            LinkEvent linkEvent = (LinkEvent) event;
            Call call = Call.get(linkEvent.getUniqueId1());
            if (call != null) {
                call.setCalledChannel(linkEvent.getChannel2());
            }
        } else if ((event instanceof HoldEvent)) {
            System.out.println(event);
        } else if (event instanceof HangupEvent) {
            Call call = Call.get(((HangupEvent) event).getUniqueId());
            if (call != null) {
                call.notifyFinish();
            }
        } else if (event instanceof AgentLoginEvent) {
        } else if (event instanceof AgentLogoffEvent) {
        } else if (event instanceof AgentRingNoAnswerEvent) {
        } else if (event instanceof AgentConnectEvent) {
            AgentConnectEvent evt = (AgentConnectEvent) event;
            String agentName = evt.getMember().split("/")[1];
            Call call = Call.get(evt.getUniqueId());
            Agent agent = getAgent(agentName);
            call.setAgent(agent);
            call.setCampaign(getQueues().get(evt.getQueue()).getCampaign());
            call.setTalkStart();
            agent.setCurrentCall(call);
            notifyCallToAgent(call);
            notifyCampaignToAgent(call);
        } else if (event instanceof AgentCompleteEvent) {
            AgentCompleteEvent evt = (AgentCompleteEvent) event;
            Call call = Call.get(evt.getUniqueId());
            call.setTalkEnd();
            notifyHangupToAgent(evt);
        } else if ((event instanceof QueueMemberStatusEvent) && ((QueueMemberStatusEvent) event).getStatus() == QueueMemberStatusEvent.AST_DEVICE_BUSY) {
        } else if ((event instanceof QueueMemberStatusEvent) && ((QueueMemberStatusEvent) event).getStatus() == QueueMemberStatusEvent.AST_DEVICE_NOT_INUSE) {
        } else if (event instanceof JoinEvent) {
            JoinEvent evt = (JoinEvent) event;
            Call call = Call.get(evt.getUniqueId());
            call.setQPosition(evt.getPosition());
            if (call.getCampaign() != null) {
                call.getCampaign().getStats().notifyAnsweredCall();
            }
        } else if (event instanceof QueueCallerAbandonEvent) {
            QueueCallerAbandonEvent evt = (QueueCallerAbandonEvent) event;
            Call call = Call.get(evt.getUniqueId());
            call.setQTime(evt.getHoldTime());
            if (call.getCampaign() != null) {
                call.getCampaign().getStats().notifyDrop(evt.getHoldTime());
            }
        } else if (event instanceof NotifyCallDroppedEvent) {
            NotifyCallDroppedEvent notifyCallDroppedEvent = (NotifyCallDroppedEvent) event;
            if (getCampaigns().containsKey(notifyCallDroppedEvent.getCampaign())) {
                getCampaigns().get(notifyCallDroppedEvent.getCampaign()).getStats().notifyDrop(Long.valueOf(notifyCallDroppedEvent.getDuration()));
            }
        } else if (event instanceof IVRChannelEvent) {
            IVRChannelEvent evt = (IVRChannelEvent) event;
            if (getCampaigns().containsKey(evt.getCampaign())) {
                NotifyIVRChannel(evt);
                Call.get(evt.getUniqueId()).setCampaign(getCampaigns().get(evt.getCampaign()));
            }
        } else if (event instanceof NotifyAgentCallEvent) {
            NotifyAgentCallEvent evt = (NotifyAgentCallEvent) event;
            System.out.println(evt);
            if (getCampaigns().containsKey(evt.getCampaign())) {
                Call call = Call.get(evt.getUniqueId());
                call.setCampaign(getCampaigns().get(evt.getCampaign()));
                call.setAgent(getAgent(evt.getAgent()));
                call.setTalkStart();
                notifyCallToAgent(call);
                notifyCampaignToAgent(call);
            }
        } else if (event instanceof NotifyAgentEndEvent) {
            System.out.println((NotifyAgentEndEvent) event);
            NotifyAgentEndEvent evt = (NotifyAgentEndEvent) event;
            Call call = Call.get(evt.getUniqueId());
            call.setTalkEnd();
            notifyHangupToAgent(evt);
        } else if (event instanceof InboundCallEvent) {
            InboundCallEvent evt = (InboundCallEvent) event;
            Call call = Call.get(evt.getUniqueId());
            call.setCallType(Call.TYPE_INBOUND);
            call.setDNIS(evt.getDNIS());
        } else if (event instanceof OutboundCallEvent) {
            OutboundCallEvent evt = (OutboundCallEvent) event;
            Call call = Call.get(evt.getUniqueId());
            call.setCallType(Call.TYPE_OUTBOUND);
        } else if (event instanceof ConsultationCallEvent) {
            ConsultationCallEvent evt = (ConsultationCallEvent) event;
            Call call = Call.get(evt.getUniqueId());
            call.setCallType(Call.TYPE_CONSULTATION);
        }
    }

    /**
     * Gets events from observed objects.
     * 
     * @param o
     * @param event
     */
    public void update(Observable o, Object event) {
        if (o instanceof AsteriskEventsListener) {
            handleManagerEvents(event);
        }
    }
}
