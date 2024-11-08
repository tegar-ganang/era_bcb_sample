package dioscuri.module.keyboard;

import dioscuri.Emulator;
import dioscuri.exception.ModuleException;
import dioscuri.exception.UnknownPortException;
import dioscuri.exception.WriteOnlyPortException;
import dioscuri.interfaces.Module;
import dioscuri.module.*;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of a keyboard module.
 *
 * @see dioscuri.module.AbstractModule
 *      <p/>
 *      Metadata module ********************************************
 *      general.type : keyboard general.name : XT/AT/PS2 compatible Keyboard
 *      general.architecture : Von Neumann general.description : Models a
 *      101-key XT/AT/PS2 compatible keyboard general.creator : Tessella Support
 *      Services, Koninklijke Bibliotheek, Nationaal Archief of the Netherlands
 *      general.version : 1.0 general.keywords : Keyboard, XT, AT, PS/2, Intel
 *      8042 general.relations : Motherboard general.yearOfIntroduction :
 *      general.yearOfEnding : general.ancestor : general.successor :
 *      <p/>
 *      <p/>
 *      Notes: - Keyboard can handle XT, AT and PS/2 compatible keyboards - This
 *      class uses a lot (if not all) of Bochs source code from keyboard.{h,cc};
 *      - Conversions from C++ to Java have been made, and will need revising
 *      and/or updating - Aside from handling keystrokes, the keyboard
 *      controller is responsible for: + the status of the PC speaker via 0x61.
 *      This is not implemented yet. + A20 address line (memory looping turned
 *      on or off) to be enabled/disabled. + mouse support - Information used in
 *      this module was taken from: +
 *      http://mudlist.eorbit.net/~adam/pickey/ports.html +
 *      http://homepages.cwi.nl/~aeb/linux/kbd/scancodes.html
 */
public class Keyboard extends ModuleKeyboard {

    private static final Logger logger = Logger.getLogger(Keyboard.class.getName());

    private Emulator emu;

    private TheKeyboard keyboard;

    private ScanCodeSets scanCodeSet;

    private int irqNumberKeyboard;

    private int irqNumberMouse;

    private boolean pendingIRQ;

    private int updateInterval;

    private static final int DATA_PORT = 0x60;

    private static final int STATUS_PORT = 0x64;

    static int kbdInitialised = 0;

    private static final int KEYBOARD = 0;

    private static final int MOUSE = 1;

