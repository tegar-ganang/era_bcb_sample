package gov.ca.dsm2.input.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains the xsection of a {@link Channel} in {@link XSectionLayer} layers.
 * Contains the distance {@link #getDistance()} at which this xsection is
 * present and the channel id {@link #getChannelId()} to which it belongs
 * 
 * @author nsandhu
 * 
 */
@SuppressWarnings("serial")
public class XSection implements Serializable {

    private String channelId;

    private double distance;

    private ArrayList<XSectionLayer> layers;

    private XSectionProfile profile;

    public XSection() {
        layers = new ArrayList<XSectionLayer>();
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public ArrayList<XSectionLayer> getLayers() {
        return layers;
    }

    public void addLayer(XSectionLayer layer) {
        layers.add(layer);
    }

    public void setLayers(List<XSectionLayer> layers) {
        this.layers.clear();
        this.layers.addAll(layers);
    }

    public void setProfile(XSectionProfile xsProfile) {
        profile = xsProfile;
    }

    public XSectionProfile getProfile() {
        return profile;
    }
}
