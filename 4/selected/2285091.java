package gov.ca.bdo.modeling.dsm2.map.client.map;

import gov.ca.bdo.modeling.dsm2.map.client.map.CrossSectionEditorPanel.ElevationDataLoaded;
import gov.ca.dsm2.input.model.Channel;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class GenerateProfileForXSection {

    private CrossSectionEditorPanel xsEditorPanel;

    private MapPanel mapPanel;

    private Channel channel;

    private int xsindex;

    public GenerateProfileForXSection(CrossSectionEditorPanel xsEditorPanel, MapPanel mapPanel, Channel channel) {
        this.xsEditorPanel = xsEditorPanel;
        this.mapPanel = mapPanel;
        this.channel = channel;
        xsindex = 0;
    }

    public void generateNextProfile() {
        mapPanel.showMessage("Generating profile for xsection " + xsindex + " for channel: " + channel.getId());
        mapPanel.getInfoPanel().clear();
        mapPanel.getInfoPanel().add(xsEditorPanel);
        xsEditorPanel.draw(channel, xsindex, mapPanel, new ElevationDataLoaded() {

            public void elevationDataLoaded() {
                xsEditorPanel.snapToElevationProfile(mapPanel);
                xsEditorPanel.trimProfile(mapPanel);
                xsEditorPanel.updateProfile(mapPanel);
                xsindex++;
                if (xsindex < channel.getXsections().size()) {
                    Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                        public void execute() {
                            GenerateProfileForXSection.this.generateNextProfile();
                        }
                    });
                } else {
                    mapPanel.showMessage(xsindex + " cross sections generated for channel: " + channel.getId());
                    mapPanel.getInfoPanel().clear();
                    mapPanel.getChannelManager().getChannelClickHandler().drawXSectionLines(channel);
                }
            }
        });
    }
}
