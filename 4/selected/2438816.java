package org.coos.messaging.routing;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.coos.messaging.COOS;
import org.coos.messaging.ConnectingException;
import org.coos.messaging.Link;
import org.coos.messaging.Message;
import org.coos.messaging.Processor;
import org.coos.messaging.ProcessorException;
import org.coos.messaging.ProcessorInterruptException;
import org.coos.messaging.impl.DefaultProcessor;
import org.coos.messaging.util.Log;
import org.coos.messaging.util.LogFactory;
import org.coos.messaging.util.URIHelper;
import org.coos.messaging.util.UuidGenerator;
import org.coos.messaging.util.UuidHelper;

/**
 * The DefaultRouter (actually default point to point router) contains the point
 * to point routing mechanisms
 *
 * @author Knut Eilif Husa, Tellu AS
 */
public class DefaultRouter extends DefaultProcessor implements Router, HashMapCallback {

    private static final String LINK_TIMEOUT_DEFAULT = "60000";

    public static final String LINK_TIMEOUT = "linkTimeout";

    private Collection<RouterProcessor> preProcessors = new ConcurrentLinkedQueue<RouterProcessor>();

    private Collection<RouterProcessor> postProcessors = new ConcurrentLinkedQueue<RouterProcessor>();

    private Map<String, TimedConcurrentHashMap<String, Link>> routingTables = new ConcurrentHashMap<String, TimedConcurrentHashMap<String, Link>>();

    private Map<String, String> aliasTable = new ConcurrentHashMap<String, String>();

    private Map<String, Link> links = new ConcurrentHashMap<String, Link>();

    private Map<String, RoutingAlgorithm> routingAlgorithms = new ConcurrentHashMap<String, RoutingAlgorithm>();

    private Map<String, RouterSegment> segmentMapping = new ConcurrentHashMap<String, RouterSegment>();

    private Collection<String> routerUuids = new ConcurrentLinkedQueue<String>();

    private Collection<String> QoSClasses = new ConcurrentLinkedQueue<String>();

    private String defaultQoSClass;

    private String COOSInstanceName;

    private boolean running = false;

    private boolean enabled = true;

    @SuppressWarnings("unused")
    private boolean loggingEnabled = false;

    private Link defaultGw = null;

    private final Log logger = LogFactory.getLog(this.getClass(), false);

    private COOS COOS;

    private long linkTimeout;

    public DefaultRouter(String routerUuid) {
        COOSInstanceName = routerUuid;
        new LinkStateAlgorithm(this, routerUuid);
        addQoSClass(Link.DEFAULT_QOS_CLASS, true);
        addSegmentMapping(UuidHelper.getSegmentFromSegmentOrEndpointUuid(routerUuid), routerUuid, "linkstate");
    }

