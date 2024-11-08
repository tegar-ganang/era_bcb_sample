package avrora.sim.mcu;

import avrora.sim.RWRegister;
import avrora.sim.Simulator;
import avrora.sim.state.BooleanView;
import avrora.sim.clock.Clock;

/**
 * The <code>Timer16Bit</code> class emulates the functionality and behavior of a 16-bit timer on the
 * Atmega128. It has several control and data registers and can fire up to six different interrupts
 * depending on the mode that it has been put into. It has three output compare units and one input
 * capture unit. UNIMPLEMENTED: input capture unit.
 *
 * @author Daniel Lee
 */
public abstract class Timer16Bit extends AtmelInternalDevice {

    public static final int MODE_NORMAL = 0;

    public static final int MODE_PWM_PHASE_CORRECT_8_BIT = 1;

    public static final int MODE_PWM_PHASE_CORRECT_9_BIT = 2;

    public static final int MODE_PWM_PHASE_CORRECT_10_BIT = 3;

    public static final int MODE_CTC_OCRnA = 4;

    public static final int MODE_FASTPWM_8_BIT = 5;

    public static final int MODE_FASTPWM_9_BIT = 6;

    public static final int MODE_FASTPWM_10_BIT = 7;

    public static final int MODE_PWM_PNF_ICRn = 8;

    public static final int MODE_PWM_PNF_OCRnA = 9;

    public static final int MODE_PWN_PHASE_CORRECT_ICRn = 10;

    public static final int MODE_PWN_PHASE_CORRECT_OCRnA = 11;

    public static final int MODE_CTC_ICRn = 12;

    public static final int MODE_FASTPWM_ICRn = 14;

    public static final int MODE_FASTPWM_OCRnA = 15;

    public static final int MAX = 0xffff;

    public static final int BOTTOM = 0x0000;

    final RegisterSet.Field ICESn_flag;

    public class InputCapturePin implements BooleanView {

        boolean level;

        public boolean getValue() {
            return level;
        }

        public void setValue(boolean v) {
            if (v != level) {
                level = v;
                if ((ICESn_flag.value == 1) == level) captureInput();
            }
        }
    }

    /**
     * The <code>OutputCompareUnit</code> class represents an output compare unit that is
     * connected to the timer. The output compare unit functions by continually comparing the
     * count of the timer to a particular value, and signalling a match when complete.
     */
    class OutputCompareUnit {

        final BufferedRegister OCRnXH_reg;

        final BufferedRegister OCRnXL_reg;

        final OCRnxPairedRegister OCRnX_reg;

        final AtmelMicrocontroller.Pin outputComparePin;

        final RegisterSet.Field mode;

        final RegisterSet.Field force;

        final char unit;

        final int flagBit;

        final ATMegaFamily.FlagRegister flagReg;

        OutputCompareUnit(Microcontroller m, RegisterSet rset, char c, int fb, ATMegaFamily.FlagRegister fr) {
            unit = c;
            OCRnXH_reg = new BufferedRegister();
            OCRnXL_reg = new BufferedRegister();
            OCRnX_reg = new OCRnxPairedRegister(OCRnXH_reg, OCRnXL_reg);
            outputComparePin = (AtmelMicrocontroller.Pin) m.getPin("OC" + n + unit);
            mode = rset.getField("COM" + n + c);
            force = rset.installField("FOC" + n + c, new FOC_Field());
            flagBit = fb;
            flagReg = fr;
            installIOReg("OCR" + n + unit + "H", new OCRnxTempHighRegister(OCRnXH_reg));
            installIOReg("OCR" + n + unit + "L", OCRnX_reg);
        }

        class FOC_Field extends RegisterSet.Field {

            public void update() {
                if (value == 1) {
                    if (read16(TCNTnH_reg, TCNTnL_reg) == read()) {
                        output();
                    }
                }
            }
        }

        void forceCompare(int count) {
            if (count == read()) {
                output();
            }
        }

        void compare(int count) {
            if (count == read()) {
                output();
                flagReg.flagBit(flagBit);
            }
        }

        void flush() {
            OCRnXH_reg.flush();
            OCRnXL_reg.flush();
        }

        private void output() {
            switch(mode.value) {
                case 1:
                    outputComparePin.write(!outputComparePin.read());
                    break;
                case 2:
                    outputComparePin.write(false);
                    break;
                case 3:
                    outputComparePin.write(true);
                    break;
            }
        }

        int read() {
            return read16(OCRnXH_reg, OCRnXL_reg);
        }
    }

    final RWRegister TCNTnH_reg;

    final TCNTnRegister TCNTnL_reg;

    final PairedRegister TCNTn_reg;

    final OutputCompareUnit[] compareUnits;

    final Simulator.Event[] tickers;

    final RWRegister highTempReg;

    final RWRegister ICRnH_reg;

