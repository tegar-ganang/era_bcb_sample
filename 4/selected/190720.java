package ru.cos.sim.visualizer.traffic.simulation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import ru.cos.cs.agents.framework.asynch.Message;
import ru.cos.sim.communication.FrameProperties;
import ru.cos.sim.communication.dto.FrameData;
import ru.cos.sim.communication.dto.MeterDTO;
import ru.cos.sim.communication.dto.MeterShortData;
import ru.cos.sim.communication.messages.AbstractMessage;
import ru.cos.sim.communication.messages.AvailableMetersMessage;
import ru.cos.sim.communication.messages.AvailableMetersRequest;
import ru.cos.sim.communication.messages.FrameDataMessage;
import ru.cos.sim.communication.messages.FrameDataRequest;
import ru.cos.sim.communication.messages.FramePropertiesMessage;
import ru.cos.sim.communication.messages.MeterDataMessage;
import ru.cos.sim.communication.messages.MeterDataRequest;
import ru.cos.sim.engine.TrafficAsynchSimulationEngine;
import ru.cos.sim.mdf.MDFReader;
import ru.cos.sim.visualizer.exception.VisualizerEngineException;

/**
 * Visual controller responsible for communication with asynchronously running 
 * Traffic Simulation Engine.<br>
 * Traffic Simulation Engine operates in a separate thread that is started by Visual Controller,
 * while Visual Controller executes in the client thread.
 * Communication between TSE and VC are performed by means of sending and receiving asynchronous messages.
 * <p>
 * VC provides set of methods for requesting and receiving information from TSE.<br>
 * The general assumption under receiving some abstract data structure DATA is follows:
 * <ul>
 * <li>method requestDATA requests DATA from TSE</li>
 * <li>method getDATA reads received from TSE DATA.</li>
 * </ul>
 * Because TSE runs asynchronously, after requesting DATA it need some time to request will be processed by TSE
 * and result be available, therefore getDATA method may return null value - it means that DATA is not yet ready
 * or was not requested at all.
 * <p>
 * @author zroslaw
 */
public class VisualController {

    /**
	 * VC implements Singleton pattern.
	 */
    private static VisualController instance;

    /**
	 * Instance of asynchronous TSE.
	 */
    private TrafficAsynchSimulationEngine simulationEngine;

    /**
	 * Instance of the thread in which TSE is running.
	 */
    private Thread simulationEngineThread;

    private FrameData lastFrameData = null;

    private Set<MeterShortData> lastAvailableMeters;

    private Map<Integer, MeterDTO> lastMetersData = new HashMap<Integer, MeterDTO>();

    /**
	 * VC implements Singleton pattern.
	 */
    public static VisualController getInstance() {
        if (instance == null) instance = new VisualController();
        return instance;
    }

    /**
	 * Initialize controller and simulation engine by MDF file.
	 * @param file MDF file instance
	 */
    public void init(File file) {
        InputStream is = null;
        ByteArrayOutputStream os = null;
        try {
            is = new FileInputStream(file);
            os = new ByteArrayOutputStream();
            IOUtils.copy(is, os);
        } catch (Throwable e) {
            throw new VisualizerEngineException("Unexcpected exception while reading MDF file", e);
        }
        if (simulationEngine != null) simulationEngine.stopSimulation();
        simulationEngine = new TrafficAsynchSimulationEngine();
        simulationEngine.init(MDFReader.read(os.toByteArray()));
        simulationEngineThread = null;
    }

    /**
	 * Run simulation.<br>
	 * Method will start new simulation thread, if it is not started yet,
	 * and start simulation process.<br>
	 * Method can be used for resuming simulation process that was paused too.
	 */
    public void run() {
        if (simulationEngineThread == null) {
            simulationEngineThread = new Thread(simulationEngine);
            simulationEngineThread.start();
        }
        simulationEngine.resumeSimulation();
    }

