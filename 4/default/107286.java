/**
 * PgbMemory controls the entire memory map of the Gameboy.
 * 
 * It also contains some executing code, mostly in cycle(),
 * which is responsible for controlling the hardware and
 * setting the interrupt flag each CPU cycle.
 * 
 * Also, DMA functions are handled here and SGB calls are
 * interpreted here and then (usually) passed on to the
 * video hardware.
 */
public final class PgbMemory {

    public PgbCart cart;

    public PgbVideo video;

    public PgbJoypad joy;

    public PgbNetplay net;

    public SoundChip soundChip;

    boolean soundOn;

    byte soundIO[];

    public static final byte INT_JOYPAD = 0x10;

    public static final byte INT_SERIAL = 0x08;

    public static final byte INT_TIMER = 0x04;

    public static final byte INT_LCD = 0x02;

    public static final byte INT_VBLANK = 0x01;

    protected byte[] hiRAM;

    protected byte[] loRAM;

    public byte IF;

    public byte IE;

    public int tima;

    public int tma;

    public byte tac;

    protected boolean timerOn;

    protected int timeCounter;

    protected int timeLimit;

    public int div;

    public int cycles;

    public int cyclesLeft;

    public int cyclesSkipped;

    protected int sgbBitCounter;

    protected int sgbPacketCounter;

    protected byte[] sgbBuffer;

    protected boolean sgbListening;

    protected int sgbCommand;

    protected int sgbPackets;

    byte gbcRAM;

    int loRAMOffset;

    byte gbcSpeed;

    byte rHDMA1;

    byte rHDMA2;

    byte rHDMA3;

    byte rHDMA4;

    int hdmaSrc;

    int hdmaDst;

    boolean hdmaDone;

    byte hdmaLastMode;

    int hdmaStop;

    public PgbMemory(PgbCart cart, PgbVideo video, PgbJoypad joy, PgbNetplay net) {
        this.cart = cart;
        this.video = video;
        this.joy = joy;
        this.net = net;
        hiRAM = new byte[0x80];
        loRAM = new byte[0x8000];
        sgbBuffer = new byte[0x80];
        soundOn = true;
        soundChip = new SoundChip();
        soundIO = new byte[64];
        reset();
    }

    public void soundPlay() {
        soundChip.outputSound();
    }

    public int unsign(byte b) {
        return (b & 0xff);
    }

    public void reset() {
        tima = 0;
        tma = 0;
        tac = 0;
        timerOn = false;
        timeCounter = 0;
        timeLimit = 0;
        div = 0xAF * 0x100;
        IF = 1;
        IE = 0;
        sgbBitCounter = 0;
        sgbPacketCounter = 0;
        sgbListening = false;
        sgbPackets = 1;
        gbcRAM = (byte) 0x00;
        loRAMOffset = 0xC000;
        gbcSpeed = (byte) 0x7E;
        cycles = 0;
        cyclesLeft = Integer.MAX_VALUE;
        recalcCyclesLeft();
        hdmaDone = true;
        hdmaLastMode = PgbVideo.STAT_HBLANK;
        soundIO[16] = -128;
        soundIO[17] = -65;
        soundIO[18] = -13;
        soundIO[20] = -65;
        soundIO[22] = 63;
        soundIO[23] = 0;
        soundIO[25] = -65;
        soundIO[26] = 127;
        soundIO[27] = -1;
        soundIO[28] = -97;
        soundIO[30] = -65;
        soundIO[32] = -1;
        soundIO[33] = 0;
        soundIO[34] = 0;
        soundIO[35] = -65;
        soundIO[36] = 119;
        soundIO[37] = -13;
        soundIO[38] = -15;
    }

    /**
	 * Recaclulates the number of cycles left.  Called
	 * when the timer, LCDC, LCY, or serial control is
	 * changed.
	 */
    public void recalcCyclesLeft() {
        cyclesLeft = Integer.MAX_VALUE;
        if (timerOn && (timeLimit - timeCounter) < cyclesLeft) {
            cyclesLeft = (timeLimit - timeCounter);
        }
        if (video.lcd_on && video.cyclesLeft() < cyclesLeft) {
            cyclesLeft = video.cyclesLeft();
        }
        if (net.cyclesLeft() < cyclesLeft) {
            cyclesLeft = net.cyclesLeft();
        }
    }

