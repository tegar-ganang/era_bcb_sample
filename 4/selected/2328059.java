package se.sics.cooja.avrmote.interfaces;

import org.apache.log4j.Logger;
import avrora.sim.FiniteStateMachine;
import avrora.sim.FiniteStateMachine.Probe;
import avrora.sim.platform.MicaZ;
import avrora.sim.radio.CC2420Radio;
import avrora.sim.radio.Medium;
import se.sics.cooja.*;
import se.sics.cooja.avrmote.MicaZMote;
import se.sics.cooja.emulatedmote.Radio802154;

/**
 * CC2420 to COOJA wrapper.
 *
 * @author Joakim Eriksson
 */
@ClassDescription("CC2420")
public class MicaZRadio extends Radio802154 {

    private static Logger logger = Logger.getLogger(MicaZRadio.class);

    private MicaZ micaz;

    private CC2420Radio cc2420;

    Medium.Transmitter trans;

    CC2420Radio.Receiver recv;

    FiniteStateMachine fsm;

    public MicaZRadio(Mote mote) {
        super(mote);
        micaz = ((MicaZMote) mote).getMicaZ();
        cc2420 = (CC2420Radio) micaz.getDevice("radio");
        trans = cc2420.getTransmitter();
        fsm = cc2420.getFiniteStateMachine();
        recv = (CC2420Radio.Receiver) cc2420.getReceiver();
        trans.insertProbe(new Medium.Probe.Empty() {

            public void fireBeforeTransmit(Medium.Transmitter t, byte val) {
                handleTransmit(val);
            }
        });
        fsm.insertProbe(new Probe() {

            public void fireBeforeTransition(int arg0, int arg1) {
            }

            public void fireAfterTransition(int arg0, int arg1) {
                RadioEvent re = null;
                if (arg1 >= 3) {
                    re = RadioEvent.HW_ON;
                } else {
                    if (arg0 > 3 && arg1 == 2) {
                    } else {
                        re = RadioEvent.HW_OFF;
                    }
                }
                if (re != null) {
                    lastEvent = re;
                    lastEventTime = MicaZRadio.this.mote.getSimulation().getSimulationTime();
                    setChanged();
                    notifyObservers();
                }
            }
        });
    }

    public int getChannel() {
        return 0;
    }

    public int getFrequency() {
        return (int) cc2420.getFrequency();
    }

    public boolean isReceiverOn() {
        FiniteStateMachine fsm = cc2420.getFiniteStateMachine();
        return fsm.getCurrentState() >= 3;
    }

    public void signalReceptionStart() {
        super.signalReceptionStart();
    }

    public double getCurrentOutputPower() {
        return 1.1;
    }

    public int getCurrentOutputPowerIndicator() {
        return 31;
    }

    public int getOutputPowerIndicatorMax() {
        return 31;
    }

    public double getCurrentSignalStrength() {
        return 1;
    }

    public void setCurrentSignalStrength(double signalStrength) {
    }

    protected void handleEndOfReception() {
        recv.nextByte(false, (byte) 0);
    }

    protected void handleReceive(byte b) {
        recv.nextByte(true, (byte) b);
    }
}
