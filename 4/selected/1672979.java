package gov.ca.bdo.modeling.dsm2.map.client.display;

import gov.ca.bdo.modeling.dsm2.map.client.map.ChannelLineDataManager;
import gov.ca.bdo.modeling.dsm2.map.client.map.ColorSchemePanel;
import gov.ca.bdo.modeling.dsm2.map.client.map.ElementType;
import gov.ca.dsm2.input.model.Channel;
import gov.ca.dsm2.input.model.Channels;
import java.util.HashMap;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.overlay.PolyStyleOptions;
import com.google.gwt.maps.client.overlay.Polyline;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;

public class MapControlPanel extends Composite {

    private static MapControlPanelUiBinder uiBinder = GWT.create(MapControlPanelUiBinder.class);

    interface MapControlPanelUiBinder extends UiBinder<Widget, MapControlPanel> {
    }

    private static NumberFormat formatter = NumberFormat.getFormat("0.000");

    @UiField
    TabLayoutPanel tabLayoutPanel;

    @UiField
    ToggleButton saveEditButton;

    @UiField
    CaptionPanel elementEditPanel;

    @UiField
    ToggleButton addButton;

    @UiField
    ToggleButton deleteButton;

    @UiField
    ListBox elementTypeBox;

    @UiField
    ToggleButton measureLengthButton;

    @UiField
    ToggleButton measureAreaButton;

    @UiField
    ToggleButton displayElevationButton;

    @UiField
    ToggleButton displayElevationProfileButton;

    @UiField
    ToggleButton flowlineButton;

    @UiField
    PushButton findNodeButton;

    @UiField
    PushButton findChannelButton;

    @UiField
    TextBox findTextBox;

    @UiField
    ScrollPanel infoPanel;

    @UiField
    Button cancelEditButton;

    @UiField
    ListBox schemeTypeBox;

    @UiField
    ListBox colorTypeBox;

    @UiField
    ToggleButton colorizeButton;

    @UiField
    FlowPanel colorPanel;

    @UiField
    TextBox minValueBox;

    @UiField
    TextBox maxValueBox;

    private HashMap<String, Integer> mapTypeToId;

    private DSM2GridMapDisplay display;