    final RWRegister ICRnL_reg;

    final PairedRegister ICRn_reg;

    Simulator.Event ticker;

    final RegisterSet.Field WGMn;

    final RegisterSet.Field CSn;

    final InputCapturePin inputCapturePin;

    long period;

    boolean blockCompareMatch;

    protected final Clock externalClock;

    Clock timerClock;

    final int n;

    int OCIEnA;

    int OCIEnB;

    int OCIEnC;

    int TOIEn;

    int TOVn;

    int OCFnA;

    int OCFnB;

    int OCFnC;

    int ICFn;

    int inputCaptureInterrupt;

    protected ATMegaFamily.FlagRegister xTIFR_reg;

    protected ATMegaFamily.MaskRegister xTIMSK_reg;

    protected ATMegaFamily.FlagRegister cTIFR_reg;

    protected ATMegaFamily.MaskRegister cTIMSK_reg;

    protected int[] periods;

    protected abstract void initValues();

    protected Timer16Bit(int n, int numUnits, AtmelMicrocontroller m) {
        super("timer" + n, m);
        this.n = n;
        RegisterSet rset = m.getRegisterSet();
        initValues();
        WGMn = rset.installField("WGM" + n, new RegisterSet.Field() {

            public void update() {
                resetTicker(tickers[value]);
            }
        });
        CSn = rset.installField("CS" + n, new RegisterSet.Field() {

            public void update() {
                resetPeriod(periods[value]);
            }
        });
        inputCaptureInterrupt = m.getProperties().getInterrupt("TIMER" + n + " CAPT");
        inputCapturePin = new InputCapturePin();
        highTempReg = new RWRegister();
        compareUnits = new OutputCompareUnit[numUnits];
        newOCU(0, numUnits, m, rset, 'A', OCFnA, xTIFR_reg);
        newOCU(1, numUnits, m, rset, 'B', OCFnB, xTIFR_reg);
        newOCU(2, numUnits, m, rset, 'C', OCFnC, cTIFR_reg);
        TCNTnH_reg = new RWRegister();
        TCNTnL_reg = new TCNTnRegister();
        TCNTn_reg = new PairedRegister(TCNTnH_reg, TCNTnL_reg);
        ICRnH_reg = new RWRegister();
        ICRnL_reg = new RWRegister();
        ICRn_reg = new PairedRegister(ICRnL_reg, ICRnH_reg);
        ICESn_flag = rset.getField("ICES" + n);
        externalClock = m.getClock("external");
        timerClock = mainClock;
        installIOReg("TCNT" + n + "H", highTempReg);
        installIOReg("TCNT" + n + "L", TCNTn_reg);
        installIOReg("ICR" + n + "H", highTempReg);
        installIOReg("ICR" + n + "L", ICRn_reg);
        tickers = new Simulator.Event[16];
        installTickers();
    }

    private void installTickers() {
        Timer16Bit.OutputCompareUnit ocA = compareUnits[0];
        Timer16Bit.BufferedRegister ocrah = ocA.OCRnXH_reg;
        Timer16Bit.BufferedRegister ocral = ocA.OCRnXL_reg;
        tickers[MODE_NORMAL] = new Mode_Normal();
        tickers[MODE_PWM_PHASE_CORRECT_8_BIT] = new Mode_PWMPhaseCorrect(0xff, null, null);
        tickers[MODE_PWM_PHASE_CORRECT_9_BIT] = new Mode_PWMPhaseCorrect(0x1ff, null, null);
        tickers[MODE_PWM_PHASE_CORRECT_10_BIT] = new Mode_PWMPhaseCorrect(0x3ff, null, null);
        tickers[MODE_CTC_OCRnA] = new Mode_CTC(ocrah, ocral);
        tickers[MODE_FASTPWM_8_BIT] = new Mode_FastPWM(0xff, null, null);
        tickers[MODE_FASTPWM_9_BIT] = new Mode_FastPWM(0x1ff, null, null);
        tickers[MODE_FASTPWM_10_BIT] = new Mode_FastPWM(0x3ff, null, null);
        tickers[MODE_PWM_PNF_ICRn] = new Mode_PWM_PNF(ICRnH_reg, ICRnL_reg);
        tickers[MODE_PWM_PNF_OCRnA] = new Mode_PWM_PNF(ocrah, ocral);
        tickers[MODE_PWN_PHASE_CORRECT_ICRn] = new Mode_PWMPhaseCorrect(0, ICRnH_reg, ICRnL_reg);
        tickers[MODE_PWN_PHASE_CORRECT_OCRnA] = new Mode_PWMPhaseCorrect(0, ocrah, ocral);
        tickers[MODE_CTC_ICRn] = new Mode_CTC(ICRnH_reg, ICRnL_reg);
        tickers[13] = new Mode_Reserved();
        tickers[MODE_FASTPWM_ICRn] = new Mode_FastPWM(0, ICRnH_reg, ICRnL_reg);
        tickers[MODE_FASTPWM_OCRnA] = new Mode_FastPWM(0, ocrah, ocral);
    }

