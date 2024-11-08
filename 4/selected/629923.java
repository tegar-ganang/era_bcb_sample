package gov.ca.bdo.modeling.dsm2.map.client.map;

import gov.ca.bdo.modeling.dsm2.map.client.WindowUtils;
import gov.ca.dsm2.input.model.Channel;
import gov.ca.dsm2.input.model.Node;
import gov.ca.dsm2.input.model.Nodes;
import gov.ca.dsm2.input.model.XSection;
import java.util.ArrayList;
import java.util.Collection;
import com.google.gwt.maps.client.event.PolylineClickHandler;
import com.google.gwt.maps.client.event.PolylineLineUpdatedHandler;
import com.google.gwt.maps.client.event.PolylineMouseOverHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.PolyEditingOptions;
import com.google.gwt.maps.client.overlay.PolyStyleOptions;
import com.google.gwt.maps.client.overlay.Polyline;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.ScatterChart;

public class ChannelClickHandler implements PolylineClickHandler {

    private final MapPanel mapPanel;

    private final String color = "#FF0000";

    private final int weight = 5;

    private final double opacity = 0.75;

    private Polyline line;

    private ChannelInfoPanel infoPanel;

    public static final int MAX_VERTEX_FLOWLINE = 50;

    public ChannelClickHandler(MapPanel mapPanel) {
        this.mapPanel = mapPanel;
    }

    public void onClick(final PolylineClickEvent event) {
        Runnable visualizationLoadCallback = new Runnable() {

            public void run() {
                doOnClick(event);
            }
        };
        VisualizationUtils.loadVisualizationApi(visualizationLoadCallback, ScatterChart.PACKAGE);
    }

    public void doOnClick(PolylineClickEvent event) {
        Polyline channelLine = event.getSender();
        String channelId = mapPanel.getChannelManager().getChannelId(channelLine);
        if (channelId == null) {
            return;
        }
        final Channel channel = mapPanel.getChannelManager().getChannels().getChannel(channelId);
        if (channel == null) {
            mapPanel.showMessage("No channel found for " + channelId);
            return;
        }
        infoPanel = new ChannelInfoPanel(channel, mapPanel);
        mapPanel.getInfoPanel().clear();
        if (mapPanel.isInEditMode() && mapPanel.isInDeletingMode()) {
            mapPanel.getChannelManager().removeChannel(channel);
            if (line != null) {
                mapPanel.getMap().removeOverlay(line);
                line = null;
            }
            return;
        }
        mapPanel.getInfoPanel().add(infoPanel);
        NodeMarkerDataManager nodeManager = mapPanel.getNodeManager();
        Node upNode = nodeManager.getNodeData(channel.getUpNodeId());
        Node downNode = nodeManager.getNodeData(channel.getDownNodeId());
        if (line != null) {
            clearOverlays();
        }
        PolyStyleOptions style = PolyStyleOptions.newInstance(color, weight, opacity);
        LatLng[] points = ModelUtils.getPointsForChannel(channel, upNode, downNode);
        line = new Polyline(points);
        mapPanel.getMap().addOverlay(line);
        line.setStrokeStyle(style);
        if (mapPanel.isInEditMode()) {
            line.setEditingEnabled(PolyEditingOptions.newInstance(MAX_VERTEX_FLOWLINE));
            line.addPolylineClickHandler(new PolylineClickHandler() {

                public void onClick(PolylineClickEvent event) {
                    updateChannelLengthLatLng(channel);
                    line.setEditingEnabled(false);
                    clearOverlays();
                    updateDisplay(channel);
                }
            });
            line.addPolylineLineUpdatedHandler(new PolylineLineUpdatedHandler() {

                public void onUpdate(PolylineLineUpdatedEvent event) {
                    updateChannelLengthLatLng(channel);
                    updateDisplay(channel);
                }
            });
            drawXSectionLines(channel);
        } else {
            line.addPolylineClickHandler(new PolylineClickHandler() {

                public void onClick(PolylineClickEvent event) {
                    clearOverlays();
                    updateDisplay(channel);
                }
            });
            line.addPolylineMouseOverHandler(new PolylineMouseOverHandler() {

                public void onMouseOver(PolylineMouseOverEvent event) {
                    WindowUtils.changeCursor("pointer");
                }
            });
            drawXSectionLines(channel);
        }
    }

    public void drawXSectionLines(Channel channel) {
        mapPanel.getChannelManager().drawXSectionLines(channel, infoPanel);
    }

    public void updateChannelLengthLatLng(Channel channel) {
        double oldLength = channel.getLength();
        channel.setLength((int) ModelUtils.getLengthInFeet(line.getLength()));
        int vcount = line.getVertexCount();
        ArrayList<double[]> points = new ArrayList<double[]>();
        for (int i = 1; i < vcount - 1; i++) {
            LatLng vertex = line.getVertex(i);
            double[] point = new double[] { vertex.getLatitude(), vertex.getLongitude() };
            points.add(point);
        }
        channel.setLatLngPoints(points);
        for (XSection xSection : channel.getXsections()) {
            updateXSectionPosition(channel, xSection, oldLength);
        }
    }

    public void updateXSectionPosition(Channel channel, XSection xSection, double oldLength) {
        Nodes nodes = mapPanel.getNodeManager().getNodes();
        ModelUtils.updateXSectionPosition(channel, nodes, xSection, oldLength);
    }

    public void updateDisplay(Channel channel) {
        ChannelInfoPanel panel = new ChannelInfoPanel(channel, mapPanel);
        mapPanel.getInfoPanel().clear();
        mapPanel.getInfoPanel().add(panel);
    }

    public void clearOverlays() {
        if (line != null) {
            line.setVisible(false);
            mapPanel.getMap().removeOverlay(line);
            line = null;
        }
        Collection<Polyline> xSectionLines = mapPanel.getChannelManager().getXSectionLines();
        if (xSectionLines != null) {
            for (Polyline line : xSectionLines) {
                mapPanel.getMap().removeOverlay(line);
            }
        }
    }
}
