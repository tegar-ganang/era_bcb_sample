package se.sics.cooja.mspmote.interfaces;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.log4j.Logger;
import org.jdom.Element;
import se.sics.cooja.COOJARadioPacket;
import se.sics.cooja.ClassDescription;
import se.sics.cooja.Mote;
import se.sics.cooja.MoteTimeEvent;
import se.sics.cooja.RadioPacket;
import se.sics.cooja.Simulation;
import se.sics.cooja.TimeEvent;
import se.sics.cooja.interfaces.CustomDataRadio;
import se.sics.cooja.interfaces.Position;
import se.sics.cooja.interfaces.Radio;
import se.sics.cooja.mspmote.MspMote;
import se.sics.cooja.mspmote.MspMoteTimeEvent;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.USART;
import se.sics.mspsim.core.USARTListener;

/**
 * TR1001 radio interface on ESB platform.
 * Assumes Contiki driver specifics such as preambles, synchbytes, GCR coding, CRC16.
 * 
 * @author Fredrik Osterlind
 */
@ClassDescription("TR1001 Radio")
public class TR1001Radio extends Radio implements USARTListener, CustomDataRadio {

    private static Logger logger = Logger.getLogger(TR1001Radio.class);

    /**
   * Cross-level:
   * Delay used when feeding packet data to radio chip (us).
   * 416us corresponds to 19200 bit/s with encoding.
   */
    public static final long DELAY_BETWEEN_BYTES = 416;

    private static final Byte CORRUPTED_DATA = (byte) 0xff;

    private MspMote mote;

    private boolean isTransmitting = false;

    private boolean isReceiving = false;

    private boolean isInterfered = false;

    private RadioEvent lastEvent = RadioEvent.UNKNOWN;

    private long lastEventTime = 0;

    private USART radioUSART = null;

    private RadioPacket receivedPacket = null;

    private RadioPacket sentPacket = null;

    private byte receivedByte, sentByte;

    private TR1001RadioPacketConverter tr1001PacketConverter = null;

    private boolean radioOn = true;

    private double signalStrength = 0;

    /**
   * Creates an interface to the TR1001 radio at mote.
   * 
   * @param mote Mote
   */
    public TR1001Radio(Mote mote) {
        this.mote = (MspMote) mote;
        IOUnit usart = this.mote.getCPU().getIOUnit("USART 0");
        if (usart != null && usart instanceof USART) {
            radioUSART = (USART) usart;
            radioUSART.setUSARTListener(this);
        } else {
            throw new RuntimeException("Bad TR1001 IO: " + usart);
        }
    }

    public RadioPacket getLastPacketTransmitted() {
        return sentPacket;
    }

    public RadioPacket getLastPacketReceived() {
        return receivedPacket;
    }

    public void setReceivedPacket(RadioPacket packet) {
        receivedPacket = packet;
        byte[] arr = TR1001RadioPacketConverter.fromCoojaToTR1001(packet);
        final ArrayDeque<Byte> data = new ArrayDeque<Byte>();
        for (Byte b : arr) {
            data.addLast(b);
        }
        TimeEvent receiveCrosslevelDataEvent = new MspMoteTimeEvent(mote, 0) {

            public void execute(long t) {
                super.execute(t);
                if (data.isEmpty()) {
                    return;
                }
                byte b = data.pop();
                if (isInterfered) {
                    radioUSART.byteReceived(0xFF);
                } else {
                    radioUSART.byteReceived(b);
                }
                mote.requestImmediateWakeup();
                mote.getSimulation().scheduleEvent(this, t + DELAY_BETWEEN_BYTES);
            }
        };
        receiveCrosslevelDataEvent.execute(mote.getSimulation().getSimulationTime());
    }

    public Object getLastCustomDataTransmitted() {
        return sentByte;
    }

    public Object getLastCustomDataReceived() {
        return receivedByte;
    }

    public void receiveCustomData(Object data) {
        if (!(data instanceof Byte)) {
            logger.fatal("Received bad custom data: " + data);
            return;
        }
        receivedByte = isInterfered ? CORRUPTED_DATA : (Byte) data;
        final byte finalByte = receivedByte;
        mote.getSimulation().scheduleEvent(new MspMoteTimeEvent(mote, 0) {

            public void execute(long t) {
                super.execute(t);
                if (radioUSART.isReceiveFlagCleared()) {
                    radioUSART.byteReceived(finalByte);
                } else {
                    logger.warn(mote.getSimulation().getSimulationTime() + ": ----- TR1001 RECEIVED BYTE DROPPED -----");
                }
                mote.requestImmediateWakeup();
            }
        }, mote.getSimulation().getSimulationTime());
    }

