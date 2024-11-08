package org.rdv.datapanel.mapAlt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.Waypoint;
import org.rdv.DataPanelManager;
import org.rdv.datapanel.AbstractDataPanel;
import org.rdv.datapanel.mapAlt.WaypointGroup;

public class MapPanel extends AbstractDataPanel {

    private Map map = new Map();

    private HashMap<String, WaypointGroup> chMap = new HashMap<String, WaypointGroup>();

    public MapPanel() {
        setDataComponent(map);
        map.setZoom(15);
        map.setCenterPosition(new GeoPosition(20, 20));
        WaypointGroup pointGroup = new WaypointGroup(new Waypoint[] { new Waypoint(20, 20), new Waypoint(22, 18), new Waypoint(21, 15) });
        map.draw(pointGroup);
    }

    public boolean supportsMultipleChannels() {
        return true;
    }

    public boolean addChannel(String channelName) {
        chMap.put(channelName, new WaypointGroup());
        return super.addChannel(channelName);
    }

    public void postTime(double time) {
        super.postTime(time);
        WaypointGroup points = new WaypointGroup();
        for (String chName : subscribedChannels()) {
            System.out.println(chName + " " + rbnbController.getChannel(chName));
        }
        Iterator i = channels.iterator();
        while (i.hasNext()) {
            String channelName = (String) i.next();
            int channelIndex = channelMap.GetIndex(channelName);
            if (channelIndex != -1) {
                double lat = channelMap.GetDataAsFloat64(channelIndex)[0];
                double lng = channelMap.GetDataAsFloat64(channelIndex)[1];
                points.add(lat, lng);
            }
        }
        if (!points.isEmpty()) map.draw(points);
    }

    public void closePanel() {
        super.closePanel();
    }

    public Properties getProperties() {
        return super.getProperties();
    }

    public boolean isChannelSubscribed(String channelName) {
        return super.isChannelSubscribed(channelName);
    }

    public void openPanel(DataPanelManager dataPanelManager) {
        super.openPanel(dataPanelManager);
    }

    public boolean removeChannel(String channelName) {
        return super.removeChannel(channelName);
    }

    public boolean setChannel(String channelName) {
        return super.setChannel(channelName);
    }

    public void setProperty(String key, String value) {
        super.setProperty(key, value);
    }

    public int subscribedChannelCount() {
        return super.subscribedChannelCount();
    }

    public List<String> subscribedChannels() {
        return super.subscribedChannels();
    }
}
