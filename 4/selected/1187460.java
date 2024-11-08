package gov.sns.apps.slacs;

import gov.sns.xal.smf.impl.*;
import gov.sns.ca.*;

/**
 * A class to control the setting and monitoring of EPICS channels 
 * and states of a cavity. This relates to the tuning and detuning of the
 * cavitie frequency on and off resonance, 
 * and setting of the phase and amplitude
 *  
 * @author  J. Galambos
 */
public class CavityController {

    /** the main controller that called this tune controller */
    Controller controller;

    /** the SCL cavity to manipulate */
    private SCLCavity sclCavity;

    /** flags indicating actions */
    protected boolean amActive = false;

    /** The channel object  - temporary for testing*/
    private Channel autoRunChannel;

    private PhaseSetHandler phaseSetHandler;

    private PhaseRBHandler phaseRBHandler;

    private AutoRunHandler autoRunHandler;

    /** the monitor for this channel */
    private Monitor phaseRBMon;

    /** Channel to get the LLRF field amplitude */
    private Channel ampRBChannel;

    double dummy1 = 0;

    double dummy2 = 0.;

    /** constructor     */
    public CavityController(Controller cont, SCLCavity cav) {
        controller = cont;
        sclCavity = cav;
        String llrfName = sclCavity.getId().replaceAll("RF:Cav", "LLRF:FCM");
        llrfName += ":";
        String ampName = llrfName + "cavAmpAvg";
        String autoRunName = llrfName + "RunState";
        ampRBChannel = ChannelFactory.defaultFactory().getChannel(ampName);
        try {
            autoRunHandler = new AutoRunHandler(autoRunName, sclCavity, controller, this);
            phaseRBHandler = new PhaseRBHandler(llrfName + "cavPhaseAvg", sclCavity, controller);
            phaseSetHandler = new PhaseSetHandler(llrfName + "CtlPhaseSet", sclCavity, controller, phaseRBHandler);
        } catch (Exception ex) {
            controller.dumpErr("channel connection error for cavity " + cav.getId());
        }
    }

    /** set the cavity to a prescibed value
	* when this value is reached, a monitor on the setpoint will 
	* trigger a call to the controller telling it that the cavity is set
	*/
    public boolean setPhase(double val) {
        try {
            phaseSetHandler.putVal(val);
        } catch (Exception ex) {
            controller.cavitySetAction(sclCavity, Controller.PROBLEM, " problem sending new phase setpoint");
            return false;
        }
        return true;
    }

    /** get the LLRF amplitude value for this cavity 
	*/
    public double getAmp() {
        try {
            return ampRBChannel.getValDbl();
        } catch (Exception ex) {
            controller.dumpErr("can't connect to LLRF amp signal for " + sclCavity.getId());
            return 0.;
        }
    }

    /** initiate the power up of the cavity to its default amplitue
	*/
    protected boolean startAutoTune() {
        if (amActive) {
            controller.dumpErr("Tried to turnOn " + sclCavity.getId() + " while it is being turned On");
            return false;
        }
        amActive = true;
        try {
            autoRunHandler.putVal(1);
        } catch (Exception exc) {
            controller.dumpErr("can't start power up for " + sclCavity.getId());
            return false;
        }
        return true;
    }

    protected String getCavId() {
        return sclCavity.getId();
    }

    protected int getAutoRunStatus() {
        try {
            return autoRunHandler.getVal();
        } catch (Exception exc) {
            controller.dumpErr("can't get autorun status for " + sclCavity.getId());
        }
        return 0;
    }
}
