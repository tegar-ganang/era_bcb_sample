package ch.unizh.ini.jaer.chip.cochlea;

import java.util.ArrayList;
import java.util.prefs.PreferenceChangeEvent;
import net.sf.jaer.biasgen.*;
import net.sf.jaer.biasgen.IPotArray;
import net.sf.jaer.biasgen.VDAC.DAC;
import net.sf.jaer.biasgen.VDAC.VPot;
import net.sf.jaer.chip.*;
import net.sf.jaer.graphics.ChipRendererDisplayMethod;
import net.sf.jaer.graphics.DisplayMethod;
import net.sf.jaer.graphics.FrameAnnotater;
import net.sf.jaer.graphics.SpaceTimeEventDisplayMethod;
import net.sf.jaer.hardwareinterface.*;
import com.sun.opengl.util.GLUT;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Observable;
import java.util.prefs.PreferenceChangeListener;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.sf.jaer.Description;
import net.sf.jaer.hardwareinterface.usb.cypressfx2.CypressFX2;
import net.sf.jaer.util.RemoteControlCommand;
import net.sf.jaer.util.RemoteControlled;

/**
 * Extends Shih-Chii's AMS cochlea AER chip to 
 * add bias generator interface, 
 * to be used when using the on-chip bias generator and the on-board DACs. Also implemements ConfigBits, Scanner, and Equalizer configuration.
 * @author tobi
 */
@Description("Binaural AER silicon cochlea with 64 channels and 8 ganglion cells of two types per channel")
public class CochleaAMS1b extends CochleaAMSNoBiasgen {

    final GLUT glut = new GLUT();

    /** Creates a new instance of CochleaAMSWithBiasgen */
    public CochleaAMS1b() {
        super();
        setBiasgen(new CochleaAMS1b.Biasgen(this));
        getCanvas().setBorderSpacePixels(40);
        for (DisplayMethod m : getCanvas().getDisplayMethods()) {
            if (m instanceof ChipRendererDisplayMethod || m instanceof SpaceTimeEventDisplayMethod) {
                m.addAnnotator(new FrameAnnotater() {

                    public void setAnnotationEnabled(boolean yes) {
                    }

                    public boolean isAnnotationEnabled() {
                        return true;
                    }

                    public void annotate(float[][][] frame) {
                    }

                    public void annotate(Graphics2D g) {
                    }

                    void renderStrokeFontString(GL gl, float x, float y, float z, float angleDeg, String s) {
                        final int font = GLUT.STROKE_ROMAN;
                        final float scale = 2f / 104f;
                        gl.glPushMatrix();
                        gl.glTranslatef(x, y, z);
                        gl.glRotatef(angleDeg, 0, 0, 1);
                        gl.glScalef(scale, scale, scale);
                        gl.glLineWidth(2);
                        for (char c : s.toCharArray()) {
                            glut.glutStrokeCharacter(font, c);
                        }
                        gl.glPopMatrix();
                    }

                    final float xlen = glut.glutStrokeLength(glut.STROKE_ROMAN, "channel"), ylen = glut.glutStrokeLength(glut.STROKE_ROMAN, "cell type");

                    public void annotate(GLAutoDrawable drawable) {
                        GL gl = drawable.getGL();
                        gl.glPushMatrix();
                        {
                            gl.glColor3f(1, 1, 1);
                            renderStrokeFontString(gl, -1, 16 / 2 - 5, 0, 90, "cell type");
                            renderStrokeFontString(gl, sizeX / 2 - 4, -3, 0, 0, "channel");
                            renderStrokeFontString(gl, 0, -3, 0, 0, "hi fr");
                            renderStrokeFontString(gl, sizeX - 15, -3, 0, 0, "low fr");
                        }
                        gl.glPopMatrix();
                    }
                });
            }
        }
    }

    /** overrides the Chip setHardware interface to construct a biasgen if one doesn't exist already.
     * Sets the hardware interface and the bias generators hardware interface
     *@param hardwareInterface the interface
     */
    @Override
    public void setHardwareInterface(final HardwareInterface hardwareInterface) {
        this.hardwareInterface = hardwareInterface;
        try {
            if (getBiasgen() == null) {
                setBiasgen(new CochleaAMS1b.Biasgen(this));
            } else {
                getBiasgen().setHardwareInterface((BiasgenHardwareInterface) hardwareInterface);
            }
        } catch (ClassCastException e) {
            System.err.println(e.getMessage() + ": probably this chip object has a biasgen but the hardware interface doesn't, ignoring");
        }
    }

