package gov.ca.bdo.modeling.dsm2.map.client.map;

import gov.ca.dsm2.input.model.Channels;
import gov.ca.dsm2.input.model.Node;
import gov.ca.dsm2.input.model.Nodes;
import java.util.Collection;
import java.util.HashMap;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.geom.Size;
import com.google.gwt.maps.client.overlay.Icon;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.maps.client.overlay.MarkerOptions;
import com.google.gwt.maps.client.overlay.Overlay;
import com.google.gwt.maps.utility.client.labeledmarker.LabeledMarker;
import com.google.gwt.maps.utility.client.labeledmarker.LabeledMarkerOptions;
import com.google.gwt.maps.utility.client.markerclusterer.MarkerClusterer;
import com.google.gwt.maps.utility.client.markerclusterer.MarkerClustererOptions;

public class NodeMarkerDataManager {

    private Nodes nodes;

    private final HashMap<String, Marker> markerMap;

    private MarkerClusterer markerClusterer;

    private MapPanel mapPanel;

    private MarkNewNodePosition dragEndHandler;

    private Icon labelIcon;

    private Icon editModeIcon;

    private NodeClickHandler nodeClickHandler;

    public NodeMarkerDataManager(MapPanel mapPanel, Nodes nodes) {
        this.nodes = nodes;
        this.mapPanel = mapPanel;
        dragEndHandler = new MarkNewNodePosition(this.mapPanel);
        nodeClickHandler = new NodeClickHandler(this.mapPanel);
        markerMap = new HashMap<String, Marker>();
        labelIcon = Icon.newInstance("images/greencirclemarker.png");
        labelIcon.setIconSize(Size.newInstance(22, 22));
        labelIcon.setIconAnchor(Point.newInstance(11, 11));
        labelIcon.setInfoWindowAnchor(Point.newInstance(11, 7));
        editModeIcon = Icon.newInstance("images/blue_MarkerN.png");
        editModeIcon.setIconSize(Size.newInstance(12, 20));
        editModeIcon.setShadowSize(Size.newInstance(22, 20));
        editModeIcon.setIconAnchor(Point.newInstance(6, 20));
        editModeIcon.setInfoWindowAnchor(Point.newInstance(5, 1));
    }

    public void clear() {
        nodes = new Nodes();
        markerMap.clear();
    }

    public Collection<Node> getAllNodes() {
        return nodes.getNodes();
    }

    public void addNode(Node node) {
        nodes.addNode(node);
        addMarkerForNode(node);
    }

    public void deleteNode(Node node) {
        nodes.removeNode(node);
        removeMarkerForNode(node);
    }

    public Node getNodeData(String id) {
        return nodes.getNode(id);
    }

    private void addMarker(Node mapMarkerData, Marker marker) {
        markerMap.put(mapMarkerData.getId(), marker);
    }

    public Marker getMarkerFor(String nodeId) {
        return markerMap.get(nodeId);
    }

    public Nodes getNodes() {
        return nodes;
    }

    private Icon getNodeIcon() {
        if (mapPanel.isInEditMode()) {
            return editModeIcon;
        } else {
            return labelIcon;
        }
    }

    /**
	 * adds node markers to map. Note: does not remove old markers @see
	 * clearNodeMarkers
	 * 
	 * @param mapPanel
	 */
    public void displayNodeMarkers() {
        for (Node node : getAllNodes()) {
            addMarkerForNode(node);
        }
        if (!mapPanel.isInEditMode()) {
            MarkerClustererOptions clusterOptions = MarkerClustererOptions.newInstance();
            clusterOptions.setGridSize(100);
            clusterOptions.setMaxZoom(10);
            Marker[] markers = new Marker[getAllNodes().size()];
            int i = 0;
            for (Marker marker : markerMap.values()) {
                markers[i++] = marker;
            }
            markerClusterer = MarkerClusterer.newInstance(mapPanel.getMap(), markers, clusterOptions);
        }
    }

