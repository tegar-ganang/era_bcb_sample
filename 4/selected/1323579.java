package avrora.sim.mcu;

import avrora.sim.*;
import avrora.sim.clock.Clock;
import cck.util.Util;
import java.util.*;

/**
 * The <code>ATMegaTimer</code> class implements a timer on the ATMega series
 * of microcontrollers.
 *
 * @author Pekka Nikander
 */
public abstract class ATMegaTimer extends AtmelInternalDevice {

    Mode mode;

    final Map comparators = new HashMap();

    boolean timerEnabled;

    boolean countUp;

    long period;

    boolean compareMatchBlocked;

    protected final Clock externalClock;

    Clock timerClock;

    int timerNumber;

    final RegisterSet.Field TOIEn;

    final FlagField TOVn;

    final RegisterSet.Field WGMn;

    final RegisterSet.Field CSn;

    private final int[] periods;

    protected ATMegaTimer(int n, AtmelMicrocontroller m, int[] p, String ovfName) {
        super("Timer" + n, m);
        timerNumber = n;
        periods = p;
        RegisterSet rset = m.getRegisterSet();
        TOIEn = rset.getField("TOIE" + n);
        int overflowInterrupt = m.properties.getInterrupt(ovfName);
        TOVn = new FlagField(interpreter.getInterruptTable(), true, overflowInterrupt);
        rset.installField("TOV" + n, TOVn);
        WGMn = rset.installField("WGM" + n, newWGMField());
        CSn = rset.installField("CS" + n, newPeriodField());
        externalClock = m.getClock("external");
        timerClock = mainClock;
    }

    private RegisterSet.Field newPeriodField() {
        return new RegisterSet.Field() {

            public void update() {
                resetPeriod(periods[value]);
            }
        };
    }

    private RegisterSet.Field newWGMField() {
        return new RegisterSet.Field() {

            public void update() {
                resetMode(value);
            }
        };
    }

    protected void addComparator(String name, Comparator comparator) {
        comparators.put(name, comparator);
    }

    protected Comparator getComparator(String name) {
        return (Comparator) comparators.get(name);
    }

    /**
     * Resets the clock period.  Called whenever CSn is assigned.
     */
    private void resetPeriod(int nPeriod) {
        if (nPeriod == 0) {
            if (timerEnabled) {
                if (devicePrinter != null) devicePrinter.println(name + " disabled");
                timerClock.removeEvent(mode);
                timerEnabled = false;
            }
            return;
        }
        if (timerEnabled) {
            timerClock.removeEvent(mode);
        }
        if (devicePrinter != null) devicePrinter.println(name + " enabled: period = " + nPeriod + " mode = " + WGMn.value);
        period = nPeriod;
        timerEnabled = true;
        timerClock.insertEvent(mode, period);
    }

    /**
     * Resets the mode according to the WGMn bits.
     */
    public abstract void resetMode(int WGMn);

    /**
     * Returns current counter value
     */
    public abstract int getCounter();

    /**
     * Sets the current counter value
     */
    public abstract void setCounter(int count);

    public abstract String getCounterName();

    /**
     * Returns the size-specific MAX value.
     */
    protected abstract int getMax();

    /**
     * Sets the overflow flag (TOVn) for this timer.
     */
    protected void signalOverflow() {
        if (devicePrinter != null) {
            devicePrinter.println(name + ".overFlow (interrupts enabled: " + TOIEn.value + ')');
        }
        TOVn.flag();
    }

    /**
     * The <code>TopValue</code> interface allows the mode-specific TOP value to be abstracted.  Depending on
     * the mode, TOP may be either defined by a register or by a constant.
     */
    interface TopValue {

        public int mask();

        public int read16();

        public void flush();
    }

    /**
     * The <code>FlagField</code> implements a <code>Field</code> that works as an interrupt posted flag.
     * <p/>
     * XXX: This is a wrong place for this class, since it is generic and not Timer specific.  Please refactor
     * it to elsewhere.  However, I didn't want to create a dependency between RegisterSets and
     * InterruptTables, and therefore didn't initially implement this in RegisterSet.
     */
    public class FlagField extends RegisterSet.Field implements InterruptTable.Notification {

