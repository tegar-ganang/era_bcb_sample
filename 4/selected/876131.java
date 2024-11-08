package gov.ca.bdo.modeling.dsm2.map.client.map;

import gov.ca.bdo.modeling.dsm2.map.client.WindowUtils;
import gov.ca.dsm2.input.model.Channel;
import gov.ca.dsm2.input.model.Channels;
import gov.ca.dsm2.input.model.Node;
import gov.ca.dsm2.input.model.XSection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import com.google.gwt.maps.client.event.PolylineMouseOverHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Overlay;
import com.google.gwt.maps.client.overlay.PolyStyleOptions;
import com.google.gwt.maps.client.overlay.Polyline;

public class ChannelLineDataManager {

    private Channels channels;

    private final HashMap<String, Polyline> lineMap = new HashMap<String, Polyline>();

    private HashMap<Polyline, String> lineToIdMap = null;

    private final PolylineEncoder encoder;

    private static final boolean ENCODE_POLYLINES = false;

    private final int weight = 3;

    private final double opacity = 0.35;

    private MapPanel mapPanel;

    private HashMap<XSection, Polyline> xsectionLineMap;

    private ChannelClickHandler channelClickHandler;

    private XSectionLineClickHandler xSectionLineClickHandler;

    public ChannelLineDataManager(MapPanel mapPanel, Channels channels) {
        this.mapPanel = mapPanel;
        channelClickHandler = new ChannelClickHandler(mapPanel);
        this.channels = channels;
        encoder = PolylineEncoder.newInstance(4, 12, 0.00001, false);
    }

    public void clear() {
        channels = new Channels();
    }

    public Channels getChannels() {
        return channels;
    }

    public void addPolyline(String channelId, Polyline line) {
        lineMap.put(channelId, line);
        if (lineToIdMap != null) {
            lineToIdMap.put(line, channelId);
        }
    }

    public Polyline getPolyline(String channelId) {
        return lineMap.get(channelId);
    }

    public List<Polyline> getLinesForNodeId(String nodeId) {
        ArrayList<Polyline> connectedChannels = new ArrayList<Polyline>();
        String upChannels = channels.getUpChannels(nodeId);
        String downChannels = channels.getDownChannels(nodeId);
        if (upChannels != null) {
            String[] channelIds = upChannels.split(",");
            for (String channelId : channelIds) {
                connectedChannels.add(getPolyline(channelId));
            }
        }
        if (downChannels != null) {
            String[] channelIds = downChannels.split(",");
            for (String channelId : channelIds) {
                connectedChannels.add(getPolyline(channelId));
            }
        }
        return connectedChannels;
    }

    public List<String> getChannelsForNodeId(String nodeId) {
        String downChannels = channels.getDownChannels(nodeId);
        String upChannels = channels.getUpChannels(nodeId);
        ArrayList<String> channelIds = new ArrayList<String>();
        parseIdsToList(downChannels, channelIds);
        parseIdsToList(upChannels, channelIds);
        return channelIds;
    }

    private void parseIdsToList(String channelIdString, ArrayList<String> channelIds) {
        if (channelIdString != null) {
            String[] ids = channelIdString.split(",");
            for (String id : ids) {
                channelIds.add(id.trim());
            }
        }
    }

    public Collection<Polyline> getPolylines() {
        return lineMap.values();
    }

    public void removePolyline(String channelId) {
        Polyline polyline = getPolyline(channelId);
        mapPanel.getMap().removeOverlay(polyline);
        lineMap.remove(channelId);
        if (lineToIdMap != null) {
            lineToIdMap.remove(polyline);
        }
    }

    public void addLines() {
        Logger.getLogger("map").info("adding lines for channels");
        for (Channel data : getChannels().getChannels()) {
            addPolylineForChannel(data);
        }
    }

    public void addChannel(Channel channel) {
        channels.addChannel(channel);
        addPolylineForChannel(channel);
    }

    public Polyline addPolylineForChannel(Channel channel) {
        Node upNode = mapPanel.getNodeManager().getNodeData(channel.getUpNodeId());
        Node downNode = mapPanel.getNodeManager().getNodeData(channel.getDownNodeId());
        if ((upNode == null) || (downNode == null)) {
            return null;
        }
        LatLng upPoint = LatLng.newInstance(upNode.getLatitude(), upNode.getLongitude());
        LatLng downPoint = LatLng.newInstance(downNode.getLatitude(), downNode.getLongitude());
        LatLng[] points = null;
        ArrayList<String> channelsWithNodes = ModelUtils.getChannelsWithNodes(upNode, downNode, getChannels());
        if (channelsWithNodes.size() > 1) {
            Collections.sort(channelsWithNodes);
            int n = channelsWithNodes.size();
            int i = 0;
            for (i = 0; i < channelsWithNodes.size(); i++) {
                if (channelsWithNodes.get(i).equals(channel.getId())) {
                    break;
                }
            }
            double lat = upPoint.getLatitude() + downPoint.getLatitude();
            double lon = upPoint.getLongitude() + downPoint.getLongitude();
            LatLng midPoint = LatLng.newInstance(lat / 2 + (i - n / 2.0) * 0.001, lon / 2 + (i - n / 2.0) * 0.001);
            points = new LatLng[] { upPoint, midPoint, downPoint };
        } else {
            points = new LatLng[] { upPoint, downPoint };
        }
        Polyline line = null;
        if (!ENCODE_POLYLINES) {
            line = new Polyline(points);
            line.setStrokeStyle(getPolylineStyle());
        } else {
            line = encoder.dpEncodeToGPolyline(points, getLineColor(), weight, opacity);
        }
        addPolyline(channel.getId(), line);
        line.addPolylineMouseOverHandler(new PolylineMouseOverHandler() {

            public void onMouseOver(PolylineMouseOverEvent event) {
                WindowUtils.changeCursor("pointer");
            }
        });
        line.addPolylineClickHandler(channelClickHandler);
        mapPanel.getMap().addOverlay(line);
        return line;
    }