    public DefaultRouter() {
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void removeRouterUuid(String routerUuid) {
        routerUuids.remove(routerUuid);
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
        for (RoutingAlgorithm routingAlgorithm : routingAlgorithms.values()) {
            routingAlgorithm.setLoggingEnabled(loggingEnabled);
        }
    }

    @SuppressWarnings("unchecked")
    public void processMessage(Message msg) {
        for (RouterProcessor routerProcessor : preProcessors) {
            try {
                routerProcessor.processMessage(msg);
            } catch (ProcessorInterruptException e) {
                return;
            } catch (ProcessorException e) {
                logger.error("ProcessorException caught.", e);
            } catch (Exception e) {
                logger.error("Unknown Exception caught.", e);
            }
        }
        if (msg.getReceiverEndpointUri() == null) {
            logger.warn("Message from " + msg.getSenderEndpointUri() + " missing receiver address");
            replyErrorReason(msg, Message.ERROR_NO_RECEIVER);
            return;
        }
        String qosClass = msg.getHeader(Message.QOS_CLASS);
        if (qosClass == null) {
            qosClass = defaultQoSClass;
        }
        Map<String, Link> routingTable = routingTables.get(qosClass);
        String hops = msg.getHeader(Message.HOPS);
        if (hops == null) {
            hops = "1";
        } else {
            hops = String.valueOf(Integer.parseInt(hops) + 1);
        }
        msg.setHeader(Message.HOPS, hops);
        if (Integer.parseInt(hops) > 244) {
            logger.warn("Message from " + msg.getSenderEndpointUri() + ", to: " + msg.getReceiverEndpointUri() + " too many hops");
            return;
        }
        String uuid = resolveAlias(msg);
        if (uuid == null) {
            logger.warn(Message.ERROR_NO_ALIAS + ":" + msg.getReceiverEndpointUri());
            replyErrorReason(msg, Message.ERROR_NO_ALIAS + ":" + msg.getReceiverEndpointUri());
            return;
        }
        uuid = UuidHelper.getQualifiedUuid(uuid);
        Link link;
        if (routerUuids.contains(uuid)) {
            logger.putMDC(UUID_PREFIX, uuid);
            if (msg.getType().equals(Message.TYPE_ROUTING_INFO)) {
                routingAlgorithms.get(UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(uuid)).processRoutingInfo(msg);
            } else if (msg.getType().equals(Message.TYPE_ALIAS)) {
                try {
                    setLinkAliases((Vector) msg.getBody(), msg.getMessageContext().getInBoundChannel().getOutLink());
                } catch (ProcessorException e) {
                    logger.error("ProcessorException caught while stopping endpoint.", e);
                }
            }
        } else {
            link = route(uuid, msg, routingTable);
            if (link != null) {
                msg.getMessageContext().setNextLink(link);
            }
            for (RouterProcessor routerProcessor : postProcessors) {
                try {
                    routerProcessor.processMessage(msg);
                } catch (ProcessorInterruptException e) {
                    return;
                } catch (ProcessorException e) {
                    logger.error("ProcessorException caught.", e);
                } catch (Exception e) {
                    logger.error("Unknown Exception caught.", e);
                }
            }
            if (link != null) {
                if (msg.getHeader(Message.TRACE_ROUTE) != null) {
                    String trace = msg.getHeader(Message.TRACE);
                    if (trace == null) {
                        trace = COOSInstanceName;
                    }
                    msg.setHeader(Message.TRACE, trace + " -> " + link.getDestinationUuid());
                }
                try {
                    link.processMessage(msg);
                } catch (ProcessorException e) {
                    replyErrorReason(msg, e.getMessage());
                }
            } else {
                String errorMsg;
                URIHelper helper = new URIHelper(msg.getReceiverEndpointUri());
                String alias = helper.getEndpoint();
                if (uuid.equals("null")) {
                    errorMsg = COOSInstanceName + ": No uuid for alias " + alias + ", No route from " + COOSInstanceName;
                } else {
                    if (uuid.equals(alias)) {
                        errorMsg = COOSInstanceName + ": No route to: " + alias + " from " + COOSInstanceName;
                    } else {
                        errorMsg = COOSInstanceName + ": No route to: " + alias + " / " + uuid + " from " + COOSInstanceName;
                    }
                }
                logger.warn(errorMsg);
                replyErrorReason(msg, Message.ERROR_NO_ROUTE);
            }
        }
    }

    /**
     * This method dynamically registers and unregisters aliases to link
     *
     * @param regAliases
     *            - An vector containing all aliases to be registered
     * @param outlink
     *            - The link to register the aliases with. This link is always
     *            directed towards an endpoint
     * @throws ProcessorException
     */
    @SuppressWarnings("unchecked")
    public void setLinkAliases(Vector regAliases, Link outlink) throws ProcessorException {
        String segment = UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(outlink.getDestinationUuid());
        if (!segment.equals(".")) {
            segment += ".";
        }
        for (int i = 0; i < regAliases.size(); i++) {
            String alias = (String) regAliases.get(i);
            if (!alias.startsWith(Router.DICO_SEGMENT + ".") && !alias.startsWith(Router.LOCAL_SEGMENT + ".") && !alias.startsWith(segment)) {
                String qualifiedAlias = segment + alias;
                regAliases.remove(alias);
                regAliases.add(qualifiedAlias);
            }
        }
        Vector<String> curAliases = outlink.getAlises();
        Iterator itCurAliases = curAliases.iterator();
        while (itCurAliases.hasNext()) {
            String alias = (String) itCurAliases.next();
            if (!regAliases.contains(alias)) {
                itCurAliases.remove();
                removeAlias(alias);
            }
        }
        Iterator itRegAliases = regAliases.iterator();
        while (itRegAliases.hasNext()) {
            String alias = (String) itRegAliases.next();
            outlink.addAlias(alias);
            String oldToUuid = aliasTable.get(alias);
            if ((oldToUuid != null) && !oldToUuid.equals(outlink.getDestinationUuid())) {
                Iterator itRegAliases2 = regAliases.iterator();
                while (itRegAliases2.hasNext()) {
                    removeAlias((String) itRegAliases2.next());
                }
                throw new ProcessorException("Can not register alias:" + alias + " since this alias is occupied for endpoint with uuid " + outlink.getDestinationUuid());
            }
            putAlias(alias, outlink.getDestinationUuid());
        }
    }

    /**
     * This method resolves receiver URIs into uuids. It handles both URIs
     * containing aliases and uuids
     *
     * @param msg
     *            the message to resolve alias for
     * @return the uuid
     */
    public String resolveAlias(Message msg) {
        URIHelper helper = new URIHelper(msg.getReceiverEndpointUri());
        String uuid;
        String alias;
        alias = helper.getEndpoint();
        String segment;
        if ((alias != null) && !helper.isEndpointUuid()) {
            if (!helper.isEndpointQualified()) {
                if ((msg.getMessageContext().getInBoundLink() != null) && (msg.getMessageContext().getInBoundLink().getDestinationUuid() != null)) {
                    segment = UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(msg.getMessageContext().getInBoundLink().getDestinationUuid());
                } else {
                    segment = ".";
                }
                if (!segment.equals(".")) {
                    segment += ".";
                }
                String qualifiedAlias = segment + alias;
                uuid = aliasTable.get(qualifiedAlias);
                if (uuid == null) {
                    qualifiedAlias = DICO_SEGMENT + "." + alias;
                    uuid = aliasTable.get(qualifiedAlias);
                }
            } else {
                uuid = aliasTable.get(alias);
            }
            if ((uuid == null) && !helper.getSegment().equals(Router.LOCAL_SEGMENT) && !helper.getSegment().equals(Router.DICO_SEGMENT)) {
                uuid = alias;
            }
        } else {
            uuid = alias;
        }
        return uuid;
    }

    /**
     * This is the core of the routing algorithm
     *
     */
    @SuppressWarnings("unchecked")
    public Link route(String uuid, Message msg, Map<String, Link> routingTable) {
        URIHelper helper = new URIHelper(msg.getSenderEndpointUri());
        if (msg.getMessageContext().getInBoundChannel() != null) {
            String destUuid = msg.getMessageContext().getInBoundLink().getDestinationUuid();
            String curSegment = UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(destUuid);
            String senderEndpointUuid = helper.getEndpoint();
            String senderSegment = UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(senderEndpointUuid);
            if (senderSegment.equals(curSegment)) {
                if (!routingTable.containsKey(senderEndpointUuid)) {
                    ((TimedConcurrentHashMap) routingTable).put(senderEndpointUuid, msg.getMessageContext().getInBoundChannel().getOutLink(), linkTimeout, this);
                }
            } else {
                if (!routingTable.containsKey(senderSegment)) {
                    ((TimedConcurrentHashMap) routingTable).put(senderSegment, msg.getMessageContext().getInBoundChannel().getOutLink(), linkTimeout, this);
                }
            }
        }
        Link link = routingTable.get(uuid);
        if (link == null) {
            String toSegment = UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(uuid);
            Link inboundLink = msg.getMessageContext().getInBoundLink();
            if (inboundLink == null) {
                return null;
            }
            String destUuid = inboundLink.getDestinationUuid();
            if (destUuid == null) {
                logger.warn(COOSInstanceName + ":destinationUuid is null on incoming link : " + inboundLink.getLinkId());
            }
            String curSegment = UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(destUuid);
            if (!toSegment.equals(curSegment)) {
                while (!toSegment.equals("")) {
                    link = routingTable.get(toSegment);
                    if (link != null) {
                        break;
                    } else {
                        toSegment = UuidHelper.getParentSegment(toSegment);
                    }
                }
            }
        }
        if (link == null) {
            if ((defaultGw != null) && (defaultGw != msg.getMessageContext().getInBoundChannel().getOutLink())) {
                link = defaultGw;
            } else {
                if (defaultGw == null) {
                    logger.warn("Defaultgw is not set!");
                } else if (defaultGw == msg.getMessageContext().getInBoundChannel().getOutLink()) {
                    logger.warn("Incoming link is defaultgw!");
                }
            }
        }
        return link;
    }

    public void replyErrorReason(Message msg, String message) {
        if (msg.getHeader(Message.TYPE).equals(Message.TYPE_MSG)) {
            msg.setReceiverEndpointUri(msg.getSenderEndpointUri());
            msg.setHeader(Message.TYPE, Message.TYPE_ERROR);
            msg.setHeader(Message.ERROR_REASON, message);
            msg.setHeader(Message.ERROR_CODE, "504");
            processMessage(msg);
        }
    }

    public Processor getDefaultProcessor() {
        return this;
    }

    /**
     * Adding a link to the router. Can either be a link to an endpoint/router
     * or a link to another segment
     *
     * @param routerUuid
     * @param link
     * @throws ConnectingException
     */
    public void addLink(String routerUuid, Link link) throws ConnectingException {
        if (UuidHelper.isUuid(routerUuid)) {
            String seg = UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(routerUuid);
            RoutingAlgorithm algorithm = routingAlgorithms.get(seg);
            if (algorithm == null) {
                throw new ConnectingException("Router is not attached to segment: " + seg);
            }
            RouterSegment rs = segmentMapping.get(seg);
            rs.setTimestamp(0);
            link.setDestinationUuid(routerUuid);
            links.put(link.getLinkId(), link);
            if ((link.getChannel() != null) && link.getChannel().isDefaultGw()) {
                defaultGw = link;
                logger.debug("Setting defaultgw " + link);
            }
            algorithm.publishLink(link);
        } else {
            link.setDestinationUuid(routerUuid);
            links.put(link.getLinkId(), link);
            routingAlgorithms.get(routerUuid).publishLink(link);
        }
        logger.debug(getCOOSInstanceName() + ": Adding link: " + link);
    }

    public Link getLink(String destinationUuid) {
        if (destinationUuid != null) {
            for (Link link : links.values()) {
                if (link.getDestinationUuid().equals(destinationUuid)) {
                    return link;
                }
            }
        }
        return null;
    }

    public void removeLinkById(String linkId) {
        Link link = links.get(linkId);
        if (link != null) {
            publishAndCleanUpAfterLinkRemoval(link);
        }
    }

    public void removeLink(String destinationUuid) {
        for (Link link : links.values()) {
            if (link.getDestinationUuid().equals(destinationUuid)) {
                publishAndCleanUpAfterLinkRemoval(link);
            }
        }
    }

    private void publishAndCleanUpAfterLinkRemoval(Link link) {
        link.setCost(LinkCost.MAX_VALUE);
        routingAlgorithms.get(UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(link.getDestinationUuid())).publishLink(link);
        for (Map<String, Link> routingTable : routingTables.values()) {
            for (String uuidKey : routingTable.keySet()) {
                Link l = routingTable.get(uuidKey);
                if ((l != null) && l.equals((link))) {
                    routingTable.remove(uuidKey);
                    for (String aliasKey : aliasTable.keySet()) {
                        String uuid = aliasTable.get(aliasKey);
                        if (uuidKey.equals(uuid)) {
                            removeAlias(aliasKey);
                        }
                    }
                }
            }
            if (COOS != null) COOS.removeChannel(link.getDestinationUuid());
        }
        for (String aliasKey : aliasTable.keySet()) {
            String uuid = aliasTable.get(aliasKey);
            if (link.getDestinationUuid().equals(uuid)) {
                removeAlias(aliasKey);
            }
        }
        if ((link.getChannel() != null) && !link.getChannel().isDefaultGw()) {
            links.remove(link.getLinkId());
        }
    }

    public void addQoSClass(String QoSClass, boolean isDefaultQoSClass) {
        this.QoSClasses.add(QoSClass);
        if ((defaultQoSClass == null) || isDefaultQoSClass) {
            defaultQoSClass = QoSClass;
        }
        if (routingTables.get(QoSClass) == null) {
            routingTables.put(QoSClass, new TimedConcurrentHashMap<String, Link>());
        }
    }

    public Collection<String> getQoSClasses() {
        return this.QoSClasses;
    }

    public void addPreProcessor(RouterProcessor preProcessor) {
        preProcessor.setRouter(this);
        preProcessors.add(preProcessor);
    }

    public void addPostProcessor(RouterProcessor postProcessor) {
        postProcessor.setRouter(this);
        postProcessors.add(postProcessor);
    }

    public String getCOOSInstanceName() {
        return this.COOSInstanceName;
    }

    public void setCOOSInstanceName(String COOSInstanceName) {
        this.COOSInstanceName = COOSInstanceName;
    }

    public void setCOOS(COOS coos) {
        this.COOSInstanceName = coos.getName();
        this.COOS = coos;
    }

    public COOS getCOOS() {
        return COOS;
    }

    public void setSegmentMappings(Hashtable<String, RouterSegment> segmentMapping) {
        this.segmentMapping = new ConcurrentHashMap<String, RouterSegment>(segmentMapping);
    }

    public void addSegmentMapping(String segment, String routerUUID, String routerAlgorithm) {
        this.segmentMapping.put(segment, new RouterSegment(segment, routerUUID, routerAlgorithm, false));
    }

    public synchronized void addRoutingAlgorithm(String routerUuid, RoutingAlgorithm routingAlgorithm) {
        if (!UuidHelper.isRouterUuid(routerUuid)) {
            throw new IllegalArgumentException("Router uuid must start with prefix " + ROUTER_UUID_PREFIX);
        }
        Processor processor = null;
        this.routingAlgorithms.put(UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(routerUuid), routingAlgorithm);
        routerUuids.add(routerUuid);
        if (routerUuids.size() > 1) {
            for (String uuid : routerUuids) {
                if (!routerUuid.equals(uuid)) {
                    Link link = new Link(0);
                    link.addFilterProcessor(processor);
                    link.setChainedProcessor(this);
                    try {
                        addLink(UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(uuid), link);
                    } catch (Exception e) {
                        logger.error("Unknown Exception caught.", e);
                    }
                    link = new Link(0);
                    link.addFilterProcessor(processor);
                    link.setChainedProcessor(this);
                    try {
                        addLink(UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(routerUuid), link);
                    } catch (Exception e) {
                        logger.error("Unknown Exception caught.", e);
                    }
                }
            }
        }
        if (running) {
            try {
                routingAlgorithm.start();
            } catch (Exception e) {
                logger.error("Unknown Exception caught.", e);
            }
        }
    }

    /**
     * Takes care of updating
     */
    @SuppressWarnings("unchecked")
    private void aliasesUpdated() {
        for (Link link : links.values()) {
            Collection aliases = link.getAlises();
            synchronized (aliases) {
                for (Iterator iterator = aliases.iterator(); iterator.hasNext(); ) {
                    String alias = (String) iterator.next();
                    if (alias.startsWith(Router.DICO_SEGMENT + ".") && !aliasTable.containsKey(alias)) {
                        iterator.remove();
                    }
                }
            }
        }
        for (String alias : aliasTable.keySet()) {
            if (alias.startsWith(Router.DICO_SEGMENT + ".")) {
                String aliasUuid = aliasTable.get(alias);
                String aliasSegment = UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(aliasUuid);
                for (Link link : links.values()) {
                    String segment = UuidHelper.getSegmentFromSegmentOrEndpointUuid(link.getDestinationUuid());
                    if (aliasSegment.equals(segment) && UuidHelper.isSegment(link.getDestinationUuid())) {
                        link.addAlias(alias);
                    }
                }
            }
        }
    }

    public String getDefaultQoSClass() {
        return defaultQoSClass;
    }

    public RoutingAlgorithm getRoutingAlgorithm(String segment) {
        return routingAlgorithms.get(segment);
    }

    public Map<String, TimedConcurrentHashMap<String, Link>> getRoutingTables() {
        return routingTables;
    }

    public Map<String, Link> getRoutingTable(String qos) {
        return routingTables.get(qos);
    }

    public Map<String, Link> getLinks() {
        return links;
    }

    public Link getLinkById(String id) {
        return links.get(id);
    }

    public void start() throws Exception {
        String linkTimeoutStr = getProperty(LINK_TIMEOUT, LINK_TIMEOUT_DEFAULT);
        linkTimeout = 0;
        if (linkTimeoutStr != null) {
            linkTimeout = Integer.parseInt(linkTimeoutStr);
        }
        for (TimedConcurrentHashMap routingTable : routingTables.values()) {
            routingTable.start();
        }
        if (!running && enabled) {
            for (RoutingAlgorithm routingAlgorithm : routingAlgorithms.values()) {
                routingAlgorithm.start();
            }
            running = true;
        }
    }

    @SuppressWarnings("unchecked")
    public void stop() throws Exception {
        for (RoutingAlgorithm routingAlgorithm : routingAlgorithms.values()) {
            routingAlgorithm.stop();
        }
        for (TimedConcurrentHashMap routingTable : routingTables.values()) {
            routingTable.stop();
        }
        running = false;
    }

    public String toString() {
        return "Router " + COOSInstanceName;
    }

    public Map<String, String> getAliasTable() {
        return aliasTable;
    }

    public void putAlias(String alias, String toUuid) {
        String oldToUuid = aliasTable.get(alias);
        if ((oldToUuid != null) && !oldToUuid.equals(toUuid)) {
            logger.warn("Possible alias conflict for alias: " + alias + ". Was pointing to : " + oldToUuid + ". Now pointing to :" + toUuid + ".");
        }
        aliasTable.put(alias, toUuid);
        aliasesUpdated();
    }

    public void removeAlias(String alias) {
        aliasTable.remove(alias);
        aliasesUpdated();
    }

    public void addDynamicSegment(String segmentName, String routingAlg) throws ConnectingException {
        RoutingAlgorithm prototype = COOS.getRoutingAlgorithm(routingAlg);
        if (prototype == null) {
            throw new ConnectingException("Routingalgorithm: " + routingAlg + " not defined. Refusing dynamic segment allocation: " + segmentName);
        }
        RoutingAlgorithm algorithm = prototype.copy();
        UuidGenerator uuidGenerator = new UuidGenerator(ROUTER_UUID_PREFIX);
        String routerUuid = segmentName + "." + uuidGenerator.generateSanitizedId();
        segmentMapping.put(segmentName, new RouterSegment(segmentName, routerUuid, routingAlg, false, true));
        algorithm.init(routerUuid, this);
    }

    public RouterSegment getSegment(String segmentName) {
        return segmentMapping.get(segmentName);
    }

    public Map<String, RouterSegment> getSegmentMap() {
        return segmentMapping;
    }

    public void removeSegment(String segmentName) {
        segmentMapping.remove(segmentName);
        routingAlgorithms.remove(segmentName);
        removeLink(segmentName);
    }

    public Link getDefaultGw() {
        return defaultGw;
    }

    public boolean remove(Object key, TimedConcurrentHashMap<?, ?> routingTable) {
        Link link = (Link) routingTable.get(key);
        if ((link != null) && (link.getChannel() != null) && link.getChannel().isDefaultGw() && link.getDestinationUuid().equals(key)) {
            return false;
        }
        for (String alias : aliasTable.keySet()) {
            if (aliasTable.get(alias).equals(key)) {
                aliasTable.remove(alias);
            }
        }
        return true;
    }
}