    /**
     * Class constructor
     */
    public Keyboard(Emulator owner) {
        emu = owner;
        keyboard = new TheKeyboard();
        scanCodeSet = new ScanCodeSets();
        updateInterval = -1;
        irqNumberKeyboard = -1;
        irqNumberMouse = -1;
        pendingIRQ = false;
        keyboard.internalBuffer.ledStatus = 0;
        keyboard.internalBuffer.scanningEnabled = 1;
        keyboard.controller.parityError = 0;
        keyboard.controller.timeOut = 0;
        keyboard.controller.auxBuffer = 0;
        keyboard.controller.keyboardLock = 1;
        keyboard.controller.commandData = 1;
        keyboard.controller.systemFlag = 0;
        keyboard.controller.inputBuffer = 0;
        keyboard.controller.outputBuffer = 0;
        keyboard.controller.kbdClockEnabled = 1;
        keyboard.controller.auxClockEnabled = 0;
        keyboard.controller.allowIRQ1 = 1;
        keyboard.controller.allowIRQ12 = 1;
        keyboard.controller.kbdOutputBuffer = 0;
        keyboard.controller.auxOutputBuffer = 0;
        keyboard.controller.lastCommand = 0;
        keyboard.controller.expectingPort60h = 0;
        keyboard.controller.irq1Requested = 0;
        keyboard.controller.irq12Requested = 0;
        keyboard.controller.batInProgress = 0;
        keyboard.controller.timerPending = 0;
        logger.log(Level.INFO, "[" + super.getType() + "]" + " AbstractModule created successfully.");
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.module.AbstractModule
     */
    @Override
    public boolean reset() {
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
        ModuleRTC rtc = (ModuleRTC) super.getConnection(Module.Type.RTC);
        ModuleMouse mouse = (ModuleMouse) super.getConnection(Module.Type.MOUSE);
        motherboard.setIOPort(DATA_PORT, this);
        motherboard.setIOPort(STATUS_PORT, this);
        irqNumberKeyboard = pic.requestIRQNumber(this);
        if (irqNumberKeyboard > -1) {
            logger.log(Level.CONFIG, "[" + super.getType() + "]" + " Keyboard IRQ number set to: " + irqNumberKeyboard);
        } else {
            logger.log(Level.WARNING, "[" + super.getType() + "]" + " Request of IRQ number failed.");
        }
        if (mouse != null) {
            irqNumberMouse = pic.requestIRQNumber(mouse);
            if (irqNumberMouse > -1) {
                logger.log(Level.INFO, "[" + super.getType() + "]" + " Mouse IRQ number set to: " + irqNumberMouse);
            } else {
                logger.log(Level.WARNING, "[" + super.getType() + "]" + " Request of IRQ number failed.");
            }
        } else {
            logger.log(Level.INFO, "[" + super.getType() + "]" + " No mouse available (or not connected to keyboard controller)");
        }
        if (!motherboard.requestTimer(this, updateInterval, true)) {
            return false;
        }
        motherboard.setTimerActiveState(this, true);
        rtc.setCMOSRegister(0x14, (byte) ((rtc.getCMOSRegister(0x14) | 0x04)));
        return resetKeyboardBuffer(1);
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.module.AbstractModule
     */
    @Override
    public String getDump() {
        try {
            String keyboardDump = "Keyboard status:\n";
            keyboardDump += "Internal buffer contents:";
            keyboardDump += keyboard.internalBuffer.getBuffer().toString() + "\n";
            keyboardDump += "Controller queue contents:";
            keyboardDump += keyboard.getControllerQueue() + "\n";
            return keyboardDump;
        } catch (Exception e) {
            return "getDump() failed due to: " + e.getMessage();
        }
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
            updateInterval = 200;
        }
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        motherboard.resetTimer(this, updateInterval);
    }

    /**
     * Update device Calls the keyboard controller 'poll' function and raises
     * the IRQs resulting from that call
     */
    public void update() {
        int returnValue;
        returnValue = poll();
        if ((returnValue & 0x01) == 1) {
            setInterrupt(irqNumberKeyboard);
            logger.log(Level.INFO, "[" + super.getType() + "]" + " timer raises IRQ1");
        }
        if ((returnValue & 0x02) == 2) {
            setInterrupt(irqNumberMouse);
            logger.log(Level.INFO, "[" + super.getType() + "]" + " timer raises IRQ12");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Addressable
     */
    @Override
    public byte getIOPortByte(int portAddress) throws UnknownPortException, WriteOnlyPortException {
        byte value;
        switch(portAddress) {
            case (0x60):
                if (keyboard.controller.auxBuffer != 0) {
                    value = keyboard.controller.auxOutputBuffer;
                    keyboard.controller.auxOutputBuffer = 0;
                    keyboard.controller.outputBuffer = 0;
                    keyboard.controller.auxBuffer = 0;
                    keyboard.controller.irq12Requested = 0;
                    if (!keyboard.getControllerQueue().isEmpty()) {
                        keyboard.controller.auxOutputBuffer = (keyboard.getControllerQueue().remove(0));
                        keyboard.controller.outputBuffer = 1;
                        keyboard.controller.auxBuffer = 1;
                        if (keyboard.controller.allowIRQ12 != 0) {
                            keyboard.controller.irq12Requested = 1;
                        }
                        logger.log(Level.CONFIG, "controller_Qsize: " + keyboard.getControllerQueue().size() + 1);
                    }
                    clearInterrupt(irqNumberMouse);
                    activateTimer();
                    logger.log(Level.CONFIG, "[" + super.getType() + "] (mouse)" + " Port 0x" + Integer.toHexString(portAddress).toUpperCase() + " read: " + Integer.toHexString(0x100 | value & 0xFF).substring(1).toUpperCase() + "h");
                    return value;
                } else if (keyboard.controller.outputBuffer != 0) {
                    value = keyboard.controller.kbdOutputBuffer;
                    keyboard.controller.outputBuffer = 0;
                    keyboard.controller.auxBuffer = 0;
                    keyboard.controller.irq1Requested = 0;
                    keyboard.controller.batInProgress = 0;
                    if (!keyboard.getControllerQueue().isEmpty()) {
                        keyboard.controller.auxOutputBuffer = (keyboard.getControllerQueue().remove(0));
                        keyboard.controller.outputBuffer = 1;
                        keyboard.controller.auxBuffer = 1;
                        if (keyboard.controller.allowIRQ1 != 0) {
                            keyboard.controller.irq1Requested = 1;
                        }
                    }
                    clearInterrupt(irqNumberKeyboard);
                    activateTimer();
                    logger.log(Level.CONFIG, "[" + super.getType() + "] (keyboard)" + " Port 0x" + Integer.toHexString(portAddress).toUpperCase() + " read: " + Integer.toHexString(0x100 | value & 0xFF).substring(1).toUpperCase() + "h");
                    return value;
                } else {
                    logger.log(Level.INFO, "[" + super.getType() + "]" + " Internal buffer elements no.: " + keyboard.internalBuffer.getBuffer().size());
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " Port 0x60 read but output buffer empty!");
                    return keyboard.controller.kbdOutputBuffer;
                }
            case (0x64):
                value = (byte) ((keyboard.controller.parityError << 7) | (keyboard.controller.timeOut << 6) | (keyboard.controller.auxBuffer << 5) | (keyboard.controller.keyboardLock << 4) | (keyboard.controller.commandData << 3) | (keyboard.controller.systemFlag << 2) | (keyboard.controller.inputBuffer << 1) | keyboard.controller.outputBuffer);
                keyboard.controller.timeOut = 0;
                return value;
            default:
                throw new UnknownPortException(super.getType() + " does not recognise port 0x" + Integer.toHexString(portAddress).toUpperCase());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Addressable
     */
    @Override
    public void setIOPortByte(int portAddress, byte value) throws UnknownPortException {
        ModuleMotherboard motherboard = (ModuleMotherboard) super.getConnection(Module.Type.MOTHERBOARD);
        logger.log(Level.CONFIG, "[" + super.getType() + "]" + " Port 0x" + Integer.toHexString(portAddress).toUpperCase() + " received write of 0x" + Integer.toHexString(0x100 | value & 0xFF).substring(1).toUpperCase());
        switch(portAddress) {
            case 0x60:
                if (keyboard.controller.expectingPort60h != 0) {
                    keyboard.controller.expectingPort60h = 0;
                    keyboard.controller.commandData = 0;
                    if (keyboard.controller.inputBuffer != 0) {
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Port 0x60 write but input buffer is not ready");
                    }
                    switch(keyboard.controller.lastCommand) {
                        case 0x60:
                            byte disableKeyboard, disableAux;
                            keyboard.controller.translateScancode = (byte) ((value >> 6) & 0x01);
                            disableAux = (byte) ((value >> 5) & 0x01);
                            disableKeyboard = (byte) ((value >> 4) & 0x01);
                            keyboard.controller.systemFlag = (byte) ((value >> 2) & 0x01);
                            keyboard.controller.allowIRQ12 = (byte) ((value >> 1) & 0x01);
                            keyboard.controller.allowIRQ1 = (byte) ((value >> 0) & 0x01);
                            this.setKeyboardClock(disableKeyboard == 0 ? true : false);
                            this.setAuxClock(disableAux == 0 ? true : false);
                            if ((keyboard.controller.allowIRQ12 != 0) && (keyboard.controller.auxBuffer != 0)) {
                                keyboard.controller.irq12Requested = 1;
                                logger.log(Level.INFO, "[" + super.getType() + "]" + " IRQ12 (mouse) allowance set to " + keyboard.controller.allowIRQ12);
                            } else if ((keyboard.controller.allowIRQ1 != 0) && (keyboard.controller.outputBuffer != 0)) {
                                keyboard.controller.irq1Requested = 1;
                                logger.log(Level.INFO, "[" + super.getType() + "]" + " IRQ1 (keyboard) allowance set to " + keyboard.controller.allowIRQ1);
                            }
                            if (keyboard.controller.translateScancode == 0) {
                                logger.log(Level.WARNING, "[" + super.getType() + "]" + " Scancode translation turned off");
                            }
                            return;
                        case (byte) 0xD1:
                            logger.log(Level.INFO, "[" + super.getType() + "]" + " Writing value 0x" + Integer.toHexString(0x100 | value & 0xFF).substring(1).toUpperCase() + " to output port P2");
                            motherboard.setA20((value & 0x02) != 0 ? true : false);
                            logger.log(Level.INFO, "[" + super.getType() + "]" + (((value & 0x02) == 2) ? "En" : "Dis") + "abled A20 gate");
                            if (!((value & 0x01) != 0)) {
                                logger.log(Level.WARNING, "[" + super.getType() + "]" + " System reset requested (is not implemented yet)");
                            }
                            return;
                        case (byte) 0xD2:
                            this.enqueueControllerBuffer(value, KEYBOARD);
                            return;
                        case (byte) 0xD3:
                            this.enqueueControllerBuffer(value, MOUSE);
                            return;
                        case (byte) 0xD4:
                            logger.log(Level.INFO, "[keyboard] writing value to mouse: " + value);
                            ModuleMouse mouse = (ModuleMouse) super.getConnection(Module.Type.MOUSE);
                            mouse.controlMouse(value);
                            return;
                        default:
                            logger.log(Level.WARNING, "[" + super.getType() + "]" + " does not recognise command [" + Integer.toHexString(keyboard.controller.lastCommand).toUpperCase() + "] writing value " + Integer.toHexString(value).toUpperCase() + " to port " + Integer.toHexString(portAddress).toUpperCase());
                            throw new UnknownPortException(super.getType() + " -> does not recognise command " + keyboard.controller.lastCommand + " writing value " + Integer.toHexString(value).toUpperCase() + " to port " + Integer.toHexString(portAddress).toUpperCase());
                    }
                } else {
                    keyboard.controller.commandData = 0;
                    keyboard.controller.expectingPort60h = 0;
                    if (keyboard.controller.kbdClockEnabled == 0) {
                        setKeyboardClock(true);
                    }
                    this.dataPortToInternalKB(value);
                }
                return;
            case 0x64:
                keyboard.controller.commandData = 1;
                keyboard.controller.lastCommand = value;
                keyboard.controller.expectingPort60h = 0;
                switch(value) {
                    case 0x20:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + "Read keyboard controller command byte");
                        if (keyboard.controller.outputBuffer != 0) {
                            logger.log(Level.WARNING, "[" + super.getType() + "]" + " command 0x" + Integer.toHexString(value).toUpperCase() + " encountered but output buffer not empty!");
                            return;
                        }
                        byte commandByte = (byte) ((keyboard.controller.translateScancode << 6) | ((keyboard.controller.auxClockEnabled == 0 ? 1 : 0) << 5) | ((keyboard.controller.kbdClockEnabled == 0 ? 1 : 0) << 4) | (0 << 3) | (keyboard.controller.systemFlag << 2) | (keyboard.controller.allowIRQ12 << 1) | (keyboard.controller.allowIRQ1 << 0));
                        this.enqueueControllerBuffer(commandByte, KEYBOARD);
                        return;
                    case 0x60:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Write keyboard controller command byte");
                        keyboard.controller.expectingPort60h = 1;
                        return;
                    case (byte) 0xA0:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + "Unsupported command on port 0x64: 0x" + Integer.toHexString(0x100 | value & 0xFF).substring(1).toUpperCase());
                        return;
                    case (byte) 0xA1:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Controller firmware version request: ignored");
                        return;
                    case (byte) 0xA7:
                        this.setAuxClock(false);
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Aux device (mouse) disabled");
                        return;
                    case (byte) 0xA8:
                        this.setAuxClock(true);
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Aux device (mouse) enabled");
                        return;
                    case (byte) 0xA9:
                        if (keyboard.controller.outputBuffer != 0) {
                            logger.log(Level.WARNING, "[" + super.getType() + "]" + " command 0x" + Integer.toHexString(value).toUpperCase() + " encountered but output buffer not empty!");
                            return;
                        }
                        this.enqueueControllerBuffer((byte) 0xFF, KEYBOARD);
                        return;
                    case (byte) 0xAA:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Controller self test");
                        if (kbdInitialised == 0) {
                            keyboard.getControllerQueue().clear();
                            keyboard.controller.outputBuffer = 0;
                            kbdInitialised++;
                        }
                        if (keyboard.controller.outputBuffer != 0) {
                            logger.log(Level.WARNING, "[" + super.getType() + "]" + " command 0x" + Integer.toHexString(value).toUpperCase() + " encountered but output buffer not empty!");
                            return;
                        }
                        keyboard.controller.systemFlag = 1;
                        this.enqueueControllerBuffer((byte) 0x55, KEYBOARD);
                        return;
                    case (byte) 0xAB:
                        if (keyboard.controller.outputBuffer != 0) {
                            logger.log(Level.WARNING, "[" + super.getType() + "]" + " command 0x" + Integer.toHexString(value).toUpperCase() + " encountered but output buffer not empty!");
                            return;
                        }
                        this.enqueueControllerBuffer((byte) 0x00, KEYBOARD);
                        return;
                    case (byte) 0xAD:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Keyboard disabled");
                        this.setKeyboardClock(false);
                        return;
                    case (byte) 0xAE:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Keyboard enabled");
                        this.setKeyboardClock(true);
                        return;
                    case (byte) 0xAF:
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + "Unsupported command on port 0x64: 0x" + Integer.toHexString(0x100 | value & 0xFF).substring(1).toUpperCase());
                        return;
                    case (byte) 0xC0:
                        if (keyboard.controller.outputBuffer != 0) {
                            logger.log(Level.WARNING, "[" + super.getType() + "]" + " command 0x" + Integer.toHexString(value).toUpperCase() + " encountered but output buffer not empty!");
                            return;
                        }
                        this.enqueueControllerBuffer((byte) 0x80, KEYBOARD);
                        return;
                    case (byte) 0xC1:
                    case (byte) 0xC2:
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + "Unsupported command on port 0x64: 0x" + Integer.toHexString(0x100 | value & 0xFF).substring(1).toUpperCase());
                        return;
                    case (byte) 0xD0:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + "Partially supported command on port 0x64: 0x" + Integer.toHexString(0x100 | value & 0xFF).substring(1).toUpperCase());
                        if (keyboard.controller.outputBuffer != 0) {
                            logger.log(Level.WARNING, "[" + super.getType() + "]" + " command 0x" + Integer.toHexString(value).toUpperCase() + " encountered but output buffer not empty!");
                            return;
                        }
                        byte p2 = (byte) ((keyboard.controller.irq12Requested << 5) | (keyboard.controller.irq1Requested << 4) | (motherboard.getA20() ? 1 : 0 << 1) | 0x01);
                        this.enqueueControllerBuffer(p2, KEYBOARD);
                        return;
                    case (byte) 0xD1:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Port 0x64: write output port P2");
                        keyboard.controller.expectingPort60h = 1;
                        return;
                    case (byte) 0xD2:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Port 0x64: write keyboard output buffer");
                        keyboard.controller.expectingPort60h = 1;
                        return;
                    case (byte) 0xD3:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Port 0x64: write mouse output buffer");
                        keyboard.controller.expectingPort60h = 1;
                        return;
                    case (byte) 0xD4:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Port 0x64: write to mouse");
                        keyboard.controller.expectingPort60h = 1;
                        return;
                    case (byte) 0xDD:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Port 0xDD: A20 address line disabled");
                        motherboard.setA20(false);
                        return;
                    case (byte) 0xDF:
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " Port 0xDF: A20 address line enabled");
                        motherboard.setA20(true);
                        return;
                    case (byte) 0xE0:
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Unsupported command to port 0x64: 0x" + Integer.toHexString(0x100 | value & 0xFF).substring(1).toUpperCase());
                        return;
                    case (byte) 0xFE:
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Port 0x64: system reset (not implemented yet)");
                        return;
                    default:
                        if ((value >= 0xF0 && value <= 0xFD) || value == 0xFF) {
                            logger.log(Level.INFO, "[" + super.getType() + "]" + " Port 0x64: pulse output bits");
                            return;
                        }
                        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Unsupported command to port 0x64: 0x" + Integer.toHexString(0x100 | value & 0xFF).substring(1).toUpperCase());
                        return;
                }
            default:
                throw new UnknownPortException("[" + super.getType() + "]" + " does not recognise OUT port 0x" + Integer.toHexString(portAddress).toUpperCase());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Addressable
     */
    @Override
    public byte[] getIOPortWord(int portAddress) throws ModuleException, WriteOnlyPortException {
        logger.log(Level.WARNING, "[" + super.getType() + "]" + " IN command (word) to port 0x" + Integer.toHexString(portAddress).toUpperCase() + " received");
        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Returned default value 0xFFFF to AX");
        return new byte[] { (byte) 0x0FF, (byte) 0x0FF };
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Addressable
     */
    @Override
    public void setIOPortWord(int portAddress, byte[] dataWord) throws ModuleException {
        logger.log(Level.WARNING, "[" + super.getType() + "]" + " OUT command (word) to port 0x" + Integer.toHexString(portAddress).toUpperCase() + " received. No action taken.");
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Addressable
     */
    @Override
    public byte[] getIOPortDoubleWord(int portAddress) throws ModuleException, WriteOnlyPortException {
        logger.log(Level.WARNING, "[" + super.getType() + "]" + " IN command (double word) to port 0x" + Integer.toHexString(portAddress).toUpperCase() + " received");
        logger.log(Level.WARNING, "[" + super.getType() + "]" + " Returned default value 0xFFFFFFFF to eAX");
        return new byte[] { (byte) 0x0FF, (byte) 0x0FF, (byte) 0x0FF, (byte) 0x0FF };
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.interfaces.Addressable
     */
    @Override
    public void setIOPortDoubleWord(int portAddress, byte[] dataDoubleWord) throws ModuleException {
        logger.log(Level.WARNING, "[" + super.getType() + "]" + " OUT command (double word) to port 0x" + Integer.toHexString(portAddress).toUpperCase() + " received. No action taken.");
    }

    /**
     * @param irqNumber
     */
    protected void setInterrupt(int irqNumber) {
        ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
        pic.setIRQ(irqNumber);
        pendingIRQ = true;
    }

    /**
     * @param irqNumber
     */
    protected void clearInterrupt(int irqNumber) {
        ModulePIC pic = (ModulePIC) super.getConnection(Module.Type.PIC);
        pic.clearIRQ(irqNumber);
        if (pendingIRQ) {
            pendingIRQ = false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.module.ModuleKeyboard
     */
    @Override
    public void generateScancode(KeyEvent keyEvent, int eventType) {
        String[] scancode;
        int i, parsedInt;
        logger.log(Level.INFO, "[" + super.getType() + "]" + " generateScancode(): " + keyEvent.getKeyCode() + ((eventType == 1) ? " pressed" : " released"));
        if (keyboard.controller.kbdClockEnabled == 0 || keyboard.internalBuffer.scanningEnabled == 0) {
            return;
        }
        if (!scanCodeSet.keyIsPresent(keyboard.controller.currentScancodeSet, keyEvent.getKeyCode(), eventType)) {
            logger.log(Level.INFO, "[" + super.getType() + "]" + " ignoring illegal keystroke.");
            return;
        }
        scancode = scanCodeSet.scancodes[keyboard.controller.currentScancodeSet][keyEvent.getKeyCode()][eventType].split(" ");
        if (!scancode[0].equals("")) {
            if (keyEvent.getKeyCode() == KeyEvent.VK_CONTROL || keyEvent.getKeyCode() == KeyEvent.VK_ALT || keyEvent.getKeyCode() == KeyEvent.VK_SHIFT) {
                if (keyEvent.getKeyLocation() == KeyEvent.KEY_LOCATION_LEFT) {
                    scanCodeSet.scancodes[keyboard.controller.currentScancodeSet][keyEvent.getKeyCode() - 3][eventType].split(" ");
                }
            }
            if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                if (keyEvent.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD) {
                    scanCodeSet.scancodes[keyboard.controller.currentScancodeSet][keyEvent.getKeyCode() + 1][eventType].split(" ");
                }
            }
            if (keyboard.controller.translateScancode != 0) {
                int valueOR = 0x00;
                for (i = 0; i < scancode.length; i++) {
                    parsedInt = Integer.parseInt(scancode[i], 16);
                    if (parsedInt == 0xF0) {
                        valueOR = 0x80;
                    } else {
                        logger.log(Level.INFO, "[" + super.getType() + "]" + " generateScancode(): Translated scancode to " + (scanCodeSet.translate8042[parsedInt] | valueOR));
                        enqueueInternalBuffer((byte) (scanCodeSet.translate8042[parsedInt] | valueOR));
                        valueOR = 0x00;
                    }
                }
            } else {
                for (i = 0; i < scancode.length; i++) {
                    logger.log(Level.INFO, "[" + super.getType() + "]" + " generateScancode(): Writing raw " + scancode[i]);
                    enqueueInternalBuffer((byte) Integer.parseInt(scancode[i], 16));
                }
            }
        } else {
            logger.log(Level.WARNING, "[" + super.getType() + "]" + " Key not recognised (scancode not found).");
        }
    }

    /**
     * Keyboard specific reset, with value to indicate reset type
     *
     * @param resetType Type of reset passed to keyboard<BR>
     *                  0: Warm reset (SW reset)<BR>
     *                  1: Cold reset (HW reset)
     * @return boolean true if module has been reset successfully, false
     *         otherwise
     */
    private boolean resetKeyboardBuffer(int resetType) {
        keyboard.internalBuffer.getBuffer().clear();
        keyboard.internalBuffer.expectingTypematic = 0;
        keyboard.internalBuffer.expectingScancodeSet = 0;
        keyboard.controller.currentScancodeSet = 1;
        keyboard.controller.translateScancode = 1;
        if (resetType != 0) {
            keyboard.internalBuffer.expectingLEDWrite = 0;
            keyboard.internalBuffer.keyPressDelay = 1;
            keyboard.internalBuffer.keyRepeatRate = 0x0b;
        }
        logger.log(Level.INFO, "[" + super.getType() + "]" + " AbstractModule has been reset.");
        return true;
    }

    /**
     * Set the keyboard clock, which determines the on/off state of the keyboard<BR>
     *
     * @param state the state of the clock should be set to:<BR>
     *              0: keyboard clock is disabled, turning the keyboard off<BR>
     *              other: keyboard clock is enabled, turning the keyboard on <BR>
     */
    private void setKeyboardClock(boolean state) {
        byte oldKBDClock;
        if (!state) {
            keyboard.controller.kbdClockEnabled = 0;
        } else {
            oldKBDClock = keyboard.controller.kbdClockEnabled;
            keyboard.controller.kbdClockEnabled = 1;
            if (oldKBDClock == 0 && keyboard.controller.outputBuffer == 0) {
                activateTimer();
            }
        }
        logger.log(Level.CONFIG, "[" + super.getType() + "]" + " Keyboard clock " + (state ? "enabled" : "disabled"));
    }

    /**
     * Set the aux device clock, which determines the on/off state of the device<BR>
     *
     * @param state the state of the clock should be set to:<BR>
     *              0: aux device clock is disabled, turning the device off<BR>
     *              other: aux device clock is enabled, turning the device on <BR>
     */
    private void setAuxClock(boolean state) {
        byte oldAuxClock;
        if (!state) {
            keyboard.controller.auxClockEnabled = 0;
        } else {
            oldAuxClock = keyboard.controller.auxClockEnabled;
            keyboard.controller.auxClockEnabled = 1;
            if (oldAuxClock == 0 && keyboard.controller.outputBuffer == 0) {
                activateTimer();
            }
        }
        logger.log(Level.CONFIG, "[" + super.getType() + "]" + " Aux clock " + (state ? "enabled" : "disabled"));
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.module.ModuleKeyboard
     */
    @Override
    public void setTimeOut(byte status) {
        keyboard.controller.timeOut = status;
    }

    /**
     * {@inheritDoc}
     *
     * @see dioscuri.module.ModuleKeyboard
     */
    @Override
    public void enqueueControllerBuffer(byte data, int source) {
        logger.log(Level.INFO, "[" + super.getType() + "]" + " Queueing 0x" + Integer.toHexString(data).toUpperCase() + " in keyboard controller buffer");
        if (keyboard.controller.outputBuffer != 0) {
            if (keyboard.getControllerQueue().size() >= TheKeyboard.CONTROLLER_QUEUE_SIZE) {
                logger.log(Level.WARNING, "[" + super.getType() + "] queueKBControllerBuffer(): Keyboard controller is full!");
            }
            keyboard.getControllerQueue().add(data);
            return;
        }
        if (source == KEYBOARD) {
            logger.log(Level.INFO, "[" + super.getType() + "]" + " source == KEYBOARD");
            keyboard.controller.kbdOutputBuffer = data;
            keyboard.controller.outputBuffer = 1;
            keyboard.controller.auxBuffer = 0;
            keyboard.controller.inputBuffer = 0;
            if (keyboard.controller.allowIRQ1 != 0) {
                keyboard.controller.irq1Requested = 1;
            }
        } else {
            logger.log(Level.INFO, "[" + super.getType() + "]" + " source == MOUSE");
            keyboard.controller.auxOutputBuffer = data;
            keyboard.controller.outputBuffer = 1;
            keyboard.controller.auxBuffer = 1;
            keyboard.controller.inputBuffer = 0;
            if (keyboard.controller.allowIRQ12 != 0) {
                keyboard.controller.irq12Requested = 1;
            }
        }
    }

    /**
     * Queue data in the internal keyboard buffer<BR>
     *
     * @param scancode the data to be added to the end of the queue
     */
    private void enqueueInternalBuffer(byte scancode) {
        logger.log(Level.INFO, "[" + super.getType() + "]" + " enqueueInternalBuffer: 0x" + Integer.toHexString(0x100 | scancode & 0xFF).substring(1).toUpperCase());
        if (keyboard.internalBuffer.getBuffer().size() >= KeyboardInternalBuffer.NUM_ELEMENTS) {
            logger.log(Level.WARNING, "[" + super.getType() + "]" + "internal keyboard buffer full, ignoring scancode " + scancode);
        } else {
            logger.log(Level.INFO, "[" + super.getType() + "]" + " enqueueInternalBuffer: adding scancode " + Integer.toHexString(0x100 | scancode & 0xFF).substring(1).toUpperCase() + "h to internal buffer");
            keyboard.internalBuffer.getBuffer().add(scancode);
            if (!(keyboard.controller.outputBuffer != 0) && (keyboard.controller.kbdClockEnabled != 0)) {
                activateTimer();
                logger.log(Level.INFO, "[" + super.getType() + "]" + " Timer activated");
            }
        }
    }

    /**
     * Activate a 'timer' to indicate to the periodic polling function<BR>
     * the internal keyboard queue should be checked for data.<BR>
     * <BR>
     * <p/>
     * A timer can only be set, not disabled.
     */
    private void activateTimer() {
        if (keyboard.controller.timerPending == 0) {
            keyboard.controller.timerPending = 1;
        }
    }

    /**
     * Keyboard controller polling function<BR>
     * This determines the IRQs to be raised and retrieves character data from
     * the internal keyboard buffer, if available
     */
    private int poll() {
        ModuleMouse mouse = (ModuleMouse) super.getConnection(Module.Type.MOUSE);
        int returnValue;
        returnValue = (keyboard.controller.irq1Requested | (keyboard.controller.irq12Requested << 1));
        keyboard.controller.irq1Requested = 0;
        keyboard.controller.irq12Requested = 0;
        if (keyboard.controller.timerPending == 0) {
            return (returnValue);
        }
        keyboard.controller.timerPending = 0;
        if (keyboard.controller.outputBuffer != 0) {
            logger.log(Level.WARNING, "[" + super.getType() + "]" + " poll(): output buffer is not empty");
            return (returnValue);
        }
        if (!keyboard.internalBuffer.getBuffer().isEmpty() && (keyboard.controller.kbdClockEnabled != 0 || keyboard.controller.batInProgress != 0)) {
            logger.log(Level.WARNING, "[" + super.getType() + "]" + " poll(): key in internal buffer waiting" + this.getDump());
            keyboard.controller.kbdOutputBuffer = keyboard.internalBuffer.getBuffer().remove(0);
            keyboard.controller.outputBuffer = 1;
            if (keyboard.controller.allowIRQ1 != 0) {
                keyboard.controller.irq1Requested = 1;
            }
        } else if (mouse != null) {
            logger.log(Level.INFO, "[" + super.getType() + "]" + " poll()...");
            if (keyboard.controller.auxClockEnabled == 1 && !mouse.isBufferEmpty()) {
                logger.log(Level.WARNING, "[" + super.getType() + "]" + " poll(): mouse event waiting");
                keyboard.controller.auxOutputBuffer = mouse.getDataFromBuffer();
                keyboard.controller.outputBuffer = 1;
                keyboard.controller.auxBuffer = 1;
                if (keyboard.controller.allowIRQ12 == 1) {
                    keyboard.controller.irq12Requested = 1;
                }
            }
        } else {
            logger.log(Level.WARNING, "[" + super.getType() + "]" + " poll(): no keys or mouse events waiting");
        }
        return (returnValue);
    }

    /**
     * Data passing directly from keyboard controller to keyboard<BR>
     * The keyboard usually immediately responds by enqueueing data in its
     * buffer for the keyboard controller<BR>
     *
     * @param value the data passed from controller to keyboard <BR>
     */
    private void dataPortToInternalKB(byte value) {
        logger.log(Level.CONFIG, "[" + super.getType() + "]" + " Controller passing byte " + Integer.toHexString(0x100 | value & 0xFF).substring(1).toUpperCase() + "h directly to keyboard");
        if (keyboard.internalBuffer.expectingTypematic != 0) {
            keyboard.internalBuffer.expectingTypematic = 0;
            keyboard.internalBuffer.keyPressDelay = (byte) ((value >> 5) & 0x03);
            switch(keyboard.internalBuffer.keyPressDelay) {
                case 0:
                    logger.log(Level.INFO, "[" + super.getType() + "]" + " typematic delay (unused) set to 250 ms");
                    break;
                case 1:
                    logger.log(Level.INFO, "[" + super.getType() + "]" + " typematic delay (unused) set to 500 ms");
                    break;
                case 2:
                    logger.log(Level.INFO, "[" + super.getType() + "]" + " typematic delay (unused) set to 750 ms");
                    break;
                case 3:
                    logger.log(Level.INFO, "[" + super.getType() + "]" + " typematic delay (unused) set to 1000 ms");
                    break;
            }
            keyboard.internalBuffer.keyRepeatRate = (byte) (value & 0x1f);
            double cps = 1000 / ((8 + (value & 0x07)) * Math.pow(2, ((value >> 3) & 0x03)) * 4.17);
            DecimalFormat format = new DecimalFormat("##.#");
            logger.log(Level.INFO, "[" + super.getType() + "]" + " Repeat rate (unused) set to " + format.format(cps) + "char. per second");
            enqueueInternalBuffer((byte) 0xFA);
            return;
        }
        if (keyboard.internalBuffer.expectingLEDWrite != 0) {
            keyboard.internalBuffer.expectingLEDWrite = 0;
            keyboard.internalBuffer.ledStatus = value;
            logger.log(Level.CONFIG, "[" + super.getType() + "]" + " Status of LEDs set to " + Integer.toHexString(keyboard.internalBuffer.ledStatus));
            switch(value) {
                case 0x00:
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_NUMLOCK_OFF);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_CAPSLOCK_OFF);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_SCROLLLOCK_OFF);
                    break;
                case 0x01:
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_NUMLOCK_OFF);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_CAPSLOCK_OFF);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_SCROLLLOCK_ON);
                    break;
                case 0x02:
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_NUMLOCK_ON);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_CAPSLOCK_OFF);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_SCROLLLOCK_OFF);
                    break;
                case 0x03:
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_NUMLOCK_ON);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_CAPSLOCK_OFF);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_SCROLLLOCK_ON);
                    break;
                case 0x04:
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_NUMLOCK_OFF);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_CAPSLOCK_ON);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_SCROLLLOCK_OFF);
                    break;
                case 0x05:
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_NUMLOCK_OFF);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_CAPSLOCK_ON);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_SCROLLLOCK_ON);
                    break;
                case 0x06:
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_NUMLOCK_ON);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_CAPSLOCK_ON);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_SCROLLLOCK_OFF);
                    break;
                case 0x07:
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_NUMLOCK_ON);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_CAPSLOCK_ON);
                    emu.statusChanged(Emulator.MODULE_KEYBOARD_SCROLLLOCK_ON);
                    break;
                default:
                    break;
            }
            enqueueInternalBuffer((byte) 0xFA);
            return;
        }
        if (keyboard.internalBuffer.expectingScancodeSet != 0) {
            keyboard.internalBuffer.expectingScancodeSet = 0;
            if (value != 0) {
                if (value < 4) {
                    keyboard.controller.currentScancodeSet = (byte) (value - 1);
                    logger.log(Level.INFO, "[" + super.getType() + "]" + " Switching to scancode set " + Integer.toHexString(keyboard.controller.currentScancodeSet + 1));
                    enqueueInternalBuffer((byte) 0xFA);
                } else {
                    logger.log(Level.WARNING, "[" + super.getType() + "]" + " Scancode set number out of range: " + Integer.toHexString(value).toUpperCase());
                    enqueueInternalBuffer((byte) 0xFF);
                }
            } else {
                enqueueInternalBuffer((byte) 0xFA);
                enqueueInternalBuffer((byte) (1 + (keyboard.controller.currentScancodeSet)));
            }
            return;
        }
        switch(value) {
            case (byte) 0x00:
                enqueueInternalBuffer((byte) 0xFA);
                break;
            case (byte) 0x05:
                keyboard.controller.systemFlag = 1;
                enqueueInternalBuffer((byte) 0xFE);
                break;
            case (byte) 0xD3:
                enqueueInternalBuffer((byte) 0xFA);
                break;
            case (byte) 0xED:
                keyboard.internalBuffer.expectingLEDWrite = 1;
                enqueueInternalBuffer((byte) 0xFA);
                break;
            case (byte) 0xEE:
                enqueueInternalBuffer((byte) 0xEE);
                break;
            case (byte) 0xF0:
                keyboard.internalBuffer.expectingScancodeSet = 1;
                logger.log(Level.INFO, "[" + super.getType() + "]" + " Expecting scancode set information");
                enqueueInternalBuffer((byte) 0xFA);
                break;
            case (byte) 0xF2:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " Read Keyboard ID command received");
                enqueueInternalBuffer((byte) 0xFA);
                enqueueInternalBuffer((byte) 0xAB);
                if (keyboard.controller.translateScancode != 0) enqueueInternalBuffer((byte) 0x41); else enqueueInternalBuffer((byte) 0x83);
                break;
            case (byte) 0xF3:
                keyboard.internalBuffer.expectingTypematic = 1;
                logger.log(Level.INFO, "[" + super.getType() + "]" + " Expecting Typematic Rate/Delay information");
                enqueueInternalBuffer((byte) 0xFA);
                break;
            case (byte) 0xF4:
                keyboard.internalBuffer.scanningEnabled = 1;
                enqueueInternalBuffer((byte) 0xFA);
                break;
            case (byte) 0xF5:
                resetKeyboardBuffer(1);
                enqueueInternalBuffer((byte) 0xFA);
                keyboard.internalBuffer.scanningEnabled = 0;
                logger.log(Level.CONFIG, "[" + super.getType() + "]" + " Reset w/ Disable command received");
                break;
            case (byte) 0xF6:
                resetKeyboardBuffer(1);
                enqueueInternalBuffer((byte) 0xFA);
                keyboard.internalBuffer.scanningEnabled = 1;
                logger.log(Level.CONFIG, "[" + super.getType() + "]" + " Reset w Enable command received");
                break;
            case (byte) 0xFE:
                logger.log(Level.WARNING, "[" + super.getType() + "]" + " Requesting resend: transmission error!!");
                break;
            case (byte) 0xFF:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " Reset w/ BAT command received");
                resetKeyboardBuffer(1);
                enqueueInternalBuffer((byte) 0xFA);
                keyboard.controller.batInProgress = 1;
                enqueueInternalBuffer((byte) 0xAA);
                break;
            case (byte) 0xF7:
            case (byte) 0xF8:
            case (byte) 0xF9:
            case (byte) 0xFA:
            case (byte) 0xFB:
            case (byte) 0xFC:
            case (byte) 0xFD:
            default:
                logger.log(Level.INFO, "[" + super.getType() + "]" + " dataPortToInternalKB(): got value of " + value);
                enqueueInternalBuffer((byte) 0xFE);
                break;
        }
    }
}