    /**
	 * Advances the state of the hardware as though the number
	 * of cpu cycles specified had passed and sets the interrupt
	 * register appropriately.
	 * 
	 * @param cv the number of cpu cycles to advance.
	 */
    public final void cycle(int cv) {
        cycles += cv;
        if (cycles > cyclesLeft) {
            div -= cyclesLeft;
            if (div < 0) {
                div = 0xFF * 0x100;
            }
            if (timerOn) {
                timeCounter += cyclesLeft;
                if (timeCounter >= timeLimit) {
                    timeCounter = 0;
                    tima++;
                }
                if (tima > 255) {
                    tima = tma;
                    IF |= INT_TIMER;
                }
            }
            {
                IF |= net.cycle(cyclesLeft);
            }
            if (video.lcd_on) {
                IF |= video.cycle(cyclesLeft, this);
            }
            if (!hdmaDone) {
                if (hdmaLastMode != PgbVideo.STAT_HBLANK && (byte) (video.getStat() & 0x03) == PgbVideo.STAT_HBLANK) {
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    write(hdmaDst++, read(hdmaSrc++));
                    if (hdmaDst >= hdmaStop) {
                        hdmaDone = true;
                    }
                }
                hdmaLastMode = (byte) (video.getStat() & 0x03);
            }
            cycles -= cyclesLeft;
            recalcCyclesLeft();
        }
    }

    /**
	 * Set the timer control.
	 */
    public void setTac(byte tac) {
        timerOn = (tac & 0x04) == 0x04;
        timeCounter = 0;
        switch(tac & 0x03) {
            case 0:
                timeLimit = 1024;
                break;
            case 1:
                timeLimit = 16;
                break;
            case 2:
                timeLimit = 64;
                break;
            case 3:
                timeLimit = 256;
                break;
        }
    }

    /**
	 * Reads (signed) bytes from GB memory.
	 * 
	 * @param address the gameboy memory address to read from.
	 */
    public final byte read(int address) {
        switch(address) {
            case 0xFF00:
                return joy.read();
            case 0xFF01:
                return net.getSerialData();
            case 0xFF02:
                return net.getSerialControl();
            case 0xFF03:
                return 0;
            case 0xFF04:
                return (byte) (div / 0x100);
            case 0xFF05:
                return (byte) tima;
            case 0xFF06:
                return (byte) tma;
            case 0xFF07:
                return (byte) tac;
            case 0xFF0F:
                return IF;
            case 0xFF40:
                return video.getLcdc();
            case 0xFF41:
                return video.getStat();
            case 0xFF42:
                return (byte) video.scy;
            case 0xFF43:
                return (byte) video.scx;
            case 0xFF44:
                return (byte) video.ly;
            case 0xFF45:
                return (byte) video.lyc;
            case 0xFF47:
                return (byte) video.bgpal;
            case 0xFF48:
                return (byte) video.objpal0;
            case 0xFF49:
                return (byte) video.objpal1;
            case 0xFF4A:
                return (byte) video.wy;
            case 0xFF4B:
                return (byte) video.wx;
            case 0xFF4D:
                return gbcGetSpeed();
            case 0xFF4F:
                return video.gbcGetVram();
            case 0xFF55:
                return getHDMAControl();
            case 0xFF56:
                return net.getIR();
            case 0xFF68:
                return video.gbcGetBgpi();
            case 0xFF69:
                return video.gbcGetBgpd();
            case 0xFF6A:
                return video.gbcGetObpi();
            case 0xFF6B:
                return video.gbcGetObpd();
            case 0xFF70:
                return gbcGetRamBank();
            case 0xFF7F:
                return 0;
            case 0xFFFF:
                return IE;
        }
        if (address < 0x8000) {
            return cart.read(address);
        }
        if (address < 0xA000) {
            return video.read(address);
        }
        if (address < 0xC000) {
            return cart.read(address);
        }
        if (address < 0xD000) {
            return loRAM[address - 0xC000];
        }
        if (address < 0xE000) {
            return loRAM[address - loRAMOffset];
        }
        if (address < 0xFE00) {
            return loRAM[address - 0xE000];
        }
        if (address < 0xFEA0) {
            return video.read(address);
        }
        if (address < 0xFF0F) {
            return 0;
        }
        if (address < 0xFF40) {
            if ((address & 0xff) == 38) {
                if (soundChip.channel1.getLength() <= 0) soundIO[38] &= ~0x01; else soundIO[38] |= 0x01;
                if (soundChip.channel2.getLength() <= 0) soundIO[38] &= ~0x02; else soundIO[38] |= 0x02;
                if (soundChip.channel3.getLength() <= 0) soundIO[38] &= ~0x04; else soundIO[38] |= 0x04;
                if (soundChip.channel4.getLength() <= 0) soundIO[38] &= ~0x08; else soundIO[38] |= 0x08;
            }
            return soundIO[address & 0xff];
        }
        if (address < 0xFF55) {
            System.out.println("read GBC register:" + Integer.toHexString(address));
            return 0;
        }
        if (address < 0xFF70) {
            System.out.println("read GBC register:" + Integer.toHexString(address));
            return 0;
        }
        if (address < 0xFF7F) {
            System.out.println("read GBC register:" + Integer.toHexString(address));
            return 0;
        }
        if (address < 0xFFFF) {
            return hiRAM[address - 0xFF80];
        }
        System.out.println("Read from unmapped memory:" + Integer.toHexString(address));
        return 0;
    }