    /**
     * Describes IPots on tmpdiff128 retina chip. These are configured by a shift register as shown here:
     *<p>
     *<img src="doc-files/tmpdiff128biasgen.gif" alt="tmpdiff128 shift register arrangement"/>
    
    <p>
    This bias generator also offers an abstracted ChipControlPanel interface that is used for a simplified user interface.
     *
     * @author tobi
     */
    public class Biasgen extends net.sf.jaer.biasgen.Biasgen implements ChipControlPanel {

        private ArrayList<HasPreference> hasPreferencesList = new ArrayList<HasPreference>();

        /** The DAC on the board. Specified with 5V reference even though Vdd=3.3 because the internal 2.5V reference is used and so that the VPot controls display correct voltage. */
        protected final DAC dac = new DAC(32, 12, 0, 5f, 3.3f);

        IPotArray ipots = new IPotArray(this);

        PotArray vpots = new PotArray(this);

        private CypressFX2 cypress = null;

        private ConfigBit aerKillBit, vResetBit, yBit, selAER, selIn, powerDown, preampAR, preampGain;

        volatile ConfigBit[] configBits = { powerDown = new ConfigBit("d5", "powerDown", "turns off on-chip biases (1=turn off, 0=normal)"), preampAR = new TriStateableConfigBit("e4", "preampAR", "preamp attack/release (0=attack/release ratio=1:500, 1=A/R=1:2000, HiZ=A/R=1:4000 (may not work))"), preampGain = new TriStateableConfigBit("e3", "preampGain", "preamp gain bit (1=gain=40dB, 0=gain=50dB, HiZ=60dB if preamp threshold \"PreampAGCThreshold (TH)\"is set above 2V)"), vResetBit = new ConfigBit("e5", "Vreset", "global latch reset (1=reset, 0=run)"), selIn = new ConfigBit("e6", "selIn", "Parallel (1) or Cascaded (0) Arch"), selAER = new ConfigBit("d3", "selAER", "Chooses whether lpf (0) or rectified (1) lpf output drives lpf neurons"), yBit = new ConfigBit("d2", "YBit", "Used to select which neurons to kill"), aerKillBit = new ConfigBit("d6", "AERKillBit", "Set high to kill channel") };

        Scanner scanner = new Scanner();

        Equalizer equalizer = new Equalizer();

        BufferIPot bufferIPot = new BufferIPot();

        boolean dacPowered = getPrefs().getBoolean("CochleaAMS1b.Biasgen.DAC.powered", true);

