package de.jlab.ui.modules.dds;

import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import javax.swing.JComponent;
import de.jlab.boards.Board;
import de.jlab.lab.Lab;
import de.jlab.ui.modules.UILabModule;
import de.jlab.ui.modules.panels.dds.DDSSweepPanel;
import de.jlab.ui.valuewatch.ValueWatch;
import de.jlab.ui.valuewatch.ValueWatchManager;

public class DdsSweep implements UILabModule {

    public static final String PARAMETER_MEASUREMENT_CHANNEL = "MeasurementChannel";

    public static final String PARAMETER_MEASUREMENT_ADDRESS = "MeasurementAddress";

    Lab theLab = null;

    Board theBoard = null;

    ValueWatchManager vwManager = null;

    public DdsSweep() {
        super();
    }

    public void close(Component comp) {
    }

    public JComponent createLabComponent() {
        DDSSweepPanel ddsPanel = new DDSSweepPanel(theLab, theBoard, vwManager);
        return ddsPanel;
    }

    public Board getBoard() {
        return theBoard;
    }

    public String getMenuPath() {
        return theBoard.getBoardInstanceIdentifierForMenu();
    }

    public String getId() {
        return "Sweep";
    }

    public HashMap<String, String> getParametersForUIComponent(Component comp) {
        HashMap<String, String> parameterMap = new HashMap<String, String>();
        parameterMap.put(PARAMETER_MEASUREMENT_CHANNEL, ((DDSSweepPanel) comp).getMeasurement().getCommChannel().getChannelName() + "");
        parameterMap.put(PARAMETER_MEASUREMENT_ADDRESS, ((DDSSweepPanel) comp).getMeasurement().getAddress() + "");
        return parameterMap;
    }

    public List<ValueWatch> getValueWatches() {
        return null;
    }

    public void setParametersForUIComponent(Component comp, HashMap<String, String> parameters) {
        Board measurementBoard = null;
        if (parameters != null) {
            String mchannelstring = parameters.get(PARAMETER_MEASUREMENT_CHANNEL);
            String maddressstring = parameters.get(PARAMETER_MEASUREMENT_ADDRESS);
            if (mchannelstring != null && maddressstring != null) {
                try {
                    int address = Integer.parseInt(maddressstring);
                    measurementBoard = theLab.getBoardForCommChannelNameAndAddress(mchannelstring, address);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        ((DDSSweepPanel) comp).setMeasurement(measurementBoard);
    }

    public void sleep(Component comp) {
    }

    public void wakeup(Component comp) {
    }

    public void init(Lab lab, Board aBoard, ValueWatchManager vwManager) {
        this.theLab = lab;
        this.theBoard = aBoard;
        this.vwManager = vwManager;
    }
}
