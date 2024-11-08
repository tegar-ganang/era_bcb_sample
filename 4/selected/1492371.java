package org.coos.messaging.routing;

import org.coos.messaging.Link;
import org.coos.messaging.Message;
import org.coos.messaging.impl.DefaultMessage;
import org.coos.messaging.util.Log;
import org.coos.messaging.util.UuidHelper;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * @author Knut Eilif Husa, Tellu AS
 *
 */
public class LinkStateAlgorithm extends DefaultRoutingAlgorithm implements TopologyMapListener {

    public static final String ALG_NAME = "linkstate";

    public static final String REFRESH_INTERVAL = "refreshInterval";

    public static final String AGING_FACTOR = "agingFactor";

    public static final String EMPTY_SEG_FACTOR = "emptyDynamicSegmentFactor";

    private final Random r = new Random();

    private TopologyMap topologyMap;

    private Timer timer;

    private int refreshInterval = 100;

    private int agingFactor = 5;

    private long emptyDynamicSegmentFactor = 2 * agingFactor;

    public LinkStateAlgorithm() {
    }

    public LinkStateAlgorithm(Router router, String routerUuid) {
        init(routerUuid, router);
    }

    @Override
    public void init(String routerUuid, Router router) {
        String refIntvStr = properties.get(REFRESH_INTERVAL);
        if (refIntvStr != null) {
            refreshInterval = Integer.parseInt(refIntvStr);
        }
        String agefactStr = properties.get(AGING_FACTOR);
        if (agefactStr != null) {
            agingFactor = Integer.parseInt(agefactStr);
        }
        String emptySegfactStr = properties.get(EMPTY_SEG_FACTOR);
        if (emptySegfactStr != null) {
            emptyDynamicSegmentFactor = Integer.parseInt(emptySegfactStr);
        }
        topologyMap = new TopologyMap(routerUuid, refreshInterval, agingFactor * refreshInterval);
        topologyMap.addListener(this);
        topologyMap.start();
        super.init(routerUuid, router);
    }

    public TopologyMap getTopologyMap() {
        return topologyMap;
    }

    public void setTopologyMap(TopologyMap topologyMap) {
        this.topologyMap = topologyMap;
    }

    public void publishLink(Link link) {
        List<Link> links = new LinkedList<Link>();
        links.add(link);
        broadcastRoutingInfo(links);
    }

    @SuppressWarnings("unchecked")
    public synchronized void processRoutingInfo(Message routingInfo) {
        Vector<LinkCost> linkCosts = (Vector<LinkCost>) routingInfo.getBody();
        String s = "";
        for (int i = 0; i < linkCosts.size(); i++) {
            LinkCost linkCost = linkCosts.elementAt(i);
            s += linkCost.getFromUuid() + "<->" + linkCost.getToUuid() + ": " + linkCost.getCost(Link.DEFAULT_QOS_CLASS) + ": " + linkCost.getAliases() + ", ";
        }
        logger.trace("Receiving on " + router.getCOOSInstanceName() + ", from " + routingInfo.getSenderEndpointUri() + " linkinfo: " + s);
        topologyMap.update(linkCosts);
    }

    private void calculateOptimalPaths() {
        QoSClasses = router.getQoSClasses();
        for (String qos : QoSClasses) {
            calculateOptimalPaths(qos);
        }
    }