        /** Creates a new instance of Biasgen for Tmpdiff128 with a given hardware interface
         *@param chip the chip this biasgen belongs to
         */
        public Biasgen(Chip chip) {
            super(chip);
            setName("CochleaAMSWithBiasgen");
            scanner.addObserver(this);
            equalizer.addObserver(this);
            bufferIPot.addObserver(this);
            for (ConfigBit b : configBits) {
                b.addObserver(this);
            }
            potArray = new IPotArray(this);
            ipots.addPot(new IPot(this, "VAGC", 0, IPot.Type.NORMAL, IPot.Sex.N, 0, 1, "Sets reference for AGC diffpair in SOS"));
            ipots.addPot(new IPot(this, "Curstartbpf", 1, IPot.Type.NORMAL, IPot.Sex.P, 0, 2, "Sets master current to local DACs for BPF Iq"));
            ipots.addPot(new IPot(this, "DacBufferNb", 2, IPot.Type.NORMAL, IPot.Sex.N, 0, 3, "Sets bias current of amp in local DACs"));
            ipots.addPot(new IPot(this, "Vbp", 3, IPot.Type.NORMAL, IPot.Sex.P, 0, 4, "Sets bias for readout amp of BPF"));
            ipots.addPot(new IPot(this, "Ibias20OpAmp", 4, IPot.Type.NORMAL, IPot.Sex.P, 0, 5, "Bias current for preamp"));
            ipots.addPot(new IPot(this, "N.C.", 5, IPot.Type.NORMAL, IPot.Sex.N, 0, 6, "not used"));
            ipots.addPot(new IPot(this, "Vsetio", 6, IPot.Type.CASCODE, IPot.Sex.P, 0, 7, "Sets 2I0 and I0 for LPF time constant"));
            ipots.addPot(new IPot(this, "Vdc1", 7, IPot.Type.NORMAL, IPot.Sex.P, 0, 8, "Sets DC shift for close end of cascade"));
            ipots.addPot(new IPot(this, "NeuronRp", 8, IPot.Type.NORMAL, IPot.Sex.P, 0, 9, "Sets bias current of neuron"));
            ipots.addPot(new IPot(this, "Vclbtgate", 9, IPot.Type.NORMAL, IPot.Sex.P, 0, 10, "Bias gate of CLBT"));
            ipots.addPot(new IPot(this, "Vioff", 10, IPot.Type.NORMAL, IPot.Sex.P, 0, 11, "Sets DC shift input to LPF"));
            ipots.addPot(new IPot(this, "Vbias2", 11, IPot.Type.NORMAL, IPot.Sex.P, 0, 12, "Sets lower cutoff freq for cascade"));
            ipots.addPot(new IPot(this, "Ibias10OpAmp", 12, IPot.Type.NORMAL, IPot.Sex.P, 0, 13, "Bias current for preamp"));
            ipots.addPot(new IPot(this, "Vthbpf2", 13, IPot.Type.CASCODE, IPot.Sex.P, 0, 14, "Sets high end of threshold current for bpf neurons"));
            ipots.addPot(new IPot(this, "Follbias", 14, IPot.Type.NORMAL, IPot.Sex.N, 0, 15, "Bias for PADS"));
            ipots.addPot(new IPot(this, "pdbiasTX", 15, IPot.Type.NORMAL, IPot.Sex.N, 0, 16, "pulldown for AER TX"));
            ipots.addPot(new IPot(this, "Vrefract", 16, IPot.Type.NORMAL, IPot.Sex.N, 0, 17, "Sets refractory period for AER neurons"));
            ipots.addPot(new IPot(this, "VbampP", 17, IPot.Type.NORMAL, IPot.Sex.P, 0, 18, "Sets bias current for input amp to neurons"));
            ipots.addPot(new IPot(this, "Vcascode", 18, IPot.Type.CASCODE, IPot.Sex.N, 0, 19, "Sets cascode voltage"));
            ipots.addPot(new IPot(this, "Vbpf2", 19, IPot.Type.NORMAL, IPot.Sex.P, 0, 20, "Sets lower cutoff freq for BPF"));
            ipots.addPot(new IPot(this, "Ibias10OTA", 20, IPot.Type.NORMAL, IPot.Sex.N, 0, 21, "Bias current for OTA in preamp"));
            ipots.addPot(new IPot(this, "Vthbpf1", 21, IPot.Type.CASCODE, IPot.Sex.P, 0, 22, "Sets low end of threshold current to bpf neurons"));
            ipots.addPot(new IPot(this, "Curstart ", 22, IPot.Type.NORMAL, IPot.Sex.P, 0, 23, "Sets master current to local DACs for SOS Vq"));
            ipots.addPot(new IPot(this, "Vbias1", 23, IPot.Type.NORMAL, IPot.Sex.P, 0, 24, "Sets higher cutoff freq for SOS"));
            ipots.addPot(new IPot(this, "NeuronVleak", 24, IPot.Type.NORMAL, IPot.Sex.P, 0, 25, "Sets leak current for neuron"));
            ipots.addPot(new IPot(this, "Vioffbpfn", 25, IPot.Type.NORMAL, IPot.Sex.N, 0, 26, "Sets DC level for input to bpf"));
            ipots.addPot(new IPot(this, "Vcasbpf", 26, IPot.Type.CASCODE, IPot.Sex.P, 0, 27, "Sets cascode voltage in cm BPF"));
            ipots.addPot(new IPot(this, "Vdc2", 27, IPot.Type.NORMAL, IPot.Sex.P, 0, 28, "Sets DC shift for SOS at far end of cascade"));
            ipots.addPot(new IPot(this, "Vterm", 28, IPot.Type.CASCODE, IPot.Sex.N, 0, 29, "Sets bias current of terminator xtor in diffusor"));
            ipots.addPot(new IPot(this, "Vclbtcasc", 29, IPot.Type.CASCODE, IPot.Sex.P, 0, 30, "Sets cascode voltage in CLBT"));
            ipots.addPot(new IPot(this, "reqpuTX", 30, IPot.Type.NORMAL, IPot.Sex.P, 0, 31, "Sets pullup bias for AER req ckts"));
            ipots.addPot(new IPot(this, "Vbpf1", 31, IPot.Type.NORMAL, IPot.Sex.P, 0, 32, "Sets higher cutoff freq for BPF"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vterm", dac, 0, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, "Sets bias current of terminator xtor in diffusor"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vrefhres", dac, 1, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "VthAGC", dac, 2, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vrefreadout", dac, 3, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "BiasDACBufferNBias", dac, 4, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "DAC buffer bias for ???"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vrefract", dac, 5, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "PreampAGCThreshold (TH)", dac, 6, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "Threshold for microphone preamp AGC gain reduction turn-on"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vrefpreamp", dac, 7, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "NeuronRp", dac, 8, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "Sets bias current of neuron - overrides onchip bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vthbpf1x", dac, 9, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vioffbpfn", dac, 10, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "NeuronVleak", dac, 11, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "Sets leak current for neuron - not connected on board"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "DCOutputLevel", dac, 12, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "Microphone DC output level to cochlea chip"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vthbpf2x", dac, 13, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "DACSpOut2", dac, 14, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "DACSpOut1", dac, 15, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vth4", dac, 16, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vcas2x", dac, 17, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vrefo", dac, 18, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vrefn2", dac, 19, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vq", dac, 20, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vcassyni", dac, 21, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vgain", dac, 22, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vrefn", dac, 23, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "VAI0", dac, 24, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vdd1", dac, 25, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vth1", dac, 26, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vref", dac, 27, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vtau", dac, 28, Pot.Type.NORMAL, Pot.Sex.P, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "VcondVt", dac, 29, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vpm", dac, 30, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, "test dac bias"));
            vpots.addPot(new VPot(CochleaAMS1b.this, "Vhm", dac, 31, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, "test dac bias"));
            loadPreferences();
        }

