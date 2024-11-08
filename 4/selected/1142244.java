package com.atolsystems.HwModeling;

import java.util.ArrayList;

/**
 *
 * @author sebastien.riou
 */
public class HwBitField extends SwBitField {

    final ArrayList<VerilogPort> readPorts;

    final ArrayList<VerilogPort> writePorts;

    final VerilogPort clkPort;

    final VerilogPort resetPort;

    public HwBitField(String name, int width, int type, ArrayList<VerilogPort> readPorts, ArrayList<VerilogPort> writePorts, VerilogPort clkPort, VerilogPort resetPort) {
        super(name, width, type);
        this.readPorts = new ArrayList<VerilogPort>();
        this.readPorts.addAll(readPorts);
        this.writePorts = new ArrayList<VerilogPort>();
        this.writePorts.addAll(writePorts);
        this.clkPort = clkPort;
        this.resetPort = resetPort;
        if (0 == (type & READ_VAL_UNDEFINED)) {
            int portWidth = 0;
            for (VerilogPort p : readPorts) portWidth += p.getWidth();
            if (portWidth != width) throw new RuntimeException("Read port width mismatch: " + portWidth + ", expected: " + width);
        }
        if (0 == (type & WRITABLE)) {
            int portWidth = 0;
            for (VerilogPort p : readPorts) portWidth += p.getWidth();
            if (portWidth != width) throw new RuntimeException("Write port width mismatch: " + portWidth + ", expected: " + width);
            if (clkPort.getWidth() != 1) throw new RuntimeException("Clock port must be a single wire, actual size: " + clkPort.getWidth());
        }
    }

    public HwBitField(String name, int width, int type, VerilogPort readPort, VerilogPort writePort, VerilogPort clkPort, VerilogPort resetPort) {
        super(name, width, type);
        this.readPorts = new ArrayList<VerilogPort>();
        this.writePorts = new ArrayList<VerilogPort>();
        if (null != readPort) readPorts.add(readPort);
        if (null != writePort) writePorts.add(writePort);
        this.clkPort = clkPort;
        this.resetPort = resetPort;
        if (0 == (type & READ_VAL_UNDEFINED)) {
            if (readPort.getWidth() != width) throw new RuntimeException("Read port width mismatch: " + readPort.getWidth() + ", expected: " + width);
        }
        if (0 != (type & WRITABLE)) {
            if (writePort.getWidth() != width) throw new RuntimeException("Write port width mismatch: " + writePort.getWidth() + ", expected: " + width);
            if (clkPort.getWidth() != 1) throw new RuntimeException("Clock port must be a single wire, actual size: " + clkPort.getWidth());
        }
    }

    public HwBitField(HwBitField bf, VerilogPort readPort, VerilogPort writePort) {
        super(bf.name, bf.width + 1, bf.type);
        this.readPorts = new ArrayList<VerilogPort>();
        this.writePorts = new ArrayList<VerilogPort>();
        readPorts.addAll(bf.readPorts);
        if (null != readPort) readPorts.add(readPort);
        writePorts.addAll(bf.writePorts);
        if (null != writePort) writePorts.add(writePort);
        this.clkPort = bf.clkPort;
        this.resetPort = bf.resetPort;
    }

    /**
     * Return a sub bitfield
     * @param bf
     * @param offset
     * @param length
     */
    public HwBitField(HwBitField bf, int offset, int length, String name) {
        super(name, length, bf.type);
        this.readPorts = new ArrayList<VerilogPort>();
        this.writePorts = new ArrayList<VerilogPort>();
        if (bf.hasReadPorts()) {
            for (int i = 0; i < length; i++) readPorts.add(bf.readPorts.get(i + offset));
        }
        if (bf.hasWritePorts()) {
            for (int i = 0; i < length; i++) writePorts.add(bf.writePorts.get(i + offset));
        }
        this.clkPort = bf.clkPort;
        this.resetPort = bf.resetPort;
    }

    public HwBitField subBitField(int offset, int length, String name) {
        HwBitField out = new HwBitField(this, offset, length, name);
        return out;
    }

    public HwBitField subBitField(int offset, String name) {
        HwBitField out = new HwBitField(this, offset, width - offset, name);
        return out;
    }

    public VerilogPort getClkPort() {
        return clkPort;
    }

    public ArrayList<VerilogPort> getReadPorts() {
        return readPorts;
    }

    public ArrayList<VerilogPort> getWritePorts() {
        return writePorts;
    }

    public VerilogPort getResetPort() {
        return resetPort;
    }

    public boolean hasReadPorts() {
        return (readPorts.isEmpty()) ? false : true;
    }

    public boolean hasWritePorts() {
        return (writePorts.isEmpty()) ? false : true;
    }

    public boolean hasClockPort() {
        return (null == clkPort) ? false : true;
    }

    public boolean hasResetPort() {
        return (null == resetPort) ? false : true;
    }
}