    /**
	 * Write bytes into the gameboy memory.
	 * 
	 * @param address the gameboy memory address to write to.
	 * @param towrite the byte to write into memory.
	 */
    public final void write(int address, byte towrite) {
        switch(address) {
            case 0xFF00:
                if (towrite == 0x30 && PgbSettings.system == PgbSettings.SYS_SGB) {
                    sgbCommandBit(joy.selected);
                }
                joy.write(towrite);
                return;
            case 0xFF01:
                net.setSerialData(towrite);
                return;
            case 0xFF02:
                net.setSerialControl(towrite);
                recalcCyclesLeft();
                return;
            case 0xFF04:
                div = 0;
                return;
            case 0xFF05:
                tima = towrite;
                return;
            case 0xFF06:
                tma = towrite;
                return;
            case 0xFF07:
                setTac(towrite);
                recalcCyclesLeft();
                return;
            case 0xFF10:
                if (soundOn) soundChip.channel1.setSweep((unsign(towrite) & 0x70) >> 4, unsign(towrite) & 7, (unsign(towrite) & 8) == 1);
                soundIO[16] = towrite;
                return;
            case 0xFF11:
                if (soundOn) {
                    soundChip.channel1.setDutyCycle((unsign(towrite) & 0xc0) >> 6);
                    if ((soundIO[20] & 0x40) == 1) soundChip.channel1.setLength(unsign(towrite) & 0x3f);
                }
                soundIO[17] = towrite;
                return;
            case 0xFF12:
                if (soundOn) soundChip.channel1.setEnvelope((unsign(towrite) & 0xf0) >> 4, unsign(towrite) & 7, (unsign(towrite) & 8) == 8);
                soundIO[18] = towrite;
                return;
            case 0xFF13:
                soundIO[19] = towrite;
                if (soundOn) soundChip.channel1.setFrequency(((unsign(soundIO[20]) & 7) << 8) + unsign(soundIO[19]));
                return;
            case 0xFF14:
                soundIO[20] = towrite;
                if (soundOn) {
                    if ((soundIO[20] & 0x80) != 0) {
                        soundChip.channel1.setLength(unsign(soundIO[17]) & 0x3f);
                        soundChip.channel1.setEnvelope((unsign(soundIO[18]) & 0xf0) >> 4, unsign(soundIO[18]) & 7, (unsign(soundIO[18]) & 8) == 8);
                        soundIO[38] |= 0x01;
                    }
                    if ((soundIO[20] & 0x40) == 0) {
                        soundChip.channel1.setLength(-1);
                        soundIO[38] &= ~0x01;
                    }
                    soundChip.channel1.setFrequency(((unsign(soundIO[20]) & 7) << 8) + unsign(soundIO[19]));
                }
                return;
            case 0xFF15:
                soundIO[21] = towrite;
                return;
            case 0xFF16:
                if (soundOn) {
                    soundChip.channel2.setDutyCycle((unsign(towrite) & 0xc0) >> 6);
                    soundChip.channel2.setLength(unsign(towrite) & 0x3f);
                }
                soundIO[22] = towrite;
                return;
            case 0xFF17:
                if (soundOn) soundChip.channel2.setEnvelope((unsign(towrite) & 0xf0) >> 4, unsign(towrite) & 7, (unsign(towrite) & 8) == 8);
                soundIO[23] = towrite;
                return;
            case 0xFF18:
                soundIO[24] = towrite;
                if (soundOn) soundChip.channel2.setFrequency(((unsign(soundIO[25]) & 7) << 8) + unsign(soundIO[24]));
                return;
            case 0xFF19:
                soundIO[25] = towrite;
                if (soundOn) {
                    if ((soundIO[25] & 0x80) != 0) {
                        soundChip.channel2.setLength(unsign(soundIO[22]) & 0x3f);
                        soundChip.channel2.setEnvelope((unsign(soundIO[23]) & 0xf0) >> 4, unsign(soundIO[23]) & 7, (unsign(soundIO[23]) & 8) == 8);
                        soundIO[38] |= 0x02;
                    }
                    if ((soundIO[25] & 0x40) == 0) {
                        soundChip.channel2.setLength(-1);
                        soundIO[39] &= ~0x02;
                    }
                    soundChip.channel2.setFrequency(((unsign(soundIO[25]) & 7) << 8) + unsign(soundIO[24]));
                }
                return;
            case 0xFF1A:
                if (soundOn) if ((unsign(towrite) & 0x80) != 0) soundChip.channel3.setVolume((unsign(soundIO[28]) & 0x60) >> 5); else soundChip.channel3.setVolume(0);
                soundIO[26] = towrite;
                return;
            case 0xFF1B:
                soundIO[27] = towrite;
                if (soundOn) if ((soundIO[30] & 0x40) == 1) soundChip.channel3.setLength(unsign(towrite));
                return;
            case 0xFF1C:
                soundIO[28] = towrite;
                if (soundOn) soundChip.channel3.setVolume((unsign(soundIO[28]) & 0x60) >> 5);
                return;
            case 0xFF1D:
                soundIO[29] = towrite;
                if (soundOn) soundChip.channel3.setFrequency(((unsign(soundIO[30]) & 7) << 8) + unsign(soundIO[29]));
                return;
            case 0xFF1E:
                soundIO[30] = towrite;
                if (soundOn) {
                    if ((soundIO[25] & 0x80) != 0) {
                        soundChip.channel3.setLength(unsign(soundIO[27]));
                        soundIO[38] |= 0x04;
                    }
                    soundChip.channel3.setFrequency(((unsign(soundIO[30]) & 7) << 8) + unsign(soundIO[29]));
                    if ((soundIO[30] & 0x40) == 0) {
                        soundChip.channel3.setLength(-1);
                        soundIO[38] &= ~0x04;
                    }
                }
                return;
            case 0xFF20:
                if (soundOn) soundChip.channel4.setLength(unsign(towrite) & 0x3f);
                soundIO[32] = towrite;
                return;
            case 0xFF21:
                if (soundOn) soundChip.channel4.setEnvelope((unsign(towrite) & 0xf0) >> 4, unsign(towrite) & 7, (unsign(towrite) & 8) == 8);
                soundIO[33] = towrite;
                return;
            case 0xFF22:
                soundIO[34] = towrite;
                return;
            case 0xFF23:
                soundIO[35] = towrite;
                if (soundOn) {
                    if ((soundIO[35] & 0x80) != 0) {
                        soundChip.channel4.setLength(unsign(soundIO[32]) & 0x3f);
                        soundIO[38] |= 0x08;
                    }
                    if ((soundIO[35] & 0x40) == 0) {
                        soundChip.channel4.setLength(-1);
                        soundIO[38] &= ~0x08;
                    }
                }
                return;
            case 0xFF24:
                soundIO[36] = towrite;
                return;
            case 0xFF25:
                soundIO[37] = towrite;
                if (soundOn) {
                    int j = 0;
                    if ((unsign(towrite) & 1) != 0) j |= 1;
                    if ((unsign(towrite) & 0x10) != 0) j |= 2;
                    soundChip.channel1.setChannel(j);
                    j = 0;
                    if ((unsign(towrite) & 2) != 0) j |= 1;
                    if ((unsign(towrite) & 0x20) != 0) j |= 2;
                    soundChip.channel2.setChannel(j);
                    j = 0;
                    if ((unsign(towrite) & 4) != 0) j |= 1;
                    if ((unsign(towrite) & 0x40) != 0) j |= 2;
                    soundChip.channel3.setChannel(j);
                    j = 0;
                    if ((unsign(towrite) & 8) != 0) j |= 1;
                    if ((unsign(towrite) & 0x80) != 0) j |= 2;
                    soundChip.channel4.setChannel(j);
                }
                return;
            case 0xFF26:
                if ((towrite & 0x80) == 0) {
                    soundChip.channel1.setVolume3(0);
                    soundChip.channel2.setVolume3(0);
                    soundChip.channel3.setVolume(0);
                    soundChip.channel4.setLength(-1);
                }
                soundIO[38] = towrite;
                return;
            case 0xFF0F:
                IF = towrite;
                return;
            case 0xFF40:
                video.setLcdc(towrite);
                recalcCyclesLeft();
                return;
            case 0xFF41:
                video.setStat(towrite);
                recalcCyclesLeft();
                return;
            case 0xFF42:
                video.scy = towrite & 0xFF;
                return;
            case 0xFF43:
                video.scx = towrite & 0xFF;
                return;
            case 0xFF44:
                video.ly = 0;
                recalcCyclesLeft();
                return;
            case 0xFF45:
                video.lyc = towrite & 0xFF;
                return;
            case 0xFF46:
                oamDMA(towrite);
                return;
            case 0xFF47:
                video.setBgPal(towrite & 0xFF);
                return;
            case 0xFF48:
                video.setObjPal0(towrite & 0xFF);
                return;
            case 0xFF49:
                video.setObjPal1(towrite & 0xFF);
                return;
            case 0xFF4A:
                video.wy = towrite & 0xFF;
                return;
            case 0xFF4B:
                video.wx = towrite & 0xFF;
                return;
            case 0xFF4D:
                gbcSetSpeed(towrite);
                return;
            case 0xFF4F:
                video.gbcSetVram(towrite);
                return;
            case 0xFF51:
                rHDMA1 = towrite;
                return;
            case 0xFF52:
                rHDMA2 = towrite;
                return;
            case 0xFF53:
                rHDMA3 = towrite;
                return;
            case 0xFF54:
                rHDMA4 = towrite;
                return;
            case 0xFF55:
                setHDMAControl(towrite);
                return;
            case 0xFF56:
                net.setIR(towrite);
                return;
            case 0xFF68:
                if (PgbSettings.system == PgbSettings.SYS_GBC) {
                    video.gbcSetBgpi(towrite);
                }
                return;
            case 0xFF69:
                if (PgbSettings.system == PgbSettings.SYS_GBC) {
                    video.gbcSetBgpd(towrite);
                }
                return;
            case 0xFF6A:
                if (PgbSettings.system == PgbSettings.SYS_GBC) {
                    video.gbcSetObpi(towrite);
                }
                return;
            case 0xFF6B:
                if (PgbSettings.system == PgbSettings.SYS_GBC) {
                    video.gbcSetObpd(towrite);
                }
                return;
            case 0xFF70:
                gbcSetRamBank(towrite);
                return;
            case 0xFF7F:
                System.out.println("write to GBC mystery register FF7F:" + Integer.toHexString(towrite & 0xFF) + " " + Integer.toBinaryString(towrite & 0xFF));
                return;
            case 0xFFFF:
                IE = towrite;
                recalcCyclesLeft();
                return;
            case 0xFF1F:
                System.out.println("write to GBC mystery register FF1F:" + Integer.toHexString(towrite & 0xFF) + " " + Integer.toBinaryString(towrite & 0xFF));
                return;
        }
        if (address < 0x8000) {
            cart.write(address, towrite);
            return;
        }
        if (address < 0xA000) {
            video.write(address, towrite);
            return;
        }
        if (address < 0xC000) {
            cart.write(address, towrite);
            return;
        }
        if (address < 0xD000) {
            loRAM[address - 0xC000] = towrite;
            return;
        }
        if (address < 0xE000) {
            loRAM[address - loRAMOffset] = towrite;
            return;
        }
        if (address < 0xFE00) {
            loRAM[address - 0xE000] = towrite;
            return;
        }
        if (address < 0xFEA0) {
            video.write(address, towrite);
            return;
        }
        if (address < 0xFF00 && address >= 0xFEA0) {
            if (PgbSettings.DEBUG && towrite != 0) {
                System.out.println("write to empty ???:" + Integer.toHexString(towrite & 0xFF));
            }
            return;
        }
        if (address >= 0xFF26 && address <= 0xFF2F) {
            soundIO[address & 0xff] = towrite;
            return;
        }
        if (address >= 0xFF30 && address <= 0xFF3F) {
            if (address == 0xFF3F && towrite == 0xFFFFFFC0) {
                System.out.println("Demotronic Check!");
                System.out.println("Wrote 0x" + Integer.toHexString(towrite) + " (" + towrite + ") @ 0x" + Integer.toHexString(address));
                return;
            }
            soundIO[address & 0xff] = towrite;
            if (soundOn) soundChip.channel3.setSamplePair(address & 0xf, unsign(towrite));
            return;
        }
        if (address < 0xFFFF && address >= 0xFF80) {
            hiRAM[address - 0xFF80] = towrite;
            return;
        }
        System.out.println("Write to unmapped memory:" + Integer.toHexString(address) + ", " + Integer.toHexString(towrite));
    }