        @Override
        public void loadPreferences() {
            super.loadPreferences();
            if (ipots != null) {
                ipots.loadPreferences();
            }
            if (vpots != null) {
                vpots.loadPreferences();
            }
            if (hasPreferencesList != null) {
                for (HasPreference hp : hasPreferencesList) {
                    hp.loadPreference();
                }
            }
        }

        @Override
        public void storePreferences() {
            super.storePreferences();
            ipots.storePreferences();
            vpots.storePreferences();
            for (HasPreference hp : hasPreferencesList) {
                hp.storePreference();
            }
        }

        @Override
        public JPanel buildControlPanel() {
            CochleaAMS1bControlPanel myControlPanel = new CochleaAMS1bControlPanel(CochleaAMS1b.this);
            return myControlPanel;
        }

        @Override
        public void setHardwareInterface(BiasgenHardwareInterface hw) {
            if (hw == null) {
                cypress = null;
                hardwareInterface = null;
                return;
            }
            if (hw instanceof CochleaAMS1bHardwareInterface) {
                hardwareInterface = hw;
                cypress = (CypressFX2) hardwareInterface;
                log.info("set hardwareInterface CochleaAMS1bHardwareInterface=" + hardwareInterface.toString());
                sendConfiguration();
                resetAERComm();
            }
        }

        final short CMD_IPOT = 1, CMD_RESET_EQUALIZER = 2, CMD_SCANNER = 3, CMD_EQUALIZER = 4, CMD_SETBIT = 5, CMD_VDAC = 6, CMD_INITDAC = 7;

        final byte[] emptyByteArray = new byte[0];

        /** Does special reset cycle, in background thread */
        void resetAERComm() {
            Runnable r = new Runnable() {

                public void run() {
                    yBit.set(true);
                    aerKillBit.set(false);
                    vResetBit.set(true);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                    vResetBit.set(false);
                    aerKillBit.set(true);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                    yBit.set(false);
                    aerKillBit.set(false);
                    log.info("AER communication reset by toggling configuration bits");
                    sendConfiguration();
                }
            };
            Thread t = new Thread(r, "ResetAERComm");
            t.start();
        }

        void sendCmd(int cmd, int index, byte[] bytes) throws HardwareInterfaceException {
            if (bytes == null) {
                bytes = emptyByteArray;
            }
            if (cypress != null) {
                cypress.sendVendorRequest(CypressFX2.VENDOR_REQUEST_SEND_BIAS_BYTES, (short) (0xffff & cmd), (short) (0xffff & index), bytes);
            }
        }

        void sendCmd(int cmd, int index) throws HardwareInterfaceException {
            sendCmd(cmd, index, emptyByteArray);
        }

