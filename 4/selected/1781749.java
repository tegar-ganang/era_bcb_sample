package sw_emulator.hardware.bus;

import sw_emulator.hardware.bus.Bus;
import sw_emulator.hardware.memory.ColorRAM;
import sw_emulator.math.Unsigned;

/**
 * Provide methods for writing and reading from C64 bus for different
 * addressing modes from the bus.
 * So this class emulates the address and the data bus of the Cpu and Vic II.
 * The appropriate table for Cpu or Vic are selected by the PLA82S100 chip using
 * the <code>setTable</code> method.
 * For convenience the 16 bits of the address are stored in 32 bits (int type),
 * and so for the 8 bits of the byte value.
 * Note that the VIC reads 12 bits from it's bus: 8 from system bus, 4 from
 * color ram chip.
 * This class also manage AEC signals (AEC tri-states the address line), and
 * it depens by the chip:
 * <ul>
 *  <li>CPU: AEC low means tri-states</li>
 *  <li>VIC: AEC high means tri-states</li>
 * </ul>
 * AEC is low if it is 0, high otherwise.
 *
 * @author Ice
 * @version 1.00 15/10/1999
 */
public class C64Bus extends Bus {

    public static final int V_CPU = 0;

    public static final int V_VIC = 1;

    /**
   * Table for read functions pointers view by the Cpu
   */
    protected readableBus[] readTableCpu;

    /**
   * Table for write functions pointers view by the Cpu
   */
    protected writeableBus[] writeTableCpu;

    /**
   * Table for read functions pointers view by the Vic
   */
    protected readableBus[] readTableVic;

    /**
   * The color ram used by the Vic read operation
   */
    protected ColorRAM color;

    /**
   * Construct a bus for the cpu and vic
   * Cpu use read and write to bus, Vic use only read.
   *
   * @param readTableCpu the cpu table for reading from bus
   * @param writeTableCpu the cpu table for writing to bus
   * @param readTableVic the vic table for reading from bus
   * @param color the color ram used by vic reads
   */
    public C64Bus(readableBus[] readTableCpu, writeableBus[] writeTableCpu, readableBus[] readTableVic, ColorRAM color) {
        setTableCpu(readTableCpu, writeTableCpu);
        setTableVic(readTableVic);
        setColor(color);
    }

    /**
   * Construct a bus. Note that this bus can not be used until the
   * <code>setTablexx</code> and <code>setColor</code> are called.
   */
    public C64Bus() {
    }

    /**
   * Store a byte value in address position to a chip connected to the bus using
   * approprite device view.
   *
   * @param addr the address location
   * @param value the byte value
   * @param view the bus view of the device that write to the bus
   * @param aec the AEC signal state of the chip
   */
    public void store(int addr, int value, int view, int aec) {
        switch(view) {
            case V_CPU:
                if (aec != 0) {
                    writeTableCpu[addr >> 8].write(addr, (byte) value);
                    previous = value;
                } else {
                }
                break;
            case V_VIC:
                break;
        }
    }

    /**
   * Load a byte value from a chip that is connected to the bus at address
   * position using a view of one device.
   *
   * @param addr the address location
   * @param view the bus view of the device that read the bus
   * @param aec the AEC signal state of the chip
   * @return the readed byte stored in 32 bits
   */
    public int load(int addr, int view, int aec) {
        switch(view) {
            case V_CPU:
                if (aec != 0) {
                    return previous = Unsigned.done(readTableCpu[addr >> 8].read(addr));
                } else {
                    return 0xFF;
                }
            case V_VIC:
                if (aec == 0) {
                    previous = Unsigned.done(readTableVic[addr >> 8].read(addr));
                    return previous + ((color.read(addr) & 0x0F) << 8);
                } else {
                    return 0xFF + ((previous & 0x0F) << 8);
                }
        }
        return 0;
    }

    /**
   * Set the tables for reading and writing from/to bus
   *
   * @param readTableCpu the table for reading from bus by cpu view
   * @param writeTableCpu the table for writing to bus by cpu view
   * @param readTableVic the table for reading from bus by vic view
   */
    public void setTableCpu(readableBus[] readTableCpu, writeableBus[] writeTableCpu) {
        this.readTableCpu = readTableCpu;
        this.writeTableCpu = writeTableCpu;
    }

    /**
   * Set the table for reading from bus
   *
   * @param readTableVic the table for reading from bus by vic view
   */
    public void setTableVic(readableBus[] readTableVic) {
        this.readTableVic = readTableVic;
    }

    /**
   * Set the actual color ram
   *
   * @param color the color ram memory
   */
    public void setColor(ColorRAM color) {
        this.color = color;
    }

    /**
   * Gives true if the bus is correctly initialized
   *
   * @return true if bus is initialized correctly
   */
    public boolean isInitialized() {
        if ((readTableVic != null) && (color != null) && (readTableCpu != null)) {
            return true;
        } else return false;
    }
}
