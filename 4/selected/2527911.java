package gov.ca.bdo.modeling.dsm2.map.client.map;

import gov.ca.dsm2.input.model.Channel;
import gov.ca.dsm2.input.model.Node;
import gov.ca.dsm2.input.model.XSection;
import gov.ca.dsm2.input.model.XSectionProfile;
import java.util.ArrayList;
import com.google.gwt.maps.client.event.PolylineClickHandler;
import com.google.gwt.maps.client.event.PolylineLineUpdatedHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.PolyEditingOptions;
import com.google.gwt.maps.client.overlay.PolyStyleOptions;
import com.google.gwt.maps.client.overlay.Polyline;

public class XSectionLineClickHandler implements PolylineClickHandler {

    private static final PolyStyleOptions redLineStyle = PolyStyleOptions.newInstance("red");

    private static final PolyStyleOptions greenLineStyle = PolyStyleOptions.newInstance("green");

    private CrossSectionEditorPanel xsEditorPanel;

    private MapPanel mapPanel;

    private ChannelInfoPanel channelInfoPanel;

    private Polyline currentlySelectedLine;

    private XSection currentlySelectedXSection;

    public XSectionLineClickHandler(MapPanel mapPanel, ChannelInfoPanel infoPanel) {
        this.mapPanel = mapPanel;
        channelInfoPanel = infoPanel;
    }

    public void updateXSLine() {
        if (currentlySelectedLine != null) {
            if (currentlySelectedXSection != null) {
                Channel channel = ModelUtils.getChannelForXSection(currentlySelectedXSection, mapPanel.getChannelManager().getChannels());
                Node upNode = mapPanel.getNodeManager().getNodes().getNode(channel.getUpNodeId());
                Node downNode = mapPanel.getNodeManager().getNodes().getNode(channel.getDownNodeId());
                mapPanel.getChannelManager().removeAndAddPolylineForXSection(currentlySelectedXSection, channel, upNode, downNode);
            }
        }
    }

    public void onClick(PolylineClickEvent event) {
        final Polyline line = event.getSender();
        currentlySelectedLine = line;
        final XSection xSection = mapPanel.getChannelManager().getXSectionFor(line);
        currentlySelectedXSection = xSection;
        if (xSection == null) {
            mapPanel.showErrorMessage("The line clicked was not a xsection ? Try again.");
            return;
        }
        if (mapPanel.isInEditMode() && mapPanel.isInDeletingMode()) {
            mapPanel.getChannelManager().removeXSection(xSection);
            return;
        }
        final Channel channel = ModelUtils.getChannelForXSection(xSection, mapPanel.getChannelManager().getChannels());
        int index = 0;
        int xSectionIndex = 0;
        for (XSection xs : channel.getXsections()) {
            Polyline xsline = mapPanel.getChannelManager().getXsectionLineFor(xs);
            if (xs == xSection) {
                if (mapPanel.isInEditMode()) {
                    xsline.setEditingEnabled(true);
                }
                xsline.setStrokeStyle(redLineStyle);
                xSectionIndex = index;
            } else {
                if (mapPanel.isInEditMode()) {
                    xsline.setEditingEnabled(false);
                }
                xsline.setStrokeStyle(greenLineStyle);
            }
            index++;
        }
        if (!mapPanel.isInEditMode()) {
            xsEditorPanel = getXsEditorPanel();
            mapPanel.getInfoPanel().clear();
            mapPanel.getInfoPanel().add(xsEditorPanel);
            xsEditorPanel.draw(channel, xSectionIndex, mapPanel);
        } else {
            xsEditorPanel = getXsEditorPanel();
            setPolylineInEditMode(line, xSection, channel);
            mapPanel.getInfoPanel().clear();
            mapPanel.getInfoPanel().add(xsEditorPanel);
            xsEditorPanel.draw(channel, xSectionIndex, mapPanel);
        }
    }

    private void setPolylineInEditMode(final Polyline line, final XSection xSection, final Channel channel) {
        line.setEditingEnabled(PolyEditingOptions.newInstance(2));
        line.addPolylineLineUpdatedHandler(new PolylineLineUpdatedHandler() {

            public void onUpdate(PolylineLineUpdatedEvent event) {
                XSectionProfile profile = xSection.getProfile();
                if (profile == null) {
                    return;
                }
                ArrayList<double[]> endPoints = new ArrayList<double[]>();
                for (int i = 0; i < 2; i++) {
                    double[] points = new double[2];
                    LatLng vertex = line.getVertex(i);
                    points[0] = vertex.getLatitude();
                    points[1] = vertex.getLongitude();
                    endPoints.add(points);
                }
                profile.setEndPoints(endPoints);
                ModelUtils.updateXSectionPosition(channel, mapPanel.getNodeManager().getNodes(), xSection, channel.getLength());
            }
        });
    }

    public CrossSectionEditorPanel getXsEditorPanel() {
        if (xsEditorPanel == null) {
            xsEditorPanel = new CrossSectionEditorPanel(mapPanel);
        }
        return xsEditorPanel;
    }
}