    public PolyStyleOptions getPolylineStyle() {
        PolyStyleOptions style = PolyStyleOptions.newInstance(getLineColor());
        style.setOpacity(opacity);
        style.setWeight(weight);
        return style;
    }

    protected String getLineColor() {
        return "#110077";
    }

    public String getNewChannelId() {
        return (channels.getMaxChannelId() + 1) + "";
    }

    public String getChannelId(Overlay overlay) {
        if (!(overlay instanceof Polyline)) {
            return null;
        }
        if (lineToIdMap == null) {
            lineToIdMap = new HashMap<Polyline, String>();
            for (String id : lineMap.keySet()) {
                lineToIdMap.put(lineMap.get(id), id);
            }
        }
        Polyline line = (Polyline) overlay;
        return lineToIdMap.get(line);
    }

    public XSection getXSectionFor(Overlay overlay) {
        for (XSection xs : xsectionLineMap.keySet()) {
            Polyline polyline = xsectionLineMap.get(xs);
            if (polyline == overlay) {
                return xs;
            }
        }
        return null;
    }

    public Collection<Polyline> getXSectionLines() {
        if (xsectionLineMap != null) {
            return xsectionLineMap.values();
        } else {
            return null;
        }
    }

    public void addXSectionLine(XSection xSection, Polyline line) {
        if (xsectionLineMap == null) {
            xsectionLineMap = new HashMap<XSection, Polyline>();
        }
        xsectionLineMap.put(xSection, line);
    }

    public Collection<XSection> getXSections() {
        return xsectionLineMap.keySet();
    }

    public Polyline getXsectionLineFor(XSection xs) {
        return xsectionLineMap.get(xs);
    }

    public void removeXSection(XSection xSection) {
        Polyline polyline = xsectionLineMap.get(xSection);
        mapPanel.getMap().removeOverlay(polyline);
        Channel channel = channels.getChannel(xSection.getChannelId());
        channel.getXsections().remove(xSection);
        xsectionLineMap.remove(xSection);
    }

    public void clearXSectionLines() {
        if (xsectionLineMap != null) {
            for (Polyline line : xsectionLineMap.values()) {
                mapPanel.getMap().removeOverlay(line);
            }
            xsectionLineMap.clear();
            xsectionLineMap = null;
        }
    }

    public void removeChannel(Channel channel) {
        removePolyline(channel.getId());
        clearXSectionLines();
        channels.removeChannel(channel);
    }

    public void setLinesColor(String color) {
        if (lineMap == null) {
            return;
        }
        PolyStyleOptions style = PolyStyleOptions.newInstance(color);
        for (Polyline line : lineMap.values()) {
            line.setStrokeStyle(style);
        }
    }

    public void clearXSections() {
        if (xsectionLineMap == null) {
            return;
        }
        ArrayList<XSection> xsections = new ArrayList<XSection>();
        xsections.addAll(xsectionLineMap.keySet());
        for (XSection x : xsections) {
            removeXSection(x);
        }
    }

    public void drawXSectionLines(Channel channel, ChannelInfoPanel infoPanel) {
        clearXSectionLines();
        Node upNode = mapPanel.getNodeManager().getNodes().getNode(channel.getUpNodeId());
        Node downNode = mapPanel.getNodeManager().getNodes().getNode(channel.getDownNodeId());
        ArrayList<XSection> xsections = channel.getXsections();
        int xSectionIndex = 0;
        xSectionLineClickHandler = new XSectionLineClickHandler(mapPanel, infoPanel);
        for (final XSection xSection : xsections) {
            createLineAndAddForXSection(xSection, channel, upNode, downNode);
            xSectionIndex++;
        }
    }

    private void createLineAndAddForXSection(final XSection xSection, Channel channel, Node upNode, Node downNode) {
        double distance = xSection.getDistance();
        distance = channel.getLength() * distance;
        LatLng[] latLngs = null;
        if (xSection.getProfile() == null) {
            latLngs = ModelUtils.calculateEndPoints(xSection, channel, upNode, downNode);
        } else {
            List<double[]> endPoints = xSection.getProfile().getEndPoints();
            latLngs = new LatLng[] { LatLng.newInstance(endPoints.get(0)[0], endPoints.get(0)[1]), LatLng.newInstance(endPoints.get(1)[0], endPoints.get(1)[1]) };
        }
        final Polyline line = new Polyline(latLngs, "green", 4);
        line.addPolylineClickHandler(xSectionLineClickHandler);
        line.addPolylineMouseOverHandler(new PolylineMouseOverHandler() {

            public void onMouseOver(PolylineMouseOverEvent event) {
                WindowUtils.changeCursor("pointer");
            }
        });
        addXSectionLine(xSection, line);
        mapPanel.getMap().addOverlay(line);
    }

    public void removeAndAddPolylineForXSection(XSection xs, Channel channel, Node upNode, Node downNode) {
        Polyline polyline = xsectionLineMap.get(xs);
        mapPanel.getMap().removeOverlay(polyline);
        xsectionLineMap.remove(xs);
        createLineAndAddForXSection(xs, channel, upNode, downNode);
    }

    public ChannelClickHandler getChannelClickHandler() {
        return channelClickHandler;
    }

    public XSectionLineClickHandler getXSectionLineClickHandler() {
        return xSectionLineClickHandler;
    }
}