    void calculateOptimalPaths(String qos) {
        logger.debug(this.router.getCOOSInstanceName() + ": Calculating optimal paths for: " + topologyMap.getRouterUuid() + " QoS: " + qos);
        Map<String, LinkCost> optimalPath = new HashMap<String, LinkCost>();
        Set<String> uuids = topologyMap.getNodeUuids();
        uuids.remove(topologyMap.getRouterUuid());
        Iterator<String> iter = uuids.iterator();
        while (iter.hasNext()) {
            String uuid = iter.next();
            optimalPath.put(uuid, new LinkCost(topologyMap.getLinkCost(uuid)));
        }
        while (!uuids.isEmpty()) {
            LinkCost minimalCost = null;
            iter = optimalPath.keySet().iterator();
            while (iter.hasNext()) {
                String uuid = iter.next();
                if (uuids.contains(uuid)) {
                    if ((minimalCost == null) || ((optimalPath.get(uuid)).getCost(qos) < minimalCost.getCost(qos))) {
                        minimalCost = optimalPath.get(uuid);
                    }
                }
            }
            String minimalCostUuid = minimalCost.getToUuid();
            uuids.remove(minimalCostUuid);
            iter = uuids.iterator();
            while (iter.hasNext()) {
                String nodeUuid = iter.next();
                if (topologyMap.isNeighbourNode(minimalCostUuid, nodeUuid)) {
                    int candidateCost = minimalCost.getCost(qos) + topologyMap.getLinkCost(minimalCostUuid, nodeUuid).getCost(qos);
                    int currentCost;
                    if (optimalPath.get(nodeUuid) != null) {
                        currentCost = (optimalPath.get(nodeUuid)).getCost(qos);
                    } else {
                        currentCost = topologyMap.getLinkCost(nodeUuid).getCost(qos);
                    }
                    if (candidateCost < currentCost) {
                        LinkCost linkCost = optimalPath.get(nodeUuid);
                        linkCost.setCost(qos, candidateCost);
                        linkCost.setNextLinkCost(optimalPath.get(minimalCostUuid));
                    }
                }
            }
        }
        Iterator<LinkCost> valIter = optimalPath.values().iterator();
        while (valIter.hasNext()) {
            LinkCost linkCost = valIter.next();
            String toUuid = linkCost.getToUuid();
            while (linkCost.getNextLink() != null) {
                linkCost = linkCost.getNextLink();
            }
            Link l = links.get(linkCost.getLinkId());
            if (linkCost.getCost(qos) < LinkCost.MAX_VALUE) {
                if (l != null) {
                    routingTables.get(qos).put(toUuid, links.get(linkCost.getLinkId()));
                    for (String alias : topologyMap.getAliases(toUuid)) {
                        router.putAlias(alias, toUuid);
                    }
                }
            } else {
                routingTables.get(qos).remove(toUuid);
                for (String alias : topologyMap.getAliases(toUuid)) {
                    router.removeAlias(alias);
                }
                if ((l != null) && (l.getChannel() != null) && !l.getChannel().isDefaultGw()) {
                    links.remove(linkCost.getLinkId());
                    if (loggingEnabled) {
                        logger.debug(routerUuid + " removing from routerTable Link to: " + linkCost.getToUuid());
                    }
                }
            }
        }
        if (loggingEnabled) {
            printRoutingTable(routerUuid, qos, routingTables.get(qos), logger);
            printAliasTable(routerUuid, aliasTable, logger);
            printOptimalPath(routerUuid, qos, optimalPath, logger);
        }
    }

    private static synchronized void printOptimalPath(String routerUuid, String qos, Map<String, LinkCost> optimalPath, Log logger) {
        StringWriter writer = new StringWriter();
        writer.write("-------------optimal paths for Qos: " + qos + " in router: " + routerUuid + "------------\n");
        Iterator<String> keys = optimalPath.keySet().iterator();
        while (keys.hasNext()) {
            String uuid = keys.next();
            writer.write("'" + uuid + "': ");
            LinkCost linkCost = optimalPath.get(uuid);
            while (linkCost != null) {
                writer.write("'" + linkCost.getFromUuid() + "', '" + linkCost.getToUuid() + "': " + linkCost.getCost(qos));
                linkCost = linkCost.getNextLink();
                if (linkCost != null) {
                    writer.write(" --> ");
                }
            }
            writer.write("\n");
        }
        writer.write("-------------------------\n");
        logger.debug(writer.toString());
    }

    public void notifyChanged(TopologyMap topologyMap) {
        try {
            calculateOptimalPaths();
        } catch (Exception e) {
            logger.warn("Exception occured in " + topologyMap.getRouterUuid(), e);
        }
        if (topologyMap.isEmpty() && router.getSegment(segment).isDynamicSegment()) {
            RouterSegment rs = router.getSegment(segment);
            long now = System.currentTimeMillis();
            if (rs.getTimestamp() == 0) {
                rs.setTimestamp(now);
            } else if ((rs.getTimestamp() + (emptyDynamicSegmentFactor * refreshInterval)) > now) {
                router.removeSegment(segment);
                for (TimedConcurrentHashMap<String, Link> routingTable : routingTables.values()) {
                    routingTable.remove(segment);
                }
                stop();
                logger.info("Removing dynamic segment: " + segment);
            }
        }
    }

    public void start() {
        timer = new Timer("LinkStateThread", true);
        broadcastRoutingInfoAndSchedule();
    }