    /**
	 * Method checks if simulation process now running.<br>
	 * Method must return false when simulation engine is alive but paused.</b>
	 * @return true if any simulation procedures now running and state of simulation objects are changing,
	 * false otherwise.
	 */
    public boolean isSimulationRunning() {
        return !simulationEngine.isPaused() && !simulationEngine.isStopped();
    }

    /**
	 * Check if simulation alive.
	 * True will be returned even when simulation process is paused.
	 * False will be returned only when simulation process thread will die.
	 * @return
	 */
    public boolean isSimulationAlive() {
        return simulationEngineThread == null || simulationEngineThread.isAlive();
    }

    /**
	 * Pause simulation process
	 */
    public void pause() {
        simulationEngine.pauseSimulation();
    }

    /**
	 * Stop simulation process
	 */
    public void stop() {
        simulationEngine.stopSimulation();
    }

    /**
	 * Set time wrap factor
	 * @param wrapFactor time wrap factor to set
	 */
    public void setTimeWrapFactor(float wrapFactor) {
        simulationEngine.setTimeWrapFactor(wrapFactor);
    }

    /**
	 * Requesting Frame Data
	 */
    public void requestFrameData() {
        simulationEngine.sendMessage(new FrameDataRequest());
    }

    /**
	 * Get frame data that was received from TSE.
	 * Method {@linkplain #requestFrameData()} must be invoked to request data from TSE.
	 * Null will be returned if there is no response from TSE.
	 * @return list of abstract data objects or null if there is no response from TSE.
	 */
    public FrameData getFrameData() {
        processResponseMessages();
        FrameData result = lastFrameData;
        lastFrameData = null;
        return result;
    }

    /**
	 * Process all response messages from TSE and set appropriate data variables.
	 */
    public void processResponseMessages() {
        Message message;
        while ((message = simulationEngine.receiveMessage()) != null) {
            AbstractMessage abstractMessage = (AbstractMessage) message;
            switch(abstractMessage.getMessageType()) {
                case FRAME_DATA:
                    FrameDataMessage frameDataMessage = (FrameDataMessage) abstractMessage;
                    lastFrameData = frameDataMessage.getFrameData();
                    break;
                case AVAILABLE_METERS:
                    AvailableMetersMessage availableMetersMessage = (AvailableMetersMessage) abstractMessage;
                    lastAvailableMeters = availableMetersMessage.getMeterShortData();
                    break;
                case METER_DATA:
                    MeterDataMessage meterDataMessage = (MeterDataMessage) abstractMessage;
                    MeterDTO meterData = meterDataMessage.getMeterData();
                    lastMetersData.put(meterData.getId(), meterData);
                    break;
                default:
                    throw new VisualControllerException("Unexpected message type " + abstractMessage.getMessageType());
            }
        }
    }

    /**
	 * Request for available meters in the simulation system.
	 */
    public void requestAvailableMeters() {
        simulationEngine.sendMessage(new AvailableMetersRequest());
    }

    /**
	 * Read available meters list
	 * @return list of available meters or null if there is no response from TSE yet
	 */
    public Set<MeterShortData> getAvailableMeters() {
        Set<MeterShortData> result = lastAvailableMeters;
        lastAvailableMeters = null;
        return result;
    }

    /**
	 * Request data from a particular meter.
	 * @param meterId is of the meter to request data from
	 */
    public void requestMeterData(int meterId) {
        simulationEngine.sendMessage(new MeterDataRequest(meterId));
    }

    /**
	 * Read meter data.
	 * @param meterId id of the meter to read data from
	 * @return particular meter data or null there is no response from TSE yet
	 */
    public MeterDTO getMeterData(int meterId) {
        MeterDTO result = lastMetersData.get(meterId);
        lastMetersData.remove(meterId);
        return result;
    }

    /**
	 * Request for setting specific frame properties.
	 * @param frameProperties frame properties to set
	 */
    public void setFrameProperties(FrameProperties frameProperties) {
        simulationEngine.sendMessage(new FramePropertiesMessage(frameProperties));
    }

    public static void dispose() {
        instance = null;
    }
}