    public MapControlPanel(boolean viewOnly, DSM2GridMapDisplay dsm2GridMapDisplay) {
        display = dsm2GridMapDisplay;
        initWidget(uiBinder.createAndBindUi(this));
        mapTypeToId = new HashMap<String, Integer>();
        mapTypeToId.put("Node", ElementType.NODE);
        mapTypeToId.put("Channel", ElementType.CHANNEL);
        mapTypeToId.put("Reservoir", ElementType.RESERVOIR);
        mapTypeToId.put("Gate", ElementType.GATE);
        mapTypeToId.put("XSection", ElementType.XSECTION);
        String[] elementTypes = new String[] { "Node", "Channel", "Reservoir", "Gate", "XSection" };
        for (String item : elementTypes) {
            elementTypeBox.addItem(item);
        }
        elementEditPanel.setVisible(false);
        cancelEditButton.setVisible(false);
        saveEditButton.setEnabled(!viewOnly);
        saveEditButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                cancelEditButton.setVisible(saveEditButton.isDown());
                elementEditPanel.setVisible(saveEditButton.isDown());
            }
        });
        cancelEditButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                saveEditButton.setDown(false);
                cancelEditButton.setVisible(false);
                elementEditPanel.setVisible(false);
                infoPanel.clear();
            }
        });
        schemeTypeBox.addItem("SEQUENTIAL");
        schemeTypeBox.addItem("DIVERGING");
        schemeTypeBox.addItem("QUALITATIVE");
        colorizeButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                colorizeChannels(false);
            }
        });
        schemeTypeBox.addChangeHandler(new ChangeHandler() {

            public void onChange(ChangeEvent event) {
                colorizeChannels(false);
            }
        });
        colorTypeBox.addItem("Mannings");
        colorTypeBox.addItem("Dispersion");
        colorTypeBox.addChangeHandler(new ChangeHandler() {

            public void onChange(ChangeEvent event) {
                colorizeChannels(false);
            }
        });
        minValueBox.addChangeHandler(new ChangeHandler() {

            public void onChange(ChangeEvent event) {
                try {
                    Double.parseDouble(minValueBox.getText());
                    colorizeChannels(true);
                } catch (NumberFormatException ex) {
                    return;
                }
            }
        });
        maxValueBox.addChangeHandler(new ChangeHandler() {

            public void onChange(ChangeEvent event) {
                try {
                    Double.parseDouble(maxValueBox.getText());
                    colorizeChannels(true);
                } catch (NumberFormatException ex) {
                    return;
                }
            }
        });
    }

    public void colorizeChannels(boolean useMinMaxBoxValue) {
        colorPanel.clear();
        if (!colorizeButton.isDown()) {
            ChannelLineDataManager channelManager = display.getMapPanel().getChannelManager();
            for (Polyline line : channelManager.getPolylines()) {
                line.setStrokeStyle(channelManager.getPolylineStyle());
            }
            return;
        }
        HashMap<String, Double> channelIdToValueMap = getManningsMap();
        if (colorTypeBox.getItemText(colorTypeBox.getSelectedIndex()).equals("Dispersion")) {
            channelIdToValueMap = getDispersionMap();
        }
        String selected = schemeTypeBox.getItemText(schemeTypeBox.getSelectedIndex());
        String[] colors = ColorRangeMapper.SEQUENTIAL_COLORS;
        if (selected.equalsIgnoreCase("SEQUENTIAL")) {
            colors = ColorRangeMapper.SEQUENTIAL_COLORS;
        } else if (selected.equalsIgnoreCase("DIVERGING")) {
            colors = ColorRangeMapper.DIVERGING_COLORS;
        } else if (selected.equalsIgnoreCase("QUALITATIVE")) {
            colors = ColorRangeMapper.QUALITATIVE_COLORS;
        }
        ColorRangeMapper colorMapper = new ColorRangeMapper(colors);
        ChannelLineDataManager channelManager = display.getMapPanel().getChannelManager();
        double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
        if (useMinMaxBoxValue) {
            min = Double.parseDouble(minValueBox.getText());
            max = Double.parseDouble(maxValueBox.getText());
        } else {
            for (String id : channelIdToValueMap.keySet()) {
                Double value = channelIdToValueMap.get(id);
                max = Math.max(max, value);
                min = Math.min(min, value);
            }
            int l = colors.length - 2;
            max = Math.ceil(max * l) / l;
            min = Math.floor(min * l) / l;
            minValueBox.setText(formatter.format(min));
            maxValueBox.setText(formatter.format(max));
        }
        colorPanel.add(new ColorSchemePanel(colors, max, min));
        for (String id : channelIdToValueMap.keySet()) {
            Double value = channelIdToValueMap.get(id);
            String colorSpec = colorMapper.convertValueToColor(value, min, max);
            PolyStyleOptions styleOptions = PolyStyleOptions.newInstance(colorSpec);
            styleOptions.setOpacity(1.0);
            styleOptions.setWeight(3);
            channelManager.getPolyline(id).setStrokeStyle(styleOptions);
        }
    }

    public HashMap<String, Double> getManningsMap() {
        Channels channels = display.getMapPanel().getChannelManager().getChannels();
        HashMap<String, Double> map = new HashMap<String, Double>();
        for (Channel c : channels.getChannels()) {
            map.put(c.getId(), c.getMannings());
        }
        return map;
    }

    public HashMap<String, Double> getDispersionMap() {
        Channels channels = display.getMapPanel().getChannelManager().getChannels();
        HashMap<String, Double> map = new HashMap<String, Double>();
        for (Channel c : channels.getChannels()) {
            map.put(c.getId(), c.getDispersion());
        }
        return map;
    }

    public HasClickHandlers getAddButton() {
        return addButton;
    }

    public HasClickHandlers getDeleteButton() {
        return deleteButton;
    }

    /**
	 * Returns the type of element to be added
	 */
    public int getAddTypeSelected() {
        return mapTypeToId.get(elementTypeBox.getItemText(elementTypeBox.getSelectedIndex())).intValue();
    }

    public HasClickHandlers getSaveEditButton() {
        return saveEditButton;
    }

    public HasClickHandlers getCancelEditButton() {
        return cancelEditButton;
    }

    public Panel getInfoPanel() {
        return infoPanel;
    }

    public TextBox getFindTextBox() {
        return findTextBox;
    }

    public HasClickHandlers getFindNodeButton() {
        return findNodeButton;
    }

    public HasClickHandlers getFindChannelButton() {
        return findChannelButton;
    }

    public CaptionPanel getElementEditPanel() {
        return elementEditPanel;
    }

    public ListBox getElementTypeBox() {
        return elementTypeBox;
    }

    public ToggleButton getMeasureLengthButton() {
        return measureLengthButton;
    }

    public ToggleButton getMeasureAreaButton() {
        return measureAreaButton;
    }

    public ToggleButton getDisplayElevationButton() {
        return displayElevationButton;
    }

    public ToggleButton getDisplayElevationProfileButton() {
        return displayElevationProfileButton;
    }

    public ToggleButton getFlowlineButton() {
        return flowlineButton;
    }

    public HashMap<String, Integer> getMapTypeToId() {
        return mapTypeToId;
    }

    public void showEditPanel() {
        tabLayoutPanel.selectTab(0);
    }
}
