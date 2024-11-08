package dioscuri;

import dioscuri.config.ConfigController;
import dioscuri.config.Emulator.Architecture.Modules.Ata.Harddiskdrive;
import dioscuri.config.Emulator.Architecture.Modules.Bios;
import dioscuri.config.Emulator.Architecture.Modules.Bios.Bootdrives;
import dioscuri.config.Emulator.Architecture.Modules.Fdc.Floppy;
import dioscuri.exception.ModuleException;
import dioscuri.interfaces.Module;
import dioscuri.interfaces.Updateable;
import dioscuri.module.*;
import dioscuri.module.ata.ATA;
import dioscuri.module.ata.ATAConstants;
import dioscuri.module.ata.ATATranslationType;
import dioscuri.module.bios.BIOS;
import dioscuri.module.clock.Clock;
import dioscuri.module.cpu.CPU;
import dioscuri.module.cpu32.*;
import dioscuri.module.dma.DMA;
import dioscuri.module.fdc.FDC;
import dioscuri.module.keyboard.Keyboard;
import dioscuri.module.memory.DynamicAllocationMemory;
import dioscuri.module.memory.Memory;
import dioscuri.module.motherboard.DeviceDummy;
import dioscuri.module.motherboard.Motherboard;
import dioscuri.module.mouse.Mouse;
import dioscuri.module.parallelport.ParallelPort;
import dioscuri.module.pic.PIC;
import dioscuri.module.pit.PIT;
import dioscuri.module.rtc.RTC;
import dioscuri.module.screen.Screen;
import dioscuri.module.serialport.SerialPort;
import dioscuri.module.video.Video;
import dioscuri.util.Utilities;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Top class owning all classes of the emulator. Entry point
 */
public class Emulator implements Runnable {

    private Modules modules;

    private ArrayList<HardwareComponent> hwComponents;

    private IO io;

    private GUI gui;

    protected dioscuri.config.Emulator emuConfig;

    protected dioscuri.config.Emulator.Architecture.Modules moduleConfig;

    private boolean isAlive;

    private boolean coldStart;

    private boolean resetBusy;

    private boolean cpu32bit;

    private boolean dynamicMem;

    private static final Logger logger = Logger.getLogger(Emulator.class.getName());

    protected static final int CMD_START = 0x00;

    protected static final int CMD_STOP = 0x01;

    protected static final int CMD_RESET = 0x02;

    protected static final int CMD_DEBUG = 0x04;

    protected static final int CMD_LOGGING = 0x05;

    protected static final int CMD_OBSERVE = 0x06;

    protected static final int CMD_LOAD_MODULES = 0x07;

    protected static final int CMD_LOAD_DATA = 0x08;

    protected static final int CMD_LOGTOFILE = 0x09;

    protected static final int CMD_DEBUG_HELP = 0x03;

    protected static final int CMD_DEBUG_STEP = 0x10;

    protected static final int CMD_DEBUG_DUMP = 0x11;

    protected static final int CMD_DEBUG_ENTER = 0x12;

    protected static final int CMD_DEBUG_STOP = 0x13;

    protected static final int CMD_DEBUG_SHOWREG = 0x14;

    protected static final int CMD_DEBUG_MEM_DUMP = 0x15;

    protected static final int CMD_MISMATCH = 0xFF;

    public static final int MODULE_FDC_TRANSFER_START = 0;

    public static final int MODULE_FDC_TRANSFER_STOP = 1;

    public static final int MODULE_ATA_HD1_TRANSFER_START = 2;

    public static final int MODULE_ATA_HD1_TRANSFER_STOP = 3;

    public static final int MODULE_KEYBOARD_NUMLOCK_ON = 4;

    public static final int MODULE_KEYBOARD_NUMLOCK_OFF = 5;

    public static final int MODULE_KEYBOARD_CAPSLOCK_ON = 6;

    public static final int MODULE_KEYBOARD_CAPSLOCK_OFF = 7;

    public static final int MODULE_KEYBOARD_SCROLLLOCK_ON = 8;

    public static final int MODULE_KEYBOARD_SCROLLLOCK_OFF = 9;

    public static final int MODULE_ATA_HD2_TRANSFER_START = 10;

    public static final int MODULE_ATA_HD2_TRANSFER_STOP = 11;

    /**
     * Class constructor
     *
     * @param owner graphical user interface (owner of emulation process)
     */
    public Emulator(GUI owner) {
        this.gui = owner;
        modules = null;
        hwComponents = new ArrayList<HardwareComponent>();
        io = new IO();
        isAlive = true;
        coldStart = true;
        resetBusy = false;
        dynamicMem = false;
    }

