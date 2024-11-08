package jhomenet.ui.panel.plot;

import java.util.List;
import java.beans.PropertyChangeEvent;
import org.apache.log4j.Logger;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import jhomenet.commons.ServerContext;
import jhomenet.commons.hw.sensor.*;
import jhomenet.commons.hw.data.*;

/**
 * The default state sensor plot panel.
 * 
 * @author David Irwin (jhomenet at gmail dot com)
 */
public class StateSensorPlot extends AbstractPlot {

    /**
	 * The logger
	 */
    private static Logger logger = Logger.getLogger(StateSensorPlot.class);

    /**
	 * Default constructor.
	 * 
	 * @param sensor
	 * @param stateAdapter
	 */
    public StateSensorPlot(StateSensor sensor, ServerContext serverContext) {
        super((Sensor) sensor, serverContext);
        for (int channel = 0; channel < sensor.getNumChannels(); channel++) {
            timeseriesCollection.addSeries(new TimeSeries(sensor.getChannelDescription(channel) + " [CH-" + channel + "]", org.jfree.data.time.Second.class));
        }
        plotData(this.sensor.getCurrentData());
    }

    /**
	 * @see jhomenet.ui.panel.plot.AbstractPlot#addDataInternal(org.jfree.data.time.TimeSeries, jhomenet.commons.hw.data.AbstractHardwareData)
	 */
    @Override
    void addDataInternal(TimeSeries ts, HardwareData data) {
        ts.add(new Second(data.getTimestamp()), ((HardwareStateData) data).getDataObject().getValue());
    }

    /**
	 * Get the desired Y-label.
	 */
    @Override
    protected String getYLabel() {
        return "State";
    }

    /**
	 * @see jhomenet.ui.panel.plot.AbstractPlot#propertyChange(java.beans.PropertyChangeEvent)
	 */
    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        logger.debug("Received property change event: " + propertyChangeEvent.getPropertyName());
        Object obj = propertyChangeEvent.getNewValue();
        if (obj instanceof HardwareStateData) {
            HardwareStateData data = (HardwareStateData) obj;
            logger.debug("New data: " + data.toString());
            plotData(data);
        } else if (obj instanceof List) {
            List<HardwareData> dataList = (List<HardwareData>) obj;
            logger.debug("New data list");
            plotData(dataList);
        }
    }
}
