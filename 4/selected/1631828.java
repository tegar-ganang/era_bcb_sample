package de.iritgo.openmetix.client.gui;

import de.iritgo.openmetix.app.AppPlugin;
import de.iritgo.openmetix.app.gagingstation.GagingStationRegistry;
import de.iritgo.openmetix.app.gagingstation.action.SendMeasurementServerAction;
import de.iritgo.openmetix.app.gui.IStationSensorSelector;
import de.iritgo.openmetix.core.gui.GUIPane;
import de.iritgo.openmetix.core.gui.swing.SwingGUIPane;
import de.iritgo.openmetix.core.iobject.IObject;
import de.iritgo.openmetix.core.logger.Log;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.core.tools.NumberTools;
import de.iritgo.openmetix.framework.action.ActionTools;
import de.iritgo.openmetix.framework.appcontext.AppContext;
import org.swixml.SwingEngine;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.sql.Timestamp;

/**
 * This gui pane lets the user manually enter measurement values.
 *
 * @version $Id: MeasurementRecordDialog.java,v 1.1 2005/04/24 18:10:42 grappendorf Exp $
 */
public class MeasurementRecordDialog extends SwingGUIPane {

    /** Contains the measurement value. */
    public JTextField value;

    /** Used to select the station and sensor for which to record a measurement. */
    public IStationSensorSelector stationSensorSelector;

    /**
	 * Create a new MeasurementRecordDialog.
	 */
    public MeasurementRecordDialog() {
        super("MeasurementRecordDialog");
    }

    /**
	 * Initialize the gui. Subclasses should override this method to create a
	 * custom gui.
	 */
    public void initGUI() {
        try {
            SwingEngine swingEngine = new SwingEngine(this);
            swingEngine.setClassLoader(AppPlugin.class.getClassLoader());
            JPanel panel = (JPanel) swingEngine.render(getClass().getResource("/swixml/MeasurementRecordDialog.xml"));
            content.add(panel, createConstraints(0, 0, 1, 1, GridBagConstraints.BOTH, 100, 100, null));
        } catch (Exception x) {
            Log.logError("client", "SimpleInstrumentConfigurator.initGUI", x.toString());
        }
    }

    /**
	 * Load the gui values from the data object attributes.
	 */
    public void loadFromObject() {
        stationSensorSelector.update();
    }

    /**
	 * Return a clone of this gui pane.
	 *
	 * @return The gui pane clone.
	 */
    public GUIPane cloneGUIPane() {
        return new MeasurementRecordDialog();
    }

    /**
	 * Return a sample of the data object that is displayed in this gui pane.
	 *
	 * @return The sample oject.
	 */
    public IObject getSampleObject() {
        return new GagingStationRegistry();
    }

    /**
	 * Send the measurement to the server.
	 */
    public Action sendAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            long stationId = stationSensorSelector.getSelectedStationId();
            long sensorId = stationSensorSelector.getSelectedSensorId();
            if (stationId != 0 && sensorId != 0) {
                ClientTransceiver transceiver = new ClientTransceiver(AppContext.instance().getChannelNumber());
                transceiver.addReceiver(AppContext.instance().getChannelNumber());
                SendMeasurementServerAction action = new SendMeasurementServerAction(new Timestamp(System.currentTimeMillis()), NumberTools.toDouble(value.getText(), 0.0), stationId, sensorId);
                action.setTransceiver(transceiver);
                ActionTools.sendToServer(action);
            }
        }
    };

    /**
	 * Close the dialog.
	 */
    public Action closeAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            display.close();
        }
    };
}
