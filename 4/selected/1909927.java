package nl.kbna.dioscuri.module.fdc;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.kbna.dioscuri.Emulator;
import nl.kbna.dioscuri.exception.ModuleException;
import nl.kbna.dioscuri.exception.ModuleUnknownPort;
import nl.kbna.dioscuri.exception.ModuleWriteOnlyPortException;
import nl.kbna.dioscuri.exception.StorageDeviceException;
import nl.kbna.dioscuri.module.Module;
import nl.kbna.dioscuri.module.ModuleDMA;
import nl.kbna.dioscuri.module.ModuleFDC;
import nl.kbna.dioscuri.module.ModuleATA;
import nl.kbna.dioscuri.module.ModuleMotherboard;
import nl.kbna.dioscuri.module.ModulePIC;
import nl.kbna.dioscuri.module.ModuleRTC;

/**
 * An implementation of a Floppy disk controller module.
 *  
 * @see Module
 * 
 * Metadata module
 * ********************************************
 * general.type                : fdc
 * general.name                : Floppy Disk Controller
 * general.architecture        : Von Neumann
 * general.description         : Implements a standard floppy disk controller for 3.5" floppies
 * general.creator             : Tessella Support Services, Koninklijke Bibliotheek, Nationaal Archief of the Netherlands
 * general.version             : 1.0
 * general.keywords            : floppy, disk, controller, 1.44, 2.88, 3.5 inch, SD, DD, HD
 * general.relations           : motherboard
 * general.yearOfIntroduction  : 1982
 * general.yearOfEnding        : 
 * general.ancestor            : 5.25 inch floppy disk
 * general.successor           : -
 * 
 * Issues:
 * - check all fixme statements
 * - sometimes MSR register is set to busy, but I am not sure if it is done the right way as it looses all previous bits:
 *                 // Data register not ready, drive busy
 *                MSR = (byte) (1 << drive);
 *                
 * Notes:
 * - Floppy disk controller is usually an 8272, 8272A, NEC765 (or compatible), or an 82072 or 82077AA for perpendicular recording at 2.88M.
 * - The FDC is only capable of reading from and writing to virtual floppy disks (images), not physical disks.
 * - The FDC does not perform a CRC (Cyclic Redundancy Check).
 * - FDC commands Scan low, scan low or equal, scan high or equal are not implemented.
 * - Datarates (speed of reading/writing) is not taken into account.
 * - Enhanced commands like lock and unlock are not fully implemented.
 * - Current FDC only works with DMA data transfer
 * 
 * Overview of FDC registers (ref: Port list, made by Ralf Brown: [http://mudlist.eorbit.net/~adam/pickey/ports.html])
 *  03F0  R   diskette controller status A (PS/2) (see #P173)
 *  03F0  R   diskette controller status A (PS/2 model 30) (see #P174)
 *  03F0  R   diskette EHD controller board jumper settings (82072AA) (see #P175)
 *  03F1  R   diskette controller status B (PS/2) (see #P176)
 *  03F1  R   diskette controller status B (PS/2 model 30) (see #P177)
 *  03F2   W  diskette controller DOR (Digital Output Register) (see #P178)
 *  03F3  ?W  tape drive register (on the 82077AA)
 *      bit 7-2  reserved, tri-state
 *      bit 1-0  tape select
 *          =00  none, drive 0 cannot be a tape drive.
 *          =01  drive1
 *          =10  drive2
 *          =11  drive3
 *  03F4  R   diskette controller main status register (see #P179)
 *      Note:   in non-DMA mode, all data transfers occur through
 *            PORT 03F5h and the status registers (bit 5 here
 *            indicates data read/write rather than than
 *            command/status read/write)
 *  03F4   W  diskette controller data rate select register (see #P180)
 *  03F5  R   diskette command/data register 0 (ST0) (see #P181)
 *      status register 1 (ST1) (see #P182)
 *      status register 2 (ST2) (see #P183)
 *      status register 3 (ST3) (see #P184)
 *  03F5   W  diskette command register.  The commands summarized here are
 *        mostly multibyte commands. This is for brief recognition only.
 *        (see #P187)
 *  03F6  --  reserved on FDC
 *  03F6  rW  FIXED disk controller data register (see #P185)
 *  03F7  RW  harddisk controller (see #P186)
 *  03F7  R   diskette controller DIR (Digital Input Register, PC/AT mode)
 *           bit 7 = 1 diskette change
 *           bit 6-0   tri-state on FDC
 *  03F7  R   diskette controller DIR (Digital Input Register, PS/2 mode)
 *           bit 7   = 1 diskette change
 *           bit 6-3 = 1
 *           bit 2       datarate select1
 *           bit 1       datarate select0
 *           bit 0   = 0 high density select (500Kb/s, 1Mb/s)
 *      conflict bit 0     FIXED DISK drive 0 select
 *  03F7  R   diskette controller DIR (Digital Input Register, PS/2 model 30)
 *           bit 7   = 0 diskette change
 *           bit 6-4 = 0
 *           bit 3       -DMA gate (value from DOR register)
 *           bit 2       NOPREC (value from CCR register)
 *           bit 1       datarate select1
 *           bit 0       datarate select0
 *      conflict bit 0     FIXED DISK drive 0 select
 *  03F7   W  configuration control register (PC/AT, PS/2)
 *           bit 7-2       reserved, tri-state
 *           bit 1-0 = 00  500 Kb/S mode (MFM)
 *               = 01  300 Kb/S mode (MFM)
 *               = 10  250 Kb/S mode (MFM)
 *               = 11  1   Mb/S mode (MFM) (on 82072/82077AA)
 *      conflict bit 0     FIXED DISK drive 0 select
 *  03F7   W  configuration control register (PS/2 model 30)
 *           bit 7-3       reserved, tri-state
 *           bit 2         NOPREC (has no function. set to 0 by hardreset)
 *           bit 1-0 = 00  500 Kb/S mode (MFM)
 *               = 01  300 Kb/S mode (MFM)
 *               = 10  250 Kb/S mode (MFM)
 *               = 11  1   Mb/S mode (MFM) (on 82072/82077AA)
 *      conflict bit 0     FIXED DISK drive 0 select 
 */
