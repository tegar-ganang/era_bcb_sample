package gov.ca.bdo.modeling.dsm2.map.client.map;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.maps.client.event.MarkerClickHandler;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class NodeClickHandler implements MarkerClickHandler {

    private MapPanel mapPanel;

    private HorizontalPanel nodeEditPanel;

    private TextBox nodeEditBox;

    private NodeIdChangeHandler handler;

    public NodeClickHandler(MapPanel mapPanel) {
        this.mapPanel = mapPanel;
        nodeEditPanel = new HorizontalPanel();
        nodeEditPanel.add(new Label("Edit Node Id:"));
        nodeEditPanel.add(nodeEditBox = new TextBox());
        handler = new NodeIdChangeHandler(mapPanel);
    }

    public void onClick(MarkerClickEvent event) {
        Marker m = event.getSender();
        String id = m.getTitle();
        if (mapPanel.isInDeletingMode()) {
            try {
                mapPanel.getInfoPanel().clear();
                mapPanel.getNodeManager().removeNode(id, mapPanel.getChannelManager().getChannels());
            } catch (Exception ex) {
                mapPanel.showMessage(ex.getMessage());
            }
            return;
        }
        handler.setPreviousValue(id);
        nodeEditBox.setText(id);
        nodeEditBox.addValueChangeHandler(handler);
        mapPanel.getInfoPanel().clear();
        mapPanel.getInfoPanel().add(nodeEditPanel);
    }

    public static final class NodeIdChangeHandler implements ValueChangeHandler<String> {

        private String previousValue = null;

        private MapPanel mapPanel;

        public NodeIdChangeHandler(MapPanel mapPanel) {
            this.mapPanel = mapPanel;
        }

        public void onValueChange(ValueChangeEvent<String> event) {
            if (!mapPanel.isInEditMode()) {
                return;
            }
            String newValue = event.getValue();
            try {
                mapPanel.getNodeManager().renameNodeId(newValue, previousValue);
                setPreviousValue(newValue);
            } catch (Exception ex) {
                Object source = event.getSource();
                if (!(source instanceof TextBox)) {
                    return;
                }
                TextBox box = (TextBox) source;
                box.setValue(previousValue, false);
            }
        }

        public void setPreviousValue(String val) {
            previousValue = val;
        }
    }
}