        InterruptTable interrupts;

        int inum;

        boolean autoclear;

        public FlagField(InterruptTable it, boolean auto, int in) {
            interrupts = it;
            autoclear = auto;
            inum = in;
            interrupts.registerInternalNotification(this, inum);
        }

        public void update() {
            if (0 != value) {
                interrupts.post(inum);
            } else {
                interrupts.unpost(inum);
            }
        }

        public void flag() {
            write(1);
        }

        public void unflag() {
            write(0);
        }

        public void force(int inum) {
            flag();
        }

        public void invoke(int inum) {
            if (autoclear) {
                unflag();
            }
        }
    }

    /**
     * The <code>Mode</code> implements the mode dependent, periodic behavior of the timer. It emulates the
     * operation of the timer at each clock cycle and uses the global timed event queue to achieve the correct
     * periodic behavior.
     * <p/>
     * The Mode abstract the specifics of the timer mode into a simpler interface, allowing the commonality
     * between the different ATMega timers to be implemented by a single, fairly simple set of methods.
     */
    protected class Mode implements Simulator.Event {

        /**
         * A mode-dependint TOP value, either a constant or a register, typically COMnA or COMnI.
         */
        final TopValue top;

        /**
         * A mode-dependent FLAG bit, set when TOP is hit. May be <code>null</code>.
         */
        final FlagField flag;

        /**
         * A mode-specific algorithm object, specifying the details of counting.
         */
        final Strategy strategy;

        /**
         * Creates a new, mutable Mode object.
         */
        protected Mode(Class sc, RegisterSet.Field f, ActiveRegister t) {
            this(sc, f, (TopValue) t);
        }

        /**
         * Creates a new, mutable Mode object.
         */
        protected Mode(Class sc, RegisterSet.Field f, TopValue t) {
            if (NORMAL.class == sc) {
                strategy = new NORMAL();
            } else if (CTC.class == sc) {
                strategy = new CTC();
            } else if (PWM.class == sc) {
                strategy = new PWM();
            } else if (FC_PWM.class == sc) {
                strategy = new FC_PWM();
            } else if (FAST_PWM.class == sc) {
                strategy = new FAST_PWM();
            } else {
                throw new Error("Unknown Strategy class " + sc);
            }
            flag = (FlagField) f;
            top = t;
        }

        /**
         * Returns the mode-specific TOP value: fixed, OCRnA, OCRnI, ...
         */
        protected int getTop() {
            return top.read16();
        }

        /**
         * Signals hitting the top; depending on the mode, sets the corresponding signal bit.
         */
        protected void signalTop() {
            if (null != flag) flag.flag();
        }

        /**
         * Updates the TOP register; called either at TOP or BOTTOM, depending on the mode
         */
        protected void updateTop() {
            top.flush();
        }

        /**
         * Called by the appropriate clock whenever the strategy should tick.
         */
        public void fire() {
            int value = getCounter();
            if (devicePrinter != null) {
                devicePrinter.println(name + " [" + getCounterName() + " = " + value);
                Iterator i = comparators.values().iterator();
                for (Comparator c = (Comparator) i.next(); c != null; c = (Comparator) i.next()) {
                    devicePrinter.println(", " + c + "(actual) = " + c.read() + ", " + c + "(buffer) = " + c.readBuffer() + ']');
                }
            }
            value = strategy.nextValue(value);
            if (!compareMatchBlocked) {
                Iterator i = comparators.values().iterator();
                while (i.hasNext()) ((Comparator) i.next()).compare(value);
            }
            setCounter(value);
            compareMatchBlocked = false;
            if (period != 0) timerClock.insertEvent(this, period);
        }

        protected void registerWritten(BufferedRegister reg) {
            strategy.registerWritten(reg);
        }

        protected abstract class Strategy {

            protected abstract int nextValue(int count);

            protected abstract void registerWritten(BufferedRegister reg);
        }

        protected class NORMAL extends Strategy {

            protected int nextValue(int count) {
                count++;
                if (getMax() + 1 == count) {
                    signalOverflow();
                    count = 0;
                }
                return count;
            }

            protected void registerWritten(BufferedRegister reg) {
                reg.flush();
            }
        }