    public void run() {
        int instr = 0;
        int total = 0;
        int loop = 0;
        while (isAlive) {
            try {
                File configFile = new File(Utilities.resolvePathAsString(gui.getConfigFilePath()));
                if (!configFile.exists() || !configFile.canRead()) {
                    logger.log(Level.WARNING, "[emu] No local config file accessible, using read-only jar settings");
                    InputStream fallBack = GUI.class.getResourceAsStream(gui.getConfigFilePath());
                    emuConfig = ConfigController.loadFromXML(fallBack);
                    fallBack.close();
                } else {
                    emuConfig = ConfigController.loadFromXML(configFile);
                }
                moduleConfig = emuConfig.getArchitecture().getModules();
                logger.log(Level.INFO, "[emu] Emulator started with " + moduleConfig.getFdc().getFloppy().size() + " floppy drives and " + moduleConfig.getAta().getHarddiskdrive().size() + " fixed disks");
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "[emu] Config file not readable: " + ex.toString());
                return;
            }
            logger.log(Level.INFO, "[emu] Retrieved settings for modules");
            boolean success = setupEmu();
            if (!success) {
                logger.log(Level.SEVERE, "[emu] Emulation process halted due to error initalizing the modules.");
                this.stop();
                return;
            }
            if (cpu32bit) {
                logger.log(Level.INFO, "[emu] Emulation process started (32-bit).");
                try {
                    AddressSpace addressSpace = null;
                    Processor cpu = (Processor) modules.getModule(Module.Type.CPU);
                    if (cpu.isProtectedMode()) addressSpace = (AddressSpace) hwComponents.get(0); else addressSpace = (AddressSpace) hwComponents.get(1);
                    while (total < 15000) {
                        instr = addressSpace.execute(cpu, cpu.getInstructionPointer());
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        total += instr;
                        loop++;
                    }
                    while (isAlive) {
                        instr = addressSpace.execute(cpu, cpu.getInstructionPointer());
                        total += instr;
                        loop++;
                    }
                } catch (ModeSwitchException e) {
                    instr = 1;
                }
            } else {
                logger.log(Level.INFO, "[emu] Emulation process started (16-bit).");
                if (!modules.getModule(Module.Type.CPU).getDebugMode()) {
                    modules.getModule(Module.Type.CPU).start();
                    if (((ModuleCPU) modules.getModule(Module.Type.CPU)).isAbnormalTermination()) {
                        logger.log(Level.SEVERE, "[emu] Emulation process halted due to error in CPU module.");
                        this.stop();
                        return;
                    }
                    if (((ModuleCPU) modules.getModule(Module.Type.CPU)).isShutdown()) {
                        logger.log(Level.SEVERE, "[emu] Emulation process halted due to request for shutdown by CPU module.");
                        this.stop();
                        return;
                    }
                } else {
                    ModuleCPU cpu = (ModuleCPU) modules.getModule(Module.Type.CPU);
                    logger.log(Level.INFO, cpu.getNextInstructionInfo());
                    while (isAlive) {
                        this.debug(io.getCommand());
                    }
                }
                while (resetBusy) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected void stop() {
        isAlive = false;
        if (modules != null) {
            for (int i = 0; i < modules.size(); i++) {
                modules.getModule(i).stop();
            }
            logger.log(Level.INFO, "[emu] Emulation process stopped.");
            gui.notifyGUI(GUI.EMU_PROCESS_STOP);
        }
    }

    protected void reset() {
        resetBusy = true;
        if (modules != null) {
            ((ModuleCPU) modules.getModule(Module.Type.CPU)).stop();
            coldStart = false;
            logger.log(Level.INFO, "[emu] Reset in progress...");
        }
        resetBusy = false;
    }

    /**
     * @param command
     */
    protected void debug(int command) {
        switch(command) {
            case CMD_DEBUG_HELP:
                io.showHelp();
                break;
            case CMD_DEBUG_STEP:
                ModuleCPU cpu = (ModuleCPU) modules.getModule(Module.Type.CPU);
                String[] sNumber = io.getArguments();
                if (sNumber != null) {
                    int totalInstructions = Integer.parseInt(sNumber[0]);
                    for (int n = 0; n < totalInstructions; n++) {
                        cpu.start();
                    }
                } else {
                    cpu.start();
                }
                logger.log(Level.SEVERE, cpu.getNextInstructionInfo());
                break;
            case CMD_DEBUG_SHOWREG:
                ModuleCPU cpu1 = (ModuleCPU) modules.getModule(Module.Type.CPU);
                logger.log(Level.SEVERE, cpu1.dumpRegisters());
                break;
            case CMD_DEBUG_DUMP:
                String[] moduleTypeArray = io.getArguments();
                if (moduleTypeArray != null) {
                    String moduleType = moduleTypeArray[0];
                    Module mod = modules.getModule(Module.Type.resolveType(moduleType));
                    if (mod != null) {
                        logger.log(Level.INFO, mod.getDump());
                    } else {
                        logger.log(Level.SEVERE, "[emu] Module not recognised.");
                    }
                }
                break;
            case CMD_DEBUG_MEM_DUMP:
                ModuleMemory mem = (ModuleMemory) modules.getModule(Module.Type.MEMORY);
                String[] sArg = io.getArguments();
                int memAddress = Integer.parseInt(sArg[0]);
                int numBytes = 0;
                if (sArg.length == 2) {
                    numBytes = Integer.parseInt(sArg[1]);
                } else {
                    numBytes = 2;
                }
                try {
                    for (int n = 0; n < numBytes; n++) {
                        logger.log(Level.SEVERE, "[emu] Value of [0x" + Integer.toHexString(memAddress + n).toUpperCase() + "]: 0x" + Integer.toHexString(0x100 | mem.getByte(memAddress + n) & 0xFF).substring(1).toUpperCase());
                    }
                } catch (ModuleException e) {
                    e.printStackTrace();
                }
                break;
            default:
                logger.log(Level.SEVERE, "[emu] No command match. Enter a correct emulator command.");
                break;
        }
    }

    /**
     * @param state
     */
    protected void setActive(boolean state) {
        isAlive = state;
    }

    /**
     * Get the modules.
     *
     * @return modules
     */
    public Modules getModules() {
        return this.modules;
    }

    /**
     * Get the hardware components.
     *
     * @return modules
     */
    public ArrayList<HardwareComponent> getHWcomponents() {
        return this.hwComponents;
    }

    /**
     * Set the modules.
     *
     * @param modules
     */
    public void setModules(Modules modules) {
        this.modules = modules;
    }

    /**
     * Get the gui.
     *
     * @return gui
     */
    public GUI getGui() {
        return this.gui;
    }

    /**
     * Get the io.
     *
     * @return io
     */
    public IO getIo() {
        return this.io;
    }

    /**
     * Get cold start.
     *
     * @return coldStart
     */
    public boolean getColdStart() {
        return this.coldStart;
    }

    /**
     * Set cold start.
     *
     * @param coldStart
     */
    public void setColdStart(boolean coldStart) {
        this.coldStart = coldStart;
    }

    /**
     * Return reference to module from given type
     *
     * @param type stating the type of the requested module
     * @return Module requested module, or null if module does not exist
     */
    protected Module getModule(Module.Type type) {
        return modules.getModule(type);
    }

    /**
     * @param keyEvent
     * @param keyEventType
     */
    protected void notifyKeyboard(KeyEvent keyEvent, int keyEventType) {
        ModuleKeyboard keyboard = (ModuleKeyboard) modules.getModule(Module.Type.KEYBOARD);
        if (keyboard != null) {
            keyboard.generateScancode(keyEvent, keyEventType);
        }
    }

    /**
     * @param mouseEvent
     */
    protected void notifyMouse(MouseEvent mouseEvent) {
        ModuleMouse mouse = (ModuleMouse) modules.getModule(Module.Type.MOUSE);
        if (mouse != null) {
            mouse.mouseMotion(mouseEvent);
        }
    }

    /**
     * @param driveLetter
     * @param carrierType
     * @param imageFile
     * @param writeProtected
     * @return -
     */
    protected boolean insertFloppy(String driveLetter, byte carrierType, File imageFile, boolean writeProtected) {
        ModuleFDC fdc = (ModuleFDC) modules.getModule(Module.Type.FDC);
        if (fdc != null) {
            return fdc.insertCarrier(driveLetter, carrierType, imageFile, writeProtected);
        }
        return false;
    }

    /**
     * @param driveLetter
     * @return -
     */
    protected boolean ejectFloppy(String driveLetter) {
        ModuleFDC fdc = (ModuleFDC) modules.getModule(Module.Type.FDC);
        if (fdc != null) {
            return fdc.ejectCarrier(driveLetter);
        }
        return false;
    }

    /**
     * @param status
     */
    public void statusChanged(int status) {
        switch(status) {
            case MODULE_FDC_TRANSFER_START:
                gui.updateGUI(GUI.EMU_FLOPPYA_TRANSFER_START);
                break;
            case MODULE_FDC_TRANSFER_STOP:
                gui.updateGUI(GUI.EMU_FLOPPYA_TRANSFER_STOP);
                break;
            case MODULE_ATA_HD1_TRANSFER_START:
                gui.updateGUI(GUI.EMU_HD1_TRANSFER_START);
                break;
            case MODULE_ATA_HD2_TRANSFER_START:
                gui.updateGUI(GUI.EMU_HD2_TRANSFER_START);
                break;
            case MODULE_ATA_HD1_TRANSFER_STOP:
                gui.updateGUI(GUI.EMU_HD1_TRANSFER_STOP);
                break;
            case MODULE_ATA_HD2_TRANSFER_STOP:
                gui.updateGUI(GUI.EMU_HD2_TRANSFER_STOP);
                break;
            case MODULE_KEYBOARD_NUMLOCK_ON:
                gui.updateGUI(GUI.EMU_KEYBOARD_NUMLOCK_ON);
                break;
            case MODULE_KEYBOARD_NUMLOCK_OFF:
                gui.updateGUI(GUI.EMU_KEYBOARD_NUMLOCK_OFF);
                break;
            case MODULE_KEYBOARD_CAPSLOCK_ON:
                gui.updateGUI(GUI.EMU_KEYBOARD_CAPSLOCK_ON);
                break;
            case MODULE_KEYBOARD_CAPSLOCK_OFF:
                gui.updateGUI(GUI.EMU_KEYBOARD_CAPSLOCK_OFF);
                break;
            case MODULE_KEYBOARD_SCROLLLOCK_ON:
                gui.updateGUI(GUI.EMU_KEYBOARD_SCROLLLOCK_ON);
                break;
            case MODULE_KEYBOARD_SCROLLLOCK_OFF:
                gui.updateGUI(GUI.EMU_KEYBOARD_SCROLLLOCK_OFF);
                break;
            default:
                break;
        }
    }

    /**
     * @return -
     */
    public String getScreenText() {
        ModuleVideo video = (ModuleVideo) modules.getModule(Module.Type.VIDEO);
        if (video != null) {
            return video.getVideoBufferCharacters();
        }
        return null;
    }

    /**
     * @return -
     */
    public BufferedImage getScreenImage() {
        return null;
    }

    /**
     * @return -
     */
    public boolean isCpu32bit() {
        return cpu32bit;
    }

    /**
     * @return -
     */
    public boolean setupEmu() {
        boolean result = true;
        if (coldStart) {
            logger.log(Level.INFO, "===================    COLD START   ===================");
            logger.log(Level.INFO, "=================== CREATE MODULES  ===================");
            result &= createModules();
            logger.log(Level.INFO, "=================== CONNECT MODULES ===================");
            result &= connectModules();
            logger.log(Level.INFO, "===================   INIT TIMERS   ===================");
            result &= setTimingParams(modules.getModule(Module.Type.CPU));
            result &= setTimingParams(modules.getModule(Module.Type.VIDEO));
            result &= setTimingParams(modules.getModule(Module.Type.PIT));
            result &= setTimingParams(modules.getModule(Module.Type.KEYBOARD));
            result &= setTimingParams(modules.getModule(Module.Type.FDC));
            result &= setTimingParams(modules.getModule(Module.Type.ATA));
        } else {
            logger.log(Level.INFO, "===================    WARM START   ===================");
            setColdStart(true);
        }
        logger.log(Level.INFO, "===================  RESET MODULES  ===================");
        result &= resetModules();
        if (cpu32bit) {
        }
        logger.log(Level.INFO, "=================== INIT OUTPUT DEVICES ===================");
        result &= initScreenOutputDevice();
        logger.log(Level.INFO, "================== INIT INPUT DEVICES =================");
        result &= setMouseParams();
        result &= setMemoryParams();
        logger.log(Level.INFO, "===================    LOAD BIOS    ===================");
        result &= loadBIOS();
        logger.log(Level.INFO, "================= LOAD STORAGE MEDIA ==================");
        result &= setFloppyParams();
        setHardDriveParams();
        logger.log(Level.INFO, "===================  MISC SETTINGS  ===================");
        result &= setBootParams();
        if (!cpu32bit) {
            result &= setDebugMode();
        }
        logger.log(Level.INFO, "================= READY FOR EXECUTION =================");
        return result;
    }

    /**
     * @return -
     */
    public boolean createModules() {
        modules = new Modules(20);
        cpu32bit = moduleConfig.getCpu().isCpu32Bit();
        this.getGui().setCpuTypeLabel(cpu32bit ? "32 bit" : "16 bit");
        Clock clk = new Clock(this);
        modules.addModule(clk);
        if (cpu32bit) {
            modules.addModule(new Processor());
        } else {
            modules.addModule(new CPU(this));
        }
        if (cpu32bit) {
            PhysicalAddressSpace physicalAddr = new PhysicalAddressSpace();
            LinearAddressSpace linearAddr = new LinearAddressSpace();
            for (int i = 0; i < PhysicalAddressSpace.SYS_RAM_SIZE; i += AddressSpace.BLOCK_SIZE) physicalAddr.allocateMemory(i, new LazyMemory(AddressSpace.BLOCK_SIZE, clk));
            hwComponents.add(linearAddr);
            hwComponents.add(physicalAddr);
        } else {
            if (dynamicMem) modules.addModule(new DynamicAllocationMemory(this)); else modules.addModule(new Memory(this));
        }
        if (moduleConfig.getBios() != null) {
            for (Bios bios : moduleConfig.getBios()) {
                logger.log(Level.CONFIG, "System bios file: " + bios.getSysbiosfilepath() + "; starting at 0x" + Integer.toHexString(bios.getRamaddresssysbiosstartdec().intValue()));
                logger.log(Level.CONFIG, "VGA bios file: " + bios.getVgabiosfilepath() + "; starting at 0x" + Integer.toHexString(bios.getRamaddressvgabiosstartdec().intValue()));
            }
            if (!cpu32bit) {
                modules.addModule(new BIOS(this));
            }
        }
        modules.addModule(new Motherboard(this));
        modules.addModule(new PIC(this));
        if (moduleConfig.getPit() != null) {
            modules.addModule(new PIT(this));
        }
        modules.addModule(new RTC(this));
        if (moduleConfig.getAta() != null) {
            modules.addModule(new ATA(this));
        }
        if (cpu32bit) {
            DMAController primaryDMA, secondaryDMA;
            primaryDMA = new DMAController(false, true);
            secondaryDMA = new DMAController(false, false);
            hwComponents.add(primaryDMA);
            hwComponents.add(secondaryDMA);
        } else {
            modules.addModule(new DMA(this));
        }
        if (moduleConfig.getFdc() != null) {
            modules.addModule(new FDC(this));
        }
        if (moduleConfig.getKeyboard() != null) {
            modules.addModule(new Keyboard(this));
        }
        if (moduleConfig.getMouse() != null) {
            modules.addModule(new Mouse(this));
        }
        modules.addModule(new ParallelPort(this));
        modules.addModule(new SerialPort(this));
        if (moduleConfig.getVideo() != null) {
            modules.addModule(new Video(this));
        }
        modules.addModule(new DeviceDummy(this));
        modules.addModule(new Screen(this));
        logger.log(Level.INFO, "[emu] All modules are created.");
        return true;
    }

    /**
     * Connect the modules together.
     *
     * @return -
     */
    public boolean connectModules() {
        boolean result = true;
        Module mod1, mod2;
        for (int i = 0; i < modules.size(); i++) {
            mod1 = modules.getModule(i);
            Module.Type[] connections = mod1.getExpectedConnections();
            for (int c = 0; c < connections.length; c++) {
                mod2 = modules.getModule(connections[c]);
                if (mod2 != null) {
                    if (mod1.setConnection(mod2)) {
                        logger.log(Level.INFO, "[emu] Successfully established connection between " + mod1.getType() + " and " + mod2.getType());
                    } else {
                        logger.log(Level.INFO, "[emu] Failed to establish connection between " + mod1.getType() + " and " + mod2.getType());
                    }
                } else {
                    logger.log(Level.INFO, "[emu] Failed to establish connection between " + mod1.getType() + " and unknown module " + connections[c]);
                }
            }
        }
        boolean isConnected = true;
        for (int i = 0; i < modules.size(); i++) {
            if (!(modules.getModule(i).isConnected())) {
                isConnected = false;
                logger.log(Level.INFO, "[emu] Could not connect module: " + modules.getModule(i).getType() + ".");
            }
        }
        if (!isConnected) {
            logger.log(Level.INFO, "[emu] Not all modules are connected. Emulator may be unstable.");
            result &= false;
        } else {
            logger.log(Level.INFO, "[emu] All modules are successfully connected.");
            result &= true;
        }
        if (cpu32bit) {
            Processor cpu = (Processor) modules.getModule(Module.Type.CPU);
            Video vid = (Video) modules.getModule(Module.Type.VIDEO);
            Motherboard mb = (Motherboard) modules.getModule(Module.Type.MOTHERBOARD);
            FDC fdc = (FDC) modules.getModule(Module.Type.FDC);
            LinearAddressSpace linearAddr = (LinearAddressSpace) hwComponents.get(0);
            PhysicalAddressSpace physicalAddr = (PhysicalAddressSpace) hwComponents.get(1);
            DMAController primaryDMA = (DMAController) hwComponents.get(2);
            DMAController secondaryDMA = (DMAController) hwComponents.get(3);
            PIC pic = (PIC) modules.getModule(Module.Type.PIC);
            IOPortHandler ioports = new IOPortHandler();
            ioports.setConnection(mb);
            cpu.acceptComponent(linearAddr);
            cpu.acceptComponent(physicalAddr);
            cpu.acceptComponent(ioports);
            cpu.setConnection(pic);
            physicalAddr.acceptComponent(linearAddr);
            linearAddr.acceptComponent(physicalAddr);
            vid.acceptComponent(physicalAddr);
            primaryDMA.acceptComponent(physicalAddr);
            secondaryDMA.acceptComponent(physicalAddr);
            primaryDMA.acceptComponent(ioports);
            secondaryDMA.acceptComponent(ioports);
            fdc.acceptComponent(primaryDMA);
            fdc.acceptComponent(secondaryDMA);
            result = true;
        }
        return result;
    }

    /**
     * Set the timing parameters
     *
     * @param module
     * @return -
     */
    public boolean setTimingParams(Module module) {
        if (module instanceof ModuleCPU) {
            int mhz = moduleConfig.getCpu().getSpeedmhz().intValue();
            ((ModuleCPU) module).setIPS(mhz * 1000000);
            return true;
        } else if (module instanceof Updateable) {
            int updateInterval;
            if (module instanceof ModulePIT) {
                updateInterval = moduleConfig.getPit().getClockrate().intValue();
            } else if (module instanceof ModuleVideo) {
                updateInterval = moduleConfig.getVideo().getUpdateintervalmicrosecs().intValue();
            } else if (module instanceof ModuleKeyboard) {
                updateInterval = moduleConfig.getKeyboard().getUpdateintervalmicrosecs().intValue();
            } else if (module instanceof ModuleFDC) {
                updateInterval = moduleConfig.getFdc().getUpdateintervalmicrosecs().intValue();
            } else if (module instanceof ModuleATA) {
                updateInterval = moduleConfig.getAta().getUpdateintervalmicrosecs().intValue();
            } else {
                logger.log(Level.WARNING, "Could not set updateInterval for type: " + module.getType());
                return false;
            }
            ((Updateable) module).setUpdateInterval(updateInterval);
            return true;
        }
        return false;
    }

    /**
     * Reset all modules.
     *
     * @return -
     */
    public boolean resetModules() {
        boolean result = true;
        for (int i = 0; i < modules.size(); i++) {
            if (!(modules.getModule(i).reset())) {
                result = false;
                logger.log(Level.SEVERE, "[emu] Could not reset module: " + modules.getModule(i).getType() + ".");
            }
        }
        if (!result) {
            logger.log(Level.SEVERE, "[emu] Not all modules are reset. Emulator may be unstable.");
        } else {
            logger.log(Level.INFO, "[emu] All modules are successfully reset.");
        }
        return result;
    }

    /**
     * Init Screen Output Device.
     *
     * @return -
     */
    public boolean initScreenOutputDevice() {
        ModuleScreen screen = (ModuleScreen) modules.getModule(Module.Type.SCREEN);
        if (screen != null) {
            getGui().setScreen(screen.getScreen());
            return true;
        } else {
            logger.log(Level.WARNING, "[emu] No screen available.");
            return false;
        }
    }

    /**
     * Read from config and set mouse parameters
     *
     * @return -
     */
    public boolean setMouseParams() {
        Mouse mouse = (Mouse) modules.getModule(Module.Type.MOUSE);
        logger.log(Level.INFO, "mouse = " + mouse);
        if (mouse != null) {
            boolean enabled = moduleConfig.getMouse().isEnabled();
            mouse.setMouseEnabled(enabled);
            if (enabled) {
                gui.setMouseEnabled();
                gui.updateGUI(GUI.EMU_DEVICES_MOUSE_ENABLED);
            } else {
                gui.setMouseDisabled();
                gui.updateGUI(GUI.EMU_DEVICES_MOUSE_DISABLED);
            }
            String type = moduleConfig.getMouse().getMousetype();
            mouse.setMouseType(type);
        }
        return true;
    }

    /**
     * Read from config and set memory parameters
     *
     * @return -
     */
    public boolean setMemoryParams() {
        ModuleMemory mem = (ModuleMemory) modules.getModule(Module.Type.MEMORY);
        if (mem != null) {
            int size = moduleConfig.getMemory().getSizemb().intValue();
            mem.setRamSizeInMB(size);
        }
        return true;
    }

    /**
     * Load the BIOS into memory
     *
     * @return -
     */
    public boolean loadBIOS() {
        boolean result = true;
        if (cpu32bit) {
            SystemBIOS sysBIOS;
            VGABIOS vgaBIOS;
            try {
                BufferedInputStream bdis = new BufferedInputStream(new DataInputStream(new FileInputStream(new File(Constants.BOCHS_BIOS))));
                byte[] byteArray = new byte[bdis.available()];
                bdis.read(byteArray, 0, byteArray.length);
                bdis.close();
                Clock clk = (Clock) modules.getModule(Module.Type.CLOCK);
                sysBIOS = new SystemBIOS(byteArray, clk);
                BufferedInputStream bdis2 = new BufferedInputStream(new DataInputStream(new FileInputStream(new File(Constants.VGA_BIOS))));
                byte[] byteArray2 = new byte[bdis2.available()];
                bdis2.read(byteArray2, 0, byteArray2.length);
                bdis2.close();
                vgaBIOS = new VGABIOS(byteArray2, clk);
                PhysicalAddressSpace physicalAddr = (PhysicalAddressSpace) hwComponents.get(1);
                sysBIOS.acceptComponent(physicalAddr);
                vgaBIOS.acceptComponent(physicalAddr);
                hwComponents.add(sysBIOS);
                hwComponents.add(vgaBIOS);
            } catch (IOException e) {
                e.printStackTrace();
                result &= false;
            }
            return result;
        } else {
            BIOS bios = (BIOS) modules.getModule(Module.Type.BIOS);
            String sysBiosFilePath = Utilities.resolvePathAsString(moduleConfig.getBios().get(0).getSysbiosfilepath());
            String vgaBiosFilePath = Utilities.resolvePathAsString(moduleConfig.getBios().get(0).getVgabiosfilepath());
            int ramAddressSysBiosStart = moduleConfig.getBios().get(0).getRamaddresssysbiosstartdec().intValue();
            int ramAddressVgaBiosStart = moduleConfig.getBios().get(0).getRamaddressvgabiosstartdec().intValue();
            try {
                if (bios.setSystemBIOS(getIo().importBinaryStream(sysBiosFilePath))) {
                    logger.log(Level.CONFIG, "[emu] System BIOS successfully stored in ROM.");
                    ModuleMemory mem = (ModuleMemory) modules.getModule(Module.Type.MEMORY);
                    mem.setBytes(ramAddressSysBiosStart, bios.getSystemBIOS());
                    logger.log(Level.CONFIG, "[emu] System BIOS successfully loaded in RAM.");
                } else {
                    logger.log(Level.SEVERE, "[emu] Not able to retrieve System BIOS binaries from file system.");
                    result &= false;
                }
            } catch (ModuleException emod) {
                logger.log(Level.SEVERE, emod.getMessage());
                result &= false;
            } catch (IOException eio) {
                logger.log(Level.SEVERE, eio.getMessage());
                result &= false;
            }
            try {
                if (bios.setVideoBIOS(getIo().importBinaryStream(vgaBiosFilePath))) {
                    logger.log(Level.CONFIG, "[emu] Video BIOS successfully stored in ROM.");
                    ModuleMemory mem = (ModuleMemory) modules.getModule(Module.Type.MEMORY);
                    mem.setBytes(ramAddressVgaBiosStart, bios.getVideoBIOS());
                    logger.log(Level.CONFIG, "[emu] Video BIOS successfully loaded in RAM.");
                } else {
                    logger.log(Level.SEVERE, "[emu] Not able to retrieve Video BIOS binaries from file system.");
                    result &= false;
                }
            } catch (ModuleException emod) {
                logger.log(Level.SEVERE, emod.getMessage());
                result &= false;
            } catch (IOException eio) {
                logger.log(Level.SEVERE, eio.getMessage());
                result &= false;
            }
            return result;
        }
    }

    /**
     * Get and set floppy parameters
     *
     * @return -
     */
    public boolean setFloppyParams() {
        ModuleFDC fdc = (ModuleFDC) modules.getModule(Module.Type.FDC);
        int numFloppyDrives = moduleConfig.getFdc().getFloppy().size();
        logger.log(Level.INFO, "[emu] Configuring " + numFloppyDrives + " floppy drives");
        fdc.setNumberOfDrives(numFloppyDrives);
        for (int i = 0; i < numFloppyDrives; i++) {
            Floppy floppyConfig = moduleConfig.getFdc().getFloppy().get(i);
            boolean inserted = floppyConfig.isInserted();
            String driveLetter = floppyConfig.getDriveletter();
            String diskformat = floppyConfig.getDiskformat();
            byte carrierType = 0x0;
            boolean writeProtected = floppyConfig.isWriteprotected();
            String imageFilePath = Utilities.resolvePathAsString(floppyConfig.getImagefilepath());
            if (diskformat.equals("360K")) carrierType = 0x01; else if (diskformat.equals("1.2M")) carrierType = 0x02; else if (diskformat.equals("720K")) carrierType = 0x03; else if (diskformat.equals("1.44M")) carrierType = 0x04; else if (diskformat.equals("2.88M")) carrierType = 0x05; else if (diskformat.equals("160K")) carrierType = 0x06; else if (diskformat.equals("180K")) carrierType = 0x07; else if (diskformat.equals("320K")) carrierType = 0x08; else logger.log(Level.SEVERE, "[emu] Floppy disk format not recognised.");
            if (inserted) {
                File imageFile = new File(imageFilePath);
                fdc.insertCarrier(driveLetter, carrierType, imageFile, writeProtected);
                if (driveLetter.equals("A")) {
                    getGui().updateGUI(GUI.EMU_FLOPPYA_INSERT);
                }
            }
        }
        return true;
    }

    /**
     * Read and set the hard drive parameters
     *
     * @return -
     */
    public boolean setHardDriveParams() {
        ModuleATA ata = (ATA) modules.getModule(Module.Type.ATA);
        int numFixedDisks = moduleConfig.getAta().getHarddiskdrive().size();
        logger.log(Level.INFO, "Configuring " + numFixedDisks + " fixed disks");
        if (moduleConfig.getAta().getHarddiskdrive().isEmpty()) {
            return false;
        }
        for (int i = 0; i < numFixedDisks; i++) {
            Harddiskdrive hddConfig = moduleConfig.getAta().getHarddiskdrive().get(i);
            boolean enabled = hddConfig.isEnabled();
            int ideChannelIndex = hddConfig.getChannelindex().intValue();
            boolean isMaster = hddConfig.isMaster();
            boolean autoDetectCylinders = hddConfig.isAutodetectcylinders();
            int numCylinders = hddConfig.getCylinders().intValue();
            int numHeads = hddConfig.getHeads().intValue();
            int numSectorsPerTrack = hddConfig.getSectorspertrack().intValue();
            String imageFilePath = Utilities.resolvePathAsString(hddConfig.getImagefilepath());
            if (enabled && ideChannelIndex >= 0 && ideChannelIndex < 4) {
                if (autoDetectCylinders) {
                    numCylinders = 0;
                }
                ata.initConfig(ideChannelIndex, isMaster, true, false, numCylinders, numHeads, numSectorsPerTrack, ATATranslationType.AUTO, imageFilePath);
                if (ideChannelIndex == 0 && i == 0) {
                    getGui().updateGUI(GUI.EMU_HD1_INSERT);
                }
                if (ideChannelIndex == 0 && i == 1) {
                    getGui().updateGUI(GUI.EMU_HD2_INSERT);
                }
            }
        }
        return true;
    }

    /**
     * Read from config and set the boot params.
     *
     * @return -
     */
    public boolean setBootParams() {
        ATA ata = (ATA) modules.getModule(Module.Type.ATA);
        Bios bios = moduleConfig.getBios().get(0);
        Bootdrives bootdrives = bios.getBootdrives();
        boolean floppyCheckDisabled = bios.isFloppycheckdisabled();
        int[] bootDrives = new int[3];
        for (int i = 0; i < bootDrives.length; i++) {
            String name = "none";
            if (i == 0) {
                name = bootdrives.getBootdrive0();
            }
            if (i == 1) {
                name = bootdrives.getBootdrive1();
            }
            if (i == 2) {
                name = bootdrives.getBootdrive2();
            }
            if (name.equalsIgnoreCase("Hard Drive")) {
                bootDrives[i] = ATAConstants.BOOT_DISKC;
            } else if (name.equalsIgnoreCase("Floppy Drive")) {
                bootDrives[i] = ATAConstants.BOOT_FLOPPYA;
            } else if (name.equalsIgnoreCase("cd")) {
                bootDrives[i] = ATAConstants.BOOT_CDROM;
            } else if (name.equalsIgnoreCase("none")) {
                bootDrives[i] = ATAConstants.BOOT_NONE;
            }
        }
        ata.setCmosSettings(bootDrives, floppyCheckDisabled);
        return true;
    }

    /**
     * Set the debug mode.
     *
     * @return -
     */
    public boolean setDebugMode() {
        boolean result = true;
        boolean isDebugMode = emuConfig.isDebug();
        if (isDebugMode) {
            for (int i = 0; i < modules.size(); i++) {
                modules.getModule(i).setDebugMode(true);
            }
            logger.log(Level.INFO, "[emu] All modules in debug mode.");
            return result;
        }
        modules.getModule(Module.Type.ATA).setDebugMode(moduleConfig.getAta().isDebug());
        modules.getModule(Module.Type.CPU).setDebugMode(moduleConfig.getCpu().isDebug());
        modules.getModule(Module.Type.MEMORY).setDebugMode(moduleConfig.getMemory().isDebug());
        modules.getModule(Module.Type.FDC).setDebugMode(moduleConfig.getFdc().isDebug());
        modules.getModule(Module.Type.PIT).setDebugMode(moduleConfig.getPit().isDebug());
        modules.getModule(Module.Type.KEYBOARD).setDebugMode(moduleConfig.getKeyboard().isDebug());
        modules.getModule(Module.Type.MOUSE).setDebugMode(moduleConfig.getMouse().isDebug());
        modules.getModule(Module.Type.VIDEO).setDebugMode(moduleConfig.getVideo().isDebug());
        boolean memDebug = moduleConfig.getMemory().isDebug();
        int memAddress = moduleConfig.getMemory().getDebugaddressdecimal().intValue();
        if (moduleConfig.getMemory().isDebug()) {
            ((ModuleMemory) modules.getModule(Module.Type.MEMORY)).setWatchValueAndAddress(memDebug, memAddress);
            logger.log(Level.CONFIG, "[emu] RAM address watch set to " + memDebug + "; address: " + memAddress);
        }
        return true;
    }
}
