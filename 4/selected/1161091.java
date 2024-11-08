package gov.ca.bdo.modeling.dsm2.map.client.map;

import gov.ca.dsm2.input.model.Channel;
import gov.ca.dsm2.input.model.Channels;
import gov.ca.dsm2.input.model.DSM2Model;
import gov.ca.dsm2.input.model.Gates;
import gov.ca.dsm2.input.model.Node;
import gov.ca.dsm2.input.model.Reservoirs;
import gov.ca.modeling.dsm2.widgets.client.events.MessageEvent;
import gov.ca.modeling.maps.widgets.client.ExpandContractMapControl;
import java.util.ArrayList;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.maps.client.Copyright;
import com.google.gwt.maps.client.CopyrightCollection;
import com.google.gwt.maps.client.MapType;
import com.google.gwt.maps.client.MapUIOptions;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.TileLayer;
import com.google.gwt.maps.client.event.MapZoomEndHandler;
import com.google.gwt.maps.client.event.PolylineClickHandler.PolylineClickEvent;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.LatLngBounds;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.geom.Size;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.maps.client.overlay.PolyStyleOptions;
import com.google.gwt.maps.client.overlay.Polyline;
import com.google.gwt.maps.client.overlay.TileLayerOverlay;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ResizeComposite;

public class MapPanel extends ResizeComposite {

    static final int ZOOM_LEVEL = 10;

    private NodeMarkerDataManager nodeManager;

    private ChannelLineDataManager channelManager;

    private MapWidget map;

    private DSM2Model model;

    private String currentStudy;

    private OutputMarkerDataManager outputMarkerDataManager;

    private boolean editMode = false;

    protected String[] studyNames = new String[0];

    private TileLayerOverlay bathymetryOverlay;

    private TextAnnotationsManager textAnnotationHandler;

    boolean SHOW_ON_CLICK = false;

    private GateOverlayManager gateOverlayManager;

    private ReservoirOverlayManager reservoirOverlayManager;

    private BoundaryMarkerDataManager boundaryOverlayManager;

    private GridVisibilityControl visibilityControl;

    private Panel infoPanel;

    private TransfersManager transfersManager;

    private ArrayList<Polyline> flowLines;

    private EventBus eventBus;

    private boolean deleteMode;

    public MapPanel(EventBus eventBus) {
        this.eventBus = eventBus;
        setMap(new MapWidget(LatLng.newInstance(38.15, -121.70), 10));
        setOptions();
        visibilityControl = new GridVisibilityControl(this);
        getMap().addControl(visibilityControl);
        ExpandContractMapControl fullScreenControl = new ExpandContractMapControl();
        map.addControl(fullScreenControl);
        initWidget(getMap());
        map.addMapZoomEndHandler(new MapZoomEndHandler() {

            public void onZoomEnd(MapZoomEndEvent event) {
                if ((event.getNewZoomLevel() <= ZOOM_LEVEL) && (event.getOldZoomLevel() > ZOOM_LEVEL)) {
                    hideNodeMarkers(true);
                }
                if ((event.getOldZoomLevel() <= ZOOM_LEVEL) && (event.getNewZoomLevel() > ZOOM_LEVEL)) {
                    hideNodeMarkers(visibilityControl.getHideNodes().getValue());
                }
            }
        });
        setStyleName("map-panel");
        deleteMode = false;
    }

    private void setOptions() {
        MapUIOptions options = MapUIOptions.newInstance(Size.newInstance(1000, 800));
        options.setKeyboard(true);
        options.setMenuMapTypeControl(true);
        options.setScaleControl(true);
        options.setScrollwheel(true);
        getMap().setUI(options);
        map.addMapType(new MapType(new TileLayer[] { getUSTopoMapsLayer() }, MapType.getNormalMap().getProjection(), "US Topo"));
    }

    public static TileLayer getUSTopoMapsLayer() {
        CopyrightCollection myCopyright = new CopyrightCollection("@ California USGS, ESRI");
        LatLng southWest = LatLng.newInstance(36.5, -123.0);
        LatLng northEast = LatLng.newInstance(39.5, -120.5);
        myCopyright.addCopyright(new Copyright(1, LatLngBounds.newInstance(southWest, northEast), 10, "@ Copyright USGS, ESRI"));
        TileLayer tileLayer = new TileLayer(myCopyright, 10, 15) {

            public double getOpacity() {
                return 1.0;
            }

            public String getTileURL(Point tile, int zoomLevel) {
                return "http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer/tile/" + zoomLevel + "/" + tile.getY() + "/" + tile.getX();
            }

            public boolean isPng() {
                return false;
            }
        };
        return tileLayer;
    }