        protected class CTC extends Strategy {

            protected int nextValue(int count) {
                count++;
                if (getTop() == count) {
                    signalTop();
                    count = 0;
                }
                if (getMax() + 1 == count) {
                    signalOverflow();
                    count = 0;
                }
                return count;
            }

            protected void registerWritten(BufferedRegister reg) {
                reg.flush();
            }
        }

        protected class FAST_PWM extends Strategy {

            boolean zero = false;

            protected int nextValue(int count) {
                count++;
                if (zero) {
                    zero = false;
                    count = 0;
                    updateTop();
                    signalOverflow();
                }
                if (getTop() == count) {
                    zero = true;
                    signalTop();
                }
                return count;
            }

            protected void registerWritten(BufferedRegister reg) {
                reg.value &= top.mask();
            }
        }

        protected class PWM extends Strategy {

            protected int nextValue(int count) {
                if (countUp) count++; else count--;
                if (count == getTop()) {
                    countUp = false;
                    signalTop();
                    updateTop();
                }
                if (count == 0) {
                    countUp = true;
                    signalOverflow();
                }
                return count;
            }

            protected void registerWritten(BufferedRegister reg) {
                reg.value &= top.mask();
            }
        }

        protected class FC_PWM extends Strategy {

            protected int nextValue(int count) {
                if (countUp) count++; else count--;
                if (count == getTop()) {
                    countUp = false;
                    signalTop();
                }
                if (count == 0) {
                    countUp = true;
                    signalOverflow();
                    updateTop();
                }
                return count;
            }

            protected void registerWritten(BufferedRegister reg) {
            }
        }
    }

    /**
     * In some CTC and PWM modes, the TOP values is a fixed one.
     */
    static class FixedTop implements TopValue {

        public static final FixedTop FF = new FixedTop(0xFF);

        public static final FixedTop _1FF = new FixedTop(0x1FF);

        public static final FixedTop _3FF = new FixedTop(0x3FF);

        public static final FixedTop FFFF = new FixedTop(0xFFFF);

        final int top;

        protected FixedTop(int t) {
            top = t;
        }

        public int read16() {
            return top;
        }

        public int mask() {
            return top;
        }

        public void flush() {
            throw Util.failure("Fixed top value flushed");
        }
    }

    /**
     * Abstract base class for InputCompareUnits and OutputCompareUnits.
     * <p/>
     * Implements the common functionality between 8- and 16-bit, Input and Output units.
     */
    abstract class Comparator {

        public static final String _ = "";

        public static final String A = "A";

        public static final String B = "B";

        public static final String C = "C";

        public static final String I = "I";

        final String type;

        final String unit;

        final AtmelMicrocontroller.Pin pin;

        final FlagField flag;

        Comparator(String t, String u, RegisterSet rset, int interruptNumber, AtmelMicrocontroller.Pin p) {
            type = t;
            unit = u;
            pin = p;
            InterruptTable it = interpreter.getInterruptTable();
            flag = new FlagField(it, true, interruptNumber);
            rset.installField(type + "F" + timerNumber + unit, flag);
        }

        public String toString() {
            return type + "R" + timerNumber + unit;
        }

        void compare(int count) {
            if (read() == count) {
                operate();
                flag.flag();
            }
        }

        protected abstract void operate();

        abstract int read();

        abstract int readBuffer();
    }

    abstract class OutputComparator extends Comparator {

        final RegisterSet.Field pinmode;

        final RegisterSet.Field force;

        OutputComparator(String u, RegisterSet rset, int interruptNumber, AtmelMicrocontroller.Pin p) {
            super("OC", u, rset, interruptNumber, p);
            pinmode = rset.getField("COM" + timerNumber + unit);
            force = rset.installField("FOC" + timerNumber + unit, new RegisterSet.Field() {

                public void update() {
                    if (1 == value) {
                        operate();
                    }
                }
            });
        }

        protected void operate() {
            if (null == pin) return;
            switch(pinmode.value) {
                case 1:
                    pin.write(!pin.read());
                    break;
                case 2:
                    pin.write(false);
                    break;
                case 3:
                    pin.write(true);
                    break;
            }
        }
    }

