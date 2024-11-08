package gov.ca.bdo.modeling.dsm2.map.client.map;

import gov.ca.dsm2.input.model.Node;
import java.util.List;
import com.google.gwt.maps.client.event.MarkerDragEndHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Marker;

public class MarkNewNodePosition implements MarkerDragEndHandler {

    private final MapPanel mapPanel;

    public MarkNewNodePosition(MapPanel mapPanel) {
        this.mapPanel = mapPanel;
    }

    public void onDragEnd(MarkerDragEndEvent event) {
        Marker marker = event.getSender();
        LatLng latLng = marker.getLatLng();
        String id = marker.getTitle();
        Node nodeData = mapPanel.getNodeManager().getNodeData(id);
        nodeData.setLatitude(latLng.getLatitude());
        nodeData.setLongitude(latLng.getLongitude());
        if (mapPanel.getChannelManager() == null) {
            return;
        }
        List<String> channelIds = mapPanel.getChannelManager().getChannelsForNodeId(nodeData.getId());
        for (String channelId : channelIds) {
            mapPanel.getChannelManager().removePolyline(channelId);
            mapPanel.getChannelManager().addPolylineForChannel(mapPanel.getChannelManager().getChannels().getChannel(channelId));
        }
    }
}