        /** The central point for communication with HW from biasgen. All objects in Biasgen are Observables
        and add Biasgen.this as Observer. They then call notifyObservers when their state changes.
         * @param observable IPot, Scanner, etc
         * @param object not used at present
         */
        @Override
        public synchronized void update(Observable observable, Object object) {
            if (cypress == null) {
                return;
            }
            try {
                if (observable instanceof IPot || observable instanceof BufferIPot) {
                    byte[] bytes = new byte[1 + ipots.getNumPots() * ipots.getPots().get(0).getNumBytes()];
                    int ind = 0;
                    Iterator itr = ((IPotArray) ipots).getShiftRegisterIterator();
                    while (itr.hasNext()) {
                        IPot p = (IPot) itr.next();
                        byte[] b = p.getBinaryRepresentation();
                        System.arraycopy(b, 0, bytes, ind, b.length);
                        ind += b.length;
                    }
                    bytes[ind] = (byte) bufferIPot.getValue();
                    sendCmd(CMD_IPOT, 0, bytes);
                } else if (observable instanceof VPot) {
                    VPot p = (VPot) observable;
                    sendDAC(p);
                } else if (observable instanceof TriStateableConfigBit) {
                    TriStateableConfigBit b = (TriStateableConfigBit) observable;
                    byte[] bytes = { (byte) ((b.get() ? (byte) 1 : (byte) 0) | (b.isHiZEnabled() ? (byte) 2 : (byte) 0)) };
                    sendCmd(CMD_SETBIT, b.portbit, bytes);
                } else if (observable instanceof ConfigBit) {
                    ConfigBit b = (ConfigBit) observable;
                    byte[] bytes = { b.get() ? (byte) 1 : (byte) 0 };
                    sendCmd(CMD_SETBIT, b.portbit, bytes);
                } else if (observable instanceof Scanner) {
                    byte[] bytes = new byte[1];
                    int index = 0;
                    if (scanner.isContinuousScanningEnabled()) {
                        bytes[0] = (byte) (0xFF & scanner.getPeriod());
                        index = 1;
                    } else {
                        bytes[0] = (byte) (scanner.getCurrentStage() & 0xFF);
                        index = 0;
                    }
                    sendCmd(CMD_SCANNER, index, bytes);
                } else if (observable instanceof Equalizer.EqualizerChannel) {
                    Equalizer.EqualizerChannel c = (Equalizer.EqualizerChannel) observable;
                    int value = (c.channel << 8) + CMD_EQUALIZER;
                    int index = c.qsos + (c.qbpf << 5) + (c.lpfkilled ? 1 << 10 : 0) + (c.bpfkilled ? 1 << 11 : 0);
                    sendCmd(value, index);
                } else if (observable instanceof Equalizer) {
                } else {
                    super.update(observable, object);
                }
            } catch (HardwareInterfaceException e) {
                log.warning(e.toString());
            }
        }

        void sendConfiguration() {
            try {
                if (!isOpen()) {
                    open();
                }
            } catch (HardwareInterfaceException e) {
                log.warning("opening device to send configuration: " + e);
                return;
            }
            log.info("sending complete configuration");
            update(ipots.getPots().get(0), null);
            for (Pot v : vpots.getPots()) {
                update(v, v);
            }
            try {
                setDACPowered(isDACPowered());
            } catch (HardwareInterfaceException ex) {
                log.warning("setting power state of DACs: " + ex);
            }
            for (ConfigBit b : configBits) {
                update(b, b);
            }
            update(scanner, scanner);
            for (Equalizer.EqualizerChannel c : equalizer.channels) {
                update(c, null);
            }
        }

        public void initDAC() throws HardwareInterfaceException {
            sendCmd(CMD_INITDAC, 0);
        }

        void sendDAC(VPot pot) throws HardwareInterfaceException {
            int chan = pot.getChannel();
            int value = pot.getBitValue();
            byte[] b = new byte[6];
            byte msb = (byte) (0xff & ((0xf00 & value) >> 8));
            byte lsb = (byte) (0xff & value);
            byte dat1 = 0;
            byte dat2 = (byte) 0xC0;
            byte dat3 = 0;
            dat1 |= (0xff & ((chan % 16) & 0xf));
            dat2 |= ((msb & 0xf) << 2) | ((0xff & (lsb & 0xc0) >> 6));
            dat3 |= (0xff & ((lsb << 2)));
            if (chan < 16) {
                b[0] = dat1;
                b[1] = dat2;
                b[2] = dat3;
                b[3] = 0;
                b[4] = 0;
                b[5] = 0;
            } else {
                b[0] = 0;
                b[1] = 0;
                b[2] = 0;
                b[3] = dat1;
                b[4] = dat2;
                b[5] = dat3;
            }
            sendCmd(CMD_VDAC, 0, b);
        }