    public void populateGrid() {
        clearAllMarkers();
        if (model == null) {
            return;
        }
        setNodeManager(new NodeMarkerDataManager(this, model.getNodes()));
        setChannelManager(new ChannelLineDataManager(this, model.getChannels()));
        refreshGrid();
    }

    protected void refreshGrid() {
        clearAllMarkers();
        populateNodeMarkers();
        populateChannelLines();
        populateGateImages();
        populateReservoirMarkers();
        populateOutputMarkers();
        populateTextAnnotationMarkers();
        populateBoundaryMarkers();
        populateTransfers();
    }

    private void populateTextAnnotationMarkers() {
        textAnnotationHandler = new TextAnnotationsManager(this);
    }

    private void populateOutputMarkers() {
        if (!visibilityControl.getHideOutputs().getValue()) {
            outputMarkerDataManager = new OutputMarkerDataManager();
            outputMarkerDataManager.setModel(model, this);
            outputMarkerDataManager.addMarkers(map);
        }
    }

    private void populateBoundaryMarkers() {
        if (!visibilityControl.getHideBoundaries().getValue()) {
            if (boundaryOverlayManager == null) {
                boundaryOverlayManager = new BoundaryMarkerDataManager();
                boundaryOverlayManager.setModel(model, this);
                boundaryOverlayManager.addMarkers(map);
            }
        }
    }

    private void populateTransfers() {
        transfersManager = new TransfersManager(map, model);
        transfersManager.addLines();
    }

    protected void clearAllMarkers() {
        if (getNodeManager() != null) {
            getNodeManager().clearNodeMarkers();
        }
        if (getMap() != null) {
            getMap().clearOverlays();
        }
    }

    protected void populateNodeMarkers() {
        if (getNodeManager() != null) {
            if (map.getZoomLevel() > ZOOM_LEVEL) {
                if (!visibilityControl.getHideNodes().getValue()) {
                    getNodeManager().displayNodeMarkers();
                }
            }
        }
    }

    protected void populateChannelLines() {
        flowLines = null;
        if (!visibilityControl.getHideChannels().getValue()) {
            getChannelManager().addLines();
        }
    }

    protected void populateGateImages() {
        if (!visibilityControl.getHideGates().getValue()) {
            Gates gates = model.getGates();
            gateOverlayManager = new GateOverlayManager(this, gates);
            gateOverlayManager.addGates();
        }
    }

    protected void populateReservoirMarkers() {
        if (!visibilityControl.getHideReservoirs().getValue()) {
            Reservoirs reservoirs = model.getReservoirs();
            reservoirOverlayManager = new ReservoirOverlayManager(this);
            reservoirOverlayManager.setReservoirs(reservoirs);
            reservoirOverlayManager.displayReservoirMarkers();
        }
    }

    public void hideNodeMarkers(boolean hide) {
        if (getNodeManager() == null) {
            return;
        }
        if (hide) {
            getNodeManager().clearNodeMarkers();
        } else {
            getNodeManager().displayNodeMarkers();
        }
    }

    public void hideChannelLines(boolean hide) {
        if (getChannelManager() != null) {
            for (Polyline line : getChannelManager().getPolylines()) {
                line.setVisible(!hide);
            }
        }
    }

    public void hideGateMarkers(boolean hide) {
        if (!hide) {
            populateGateImages();
        }
        gateOverlayManager.hideMarkers(hide);
    }

    public void hideReservoirMarkers(boolean hide) {
        reservoirOverlayManager.hideMarkers(hide);
    }

    public void hideOutputMarkers(boolean hide) {
        if (!hide && (outputMarkerDataManager == null)) {
            populateOutputMarkers();
        }
        if (outputMarkerDataManager != null) {
            outputMarkerDataManager.hideMarkers(hide);
        }
    }

    public void hideTransfers(boolean hide) {
        transfersManager.hideTransfers(hide);
    }

    public void hideBoundaryMarkers(boolean hide) {
        if ((hide == false) && (boundaryOverlayManager == null)) {
            populateBoundaryMarkers();
        }
        if (boundaryOverlayManager != null) {
            boundaryOverlayManager.hideMarkers(hide);
        }
        if (hide) {
            boundaryOverlayManager = null;
        }
    }

    public void showBathymetry(boolean show) {
        if (bathymetryOverlay == null) {
            createBathymetryOverlay();
        }
        if (show) {
            map.addOverlay(bathymetryOverlay);
        } else {
            map.removeOverlay(bathymetryOverlay);
        }
    }

