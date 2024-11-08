package dioscuri.module.fdc;

import dioscuri.Emulator;
import dioscuri.exception.ModuleException;
import dioscuri.exception.StorageDeviceException;
import dioscuri.exception.UnknownPortException;
import dioscuri.exception.WriteOnlyPortException;
import dioscuri.interfaces.Module;
import dioscuri.module.*;
import dioscuri.module.cpu32.DMAController;
import dioscuri.module.cpu32.DMATransferCapable;
import dioscuri.module.cpu32.HardwareComponent;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of a Floppy disk controller module.
 *
 * @see dioscuri.module.AbstractModule
 *      <p/>
 *      Metadata module ********************************************
 *      general.type : fdc general.name : Floppy Disk Controller
 *      general.architecture : Von Neumann general.description : Implements a
 *      standard floppy disk controller for 3.5" floppies general.creator :
 *      Tessella Support Services, Koninklijke Bibliotheek, Nationaal Archief of
 *      the Netherlands general.version : 1.0 general.keywords : floppy, disk,
 *      controller, 1.44, 2.88, 3.5 inch, SD, DD, HD general.relations :
 *      motherboard general.yearOfIntroduction : 1982 general.yearOfEnding :
 *      general.ancestor : 5.25 inch floppy disk general.successor : -
 *      <p/>
 *      Issues: - check all fixme statements - sometimes MSR register is set to
 *      busy, but I am not sure if it is done the right way as it loses all
 *      previous bits: // Data register not ready, drive busy MSR = (byte) (1 <<
 *      drive);
 *      <p/>
 *      Notes: - Floppy disk controller is usually an 8272, 8272A, NEC765 (or
 *      compatible), or an 82072 or 82077AA for perpendicular recording at
 *      2.88M. - The FDC is only capable of reading from and writing to virtual
 *      floppy disks (images), not physical disks. - The FDC does not perform a
 *      CRC (Cyclic Redundancy Check). - FDC commands Scan low, scan low or
 *      equal, scan high or equal are not implemented. - Datarates (speed of
 *      reading/writing) is not taken into account. - Enhanced commands like
 *      lock and unlock are not fully implemented. - Current FDC only works with
 *      DMA data transfer
 *      <p/>
 *      Overview of FDC registers (ref: Port list, made by Ralf Brown:
 *      [http://mudlist.eorbit.net/~adam/pickey/ports.html]) 03F0 R diskette
 *      controller status A (PS/2) (see #P173) 03F0 R diskette controller status
 *      A (PS/2 model 30) (see #P174) 03F0 R diskette EHD controller board
 *      jumper settings (82072AA) (see #P175) 03F1 R diskette controller status
 *      B (PS/2) (see #P176) 03F1 R diskette controller status B (PS/2 model 30)
 *      (see #P177) 03F2 W diskette controller DOR (Digital Output Register)
 *      (see #P178) 03F3 ?W tape drive register (on the 82077AA) bit 7-2
 *      reserved, tri-state bit 1-0 tape select =00 none, drive 0 cannot be a
 *      tape drive. =01 drive1 =10 drive2 =11 drive3 03F4 R diskette controller
 *      main status register (see #P179) Note: in non-DMA mode, all data
 *      transfers occur through PORT 03F5h and the status registers (bit 5 here
 *      indicates data read/write rather than than command/status read/write)
 *      03F4 W diskette controller data rate select register (see #P180) 03F5 R
 *      diskette command/data register 0 (ST0) (see #P181) status register 1
 *      (ST1) (see #P182) status register 2 (ST2) (see #P183) status register 3
 *      (ST3) (see #P184) 03F5 W diskette command register. The commands
 *      summarized here are mostly multibyte commands. This is for brief
 *      recognition only. (see #P187) 03F6 -- reserved on FDC 03F6 rW FIXED disk
 *      controller data register (see #P185) 03F7 RW harddisk controller (see
 *      #P186) 03F7 R diskette controller DIR (Digital Input Register, PC/AT
 *      mode) bit 7 = 1 diskette change bit 6-0 tri-state on FDC 03F7 R diskette
 *      controller DIR (Digital Input Register, PS/2 mode) bit 7 = 1 diskette
 *      change bit 6-3 = 1 bit 2 datarate select1 bit 1 datarate select0 bit 0 =
 *      0 high density select (500Kb/s, 1Mb/s) conflict bit 0 FIXED DISK drive 0
 *      select 03F7 R diskette controller DIR (Digital Input Register, PS/2
 *      model 30) bit 7 = 0 diskette change bit 6-4 = 0 bit 3 -DMA gate (value
 *      from DOR register) bit 2 NOPREC (value from CCR register) bit 1 datarate
 *      select1 bit 0 datarate select0 conflict bit 0 FIXED DISK drive 0 select
 *      03F7 W configuration control register (PC/AT, PS/2) bit 7-2 reserved,
 *      tri-state bit 1-0 = 00 500 Kb/S mode (MFM) = 01 300 Kb/S mode (MFM) = 10
 *      250 Kb/S mode (MFM) = 11 1 Mb/S mode (MFM) (on 82072/82077AA) conflict
 *      bit 0 FIXED DISK drive 0 select 03F7 W configuration control register
 *      (PS/2 model 30) bit 7-3 reserved, tri-state bit 2 NOPREC (has no
 *      function. set to 0 by hardreset) bit 1-0 = 00 500 Kb/S mode (MFM) = 01
 *      300 Kb/S mode (MFM) = 10 250 Kb/S mode (MFM) = 11 1 Mb/S mode (MFM) (on
 *      82072/82077AA) conflict bit 0 FIXED DISK drive 0 select
 */
public class FDC extends ModuleFDC implements DMATransferCapable {

    private static final Logger logger = Logger.getLogger(FDC.class.getName());

    private Emulator emu;

    private DMAController dma32;

    private int irqNumber;

    private boolean pendingIRQ;

    private int resetSenseInterrupt;

    public DMA8Handler dma8Handler;

    private boolean tc;

    private boolean dmaAndInterruptEnabled;

    private int updateInterval;

    private Drive[] drives;

    private int numberOfDrives;

    private int[] dataRates;

    private int dataRate;

    private boolean fdcEnabled;

    private boolean fdcEnabledPrevious;

    private int drive;

    private int formatCount;

    private byte formatFillbyte;

    protected byte[] floppyBuffer;

    private int floppyBufferIndex;

    private byte floppyBufferCurrentByte;

    private byte[] command;

    private int commandIndex;

    private int commandSize;

    private int commandPending;

    private boolean commandComplete;

    private byte[] result;

    private int resultIndex;

    private int resultSize;

    byte nonDMA;

    byte lock;

    byte srt;

    byte hut;

    byte hlt;

    byte config;

    byte preTrack;

    byte perpMode;

    private byte dor;

    private byte tdr;

    private byte msr;

    private int statusRegister0;

    private int statusRegister1;

    private int statusRegister2;

    private int statusRegister3;

    private static final int PORT_FLOPPY_STATUS_A = 0x03F0;

    private static final int PORT_FLOPPY_STATUS_B = 0x03F1;

    private static final int PORT_FLOPPY_DOR = 0x03F2;

    private static final int PORT_FLOPPY_TAPEDRIVE = 0x03F3;

    private static final int PORT_FLOPPY_MAIN_DATARATE = 0x03F4;

    private static final int PORT_FLOPPY_CMD_DATA = 0x03F5;

    private static final int PORT_FLOPPY_RESERVED_FIXED = 0x03F6;

    private static final int PORT_FLOPPY_HD_CONTROLLER = 0x03F7;

    private static final byte FLOPPY_DRIVETYPE_NONE = 0x00;

    private static final byte FLOPPY_DRIVETYPE_525DD = 0x01;

    private static final byte FLOPPY_DRIVETYPE_525HD = 0x02;

    private static final byte FLOPPY_DRIVETYPE_350DD = 0x03;

    private static final byte FLOPPY_DRIVETYPE_350HD = 0x04;

    private static final byte FLOPPY_DRIVETYPE_350ED = 0x05;

    private static final byte FLOPPY_DISKTYPE_NONE = 0x00;

    private static final byte FLOPPY_DISKTYPE_360K = 0x01;

    private static final byte FLOPPY_DISKTYPE_1_2 = 0x02;

    private static final byte FLOPPY_DISKTYPE_720K = 0x03;

    private static final byte FLOPPY_DISKTYPE_1_44 = 0x04;

    private static final byte FLOPPY_DISKTYPE_2_88 = 0x05;

    private static final byte FLOPPY_DISKTYPE_160K = 0x06;

    private static final byte FLOPPY_DISKTYPE_180K = 0x07;

    private static final byte FLOPPY_DISKTYPE_320K = 0x08;

    private static final int FDC_CMD_MRQ = 0x80;

    private static final int FDC_CMD_DIO = 0x40;

    private static final int FDC_CMD_NDMA = 0x20;

    private static final int FDC_CMD_BUSY = 0x10;

    private static final int FDC_CMD_ACTD = 0x08;

    private static final int FDC_CMD_ACTC = 0x04;

    private static final int FDC_CMD_ACTB = 0x02;

    private static final int FDC_CMD_ACTA = 0x01;

    private static final int FDC_DMA_CHANNEL = 2;

    /**
     * Class constructor
     *
     * @param owner
     */
    public FDC(Emulator owner) {
        emu = owner;
        updateInterval = -1;
        irqNumber = -1;
        pendingIRQ = false;
        resetSenseInterrupt = 0;
        dma8Handler = null;
        dmaAndInterruptEnabled = false;
        drives = new Drive[1];
        drives[0] = new Drive();
        dataRates = new int[] { 500, 300, 250, 1000 };
        dataRate = 0;
        fdcEnabled = false;
        fdcEnabledPrevious = false;
        drive = 0;
        command = new byte[10];
        commandIndex = 0;
        commandSize = 0;
        commandPending = 0;
        commandComplete = false;
        formatCount = 0;
        formatFillbyte = 0;
        tc = false;
        floppyBuffer = new byte[512 + 2];
        floppyBufferIndex = 0;
        nonDMA = 0;
        lock = 0;
        srt = 0;
        hut = 0;
        hlt = 0;
        config = 0;
        preTrack = 0;
        perpMode = 0;
        result = new byte[10];
        resultIndex = 0;
        resultSize = 0;
        dor = 0;
        msr = 0 | FDC_CMD_DIO;
        tdr = 0;
        statusRegister0 = 0;
        statusRegister1 = 0;
        statusRegister2 = 0;
        statusRegister3 = 0;
        logger.log(Level.INFO, "[" + super.getType() + "] " + getClass().getName() + " -> AbstractModule created successfully.");
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.module.AbstractModule
     */
    @Override
    public boolean reset() {
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        ModuleRTC rtc = (ModuleRTC) super.getConnection(Module.Type.RTC);
        ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
        ModuleDMA dma = (ModuleDMA) super.getConnection(Module.Type.DMA);
        ModuleATA ata = (ModuleATA) super.getConnection(Module.Type.ATA);
        motherboard.setIOPort(PORT_FLOPPY_STATUS_A, this);
        motherboard.setIOPort(PORT_FLOPPY_STATUS_B, this);
        motherboard.setIOPort(PORT_FLOPPY_DOR, this);
        motherboard.setIOPort(PORT_FLOPPY_TAPEDRIVE, this);
        motherboard.setIOPort(PORT_FLOPPY_MAIN_DATARATE, this);
        motherboard.setIOPort(PORT_FLOPPY_CMD_DATA, this);
        motherboard.setIOPort(PORT_FLOPPY_RESERVED_FIXED, this);
        motherboard.setIOPort(PORT_FLOPPY_HD_CONTROLLER, this);
        irqNumber = pic.requestIRQNumber(this);
        if (irqNumber > -1) {
            logger.log(Level.CONFIG, "[" + super.getType() + "]" + " IRQ number set to: " + irqNumber);
        } else {
            logger.log(Level.WARNING, "[" + super.getType() + "]" + " Request of IRQ number failed.");
        }
        if (motherboard.requestTimer(this, updateInterval, false)) {
            logger.log(Level.CONFIG, "[" + super.getType() + "]" + " Timer requested successfully.");
        } else {
            logger.log(Level.WARNING, "[" + super.getType() + "]" + " Failed to request a timer.");
        }
        if (!emu.isCpu32bit()) {
            dma8Handler = new DMA8Handler(this);
            if (dma.registerDMAChannel(FDC_DMA_CHANNEL, dma8Handler)) {
                logger.log(Level.CONFIG, "[" + super.getType() + "]" + " DMA channel registered to line: " + FDC_DMA_CHANNEL);
            } else {
                logger.log(Level.WARNING, "[" + super.getType() + "]" + " Failed to register DMA channel " + FDC_DMA_CHANNEL);
            }
        }
        rtc.setCMOSRegister(0x14, (byte) (rtc.getCMOSRegister(0x14) | 0x01));
        return reset(1);
    }

    /**
     * FDC specific reset, with value to indicate reset type
     *
     * @param resetType Type of reset passed to FDC<BR>
     *                  0: Warm reset (SW reset)<BR>
     *                  1: Cold reset (HW reset)
     * @return boolean true if module has been reset successfully, false
     *         otherwise
     */
    private boolean reset(int resetType) {
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        ModuleRTC rtc = (ModuleRTC) super.getConnection(Module.Type.RTC);
        ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
        ModuleDMA dma = (ModuleDMA) super.getConnection(Module.Type.DMA);
        ModuleATA ata = (ModuleATA) super.getConnection(Module.Type.ATA);
        pendingIRQ = false;
        resetSenseInterrupt = 0;
        msr = 0 | FDC_CMD_DIO;
        statusRegister0 = 0;
        statusRegister1 = 0;
        statusRegister2 = 0;
        statusRegister3 = 0;
        if (resetType == 1) {
            dor = 0x0C;
            for (int i = 0; i < drives.length; i++) {
                drives[i].dir |= 0x80;
            }
            dataRate = 2;
            lock = 0;
        } else {
            logger.log(Level.INFO, "[" + super.getType() + "]" + " FDC controller reset (software)");
        }
        if (lock == 0) {
            config = 0;
            preTrack = 0;
        }
        perpMode = 0;
        for (int i = 0; i < drives.length; i++) {
            drives[i].reset();
        }
        pic.clearIRQ(irqNumber);
        if (!emu.isCpu32bit()) {
            dma.setDMARequest(FDC_DMA_CHANNEL, false);
        }
        this.enterIdlePhase();
        if (resetType == 1) {
            logger.log(Level.INFO, "[" + super.getType() + "]" + " AbstractModule has been reset.");
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Module
     */
    @Override
    public void stop() {
        for (int i = 0; i < drives.length; i++) {
            if (drives[i] != null && drives[i].containsFloppy()) {
                if (!this.ejectCarrier(i)) {
                    logger.log(Level.SEVERE, "[" + super.getType() + "] Drive " + i + ": eject floppy failed.");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.module.AbstractModule
     */
    @Override
    public String getDump() {
        String dump = "";
        String ret = "\r\n";
        String tab = "\t";
        dump = "FDC dump:" + ret;
        dump += "In total " + drives.length + " floppy drives exist:" + ret;
        for (int i = 0; i < drives.length; i++) {
            if (drives[i] != null) {
                dump += "Drive " + i + tab + ":" + tab + drives[i].toString() + ret;
            } else {
                dump += "Drive " + i + tab + ":" + tab + "not enabled" + ret;
            }
        }
        return dump;
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Updateable
     */
    @Override
    public int getUpdateInterval() {
        return updateInterval;
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Updateable
     */
    @Override
    public void setUpdateInterval(int interval) {
        if (interval > 0) {
            updateInterval = interval;
        } else {
            updateInterval = 1000;
        }
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        motherboard.resetTimer(this, updateInterval);
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Updateable
     */
    @Override
    public void update() {
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        ModuleDMA dma = (ModuleDMA) super.getConnection(Module.Type.DMA);
        if (commandPending != 0x00) {
            drive = dor & 0x03;
            switch(commandPending) {
                case 0x07:
                    statusRegister0 = 0x20 | drive;
                    if ((drives[drive].getDriveType() == FLOPPY_DRIVETYPE_NONE) || !drives[drive].isMotorRunning()) {
                        statusRegister0 |= 0x50;
                    }
                    this.enterIdlePhase();
                    this.setInterrupt();
                    break;
                case 0x0F:
                    statusRegister0 = 0x20 | (drives[drive].hds << 2) | drive;
                    this.enterIdlePhase();
                    this.setInterrupt();
                    break;
                case 0x4A:
                    this.enterResultPhase();
                    break;
                case 0x45:
                case 0xC5:
                    if (tc) {
                        statusRegister0 = (drives[drive].hds << 2) | drive;
                        statusRegister1 = 0;
                        statusRegister2 = 0;
                        this.enterResultPhase();
                    } else {
                        if (emu.isCpu32bit()) {
                            dma32.holdDREQ(FDC_DMA_CHANNEL & 3);
                        } else {
                            dma.setDMARequest(FDC_DMA_CHANNEL, true);
                        }
                    }
                    break;
                case 0x46:
                case 0x66:
                case 0xC6:
                case 0xE6:
                    if (emu.isCpu32bit()) {
                        dma32.holdDREQ(FDC_DMA_CHANNEL & 3);
                    } else {
                        dma.setDMARequest(FDC_DMA_CHANNEL, true);
                    }
                    break;
                case 0x4D:
                    if ((formatCount == 0) || tc) {
                        formatCount = 0;
                        statusRegister0 = (drives[drive].hds << 2) | drive;
                        this.enterResultPhase();
                    } else {
                        if (emu.isCpu32bit()) {
                            dma32.holdDREQ(FDC_DMA_CHANNEL & 3);
                        } else {
                            dma.setDMARequest(FDC_DMA_CHANNEL, true);
                        }
                    }
                    break;
                case 0xFE:
                    this.reset(0);
                    commandPending = 0;
                    statusRegister0 = 0xC0;
                    this.setInterrupt();
                    resetSenseInterrupt = 4;
                    break;
                default:
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: unknown command during update.");
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Addressable
     */
    @Override
    public byte getIOPortByte(int portAddress) throws ModuleException, UnknownPortException, WriteOnlyPortException {
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        ModuleRTC rtc = (ModuleRTC) super.getConnection(Module.Type.RTC);
        ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
        ModuleDMA dma = (ModuleDMA) super.getConnection(Module.Type.DMA);
        ModuleATA ata = (ModuleATA) super.getConnection(Module.Type.ATA);
        logger.log(Level.INFO, "[" + super.getType() + "]" + " IN command (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + " received");
        byte value = 0;
        switch(portAddress) {
            case 0x03F0:
            case 0x03F1:
                logger.log(Level.WARNING, "[" + super.getType() + "]" + " Reading ports 0x3F0 and 0x3F1 not implemented");
                break;
            case 0x03F2:
                logger.log(Level.CONFIG, "[" + super.getType() + "]" + " Is reading allowed of port 0x3F2? Returned DOR");
                value = dor;
                break;
            case 0x03F3:
                int drv = dor & 0x03;
                if (drives[drv].containsFloppy()) {
                    switch(drives[drv].getFloppyType()) {
                        case FLOPPY_DISKTYPE_160K:
                        case FLOPPY_DISKTYPE_180K:
                        case FLOPPY_DISKTYPE_320K:
                        case FLOPPY_DISKTYPE_360K:
                        case FLOPPY_DISKTYPE_1_2:
                            value = 0x00;
                            break;
                        case FLOPPY_DISKTYPE_720K:
                            value = (byte) 0xc0;
                            break;
                        case FLOPPY_DISKTYPE_1_44:
                            value = (byte) 0x80;
                            break;
                        case FLOPPY_DISKTYPE_2_88:
                            value = 0x40;
                            break;
                        default:
                            value = 0x20;
                            break;
                    }
                } else {
                    value = 0x20;
                }
                break;
            case 0x03F4:
                value = msr;
                break;
            case 0x03F5:
                if (resultSize == 0) {
                    logger.log(Level.INFO, "[" + super.getType() + "]" + " Port 0x3F5: no results to read");
                    msr = 0 | FDC_CMD_DIO;
                    value = result[0];
                } else {
                    value = result[resultIndex];
                    resultIndex++;
                    msr &= 0xF0;
                    this.clearInterrupt();
                    if (resultIndex >= resultSize) {
                        this.enterIdlePhase();
                    }
                }
                break;
            case 0x03F6:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " IN (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + ": reserved port.");
                value = ata.getIOPortByte(portAddress);
                break;
            case 0x03F7:
                value = ata.getIOPortByte(portAddress);
                value &= 0x7f;
                drv = dor & 0x03;
                if ((dor & (1 << (drv + 4))) == 1) {
                    if (drives[drv] != null) {
                        value |= (drives[drv].dir & 0x80);
                    } else {
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Non-existing drive requested at port " + Integer.toHexString(portAddress).toUpperCase());
                    }
                }
                break;
            default:
                logger.log(Level.WARNING, "[" + super.getType() + "]" + " Unknown port address encountered: " + Integer.toHexString(portAddress).toUpperCase());
                break;
        }
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Addressable
     */
    @Override
    public void setIOPortByte(int portAddress, byte value) throws ModuleException, UnknownPortException {
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        ModuleRTC rtc = (ModuleRTC) super.getConnection(Module.Type.RTC);
        ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
        ModuleDMA dma = (ModuleDMA) super.getConnection(Module.Type.DMA);
        ModuleATA ata = (ModuleATA) super.getConnection(Module.Type.ATA);
        logger.log(Level.INFO, "[" + super.getType() + "]" + " OUT (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + ": 0x" + Integer.toHexString(((int) value) & 0xFF).toUpperCase());
        switch(portAddress) {
            case 0x03F0:
            case 0x03F1:
                logger.log(Level.WARNING, "[" + super.getType() + "]" + " Port address is read only and cannot be written to: " + Integer.toHexString(portAddress).toUpperCase());
                break;
            case 0x03F2:
                for (int i = 0; i < drives.length; i++) {
                    if (drives[i] != null) {
                        switch(i) {
                            case 0:
                                drives[0].setMotor((value & 0x10) > 0 ? true : false);
                                break;
                            case 1:
                                drives[1].setMotor((value & 0x20) > 0 ? true : false);
                                break;
                            case 2:
                                drives[2].setMotor((value & 0x40) > 0 ? true : false);
                                break;
                            case 3:
                                drives[3].setMotor((value & 0x80) > 0 ? true : false);
                                break;
                            default:
                                logger.log(Level.WARNING, "[" + super.getType() + "]" + " Unknown drive selected at port " + Integer.toHexString(portAddress).toUpperCase());
                                break;
                        }
                    } else {
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Non-existing drive selected at port " + Integer.toHexString(portAddress).toUpperCase());
                    }
                }
                dmaAndInterruptEnabled = ((value & 0x08) > 0) ? true : false;
                fdcEnabled = ((value & 0x04) > 0) ? true : false;
                drive = value & 0x03;
                fdcEnabledPrevious = ((dor & 0x04) > 0) ? true : false;
                dor = value;
                if (!fdcEnabledPrevious && fdcEnabled) {
                    motherboard.resetTimer(this, updateInterval);
                    motherboard.setTimerActiveState(this, true);
                } else if (fdcEnabledPrevious && !fdcEnabled) {
                    msr = 0 | FDC_CMD_DIO;
                    commandPending = 0xFE;
                }
                logger.log(Level.INFO, "[" + super.getType() + "]" + " OUT (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + ", DMA/IRQ=" + dmaAndInterruptEnabled + ", FDC=" + fdcEnabled + ", drive=" + drive + ", motorRunning=" + drives[drive].isMotorRunning());
                break;
            case 0x03F4:
                dataRate = value & 0x03;
                if ((value & 0x80) > 0) {
                    msr = 0 | FDC_CMD_DIO;
                    commandPending = 0xFE;
                    motherboard.resetTimer(this, updateInterval);
                    motherboard.setTimerActiveState(this, true);
                }
                if ((value & 0x7C) > 0) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " OUT (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + ": drive is busy, but in non-DMA mode which is not supported.");
                }
                break;
            case 0x03F5:
                if (commandComplete) {
                    if (commandPending != 0) {
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " new command received while old command is still pending.");
                    }
                    command[0] = value;
                    commandComplete = false;
                    commandIndex = 1;
                    msr &= 0x0f;
                    msr |= FDC_CMD_MRQ | FDC_CMD_BUSY;
                    int cmd = value & 0xFF;
                    switch(cmd) {
                        case 0x03:
                            commandSize = 3;
                            break;
                        case 0x04:
                            commandSize = 2;
                            break;
                        case 0x07:
                            commandSize = 2;
                            break;
                        case 0x08:
                            commandSize = 1;
                            break;
                        case 0x0F:
                            commandSize = 3;
                            break;
                        case 0x4A:
                            commandSize = 2;
                            break;
                        case 0x4D:
                            commandSize = 10;
                            break;
                        case 0x45:
                        case 0xC5:
                            commandSize = 9;
                            break;
                        case 0x46:
                        case 0x66:
                        case 0xC6:
                        case 0xE6:
                            commandSize = 9;
                            break;
                        case 0x0E:
                        case 0x10:
                        case 0x14:
                        case 0x94:
                            commandSize = 0;
                            commandPending = cmd;
                            this.enterResultPhase();
                            break;
                        case 0x12:
                            commandSize = 2;
                            break;
                        case 0x13:
                            commandSize = 4;
                            break;
                        case 0x18:
                            commandSize = 0;
                            statusRegister0 = 0x80;
                            this.enterResultPhase();
                            break;
                        case 0x8F:
                        case 0xCF:
                            commandSize = 3;
                            break;
                        default:
                            logger.log(Level.WARNING, "[" + super.getType() + "]" + " OUT (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + ": invalid FDC command.");
                            commandSize = 0;
                            statusRegister0 = 0x80;
                            this.enterResultPhase();
                            break;
                    }
                } else {
                    command[commandIndex++] = value;
                }
                if (commandIndex == commandSize) {
                    this.executeCommand();
                    commandComplete = true;
                }
                break;
            case 0x03F6:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " OUT (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + ": reserved port.");
                ata.setIOPortByte(portAddress, value);
                break;
            case 0x03F7:
                dataRate = value & 0x03;
                switch(dataRate) {
                    case 0:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Datarate is set to 500 Kbps");
                        break;
                    case 1:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Datarate is set to 300 Kbps");
                        break;
                    case 2:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Datarate is set to 250 Kbps");
                        break;
                    case 3:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Datarate is set to 1 Mbps");
                        break;
                }
                break;
            default:
                logger.log(Level.WARNING, "[" + super.getType() + "]" + " Unknown port address encountered: " + Integer.toHexString(portAddress).toUpperCase());
                break;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Addressable
     */
    @Override
    public byte[] getIOPortWord(int portAddress) throws ModuleException, WriteOnlyPortException {
        logger.log(Level.WARNING, "[" + super.getType() + "]" + " IN command (word) to port " + Integer.toHexString(portAddress).toUpperCase() + " received");
        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Returned default value 0xFFFF");
        return new byte[] { (byte) 0x0FF, (byte) 0x0FF };
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Addressable
     */
    @Override
    public void setIOPortWord(int portAddress, byte[] dataWord) throws ModuleException {
        logger.log(Level.WARNING, "[" + super.getType() + "]" + " OUT command (word) to port " + Integer.toHexString(portAddress).toUpperCase() + " received. No action taken.");
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Addressable
     */
    @Override
    public byte[] getIOPortDoubleWord(int portAddress) throws ModuleException, WriteOnlyPortException {
        logger.log(Level.WARNING, "[" + super.getType() + "]" + " IN command (double word) to port " + Integer.toHexString(portAddress).toUpperCase() + " received");
        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Returned default value 0xFFFFFFFF");
        return new byte[] { (byte) 0x0FF, (byte) 0x0FF, (byte) 0x0FF, (byte) 0x0FF };
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Addressable
     */
    @Override
    public void setIOPortDoubleWord(int portAddress, byte[] dataDoubleWord) throws ModuleException {
        logger.log(Level.WARNING, "[" + super.getType() + "]" + " OUT command (double word) to port " + Integer.toHexString(portAddress).toUpperCase() + " received. No action taken.");
    }

    /**
     * Raise interrupt signal
     */
    protected void setInterrupt() {
        ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
        pic.setIRQ(irqNumber);
        pendingIRQ = true;
        resetSenseInterrupt = 0;
    }

    /**
     * Clear interrupt signal
     */
    protected void clearInterrupt() {
        if (pendingIRQ) {
            ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
            pic.clearIRQ(irqNumber);
            pendingIRQ = false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.module.ModuleFDC
     */
    @Override
    public boolean setNumberOfDrives(int totalDrives) {
        ModuleRTC rtc = (ModuleRTC) super.getConnection(Module.Type.RTC);
        if (totalDrives > 0 && totalDrives <= 4) {
            numberOfDrives = totalDrives;
            drives = new Drive[numberOfDrives];
            for (int i = 0; i < drives.length; i++) {
                drives[i] = new Drive();
            }
            if (numberOfDrives == 1) {
                rtc.setCMOSRegister(0x14, (byte) ((rtc.getCMOSRegister(0x14) & 0x3F) | 0x00));
            } else if (numberOfDrives == 2) {
                rtc.setCMOSRegister(0x14, (byte) ((rtc.getCMOSRegister(0x14) & 0x3F) | 0x40));
            } else if (numberOfDrives == 3) {
                rtc.setCMOSRegister(0x14, (byte) ((rtc.getCMOSRegister(0x14) & 0x3F) | 0x80));
            } else if (numberOfDrives == 4) {
                rtc.setCMOSRegister(0x14, (byte) ((rtc.getCMOSRegister(0x14) & 0x3F) | 0xC0));
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.module.ModuleFDC
     */
    @Override
    public boolean insertCarrier(String driveLetter, byte carrierType, File imageFile, boolean writeProtected) {
        int driveIndex = -1;
        if (driveLetter.equalsIgnoreCase("A")) {
            driveIndex = 0;
        } else if (driveLetter.equalsIgnoreCase("B")) {
            driveIndex = 1;
        }
        return insertCarrier(driveIndex, carrierType, imageFile, writeProtected);
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.module.ModuleFDC
     */
    @Override
    public boolean ejectCarrier(String driveLetter) {
        int driveIndex = -1;
        if (driveLetter.equalsIgnoreCase("A")) {
            driveIndex = 0;
        } else if (driveLetter.equalsIgnoreCase("B")) {
            driveIndex = 1;
        }
        return ejectCarrier(driveIndex);
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.module.ModuleFDC
     */
    @Override
    public boolean insertCarrier(int driveIndex, byte carrierType, File imageFile, boolean writeProtected) {
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        ModuleRTC rtc = (ModuleRTC) super.getConnection(Module.Type.RTC);
        ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
        ModuleDMA dma = (ModuleDMA) super.getConnection(Module.Type.DMA);
        ModuleATA ata = (ModuleATA) super.getConnection(Module.Type.ATA);
        if (driveIndex == 0) {
            String driveLetter = "A";
            if (drives.length > 0 && !(drives[driveIndex].containsFloppy())) {
                switch(carrierType) {
                    case FLOPPY_DISKTYPE_NONE:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0x0f) | 0x00));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_NONE);
                        break;
                    case FLOPPY_DISKTYPE_360K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0x0f) | 0x10));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525DD);
                        break;
                    case FLOPPY_DISKTYPE_1_2:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0x0f) | 0x20));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525HD);
                        break;
                    case FLOPPY_DISKTYPE_720K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0x0f) | 0x30));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_350DD);
                        break;
                    case FLOPPY_DISKTYPE_1_44:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0x0f) | 0x40));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_350HD);
                        break;
                    case FLOPPY_DISKTYPE_2_88:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0x0f) | 0x50));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_350ED);
                        break;
                    case FLOPPY_DISKTYPE_160K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0x0f) | 0x60));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525DD);
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Drive " + driveLetter + " set to reserved CMOS floppy drive type 6");
                        break;
                    case FLOPPY_DISKTYPE_180K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0x0f) | 0x70));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525DD);
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Drive " + driveLetter + " set to reserved CMOS floppy drive type 7");
                        break;
                    case FLOPPY_DISKTYPE_320K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0x0f) | 0x80));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525DD);
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Drive " + driveLetter + " set to reserved CMOS floppy drive type 8");
                        break;
                    default:
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Unsupported floppy drive type.");
                        break;
                }
                try {
                    drives[driveIndex].insertFloppy(carrierType, imageFile, writeProtected);
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " Floppy \"" + imageFile.getName() + "\" is inserted in drive " + driveIndex);
                    return true;
                } catch (StorageDeviceException e) {
                    logger.log(Level.SEVERE, "[" + super.getType() + "]" + " Error: " + e.getMessage());
                }
            } else {
                logger.log(Level.SEVERE, "[" + super.getType() + "]" + " Drive " + driveLetter + " does not exist or already contains a floppy. Eject floppy first!");
            }
        } else if (driveIndex == 1) {
            String driveLetter = "B";
            if (drives.length > 1 && !(drives[driveIndex].containsFloppy())) {
                switch(carrierType) {
                    case FLOPPY_DISKTYPE_NONE:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0xf0) | 0x00));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_NONE);
                        break;
                    case FLOPPY_DISKTYPE_360K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0xf0) | 0x01));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525DD);
                        break;
                    case FLOPPY_DISKTYPE_1_2:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0xf0) | 0x02));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525HD);
                        break;
                    case FLOPPY_DISKTYPE_720K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0xf0) | 0x03));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_350DD);
                        break;
                    case FLOPPY_DISKTYPE_1_44:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0xf0) | 0x04));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_350HD);
                        break;
                    case FLOPPY_DISKTYPE_2_88:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0xf0) | 0x05));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_350ED);
                        break;
                    case FLOPPY_DISKTYPE_160K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0xf0) | 0x06));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525DD);
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Drive " + driveLetter + " set to reserved CMOS floppy drive type 6");
                        break;
                    case FLOPPY_DISKTYPE_180K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0xf0) | 0x07));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525DD);
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Drive " + driveLetter + " set to reserved CMOS floppy drive type 7");
                        break;
                    case FLOPPY_DISKTYPE_320K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0xf0) | 0x08));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525DD);
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Drive " + driveLetter + " set to reserved CMOS floppy drive type 8");
                        break;
                    default:
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Unsupported floppy drive type.");
                        break;
                }
                try {
                    drives[driveIndex].insertFloppy(carrierType, imageFile, writeProtected);
                    logger.log(Level.INFO, "[" + super.getType() + "]" + " Floppy \"" + imageFile.getName() + "\" is inserted in drive " + driveIndex);
                    return true;
                } catch (StorageDeviceException e) {
                    logger.log(Level.SEVERE, "[" + super.getType() + "]" + " Error: " + e.getMessage());
                }
            } else {
                logger.log(Level.SEVERE, "[" + super.getType() + "]" + " Drive " + driveLetter + " does not exist or already contains a floppy. Eject floppy first!");
            }
        } else {
            logger.log(Level.SEVERE, "[" + super.getType() + "]" + " Can not insert floppy because additional drives are not implemented.");
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.module.ModuleFDC
     */
    @Override
    public boolean ejectCarrier(int driveIndex) {
        try {
            if (driveIndex != -1) {
                boolean writeProtected = drives[driveIndex].writeProtected;
                drives[driveIndex].ejectFloppy();
                logger.log(Level.INFO, "[" + super.getType() + "]" + " Floppy is ejected from drive " + drive + ".");
                if (!writeProtected) {
                    logger.log(Level.INFO, "[" + super.getType() + "]" + " Floppy data is stored to image file.");
                }
                return true;
            } else {
                logger.log(Level.SEVERE, "[" + super.getType() + "]" + " Can not eject floppy because drive is not recognized.");
            }
        } catch (StorageDeviceException e) {
            logger.log(Level.SEVERE, "[" + super.getType() + "]" + e.getMessage());
            return false;
        }
        return false;
    }

    /**
     * Execute command of FDC Note: assumed is that all bytes of the command are
     * fetched. After execution of the command, the FDC will automatically enter
     * the result or idle phase.
     */
    private void executeCommand() {
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        ModuleDMA dma = (ModuleDMA) super.getConnection(Module.Type.DMA);
        int drv, hds, cylinder, sector, eot;
        int sectorSize, sectorTime, logicalSector, dataLength;
        boolean ableToTransfer;
        commandPending = command[0] & 0xFF;
        switch(commandPending) {
            case 0x03:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " CMD: specify");
                srt = (byte) ((command[1] >> 4) & 0x0F);
                hut = (byte) (command[1] & 0x0F);
                hlt = (byte) ((command[2] >> 1) & 0x7F);
                nonDMA = (byte) (command[2] & 0x01);
                this.enterIdlePhase();
                break;
            case 0x04:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " CMD: sense drive status");
                drv = (command[1] & 0x03);
                drives[drv].hds = (command[1] >> 2) & 0x01;
                statusRegister3 = (byte) (0x28 | (drives[drv].hds << 2) | drv);
                statusRegister3 |= (drives[drv].writeProtected ? 0x40 : 0x00);
                if (drives[drv].cylinder == 0) {
                    statusRegister3 |= 0x10;
                }
                this.enterResultPhase();
                break;
            case 0x45:
            case 0x46:
            case 0x66:
            case 0xC5:
            case 0xC6:
            case 0xE6:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " CMD: Read/write data");
                emu.statusChanged(Emulator.MODULE_FDC_TRANSFER_START);
                if ((dor & 0x08) == 0) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: read/write normal data -> DMA is disabled");
                }
                drv = command[1] & 0x03;
                dor &= 0xFC;
                dor |= drv;
                drives[drv].multiTrack = (((command[0] >> 7) & 0x01) == 0x01 ? true : false);
                cylinder = command[2];
                hds = command[3] & 0x01;
                sector = command[4];
                sectorSize = command[5];
                eot = command[6];
                dataLength = command[8];
                ableToTransfer = true;
                if (!(drives[drv].isMotorRunning())) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: read/write normal data -> drive motor of drive " + drv + " is not running.");
                    msr = FDC_CMD_BUSY;
                    ableToTransfer = false;
                }
                if (drives[drv].getDriveType() == FLOPPY_DRIVETYPE_NONE) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: read/write normal data -> incorrect drive type if drive " + drv + ".");
                    msr = FDC_CMD_BUSY;
                    ableToTransfer = false;
                }
                if (hds != ((command[1] >> 2) & 0x01)) {
                    logger.log(Level.WARNING, "[" + super.getType() + "] head number in command[1] doesn't match head field");
                    ableToTransfer = false;
                    statusRegister0 = 0x40 | (drives[drv].hds << 2) | drv;
                    statusRegister1 = 0x04;
                    statusRegister2 = 0x00;
                    enterResultPhase();
                }
                if (!drives[drv].containsFloppy()) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: read/write normal data -> floppy is not inserted in drive " + drv + ".");
                    msr = FDC_CMD_BUSY;
                    ableToTransfer = false;
                }
                if (sectorSize != 0x02) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: read/write normal data -> sector size (bytes per sector) not supported.");
                    ableToTransfer = false;
                }
                if (cylinder >= drives[drv].tracks) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: read/write normal data -> cylinder number exceeds maximum number of tracks.");
                    ableToTransfer = false;
                }
                if (sector > drives[drv].sectorsPerTrack) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: read/write normal data -> sector number (" + sector + ") exceeds sectors per track (" + drives[drv].sectorsPerTrack + ").");
                    drives[drv].cylinder = cylinder;
                    drives[drv].hds = hds;
                    drives[drv].sector = sector;
                    statusRegister0 = 0x40 | (drives[drv].hds << 2) | drv;
                    statusRegister1 = 0x04;
                    statusRegister2 = 0x00;
                    enterResultPhase();
                    return;
                }
                if (cylinder != drives[drv].cylinder) {
                    logger.log(Level.CONFIG, "[" + super.getType() + "]" + " CMD: read/write normal data -> requested cylinder differs from selected cylinder on drive. Will proceed.");
                    drives[drv].resetChangeline();
                }
                logicalSector = (cylinder * drives[drv].heads * drives[drv].sectorsPerTrack) + (hds * drives[drv].sectorsPerTrack) + (sector - 1);
                logger.log(Level.CONFIG, "[" + super.getType() + "]" + " Logical sectors calculated: " + logicalSector);
                if (logicalSector >= drives[drv].sectors) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: read/write normal data -> logical sectors exceeds total number of sectors on disk.");
                    ableToTransfer = false;
                }
                if (ableToTransfer) {
                    if (eot == 0) {
                        eot = drives[drv].sectorsPerTrack;
                    }
                    drives[drv].cylinder = cylinder;
                    drives[drv].hds = hds;
                    drives[drv].sector = sector;
                    drives[drv].eot = eot;
                    if ((command[0] & 0x4F) == 0x46) {
                        try {
                            drives[drv].readData(logicalSector * 512, 512, floppyBuffer);
                        } catch (StorageDeviceException e) {
                            logger.log(Level.WARNING, "[" + super.getType() + "]" + " " + e.getMessage());
                        }
                        msr = FDC_CMD_BUSY;
                        msr |= FDC_CMD_DIO;
                        sectorTime = 200000 / drives[drv].sectorsPerTrack;
                        motherboard.resetTimer(this, sectorTime);
                        motherboard.setTimerActiveState(this, true);
                    } else if ((command[0] & 0x7F) == 0x45) {
                        msr = FDC_CMD_BUSY;
                        msr &= ~FDC_CMD_DIO;
                        if (emu.isCpu32bit()) {
                            dma32.holdDREQ(FDC_DMA_CHANNEL & 3);
                        } else {
                            dma.setDMARequest(FDC_DMA_CHANNEL, true);
                        }
                    } else {
                        msr |= FDC_CMD_DIO;
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: unknown read/write command");
                    }
                } else {
                    logger.log(Level.SEVERE, "[" + super.getType() + "]" + " CMD: not able to transfer data");
                }
                break;
            case 0x07:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " CMD: recalibrate drive");
                drv = (command[1] & 0x03);
                dor &= 0xFC;
                dor |= drv;
                motherboard.resetTimer(this, calculateStepDelay(drv, 0));
                motherboard.setTimerActiveState(this, true);
                drives[drv].cylinder = 0;
                msr = (byte) (1 << drv);
                break;
            case 0x08:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " CMD: sense interrupt status");
                if (resetSenseInterrupt > 0) {
                    drv = 4 - resetSenseInterrupt;
                    statusRegister0 &= 0xF8;
                    statusRegister0 |= (drives[drv].hds << 2) | drv;
                    resetSenseInterrupt--;
                } else if (!pendingIRQ) {
                    statusRegister0 = 0x80;
                }
                this.enterResultPhase();
                break;
            case 0x4A:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " CMD: read ID");
                drv = command[1] & 0x03;
                drives[drv].hds = (command[1] >> 2) & 0x01;
                dor &= 0xFC;
                dor |= drv;
                if (drives[drv].isMotorRunning()) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: read ID -> drive motor is not running.");
                    msr = FDC_CMD_BUSY;
                    return;
                }
                if (drives[drv].getDriveType() == FLOPPY_DRIVETYPE_NONE) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: read ID -> incorrect drive type.");
                    msr = FDC_CMD_BUSY;
                    return;
                }
                if (!drives[drv].containsFloppy()) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: read ID -> floppy is not inserted.");
                    msr = FDC_CMD_BUSY;
                    return;
                }
                statusRegister0 = (drives[drv].hds << 2) | drv;
                sectorTime = 200000 / drives[drv].sectorsPerTrack;
                motherboard.resetTimer(this, sectorTime);
                motherboard.setTimerActiveState(this, true);
                msr = FDC_CMD_BUSY;
                this.enterResultPhase();
                break;
            case 0x4D:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " CMD: format track");
                drv = command[1] & 0x03;
                dor &= 0xFC;
                dor |= drv;
                if (drives[drv].isMotorRunning()) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: format track -> drive motor is not running.");
                    msr = FDC_CMD_BUSY;
                    return;
                }
                drives[drv].hds = (command[1] >> 2) & 0x01;
                if (drives[drv].getDriveType() == FLOPPY_DRIVETYPE_NONE) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: format track -> incorrect drive type.");
                    msr = FDC_CMD_BUSY;
                    return;
                }
                if (!drives[drv].containsFloppy()) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: format track -> floppy is not inserted.");
                    msr = FDC_CMD_BUSY;
                    return;
                }
                sectorSize = command[2];
                formatCount = command[3];
                formatFillbyte = command[5];
                if (sectorSize != 0x02) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: format track -> sector size (bytes per sector) not supported.");
                }
                if (formatCount != drives[drv].sectorsPerTrack) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: format track -> wrong number of sectors per track encountered.");
                }
                if (drives[drv].writeProtected) {
                    logger.log(Level.SEVERE, "[" + super.getType() + "]" + " CMD: format track -> floppy is write protected.");
                    statusRegister0 = 0x40 | (drives[drv].hds << 2) | drv;
                    statusRegister1 = 0x27;
                    statusRegister2 = 0x31;
                    this.enterResultPhase();
                    return;
                }
                formatCount = formatCount * 4;
                if (emu.isCpu32bit()) {
                    dma32.holdDREQ(FDC_DMA_CHANNEL & 3);
                } else {
                    dma.setDMARequest(FDC_DMA_CHANNEL, true);
                }
                msr = FDC_CMD_BUSY;
                break;
            case 0x0F:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " CMD: seek");
                drv = command[1] & 0x03;
                dor &= 0xFC;
                dor |= drv;
                drives[drv].hds = (command[1] >> 2) & 0x01;
                motherboard.resetTimer(this, calculateStepDelay(drv, command[2]));
                motherboard.setTimerActiveState(this, true);
                drives[drv].cylinder = command[2];
                msr = (byte) (1 << drv);
                break;
            case 0x0E:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " CMD: dump registers (EHD)");
                this.enterResultPhase();
                break;
            case 0x12:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " CMD: perpendicular mode (EHD)");
                perpMode = command[1];
                this.enterIdlePhase();
                break;
            case 0x13:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " CMD: configure (EHD)");
                config = command[2];
                preTrack = command[3];
                this.enterIdlePhase();
                break;
            default:
                logger.log(Level.WARNING, "[" + super.getType() + "]" + " Unsupported FDC command 0x" + command[0]);
        }
    }

    /**
     * Store result after execution phase
     */
    private void enterResultPhase() {
        int drv = dor & 0x03;
        resultIndex = 0;
        msr &= 0x0f;
        msr |= FDC_CMD_MRQ | FDC_CMD_DIO | FDC_CMD_BUSY;
        if ((statusRegister0 & 0xc0) == 0x80) {
            resultSize = 1;
            result[0] = (byte) statusRegister0;
            logger.log(Level.WARNING, "[" + super.getType() + "]" + " result phase: invalid command.");
            return;
        }
        switch(commandPending) {
            case 0x04:
                resultSize = 1;
                result[0] = (byte) statusRegister3;
                break;
            case 0x08:
                resultSize = 2;
                result[0] = (byte) statusRegister0;
                result[1] = (byte) drives[drv].cylinder;
                break;
            case 0x45:
            case 0x46:
            case 0x4A:
            case 0x4D:
            case 0x66:
            case 0xC5:
            case 0xC6:
            case 0xE6:
                resultSize = 7;
                result[0] = (byte) statusRegister0;
                result[1] = (byte) statusRegister1;
                result[2] = (byte) statusRegister2;
                result[3] = (byte) drives[drv].cylinder;
                result[4] = (byte) drives[drv].hds;
                result[5] = (byte) drives[drv].sector;
                result[6] = 2;
                this.setInterrupt();
                break;
            case 0x0E:
                resultSize = 10;
                for (int i = 0; i < 2; i++) {
                    result[i] = (byte) drives[i].cylinders;
                }
                result[2] = 0;
                result[3] = 0;
                result[4] = (byte) (((srt << 4) & 0xF0) | hut);
                result[5] = (byte) (((hlt << 1) & 0xFE) | nonDMA);
                result[6] = (byte) drives[drv].eot;
                result[7] = (byte) ((lock << 7) | (perpMode & 0x7f));
                result[8] = config;
                result[9] = preTrack;
                break;
            case 0x10:
                resultSize = 1;
                result[0] = (byte) 0x90;
                break;
            case 0x14:
            case 0x94:
                lock = (byte) (commandPending >> 7);
                resultSize = 1;
                result[0] = (byte) (lock << 4);
                break;
            default:
                logger.log(Level.WARNING, "[" + super.getType() + "]" + " CMD: no command match");
                break;
        }
        emu.statusChanged(Emulator.MODULE_FDC_TRANSFER_STOP);
    }

    /**
     * Reset parameters after result or execution phase
     */
    private void enterIdlePhase() {
        msr &= 0x0F;
        msr |= FDC_CMD_MRQ;
        commandComplete = true;
        commandIndex = 0;
        commandSize = 0;
        commandPending = 0;
        floppyBufferIndex = 0;
        logger.log(Level.INFO, "[" + super.getType() + "]" + " idle phase finished");
    }

    /**
     * Get byte from floppy buffer for DMA transfer This method is used for DMA
     * transfer a byte from FDC to memory
     *
     * @return byte current byte from floppy buffer
     */
    protected byte getDMAByte() {
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        ModuleRTC rtc = (ModuleRTC) super.getConnection(Module.Type.RTC);
        ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
        ModuleDMA dma = (ModuleDMA) super.getConnection(Module.Type.DMA);
        ModuleATA ata = (ModuleATA) super.getConnection(Module.Type.ATA);
        int drv, logicalSector, sectorTime;
        floppyBufferCurrentByte = floppyBuffer[floppyBufferIndex];
        floppyBufferIndex++;
        tc = dma.isTerminalCountReached();
        if ((floppyBufferIndex >= 512) || tc) {
            drv = dor & 0x03;
            if (floppyBufferIndex >= 512) {
                drives[drv].incrementSector();
                floppyBufferIndex = 0;
            }
            if (tc) {
                statusRegister0 = ((drives[drv].hds) << 2) | drv;
                statusRegister1 = 0;
                statusRegister2 = 0;
                dma.setDMARequest(FDC.FDC_DMA_CHANNEL, false);
                this.enterResultPhase();
            } else {
                logicalSector = (drives[drv].cylinder * drives[drv].heads * drives[drv].sectorsPerTrack) + (drives[drv].hds * drives[drv].sectorsPerTrack) + (drives[drv].sector - 1);
                try {
                    drives[drv].readData(logicalSector * 512, 512, floppyBuffer);
                } catch (StorageDeviceException e) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " " + e.getMessage());
                }
                dma.setDMARequest(FDC.FDC_DMA_CHANNEL, false);
                sectorTime = 200000 / drives[drv].sectorsPerTrack;
                logger.log(Level.CONFIG, motherboard.getCurrentInstructionNumber() + " " + "[" + super.getType() + "]" + " Activating floppy time to sector time of " + sectorTime + "(" + sectorTime * 5 + ")");
                motherboard.resetTimer(this, sectorTime);
                motherboard.setTimerActiveState(this, true);
            }
        }
        return floppyBufferCurrentByte;
    }

    /**
     * Set byte in floppy buffer for DMA transfer This method is used for DMA
     * transfer a byte from memory to FDC
     *
     * @param data
     */
    protected void setDMAByte(byte data) {
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        ModuleRTC rtc = (ModuleRTC) super.getConnection(Module.Type.RTC);
        ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
        ModuleDMA dma = (ModuleDMA) super.getConnection(Module.Type.DMA);
        ModuleATA ata = (ModuleATA) super.getConnection(Module.Type.ATA);
        int drv, logicalSector, sectorTime;
        tc = dma.isTerminalCountReached();
        drv = dor & 0x03;
        if (commandPending == 0x4D) {
            formatCount--;
            switch(3 - (formatCount & 0x03)) {
                case 0:
                    drives[drv].cylinder = data;
                    break;
                case 1:
                    if (data != drives[drv].hds) {
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " DMA transfer formatting track: head number does not match head field.");
                    }
                    break;
                case 2:
                    drives[drv].sector = data;
                    break;
                case 3:
                    if (data != 2) {
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " DMA transfer formatting track: sector size is not supported.");
                    } else {
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " DMA transfer formatting track: cyl=" + drives[drv].cylinder + ", head=" + drives[drv].hds + ", sector=" + drives[drv].sector);
                        for (int i = 0; i < 512; i++) {
                            floppyBuffer[i] = formatFillbyte;
                        }
                        logicalSector = (drives[drv].cylinder * drives[drv].heads * drives[drv].sectorsPerTrack) + (drives[drv].hds * drives[drv].sectorsPerTrack) + (drives[drv].sector - 1);
                        try {
                            drives[drv].writeData(logicalSector * 512, 512, floppyBuffer);
                        } catch (StorageDeviceException e) {
                            logger.log(Level.WARNING, "[" + super.getType() + "]" + " " + e.getMessage());
                        }
                        dma.setDMARequest(FDC.FDC_DMA_CHANNEL, false);
                        sectorTime = 200000 / drives[drv].sectorsPerTrack;
                        motherboard.resetTimer(this, sectorTime);
                        motherboard.setTimerActiveState(this, true);
                    }
                    break;
                default:
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " DMA transfer formatting track failed.");
                    break;
            }
        } else {
            floppyBuffer[floppyBufferIndex++] = data;
            if ((floppyBufferIndex >= 512) || (tc)) {
                logicalSector = (drives[drv].cylinder * drives[drv].heads * drives[drv].sectorsPerTrack) + (drives[drv].hds * drives[drv].sectorsPerTrack) + (drives[drv].sector - 1);
                if (drives[drv].writeProtected) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " DMA transfer to floppy failed: floppy is write protected.");
                    statusRegister0 = 0x40 | (drives[drv].hds << 2) | drv;
                    statusRegister1 = 0x27;
                    statusRegister2 = 0x31;
                    this.enterResultPhase();
                    return;
                }
                try {
                    drives[drv].writeData(logicalSector * 512, 512, floppyBuffer);
                    drives[drv].incrementSector();
                    floppyBufferIndex = 0;
                } catch (StorageDeviceException e) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " " + e.getMessage());
                }
                dma.setDMARequest(FDC.FDC_DMA_CHANNEL, false);
                sectorTime = 200000 / drives[drv].sectorsPerTrack;
                motherboard.resetTimer(this, sectorTime);
                motherboard.setTimerActiveState(this, true);
            }
        }
    }

    /**
     * Calculate the delay for timer This method makes an approximation of the
     * delay in the drive It does this based on the gap between current position
     * of head in cylinder and desired cylinder
     *
     * @param drive
     * @param newCylinder
     * @return -
     */
    protected int calculateStepDelay(int drive, int newCylinder) {
        int numSteps;
        int oneStepDelayTime;
        if (newCylinder == drives[drive].cylinder) {
            numSteps = 1;
        } else {
            numSteps = Math.abs(newCylinder - drives[drive].cylinder);
            drives[drive].resetChangeline();
        }
        oneStepDelayTime = ((srt ^ 0x0f) + 1) * 500000 / dataRates[dataRate];
        logger.log(Level.INFO, "[" + super.getType() + "]" + " Calculated step delay: " + numSteps * oneStepDelayTime);
        return (numSteps * oneStepDelayTime);
    }

    /**
     * Unregisters all registered devices (IRQ, timer, DMA)
     *
     * @return boolean true if succesfully, false otherwise
     */
    private boolean unregisterDevices() {
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        ModuleRTC rtc = (ModuleRTC) super.getConnection(Module.Type.RTC);
        ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
        ModuleDMA dma = (ModuleDMA) super.getConnection(Module.Type.DMA);
        ModuleATA ata = (ModuleATA) super.getConnection(Module.Type.ATA);
        boolean reslt = false;
        pic.clearIRQ(irqNumber);
        logger.log(Level.CONFIG, "[" + super.getType() + "]" + " IRQ unregister result: " + reslt);
        logger.log(Level.CONFIG, "[" + super.getType() + "]" + " Timer unregister result: " + reslt);
        logger.log(Level.CONFIG, "[" + super.getType() + "]" + " DMA unregister result: " + reslt);
        return reslt;
    }

    /**
     * @param nchan
     * @param pos
     * @param size
     * @return -
     */
    public int transferHandler(int nchan, int pos, int size) {
        final int SECTOR_LENGTH = 512;
        int dmaSize = Math.min(size, SECTOR_LENGTH);
        if (drives[drive] == null) {
            logger.log(Level.SEVERE, "[" + super.getType() + "]" + " no floppy in DMA transfer, aborting");
            return 0;
        }
        int startOffset;
        for (startOffset = floppyBufferIndex; startOffset < size; startOffset += SECTOR_LENGTH) {
            int relativeOffset = startOffset % SECTOR_LENGTH;
            if ((msr & FDC_CMD_DIO) == FDC_CMD_DIO) {
                dma32.writeMemory(nchan, floppyBuffer, relativeOffset, startOffset, dmaSize);
            } else {
                dma32.readMemory(nchan, floppyBuffer, relativeOffset, startOffset, dmaSize);
            }
            if (relativeOffset == 0) {
                try {
                    if ((msr & FDC_CMD_DIO) == FDC_CMD_DIO) {
                        drives[drive].incrementSector();
                        floppyBufferIndex = 0;
                        int logicalSector = (drives[drive].cylinder * drives[drive].heads * drives[drive].sectorsPerTrack) + (drives[drive].hds * drives[drive].sectorsPerTrack) + (drives[drive].sector - 1);
                        drives[drive].readData(logicalSector * 512, 512, floppyBuffer);
                    } else {
                        int logicalSector = (drives[drive].cylinder * drives[drive].heads * drives[drive].sectorsPerTrack) + (drives[drive].hds * drives[drive].sectorsPerTrack) + (drives[drive].sector - 1);
                        drives[drive].writeData(logicalSector * 512, 512, floppyBuffer);
                        drives[drive].incrementSector();
                        floppyBufferIndex = 0;
                    }
                } catch (StorageDeviceException e) {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " " + e.getMessage());
                }
            }
        }
        statusRegister0 = ((drives[drive].hds) << 2) | drive;
        statusRegister1 = 0;
        statusRegister2 = 0;
        dma32.releaseDREQ(FDC_DMA_CHANNEL & 3);
        this.enterResultPhase();
        return startOffset;
    }

    /**
     * @param component
     */
    public void acceptComponent(HardwareComponent component) {
        if ((component instanceof DMAController) && component.initialised()) {
            if (((DMAController) component).isFirst()) {
                if (FDC_DMA_CHANNEL != -1) {
                    dma32 = (DMAController) component;
                    dma32.registerChannel(FDC_DMA_CHANNEL & 3, this);
                }
            }
        }
    }
}
