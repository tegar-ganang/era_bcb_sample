package ch.unizh.ini.jaer.chip.dvs320;

import ch.unizh.ini.jaer.chip.dvs320.cDVSTestHardwareInterfaceProxy;
import ch.unizh.ini.jaer.chip.retina.*;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JRadioButton;
import net.sf.jaer.aemonitor.*;
import net.sf.jaer.biasgen.*;
import net.sf.jaer.biasgen.VDAC.VPot;
import net.sf.jaer.chip.*;
import net.sf.jaer.event.*;
import net.sf.jaer.graphics.AEViewer;
import net.sf.jaer.hardwareinterface.*;
import java.awt.BorderLayout;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Observable;
import java.util.StringTokenizer;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import net.sf.jaer.Description;
import net.sf.jaer.biasgen.Pot.Sex;
import net.sf.jaer.biasgen.Pot.Type;
import net.sf.jaer.biasgen.VDAC.DAC;
import net.sf.jaer.biasgen.VDAC.VPotGUIControl;
import net.sf.jaer.util.ParameterControlPanel;
import net.sf.jaer.util.RemoteControlCommand;
import net.sf.jaer.util.RemoteControlled;

/**
 * Describes  retina and its event extractor and bias generator.
 * Two constructors ara available, the vanilla constructor is used for event playback and the
 *one with a HardwareInterface parameter is useful for live capture.
 * {@link #setHardwareInterface} is used when the hardware interface is constructed after the retina object.
 *The constructor that takes a hardware interface also constructs the biasgen interface.
 * <p>
 * cDVSTest features several arrays of pixels, a fully configurable bias generator,
 * and a configurable output selector for digital and analog current and voltage outputs for characterization.
 * The output is word serial and includes an intensity neuron which rides onto the other addresses.
 * cDVSTest10 is built in UMC18 CIS process and has 14.5u pixels.
 *
 * @author tobi
 */
@Description("cDVSTest color Dynamic Vision Test chip")
public class cDVSTest30 extends AETemporalConstastRetina implements HasIntensity {

    public static final int SIZEX_TOTAL = 140;

    public static final int SIZE_Y = 64;

    public static final int SIZE_Y_CDVS = 32;

    public static final int SIZE_X_CDVS = 32;

    public static final int SIZE_X_DVS = 64;

    public static final int COLOR_CHANGE_BIT = 1;

    /**
     * bit masks/shifts for cDVS  AE data
     */
    public static final int POLMASK = 1, XSHIFT = Integer.bitCount(POLMASK), XMASK = 127 << XSHIFT, YSHIFT = 16, YMASK = 63 << YSHIFT, INTENSITYMASK = 0x40000000;

    /** data type is either timestamp or data (AE address or ADC reading) */
    public static final int DATA_TYPE_MASK = 0xc000, DATA_TYPE_ADDRESS = 0x0000, DATA_TYPE_TIMESTAMP = 0x4000, DATA_TYPE_WRAP = 0x8000, DATA_TYPE_TIMESTAMP_RESET = 0xd000;

    /** Address-type refers to data if is it an "address". This data is either an AE address or ADC reading.*/
    public static final int ADDRESS_TYPE_MASK = 0x2000, EVENT_ADDRESS_MASK = POLMASK | XMASK | YMASK, ADDRESS_TYPE_EVENT = 0x0000, ADDRESS_TYPE_ADC = 0x2000;

    /** For ADC data, the data is defined by the ADC channel and whether it is the first ADC value from the scanner. */
    public static final int ADC_TYPE_MASK = 0x1000, ADC_DATA_MASK = 0xfff, ADC_START_BIT = 0x1000, ADC_CHANNEL_MASK = 0x0000;

    public static final int MAX_ADC = (int) ((1 << 12) - 1);

    /** The computed intensity value. */
    private float globalIntensity = 0;

    private CDVSLogIntensityFrameData frameData = new CDVSLogIntensityFrameData();

    private cDVSTest30Renderer cDVSRenderer = null;

    private cDVSTest30DisplayMethod cDVSDisplayMethod = null;

    private boolean displayLogIntensity;

    private boolean displayColorChangeEvents;

    private boolean displayLogIntensityChangeEvents;

    /** Creates a new instance of cDVSTest10.  */
    public cDVSTest30() {
        setName("cDVSTest30");
        setEventClass(cDVSEvent.class);
        setSizeX(SIZEX_TOTAL);
        setSizeY(SIZE_Y);
        setNumCellTypes(3);
        setPixelHeightUm(14.5f);
        setPixelWidthUm(14.5f);
        setEventExtractor(new cDVSTestExtractor(this));
        setBiasgen(new cDVSTest30.cDVSTestBiasgen(this));
        displayLogIntensity = getPrefs().getBoolean("displayLogIntensity", true);
        displayColorChangeEvents = getPrefs().getBoolean("displayColorChangeEvents", true);
        displayLogIntensityChangeEvents = getPrefs().getBoolean("displayLogIntensityChangeEvents", true);
        setRenderer((cDVSRenderer = new cDVSTest30Renderer(this)));
        cDVSDisplayMethod = new cDVSTest30DisplayMethod(this);
        getCanvas().addDisplayMethod(cDVSDisplayMethod);
        getCanvas().setDisplayMethod(cDVSDisplayMethod);
        cDVSDisplayMethod.setIntensitySource(this);
    }

