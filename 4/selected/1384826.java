package org.coos.messaging.routing;

import org.coos.messaging.Link;
import org.coos.messaging.Message;
import org.coos.messaging.impl.DefaultMessage;
import org.coos.messaging.util.UuidHelper;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class SimpleDVAlgorithm extends DefaultRoutingAlgorithm implements HashMapCallback {

    public static final String ALG_NAME = "simpledv";

    public static final String REFRESH_INTERVAL = "refreshInterval";

    public static final String AGING_FACTOR = "agingFactor";

    private Timer timer;

    private int refreshInterval = 100;

    private int agingFactor = 5;

    public SimpleDVAlgorithm() {
    }

    public SimpleDVAlgorithm(Router router, String routerUuid) {
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
        super.init(routerUuid, router);
    }

    @SuppressWarnings("unchecked")
    public void processRoutingInfo(Message routingInfo) {
        Vector linkCosts = (Vector) routingInfo.getBody();
        String s = "";
        for (Map<String, Link> routingTable : routingTables.values()) {
            for (int i = 0; i < linkCosts.size(); i++) {
                LinkCost linkCost = (LinkCost) linkCosts.elementAt(i);
                s += linkCost.getFromUuid() + "<->" + linkCost.getToUuid() + ": " + linkCost.getCost(Link.DEFAULT_QOS_CLASS) + ": " + linkCost.getAliases() + ", ";
                if (routingInfo.getMessageContext().getInBoundChannel() != null) {
                    String toUuid = linkCost.getToUuid();
                    Link link = routingInfo.getMessageContext().getInBoundChannel().getOutLink();
                    if (linkCost.getCost(Link.DEFAULT_QOS_CLASS) < LinkCost.MAX_VALUE) {
                        ((TimedConcurrentHashMap) routingTable).put(toUuid, link, agingFactor * refreshInterval, this);
                        List<String> aliases = linkCost.getAliases();
                        for (String alias : aliases) {
                            router.putAlias(alias, toUuid);
                        }
                    }
                } else {
                    String toUuid = linkCost.getToUuid();
                    Link link = links.get(linkCost.getLinkId());
                    if ((linkCost.getCost(Link.DEFAULT_QOS_CLASS) < LinkCost.MAX_VALUE) && (link != null)) {
                        ((TimedConcurrentHashMap) routingTable).put(toUuid, link, agingFactor * refreshInterval, this);
                        List<String> aliases = linkCost.getAliases();
                        for (String alias : aliases) {
                            router.putAlias(alias, toUuid);
                        }
                    }
                }
            }
        }
        logger.trace("Receiving on " + router.getCOOSInstanceName() + ", from " + routingInfo.getSenderEndpointUri() + " linkinfo: " + s);
        if (loggingEnabled) {
            for (String qos : routingTables.keySet()) {
                printRoutingTable(routerUuid, qos, routingTables.get(qos), logger);
            }
            printAliasTable(routerUuid, aliasTable, logger);
        }
    }

    @SuppressWarnings("unchecked")
    public void publishLink(Link link) {
        List<Link> links = new LinkedList<Link>();
        links.add(link);
        Iterator<String> iter = routingTables.keySet().iterator();
        while (iter.hasNext()) {
            String qos = iter.next();
            TimedConcurrentHashMap routingTable = routingTables.get(qos);
            if (link.getCost() < LinkCost.MAX_VALUE) {
                routingTable.put(link.getDestinationUuid(), link, agingFactor * refreshInterval, this);
            }
        }
    }

    private void broadcastRoutingInfo(Map<String, Link> routingTable) {
        try {
            String s = "";
            for (Link link : routingTable.values()) {
                s += link.getDestinationUuid() + ": " + link.getAlises() + ", ";
            }
            Set<String> uuids = new HashSet<String>();
            for (Link link : routingTable.values()) {
                if ((link.getChannel() != null) && !link.getChannel().isReceiveRoutingInfo()) {
                    continue;
                }
                uuids.add(link.getDestinationUuid());
            }
            for (String uuid : uuids) {
                if (UuidHelper.getSegmentFromEndpointNameOrEndpointUuid(uuid).equals(segment) && UuidHelper.isRouterUuid(uuid)) {
                    sendRouterInfo(uuid, constructRoutingInfo(uuid, routingTable));
                    logger.trace("RouterInfo from: " + router.getCOOSInstanceName() + ", to:" + uuid + ":: " + s);
                }
            }
            sendRouterInfo(routerUuid, constructLocalRoutingInfo(links.values()));
        } catch (Exception e) {
            logger.error("Exception ignored.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Vector<LinkCost> constructRoutingInfo(String receivingRouterUuid, Map<String, Link> routingTable) {
        Vector<LinkCost> routingInfo = new Vector<LinkCost>();
        for (String uuid : routingTable.keySet()) {
            Link link = routingTable.get(uuid);
            String uuidSegment = UuidHelper.getSegmentFromSegmentOrEndpointUuid(uuid);
            Vector<String> broadCastAliases = new Vector<String>();
            Map<String, String> aliasTable = router.getAliasTable();
            for (String alias1 : aliasTable.keySet()) {
                String uuid1 = aliasTable.get(alias1);
                if (!alias1.startsWith(Router.LOCAL_SEGMENT) && uuid.equals(uuid1)) {
                    broadCastAliases.add(alias1);
                }
            }
            if (uuidSegment.equals(segment) && !UuidHelper.isSegment(uuid) && !link.equals(routingTable.get(receivingRouterUuid))) {
                routingInfo.addElement(new LinkCost(routerUuid, uuid, link.getLinkId(), link.getCostMap(), broadCastAliases));
            } else if (UuidHelper.isSegment(uuid) && !uuidSegment.equals(segment) && UuidHelper.isInParentChildRelation(uuidSegment, UuidHelper.getSegmentFromSegmentOrEndpointUuid(receivingRouterUuid))) {
                routingInfo.addElement(new LinkCost(routerUuid, uuid, link.getLinkId(), link.getCostMap(), link.getAlises()));
            }
        }
        routingInfo.addElement(new LinkCost(routerUuid, routerUuid, null, new HashMap<String, Integer>(), new Vector<String>()));
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

    private void broadcastRoutingInfoAndSchedule() {
        broadcastRoutingInfo(routingTables.get(Link.DEFAULT_QOS_CLASS));
        try {
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    broadcastRoutingInfoAndSchedule();
                }
            }, refreshInterval);
        } catch (IllegalStateException e) {
        }
    }

    public void start() {
        timer = new Timer("SimpleDVTimer", true);
        broadcastRoutingInfoAndSchedule();
    }

    public void stop() throws Exception {
        if (timer != null) {
            timer.cancel();
        }
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

    public String getAlgorithmName() {
        return ALG_NAME;
    }
}
