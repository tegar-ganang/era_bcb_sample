package se.sics.cooja.mspmote.interfaces;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.log4j.Logger;
import org.jdom.Element;
import se.sics.cooja.ClassDescription;
import se.sics.cooja.Mote;
import se.sics.cooja.RadioPacket;
import se.sics.cooja.Simulation;
import se.sics.cooja.interfaces.CustomDataRadio;
import se.sics.cooja.interfaces.Position;
import se.sics.cooja.interfaces.Radio;
import se.sics.cooja.mspmote.MspMoteTimeEvent;
import se.sics.cooja.mspmote.SkyMote;
import se.sics.mspsim.chip.CC2420;
import se.sics.mspsim.chip.RFListener;
import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.OperatingModeListener;

/**
 * CC2420 to COOJA wrapper.
 *
 * @author Fredrik Osterlind
 */
@ClassDescription("CC2420")
public class SkyByteRadio extends Radio implements CustomDataRadio {

    private static Logger logger = Logger.getLogger(SkyByteRadio.class);

    /**
   * Cross-level:
   * Inter-byte delay for delivering cross-level packet bytes.
   */
    public static final long DELAY_BETWEEN_BYTES = (long) (1000.0 * Simulation.MILLISECOND / (250000.0 / 8.0));

    private long lastEventTime = 0;

    private RadioEvent lastEvent = RadioEvent.UNKNOWN;

    private SkyMote mote;

    private CC2420 cc2420;

    private boolean isInterfered = false;

    private boolean isTransmitting = false;

    private boolean isReceiving = false;

    private byte lastOutgoingByte;

    private byte lastIncomingByte;

    private RadioPacket lastOutgoingPacket = null;

    private RadioPacket lastIncomingPacket = null;

    public SkyByteRadio(Mote mote) {
        this.mote = (SkyMote) mote;
        this.cc2420 = this.mote.skyNode.radio;
        cc2420.setRFListener(new RFListener() {

            int len = 0;

            int expLen = 0;

            byte[] buffer = new byte[127 + 15];

            public void receivedByte(byte data) {
                if (!isTransmitting()) {
                    lastEventTime = SkyByteRadio.this.mote.getSimulation().getSimulationTime();
                    lastEvent = RadioEvent.TRANSMISSION_STARTED;
                    isTransmitting = true;
                    setChanged();
                    notifyObservers();
                }
                if (len >= buffer.length) {
                    logger.debug("Error: bad size: " + len + ", dropping outgoing byte: " + data);
                    return;
                }
                lastOutgoingByte = data;
                lastEventTime = SkyByteRadio.this.mote.getSimulation().getSimulationTime();
                lastEvent = RadioEvent.CUSTOM_DATA_TRANSMITTED;
                setChanged();
                notifyObservers();
                buffer[len++] = data;
                if (len == 6) {
                    expLen = data + 6;
                }
                if (len == expLen) {
                    lastOutgoingPacket = CC2420RadioPacketConverter.fromCC2420ToCooja(buffer);
                    lastEventTime = SkyByteRadio.this.mote.getSimulation().getSimulationTime();
                    lastEvent = RadioEvent.PACKET_TRANSMITTED;
                    setChanged();
                    notifyObservers();
                    lastEventTime = SkyByteRadio.this.mote.getSimulation().getSimulationTime();
                    isTransmitting = false;
                    lastEvent = RadioEvent.TRANSMISSION_FINISHED;
                    setChanged();
                    notifyObservers();
                    len = 0;
                }
            }
        });
        cc2420.addOperatingModeListener(new OperatingModeListener() {

            public void modeChanged(Chip source, int mode) {
                if (isReceiverOn()) {
                    lastEvent = RadioEvent.HW_ON;
                } else {
                    if (isTransmitting()) {
                        logger.fatal("Turning off radio while transmitting");
                        lastEventTime = SkyByteRadio.this.mote.getSimulation().getSimulationTime();
                        isTransmitting = false;
                        lastEvent = RadioEvent.TRANSMISSION_FINISHED;
                        setChanged();
                        notifyObservers();
                    }
                    lastEvent = RadioEvent.HW_OFF;
                }
                lastEventTime = SkyByteRadio.this.mote.getSimulation().getSimulationTime();
                setChanged();
                notifyObservers();
            }
        });
    }