    @Override
    public void onDeregistration() {
        unregisterControlPanel();
    }

    @Override
    public void onRegistration() {
        registerControlPanel();
    }

    cDVSTest30DisplayControlPanel controlPanel = null;

    public void registerControlPanel() {
        try {
            AEViewer viewer = getAeViewer();
            JPanel imagePanel = viewer.getImagePanel();
            imagePanel.add((controlPanel = new cDVSTest30DisplayControlPanel(this)), BorderLayout.SOUTH);
            imagePanel.revalidate();
        } catch (Exception e) {
            log.warning("could not register control panel: " + e);
        }
    }

    void unregisterControlPanel() {
        try {
            AEViewer viewer = getAeViewer();
            JPanel imagePanel = viewer.getImagePanel();
            imagePanel.remove(controlPanel);
            imagePanel.revalidate();
        } catch (Exception e) {
            log.warning("could not unregister control panel: " + e);
        }
    }

    /** Creates a new instance of cDVSTest10
     * @param hardwareInterface an existing hardware interface. This constructor is preferred. It makes a new cDVSTest10Biasgen object to talk to the on-chip biasgen.
     */
    public cDVSTest30(HardwareInterface hardwareInterface) {
        this();
        setHardwareInterface(hardwareInterface);
    }

    @Override
    public float getIntensity() {
        return globalIntensity;
    }

    @Override
    public void setIntensity(float f) {
        globalIntensity = f;
    }

    private boolean useOffChipCalibration = false;

    /**
     * @return the frameData
     */
    public CDVSLogIntensityFrameData getFrameData() {
        return frameData;
    }

    /**
     * @return the useOffChipCalibration
     */
    public boolean isUseOffChipCalibration() {
        return useOffChipCalibration;
    }

    /**
     * @param useOffChipCalibration the useOffChipCalibration to set
     */
    public void setUseOffChipCalibration(boolean useOffChipCalibration) {
        this.useOffChipCalibration = useOffChipCalibration;
        this.getFrameData().setUseOffChipCalibration(useOffChipCalibration);
        if (useOffChipCalibration) {
            this.getFrameData().setCalibData1();
        }
    }

    /** The event extractor. Each pixel has two polarities 0 and 1.
     * There is one extra neuron which signals absolute intensity.
     * <p>
     *The bits in the raw data coming from the device are as follows.
     * <p>
     *Bit 0 is polarity, on=1, off=0<br>
     *Bits 1-9 are x address (max value 320)<br>
     *Bits 10-17 are y address (max value 240) <br>
     *Bit 18 signals the special intensity neuron, 
     * but it always comes together with an x address.
     * It means there was an intensity spike AND a normal pixel spike.
     *<p>
     */
    public class cDVSTestExtractor extends RetinaExtractor {

        private int lastIntenTs = 0;

        public cDVSTestExtractor(cDVSTest30 chip) {
            super(chip);
        }

        private float avdt = 100;

        /** extracts the meaning of the raw events.
         *@param in the raw events, can be null
         *@return out the processed events. these are partially processed in-place. empty packet is returned if null is supplied as in.
         */
        @Override
        public synchronized EventPacket extractPacket(AEPacketRaw in) {
            if (out == null) {
                out = new EventPacket(chip.getEventClass());
            } else {
                out.clear();
            }
            if (in == null) {
                return out;
            }
            int n = in.getNumEvents();
            int skipBy = 1;
            if (isSubSamplingEnabled()) {
                while (n / skipBy > getSubsampleThresholdEventCount()) {
                    skipBy++;
                }
            }
            int[] datas = in.getAddresses();
            int[] timestamps = in.getTimestamps();
            OutputEventIterator outItr = out.outputIterator();
            for (int i = 0; i < n; i++) {
                int data = datas[i];
                if ((data & ADDRESS_TYPE_MASK) == ADDRESS_TYPE_EVENT) {
                    if ((data & INTENSITYMASK) == INTENSITYMASK) {
                        int dt = timestamps[i] - lastIntenTs;
                        if (dt > 50) {
                            avdt = 0.05f * dt + 0.95f * avdt;
                            setIntensity(1000f / avdt);
                        }
                        lastIntenTs = timestamps[i];
                    } else {
                        cDVSEvent e = (cDVSEvent) outItr.nextOutput();
                        e.address = data & EVENT_ADDRESS_MASK;
                        e.timestamp = (timestamps[i]);
                        e.polarity = (byte) (data & POLMASK);
                        e.x = (short) (((data & XMASK) >>> XSHIFT));
                        e.y = (short) ((data & YMASK) >>> YSHIFT);
                        if (e.x < SIZE_X_CDVS * 2) {
                            if ((e.y & 1) == 0) {
                                if (e.polarity == 1) {
                                    e.eventType = cDVSEvent.EventType.Brighter;
                                } else {
                                    e.eventType = cDVSEvent.EventType.Darker;
                                }
                            } else {
                                if (e.polarity == 0) {
                                    e.eventType = cDVSEvent.EventType.Bluer;
                                } else {
                                    e.eventType = cDVSEvent.EventType.Redder;
                                }
                            }
                            e.x = (short) (e.x >>> 1);
                            e.y = (short) (e.y >>> 1);
                        } else {
                            if (e.polarity == 1) {
                                e.eventType = cDVSEvent.EventType.Brighter;
                            } else {
                                e.eventType = cDVSEvent.EventType.Darker;
                            }
                        }
                    }
                } else if ((data & ADDRESS_TYPE_MASK) == ADDRESS_TYPE_ADC) {
                    if ((data & ADC_START_BIT) == ADC_START_BIT) {
                        getFrameData().swapBuffers();
                        getFrameData().setTimestamp(timestamps[i]);
                    }
                    getFrameData().put(data & ADC_DATA_MASK);
                }
            }
            return out;
        }
    }