public class FDC extends ModuleFDC {

    private Emulator emu;

    private String[] moduleConnections = new String[] { "motherboard", "rtc", "pic", "dma", "ata" };

    private ModuleMotherboard motherboard;

    private ModuleRTC rtc;

    private ModulePIC pic;

    private ModuleDMA dma;

    private ModuleATA ata;

    private boolean isObserved;

    private boolean debugMode;

    private static Logger logger = Logger.getLogger("nl.kbna.dioscuri.module.fdc");

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

    public static final int MODULE_ID = 1;

    public static final String MODULE_TYPE = "fdc";

    public static final String MODULE_NAME = "Floppy Disk Controller";

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

    private static final int FDC_CMD_MRQ = 0x080;

    private static final int FDC_CMD_DIO = 0x040;

    private static final int FDC_CMD_NDMA = 0x020;

    private static final int FDC_CMD_BUSY = 0x010;

    private static final int FDC_CMD_ACTD = 0x008;

    private static final int FDC_CMD_ACTC = 0x004;

    private static final int FDC_CMD_ACTB = 0x002;

    private static final int FDC_CMD_ACTA = 0x001;

    private static final int FDC_DMA_CHANNEL = 2;

    /**
     * Class constructor
     * 
     */
    public FDC(Emulator owner) {
        emu = owner;
        isObserved = false;
        debugMode = false;
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
        msr = 0;
        tdr = 0;
        statusRegister0 = 0;
        statusRegister1 = 0;
        statusRegister2 = 0;
        statusRegister3 = 0;
        logger.log(Level.INFO, "[" + MODULE_TYPE + "] " + MODULE_NAME + " -> Module created successfully.");
    }

    /**
     * Returns the ID of the module
     * 
     * @return string containing the ID of module 
     * @see Module
     */
    public int getID() {
        return MODULE_ID;
    }

    /**
     * Returns the type of the module
     * 
     * @return string containing the type of module 
     * @see Module
     */
    public String getType() {
        return MODULE_TYPE;
    }

    /**
     * Returns the name of the module
     * 
     * @return string containing the name of module 
     * @see Module
     */
    public String getName() {
        return MODULE_NAME;
    }

    /**
     * Returns a String[] with all names of modules it needs to be connected to
     * 
     * @return String[] containing the names of modules, or null if no connections
     */
    public String[] getConnection() {
        return moduleConnections;
    }

    /**
     * Sets up a connection with another module
     * 
     * @param mod   Module that is to be connected to this class
     * 
     * @return true if connection has been established successfully, false otherwise
     * 
     * @see Module
     */
    public boolean setConnection(Module mod) {
        if (mod.getType().equalsIgnoreCase("motherboard")) {
            this.motherboard = (ModuleMotherboard) mod;
            return true;
        } else if (mod.getType().equalsIgnoreCase("rtc")) {
            this.rtc = (ModuleRTC) mod;
            return true;
        } else if (mod.getType().equalsIgnoreCase("pic")) {
            this.pic = (ModulePIC) mod;
            return true;
        } else if (mod.getType().equalsIgnoreCase("dma")) {
            this.dma = (ModuleDMA) mod;
            return true;
        } else if (mod.getType().equalsIgnoreCase("ata")) {
            this.ata = (ModuleATA) mod;
            return true;
        }
        return false;
    }

    /**
     * Checks if this module is connected to operate normally
     * 
     * @return true if this module is connected successfully, false otherwise
     */
    public boolean isConnected() {
        if (this.motherboard != null && this.rtc != null && this.pic != null && this.dma != null && this.ata != null) {
            return true;
        }
        return false;
    }