    /**
	 * Works when adding one node at a time in edit mode. for labeled mode,
	 * you'll have to call refresh
	 * 
	 * @param mapMarkerData
	 */
    private void addMarkerForNode(Node mapMarkerData) {
        MarkerOptions options = null;
        if (!mapPanel.isInEditMode()) {
            LabeledMarkerOptions opts = LabeledMarkerOptions.newInstance();
            opts.setLabelOffset(Size.newInstance(-5, -5));
            opts.setLabelText(mapMarkerData.getId());
            opts.setLabelClass("hm-marker-label");
            options = opts;
        } else {
            options = MarkerOptions.newInstance();
        }
        options.setTitle(mapMarkerData.getId());
        options.setIcon(getNodeIcon());
        options.setDragCrossMove(mapPanel.isInEditMode());
        options.setDraggable(mapPanel.isInEditMode());
        options.setClickable(mapPanel.isInEditMode());
        options.setAutoPan(mapPanel.isInEditMode());
        Marker marker = null;
        LatLng point = LatLng.newInstance(mapMarkerData.getLatitude(), mapMarkerData.getLongitude());
        if (!mapPanel.isInEditMode()) {
            marker = new LabeledMarker(point, (LabeledMarkerOptions) options);
        } else {
            marker = new Marker(point, options);
        }
        if (mapPanel.isInEditMode()) {
            marker.addMarkerDragEndHandler(dragEndHandler);
            marker.addMarkerClickHandler(nodeClickHandler);
        }
        addMarker(mapMarkerData, marker);
        if (mapPanel.isInEditMode()) {
            mapPanel.getMap().addOverlay(marker);
        }
    }

    private void removeMarkerForNode(Node node) {
        if (mapPanel.isInEditMode()) {
            mapPanel.getMap().removeOverlay(getMarkerFor(node.getId()));
        } else {
            markerClusterer.removeMarker(getMarkerFor(node.getId()));
        }
    }

    /**
	 * clears node markers
	 * 
	 * @param mapPanel
	 */
    public void clearNodeMarkers() {
        if (markerClusterer != null) {
            markerClusterer.clearMarkers();
            markerClusterer = null;
        }
        for (Marker marker : markerMap.values()) {
            mapPanel.getMap().removeOverlay(marker);
        }
        markerMap.clear();
    }

    public String getNewNodeId() {
        return (nodes.calculateMaxNodeId() + 1) + "";
    }

    public Node getNodeForMarker(Overlay overlay) {
        if (overlay instanceof Marker) {
            Marker m = (Marker) overlay;
            String id = m.getTitle();
            Marker markerFor = getMarkerFor(id);
            if (markerFor == m) {
                return nodes.getNode(id);
            }
        }
        return null;
    }

    public void removeNode(String nodeId, Channels channels) {
        Node node = mapPanel.getNodeManager().getNodes().getNode(nodeId);
        if (node == null) {
            return;
        }
        String channelsConnectedTo = ModelUtils.getChannelsConnectedTo(channels, node);
        if (channelsConnectedTo != null) {
            throw new RuntimeException("Cannot delete node connected to channels: " + channelsConnectedTo);
        }
        removeMarkerForNode(node);
        nodes.removeNode(node);
    }

    public void renameNodeId(String newValue, String previousValue) {
        nodes.renameNodeId(newValue, previousValue);
        mapPanel.getMap().removeOverlay(getMarkerFor(previousValue));
        addMarkerForNode(nodes.getNode(newValue));
        mapPanel.getChannelManager().getChannels().updateNodeId(newValue, previousValue);
        mapPanel.getReservoirManager().getReservoirs().updateNodeId(newValue, previousValue);
        mapPanel.getGateManager().getGates().updateNodeId(newValue, previousValue);
        mapPanel.getBoundaryManager().getBoundaryInputs().updateNodeId(newValue, previousValue);
    }
}