    private void createBathymetryOverlay() {
        CopyrightCollection myCopyright = new CopyrightCollection("@ California DWR 2010");
        LatLng southWest = LatLng.newInstance(36.5, -123.0);
        LatLng northEast = LatLng.newInstance(39.5, -120.5);
        myCopyright.addCopyright(new Copyright(1, LatLngBounds.newInstance(southWest, northEast), 10, "DWR"));
        TileLayer tileLayer = new TileLayer(myCopyright, 10, 17) {

            public double getOpacity() {
                return 0.6;
            }

            public String getTileURL(Point tile, int zoomLevel) {
                int version = (tile.getX() + tile.getY()) % 4 + 1;
                String uniqueValue = tile.getX() + "" + tile.getY() + "" + zoomLevel;
                int hashCode = uniqueValue.hashCode();
                return "http://" + version + ".latest.dsm2bathymetry.appspot.com/tiles/" + hashCode + "_tile" + tile.getX() + "_" + tile.getY() + "_" + zoomLevel + ".png";
            }

            public boolean isPng() {
                return true;
            }
        };
        bathymetryOverlay = new TileLayerOverlay(tileLayer);
    }

    public void centerAndZoomOnNode(String nodeId) {
        Marker marker = getNodeManager().getMarkerFor(nodeId);
        if (marker == null) {
            return;
        }
        LatLng point = marker.getLatLng();
        getMap().panTo(point);
        if (getMap().getZoomLevel() < 13) {
            getMap().setZoomLevel(13);
        }
    }

    public void centerAndZoomOnChannel(String channelId) {
        Polyline line = getChannelManager().getPolyline(channelId);
        if (line == null) {
            return;
        }
        LatLngBounds bounds = line.getBounds();
        getMap().panTo(bounds.getCenter());
        while (!getMap().getBounds().containsBounds(bounds)) {
            getMap().setZoomLevel(getMap().getZoomLevel() - 1);
        }
        new ChannelClickHandler(this).onClick(new PolylineClickEvent(line, line.getVertex(0)));
    }

    public MapWidget getMapWidget() {
        return getMap();
    }

    public void setNodeManager(NodeMarkerDataManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public NodeMarkerDataManager getNodeManager() {
        return nodeManager;
    }

    public void setChannelManager(ChannelLineDataManager channelManager) {
        this.channelManager = channelManager;
    }

    public ChannelLineDataManager getChannelManager() {
        return channelManager;
    }

    public BoundaryMarkerDataManager getBoundaryManager() {
        return boundaryOverlayManager;
    }

    public void setMap(MapWidget map) {
        this.map = map;
    }

    public MapWidget getMap() {
        return map;
    }

    public void setStudy(String studyName) {
        currentStudy = studyName;
    }

    public String getCurrentStudy() {
        return currentStudy;
    }

    public boolean isInEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        refreshGrid();
    }

    public void setDeletingMode(boolean deleteMode) {
        this.deleteMode = deleteMode;
    }

    public boolean isInDeletingMode() {
        return deleteMode;
    }

    public String[] getStudyNames() {
        return studyNames;
    }

    public void turnOnTextAnnotation() {
        textAnnotationHandler.startAddingText();
    }

    public void turnOffTextAnnotation() {
        if (textAnnotationHandler != null) {
            textAnnotationHandler.stopAddingText();
        }
    }

    public DSM2Model getModel() {
        return model;
    }

    public void setModel(DSM2Model model) {
        this.model = model;
    }

    public void setInfoPanel(Panel panel) {
        infoPanel = panel;
    }

    public Panel getInfoPanel() {
        return infoPanel;
    }

    public void showFlowLines() {
        if (flowLines == null) {
            NodeMarkerDataManager nm = getNodeManager();
            DSM2Model model = getModel();
            Channels channels = model.getChannels();
            flowLines = new ArrayList<Polyline>();
            final PolyStyleOptions style = PolyStyleOptions.newInstance("#FF0000", 4, 1.0);
            for (Channel channel : channels.getChannels()) {
                Node upNode = nm.getNodeData(channel.getUpNodeId());
                Node downNode = nm.getNodeData(channel.getDownNodeId());
                LatLng[] points = ModelUtils.getPointsForChannel(channel, upNode, downNode);
                Polyline line = new Polyline(points);
                line.setStrokeStyle(style);
                flowLines.add(line);
            }
        }
        for (Polyline line : flowLines) {
            map.addOverlay(line);
        }
    }

    public void hideFlowLines() {
        if (flowLines != null) {
            for (Polyline line : flowLines) {
                map.removeOverlay(line);
            }
        }
    }

    public GateOverlayManager getGateManager() {
        return gateOverlayManager;
    }

    public ReservoirOverlayManager getReservoirManager() {
        return reservoirOverlayManager;
    }

    public void showMessage(String message) {
        eventBus.fireEvent(new MessageEvent(message));
    }

    public void showErrorMessage(String message) {
        eventBus.fireEvent(new MessageEvent(message, MessageEvent.ERROR));
    }

    public GridVisibilityControl getGridVisibility() {
        return visibilityControl;
    }
}