    private void broadcastRoutingInfoAndSchedule() {
        broadcastRoutingInfo(links.values());
        try {
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    broadcastRoutingInfoAndSchedule();
                }
            }, refreshInterval + r.nextInt((int) (0.1 * refreshInterval)));
        } catch (IllegalStateException e) {
        }
    }

    private void broadcastRoutingInfo(Collection<Link> links) {
        try {
            String s = "";
            for (Link link : links) {
                s += link.getDestinationUuid() + ": " + link.getAlises() + ", ";
            }
            Set<String> uuids = new HashSet<String>();
            for (Map<String, Link> routingTable : routingTables.values()) {
                for (String uuid : routingTable.keySet()) {
                    if ((routingTable.get(uuid) != null && routingTable.get(uuid).getChannel() != null) && !routingTable.get(uuid).getChannel().isReceiveRoutingInfo()) {
                        continue;
                    }
                    uuids.add(uuid);
                }
            }
            for (String uuid : uuids) {
                if (UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(uuid).equals(segment) && UuidHelper.isRouterUuid(uuid)) {
                    sendRouterInfo(uuid, constructRoutingInfo(uuid, links));
                    logger.trace("RouterInfo from: " + router.getCOOSInstanceName() + ", to:" + uuid + ":: " + s);
                }
            }
            sendRouterInfo(routerUuid, constructLocalRoutingInfo(links));
        } catch (Exception e) {
            logger.error("Exception ignored.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Vector<LinkCost> constructRoutingInfo(String receivingRouterUuid, Collection<Link> links) {
        Vector<LinkCost> routingInfo = new Vector<LinkCost>();
        for (Link link : links) {
            String uuid = link.getDestinationUuid();
            String uuidSegment = UuidHelper.getSegmentFromSegmentOrEndpointUuid(uuid);
            Vector<String> aliases = link.getAlises();
            Vector<String> broadCastAliases = new Vector<String>();
            for (String alias : aliases) {
                if (!alias.startsWith(Router.LOCAL_SEGMENT)) {
                    broadCastAliases.add(alias);
                }
            }
            if (uuidSegment.equals(segment) && !UuidHelper.isSegment(uuid)) {
                routingInfo.addElement(new LinkCost(routerUuid, uuid, link.getLinkId(), link.getCostMap(), broadCastAliases));
            } else if (UuidHelper.isSegment(uuid) && !uuidSegment.equals(segment) && UuidHelper.isInParentChildRelation(uuidSegment, UuidHelper.getSegmentFromSegmentOrEndpointUuid(receivingRouterUuid))) {
                routingInfo.addElement(new LinkCost(routerUuid, uuid, link.getLinkId(), link.getCostMap(), link.getAlises()));
            }
        }
        return routingInfo;
    }

    @SuppressWarnings("unchecked")
    private Vector<LinkCost> constructLocalRoutingInfo(Collection<Link> links) {
        Vector<LinkCost> routingInfoLocal = new Vector<LinkCost>();
        for (Link link : links) {
            String uuid = link.getDestinationUuid();
            String uuidSegment = UuidHelper.getSegmentFromSegmentOrEndpointUuid(uuid);
            if (uuidSegment.equals(segment) && !UuidHelper.isSegment(uuid)) {
                routingInfoLocal.addElement(new LinkCost(routerUuid, uuid, link.getLinkId(), link.getCostMap(), link.getAlises()));
            } else if (UuidHelper.isSegment(uuid) && !uuidSegment.equals(segment)) {
                routingInfoLocal.addElement(new LinkCost(routerUuid, uuid, link.getLinkId(), link.getCostMap(), null));
            }
        }
        return routingInfoLocal;
    }

    private void sendRouterInfo(String uuid, Vector<LinkCost> routingInfo) {
        try {
            DefaultMessage msg = new DefaultMessage();
            msg.setReceiverEndpointUri("coos://" + uuid);
            msg.setSenderEndpointUri("coos://" + routerUuid);
            msg.setHeader(Message.SERIALIZATION_METHOD, Message.SERIALIZATION_METHOD_JAVA);
            msg.setHeader(Message.TYPE, Message.TYPE_ROUTING_INFO);
            msg.setBody(routingInfo);
            router.processMessage(msg);
        } catch (Exception e) {
            logger.error("Exception ignored.", e);
        }
    }

    public void stop() {
        topologyMap.stop();
        if (timer != null) {
            timer.cancel();
        }
    }

    public String getAlgorithmName() {
        return ALG_NAME;
    }
}
