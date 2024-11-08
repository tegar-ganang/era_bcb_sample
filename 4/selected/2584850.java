package avrora.sim.mcu;

import avrora.sim.RWRegister;
import avrora.sim.Simulator;
import avrora.sim.state.RegisterView;
import avrora.sim.state.RegisterUtil;
import avrora.sim.clock.Clock;

/**
 * Base class of 8-bit timers. Timer0 and Timer2 are subclasses of this.
 *
 * @author Daniel Lee
 */
public abstract class Timer8Bit extends AtmelInternalDevice {

    public static final int MODE_NORMAL = 0;

    public static final int MODE_PWM = 1;

    public static final int MODE_CTC = 2;

    public static final int MODE_FASTPWM = 3;

    public static final int MAX = 0xff;

    public static final int BOTTOM = 0x00;

    final ControlRegister TCCRn_reg;

    final TCNTnRegister TCNTn_reg;

    final BufferedRegister OCRn_reg;

    protected final int n;

    protected Simulator.Event ticker;

    protected final Clock externalClock;

    protected Clock timerClock;

    protected int period;

    final AtmelMicrocontroller.Pin outputComparePin;

    final Simulator.Event[] tickers;

    boolean blockCompareMatch;

    final int OCIEn;

    final int TOIEn;

    final int OCFn;

    final int TOVn;

    protected ATMegaFamily.FlagRegister TIFR_reg;

    protected ATMegaFamily.MaskRegister TIMSK_reg;

    final int[] periods;

    protected Timer8Bit(AtmelMicrocontroller m, int n, int OCIEn, int TOIEn, int OCFn, int TOVn, int[] periods) {
        super("timer" + n, m);
        TCCRn_reg = new ControlRegister();
        TCNTn_reg = new TCNTnRegister();
        OCRn_reg = new BufferedRegister();
        TIFR_reg = (ATMegaFamily.FlagRegister) m.getIOReg("TIFR");
        TIMSK_reg = (ATMegaFamily.MaskRegister) m.getIOReg("TIMSK");
        externalClock = m.getClock("external");
        timerClock = mainClock;
        outputComparePin = (AtmelMicrocontroller.Pin) microcontroller.getPin("OC" + n);
        this.OCIEn = OCIEn;
        this.TOIEn = TOIEn;
        this.OCFn = OCFn;
        this.TOVn = TOVn;
        this.n = n;
        this.periods = periods;
        installIOReg("TCCR" + n, TCCRn_reg);
        installIOReg("TCNT" + n, TCNTn_reg);
        installIOReg("OCR" + n, OCRn_reg);
        tickers = new Simulator.Event[4];
        installTickers();
    }

    private void installTickers() {
        tickers[MODE_NORMAL] = new Mode_Normal();
        tickers[MODE_CTC] = new Mode_CTC();
        tickers[MODE_FASTPWM] = new Mode_FastPWM();
        tickers[MODE_PWM] = new Mode_PWM();
    }

    protected void compareMatch() {
        if (devicePrinter != null) {
            boolean enabled = TIMSK_reg.readBit(OCIEn);
            devicePrinter.println("Timer" + n + ".compareMatch (enabled: " + enabled + ')');
        }
        TIFR_reg.flagBit(OCFn);
    }

