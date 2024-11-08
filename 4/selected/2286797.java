package se.sics.cooja.emulatedmote;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import org.apache.log4j.Logger;
import org.jdom.Element;
import se.sics.cooja.*;
import se.sics.cooja.interfaces.CustomDataRadio;
import se.sics.cooja.interfaces.Position;
import se.sics.cooja.interfaces.Radio;

/**
 * 802.15.4 radio class for COOJA.
 *
 * @author Joakim Eriksson
 */
public abstract class Radio802154 extends Radio implements CustomDataRadio {

    private static final boolean DEBUG = false;

    private static Logger logger = Logger.getLogger(Radio802154.class);

    protected long lastEventTime = 0;

    protected RadioEvent lastEvent = RadioEvent.UNKNOWN;

    protected boolean isInterfered = false;

    private boolean isTransmitting = false;

    protected boolean isReceiving = false;

    private boolean radioOn = true;

    private RadioByte lastOutgoingByte = null;

    private RadioByte lastIncomingByte = null;

    private RadioPacket lastOutgoingPacket = null;

    private RadioPacket lastIncomingPacket = null;

    protected Mote mote;

    public Radio802154(Mote mote) {
        this.mote = mote;
    }

    int len = 0;

    int expLen = 0;

    byte[] buffer = new byte[127 + 15];

    protected void handleTransmit(byte val) {
        if (len == 0) {
            lastEventTime = mote.getSimulation().getSimulationTime();
            lastEvent = RadioEvent.TRANSMISSION_STARTED;
            if (DEBUG) logger.debug("----- 802.15.4 TRANSMISSION STARTED -----");
            setChanged();
            notifyObservers();
        }
        lastOutgoingByte = new RadioByte(val);
        lastEventTime = mote.getSimulation().getSimulationTime();
        lastEvent = RadioEvent.CUSTOM_DATA_TRANSMITTED;
        setChanged();
        notifyObservers();
        buffer[len++] = val;
        if (len == 6) {
            expLen = val + 6;
        }
        if (len == expLen) {
            if (DEBUG) logger.debug("----- 802.15.4 CUSTOM DATA TRANSMITTED -----");
            lastOutgoingPacket = Radio802154PacketConverter.fromCC2420ToCooja(buffer);
            lastEventTime = mote.getSimulation().getSimulationTime();
            lastEvent = RadioEvent.PACKET_TRANSMITTED;
            if (DEBUG) logger.debug("----- 802.15.4 PACKET TRANSMITTED -----");
            setChanged();
            notifyObservers();
            lastEventTime = mote.getSimulation().getSimulationTime();
            lastEvent = RadioEvent.TRANSMISSION_FINISHED;
            setChanged();
            notifyObservers();
            len = 0;
        }
    }

    public RadioPacket getLastPacketTransmitted() {
        return lastOutgoingPacket;
    }

    public RadioPacket getLastPacketReceived() {
        return lastIncomingPacket;
    }

    public void setReceivedPacket(RadioPacket packet) {
    }

    public Object getLastCustomDataTransmitted() {
        return lastOutgoingByte;
    }

    public Object getLastCustomDataReceived() {
        return lastIncomingByte;
    }

    public void receiveCustomData(Object data) {
        if (data instanceof RadioByte) {
            lastIncomingByte = (RadioByte) data;
            handleReceive(lastIncomingByte.getPacketData()[0]);
        }
    }

    public boolean isTransmitting() {
        return isTransmitting;
    }

    public boolean isReceiving() {
        return isReceiving;
    }

    public boolean isInterfered() {
        return isInterfered;
    }

    protected abstract void handleReceive(byte b);

    protected abstract void handleEndOfReception();

    public abstract int getChannel();

    public abstract int getFrequency();

    public abstract boolean isReceiverOn();

    public abstract double getCurrentOutputPower();

    public abstract int getCurrentOutputPowerIndicator();

    public abstract int getOutputPowerIndicatorMax();

    public abstract double getCurrentSignalStrength();

    public abstract void setCurrentSignalStrength(double signalStrength);

