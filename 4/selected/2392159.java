package gov.ca.bdo.modeling.dsm2.map.client.map;

import gov.ca.bdo.modeling.dsm2.map.client.map.CrossSectionEditorPanel.ElevationDataLoaded;
import gov.ca.dsm2.input.model.Channel;
import gov.ca.dsm2.input.model.Node;
import gov.ca.dsm2.input.model.XSection;
import gov.ca.dsm2.input.model.XSectionLayer;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.taskdefs.Sleep;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.visualizations.ScatterChart;
import com.google.gwt.visualization.client.visualizations.ScatterChart.Options;

public class ChannelInfoPanel extends Composite {

    private FlowPanel xsectionPanel;

    private FlowPanel xsectionDisclosure;

    private FlowPanel xsectionContainerPanel;

    private MapPanel mapPanel;

    public ChannelInfoPanel(Channel channel, MapPanel mapPanel) {
        this.mapPanel = mapPanel;
        xsectionContainerPanel = new FlowPanel();
        xsectionPanel = new FlowPanel();
        xsectionContainerPanel.add(xsectionPanel);
        drawXSection(channel, -1);
        VerticalPanel vpanel = new VerticalPanel();
        FlowPanel basicPanel = new FlowPanel();
        basicPanel.add(getBasicInfoPanel(channel));
        vpanel.add(basicPanel);
        xsectionDisclosure = new FlowPanel();
        xsectionDisclosure.add(xsectionContainerPanel);
        vpanel.add(xsectionDisclosure);
        initWidget(vpanel);
    }

    /**
	 * Creates a profile of the xsection perpendicular to the flow line for the
	 * channel
	 * 
	 * @param channel
	 * @param index
	 *            , the index of the xsection to be drawn or -1 for all
	 * @return
	 */
    public void drawXSection(Channel channel, int index) {
        String title = "Channel: " + channel.getId() + " Length: " + channel.getLength() + " X-Section View";
        Options options = Options.create();
        options.setHeight(350);
        options.setTitle(title);
        options.setTitleX("Centered Width");
        options.setTitleY("Elevation (ft)");
        options.setWidth(500);
        options.setLineSize(1);
        options.setLegend(LegendPosition.BOTTOM);
        options.setShowCategories(false);
        DataTable table = DataTable.create();
        table.addColumn(ColumnType.NUMBER, "Width");
        int i = 0;
        int numberOfXSections = channel.getXsections().size();
        String[] colors = new String[numberOfXSections];
        int actualCount = 0;
        for (XSection xsection : channel.getXsections()) {
            if (((index >= 0) && (index < numberOfXSections)) && (i != index)) {
                i++;
                continue;
            }
            double distance = xsection.getDistance();
            colors[actualCount] = "#" + getHexString((int) Math.round(255 * distance)) + "33" + getHexString((int) Math.round(255 - 255 * distance));
            i++;
            actualCount++;
            table.addColumn(ColumnType.NUMBER, " @ " + distance);
            for (XSectionLayer layer : xsection.getLayers()) {
                double elevation = layer.getElevation();
                double topWidth = layer.getTopWidth();
                table.insertRows(0, 1);
                table.setValue(0, actualCount, elevation);
                table.setValue(0, 0, -topWidth / 2);
                int nrows = table.getNumberOfRows();
                if (nrows >= table.getNumberOfRows()) {
                    table.insertRows(nrows, 1);
                }
                table.setValue(nrows, actualCount, elevation);
                table.setValue(nrows, 0, topWidth / 2);
            }
        }
        String[] cArray = new String[actualCount];
        System.arraycopy(colors, 0, cArray, 0, actualCount);
        options.setColors(cArray);
        ScatterChart chart = new ScatterChart(table, options);
        xsectionPanel.setVisible(false);
        xsectionPanel.clear();
        xsectionPanel.add(chart);
        xsectionPanel.setVisible(true);
    }