    /** overrides the Chip setHardware interface to construct a biasgen if one doesn't exist already.
     * Sets the hardware interface and the bias generators hardware interface
     *@param hardwareInterface the interface
     */
    @Override
    public void setHardwareInterface(final HardwareInterface hardwareInterface) {
        this.hardwareInterface = hardwareInterface;
        cDVSTest30.cDVSTestBiasgen bg;
        try {
            if (getBiasgen() == null) {
                setBiasgen(bg = new cDVSTest30.cDVSTestBiasgen(this));
            } else {
                getBiasgen().setHardwareInterface((BiasgenHardwareInterface) hardwareInterface);
            }
        } catch (ClassCastException e) {
            log.warning(e.getMessage() + ": probably this chip object has a biasgen but the hardware interface doesn't, ignoring");
        }
    }

    /**
     * Describes ConfigurableIPots on cDVSTest retina chip as well as the other configuration bits which control, for example, which
     * outputs are selected. These are all configured by a single shared shift register.
     * <p>
     * With Rx=82k, measured Im=2uA. With Rx=27k, measured Im=8uA.
     * <p>
     * cDVSTest has a pair of shared shifted sources, one for n-type and one for p-type. These sources supply a regulated voltaqe near the power rail.
     * The shifted sources are programed by bits at the start of the global shared shift register.
     *
     * <p>
     * TODO check following javadoc
     *
     * The pr, foll, and refr biases use the lowpower bias for their p src bias and the pr, foll and refr pfets
     * all have their psrcs wired to this shifted p src bias. Also, the pr, foll and refr biases also drive the same
     * shifted psrc bias with their own shifted psrc bias. Therefore all 4 of these biases (pr, foll, refr, and lowpower) should
     * have the same bufferbias bits. This constraint is enforced by software.
     * <p>
     * The output configuration bits follow the bias values and consist of 12 bits that select 3 outputs.
     * <nl>
     * <li> Bits 0-3 select the current output.
     * <li> 5 copies of digital Bits 4-8 select the digital output.
     * <li> Bits
     *
     * @author tobi
     */
    public class cDVSTestBiasgen extends net.sf.jaer.biasgen.Biasgen {

        ArrayList<HasPreference> hasPreferencesList = new ArrayList<HasPreference>();

        private ConfigurableIPotRev0 pcas, diffOn, diffOff, diff, red, blue, amp;

        private ConfigurableIPotRev0 refr, pr, foll;

        cDVSTest30OutputControlPanel controlPanel;

        AllMuxes allMuxes = new AllMuxes();

        private ShiftedSourceBias ssn, ssp, ssnMid, sspMid;

        private ShiftedSourceBias[] ssBiases = new ShiftedSourceBias[4];

        private VPot thermometerDAC;

        cDVSTestHardwareInterfaceProxy adcProxy = new cDVSTestHardwareInterfaceProxy(cDVSTest30.this);

        ConfigBits configBits = new ConfigBits();

        int pos = 0;

        JPanel bPanel;

        JTabbedPane bgTabbedPane;

