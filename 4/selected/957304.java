package avrora.sim.mcu;

import avrora.sim.RWRegister;
import avrora.sim.Simulator;
import avrora.sim.state.BooleanView;
import avrora.sim.state.RegisterUtil;
import cck.util.Arithmetic;
import cck.text.StringUtil;

/**
 * This is an implementation of the non-volatile EEPROM on the ATMega128 microcontroller.
 *
 * @author Daniel Lee
 * @author Sascha Silbe
 */
public class EEPROM extends AtmelInternalDevice {

    final int EEPROM_SIZE, EEPROM_SIZE_numBits;

    public static final int EEARH = 0x1F;

    public static final int EEARL = 0x1E;

    public static final int EEDR = 0x1D;

    public static final int EECR = 0x1C;

    final byte[] EEPROM_data;

    final RWRegister EEDR_reg;

    final EECRReg EECR_reg;

    final RWRegister EEARL_reg;

    final EEARHReg EEARH_reg;

    static final int EERIE = 3;

    static final int EEMWE = 2;

    static final int EEWE = 1;

    static final int EERE = 0;

    static final int EEPROM_INTERRUPT = 23;

    boolean masterWriteEnable;

    boolean writeEnable;

    boolean readEnable;

    final EEPROMTicker ticker;

    final EEPROMWriteFinishedEvent writeFinishedEvent;

    int writeCount = -1;

    boolean writeEnableWritten;

    boolean readEnableWritten;

    EEPROM(int size, AtmelMicrocontroller m) {
        super("eeprom", m);
        ticker = new EEPROMTicker();
        writeFinishedEvent = new EEPROMWriteFinishedEvent();
        EEDR_reg = new RWRegister();
        EECR_reg = new EECRReg();
        EEARL_reg = new EEARLReg();
        EEARH_reg = new EEARHReg();
        EEPROM_SIZE = size;
        EEPROM_SIZE_numBits = Arithmetic.log(size);
        EEPROM_data = new byte[EEPROM_SIZE];
        installIOReg("EEDR", EEDR_reg);
        installIOReg("EECR", EECR_reg);
        installIOReg("EEARL", EEARL_reg);
        installIOReg("EEARH", EEARH_reg);
    }

    public int getSize() {
        return EEPROM_SIZE;
    }

    public void setContent(byte[] contents) {
        for (int addr = 0; addr < contents.length; addr++) {
            EEPROM_data[addr] = contents[addr];
        }
        if (devicePrinter != null) devicePrinter.println("EEPROM: content set");
    }

    public byte[] getContent() {
        return EEPROM_data;
    }

    protected class EEARHReg extends RWRegister {

        public void write(byte val) {
            if (writeEnable) return;
            value = (byte) (val & ((EEPROM_SIZE >> 8) - 1));
        }
    }

    protected class EEARLReg extends RWRegister {

        public void write(byte val) {
            if (writeEnable) return;
            value = (byte) (val & Math.min(EEPROM_SIZE - 1, 255));
        }
    }

    protected class EECRReg extends RWRegister {

        final BooleanView _eerie = RegisterUtil.booleanView(this, EERIE);

        final BooleanView _eere = RegisterUtil.booleanView(this, EERE);

        final BooleanView _eemwe = RegisterUtil.booleanView(this, EEMWE);

        final BooleanView _eewe = RegisterUtil.booleanView(this, EEWE);

        public void decode() {
            if (newTrue(readEnable, readEnable = _eere.getValue())) {
                if (devicePrinter != null) devicePrinter.println("EEPROM: EERE flagged");
                readEnableWritten = true;
            }
            if (newTrue(writeEnable, writeEnable = _eewe.getValue())) {
                if (devicePrinter != null) devicePrinter.println("EEPROM: EEWE flagged");
                writeEnableWritten = true;
            }
            if (newTrue(masterWriteEnable, masterWriteEnable = _eemwe.getValue())) {
                if (devicePrinter != null) devicePrinter.println("EEPROM: reset write count to 4");
                writeCount = 4;
            }
            interpreter.setEnabled(EEPROM_INTERRUPT, _eerie.getValue());
            interpreter.setPosted(EEPROM_INTERRUPT, !writeEnable);
            mainClock.insertEvent(ticker, 1);
        }

        public void write(byte val) {
            value = (byte) (0xf & val);
            if (devicePrinter != null) {
                devicePrinter.println("EEPROM: EECR written to, val = " + StringUtil.toBin(value, 4));
            }
            decode();
        }

        private boolean newTrue(boolean b1, boolean b2) {
            return !b1 && b2;
        }

        public void resetEERE() {
            _eere.setValue(false);
            decode();
        }

        public void resetEEMWE() {
            _eemwe.setValue(false);
            decode();
        }

        public void resetEEWE() {
            _eewe.setValue(false);
            decode();
        }
    }

    protected class EEPROMTicker implements Simulator.Event {

        public void fire() {
            if (devicePrinter != null) {
                devicePrinter.println("Tick : " + writeCount);
            }
            int address = read16(EEARH_reg, EEARL_reg);
            if (writeCount > 0) {
                if (writeEnableWritten) {
                    if (devicePrinter != null) devicePrinter.println("EEPROM: " + EEDR_reg.read() + " written to " + address);
                    EEPROM_data[address] = EEDR_reg.read();
                    mainClock.insertEvent(writeFinishedEvent, (long) (mainClock.getHZ() * 0.0085));
                    simulator.delay(2);
                }
            }
            if (readEnableWritten && !writeEnable) {
                if (devicePrinter != null) devicePrinter.println("EEPROM: " + EEPROM_data[address] + " read from " + address);
                EEDR_reg.write(EEPROM_data[address]);
                EECR_reg.resetEERE();
                simulator.delay(4);
            }
            if (writeCount > 0) {
                writeCount--;
                mainClock.insertEvent(ticker, 1);
            }
            if (writeCount == 0) {
                if (devicePrinter != null) devicePrinter.println("EEPROM: write count hit 0, clearing EEMWE");
                writeCount--;
                EECR_reg.resetEEMWE();
            }
            writeEnableWritten = false;
            readEnableWritten = false;
        }
    }

    protected class EEPROMWriteFinishedEvent implements Simulator.Event {

        public void fire() {
            if (devicePrinter != null) devicePrinter.println("EEPROM: write finished, clearing EEWE");
            EECR_reg.resetEEWE();
        }
    }
}