        /** Sets the VDACs on the board to be powered or high impedance output. This is a global operation.
         * 
         * @param yes true to power up DACs
         * @throws net.sf.jaer.hardwareinterface.HardwareInterfaceException
         */
        public void setDACPowered(boolean yes) throws HardwareInterfaceException {
            putPref("CochleaAMS1b.Biasgen.DAC.powered", yes);
            byte[] b = new byte[6];
            Arrays.fill(b, (byte) 0);
            final byte up = (byte) 9, down = (byte) 8;
            if (yes) {
                b[0] = up;
                b[3] = up;
            } else {
                b[0] = down;
                b[3] = down;
            }
            sendCmd(CMD_VDAC, 0, b);
        }

        /** Returns the DAC powered state
         * 
         * @return true if powered up
         */
        public boolean isDACPowered() {
            return dacPowered;
        }

        class BufferIPot extends Observable implements RemoteControlled, PreferenceChangeListener, HasPreference {

            final int max = 63;

            private volatile int value;

            private final String key = "CochleaAMS1b.Biasgen.BufferIPot.value";

            BufferIPot() {
                if (getRemoteControl() != null) {
                    getRemoteControl().addCommandListener(this, "setbufferbias bitvalue", "Sets the buffer bias value");
                }
                loadPreference();
                getPrefs().addPreferenceChangeListener(this);
                hasPreferencesList.add(this);
            }

            public int getValue() {
                return value;
            }

            public void setValue(int value) {
                if (value > max) {
                    value = max;
                } else if (value < 0) {
                    value = 0;
                }
                this.value = value;
                setChanged();
                notifyObservers();
            }

            @Override
            public String toString() {
                return String.format("BufferIPot with max=%d, value=%d", max, value);
            }

            public String processRemoteControlCommand(RemoteControlCommand command, String input) {
                String[] tok = input.split("\\s");
                if (tok.length < 2) {
                    return "bufferbias " + getValue() + "\n";
                } else {
                    try {
                        int val = Integer.parseInt(tok[1]);
                        setValue(val);
                    } catch (NumberFormatException e) {
                        return "?\n";
                    }
                }
                return "bufferbias " + getValue() + "\n";
            }

            public void preferenceChange(PreferenceChangeEvent e) {
                if (e.getKey().equals(key)) {
                    setValue(Integer.parseInt(e.getNewValue()));
                }
            }

            public void loadPreference() {
                setValue(getPrefs().getInt(key, max / 2));
            }

            public void storePreference() {
                putPref(key, value);
            }
        }

        /** A single bitmask of digital configuration */
        class ConfigBit extends Observable implements PreferenceChangeListener, HasPreference {

            int port;

            short portbit;

            int bitmask;

            private volatile boolean value;

            String name, tip;

            String key;

            String portBitString;

            ConfigBit(String portBit, String name, String tip) {
                if (portBit == null || portBit.length() != 2) {
                    throw new Error("BitConfig portBit=" + portBit + " but must be 2 characters");
                }
                String s = portBit.toLowerCase();
                if (!(s.startsWith("c") || s.startsWith("d") || s.startsWith("e"))) {
                    throw new Error("BitConfig portBit=" + portBit + " but must be 2 characters and start with C, D, or E");
                }
                portBitString = portBit;
                char ch = s.charAt(0);
                switch(ch) {
                    case 'c':
                        port = 0;
                        break;
                    case 'd':
                        port = 1;
                        break;
                    case 'e':
                        port = 2;
                        break;
                    default:
                        throw new Error("BitConfig portBit=" + portBit + " but must be 2 characters and start with C, D, or E");
                }
                bitmask = 1 << Integer.valueOf(s.substring(1, 2));
                portbit = (short) (0xffff & ((port << 8) + (0xff & bitmask)));
                this.name = name;
                this.tip = tip;
                key = "CochleaAMS1b.Biasgen.BitConfig." + name;
                loadPreference();
                getPrefs().addPreferenceChangeListener(this);
                hasPreferencesList.add(this);
            }

            void set(boolean value) {
                this.value = value;
                setChanged();
                notifyObservers();
            }

            boolean get() {
                return value;
            }

            @Override
            public String toString() {
                return String.format("ConfigBit name=%s portbit=%s value=%s", name, portBitString, Boolean.toString(value));
            }