    abstract class InputComparator extends Comparator {

        InputComparator(String u, RegisterSet rset, int interruptNumber, AtmelMicrocontroller.Pin p) {
            super("IC", u, rset, interruptNumber, p);
        }

        protected void operate() {
        }
    }

    /**
     * Overloads the write behavior of this class of register in order to implement compare match blocking for
     * one timer period.
     * <p/>
     * XXX: Make this into a facade
     */
    protected class TCNTnRegister implements ActiveRegister {

        public final String name;

        private final ActiveRegister register;

        protected TCNTnRegister(String n, ActiveRegister r) {
            name = n;
            register = r;
        }

        public void write(byte val) {
            register.write(val);
            compareMatchBlocked = true;
        }

        public byte read() {
            return register.read();
        }
    }

    /**
     * In PWM modes, writes to the OCRnx registers may be buffered, depending on the mode. Specifically, the
     * actual write may be delayed until a certain event (the counter reaching either TOP or BOTTOM) specified
     * by the particular PWN mode. BufferedRegister implements this by writing to a buffer register on a write
     * and reading from the buffered register in a read. When the buffered register is to be updated, the
     * <code>flush()</code> method should be called.
     * <p/>
     * Note that the underlying register may be either an 8-bit or 16-bit register; the BufferedRegister is
     * oblivious to that.
     */
    protected class BufferedRegister extends RW16Register implements TopValue, ActiveRegister {

        int value;

        private final ActiveRegister reg8;

        private final RW16Register reg16;

        protected BufferedRegister(ActiveRegister r) {
            this.reg8 = r;
            this.reg16 = null;
        }

        protected BufferedRegister(RW16Register r) {
            this.reg16 = r;
            this.reg8 = null;
        }

        public void write(byte val) {
            value = val;
            mode.registerWritten(this);
        }

        public void write(int val) {
            value = val;
            mode.registerWritten(this);
        }

        public int readBuffer() {
            return super.read16();
        }

        public byte read() {
            return (byte) read16();
        }

        public int read16() {
            return (null != reg8) ? reg8.read() : reg16.read16();
        }

        public int mask() {
            return 0xffff;
        }

        public void flush() {
            if (null != reg8) reg8.write((byte) value); else reg16.write(value);
        }
    }

    /**
     * A temporary register shared by all 16-bit registers in a 16-bit timer/counter. See
     * <code>LowRegister</code> for more information. While this is not needed in 8-bit timers, it doesn't pay
     * much to implement it here.
     */
    protected final RWRegister tempHighReg = new RWRegister();

    /**
     * The <code>LowRegister</code> and <code>HIghRegister</code> classes exists to implement the shared
     * temporary register for the high byte of the 16-bit registers corresponding to a 16-bit timer. Accesses
     * to the high byte of a register pair should go through this temporary byte. According to the manual,
     * writes to the high byte are stored in the temporary register. When the low byte is written to, both the
     * low and high byte are updated. On a read, the temporary high byte is updated when a read occurs on the
     * low byte. The LowRegister should be installed in place of the low register. Reads/writes on this
     * register will act accordingly on the low register, as well as initiate a read/write on the associated
     * high register.
     */
    protected class LowRegister implements ActiveRegister {

        final RW16Register reg;

        LowRegister(RW16Register r) {
            reg = r;
        }

        public void write(byte val) {
            reg.write((tempHighReg.read() << 8) + val);
        }

        public byte read() {
            tempHighReg.write((byte) (reg.read16() >> 8));
            return (byte) reg.read16();
        }
    }

    /**
     * For other 16-bit registers but OCRnxH, both reads and writes go through the temporary register.
     */
    protected class HighRegister implements ActiveRegister {

        public void write(byte val) {
            tempHighReg.write(val);
        }

        public byte read() {
            return tempHighReg.read();
        }
    }

    /**
     * Writes to OCRnxH go through the temporary register
     * but writes are direct.
     */
    protected class OCRnxHighRegister extends HighRegister {

        final RW16Register reg;

        OCRnxHighRegister(RW16Register r) {
            reg = r;
        }

        public byte read() {
            return (byte) (reg.read16() >> 8);
        }
    }
}
