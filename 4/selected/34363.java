package jemu.system.vz;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import jemu.core.cpu.*;
import jemu.core.device.*;
import jemu.core.device.memory.*;
import jemu.core.device.sound.*;
import jemu.ui.*;
import jemu.util.diss.*;

/**
 * Title:        JEMU
 * Description:  The Java Emulation Platform
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author
 * @version 1.0
 */
public class VZ extends Computer {

    protected static final int CYCLES_PER_SEC_VZ200 = 3579500;

    protected static final int CYCLES_PER_SEC_VZ300 = 3546900;

    protected static final int CYCLES_PER_SCAN = 228;

    protected static final int AUDIO_TEST = 0x40000000;

    protected static final byte[] SOUND_LEVELS = { 127, -128, 0, 127 };

    protected boolean vz200;

    protected int cyclesPerSecond = CYCLES_PER_SEC_VZ200;

    protected Z80 z80 = new Z80(cyclesPerSecond);

    protected VZMemory memory = new VZMemory();

    protected int cycles = 0;

    protected int frameFlyback = 0x80;

    protected int vdcLatch = 0x00;

    protected SimpleRenderer renderer = (SimpleRenderer) addDevice(new FullRenderer(memory));

    protected Keyboard keyboard = new Keyboard();

    protected Disassembler disassembler = new DissZ80();

    protected SoundPlayer player = SoundUtil.getSoundPlayer(false);

    protected byte soundByte = 127;

    protected int soundUpdate = 0;

    protected int audioAdd;

    protected int scansOfFlyback;

    protected int scansPerFrame;

    protected int cyclesPerFrame;

    protected int cyclesToFlyback;

    public VZ(Applet applet, String name) {
        super(applet, name);
        vz200 = "VZ200".equalsIgnoreCase(name);
        if (vz200) {
            cyclesPerSecond = CYCLES_PER_SEC_VZ200;
            scansPerFrame = 312;
            scansOfFlyback = 57;
            renderer.setVerticalAdjustment(0);
        } else {
            cyclesPerSecond = CYCLES_PER_SEC_VZ300;
            scansPerFrame = 310;
            scansOfFlyback = 56;
            renderer.setVerticalAdjustment(1);
        }
        z80.setCyclesPerSecond(cyclesPerSecond);
        cyclesPerFrame = CYCLES_PER_SCAN * scansPerFrame;
        cyclesToFlyback = cyclesPerFrame - (CYCLES_PER_SCAN * scansOfFlyback);
        audioAdd = player.getClockAdder(AUDIO_TEST, cyclesPerSecond);
        z80.setMemoryDevice(this);
        z80.setCycleDevice(this);
        setTimer(player);
        player.setFormat(SoundUtil.ULAW);
        setBasePath("vz");
    }

    public void initialise() {
        memory.setMemory(0, getFile(romPath + "VZBAS" + (vz200 ? "12" : "20") + ".ROM", 16384));
        renderer.setFontData(getFile(romPath + "VZ.CHR", 768));
        super.initialise();
    }

    public void dispose() {
        super.dispose();
        player.dispose();
    }

    public Memory getMemory() {
        return memory;
    }

    public Processor getProcessor() {
        return z80;
    }

    public void cycle() {
        if (++cycles == cyclesToFlyback) {
            frameFlyback = 0x00;
            z80.setInterrupt(1);
        } else if (cycles == cyclesPerFrame) {
            cycles = 0;
            frameFlyback = 0x80;
            z80.clearInterrupt(1);
            if (frameSkip == 0) renderer.renderScreen(memory);
            syncProcessor();
        }
        soundUpdate += audioAdd;
        if ((soundUpdate & AUDIO_TEST) != 0) {
            soundUpdate -= AUDIO_TEST;
            player.writeMono(soundByte);
        }
        if (frameSkip == 0) renderer.cycle();
    }

    public int readByte(int address) {
        return (address >= 0x7000 || address < 0x6800) ? memory.readByte(address) : frameFlyback | (keyboard.readByte(address) & 0x7f);
    }

    public int writeByte(int address, int value) {
        if (address >= 0x7800) return memory.writeByte(address, value); else if (address >= 0x7000) return renderer.setData(memory.writeByte(address, value)); else if (address >= 0x6800) {
            if (((vdcLatch ^ value) & 0x21) != 0) soundByte = SOUND_LEVELS[(value & 0x01) | ((value >> 4) & 0x02)];
            vdcLatch = value;
            renderer.setVDCLatch(value);
        }
        return value & 0xff;
    }

    public void keyPressed(KeyEvent e) {
        keyboard.keyPressed(e.getKeyCode());
    }

    public void keyReleased(KeyEvent e) {
        keyboard.keyReleased(e.getKeyCode());
    }

    protected void vzFileException(String error) throws Exception {
        if (error == null) error = "Bad VZ File format";
        throw new Exception(error);
    }

    public void loadFile(int type, String name) throws Exception {
        InputStream in = openFile(name);
        try {
            byte[] header = new byte[24];
            int len = in.read(header);
            if (len != 24) vzFileException(null);
            for (int i = 0; i < 18; i++) memory.writeByte(0x7ab2 + i, header[i + 4] & 0xff);
            type = header[21] & 0xff;
            memory.writeByte(0x7ad2, type);
            int start = (header[22] & 0xff) + 256 * (header[23] & 0xff);
            memory.writeByte(0x781e, header[22] & 0xff);
            memory.writeByte(0x781f, header[23] & 0xff);
            memory.writeByte(0x7839, 0x63);
            memory.writeByte(0x787d, 0xc9);
            int address = start;
            int read;
            do {
                read = in.read();
                if (read != -1) {
                    memory.writeByte(address, read);
                    address = (address + 1) & 0xffff;
                }
            } while (read != -1);
            in.close();
            memory.writeByte(0x78f9, address & 0xff);
            memory.writeByte(0x78fa, (address >> 8) & 0xff);
            memory.writeByte(0xffc9, 0x1e);
            memory.writeByte(0xffca, 0x1d);
            z80.setSP(0xffc9);
            z80.setPC(0x36be);
        } finally {
            in.close();
        }
    }

    public void setDisplay(Display value) {
        super.setDisplay(value);
        renderer.setDisplay(value);
    }

    public Dimension getDisplaySize(boolean large) {
        return renderer.getDisplaySize();
    }

    public Disassembler getDisassembler() {
        return disassembler;
    }

    public void emulate(int mode) {
        player.play();
        super.emulate(mode);
        player.stop();
    }

    public void displayLostFocus() {
        keyboard.reset();
    }
}