            @Override
            public void preferenceChange(PreferenceChangeEvent e) {
                if (e.getKey().equals(key)) {
                    boolean newv = Boolean.parseBoolean(e.getNewValue());
                    set(newv);
                }
            }

            @Override
            public void loadPreference() {
                set(getPrefs().getBoolean(key, false));
            }

            @Override
            public void storePreference() {
                putPref(key, value);
            }
        }

        /** Adds a hiZ state to the bit to set port bit to input */
        class TriStateableConfigBit extends ConfigBit {

            private volatile boolean hiZEnabled = false;

            String hiZKey;

            TriStateableConfigBit(String portBit, String name, String tip) {
                super(portBit, name, tip);
                hiZKey = "CochleaAMS1b.Biasgen.BitConfig." + name + ".hiZEnabled";
                loadPreference();
            }

            /**
             * @return the hiZEnabled
             */
            public boolean isHiZEnabled() {
                return hiZEnabled;
            }

            /**
             * @param hiZEnabled the hiZEnabled to set
             */
            public void setHiZEnabled(boolean hiZEnabled) {
                this.hiZEnabled = hiZEnabled;
                setChanged();
                notifyObservers();
            }

            @Override
            public String toString() {
                return String.format("TriStateableConfigBit name=%s portbit=%s value=%s hiZEnabled=%s", name, portBitString, Boolean.toString(get()), hiZEnabled);
            }

            public void loadPreference() {
                super.loadPreference();
                setHiZEnabled(getPrefs().getBoolean(key, false));
            }

            public void storePreference() {
                super.storePreference();
                putPref(key, hiZEnabled);
            }
        }

        class Scanner extends Observable implements PreferenceChangeListener, HasPreference {

            Scanner() {
                loadPreference();
                getPrefs().addPreferenceChangeListener(this);
                hasPreferencesList.add(this);
            }

            int nstages = 64;

            private volatile int currentStage;

            private volatile boolean continuousScanningEnabled;

            private volatile int period;

            int minPeriod = 10;

            int maxPeriod = 255;

            public int getCurrentStage() {
                return currentStage;
            }

            public void setCurrentStage(int currentStage) {
                this.currentStage = currentStage;
                continuousScanningEnabled = false;
                setChanged();
                notifyObservers();
            }

            public boolean isContinuousScanningEnabled() {
                return continuousScanningEnabled;
            }

            public void setContinuousScanningEnabled(boolean continuousScanningEnabled) {
                this.continuousScanningEnabled = continuousScanningEnabled;
                setChanged();
                notifyObservers();
            }

            public int getPeriod() {
                return period;
            }

            public void setPeriod(int period) {
                if (period < minPeriod) {
                    period = 10;
                }
                if (period > maxPeriod) {
                    period = (byte) (maxPeriod);
                }
                this.period = period;
                setChanged();
                notifyObservers();
            }

            public void preferenceChange(PreferenceChangeEvent e) {
                if (e.getKey().equals("CochleaAMS1b.Biasgen.Scanner.currentStage")) {
                    setCurrentStage(Integer.parseInt(e.getNewValue()));
                } else if (e.getKey().equals("CochleaAMS1b.Biasgen.Scanner.currentStage")) {
                    setContinuousScanningEnabled(Boolean.parseBoolean(e.getNewValue()));
                }
            }

            public void loadPreference() {
                setCurrentStage(getPrefs().getInt("CochleaAMS1b.Biasgen.Scanner.currentStage", 0));
                setContinuousScanningEnabled(getPrefs().getBoolean("CochleaAMS1b.Biasgen.Scanner.continuousScanningEnabled", false));
                setPeriod(getPrefs().getInt("CochleaAMS1b.Biasgen.Scanner.period", 50));
            }

            public void storePreference() {
                putPref("CochleaAMS1b.Biasgen.Scanner.period", period);
                putPref("CochleaAMS1b.Biasgen.Scanner.continuousScanningEnabled", continuousScanningEnabled);
                putPref("CochleaAMS1b.Biasgen.Scanner.currentStage", currentStage);
            }
        }

        class Equalizer extends Observable {

            final int numChannels = 128, maxValue = 31;

            EqualizerChannel[] channels = new EqualizerChannel[numChannels];

            Equalizer() {
                for (int i = 0; i < numChannels; i++) {
                    channels[i] = new EqualizerChannel(i);
                    channels[i].addObserver(Biasgen.this);
                }
            }

            class EqualizerChannel extends Observable implements ChangeListener, PreferenceChangeListener, HasPreference {