    public final void write(int address, int towrite) {
        write(address, (byte) towrite);
    }

    /**
	 * write two bytes
	 */
    public void writeWord(int address, int word) {
        write(address, word & 0xFF);
        write(address + 1, word >> 8);
    }

    /**
	 * set the gameboy color ram bank
	 */
    public void gbcSetRamBank(byte control) {
        if (PgbSettings.system == PgbSettings.SYS_GBC) {
            gbcRAM = control;
            int bank = ((gbcRAM & 0x07) == 0) ? 0 : (gbcRAM & 0x07) - 1;
            loRAMOffset = 0xC000 - bank * 0x1000;
        }
    }

    public byte gbcGetRamBank() {
        return gbcRAM;
    }

    /**
	 * set the gameboy color speed
	 */
    public void gbcSetSpeed(byte control) {
        gbcSpeed = control;
    }

    public byte gbcGetSpeed() {
        if (PgbSettings.system == PgbSettings.SYS_GBC) {
            return gbcSpeed;
        } else {
            return 0;
        }
    }

    /**
	 * this does GB Classic DMA transfers
	 */
    public void oamDMA(byte address) {
        int i, start;
        start = (address & 0xFF) * 0x0100;
        for (i = 0; i < 0xA0; i++) {
            video.write(0xFE00 + i, read(start + i));
        }
    }