    public void signalReceptionStart() {
        isReceiving = true;
        lastEventTime = mote.getSimulation().getSimulationTime();
        lastEvent = RadioEvent.RECEPTION_STARTED;
        if (DEBUG) logger.debug("----- 802.15.4 RECEPTION STARTED -----");
        setChanged();
        notifyObservers();
    }

    public void signalReceptionEnd() {
        isReceiving = false;
        isInterfered = false;
        handleEndOfReception();
        lastEventTime = mote.getSimulation().getSimulationTime();
        lastEvent = RadioEvent.RECEPTION_FINISHED;
        if (DEBUG) logger.debug("----- 802.15.4 RECEPTION FINISHED -----");
        setChanged();
        notifyObservers();
    }

    public RadioEvent getLastEvent() {
        return lastEvent;
    }

    public void interfereAnyReception() {
        isInterfered = true;
        isReceiving = false;
        lastIncomingPacket = null;
        handleEndOfReception();
        lastEventTime = mote.getSimulation().getSimulationTime();
        lastEvent = RadioEvent.RECEPTION_INTERFERED;
        setChanged();
        notifyObservers();
    }

    public JPanel getInterfaceVisualizer() {
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new GridLayout(5, 2));
        final JLabel statusLabel = new JLabel("");
        final JLabel lastEventLabel = new JLabel("");
        final JLabel channelLabel = new JLabel("");
        final JLabel powerLabel = new JLabel("");
        final JLabel ssLabel = new JLabel("");
        final JButton updateButton = new JButton("Update");
        panel.add(new JLabel("STATE:"));
        panel.add(statusLabel);
        panel.add(new JLabel("LAST EVENT:"));
        panel.add(lastEventLabel);
        panel.add(new JLabel("CHANNEL:"));
        panel.add(channelLabel);
        panel.add(new JLabel("OUTPUT POWER:"));
        panel.add(powerLabel);
        panel.add(new JLabel("SIGNAL STRENGTH:"));
        JPanel smallPanel = new JPanel(new GridLayout(1, 2));
        smallPanel.add(ssLabel);
        smallPanel.add(updateButton);
        panel.add(smallPanel);
        updateButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                channelLabel.setText(getChannel() + " (freq=" + getFrequency() + " MHz)");
                powerLabel.setText(getCurrentOutputPower() + " dBm (indicator=" + getCurrentOutputPowerIndicator() + "/" + getOutputPowerIndicatorMax() + ")");
                ssLabel.setText(getCurrentSignalStrength() + " dBm");
            }
        });
        Observer observer;
        this.addObserver(observer = new Observer() {

            public void update(Observable obs, Object obj) {
                if (isTransmitting()) {
                    statusLabel.setText("transmitting");
                } else if (isReceiving()) {
                    statusLabel.setText("receiving");
                } else if (radioOn) {
                    statusLabel.setText("listening for traffic");
                } else {
                    statusLabel.setText("HW off");
                }
                lastEventLabel.setText(lastEvent + " @ time=" + lastEventTime);
                channelLabel.setText(getChannel() + " (freq=" + getFrequency() + " MHz)");
                powerLabel.setText(getCurrentOutputPower() + " dBm (indicator=" + getCurrentOutputPowerIndicator() + "/" + getOutputPowerIndicatorMax() + ")");
                ssLabel.setText(getCurrentSignalStrength() + " dBm");
            }
        });
        observer.update(null, null);
        wrapperPanel.add(BorderLayout.NORTH, panel);
        wrapperPanel.putClientProperty("intf_obs", observer);
        return wrapperPanel;
    }

    public void releaseInterfaceVisualizer(JPanel panel) {
        Observer observer = (Observer) panel.getClientProperty("intf_obs");
        if (observer == null) {
            logger.fatal("Error when releasing panel, observer is null");
            return;
        }
        this.deleteObserver(observer);
    }

    public Mote getMote() {
        return mote;
    }

    public Position getPosition() {
        return mote.getInterfaces().getPosition();
    }

    public Collection<Element> getConfigXML() {
        return null;
    }

    public void setConfigXML(Collection<Element> configXML, boolean visAvailable) {
    }
}