        /** Creates a new instance of cDVSTestBiasgen for cDVSTest with a given hardware interface
         *@param chip the chip this biasgen belongs to
         */
        public cDVSTestBiasgen(Chip chip) {
            super(chip);
            setName("cDVSTest");
            getMasterbias().setKPrimeNFet(55e-3f);
            getMasterbias().setMultiplier(9 * (24f / 2.4f) / (4.8f / 2.4f));
            getMasterbias().setWOverL(4.8f / 2.4f);
            ssn = new ShiftedSourceBias(this);
            ssn.setSex(Pot.Sex.N);
            ssn.setName("SSN");
            ssn.setTooltipString("n-type shifted source that generates a regulated voltage near ground");
            ssn.addObserver(this);
            ssp = new ShiftedSourceBias(this);
            ssp.setSex(Pot.Sex.P);
            ssp.setName("SSP");
            ssp.setTooltipString("p-type shifted source that generates a regulated voltage near Vdd");
            ssp.addObserver(this);
            ssnMid = new ShiftedSourceBias(this);
            ssnMid.setSex(Pot.Sex.N);
            ssnMid.setName("SSNMid");
            ssnMid.setTooltipString("n-type shifted source that generates a regulated voltage inside rail, about 2 diode drops from ground");
            ssnMid.addObserver(this);
            sspMid = new ShiftedSourceBias(this);
            sspMid.setSex(Pot.Sex.P);
            sspMid.setName("SSPMid");
            sspMid.setTooltipString("p-type shifted source that generates a regulated voltage about 2 diode drops from Vdd");
            sspMid.addObserver(this);
            ssBiases[0] = ssnMid;
            ssBiases[1] = ssn;
            ssBiases[2] = sspMid;
            ssBiases[3] = ssp;
            final float Vdd = 1.8f;
            DAC dac = new DAC(1, 8, 0, Vdd, Vdd);
            thermometerDAC = new VPot(cDVSTest30.this, "LogAmpRef", dac, 0, Type.NORMAL, Sex.N, 9, 0, "Voltage DAC for log intensity switched cap amplifier");
            thermometerDAC.addObserver(this);
            setPotArray(new IPotArray(this));
            try {
                pot("diff,n,normal,differencing amp");
                pot("ON,n,normal,DVS brighter threshold");
                pot("OFF,n,normal,DVS darker threshold");
                pot("Red,n,normal,Redder threshold");
                pot("Blue,n,normal,Bluer threshold");
                pot("Amp,n,normal,DVS ON threshold");
                pot("pcas,p,cascode,DVS ON threshold");
                pot("pixInvB,n,normal,pixel inverter bias");
                pot("pr,p,normal,photoreceptor bias current");
                pot("fb,p,normal,photoreceptor follower bias current");
                pot("refr,p,normal,DVS refractory current");
                pot("AReqPd,n,normal,request pulldown threshold");
                pot("AReqEndPd,n,normal,handshake state machine pulldown bias current");
                pot("AEPuX,p,normal,AER column pullup");
                pot("AEPuY,p,normal,AER row pullup");
                pot("If_threshold,n,normal,integrate and fire intensity neuroon threshold");
                pot("If_refractory,n,normal,integrate and fire intensity neuron refractory period bias current");
                pot("FollPadBias,n,normal,follower pad buffer bias current");
                pot("ROgate,p,normal,bias voltage for log readout transistor ");
                pot("ROcas,p,normal,bias voltage for log readout cascode ");
                pot("refcurrent,p,normal,reference current for log readout ");
                pot("RObuffer,n,normal,buffer bias for log readout");
            } catch (Exception e) {
                throw new Error(e.toString());
            }
            loadPreferences();
        }

        /**
         * Formats bits represented in a string as '0' or '1' as a byte array to be sent over the interface to the firmware, for loading
         * in big endian bit order, in order of the bytes sent starting with byte 0.
         * <p>
         * Because the firmware writes integral bytes it is important that the 
         * bytes sent to the device are padded with leading bits 
         * (at msbs of first byte) that are finally shifted out of the on-chip shift register.
         * 
         * Therefore <code>bitString2Bytes</code> should only be called ONCE, after the complete bit string has been assembled, unless it is known
         * the other bits are an integral number of bytes.
         * 
         * @param bitString in msb to lsb order from left end, where msb will be in msb of first output byte
         * @return array of bytes to send
         */
        protected byte[] bitString2Bytes(String bitString) {
            int nbits = bitString.length();
            int nbytes = (nbits % 8 == 0) ? (nbits / 8) : (nbits / 8 + 1);
            int npad = nbytes * 8 - nbits;
            String pad = new String(new char[npad]).replace("\0", "0");
            bitString = pad + bitString;
            byte[] byteArray = new byte[nbytes];
            int bit = 0;
            for (int bite = 0; bite < nbytes; bite++) {
                for (int i = 0; i < 8; i++) {
                    byteArray[bite] = (byte) ((0xff & byteArray[bite]) << 1);
                    if (bitString.charAt(bit) == '1') {
                        byteArray[bite] |= 1;
                    }
                    bit++;
                }
            }
            return byteArray;
        }

        @Override
        public void setHardwareInterface(BiasgenHardwareInterface hardwareInterface) {
            super.setHardwareInterface(hardwareInterface);
            if (hardwareInterface == null) {
                if (adcProxy != null) {
                    adcProxy.setHw(null);
                }
            } else if (hardwareInterface instanceof cDVSTestHardwareInterface) {
                adcProxy.setHw((cDVSTestHardwareInterface) hardwareInterface);
            } else {
                log.warning("cannot set ADC hardware interface proxy hardware interface to " + hardwareInterface + " because it is not a cDVSTestHardwareInterface");
            }
        }

