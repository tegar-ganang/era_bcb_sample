package se.sics.cooja.interfaces;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.log4j.Logger;
import org.jdom.Element;
import se.sics.cooja.Mote;
import se.sics.cooja.MoteTimeEvent;
import se.sics.cooja.RadioPacket;
import se.sics.cooja.Simulation;

/**
 * Application radio.
 * 
 * May be used by Java-based mote to implement radio functionality.
 * Supports radio channels and output power functionality.
 * The mote itself should observe the radio for incoming radio packet data.
 *
 * @author Fredrik Osterlind
 */
public class ApplicationRadio extends Radio {

    private static Logger logger = Logger.getLogger(ApplicationRadio.class);

    private Simulation simulation;

    private Mote mote;

    private RadioPacket packetFromMote = null;

    private RadioPacket packetToMote = null;

    private boolean isTransmitting = false;

    private boolean isReceiving = false;

    private boolean isInterfered = false;

    private long transmissionEndTime = 0;

    private RadioEvent lastEvent = RadioEvent.UNKNOWN;

    private long lastEventTime = 0;

    private double signalStrength = -100;

    private int radioChannel = -1;

    private double outputPower = 0;

    private int outputPowerIndicator = 100;

    private int interfered;

    public ApplicationRadio(Mote mote) {
        this.mote = mote;
        this.simulation = mote.getSimulation();
    }

    public RadioPacket getLastPacketTransmitted() {
        return packetFromMote;
    }

    public RadioPacket getLastPacketReceived() {
        return packetToMote;
    }

    public void setReceivedPacket(RadioPacket packet) {
        packetToMote = packet;
    }

    public void signalReceptionStart() {
        packetToMote = null;
        if (isInterfered() || isReceiving() || isTransmitting()) {
            interfereAnyReception();
            return;
        }
        isReceiving = true;
        lastEventTime = simulation.getSimulationTime();
        lastEvent = RadioEvent.RECEPTION_STARTED;
        this.setChanged();
        this.notifyObservers();
    }

    public void signalReceptionEnd() {
        if (isInterfered() || packetToMote == null) {
            interfered--;
            if (interfered == 0) isInterfered = false;
            if (interfered < 0) {
                isInterfered = false;
                interfered = 0;
            }
            packetToMote = null;
            if (interfered > 0) return;
        }
        isReceiving = false;
        lastEventTime = simulation.getSimulationTime();
        lastEvent = RadioEvent.RECEPTION_FINISHED;
        this.setChanged();
        this.notifyObservers();
    }

    public boolean isTransmitting() {
        return isTransmitting;
    }

    public long getTransmissionEndTime() {
        return transmissionEndTime;
    }

    public boolean isReceiving() {
        return isReceiving;
    }

    public int getChannel() {
        return radioChannel;
    }

    public Position getPosition() {
        return mote.getInterfaces().getPosition();
    }

    public RadioEvent getLastEvent() {
        return lastEvent;
    }

    public void interfereAnyReception() {
        interfered++;
        if (!isInterfered()) {
            isInterfered = true;
            lastEvent = RadioEvent.RECEPTION_INTERFERED;
            lastEventTime = simulation.getSimulationTime();
            this.setChanged();
            this.notifyObservers();
        }
    }

    public boolean isInterfered() {
        return isInterfered;
    }

    public double getCurrentOutputPower() {
        return outputPower;
    }

    public int getOutputPowerIndicatorMax() {
        return outputPowerIndicator;
    }

    public int getCurrentOutputPowerIndicator() {
        return outputPowerIndicator;
    }

    public double getCurrentSignalStrength() {
        return signalStrength;
    }

    public void setCurrentSignalStrength(double signalStrength) {
        this.signalStrength = signalStrength;
    }

