package gov.ca.bdo.modeling.dsm2.map.client.map;

import gov.ca.bdo.modeling.dsm2.map.client.model.RegularTimeSeries;
import gov.ca.bdo.modeling.dsm2.map.client.service.DataService;
import gov.ca.bdo.modeling.dsm2.map.client.service.DataServiceAsync;
import gov.ca.dsm2.input.model.Channel;
import gov.ca.dsm2.input.model.ChannelOutput;
import gov.ca.dsm2.input.model.DSM2Model;
import gov.ca.dsm2.input.model.Node;
import java.util.HashMap;
import java.util.List;
import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.geom.Size;
import com.google.gwt.maps.client.overlay.Icon;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.maps.client.overlay.MarkerOptions;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class OutputMarkerDataManager {

    private DSM2Model model;

    private final HashMap<String, Marker> outputMarkerMap;

    private final HashMap<String, String> outputVariableMap;

    private MapPanel mapPanel;

    private final DataServiceAsync dataService;

    public OutputMarkerDataManager() {
        outputVariableMap = new HashMap<String, String>();
        outputMarkerMap = new HashMap<String, Marker>();
        dataService = (DataServiceAsync) GWT.create(DataService.class);
    }

    public void setModel(DSM2Model model, MapPanel mapPanel) {
        this.model = model;
        this.mapPanel = mapPanel;
    }

    public void addMarkers(MapWidget map) {
        List<ChannelOutput> channelOutputs = model.getOutputs().getChannelOutputs();
        for (ChannelOutput channelOutput : channelOutputs) {
            double distance = 0.0;
            try {
                distance = Double.parseDouble(channelOutput.distance);
            } catch (NumberFormatException nfe) {
                if ("length".equals(channelOutput.distance)) {
                    Channel channel = model.getChannels().getChannel(channelOutput.channelId);
                    if (channel == null) {
                        continue;
                    }
                    distance = channel.getLength();
                }
            }
            LatLng position = calculateMarkerLocation(channelOutput.channelId, distance);
            if (position == null) {
                continue;
            }
            addOutputMarkerToMap(map, position, channelOutput);
        }
    }

    private void addOutputMarkerToMap(MapWidget map, LatLng position, final ChannelOutput output) {
        final String outputName = output.name;
        if (outputVariableMap.containsKey(outputName)) {
            String newValue = outputVariableMap.get(outputName) + "," + output.variable;
            outputVariableMap.put(outputName, newValue);
        } else {
            outputVariableMap.put(outputName, output.variable);
        }
        if (outputMarkerMap.containsKey(outputName)) {
            return;
        }
        Icon icon = Icon.newInstance("images/blue_MarkerO.png");
        icon.setShadowURL("images/shadow-marker.png");
        icon.setIconSize(Size.newInstance(20, 34));
        icon.setShadowSize(Size.newInstance(38, 34));
        icon.setIconAnchor(Point.newInstance(10, 17));
        icon.setInfoWindowAnchor(Point.newInstance(10, 17));
        MarkerOptions options = MarkerOptions.newInstance();
        options.setTitle(output.name);
        options.setIcon(icon);
        options.setDragCrossMove(true);
        options.setDraggable(true);
        options.setClickable(true);
        options.setAutoPan(true);
        Marker outputMarker = new Marker(position, options);
        outputMarker.addMarkerClickHandler(new OutputClickHandler(outputName, this, mapPanel));
        outputMarkerMap.put(outputName, outputMarker);
        map.addOverlay(outputMarker);
    }

    private LatLng calculateMarkerLocation(String channelId, double distance) {
        Channel channel = model.getChannels().getChannel(channelId);
        if (channel == null) {
            return null;
        }
        double distanceNormalized = distance / channel.getLength();
        String upNodeId = channel.getUpNodeId();
        String downNodeId = channel.getDownNodeId();
        Node upNode = model.getNodes().getNode(upNodeId);
        Node downNode = model.getNodes().getNode(downNodeId);
        double x2 = downNode.getLatitude();
        double y2 = downNode.getLongitude();
        double x1 = upNode.getLatitude();
        double y1 = upNode.getLongitude();
        double slope = (y2 - y1) / (x2 - x1);
        double xn = x1 + distanceNormalized * (x2 - x1);
        double yn = slope * (xn - x1) + y1;
        return LatLng.newInstance(xn, yn);
    }

    public String getOutputVariables(String name) {
        return outputVariableMap.get(name);
    }

    public void getRegularTimeSeries(String name, String[] variables, final OutputPanel panel) {
        dataService.getRegularTimeSeries(mapPanel.getCurrentStudy(), name, variables, new AsyncCallback<List<RegularTimeSeries>>() {

            public void onSuccess(List<RegularTimeSeries> result) {
                panel.displayData(result);
            }

            public void onFailure(Throwable caught) {
                panel.displayError(caught);
            }
        });
    }

    public void hideMarkers(boolean hide) {
        for (String name : outputMarkerMap.keySet()) {
            if (hide) {
                mapPanel.getMap().removeOverlay(outputMarkerMap.get(name));
            } else {
                mapPanel.getMap().addOverlay(outputMarkerMap.get(name));
            }
        }
    }
}