        private void pot(String s) throws ParseException {
            try {
                String d = ",";
                StringTokenizer t = new StringTokenizer(s, d);
                if (t.countTokens() != 4) {
                    throw new Error("only " + t.countTokens() + " tokens in pot " + s + "; use , to separate tokens for name,sex,type,tooltip\nsex=n|p, type=normal|cascode");
                }
                String name = t.nextToken();
                String a;
                a = t.nextToken();
                Sex sex = null;
                if (a.equalsIgnoreCase("n")) {
                    sex = Sex.N;
                } else if (a.equalsIgnoreCase("p")) {
                    sex = Sex.P;
                } else {
                    throw new ParseException(s, s.lastIndexOf(a));
                }
                a = t.nextToken();
                Type type = null;
                if (a.equalsIgnoreCase("normal")) {
                    type = Type.NORMAL;
                } else if (a.equalsIgnoreCase("cascode")) {
                    type = Type.CASCODE;
                } else {
                    throw new ParseException(s, s.lastIndexOf(a));
                }
                String tip = t.nextToken();
                getPotArray().addPot(new ConfigurableIPotCDVSTest(this, name, pos++, type, sex, false, true, ConfigurableIPotCDVSTest.maxBitValue / 2, ConfigurableIPotCDVSTest.maxBuffeBitValue, pos, tip));
            } catch (Exception e) {
                throw new Error(e.toString());
            }
        }

        @Override
        public void loadPreferences() {
            super.loadPreferences();
            if (hasPreferencesList != null) {
                for (HasPreference hp : hasPreferencesList) {
                    hp.loadPreference();
                }
            }
            if (ssBiases != null) {
                for (ShiftedSourceBias ss : ssBiases) {
                    ss.loadPreferences();
                }
            }
            if (thermometerDAC != null) {
                thermometerDAC.loadPreferences();
            }
        }

        @Override
        public void storePreferences() {
            super.storePreferences();
            for (HasPreference hp : hasPreferencesList) {
                hp.storePreference();
            }
            if (ssBiases != null) {
                for (ShiftedSourceBias ss : ssBiases) {
                    ss.storePreferences();
                }
            }
            if (thermometerDAC != null) {
                thermometerDAC.storePreferences();
            }
        }

        /**
         *
         * Overrides the default method to add the custom control panel for configuring the cDVSTest output muxes.
         *
         * @return a new panel for controlling this bias generator functionally
         */
        @Override
        public JPanel buildControlPanel() {
            bPanel = new JPanel();
            bPanel.setLayout(new BorderLayout());
            bgTabbedPane = new JTabbedPane();
            JPanel combinedBiasShiftedSourcePanel = new JPanel();
            combinedBiasShiftedSourcePanel.setLayout(new BoxLayout(combinedBiasShiftedSourcePanel, BoxLayout.Y_AXIS));
            combinedBiasShiftedSourcePanel.add(super.buildControlPanel());
            combinedBiasShiftedSourcePanel.add(new ShiftedSourceControls(ssn));
            combinedBiasShiftedSourcePanel.add(new ShiftedSourceControls(ssp));
            combinedBiasShiftedSourcePanel.add(new ShiftedSourceControls(ssnMid));
            combinedBiasShiftedSourcePanel.add(new ShiftedSourceControls(sspMid));
            combinedBiasShiftedSourcePanel.add(new VPotGUIControl(thermometerDAC));
            bgTabbedPane.addTab("Biases", combinedBiasShiftedSourcePanel);
            bgTabbedPane.addTab("Output control", new cDVSTest30OutputControlPanel(cDVSTest30.this));
            final String tabTitle = "ADC control";
            bgTabbedPane.addTab(tabTitle, new ParameterControlPanel(adcProxy));
            bPanel.add(bgTabbedPane, BorderLayout.CENTER);
            bgTabbedPane.addMouseListener(new java.awt.event.MouseAdapter() {

                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    tabbedPaneMouseClicked(evt);
                }
            });
            bgTabbedPane.addTab("More config", configBits.makeControlPanel());
            bgTabbedPane.setSelectedIndex(getPrefs().getInt("cDVSTest30.bgTabbedPaneSelectedIndex", 0));
            return bPanel;
        }

        private void tabbedPaneMouseClicked(java.awt.event.MouseEvent evt) {
            getPrefs().putInt("cDVSTest30.bgTabbedPaneSelectedIndex", bgTabbedPane.getSelectedIndex());
        }

        /** Formats the data sent to the microcontroller to load bias and other configuration. */
        @Override
        public byte[] formatConfigurationBytes(Biasgen biasgen) {
            ByteBuffer bb = ByteBuffer.allocate(1000);
            byte[] biasBytes = super.formatConfigurationBytes(biasgen);
            String configBitsBits = configBits.getBitString();
            String muxBitsBits = allMuxes.getBitString();
            byte[] muxAndConfigBytes = bitString2Bytes((configBitsBits + muxBitsBits));
            bb.put(muxAndConfigBytes);
            byte vdac = (byte) thermometerDAC.getBitValue();
            bb.put(vdac);
            bb.put(biasBytes);
            for (ShiftedSourceBias ss : ssBiases) {
                bb.put(ss.getBinaryRepresentation());
            }
            byte[] allBytes = new byte[bb.position()];
            bb.flip();
            bb.get(allBytes);
            StringBuilder sb = new StringBuilder("bytes sent to FX2 to be loaded big endian for each byte in order \n");
            for (byte b : allBytes) {
                sb.append(String.format("%02x ", b));
            }
            log.info(sb.toString());
            return allBytes;
        }