    public void dataReceived(USART source, int data) {
        if (!isTransmitting()) {
            tr1001PacketConverter = new TR1001RadioPacketConverter();
            lastEvent = RadioEvent.TRANSMISSION_STARTED;
            lastEventTime = mote.getSimulation().getSimulationTime();
            isTransmitting = true;
            this.setChanged();
            this.notifyObservers();
            if (timeoutTransmission.isScheduled()) {
                logger.warn("Timeout TX event already scheduled");
                timeoutTransmission.remove();
            }
            mote.getSimulation().scheduleEvent(timeoutTransmission, mote.getSimulation().getSimulationTime() + 40 * Simulation.MILLISECOND);
        }
        lastEvent = RadioEvent.CUSTOM_DATA_TRANSMITTED;
        sentByte = (byte) data;
        this.setChanged();
        this.notifyObservers();
        boolean finished = tr1001PacketConverter.fromTR1001ToCoojaAccumulated(sentByte);
        if (finished) {
            timeoutTransmission.remove();
            if (tr1001PacketConverter.accumulatedConversionIsOk()) {
                sentPacket = tr1001PacketConverter.getAccumulatedConvertedCoojaPacket();
                lastEvent = RadioEvent.PACKET_TRANSMITTED;
                this.setChanged();
                this.notifyObservers();
            }
            isTransmitting = false;
            lastEvent = RadioEvent.TRANSMISSION_FINISHED;
            TR1001Radio.this.setChanged();
            TR1001Radio.this.notifyObservers();
        }
    }

    public void stateChanged(int state) {
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
        return -1;
    }

    public void signalReceptionStart() {
        lastEvent = RadioEvent.RECEPTION_STARTED;
        isReceiving = true;
        isInterfered = false;
        this.setChanged();
        this.notifyObservers();
    }

    public void signalReceptionEnd() {
        isInterfered = false;
        isReceiving = false;
        lastEvent = RadioEvent.RECEPTION_FINISHED;
        this.setChanged();
        this.notifyObservers();
    }

    public RadioEvent getLastEvent() {
        return lastEvent;
    }

    public void interfereAnyReception() {
        if (!isInterfered()) {
            isInterfered = true;
            receivedPacket = null;
            lastEventTime = mote.getSimulation().getSimulationTime();
            lastEvent = RadioEvent.RECEPTION_INTERFERED;
            this.setChanged();
            this.notifyObservers();
        }
    }

    public double getCurrentOutputPower() {
        return 0;
    }

    public int getOutputPowerIndicatorMax() {
        return 100;
    }

    public int getCurrentOutputPowerIndicator() {
        return 100;
    }

    public double getCurrentSignalStrength() {
        return signalStrength;
    }

    public void setCurrentSignalStrength(double signalStrength) {
        this.signalStrength = signalStrength;
    }

    public Position getPosition() {
        return mote.getInterfaces().getPosition();
    }

    private TimeEvent timeoutTransmission = new MoteTimeEvent(mote, 0) {

        public void execute(long t) {
            if (!isTransmitting()) {
                return;
            }
            logger.warn("TR1001 transmission timed out, delivering empty packet");
            sentPacket = new COOJARadioPacket(new byte[0]);
            lastEvent = RadioEvent.PACKET_TRANSMITTED;
            TR1001Radio.this.setChanged();
            TR1001Radio.this.notifyObservers();
            isTransmitting = false;
            lastEvent = RadioEvent.TRANSMISSION_FINISHED;
            TR1001Radio.this.setChanged();
            TR1001Radio.this.notifyObservers();
        }
    };

    public JPanel getInterfaceVisualizer() {
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new GridLayout(5, 2));
        final JLabel statusLabel = new JLabel("");
        final JLabel lastEventLabel = new JLabel("");
        final JLabel channelLabel = new JLabel("ALL CHANNELS (-1)");
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
                powerLabel.setText(getCurrentOutputPower() + " dBm (indicator=" + getCurrentOutputPowerIndicator() + "/" + getOutputPowerIndicatorMax() + ")");
                ssLabel.setText(getCurrentSignalStrength() + " dBm");
            }
        });
        observer.update(null, null);
        panel.putClientProperty("intf_obs", observer);
        wrapperPanel.add(BorderLayout.NORTH, panel);
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

    public Collection<Element> getConfigXML() {
        return null;
    }

    public void setConfigXML(Collection<Element> configXML, boolean visAvailable) {
    }

    public Mote getMote() {
        return mote;
    }

    public boolean isReceiverOn() {
        return true;
    }
}