    public BooleanView getInputCapturePin() {
        return inputCapturePin;
    }

    void captureInput() {
        ICRnL_reg.write(TCNTnL_reg.value);
        ICRnH_reg.write(TCNTnH_reg.value);
        xTIFR_reg.flagBit(ICFn);
        interpreter.getInterruptTable().post(inputCaptureInterrupt);
    }

    void newOCU(int unit, int numUnits, Microcontroller m, RegisterSet rset, char uname, int fb, ATMegaFamily.FlagRegister fr) {
        if (unit < numUnits) {
            compareUnits[unit] = new OutputCompareUnit(m, rset, uname, fb, fr);
        }
    }

    /**
     * Flags the overflow interrupt for this timer.
     */
    protected void overflow() {
        if (devicePrinter != null) {
            boolean enabled = xTIMSK_reg.readBit(TOIEn);
            devicePrinter.println("Timer" + n + ".overFlow (enabled: " + enabled + ')' + "  ");
        }
        xTIFR_reg.flagBit(TOVn);
    }

    /**
     * The <code>PairedRegister</code> class exists to implement the shared temporary register for the
     * high byte of the 16-bit registers corresponding to a 16-bit timer. Accesses to the high byte of
     * a register pair should go through this temporary byte. According to the manual, writes to the
     * high byte are stored in the temporary register. When the low byte is written to, both the low
     * and high byte are updated. On a read, the temporary high byte is updated when a read occurs on
     * the low byte. The PairedRegister should be installed in place of the low register. Reads/writes
     * on this register will act accordingly on the low register, as well as initiate a read/write on
     * the associated high register.
     */
    protected class PairedRegister extends RWRegister {

        RWRegister high;

        RWRegister low;

        PairedRegister(RWRegister high, RWRegister low) {
            this.high = high;
            this.low = low;
        }

        public void write(byte val) {
            low.write(val);
            high.write(highTempReg.read());
        }

        public byte read() {
            highTempReg.write(high.read());
            return low.read();
        }
    }

    /**
     * The normal 16-bit read behavior described in the doc for PairedRegister does not apply for the
     * OCRnx registers. Reads on the OCRnxH registers are direct.
     */
    protected class OCRnxPairedRegister extends PairedRegister {

        OCRnxPairedRegister(RWRegister high, RWRegister low) {
            super(high, low);
        }

        public byte read() {
            return low.read();
        }
    }

    /**
     * See doc for OCRnxPairedRegister.
     */
    protected class OCRnxTempHighRegister extends RWRegister {

        RWRegister register;

        OCRnxTempHighRegister(RWRegister register) {
            this.register = register;
        }

        public void write(byte val) {
            highTempReg.write(val);
        }

        public byte read() {
            return register.read();
        }
    }

    /**
     * Overloads the write behavior of this class of register in order to implement compare match
     * blocking for one timer period.
     */
    protected class TCNTnRegister extends RWRegister {

        public void write(byte val) {
            value = val;
            blockCompareMatch = true;
        }
    }

    private void resetPeriod(int nPeriod) {
        if (nPeriod == 0) {
            if (devicePrinter != null) devicePrinter.println("Timer" + n + " disabled");
            if (ticker != null) timerClock.removeEvent(ticker);
        } else {
            if (devicePrinter != null) devicePrinter.println("Timer" + n + " enabled: period = " + nPeriod + " mode = " + WGMn.value);
            if (ticker != null) timerClock.removeEvent(ticker);
            ticker = tickers[WGMn.value];
            period = nPeriod;
            timerClock.insertEvent(ticker, period);
        }
    }

    public void resetTicker(Simulator.Event e) {
        if (ticker != null) simulator.removeEvent(ticker);
        ticker = e;
        simulator.insertEvent(e, period);
    }

    /**
     * In PWN modes, writes to the OCRnx registers are buffered. Specifically, the actual write is
     * delayed until a certain event (the counter reaching either TOP or BOTTOM) specified by the
     * particular PWN mode. BufferedRegister implements this by writing to a buffer register on a
     * write and reading from the buffered register in a read. When the buffered register is to be
     * updated, the flush() method should be called.
     */
    protected class BufferedRegister extends RWRegister {

        final RWRegister register;

        protected BufferedRegister() {
            this.register = new RWRegister();
        }

        public void write(byte val) {
            super.write(val);
            int mode = WGMn.value;
            if (mode == MODE_NORMAL || mode == MODE_CTC_OCRnA || mode == MODE_CTC_ICRn) {
                flush();
            }
        }

        public byte readBuffer() {
            return super.read();
        }