        /** the change in current from an increase* or decrease* call */
        public final float RATIO = 1.05f;

        /** the minimum on/diff or diff/off current allowed by decreaseThreshold */
        public final float MIN_THRESHOLD_RATIO = 2f;

        public final float MAX_DIFF_ON_CURRENT = 6e-6f;

        public final float MIN_DIFF_OFF_CURRENT = 1e-9f;

        public synchronized void increaseThreshold() {
            if (diffOn.getCurrent() * RATIO > MAX_DIFF_ON_CURRENT) {
                return;
            }
            if (diffOff.getCurrent() / RATIO < MIN_DIFF_OFF_CURRENT) {
                return;
            }
            diffOn.changeByRatio(RATIO);
            diffOff.changeByRatio(1 / RATIO);
        }

        public synchronized void decreaseThreshold() {
            float diffI = diff.getCurrent();
            if (diffOn.getCurrent() / MIN_THRESHOLD_RATIO < diffI) {
                return;
            }
            if (diffOff.getCurrent() > diffI / MIN_THRESHOLD_RATIO) {
                return;
            }
            diffOff.changeByRatio(RATIO);
            diffOn.changeByRatio(1 / RATIO);
        }

        public synchronized void increaseRefractoryPeriod() {
            refr.changeByRatio(1 / RATIO);
        }

        public synchronized void decreaseRefractoryPeriod() {
            refr.changeByRatio(RATIO);
        }

        public synchronized void increaseBandwidth() {
            pr.changeByRatio(RATIO);
            foll.changeByRatio(RATIO);
        }

        public synchronized void decreaseBandwidth() {
            pr.changeByRatio(1 / RATIO);
            foll.changeByRatio(1 / RATIO);
        }

        public synchronized void moreONType() {
            diffOn.changeByRatio(1 / RATIO);
            diffOff.changeByRatio(RATIO);
        }

        public synchronized void moreOFFType() {
            diffOn.changeByRatio(RATIO);
            diffOff.changeByRatio(1 / RATIO);
        }

        /** Bits on the on-chip shift register but not an output mux control
        
         */
        class ConfigBits extends Observable implements HasPreference {

            final int TOTAL_NUM_BITS = 8;

            boolean value = false;

            class ConfigBit implements HasPreference {

                int position;

                boolean value = false;

                SelectAction action = new SelectAction();

                class SelectAction extends AbstractAction {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        select(!value);
                    }
                }

                ConfigBit(String name, int position, String desc) {
                    this.position = position;
                    action.putValue(Action.SHORT_DESCRIPTION, desc);
                    action.putValue(Action.NAME, name);
                }

                private String key() {
                    return "cDVSTest." + getClass().getSimpleName() + "." + name + ".value";
                }

                @Override
                public void loadPreference() {
                    select(getPrefs().getBoolean(key(), false));
                }

                @Override
                public void storePreference() {
                    getPrefs().putBoolean(key(), value);
                }

                void select(boolean yes) {
                    selectWithoutNotify(yes);
                    setChanged();
                    notifyObservers();
                }

                void selectWithoutNotify(boolean v) {
                    value = v;
                    try {
                        sendConfiguration(cDVSTest30.cDVSTestBiasgen.this);
                    } catch (HardwareInterfaceException ex) {
                        log.warning("selecting output: " + ex);
                    }
                }
            }

            ConfigBit pullupX = new ConfigBit("useStaticPullupX", 0, "turn on static pullup for X addresses (columns)"), pullupY = new ConfigBit("useStaticPullupY", 1, "turn on static pullup for Y addresses (rows)");

            /** Array of configuration bits at end of shift register on-chip */
            protected ConfigBit[] configBits = { pullupX, pullupY };

            @Override
            public void loadPreference() {
                for (ConfigBit b : configBits) {
                    b.loadPreference();
                }
            }

            @Override
            public void storePreference() {
                for (ConfigBit b : configBits) {
                    b.storePreference();
                }
            }

            /** Returns the bit string to send to the firmware to load a bit sequence for the config bits in the shift register;
             * bits are loaded big endian into shift register (msb first) and here returned string has msb at left-most position, i.e. left end of string.
             * @return big endian string of bits
             */
            String getBitString() {
                StringBuilder s = new StringBuilder();
                s.append("000000");
                s.append(pullupY.value ? "1" : "0");
                s.append(pullupX.value ? "1" : "0");
                return s.reverse().toString();
            }

