package ui.models;

import messages.ChannelData;
import messages.Layer;
import utility.Constants;
import utility.Conversion;
import utility.Logger;
import utility.Model;
import communications.CommunicationFactory;
import exceptions.LanboxCommunicationException;
import exceptions.LanboxGuiException;

/**
 * This model represents the internal state of the lanbox. It also reads
 * continuously from the lanbox to update it's status. Writing to the lanbox can
 * be done from elsewhere.
 * 
 * The idea is (when everything is completely implemented) that there exists a
 * one-to-one mapping between this object and the lanbox internals.
 * 
 * @author Jan Van Besien
 *  
 */
public class LanboxStatusModel extends Model {

    private boolean reading;

    private ChannelData channelData;

    private Layer[] layers;

    public LanboxStatusModel() {
        reading = false;
        layers = new Layer[0];
    }

    public void startReading() {
        reading = true;
        Thread t = new Thread(new LanboxReader());
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public void stopReading() {
        reading = false;
    }

    public boolean toggleReading() {
        if (reading) {
            stopReading();
        } else {
            startReading();
        }
        return reading;
    }

    /**
	 * Endlesly read status from lanbox and update status.
	 * 
	 * @author Jan Van Besien
	 *  
	 */
    private class LanboxReader implements Runnable {

        public void run() {
            while (reading) {
                long start = System.currentTimeMillis();
                try {
                    channelData = CommunicationFactory.getLanboxCommunication().channelReadData("FE", "01", Conversion.dec2hex("" + Constants.channels));
                    layers = CommunicationFactory.getLanboxCommunication().commonGetLayers();
                } catch (LanboxCommunicationException e) {
                    Logger.getLogger().log("TIMEOUT on command, retry...");
                    try {
                        channelData = CommunicationFactory.getLanboxCommunication().channelReadData("FE", "01", Conversion.dec2hex("" + Constants.channels));
                        layers = CommunicationFactory.getLanboxCommunication().commonGetLayers();
                    } catch (LanboxCommunicationException ex) {
                        Logger.getLogger().log("CONNECTION LOST, check cable");
                    }
                }
                fireStateChanged();
                long end = System.currentTimeMillis();
                System.out.println("loop took " + (end - start) + " ms");
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
	 * @return Returns the cue.
	 */
    public synchronized int getCue(String name) {
        return getLayer(name).getCue();
    }

    /**
	 * @return Returns the cuestep.
	 */
    public synchronized int getCuestep(String name) {
        return getLayer(name).getCueStep();
    }

    /**
	 * @return Returns the mixerChannelValues.
	 */
    public synchronized int[] getChannelValues() {
        return channelData.getValues();
    }

    public synchronized Layer getLayer(String name) {
        Layer l = null;
        for (int i = 0; i < layers.length; i++) if (layers[i].getName().equals(name)) l = layers[i];
        if (l == null) throw new LanboxGuiException("Layer " + name + " does not exist."); else return l;
    }

    public synchronized Layer[] getLayers() {
        return layers;
    }
}