                final int max = 31;

                int channel;

                private String prefsKey;

                private volatile int qsos;

                private volatile int qbpf;

                private volatile boolean bpfkilled, lpfkilled;

                EqualizerChannel(int n) {
                    channel = n;
                    prefsKey = "CochleaAMS1b.Biasgen.Equalizer.EqualizerChannel." + channel + ".";
                    loadPreference();
                    getPrefs().addPreferenceChangeListener(this);
                    hasPreferencesList.add(this);
                }

                @Override
                public String toString() {
                    return String.format("EqualizerChannel: channel=%-3d qbpf=%-2d qsos=%-2d bpfkilled=%-6s lpfkilled=%-6s", channel, qbpf, qsos, Boolean.toString(bpfkilled), Boolean.toString(lpfkilled));
                }

                public int getQSOS() {
                    return qsos;
                }

                public void setQSOS(int qsos) {
                    if (this.qsos != qsos) {
                        setChanged();
                    }
                    this.qsos = qsos;
                    notifyObservers();
                }

                public int getQBPF() {
                    return qbpf;
                }

                public void setQBPF(int qbpf) {
                    if (this.qbpf != qbpf) {
                        setChanged();
                    }
                    this.qbpf = qbpf;
                    notifyObservers();
                }

                public boolean isLpfKilled() {
                    return lpfkilled;
                }

                public void setLpfKilled(boolean killed) {
                    if (killed != this.lpfkilled) {
                        setChanged();
                    }
                    this.lpfkilled = killed;
                    notifyObservers();
                }

                public boolean isBpfkilled() {
                    return bpfkilled;
                }

                public void setBpfKilled(boolean bpfkilled) {
                    if (bpfkilled != this.bpfkilled) {
                        setChanged();
                    }
                    this.bpfkilled = bpfkilled;
                    notifyObservers();
                }

                public void stateChanged(ChangeEvent e) {
                    if (e.getSource() instanceof CochleaAMS1bControlPanel.EqualizerSlider) {
                        CochleaAMS1bControlPanel.EqualizerSlider s = (CochleaAMS1bControlPanel.EqualizerSlider) e.getSource();
                        if (s instanceof CochleaAMS1bControlPanel.QSOSSlider) {
                            s.channel.setQSOS(s.getValue());
                        }
                        if (s instanceof CochleaAMS1bControlPanel.QBPFSlider) {
                            s.channel.setQBPF(s.getValue());
                        }
                        setChanged();
                        notifyObservers();
                    } else if (e.getSource() instanceof CochleaAMS1bControlPanel.KillBox) {
                        CochleaAMS1bControlPanel.KillBox b = (CochleaAMS1bControlPanel.KillBox) e.getSource();
                        if (b instanceof CochleaAMS1bControlPanel.LPFKillBox) {
                            b.channel.setLpfKilled(b.isSelected());
                        } else {
                            b.channel.setBpfKilled(b.isSelected());
                        }
                        setChanged();
                        notifyObservers();
                    }
                }

                public void preferenceChange(PreferenceChangeEvent e) {
                    if (e.getKey().equals(prefsKey + "qsos")) {
                        setQSOS(Integer.parseInt(e.getNewValue()));
                    } else if (e.getKey().equals(prefsKey + "qbpf")) {
                        setQBPF(Integer.parseInt(e.getNewValue()));
                    } else if (e.getKey().equals(prefsKey + "bpfkilled")) {
                        setBpfKilled(Boolean.parseBoolean(e.getNewValue()));
                    } else if (e.getKey().equals(prefsKey + "lpfkilled")) {
                        setLpfKilled(Boolean.parseBoolean(e.getNewValue()));
                    }
                }

                public void loadPreference() {
                    qsos = getPrefs().getInt(prefsKey + "qsos", 15);
                    qbpf = getPrefs().getInt(prefsKey + "qbpf", 15);
                    bpfkilled = getPrefs().getBoolean(prefsKey + "bpfkilled", false);
                    lpfkilled = getPrefs().getBoolean(prefsKey + "lpfkilled", false);
                    setChanged();
                    notifyObservers();
                }

                public void storePreference() {
                    putPref(prefsKey + "bpfkilled", bpfkilled);
                    putPref(prefsKey + "lpfkilled", lpfkilled);
                    putPref(prefsKey + "qbpf", qbpf);
                    putPref(prefsKey + "qsos", qsos);
                }
            }
        }
    }
}