    /**
   * Start transmitting given packet.
   *
   * @param packet Packet data
   * @param duration Duration to transmit
   */
    public void startTransmittingPacket(final RadioPacket packet, final long duration) {
        Runnable startTransmission = new Runnable() {

            public void run() {
                if (isTransmitting) {
                    logger.warn("Already transmitting, aborting new transmission");
                    return;
                }
                isTransmitting = true;
                lastEvent = RadioEvent.TRANSMISSION_STARTED;
                lastEventTime = simulation.getSimulationTime();
                ApplicationRadio.this.setChanged();
                ApplicationRadio.this.notifyObservers();
                packetFromMote = packet;
                lastEvent = RadioEvent.PACKET_TRANSMITTED;
                ApplicationRadio.this.setChanged();
                ApplicationRadio.this.notifyObservers();
                simulation.scheduleEvent(new MoteTimeEvent(mote, 0) {

                    public void execute(long t) {
                        isTransmitting = false;
                        lastEvent = RadioEvent.TRANSMISSION_FINISHED;
                        lastEventTime = t;
                        ApplicationRadio.this.setChanged();
                        ApplicationRadio.this.notifyObservers();
                    }
                }, simulation.getSimulationTime() + duration);
            }
        };
        if (simulation.isSimulationThread()) {
            startTransmission.run();
        } else {
            simulation.invokeSimulationThread(startTransmission);
        }
    }

    /**
   * @param i New output power indicator
   */
    public void setOutputPowerIndicator(int i) {
        outputPowerIndicator = i;
    }

    /**
   * @param p New output power
   */
    public void setOutputPower(int p) {
        outputPower = p;
    }

    /**
   * @param channel New radio channel
   */
    public void setChannel(int channel) {
        radioChannel = channel;
    }

    public JPanel getInterfaceVisualizer() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        final JLabel statusLabel = new JLabel("");
        final JLabel lastEventLabel = new JLabel("");
        final JLabel channelLabel = new JLabel("");
        final JLabel ssLabel = new JLabel("");
        final JButton updateButton = new JButton("Update SS");
        panel.add(statusLabel);
        panel.add(lastEventLabel);
        panel.add(ssLabel);
        panel.add(updateButton);
        panel.add(Box.createVerticalStrut(3));
        panel.add(channelLabel);
        panel.add(Box.createVerticalGlue());
        updateButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ssLabel.setText("Signal strength (not auto-updated): " + getCurrentSignalStrength() + " dBm");
            }
        });
        final Observer observer = new Observer() {

            public void update(Observable obs, Object obj) {
                if (isTransmitting()) {
                    statusLabel.setText("Transmitting");
                }
                if (isReceiving()) {
                    statusLabel.setText("Receiving");
                } else {
                    statusLabel.setText("Listening");
                }
                lastEventLabel.setText("Last event (time=" + lastEventTime + "): " + lastEvent);
                ssLabel.setText("Signal strength (not auto-updated): " + getCurrentSignalStrength() + " dBm");
            }
        };
        this.addObserver(observer);
        observer.update(null, null);
        panel.putClientProperty("intf_obs", observer);
        return panel;
    }

    public void releaseInterfaceVisualizer(JPanel panel) {
        Observer observer = (Observer) panel.getClientProperty("intf_obs");
        if (observer == null) {
            logger.fatal("Error when releasing panel, observer is null");
            return;
        }
        this.deleteObserver(observer);
    }

    public Collection<Element> getConfigXML() {
        return null;
    }

    public void setConfigXML(Collection<Element> configXML, boolean visAvailable) {
    }

    public Mote getMote() {
        return mote;
    }

    private boolean radioOn = true;

    public void setReceiverOn(boolean radioOn) {
        if (this.radioOn == radioOn) {
            return;
        }
        this.radioOn = radioOn;
        lastEvent = radioOn ? RadioEvent.HW_ON : RadioEvent.HW_OFF;
        lastEventTime = simulation.getSimulationTime();
        this.setChanged();
        this.notifyObservers();
    }

    public boolean isReceiverOn() {
        return radioOn;
    }
}