        public byte read() {
            return register.read();
        }

        protected void flush() {
            register.write(value);
        }
    }

    protected class Mode_Reserved implements Simulator.Event {

        public void fire() {
        }
    }

    protected class Mode_Normal implements Simulator.Event {

        public void fire() {
            int ncount = read16(TCNTnH_reg, TCNTnL_reg);
            tickerStart(ncount);
            if (ncount >= MAX) {
                overflow();
                ncount = BOTTOM;
            } else {
                ncount++;
            }
            tickerFinish(this, ncount);
        }
    }

    protected class Mode_CTC implements Simulator.Event {

        protected final RWRegister compareRegHigh;

        protected final RWRegister compareRegLow;

        public Mode_CTC(RWRegister compareRegH, RWRegister compareRegL) {
            compareRegHigh = compareRegH;
            compareRegLow = compareRegL;
        }

        public void fire() {
            int ncount = read16(TCNTnH_reg, TCNTnL_reg);
            tickerStart(ncount);
            if (ncount >= MAX) {
                overflow();
                ncount = BOTTOM;
            } else if (compareRegHigh != null && ncount == read16(compareRegHigh, compareRegLow)) {
                ncount = BOTTOM;
            } else {
                ncount++;
            }
            tickerFinish(this, ncount);
        }
    }

    protected class Mode_FastPWM implements Simulator.Event {

        protected final int top;

        protected final RWRegister compareRegHigh;

        protected final RWRegister compareRegLow;

        protected Mode_FastPWM(int t, RWRegister compareRegH, RWRegister compareRegL) {
            top = t;
            compareRegHigh = compareRegH;
            compareRegLow = compareRegL;
        }

        public void fire() {
            int ncount = read16(TCNTnH_reg, TCNTnL_reg);
            tickerStart(ncount);
            int top = this.top;
            if (compareRegHigh != null) {
                top = read16(compareRegHigh, compareRegLow);
            }
            if (ncount == top) {
                ncount = BOTTOM;
                overflow();
                flushOCRnx();
            } else if (ncount >= MAX) {
                overflow();
                ncount = BOTTOM;
            } else {
                ncount++;
            }
            tickerFinish(this, ncount);
        }
    }

    protected class Mode_PWM_PNF implements Simulator.Event {

        protected byte increment = 1;

        protected final RWRegister compareRegHigh;

        protected final RWRegister compareRegLow;

        protected Mode_PWM_PNF(RWRegister compareRegH, RWRegister compareRegL) {
            compareRegHigh = compareRegH;
            compareRegLow = compareRegL;
        }

        public void fire() {
            int ncount = read16(TCNTnH_reg, TCNTnL_reg);
            tickerStart(ncount);
            int compare = MAX;
            if (compareRegHigh != null) {
                compare = read16(compareRegHigh, compareRegLow);
            }
            if (ncount == compare) {
                increment = -1;
            } else if (ncount <= BOTTOM) {
                overflow();
                flushOCRnx();
                increment = 1;
                ncount = BOTTOM;
            } else if (ncount >= MAX) {
                ncount = BOTTOM - 1;
            }
            ncount += increment;
            tickerFinish(this, ncount);
        }
    }

    protected class Mode_PWMPhaseCorrect implements Simulator.Event {

        protected byte increment = 1;

        protected final int top;

        protected final RWRegister compareRegHigh;

        protected final RWRegister compareRegLow;

        protected Mode_PWMPhaseCorrect(int t, RWRegister compareRegH, RWRegister compareRegL) {
            top = t;
            compareRegHigh = compareRegH;
            compareRegLow = compareRegL;
        }

        public void fire() {
            int ncount = read16(TCNTnH_reg, TCNTnL_reg);
            tickerStart(ncount);
            int top = this.top;
            if (compareRegHigh != null) {
                top = read16(compareRegHigh, compareRegLow);
            }
            if (ncount == top) {
                increment = -1;
                flushOCRnx();
            } else if (ncount <= BOTTOM) {
                overflow();
                increment = 1;
                ncount = BOTTOM;
            } else if (ncount >= MAX) {
                ncount = BOTTOM - 1;
            }
            ncount += increment;
            tickerFinish(this, ncount);
        }
    }

    private void tickerStart(int count) {
        if (!blockCompareMatch) {
            for (int cntr = 0; cntr < compareUnits.length; cntr++) compareUnits[cntr].compare(count);
        }
    }

    private void tickerFinish(Simulator.Event ticker, int ncount) {
        write16(ncount, TCNTnH_reg, TCNTnL_reg);
        blockCompareMatch = false;
        if (period != 0) timerClock.insertEvent(ticker, period);
    }

    private void flushOCRnx() {
        for (int cntr = 0; cntr < compareUnits.length; cntr++) compareUnits[cntr].flush();
    }
}