    /**
	 * this does GBC HDMA transfers
	 */
    public void setHDMAControl(byte control) {
        boolean mode = (control & 0x80) == 0x80;
        int i, length = ((control & 0x7F) + 1) * 0x10;
        hdmaSrc = ((rHDMA1 & 0xFF) << 8) | (rHDMA2 & 0xF0);
        hdmaDst = ((rHDMA3 & 0x1F) << 8) | (rHDMA4 & 0xF0) + 0x8000;
        if (mode) {
            hdmaDone = false;
            hdmaStop = hdmaDst + length;
        } else {
            for (i = 0; i < length; i++) {
                write(hdmaDst++, read(hdmaSrc++));
            }
        }
    }

    public byte getHDMAControl() {
        return hdmaDone ? (byte) 0x01 : (byte) 0x00;
    }

    /**
	 * this is called every time 0x30 is written to the
	 * joystick register.  it is responsible for decoding
	 * the sgb command bits
	 */
    public void sgbCommandBit(byte b) {
        if (sgbPacketCounter == 0 && sgbBitCounter == 8) {
            sgbCheckCommand();
        }
        if (sgbBitCounter == 128) {
            if (++sgbPacketCounter == sgbPackets) {
                sgbCommandExec();
                sgbPacketCounter = 0;
            }
            sgbBitCounter = 0;
            sgbListening = false;
            return;
        }
        if (b == 0x00) {
            sgbListening = true;
            return;
        }
        if (sgbListening && b == 0x10) {
            sgbBuffer[sgbPacketCounter * 16 + sgbBitCounter / 8] |= (byte) (1 << (sgbBitCounter & 7));
            sgbBitCounter++;
            return;
        }
        if (sgbListening && b == 0x20) {
            sgbBuffer[sgbPacketCounter * 16 + sgbBitCounter / 8] &= ~(byte) (1 << (sgbBitCounter & 7));
            sgbBitCounter++;
            return;
        }
    }