            JPanel makeControlPanel() {
                JPanel pan = new JPanel();
                pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
                for (ConfigBit b : configBits) {
                    JRadioButton but = new JRadioButton(b.action);
                    but.setSelected(b.value);
                    pan.add(but);
                }
                return pan;
            }
        }

        /** A mux for selecting output on the on-chip configuration/biasgen shift register. */
        class OutputMux extends Observable implements HasPreference, RemoteControlled {

            int nSrBits;

            int nInputs;

            OutputMap map;

            private String name = "OutputMux";

            int selectedChannel = -1;

            String bitString = null;

            final String CMD_SELECTMUX = "selectMux_";

            /**
             *  A set of output mux channels.
             * @param nsr number of shift register bits
             * @param nin number of input ports to mux
             * @param m the map where the info is stored
             */
            OutputMux(int nsr, int nin, OutputMap m) {
                nSrBits = nsr;
                nInputs = nin;
                map = m;
                hasPreferencesList.add(this);
            }

            @Override
            public String toString() {
                return "OutputMux name=" + name + " nSrBits=" + nSrBits + " nInputs=" + nInputs + " selectedChannel=" + selectedChannel + " channelName=" + getChannelName(selectedChannel) + " code=" + getCode(selectedChannel) + " getBitString=" + bitString;
            }

            void select(int i) {
                selectWithoutNotify(i);
                setChanged();
                notifyObservers();
            }

            void selectWithoutNotify(int i) {
                selectedChannel = i;
                try {
                    sendConfiguration(cDVSTest30.cDVSTestBiasgen.this);
                } catch (HardwareInterfaceException ex) {
                    log.warning("selecting output: " + ex);
                }
            }

            void put(int k, String name) {
                map.put(k, name);
            }

            OutputMap getMap() {
                return map;
            }

            int getCode(int i) {
                return map.get(i);
            }

            /** Returns the bit string to send to the firmware to load a bit sequence for this mux in the shift register;
             * bits are loaded big endian, msb first but returned string has msb at right-most position, i.e. end of string.
             * @return big endian string e.g. code=11, s='1011', code=7, s='0111' for nSrBits=4.
             */
            String getBitString() {
                StringBuilder s = new StringBuilder();
                int code = selectedChannel != -1 ? getCode(selectedChannel) : 0;
                int k = nSrBits - 1;
                while (k >= 0) {
                    int x = code & (1 << k);
                    boolean b = (x == 0);
                    s.append(b ? '0' : '1');
                    k--;
                }
                bitString = s.toString();
                return bitString;
            }

            String getChannelName(int i) {
                return map.nameMap.get(i);
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            private String key() {
                return "cDVSTest." + getClass().getSimpleName() + "." + name + ".selectedChannel";
            }

            @Override
            public void loadPreference() {
                select(getPrefs().getInt(key(), -1));
            }

            @Override
            public void storePreference() {
                getPrefs().putInt(key(), selectedChannel);
            }

            /** Command is e.g. "selectMux_Currents 1".
             *
             * @param command the first token which dispatches the command here for this class of Mux.
             * @param input the command string.
             * @return some informative string for debugging bad commands.
             */
            @Override
            public String processRemoteControlCommand(RemoteControlCommand command, String input) {
                String[] t = input.split("\\s");
                if (t.length < 2) {
                    return "? " + this + "\n";
                } else {
                    String s = t[0], a = t[1];
                    try {
                        select(Integer.parseInt(a));
                        return this + "\n";
                    } catch (NumberFormatException e) {
                        log.warning("Bad number format: " + input + " caused " + e);
                        return e.toString() + "\n";
                    } catch (Exception ex) {
                        log.warning(ex.toString());
                        return ex.toString();
                    }
                }
            }
        }

        class OutputMap extends HashMap<Integer, Integer> {

            HashMap<Integer, String> nameMap = new HashMap<Integer, String>();

            void put(int k, int v, String name) {
                put(k, v);
                nameMap.put(k, name);
            }

            void put(int k, String name) {
                nameMap.put(k, name);
            }
        }

        class VoltageOutputMap extends OutputMap {

            final void put(int k, int v) {
                put(k, v, "Voltage " + k);
            }

            VoltageOutputMap() {
                put(0, 1);
                put(1, 3);
                put(2, 5);
                put(3, 7);
                put(4, 9);
                put(5, 11);
                put(6, 13);
                put(7, 15);
            }
        }

        class DigitalOutputMap extends OutputMap {

            DigitalOutputMap() {
                for (int i = 0; i < 16; i++) {
                    put(i, i, "DigOut " + i);
                }
            }
        }

        class VoltageOutputMux extends OutputMux {

            VoltageOutputMux(int n) {
                super(4, 8, new VoltageOutputMap());
                setName("Voltages" + n);
            }
        }

        class LogicMux extends OutputMux {

            LogicMux(int n) {
                super(4, 16, new DigitalOutputMap());
                setName("LogicSignals" + n);
            }
        }

        class AllMuxes extends ArrayList<OutputMux> {

            OutputMux[] vmuxes = { new VoltageOutputMux(1), new VoltageOutputMux(2), new VoltageOutputMux(3), new VoltageOutputMux(4) };

            OutputMux[] dmuxes = { new LogicMux(1), new LogicMux(2), new LogicMux(3), new LogicMux(4), new LogicMux(5) };

            String getBitString() {
                int nBits = 0;
                StringBuilder s = new StringBuilder();
                for (OutputMux m : this) {
                    s.append(m.getBitString());
                    nBits += m.nSrBits;
                }
                return s.toString();
            }

            AllMuxes() {
                addAll(Arrays.asList(dmuxes));
                addAll(Arrays.asList(vmuxes));
                dmuxes[0].setName("DigMux4");
                dmuxes[1].setName("DigMux3");
                dmuxes[2].setName("DigMux2");
                dmuxes[3].setName("DigMux1");
                dmuxes[4].setName("DigMux0");
                for (int i = 0; i < 5; i++) {
                    dmuxes[i].put(0, "nRxcolE");
                    dmuxes[i].put(1, "nAxcolE");
                    dmuxes[i].put(2, "nRY0");
                    dmuxes[i].put(3, "AY0");
                    dmuxes[i].put(4, "nAX0");
                    dmuxes[i].put(5, "nRXon");
                    dmuxes[i].put(6, "arbtopR");
                    dmuxes[i].put(7, "arbtopA");
                    dmuxes[i].put(8, "FF1");
                    dmuxes[i].put(9, "Acol");
                    dmuxes[i].put(10, "Rcol");
                    dmuxes[i].put(11, "Rrow");
                    dmuxes[i].put(12, "RxcolG");
                    dmuxes[i].put(13, "nArow");
                }
                dmuxes[0].put(14, "nResetRxcol");
                dmuxes[0].put(15, "nArowBottom");
                dmuxes[1].put(14, "AY1right");
                dmuxes[1].put(15, "nRY1right");
                dmuxes[2].put(14, "AY1right");
                dmuxes[2].put(15, "nRY1right");
                dmuxes[3].put(14, "FF2");
                dmuxes[3].put(15, "RCarb");
                dmuxes[4].put(14, "FF2");
                dmuxes[4].put(15, "RCarb");
                vmuxes[0].setName("AnaMux3");
                vmuxes[1].setName("AnaMux2");
                vmuxes[2].setName("AnaMux1");
                vmuxes[3].setName("AnaMux0");
                for (int i = 0; i < 4; i++) {
                    vmuxes[i].put(0, "readout");
                    vmuxes[i].put(1, "DiffAmpOut");
                    vmuxes[i].put(2, "InPh");
                }
                vmuxes[0].put(3, "refcurrent");
                vmuxes[0].put(4, "DiffAmpRef");
                vmuxes[0].put(5, "log");
                vmuxes[0].put(6, "Vt");
                vmuxes[0].put(7, "top");
                vmuxes[1].put(3, "refcurrent");
                vmuxes[1].put(4, "DiffAmpRef");
                vmuxes[1].put(5, "log");
                vmuxes[1].put(6, "Vt");
                vmuxes[1].put(7, "top");
                vmuxes[2].put(3, "phi1");
                vmuxes[2].put(4, "phi2");
                vmuxes[2].put(5, "Vcoldiff");
                vmuxes[2].put(6, "Vs");
                vmuxes[2].put(7, "sum");
                vmuxes[3].put(3, "phi1");
                vmuxes[3].put(4, "phi2");
                vmuxes[3].put(5, "Vcoldiff");
                vmuxes[3].put(6, "Vs");
                vmuxes[3].put(7, "sum");
            }
        }
    }

    /**
     * @return the displayLogIntensity
     */
    public boolean isDisplayLogIntensity() {
        return displayLogIntensity;
    }

    /**
     * @param displayLogIntensity the displayLogIntensity to set
     */
    public void setDisplayLogIntensity(boolean displayLogIntensity) {
        this.displayLogIntensity = displayLogIntensity;
        getPrefs().putBoolean("displayLogIntensity", displayLogIntensity);
        getAeViewer().interruptViewloop();
    }

    /**
     * @return the displayColorChangeEvents
     */
    public boolean isDisplayColorChangeEvents() {
        return displayColorChangeEvents;
    }

    /**
     * @param displayColorChangeEvents the displayColorChangeEvents to set
     */
    public void setDisplayColorChangeEvents(boolean displayColorChangeEvents) {
        this.displayColorChangeEvents = displayColorChangeEvents;
        getPrefs().putBoolean("displayColorChangeEvents", displayColorChangeEvents);
        getAeViewer().interruptViewloop();
    }

    /**
     * @return the displayLogIntensityChangeEvents
     */
    public boolean isDisplayLogIntensityChangeEvents() {
        return displayLogIntensityChangeEvents;
    }

    /**
     * @param displayLogIntensityChangeEvents the displayLogIntensityChangeEvents to set
     */
    public void setDisplayLogIntensityChangeEvents(boolean displayLogIntensityChangeEvents) {
        this.displayLogIntensityChangeEvents = displayLogIntensityChangeEvents;
        getPrefs().putBoolean("displayLogIntensityChangeEvents", displayLogIntensityChangeEvents);
        getAeViewer().interruptViewloop();
    }
}