    /**
     * Reset all parameters of module
     * 
     * @return boolean true if module has been reset successfully, false otherwise
     */
    public boolean reset() {
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
            logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " IRQ number set to: " + irqNumber);
            pic.clearIRQ(irqNumber);
            pendingIRQ = false;
            resetSenseInterrupt = 0;
        } else {
            logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Request of IRQ number failed.");
        }
        if (motherboard.requestTimer(this, updateInterval, false) == true) {
            logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " Timer requested successfully.");
        } else {
            logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Failed to request a timer.");
        }
        dma8Handler = new DMA8Handler(this);
        if (dma.registerDMAChannel(FDC_DMA_CHANNEL, dma8Handler) == true) {
            logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " DMA channel registered to line: " + FDC_DMA_CHANNEL);
        } else {
            logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Failed to register DMA channel " + FDC_DMA_CHANNEL);
        }
        rtc.setCMOSRegister(0x14, (byte) (rtc.getCMOSRegister(0x14) | 0x01));
        dor = 0x0C;
        msr = 0;
        statusRegister0 = 0;
        statusRegister1 = 0;
        statusRegister2 = 0;
        statusRegister3 = 0;
        dataRate = 2;
        lock = 0;
        config = 0;
        preTrack = 0;
        perpMode = 0;
        for (int i = 0; i < drives.length; i++) {
            drives[i].reset();
        }
        this.enterIdlePhase();
        logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Module has been reset.");
        return true;
    }

    /**
     * Starts the module
     * @see Module
     */
    public void start() {
    }

    /**
     * Stops the module
     * @see Module
     */
    public void stop() {
        for (int i = 0; i < drives.length; i++) {
            if (drives[i] != null && drives[i].containsFloppy() == true) {
                if (this.ejectCarrier(i) == false) {
                    logger.log(Level.SEVERE, "[" + MODULE_TYPE + "] Drive " + i + ": eject floppy failed.");
                }
            }
        }
    }

    /**
     * Returns the status of observed toggle
     * 
     * @return state of observed toggle
     * 
     * @see Module
     */
    public boolean isObserved() {
        return isObserved;
    }

    /**
     * Sets the observed toggle
     * 
     * @param status
     * 
     * @see Module
     */
    public void setObserved(boolean status) {
        isObserved = status;
    }

    /**
     * Returns the status of the debug mode toggle
     * 
     * @return state of debug mode toggle
     * 
     * @see Module
     */
    public boolean getDebugMode() {
        return debugMode;
    }

    /**
     * Sets the debug mode toggle
     * 
     * @param status
     * 
     * @see Module
     */
    public void setDebugMode(boolean status) {
        debugMode = status;
    }

    /**
     * Returns data from this module
     *
     * @param Module requester, the requester of the data
     * @return byte[] with data
     * 
     * @see Module
     */
    public byte[] getData(Module requester) {
        return null;
    }

    /**
     * Set data for this module
     *
     * @param byte[] containing data
     * @param Module sender, the sender of the data
     * 
     * @return true if data is set successfully, false otherwise
     * 
     * @see Module
     */
    public boolean setData(byte[] data, Module sender) {
        return false;
    }

    /**
     * Set String[] data for this module
     * 
     * @param String[] data
     * @param Module sender, the sender of the data
     * 
     * @return boolean true is successful, false otherwise
     * 
     * @see Module
     */
    public boolean setData(String[] data, Module sender) {
        return false;
    }

    /**
     * Returns a dump of this module
     * 
     * @return string
     * 
     * @see Module
     */
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
     * Retrieve the interval between subsequent updates
     * 
     * @return int interval in microseconds
     */
    public int getUpdateInterval() {
        return updateInterval;
    }

    /**
     * Defines the interval between subsequent updates
     * 
     * @param int interval in microseconds
     */
    public void setUpdateInterval(int interval) {
        if (interval > 0) {
            updateInterval = interval;
        } else {
            updateInterval = 1000;
        }
        motherboard.resetTimer(this, updateInterval);
    }

    /**
     * Update device
     * 
     */
    public void update() {
        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " UPDATE IN PROGRESS");
        if (commandPending != 0x00) {
            drive = dor & 0x03;
            switch(commandPending) {
                case 0x07:
                    statusRegister0 = 0x20 | drive;
                    if ((drives[drive].getDriveType() == FLOPPY_DRIVETYPE_NONE) || (drives[drive].isMotorRunning() == false)) {
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
                        dma.setDMARequest(FDC_DMA_CHANNEL, true);
                    }
                    break;
                case 0x46:
                case 0x66:
                case 0xC6:
                case 0xE6:
                    dma.setDMARequest(FDC_DMA_CHANNEL, true);
                    break;
                case 0x4D:
                    if ((formatCount == 0) || tc) {
                        formatCount = 0;
                        statusRegister0 = (drives[drive].hds << 2) | drive;
                        this.enterResultPhase();
                    } else {
                        dma.setDMARequest(FDC_DMA_CHANNEL, true);
                    }
                    break;
                case 0xFE:
                    this.reset();
                    commandPending = 0;
                    statusRegister0 = 0xC0;
                    this.setInterrupt();
                    resetSenseInterrupt = 4;
                    break;
                default:
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: unknown command during update.");
            }
        }
    }

    /**
     * Return a byte from I/O address space at given port
     * 
     * @param int portAddress containing the address of the I/O port
     * 
     * @return byte containing the data at given I/O address port
     * @throws ModuleException, ModuleUnknownPort, ModuleWriteOnlyPortException
     */
    public byte getIOPortByte(int portAddress) throws ModuleException, ModuleUnknownPort, ModuleWriteOnlyPortException {
        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " IN command (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + " received");
        byte value = 0;
        switch(portAddress) {
            case 0x03F0:
            case 0x03F1:
                logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Reading ports 0x3F0 and 0x3F1 not implemented");
                break;
            case 0x03F2:
                logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " Is reading allowed of port 0x3F2? Returned DOR");
                value = dor;
                break;
            case 0x03F3:
                int drive = dor & 0x03;
                if (drives[drive].containsFloppy()) {
                    switch(drives[drive].getFloppyType()) {
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
                    logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Port 0x3F5: no results to read");
                    msr = 0;
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
                logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " IN (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + ": reserved port.");
                value = ata.getIOPortByte(portAddress);
                break;
            case 0x03F7:
                value = ata.getIOPortByte(portAddress);
                value &= 0x7f;
                drive = dor & 0x03;
                if ((dor & (1 << (drive + 4))) == 1) {
                    if (drives[drive] != null) {
                        value |= (drives[drive].dir & 0x80);
                    } else {
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Non-existing drive requested at port " + Integer.toHexString(portAddress).toUpperCase());
                    }
                }
                break;
            default:
                logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Unknown port address encountered: " + Integer.toHexString(portAddress).toUpperCase());
                break;
        }
        return value;
    }

    /**
     * Set a byte in I/O address space at given port
     * 
     * @param int portAddress containing the address of the I/O port
     * @param byte value
     * 
     * @throws ModuleException, ModuleUnknownPort, ModuleWriteOnlyPortException
     */
    public void setIOPortByte(int portAddress, byte value) throws ModuleException, ModuleUnknownPort {
        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " OUT (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + ": 0x" + Integer.toHexString(((int) value) & 0xFF).toUpperCase());
        switch(portAddress) {
            case 0x03F0:
            case 0x03F1:
                logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Port address is read only and cannot be written to: " + Integer.toHexString(portAddress).toUpperCase());
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
                                logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Unknown drive selected at port " + Integer.toHexString(portAddress).toUpperCase());
                                break;
                        }
                    } else {
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Non-existing drive selected at port " + Integer.toHexString(portAddress).toUpperCase());
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
                    msr = 0;
                    commandPending = 0xFE;
                }
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " OUT (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + ", DMA/IRQ=" + dmaAndInterruptEnabled + ", FDC=" + fdcEnabled + ", drive=" + drive + ", motorRunning=" + drives[drive].isMotorRunning());
                break;
            case 0x03F4:
                dataRate = value & 0x03;
                if ((value & 0x80) > 0) {
                    msr = 0;
                    commandPending = 0xFE;
                    motherboard.resetTimer(this, updateInterval);
                    motherboard.setTimerActiveState(this, true);
                }
                if ((value & 0x7C) > 0) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " OUT (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + ": drive is busy, but in non-DMA mode which is not supported.");
                }
                break;
            case 0x03F5:
                if (commandComplete) {
                    if (commandPending != 0) {
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " new command received while old command is still pending.");
                    }
                    command[0] = value;
                    commandComplete = false;
                    commandIndex = 1;
                    msr &= 0x0f;
                    msr |= FDC_CMD_MRQ | FDC_CMD_BUSY;
                    int command = value & 0xFF;
                    switch(command) {
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
                            commandPending = command;
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
                            logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " OUT (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + ": invalid FDC command.");
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
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " OUT (byte) to port " + Integer.toHexString(portAddress).toUpperCase() + ": reserved port.");
                ata.setIOPortByte(portAddress, value);
                break;
            case 0x03F7:
                dataRate = value & 0x03;
                switch(dataRate) {
                    case 0:
                        logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " Datarate is set to 500 Kbps");
                        break;
                    case 1:
                        logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " Datarate is set to 300 Kbps");
                        break;
                    case 2:
                        logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " Datarate is set to 250 Kbps");
                        break;
                    case 3:
                        logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " Datarate is set to 1 Mbps");
                        break;
                }
                break;
            default:
                logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Unknown port address encountered: " + Integer.toHexString(portAddress).toUpperCase());
                break;
        }
        return;
    }

    public byte[] getIOPortWord(int portAddress) throws ModuleException, ModuleWriteOnlyPortException {
        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " IN command (word) to port " + Integer.toHexString(portAddress).toUpperCase() + " received");
        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Returned default value 0xFFFF");
        return new byte[] { (byte) 0x0FF, (byte) 0x0FF };
    }

    public void setIOPortWord(int portAddress, byte[] dataWord) throws ModuleException {
        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " OUT command (word) to port " + Integer.toHexString(portAddress).toUpperCase() + " received. No action taken.");
        return;
    }

    public byte[] getIOPortDoubleWord(int portAddress) throws ModuleException, ModuleWriteOnlyPortException {
        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " IN command (double word) to port " + Integer.toHexString(portAddress).toUpperCase() + " received");
        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Returned default value 0xFFFFFFFF");
        return new byte[] { (byte) 0x0FF, (byte) 0x0FF, (byte) 0x0FF, (byte) 0x0FF };
    }

    public void setIOPortDoubleWord(int portAddress, byte[] dataDoubleWord) throws ModuleException {
        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " OUT command (double word) to port " + Integer.toHexString(portAddress).toUpperCase() + " received. No action taken.");
        return;
    }

    /**
     * Raise interrupt signal
     * 
     */
    protected void setInterrupt() {
        pic.setIRQ(irqNumber);
        pendingIRQ = true;
        resetSenseInterrupt = 0;
    }

    /**
     * Clear interrupt signal
     * 
     */
    protected void clearInterrupt() {
        if (pendingIRQ == true) {
            pic.clearIRQ(irqNumber);
            pendingIRQ = false;
        }
    }

    /**
     * Defines the total number of available drives
     * Note: total number may not exceed 4, but must be more than 0
     * 
     * @param int total number of drives
     * @return boolean true if drives set successfully, false otherwise
     */
    public boolean setNumberOfDrives(int totalDrives) {
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
     * Inserts a new carrier into a selected drive
     * 
     * @param String drive to which carrier has to be inserted
     * @param byte carrierType that defines the type of the carrier
     * @param File containing the disk image raw bytes of the carrier
     * @param boolean write protected
     * 
     * @return boolean true if carrier is inserted successfully, false otherwise
     */
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
     * Ejects a carrier (if any) from a selected drive
     * 
     * @param String drive of which carrier has to be ejected
     * 
     * @return boolean true if carrier is ejected successfully, false otherwise
     */
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
     * Inserts a new carrier into a selected drive
     * 
     * @param int driveIndex to which carrier has to be inserted
     * @param byte carrierType that defines the type of the carrier
     * @param File containing the disk image raw bytes of the carrier
     * @param boolean write protected
     * 
     * @return boolean true if carrier is inserted successfully, false otherwise
     */
    public boolean insertCarrier(int driveIndex, byte carrierType, File imageFile, boolean writeProtected) {
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
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Drive " + driveLetter + " set to reserved CMOS floppy drive type 6");
                        break;
                    case FLOPPY_DISKTYPE_180K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0x0f) | 0x70));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525DD);
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Drive " + driveLetter + " set to reserved CMOS floppy drive type 7");
                        break;
                    case FLOPPY_DISKTYPE_320K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0x0f) | 0x80));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525DD);
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Drive " + driveLetter + " set to reserved CMOS floppy drive type 8");
                        break;
                    default:
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Unsupported floppy drive type.");
                        break;
                }
                try {
                    drives[driveIndex].insertFloppy(carrierType, imageFile, writeProtected);
                    logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Floppy \"" + imageFile.getName() + "\" is inserted in drive " + driveIndex);
                    return true;
                } catch (StorageDeviceException e) {
                    logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " Error: " + e.getMessage());
                }
            } else {
                logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " Drive " + driveLetter + " does not exist or already contains a floppy. Eject floppy first!");
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
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Drive " + driveLetter + " set to reserved CMOS floppy drive type 6");
                        break;
                    case FLOPPY_DISKTYPE_180K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0xf0) | 0x07));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525DD);
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Drive " + driveLetter + " set to reserved CMOS floppy drive type 7");
                        break;
                    case FLOPPY_DISKTYPE_320K:
                        rtc.setCMOSRegister(0x10, (byte) ((rtc.getCMOSRegister(0x10) & 0xf0) | 0x08));
                        drives[driveIndex].setDriveType(FLOPPY_DRIVETYPE_525DD);
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Drive " + driveLetter + " set to reserved CMOS floppy drive type 8");
                        break;
                    default:
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Unsupported floppy drive type.");
                        break;
                }
                try {
                    drives[driveIndex].insertFloppy(carrierType, imageFile, writeProtected);
                    logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Floppy \"" + imageFile.getName() + "\" is inserted in drive " + driveIndex);
                    return true;
                } catch (StorageDeviceException e) {
                    logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " Error: " + e.getMessage());
                }
            } else {
                logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " Drive " + driveLetter + " does not exist or already contains a floppy. Eject floppy first!");
            }
        } else {
            logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " Can not insert floppy because additional drives are not implemented.");
        }
        return false;
    }

    /**
     * Ejects a carrier (if any) from a selected drive
     * 
     * @param int driveIndex of which carrier has to be ejected
     * 
     * @return boolean true if carrier is ejected successfully, false otherwise
     */
    public boolean ejectCarrier(int driveIndex) {
        try {
            if (driveIndex != -1) {
                boolean writeProtected = drives[driveIndex].writeProtected;
                drives[driveIndex].ejectFloppy();
                logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Floppy is ejected from drive " + drive + ".");
                if (writeProtected == false) {
                    logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Floppy data is stored to image file.");
                }
                return true;
            } else {
                logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " Can not eject floppy because drive is not recognized.");
            }
        } catch (StorageDeviceException e) {
            logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + e.getMessage());
            return false;
        }
        return false;
    }

    /**
     * Execute command of FDC
     * Note: assumed is that all bytes of the command are fetched.
     * After execution of the command, the FDC will automatically enter the result or idle phase.
     * 
     */
    private void executeCommand() {
        int drive, hds, cylinder, sector, eot;
        int sectorSize, sectorTime, logicalSector, dataLength;
        boolean ableToTransfer;
        commandPending = command[0] & 0xFF;
        switch(commandPending) {
            case 0x03:
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " CMD: specify");
                srt = (byte) ((command[1] >> 4) & 0x0F);
                hut = (byte) (command[1] & 0x0F);
                hlt = (byte) ((command[2] >> 1) & 0x7F);
                nonDMA = (byte) (command[2] & 0x01);
                this.enterIdlePhase();
                break;
            case 0x04:
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " CMD: sense drive status");
                drive = (command[1] & 0x03);
                drives[drive].hds = (command[1] >> 2) & 0x01;
                statusRegister3 = (byte) (0x28 | (drives[drive].hds << 2) | drive);
                statusRegister3 |= (drives[drive].writeProtected ? 0x40 : 0x00);
                if (drives[drive].cylinder == 0) {
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
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " CMD: Read/write data");
                emu.statusChanged(Emulator.MODULE_FDC_TRANSFER_START);
                if ((dor & 0x08) == 0) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: read/write normal data -> DMA is disabled");
                }
                drive = command[1] & 0x03;
                dor &= 0xFC;
                dor |= drive;
                drives[drive].multiTrack = (((command[0] >> 7) & 0x01) == 0x01 ? true : false);
                cylinder = command[2];
                hds = command[3] & 0x01;
                sector = command[4];
                sectorSize = command[5];
                eot = command[6];
                dataLength = command[8];
                ableToTransfer = true;
                if (!(drives[drive].isMotorRunning())) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: read/write normal data -> drive motor of drive " + drive + " is not running.");
                    msr = FDC_CMD_BUSY;
                    ableToTransfer = false;
                }
                if (hds != ((command[1] >> 2) & 0x01)) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "] head number in command[1] doesn't match head field");
                    ableToTransfer = false;
                    statusRegister0 = 0x40 | (drives[drive].hds << 2) | drive;
                    statusRegister1 = 0x04;
                    statusRegister2 = 0x00;
                    enterResultPhase();
                }
                if (drives[drive].getDriveType() == FLOPPY_DRIVETYPE_NONE) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: read/write normal data -> incorrect drive type if drive " + drive + ".");
                    msr = FDC_CMD_BUSY;
                    ableToTransfer = false;
                }
                if (!drives[drive].containsFloppy()) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: read/write normal data -> floppy is not inserted in drive " + drive + ".");
                    msr = FDC_CMD_BUSY;
                    ableToTransfer = false;
                }
                if (sectorSize != 0x02) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: read/write normal data -> sector size (bytes per sector) not supported.");
                    ableToTransfer = false;
                }
                if (cylinder != drives[drive].cylinder) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: read/write normal data -> requested cylinder differs from selected cylinder on drive. Will proceed.");
                    drives[drive].resetChangeline();
                }
                if (cylinder >= drives[drive].tracks) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: read/write normal data -> cylinder number exceeds maximum number of tracks.");
                    ableToTransfer = false;
                }
                if (sector > drives[drive].sectorsPerTrack) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: read/write normal data -> sector number exceeds sectors per track.");
                    ableToTransfer = false;
                }
                logicalSector = (cylinder * drives[drive].heads * drives[drive].sectorsPerTrack) + (hds * drives[drive].sectorsPerTrack) + (sector - 1);
                if (logicalSector >= drives[drive].sectors) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: read/write normal data -> logical sectors exceeds total number of sectors on disk.");
                    ableToTransfer = false;
                }
                if (ableToTransfer == true) {
                    if (eot == 0) {
                        eot = drives[drive].sectorsPerTrack;
                    }
                    drives[drive].cylinder = cylinder;
                    drives[drive].hds = hds;
                    drives[drive].sector = sector;
                    drives[drive].eot = eot;
                    if ((command[0] & 0x4F) == 0x46) {
                        try {
                            drives[drive].readData(logicalSector * 512, 512, floppyBuffer);
                        } catch (StorageDeviceException e) {
                            logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " " + e.getMessage());
                        }
                        msr = FDC_CMD_BUSY;
                        sectorTime = 200000 / drives[drive].sectorsPerTrack;
                        motherboard.resetTimer(this, sectorTime);
                        motherboard.setTimerActiveState(this, true);
                    } else if ((command[0] & 0x7F) == 0x45) {
                        msr = FDC_CMD_BUSY;
                        dma.setDMARequest(FDC_DMA_CHANNEL, true);
                    } else {
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: unknown read/write command");
                    }
                } else {
                    logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " CMD: not able to transfer data");
                }
                break;
            case 0x07:
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " CMD: recalibrate drive");
                drive = (command[1] & 0x03);
                dor &= 0xFC;
                dor |= drive;
                motherboard.resetTimer(this, calculateStepDelay(drive, 0));
                motherboard.setTimerActiveState(this, true);
                drives[drive].cylinder = 0;
                msr = (byte) (1 << drive);
                break;
            case 0x08:
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " CMD: sense interrupt status");
                if (resetSenseInterrupt > 0) {
                    drive = 4 - resetSenseInterrupt;
                    statusRegister0 &= 0xF8;
                    statusRegister0 |= (drives[drive].hds << 2) | drive;
                    resetSenseInterrupt--;
                } else if (!pendingIRQ) {
                    statusRegister0 = 0x80;
                }
                this.enterResultPhase();
                break;
            case 0x4A:
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " CMD: read ID");
                drive = command[1] & 0x03;
                drives[drive].hds = (command[1] >> 2) & 0x01;
                dor &= 0xFC;
                dor |= drive;
                if (drives[drive].isMotorRunning()) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: read ID -> drive motor is not running.");
                    msr = FDC_CMD_BUSY;
                    return;
                }
                if (drives[drive].getDriveType() == FLOPPY_DRIVETYPE_NONE) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: read ID -> incorrect drive type.");
                    msr = FDC_CMD_BUSY;
                    return;
                }
                if (!drives[drive].containsFloppy()) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: read ID -> floppy is not inserted.");
                    msr = FDC_CMD_BUSY;
                    return;
                }
                statusRegister0 = (drives[drive].hds << 2) | drive;
                sectorTime = 200000 / drives[drive].sectorsPerTrack;
                motherboard.resetTimer(this, sectorTime);
                motherboard.setTimerActiveState(this, true);
                msr = FDC_CMD_BUSY;
                this.enterResultPhase();
                break;
            case 0x4D:
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " CMD: format track");
                drive = command[1] & 0x03;
                dor &= 0xFC;
                dor |= drive;
                if (drives[drive].isMotorRunning()) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: format track -> drive motor is not running.");
                    msr = FDC_CMD_BUSY;
                    return;
                }
                drives[drive].hds = (command[1] >> 2) & 0x01;
                if (drives[drive].getDriveType() == FLOPPY_DRIVETYPE_NONE) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: format track -> incorrect drive type.");
                    msr = FDC_CMD_BUSY;
                    return;
                }
                if (!drives[drive].containsFloppy()) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: format track -> floppy is not inserted.");
                    msr = FDC_CMD_BUSY;
                    return;
                }
                sectorSize = command[2];
                formatCount = command[3];
                formatFillbyte = command[5];
                if (sectorSize != 0x02) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: format track -> sector size (bytes per sector) not supported.");
                }
                if (formatCount != drives[drive].sectorsPerTrack) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: format track -> wrong number of sectors per track encountered.");
                }
                if (drives[drive].writeProtected) {
                    logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " CMD: format track -> floppy is write protected.");
                    statusRegister0 = 0x40 | (drives[drive].hds << 2) | drive;
                    statusRegister1 = 0x27;
                    statusRegister2 = 0x31;
                    this.enterResultPhase();
                    return;
                }
                formatCount = formatCount * 4;
                dma.setDMARequest(FDC.FDC_DMA_CHANNEL, true);
                msr = FDC_CMD_BUSY;
                break;
            case 0x0F:
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " CMD: seek");
                drive = command[1] & 0x03;
                dor &= 0xFC;
                dor |= drive;
                drives[drive].hds = (command[1] >> 2) & 0x01;
                motherboard.resetTimer(this, calculateStepDelay(drive, command[2]));
                motherboard.setTimerActiveState(this, true);
                drives[drive].cylinder = command[2];
                msr = (byte) (1 << drive);
                break;
            case 0x0E:
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " CMD: dump registers (EHD)");
                this.enterResultPhase();
                break;
            case 0x12:
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " CMD: perpendicular mode (EHD)");
                perpMode = command[1];
                this.enterIdlePhase();
                break;
            case 0x13:
                logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " CMD: configure (EHD)");
                config = command[2];
                preTrack = command[3];
                this.enterIdlePhase();
                break;
            default:
                logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " Unsupported FDC command 0x" + command[0]);
        }
    }

    /**
     * Store result after execution phase
     * 
     */
    private void enterResultPhase() {
        int drive;
        drive = dor & 0x03;
        resultIndex = 0;
        msr &= 0x0f;
        msr |= FDC_CMD_MRQ | FDC_CMD_DIO | FDC_CMD_BUSY;
        if ((statusRegister0 & 0xc0) == 0x80) {
            resultSize = 1;
            result[0] = (byte) statusRegister0;
            logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " result phase: invalid command.");
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
                result[1] = (byte) drives[drive].cylinder;
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
                result[3] = (byte) drives[drive].cylinder;
                result[4] = (byte) drives[drive].hds;
                result[5] = (byte) drives[drive].sector;
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
                result[6] = (byte) drives[drive].eot;
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
                logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " CMD: no command match");
                break;
        }
        emu.statusChanged(Emulator.MODULE_FDC_TRANSFER_STOP);
    }

    /**
     * Reset parameters after result or execution phase
     * 
     */
    private void enterIdlePhase() {
        msr &= 0x0F;
        msr |= FDC_CMD_MRQ;
        commandComplete = true;
        commandIndex = 0;
        commandSize = 0;
        commandPending = 0;
        floppyBufferIndex = 0;
        logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " idle phase finished");
    }

    /**
     * Get byte from floppy buffer for DMA transfer
     * This method is used for DMA transfer a byte from FDC to memory
     * 
     * @return byte current byte from floppy buffer
     */
    protected byte getDMAByte() {
        int drive, logicalSector, sectorTime;
        floppyBufferCurrentByte = floppyBuffer[floppyBufferIndex];
        floppyBufferIndex++;
        tc = dma.isTerminalCountReached();
        if ((floppyBufferIndex >= 512) || tc == true) {
            drive = dor & 0x03;
            if (floppyBufferIndex >= 512) {
                drives[drive].incrementSector();
                floppyBufferIndex = 0;
            }
            if (tc == true) {
                statusRegister0 = ((drives[drive].hds) << 2) | drive;
                statusRegister1 = 0;
                statusRegister2 = 0;
                dma.setDMARequest(FDC.FDC_DMA_CHANNEL, false);
                this.enterResultPhase();
            } else {
                logicalSector = (drives[drive].cylinder * drives[drive].heads * drives[drive].sectorsPerTrack) + (drives[drive].hds * drives[drive].sectorsPerTrack) + (drives[drive].sector - 1);
                try {
                    drives[drive].readData(logicalSector * 512, 512, floppyBuffer);
                } catch (StorageDeviceException e) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " " + e.getMessage());
                }
                dma.setDMARequest(FDC.FDC_DMA_CHANNEL, false);
                sectorTime = 200000 / drives[drive].sectorsPerTrack;
                motherboard.resetTimer(this, sectorTime);
                motherboard.setTimerActiveState(this, true);
            }
        }
        return floppyBufferCurrentByte;
    }

    /**
     * Set byte in floppy buffer for DMA transfer
     * This method is used for DMA transfer a byte from memory to FDC
     * 
     * @param byte data to be stored in floppy buffer
     */
    protected void setDMAByte(byte data) {
        int drive, logicalSector, sectorTime;
        tc = dma.isTerminalCountReached();
        drive = dor & 0x03;
        if (commandPending == 0x4D) {
            formatCount--;
            switch(3 - (formatCount & 0x03)) {
                case 0:
                    drives[drive].cylinder = data;
                    break;
                case 1:
                    if (data != drives[drive].hds) {
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " DMA transfer formatting track: head number does not match head field.");
                    }
                    break;
                case 2:
                    drives[drive].sector = data;
                    break;
                case 3:
                    if (data != 2) {
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " DMA transfer formatting track: sector size is not supported.");
                    } else {
                        logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " DMA transfer formatting track: cyl=" + drives[drive].cylinder + ", head=" + drives[drive].hds + ", sector=" + drives[drive].sector);
                        for (int i = 0; i < 512; i++) {
                            floppyBuffer[i] = formatFillbyte;
                        }
                        logicalSector = (drives[drive].cylinder * drives[drive].heads * drives[drive].sectorsPerTrack) + (drives[drive].hds * drives[drive].sectorsPerTrack) + (drives[drive].sector - 1);
                        try {
                            drives[drive].writeData(logicalSector * 512, 512, floppyBuffer);
                        } catch (StorageDeviceException e) {
                            logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " " + e.getMessage());
                        }
                        dma.setDMARequest(FDC.FDC_DMA_CHANNEL, false);
                        sectorTime = 200000 / drives[drive].sectorsPerTrack;
                        motherboard.resetTimer(this, sectorTime);
                        motherboard.setTimerActiveState(this, true);
                    }
                    break;
                default:
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " DMA transfer formatting track failed.");
                    break;
            }
        } else {
            floppyBuffer[floppyBufferIndex++] = data;
            if ((floppyBufferIndex >= 512) || (tc == true)) {
                logicalSector = (drives[drive].cylinder * drives[drive].heads * drives[drive].sectorsPerTrack) + (drives[drive].hds * drives[drive].sectorsPerTrack) + (drives[drive].sector - 1);
                if (drives[drive].writeProtected == true) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " DMA transfer to floppy failed: floppy is write protected.");
                    statusRegister0 = 0x40 | (drives[drive].hds << 2) | drive;
                    statusRegister1 = 0x27;
                    statusRegister2 = 0x31;
                    this.enterResultPhase();
                    return;
                }
                try {
                    drives[drive].writeData(logicalSector * 512, 512, floppyBuffer);
                    drives[drive].incrementSector();
                    floppyBufferIndex = 0;
                } catch (StorageDeviceException e) {
                    logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " " + e.getMessage());
                }
                dma.setDMARequest(FDC.FDC_DMA_CHANNEL, false);
                sectorTime = 200000 / drives[drive].sectorsPerTrack;
                motherboard.resetTimer(this, sectorTime);
                motherboard.setTimerActiveState(this, true);
            }
        }
    }

    /**
     * Calculate the delay for timer
     * This method makes an approximation of the delay in the drive
     * It does this based on the gap between current position of head in cylinder and desired cylinder
     * 
     * @param int drive
     * @param int desired cylinder to go to
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
        logger.log(Level.FINE, "[" + MODULE_TYPE + "]" + " Calculated step delay: " + numSteps * oneStepDelayTime);
        return (numSteps * oneStepDelayTime);
    }
}