    private String getHexString(int value) {
        String hexString = Integer.toHexString(value);
        if (hexString.length() == 1) {
            hexString = "0" + hexString;
        }
        return hexString;
    }

    private Panel getBasicInfoPanel(final Channel channel) {
        if (mapPanel.isInEditMode()) {
            FlexTable table = new FlexTable();
            table.setHTML(0, 0, "<h3>Channel " + channel.getId() + "[ " + channel.getUpNodeId() + "->" + channel.getDownNodeId() + " ]" + "</h3>");
            table.getFlexCellFormatter().setColSpan(0, 0, 2);
            table.setHTML(1, 0, "Length");
            table.setHTML(1, 1, channel.getLength() + "");
            table.setHTML(2, 0, "Mannings");
            TextBox manningsBox = new TextBox();
            manningsBox.addValueChangeHandler(new ValueChangeHandler<String>() {

                public void onValueChange(ValueChangeEvent<String> event) {
                    double value = Double.parseDouble(event.getValue());
                    value = Math.round(value * 1000) / 1000.0;
                    channel.setMannings(value);
                }
            });
            manningsBox.setText(channel.getMannings() + "");
            table.setWidget(2, 1, manningsBox);
            table.setHTML(3, 0, "Dispersion");
            TextBox dispersionBox = new TextBox();
            dispersionBox.setText(channel.getDispersion() + "");
            dispersionBox.addValueChangeHandler(new ValueChangeHandler<String>() {

                public void onValueChange(ValueChangeEvent<String> event) {
                    double value = Double.parseDouble(event.getValue());
                    value = Math.round(value * 1000) / 1000.0;
                    channel.setDispersion(value);
                }
            });
            table.setWidget(3, 1, dispersionBox);
            table.setWidget(1, 2, getXSectionGenerationPanel(channel));
            table.getFlexCellFormatter().setColSpan(1, 2, 2);
            table.getFlexCellFormatter().setRowSpan(1, 2, 2);
            return table;
        } else {
            return new HTMLPanel("<h3>Channel " + channel.getId() + "[ " + channel.getUpNodeId() + "->" + channel.getDownNodeId() + " ]" + "</h3>" + "<table>" + "<tr><td>Length</td><td>" + channel.getLength() + "</td></tr>" + "<tr><td>Mannings</td><td>" + channel.getMannings() + "</td></tr>" + "<tr><td>Dispersion</td><td>" + channel.getDispersion() + "</td></tr>" + "</table>");
        }
    }

    private Panel getXSectionGenerationPanel(final Channel channel) {
        final Node upNode = mapPanel.getNodeManager().getNodes().getNode(channel.getUpNodeId());
        final Node downNode = mapPanel.getNodeManager().getNodes().getNode(channel.getDownNodeId());
        Panel panel = new FlowPanel();
        Button generateXSectionsButton = new Button("Generate XSections");
        Button clearXSectionsButton = new Button("Clear XSections");
        panel.add(generateXSectionsButton);
        panel.add(clearXSectionsButton);
        generateXSectionsButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                List<XSection> generated = ModelUtils.generateCrossSections(channel, upNode, downNode, 5000.0, 3000.0);
                for (XSection x : generated) {
                    channel.addXSection(x);
                }
                mapPanel.getChannelManager().drawXSectionLines(channel, ChannelInfoPanel.this);
                mapPanel.showMessage("Generated xsections with minimum spacing of 5000ft for channel");
                final CrossSectionEditorPanel xsEditorPanel = mapPanel.getChannelManager().getXSectionLineClickHandler().getXsEditorPanel();
                new GenerateProfileForXSection(xsEditorPanel, mapPanel, channel).generateNextProfile();
            }
        });
        clearXSectionsButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                mapPanel.getChannelManager().clearXSections();
                mapPanel.showMessage("Removed all xsections for selected channel");
            }
        });
        return panel;
    }
}