    public RadioPacket getLastPacketTransmitted() {
        return lastOutgoingPacket;
    }

    public RadioPacket getLastPacketReceived() {
        return lastIncomingPacket;
    }

    public void setReceivedPacket(RadioPacket packet) {
        lastIncomingPacket = packet;
        if (cc2420.getState() != CC2420.RadioState.RX_SFD_SEARCH) {
            logger.warn("Radio is turned off, dropping packet data");
            return;
        }
        byte[] packetData = CC2420RadioPacketConverter.fromCoojaToCC2420(packet);
        long deliveryTime = getMote().getSimulation().getSimulationTime();
        for (byte b : packetData) {
            if (isInterfered()) {
                b = (byte) 0xFF;
            }
            final byte byteToDeliver = b;
            getMote().getSimulation().scheduleEvent(new MspMoteTimeEvent(mote, 0) {

                public void execute(long t) {
                    super.execute(t);
                    cc2420.receivedByte(byteToDeliver);
                    mote.requestImmediateWakeup();
                }
            }, deliveryTime);
            deliveryTime += DELAY_BETWEEN_BYTES;
        }
    }

    public Object getLastCustomDataTransmitted() {
        return lastOutgoingByte;
    }

    public Object getLastCustomDataReceived() {
        return lastIncomingByte;
    }

    public void receiveCustomData(Object data) {
        if (!(data instanceof Byte)) {
            logger.fatal("Bad custom data: " + data);
            return;
        }
        lastIncomingByte = (Byte) data;
        final byte inputByte;
        if (isInterfered()) {
            inputByte = (byte) 0xFF;
        } else {
            inputByte = lastIncomingByte;
        }
        mote.getSimulation().scheduleEvent(new MspMoteTimeEvent(mote, 0) {

            public void execute(long t) {
                super.execute(t);
                cc2420.receivedByte(inputByte);
                mote.requestImmediateWakeup();
            }
        }, mote.getSimulation().getSimulationTime());
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

    public int getChannel() {
        cc2420.updateActiveFrequency();
        return cc2420.getActiveChannel();
    }

    public int getFrequency() {
        cc2420.updateActiveFrequency();
        return cc2420.getActiveFrequency();
    }

    public void signalReceptionStart() {
        isReceiving = true;
        lastEventTime = mote.getSimulation().getSimulationTime();
        lastEvent = RadioEvent.RECEPTION_STARTED;
        setChanged();
        notifyObservers();
    }

    public void signalReceptionEnd() {
        isReceiving = false;
        isInterfered = false;
        lastEventTime = mote.getSimulation().getSimulationTime();
        lastEvent = RadioEvent.RECEPTION_FINISHED;
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
        lastEventTime = mote.getSimulation().getSimulationTime();
        lastEvent = RadioEvent.RECEPTION_INTERFERED;
        setChanged();
        notifyObservers();
    }

    public double getCurrentOutputPower() {
        return cc2420.getOutputPower();
    }

    public int getCurrentOutputPowerIndicator() {
        return cc2420.getOutputPowerIndicator();
    }

    public int getOutputPowerIndicatorMax() {
        return 31;
    }

    public double getCurrentSignalStrength() {
        return cc2420.getRSSI();
    }

    public void setCurrentSignalStrength(double signalStrength) {
        cc2420.setRSSI((int) signalStrength);
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
                } else if (isReceiverOn()) {
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

    public boolean isReceiverOn() {
        if (mote.skyNode.radio.getMode() == CC2420.MODE_POWER_OFF) {
            return false;
        }
        if (mote.skyNode.radio.getMode() == CC2420.MODE_TXRX_OFF) {
            return false;
        }
        return true;
    }
}