    /**
	 * check out the first byte of the buffer to
	 *  parse the command and packets
	 */
    public void sgbCheckCommand() {
        sgbCommand = (sgbBuffer[0] & 0xF8) >> 3;
        sgbPackets = sgbBuffer[0] & 0x07;
    }

    /**
	 * called to process the current SGB command buffer,
	 * executing it if all packets have been written
	 */
    public void sgbCommandExec() {
        int i;
        String desc;
        if (PgbSettings.DEBUG) {
            System.out.println("recieved SGB sgbCommand: " + Integer.toHexString(sgbCommand) + ", sgbPackets:" + sgbPackets);
            System.out.print("bits: ");
            for (i = 0; i < 16 * sgbPackets; i++) {
                System.out.print(Integer.toHexString(sgbBuffer[i] & 0xFF));
                System.out.print(" ");
                if ((i & 15) == 15) {
                    System.out.print("\n");
                }
            }
        }
        switch(sgbCommand) {
            case 0x00:
                desc = "Download color palettes 0 & 1";
                video.sgbSetPalette(0, 0, sgbBuffer[2], sgbBuffer[1]);
                video.sgbSetPalette(0, 1, sgbBuffer[4], sgbBuffer[3]);
                video.sgbSetPalette(0, 2, sgbBuffer[6], sgbBuffer[5]);
                video.sgbSetPalette(0, 3, sgbBuffer[8], sgbBuffer[7]);
                video.sgbSetPalette(1, 0, sgbBuffer[2], sgbBuffer[1]);
                video.sgbSetPalette(1, 1, sgbBuffer[10], sgbBuffer[9]);
                video.sgbSetPalette(1, 2, sgbBuffer[12], sgbBuffer[11]);
                video.sgbSetPalette(1, 3, sgbBuffer[14], sgbBuffer[13]);
                break;
            case 0x01:
                desc = "Download color palettes 2 & 3";
                video.sgbSetPalette(2, 0, sgbBuffer[2], sgbBuffer[1]);
                video.sgbSetPalette(2, 1, sgbBuffer[4], sgbBuffer[3]);
                video.sgbSetPalette(2, 2, sgbBuffer[6], sgbBuffer[5]);
                video.sgbSetPalette(2, 3, sgbBuffer[8], sgbBuffer[7]);
                video.sgbSetPalette(3, 0, sgbBuffer[2], sgbBuffer[1]);
                video.sgbSetPalette(3, 1, sgbBuffer[10], sgbBuffer[9]);
                video.sgbSetPalette(3, 2, sgbBuffer[12], sgbBuffer[11]);
                video.sgbSetPalette(3, 3, sgbBuffer[14], sgbBuffer[13]);
                break;
            case 0x02:
                desc = "Download color palettes 0 & 3";
                video.sgbSetPalette(0, 0, sgbBuffer[2], sgbBuffer[1]);
                video.sgbSetPalette(0, 1, sgbBuffer[4], sgbBuffer[3]);
                video.sgbSetPalette(0, 2, sgbBuffer[6], sgbBuffer[5]);
                video.sgbSetPalette(0, 3, sgbBuffer[8], sgbBuffer[7]);
                video.sgbSetPalette(3, 0, sgbBuffer[2], sgbBuffer[1]);
                video.sgbSetPalette(3, 1, sgbBuffer[10], sgbBuffer[9]);
                video.sgbSetPalette(3, 2, sgbBuffer[12], sgbBuffer[11]);
                video.sgbSetPalette(3, 3, sgbBuffer[14], sgbBuffer[13]);
                break;
            case 0x03:
                desc = "Download color palettes 1 & 2";
                video.sgbSetPalette(1, 0, sgbBuffer[2], sgbBuffer[1]);
                video.sgbSetPalette(1, 1, sgbBuffer[4], sgbBuffer[3]);
                video.sgbSetPalette(1, 2, sgbBuffer[6], sgbBuffer[5]);
                video.sgbSetPalette(1, 3, sgbBuffer[8], sgbBuffer[7]);
                video.sgbSetPalette(2, 0, sgbBuffer[2], sgbBuffer[1]);
                video.sgbSetPalette(2, 1, sgbBuffer[10], sgbBuffer[9]);
                video.sgbSetPalette(2, 2, sgbBuffer[12], sgbBuffer[11]);
                video.sgbSetPalette(2, 3, sgbBuffer[14], sgbBuffer[13]);
                break;
            case 0x04:
                desc = "'Block' Area Designation Mode";
                for (i = 0; i < sgbBuffer[1]; i++) {
                    video.sgbBlockDesignate(sgbBuffer[i * 6 + 2], sgbBuffer[i * 6 + 3], sgbBuffer[i * 6 + 4], sgbBuffer[i * 6 + 5], sgbBuffer[i * 6 + 6], sgbBuffer[i * 6 + 7]);
                }
                break;
            case 0x05:
                desc = "'Line' Area Designation Mode";
                for (i = 0; i < sgbBuffer[1]; i++) {
                    video.sgbLineDesignate(sgbBuffer[i + 2]);
                }
                break;
            case 0x06:
                desc = "'Divide' Area Designation Mode";
                video.sgbDivideDesignate(sgbBuffer[1], sgbBuffer[2]);
                break;
            case 0x07:
                desc = "'1CHR' Area Designation Mode";
                for (i = 0; i < 90; i++) {
                    video.sgbSetPaletteOverlay(i * 4 + 0, (sgbBuffer[i + 6]) & 0x03 >> 0);
                    video.sgbSetPaletteOverlay(i * 4 + 1, (sgbBuffer[i + 6]) & 0x0C >> 2);
                    video.sgbSetPaletteOverlay(i * 4 + 2, (sgbBuffer[i + 6]) & 0x30 >> 4);
                    video.sgbSetPaletteOverlay(i * 4 + 3, (sgbBuffer[i + 6]) & 0xC0 >> 6);
                }
                break;
            case 0x08:
                desc = "Sound On/Off";
                break;
            case 0x0A:
                desc = "Set SGB Palette Indirect";
                video.sgbSetPaletteIndirect((sgbBuffer[2] & 0x01) << 8 | (sgbBuffer[1] & 0xFF), (sgbBuffer[4] & 0x01) << 8 | (sgbBuffer[3] & 0xFF), (sgbBuffer[6] & 0x01) << 8 | (sgbBuffer[5] & 0xFF), (sgbBuffer[8] & 0x01) << 8 | (sgbBuffer[7] & 0xFF), (sgbBuffer[10] & 0x01) << 8 | (sgbBuffer[9] & 0xFF));
                break;
            case 0x0B:
                desc = "Set System Color Palette Data";
                video.sgbPaletteTransfer();
                break;
            case 0x0F:
                desc = "Super NES WRAM Transfer 1";
                break;
            case 0x11:
                desc = "Controller 2 Request";
                joy.setSgbPlayer(sgbBuffer[1]);
                break;
            case 0x13:
                desc = "CharSet Transfer";
                video.sgbCharsetTransfer((sgbBuffer[1] & 0x04) == 0x04, (sgbBuffer[1] & 0x01) == 0x01);
                break;
            case 0x14:
                desc = "Picture Transfer";
                video.sgbPictureTransfer();
                break;
            case 0x15:
                desc = "Set Attribute from ATF";
                video.sgbAtfTransfer();
                break;
            case 0x16:
                desc = "Set Data from ATF";
                video.sgbSetOverlayFromAtf(sgbBuffer[1]);
                break;
            case 0x17:
                desc = "GameBoy Window Mask";
                if (sgbBuffer[1] == 0) {
                    video.sgbvramon = false;
                }
                if (sgbBuffer[1] == 1 || sgbBuffer[1] == 3) {
                    video.sgbvramon = true;
                }
                break;
            default:
                desc = "Unprogrammed";
                break;
        }
        if (PgbSettings.DEBUG) {
            System.out.println(desc + "\n");
        }
    }
}
