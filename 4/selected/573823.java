package se.sics.mrm;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import org.apache.log4j.Logger;
import org.jdom.Element;
import se.sics.cooja.ClassDescription;
import se.sics.cooja.RadioConnection;
import se.sics.cooja.Simulation;
import se.sics.cooja.interfaces.Position;
import se.sics.cooja.interfaces.Radio;
import se.sics.cooja.radiomediums.AbstractRadioMedium;

/**
 * This is the main class of the COOJA Multi-path Ray-tracing Medium (MRM)
 * package.
 *
 * MRM is an alternative to the simpler radio mediums available in
 * COOJA. It is packet based and uses a 2D ray-tracing approach to approximate
 * signal strength attenuation between simulated radios. Currently the
 * ray-tracing only supports reflections and refractions through homogeneous
 * obstacles.
 *
 * MRM registers two plugins: a plugin for visualizing the radio
 * environments, and a plugin for configuring the radio medium parameters.
 *
 * Future work includes adding support for diffraction and scattering.
 *
 * @author Fredrik Osterlind
 */
@ClassDescription("Multi-path Ray-tracer Medium (MRM)")
public class MRM extends AbstractRadioMedium {

    private static Logger logger = Logger.getLogger(MRM.class);

    private ChannelModel currentChannelModel = null;

    private Random random = null;

    /**
   * Notifies observers when this radio medium has changed settings.
   */
    private SettingsObservable settingsObservable = new SettingsObservable();

    /**
   * Creates a new Multi-path Ray-tracing Medium (MRM).
   */
    public MRM(Simulation simulation) {
        super(simulation);
        random = simulation.getRandomGenerator();
        currentChannelModel = new ChannelModel();
        simulation.getGUI().registerTemporaryPlugin(AreaViewer.class);
        simulation.getGUI().registerTemporaryPlugin(FormulaViewer.class);
    }

    public MRMRadioConnection createConnections(Radio sendingRadio) {
        Position sendingPosition = sendingRadio.getPosition();
        MRMRadioConnection newConnection = new MRMRadioConnection(sendingRadio);
        for (Radio listeningRadio : getRegisteredRadios()) {
            if (sendingRadio == listeningRadio) {
                continue;
            }
            if (sendingRadio.getChannel() >= 0 && listeningRadio.getChannel() >= 0 && sendingRadio.getChannel() != listeningRadio.getChannel()) {
                continue;
            }
            double listeningPositionX = listeningRadio.getPosition().getXCoordinate();
            double listeningPositionY = listeningRadio.getPosition().getYCoordinate();
            double[] probData = currentChannelModel.getProbability(sendingPosition.getXCoordinate(), sendingPosition.getYCoordinate(), listeningPositionX, listeningPositionY, -Double.MAX_VALUE);
            if (random.nextFloat() < probData[0]) {
                if (listeningRadio.isInterfered()) {
                    newConnection.addInterfered(listeningRadio, probData[1]);
                } else if (listeningRadio.isReceiving()) {
                    newConnection.addInterfered(listeningRadio, probData[1]);
                    listeningRadio.interfereAnyReception();
                    MRMRadioConnection existingConn = null;
                    for (RadioConnection conn : getActiveConnections()) {
                        for (Radio dstRadio : ((MRMRadioConnection) conn).getDestinations()) {
                            if (dstRadio == listeningRadio) {
                                existingConn = (MRMRadioConnection) conn;
                                break;
                            }
                        }
                    }
                    if (existingConn != null) {
                        existingConn.addInterfered(listeningRadio);
                        listeningRadio.interfereAnyReception();
                    }
                } else {
                    newConnection.addDestination(listeningRadio, probData[1]);
                }
            } else if (probData[1] > currentChannelModel.getParameterDoubleValue("bg_noise_mean")) {
                newConnection.addInterfered(listeningRadio, probData[1]);
                listeningRadio.interfereAnyReception();
            }
        }
        return newConnection;
    }

    public void updateSignalStrengths() {
        for (Radio radio : getRegisteredRadios()) {
            radio.setCurrentSignalStrength(currentChannelModel.getParameterDoubleValue(("bg_noise_mean")));
        }
        for (RadioConnection conn : getActiveConnections()) {
            for (Radio dstRadio : ((MRMRadioConnection) conn).getDestinations()) {
                double signalStrength = ((MRMRadioConnection) conn).getDestinationSignalStrength(dstRadio);
                if (signalStrength > dstRadio.getCurrentSignalStrength()) {
                    dstRadio.setCurrentSignalStrength(signalStrength);
                }
            }
        }
        for (RadioConnection conn : getActiveConnections()) {
            for (Radio interferedRadio : ((MRMRadioConnection) conn).getInterfered()) {
                double signalStrength = ((MRMRadioConnection) conn).getInterferenceSignalStrength(interferedRadio);
                if (signalStrength > interferedRadio.getCurrentSignalStrength()) {
                    interferedRadio.setCurrentSignalStrength(signalStrength);
                }
                if (!interferedRadio.isInterfered()) {
                    interferedRadio.interfereAnyReception();
                }
            }
        }
    }

    public Collection<Element> getConfigXML() {
        return currentChannelModel.getConfigXML();
    }

    public boolean setConfigXML(Collection<Element> configXML, boolean visAvailable) {
        return currentChannelModel.setConfigXML(configXML);
    }

    /**
   * Adds an observer which is notified when this radio medium has
   * changed settings, such as added or removed radios.
   *
   * @param obs New observer
   */
    public void addSettingsObserver(Observer obs) {
        settingsObservable.addObserver(obs);
    }

    /**
   * Deletes an earlier registered setting observer.
   *
   * @param obs Earlier registered observer
   */
    public void deleteSettingsObserver(Observer obs) {
        settingsObservable.deleteObserver(obs);
    }

    /**
   * Returns position of given radio.
   *
   * @param radio Registered radio
   * @return Position of given radio
   */
    public Position getRadioPosition(Radio radio) {
        return radio.getPosition();
    }

    /**
   * @return Number of registered radios.
   */
    public int getRegisteredRadioCount() {
        return getRegisteredRadios().length;
    }

    /**
   * Returns radio at given index.
   *
   * @param index Index of registered radio.
   * @return Radio at given index
   */
    public Radio getRegisteredRadio(int index) {
        return getRegisteredRadios()[index];
    }

    /**
   * Returns the current channel model object, responsible for
   * all probability and transmission calculations.
   *
   * @return Current channel model
   */
    public ChannelModel getChannelModel() {
        return currentChannelModel;
    }

    class SettingsObservable extends Observable {

        private void notifySettingsChanged() {
            setChanged();
            notifyObservers();
        }
    }

    class MRMRadioConnection extends RadioConnection {

        private Hashtable<Radio, Double> signalStrengths = new Hashtable<Radio, Double>();

        public MRMRadioConnection(Radio sourceRadio) {
            super(sourceRadio);
        }

        public void addDestination(Radio radio, double signalStrength) {
            signalStrengths.put(radio, signalStrength);
            addDestination(radio);
        }

        public void addInterfered(Radio radio, double signalStrength) {
            signalStrengths.put(radio, signalStrength);
            addInterfered(radio);
        }

        public double getDestinationSignalStrength(Radio radio) {
            return signalStrengths.get(radio);
        }

        public double getInterferenceSignalStrength(Radio radio) {
            return signalStrengths.get(radio);
        }
    }
}