    protected void overflow() {
        if (devicePrinter != null) {
            boolean enabled = TIMSK_reg.readBit(TOIEn);
            devicePrinter.println("Timer" + n + ".overFlow (enabled: " + enabled + ')');
        }
        TIFR_reg.flagBit(TOVn);
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

    /**
     * <code>BufferedRegister</code> implements a register with a write buffer. In PWN modes, writes
     * to this register are not performed until flush() is called. In non-PWM modes, the writes are
     * immediate.
     */
    protected class BufferedRegister extends RWRegister {

        final RWRegister register;

        protected BufferedRegister() {
            this.register = new RWRegister();
        }

        public void write(byte val) {
            super.write(val);
            if (TCCRn_reg.mode == MODE_NORMAL || TCCRn_reg.mode == MODE_CTC) {
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

    protected class ControlRegister extends RWRegister {

        public static final int FOCn = 7;

        public static final int WGMn0 = 6;

        public static final int COMn1 = 5;

        public static final int COMn0 = 4;

        public static final int WGMn1 = 3;

        public static final int CSn2 = 2;

        public static final int CSn1 = 1;

        public static final int CSn0 = 0;

        final RegisterView CSn = RegisterUtil.bitRangeView(this, 0, 2);

        final RegisterView COMn = RegisterUtil.bitRangeView(this, 4, 5);

        final RegisterView WGMn = RegisterUtil.permutedView(this, new byte[] { 6, 3 });

        int mode = -1;

        int scale = -1;

        public void write(byte val) {
            value = (byte) (val & 0x7f);
            if ((val & 0x80) != 0) {
                forcedOutputCompare();
            }
            int nmode = WGMn.getValue();
            int nscale = CSn.getValue();
            if (nmode != mode || nscale != scale) {
                if (ticker != null) timerClock.removeEvent(ticker);
                mode = nmode;
                scale = nscale;
                ticker = tickers[mode];
                period = periods[scale];
                if (period != 0) {
                    timerClock.insertEvent(ticker, period);
                }
                if (devicePrinter != null) {
                    if (period != 0) devicePrinter.println("Timer" + n + " enabled: period = " + period + " mode = " + mode); else devicePrinter.println("Timer" + n + " disabled");
                }
            }
        }

        private void forcedOutputCompare() {
            int count = TCNTn_reg.read() & 0xff;
            int compare = OCRn_reg.read() & 0xff;
            if (count == compare) {
                switch(COMn.getValue()) {
                    case 1:
                        if (WGMn.getValue() == MODE_NORMAL || WGMn.getValue() == MODE_CTC) outputComparePin.write(!outputComparePin.read());
                        break;
                    case 2:
                        outputComparePin.write(false);
                        break;
                    case 3:
                        outputComparePin.write(true);
                        break;
                }
            }
        }
    }

    class Mode_Normal implements Simulator.Event {

        public void fire() {
            int ncount = (int) TCNTn_reg.read() & 0xff;
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

    class Mode_PWM implements Simulator.Event {

        protected byte increment = 1;

        public void fire() {
            int ncount = (int) TCNTn_reg.read() & 0xff;
            tickerStart(ncount);
            if (ncount >= MAX) {
                increment = -1;
                ncount = MAX;
                OCRn_reg.flush();
            } else if (ncount <= BOTTOM) {
                overflow();
                increment = 1;
                ncount = BOTTOM;
            }
            ncount += increment;
            tickerFinish(this, ncount);
        }
    }

    class Mode_CTC implements Simulator.Event {

        public void fire() {
            int ncount = (int) TCNTn_reg.read() & 0xff;
            tickerStart(ncount);
            if (ncount >= MAX) {
                overflow();
                ncount = BOTTOM;
            } else if (ncount == ((int) OCRn_reg.read() & 0xff)) {
                ncount = BOTTOM;
            } else {
                ncount++;
            }
            tickerFinish(this, ncount);
        }
    }

    class Mode_FastPWM implements Simulator.Event {

        public void fire() {
            int ncount = (int) TCNTn_reg.read() & 0xff;
            tickerStart(ncount);
            if (ncount >= MAX) {
                ncount = BOTTOM;
                overflow();
                OCRn_reg.flush();
            } else {
                ncount++;
            }
            tickerFinish(this, ncount);
        }
    }

    private void tickerStart(int count) {
        if (!blockCompareMatch && count == ((int) OCRn_reg.read() & 0xff)) {
            compareMatch();
        }
    }

    private void tickerFinish(Simulator.Event ticker, int ncount) {
        TCNTn_reg.write((byte) ncount);
        blockCompareMatch = false;
        timerClock.insertEvent(ticker, period);
    }
}
