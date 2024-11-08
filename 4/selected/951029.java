package nl.kbna.dioscuri.module.video;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.kbna.dioscuri.Emulator;
import nl.kbna.dioscuri.exception.ModuleException;
import nl.kbna.dioscuri.exception.ModuleUnknownPort;
import nl.kbna.dioscuri.exception.ModuleWriteOnlyPortException;
import nl.kbna.dioscuri.module.Module;
import nl.kbna.dioscuri.module.ModuleCPU;
import nl.kbna.dioscuri.module.ModuleMotherboard;
import nl.kbna.dioscuri.module.ModuleRTC;
import nl.kbna.dioscuri.module.ModuleScreen;
import nl.kbna.dioscuri.module.ModuleVideo;

/**
 * An implementation of a video (VGA) module.
 * Based on Bochs code (http://bochs.sourceforge.net/)
 * 
 * @see Module 
 * 
 * Metadata module 
 * ******************************************** 
 * general.type               : video 
 * general.name               : General VGA display adapter 
 * general.architecture       : Von Neumann 
 * general.description        : Models a simple VGA adapter 
 * general.creator            : Tessella Support Services, Koninklijke Bibliotheek, Nationaal Archief of the Netherlands 
 * general.version            : 1.0 
 * general.keywords           : VGA, Video Graphics Array, video, graphics, 640, 480 
 * general.relations          : Motherboard
 * general.yearOfIntroduction : 
 * general.yearOfEnding       : 
 * general.ancestor           : 
 * general.successor          :
 */
public class Video extends ModuleVideo {

    public VideoCard videocard = new VideoCard();

    private static final int MAX_TEXT_LINES = 100;

    private int initialScreenWidth = 640;

    private int initialScreenHeight = 480;

    int oldScreenWidth = 0;

    int oldScreenHeight = 0;

    int oldMaxScanLine = 0;

    private Emulator emu;

    private String[] moduleConnections = new String[] { "motherboard", "cpu", "screen", "rtc" };

    private ModuleMotherboard motherboard;

    private ModuleCPU cpu;

    private ModuleScreen screen;

    private ModuleRTC rtc;

    private boolean isObserved;

    private boolean debugMode;

    private int updateInterval;

    private static Logger logger = Logger.getLogger("nl.kbna.dioscuri.module.video");

    public static final int MODULE_ID = 1;

    public static final String MODULE_TYPE = "video";

    public static final String MODULE_NAME = "Video Graphics Array (VGA) adapter";

    /**
     * Class constructor
     */
    public Video(Emulator owner) {
        emu = owner;
        videocard.vgaMemReqUpdate = false;
        logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Module created successfully.");
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
     * @param mod Module that is to be connected to this class
     * @return true if connection has been established successfully, false otherwise
     * @see Module
     */
    public boolean setConnection(Module mod) {
        if (mod.getType().equalsIgnoreCase("motherboard")) {
            this.motherboard = (ModuleMotherboard) mod;
            return true;
        }
        if (mod.getType().equalsIgnoreCase("cpu")) {
            this.cpu = (ModuleCPU) mod;
            return true;
        } else if (mod.getType().equals("screen")) {
            this.screen = (ModuleScreen) mod;
            return true;
        } else if (mod.getType().equalsIgnoreCase("rtc")) {
            this.rtc = (ModuleRTC) mod;
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
        if (motherboard != null && screen != null && rtc != null && cpu != null) {
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
        int ioAddress;
        motherboard.setIOPort(0x3B4, this);
        motherboard.setIOPort(0x3B5, this);
        motherboard.setIOPort(0x3BA, this);
        for (ioAddress = 0x3C0; ioAddress <= 0x3CF; ioAddress++) {
            motherboard.setIOPort(ioAddress, this);
        }
        motherboard.setIOPort(0x3D4, this);
        motherboard.setIOPort(0x3D5, this);
        motherboard.setIOPort(0x3DA, this);
        if (!motherboard.requestTimer(this, updateInterval, true)) {
            return false;
        }
        motherboard.setTimerActiveState(this, true);
        videocard.reset();
        for (int y = 0; y < (initialScreenHeight / VideoCard.Y_TILESIZE); y++) for (int x = 0; x < (initialScreenWidth / VideoCard.X_TILESIZE); x++) videocard.setTileUpdate(x, y, false);
        rtc.setCMOSRegister(0x14, (byte) ((rtc.getCMOSRegister(0x14) & 0xCF) | 0x00));
        logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Module has been reset");
        return true;
    }

    /**
     * Starts the module
     * 
     * @see Module
     */
    public void start() {
    }

    /**
     * Stops the module
     * 
     * @see Module
     */
    public void stop() {
    }

    /**
     * Returns the status of observed toggle
     * 
     * @return state of observed toggle
     * @see Module
     */
    public boolean isObserved() {
        return isObserved;
    }

    /**
     * Sets the observed toggle
     * 
     * @param status
     * @see Module
     */
    public void setObserved(boolean status) {
        isObserved = status;
    }

    /**
     * Returns the status of the debug mode toggle
     * 
     * @return state of debug mode toggle
     * @see Module
     */
    public boolean getDebugMode() {
        return debugMode;
    }

    /**
     * Sets the debug mode toggle
     * 
     * @param status
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
     * @return true if data is set successfully, false otherwise
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
     * @return boolean true is successful, false otherwise
     * @see Module
     */
    public boolean setData(String[] data, Module sender) {
        return false;
    }

    /**
     * Returns a dump of this module
     * 
     * @return string
     * @see Module
     */
    public String getDump() {
        String dump = "Video status:\n";
        dump += "Read mode: " + videocard.graphicsController.readMode + "\n";
        dump += "Write mode: " + videocard.graphicsController.writeMode + "\n";
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
     * Refreshes the framebuffer and send redraw to screen
     */
    public void update() {
        int screenHeight;
        int screenWidth;
        if (!videocard.vgaMemReqUpdate) return;
        if (!(videocard.vgaEnabled) || !(videocard.attributeController.paletteAddressSource != 0) || !(videocard.sequencer.synchReset != 0) || !(videocard.sequencer.aSynchReset != 0)) return;
        if (videocard.graphicsController.alphaNumDisable != 0) {
            byte colour;
            int bitNumber, r, c, x, y;
            int byteOffset, startAddress;
            int xc, yc, xti, yti;
            logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " update() in progress; graphics mode");
            startAddress = (((int) videocard.crtControllerRegister.regArray[0x0C] & 0xFF) << 8) | ((int) videocard.crtControllerRegister.regArray[0x0D]) & 0xFF;
            int newHeightWidth[] = new int[2];
            newHeightWidth = determineScreenSize();
            screenHeight = newHeightWidth[0];
            screenWidth = newHeightWidth[1];
            if ((screenWidth != oldScreenWidth) || (screenHeight != oldScreenHeight)) {
                screen.updateScreenSize(screenWidth, screenHeight, 0, 0);
                oldScreenWidth = screenWidth;
                oldScreenHeight = screenHeight;
            }
            switch(videocard.graphicsController.shift256Reg) {
                case 0:
                    byte attribute, palette_reg_val, DAC_regno;
                    int line_compare;
                    int plane0, plane1, plane2, plane3;
                    if (videocard.graphicsController.memoryMapSelect == 3) {
                        for (yc = 0, yti = 0; yc < screenHeight; yc += VideoCard.Y_TILESIZE, yti++) {
                            for (xc = 0, xti = 0; xc < screenWidth; xc += VideoCard.X_TILESIZE, xti++) {
                                if ((videocard.getTileUpdate(xti, yti))) {
                                    for (r = 0; r < VideoCard.Y_TILESIZE; r++) {
                                        y = yc + r;
                                        if (videocard.crtControllerRegister.scanDoubling != 0) y >>= 1;
                                        for (c = 0; c < VideoCard.X_TILESIZE; c++) {
                                            x = xc + c;
                                            byteOffset = startAddress + ((y & 1) << 13);
                                            byteOffset += (320 / 4) * (y / 2);
                                            byteOffset += (x / 8);
                                            bitNumber = 7 - (x % 8);
                                            palette_reg_val = (byte) (((videocard.vgaMemory[byteOffset]) >> bitNumber) & 1);
                                            DAC_regno = videocard.attributeController.paletteRegister[palette_reg_val];
                                            videocard.tile[r * VideoCard.X_TILESIZE + c] = DAC_regno;
                                        }
                                    }
                                    videocard.setTileUpdate(xti, yti, false);
                                    screen.updateGraphicsTile(videocard.tile, xc, yc);
                                }
                            }
                        }
                    } else {
                        {
                            plane0 = 0 << 16;
                            plane1 = 1 << 16;
                            plane2 = 2 << 16;
                            plane3 = 3 << 16;
                            line_compare = videocard.lineCompare;
                            if (videocard.crtControllerRegister.scanDoubling != 0) line_compare >>= 1;
                        }
                        for (yc = 0, yti = 0; yc < screenHeight; yc += VideoCard.Y_TILESIZE, yti++) {
                            for (xc = 0, xti = 0; xc < screenWidth; xc += VideoCard.X_TILESIZE, xti++) {
                                if ((videocard.getTileUpdate(xti, yti))) {
                                    for (r = 0; r < VideoCard.Y_TILESIZE; r++) {
                                        y = yc + r;
                                        if (videocard.crtControllerRegister.scanDoubling != 0) y >>= 1;
                                        for (c = 0; c < VideoCard.X_TILESIZE; c++) {
                                            x = xc + c;
                                            if (videocard.sequencer.dotClockRate != 0) x >>= 1;
                                            bitNumber = 7 - (x % 8);
                                            if (y > line_compare) {
                                                byteOffset = x / 8 + ((y - line_compare - 1) * videocard.lineOffset);
                                            } else {
                                                byteOffset = startAddress + x / 8 + (y * videocard.lineOffset);
                                            }
                                            attribute = (byte) ((((videocard.vgaMemory[plane0 + byteOffset] >> bitNumber) & 0x01) << 0) | (((videocard.vgaMemory[plane1 + byteOffset] >> bitNumber) & 0x01) << 1) | (((videocard.vgaMemory[plane2 + byteOffset] >> bitNumber) & 0x01) << 2) | (((videocard.vgaMemory[plane3 + byteOffset] >> bitNumber) & 0x01) << 3));
                                            attribute &= videocard.attributeController.colourPlaneEnable;
                                            if (videocard.attributeController.modeControlReg.blinkIntensity != 0) attribute ^= 0x08;
                                            palette_reg_val = videocard.attributeController.paletteRegister[attribute];
                                            if (videocard.attributeController.modeControlReg.paletteBitsSelect != 0) {
                                                DAC_regno = (byte) ((palette_reg_val & 0x0f) | (videocard.attributeController.colourSelect << 4));
                                            } else {
                                                DAC_regno = (byte) ((palette_reg_val & 0x3f) | ((videocard.attributeController.colourSelect & 0x0c) << 4));
                                            }
                                            videocard.tile[r * VideoCard.X_TILESIZE + c] = DAC_regno;
                                        }
                                    }
                                    videocard.setTileUpdate(xti, yti, false);
                                    screen.updateGraphicsTile(videocard.tile, xc, yc);
                                }
                            }
                        }
                    }
                    break;
                case 1:
                    for (yc = 0, yti = 0; yc < screenHeight; yc += VideoCard.Y_TILESIZE, yti++) {
                        for (xc = 0, xti = 0; xc < screenWidth; xc += VideoCard.X_TILESIZE, xti++) {
                            if (videocard.getTileUpdate(xti, yti)) {
                                for (r = 0; r < VideoCard.Y_TILESIZE; r++) {
                                    y = yc + r;
                                    if (videocard.crtControllerRegister.scanDoubling != 0) y >>= 1;
                                    for (c = 0; c < VideoCard.X_TILESIZE; c++) {
                                        x = xc + c;
                                        if (videocard.sequencer.dotClockRate != 0) x >>= 1;
                                        byteOffset = startAddress + ((y & 1) << 13);
                                        byteOffset += (320 / 4) * (y / 2);
                                        byteOffset += (x / 4);
                                        attribute = (byte) (6 - 2 * (x % 4));
                                        palette_reg_val = (byte) ((videocard.vgaMemory[byteOffset]) >> attribute);
                                        palette_reg_val &= 3;
                                        DAC_regno = videocard.attributeController.paletteRegister[palette_reg_val];
                                        videocard.tile[r * VideoCard.X_TILESIZE + c] = DAC_regno;
                                    }
                                }
                                videocard.setTileUpdate(xti, yti, false);
                                screen.updateGraphicsTile(videocard.tile, xc, yc);
                            }
                        }
                    }
                    break;
                case 2:
                case 3:
                    if (videocard.sequencer.chainFourEnable != 0) {
                        int pixely, pixelx, plane;
                        if (videocard.miscOutputRegister.lowHighPage != 1) logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " update: select_high_bank != 1");
                        for (yc = 0, yti = 0; yc < screenHeight; yc += VideoCard.Y_TILESIZE, yti++) {
                            for (xc = 0, xti = 0; xc < screenWidth; xc += VideoCard.X_TILESIZE, xti++) {
                                if (videocard.getTileUpdate(xti, yti)) {
                                    for (r = 0; r < VideoCard.Y_TILESIZE; r++) {
                                        pixely = yc + r;
                                        if (videocard.crtControllerRegister.scanDoubling != 0) pixely >>= 1;
                                        for (c = 0; c < VideoCard.X_TILESIZE; c++) {
                                            pixelx = (xc + c) >> 1;
                                            plane = (pixelx % 4);
                                            byteOffset = startAddress + (plane * 65536) + (pixely * videocard.lineOffset) + (pixelx & ~0x03);
                                            colour = videocard.vgaMemory[byteOffset];
                                            videocard.tile[r * VideoCard.X_TILESIZE + c] = colour;
                                        }
                                    }
                                    videocard.setTileUpdate(xti, yti, false);
                                    screen.updateGraphicsTile(videocard.tile, xc, yc);
                                }
                            }
                        }
                    } else {
                        int pixely, pixelx, plane;
                        for (yc = 0, yti = 0; yc < screenHeight; yc += VideoCard.Y_TILESIZE, yti++) {
                            for (xc = 0, xti = 0; xc < screenWidth; xc += VideoCard.X_TILESIZE, xti++) {
                                if (videocard.getTileUpdate(xti, yti)) {
                                    for (r = 0; r < VideoCard.Y_TILESIZE; r++) {
                                        pixely = yc + r;
                                        if (videocard.crtControllerRegister.scanDoubling != 0) pixely >>= 1;
                                        for (c = 0; c < VideoCard.X_TILESIZE; c++) {
                                            pixelx = (xc + c) >> 1;
                                            plane = (pixelx % 4);
                                            byteOffset = (plane * 65536) + (pixely * videocard.lineOffset) + (pixelx >> 2);
                                            colour = videocard.vgaMemory[startAddress + byteOffset];
                                            videocard.tile[r * VideoCard.X_TILESIZE + c] = colour;
                                        }
                                    }
                                    videocard.setTileUpdate(xti, yti, false);
                                    screen.updateGraphicsTile(videocard.tile, xc, yc);
                                }
                            }
                        }
                    }
                    break;
                default:
                    logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " update: shift_reg == " + videocard.graphicsController.shift256Reg);
            }
            videocard.vgaMemReqUpdate = false;
            return;
        } else {
            int cursorXPos;
            int cursorYPos;
            int cursorWidth = ((videocard.sequencer.clockingMode & 0x01) == 1) ? 8 : 9;
            int fullCursorAddress = 2 * ((videocard.crtControllerRegister.regArray[0x0E] << 8) + videocard.crtControllerRegister.regArray[0x0F]);
            int maxScanLine = videocard.crtControllerRegister.regArray[0x09] & 0x1F;
            int numColumns = videocard.crtControllerRegister.regArray[0x01] + 1;
            int numRows;
            TextModeAttributes textModeAttribs = new TextModeAttributes();
            screenWidth = cursorWidth * numColumns;
            screenHeight = videocard.verticalDisplayEnd + 1;
            int fullStartAddress = 2 * ((videocard.crtControllerRegister.regArray[0x0C] << 8) + videocard.crtControllerRegister.regArray[0x0D]);
            logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " update() in progress; text mode");
            textModeAttribs.fullStartAddress = (short) fullStartAddress;
            textModeAttribs.cursorStartLine = (byte) (videocard.crtControllerRegister.regArray[0x0A] & 0x3F);
            textModeAttribs.cursorEndLine = (byte) (videocard.crtControllerRegister.regArray[0x0B] & 0x1F);
            textModeAttribs.lineOffset = (short) (videocard.crtControllerRegister.regArray[0x13] << 2);
            textModeAttribs.lineCompare = (short) (videocard.lineCompare);
            textModeAttribs.horizPanning = (byte) (videocard.attributeController.horizPixelPanning & 0x0F);
            textModeAttribs.vertPanning = (byte) (videocard.crtControllerRegister.regArray[0x08] & 0x1F);
            textModeAttribs.lineGraphics = videocard.attributeController.modeControlReg.lineGraphicsEnable;
            textModeAttribs.splitHorizPanning = videocard.attributeController.modeControlReg.pixelPanningMode;
            if ((videocard.sequencer.clockingMode & 0x01) == 0) {
                if (textModeAttribs.horizPanning >= 8) textModeAttribs.horizPanning = 0; else textModeAttribs.horizPanning++;
            } else {
                textModeAttribs.horizPanning &= 0x07;
            }
            if (maxScanLine == 0) {
                logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " character height = 1, skipping text update");
                return;
            }
            if ((maxScanLine == 1) && (videocard.verticalDisplayEnd == 399)) {
                maxScanLine = 3;
            }
            numRows = (videocard.verticalDisplayEnd + 1) / (maxScanLine + 1);
            if (numRows > MAX_TEXT_LINES) {
                logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " Number of text rows (" + numRows + ") exceeds maximum (" + MAX_TEXT_LINES + ")!");
                return;
            }
            if ((screenWidth != oldScreenWidth) || (screenHeight != oldScreenHeight) || (maxScanLine != oldMaxScanLine)) {
                screen.updateScreenSize(screenWidth, screenHeight, cursorWidth, maxScanLine + 1);
                oldScreenWidth = screenWidth;
                oldScreenHeight = screenHeight;
                oldMaxScanLine = maxScanLine;
            }
            if (fullCursorAddress < fullStartAddress) {
                cursorXPos = 0xFFFF;
                cursorYPos = 0xFFFF;
            } else {
                cursorXPos = ((fullCursorAddress - fullStartAddress) / 2) % (screenWidth / cursorWidth);
                cursorYPos = ((fullCursorAddress - fullStartAddress) / 2) / (screenWidth / cursorWidth);
            }
            screen.updateText(0, fullStartAddress, cursorXPos, cursorYPos, textModeAttribs.getAttributes(), numRows);
            System.arraycopy(videocard.vgaMemory, fullStartAddress, videocard.textSnapshot, 0, 2 * numColumns * numRows);
            videocard.vgaMemReqUpdate = false;
        }
    }

    /**
     * Prepares an area of the screen starting at xOrigin, yOrigin and size width, height<BR>
     * for an update; in graphics mode this sets the corresponding tiles for update, in <BR>
     * text mode this invalidates the whole text snapshot
     * 
     * @param xOrigin   The x coordinate of the upper left value of the region that is updated, in pixels 
     * @param yOrigin   The y coordinate of the upper left value of the region that is updated, in pixels
     * @param width The width of the region to be updated, in pixels
     * @param height    The height of the region to be updated, in pixels
     */
    void setAreaForUpdate(int xOrigin, int yOrigin, int width, int height) {
        int xTileOrigin, yTileOrigin;
        int xTileMax, yTileMax;
        if ((width == 0) || (height == 0)) {
            return;
        }
        videocard.vgaMemReqUpdate = true;
        if (videocard.graphicsController.alphaNumDisable != 0) {
            xTileOrigin = xOrigin / VideoCard.X_TILESIZE;
            yTileOrigin = yOrigin / VideoCard.Y_TILESIZE;
            if (xOrigin < oldScreenWidth) {
                xTileMax = (xOrigin + width - 1) / VideoCard.X_TILESIZE;
            } else {
                xTileMax = (oldScreenWidth - 1) / VideoCard.X_TILESIZE;
            }
            if (yOrigin < oldScreenHeight) {
                yTileMax = (yOrigin + height - 1) / VideoCard.Y_TILESIZE;
            } else {
                yTileMax = (oldScreenHeight - 1) / VideoCard.Y_TILESIZE;
            }
            for (int yCounter = yTileOrigin; yCounter <= yTileMax; yCounter++) {
                for (int xCounter = xTileOrigin; xCounter <= xTileMax; xCounter++) {
                    videocard.setTileUpdate(xCounter, yCounter, true);
                }
            }
        } else {
            Arrays.fill(videocard.textSnapshot, (byte) 0);
        }
    }

    /**
     * IN instruction to video adapter<BR>
     * 
     * @param portAddress the target port; can be any of 0x3B4, 0x3B5, 0x3BA, 0x3C0-0x3CF, 0x3D4, 0x3D4, 0x3DA
     * @return byte from target port
     */
    public byte getIOPortByte(int portAddress) throws ModuleException, ModuleUnknownPort, ModuleWriteOnlyPortException {
        byte returnValue = 0;
        if ((portAddress >= 0x3B0) && (portAddress <= 0x3BF) && (videocard.miscOutputRegister.ioAddressSelect != 0)) {
            return (byte) (0xFF);
        }
        if ((portAddress >= 0x3D0) && (portAddress <= 0x3DF) && (videocard.miscOutputRegister.ioAddressSelect == 0)) {
            return (byte) (0xFF);
        }
        switch(portAddress) {
            case 0x3BA:
            case 0x3CA:
            case 0x3DA:
                long microSeconds;
                short vertResolution;
                videocard.attributeController.dataAddressFlipFlop = false;
                videocard.displayDisabled = 0;
                microSeconds = (long) ((((double) cpu.getCurrentInstructionNumber() / cpu.getIPS())) * 1000000);
                switch((videocard.miscOutputRegister.verticalSyncPol << 1) | videocard.miscOutputRegister.horizontalSyncPol) {
                    case 0:
                        vertResolution = 200;
                        break;
                    case 1:
                        vertResolution = 400;
                        break;
                    case 2:
                        vertResolution = 350;
                        break;
                    default:
                        vertResolution = 480;
                        break;
                }
                if ((microSeconds % 13888) < 70) {
                    videocard.vertRetrace = 1;
                    videocard.displayDisabled = 1;
                } else {
                    videocard.vertRetrace = 0;
                }
                if ((microSeconds % (13888 / vertResolution)) == 0) {
                    videocard.horizRetrace = 1;
                    videocard.displayDisabled = 1;
                } else {
                    videocard.horizRetrace = 0;
                }
                return (byte) (videocard.vertRetrace << 3 | videocard.displayDisabled);
            case 0x3C0:
                if (!videocard.attributeController.dataAddressFlipFlop) {
                    return (byte) ((videocard.attributeController.paletteAddressSource << 5) | videocard.attributeController.index);
                } else {
                    logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " Port [0x3C0] read, but flipflop not set to address mode");
                    return 0;
                }
            case 0x3C1:
                switch(videocard.attributeController.index) {
                    case 0x00:
                    case 0x01:
                    case 0x02:
                    case 0x03:
                    case 0x04:
                    case 0x05:
                    case 0x06:
                    case 0x07:
                    case 0x08:
                    case 0x09:
                    case 0x0A:
                    case 0x0B:
                    case 0x0C:
                    case 0x0D:
                    case 0x0E:
                    case 0x0F:
                        returnValue = videocard.attributeController.paletteRegister[videocard.attributeController.index];
                        return (returnValue);
                    case 0x10:
                        return ((byte) ((videocard.attributeController.modeControlReg.graphicsEnable << 0) | (videocard.attributeController.modeControlReg.monoColourEmu << 1) | (videocard.attributeController.modeControlReg.lineGraphicsEnable << 2) | (videocard.attributeController.modeControlReg.blinkIntensity << 3) | (videocard.attributeController.modeControlReg.pixelPanningMode << 5) | (videocard.attributeController.modeControlReg.colour8Bit << 6) | (videocard.attributeController.modeControlReg.paletteBitsSelect << 7)));
                    case 0x11:
                        return (videocard.attributeController.overscanColour);
                    case 0x12:
                        return (videocard.attributeController.colourPlaneEnable);
                    case 0x13:
                        return (videocard.attributeController.horizPixelPanning);
                    case 0x14:
                        return (videocard.attributeController.colourSelect);
                    default:
                        logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " Port [0x3C1] reads unknown register 0x" + Integer.toHexString(videocard.attributeController.index).toUpperCase());
                        return 0;
                }
            case 0x3C2:
                logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Port [0x3C1] reads Input Status #0; ignored");
                return 0;
            case 0x3C3:
                return (videocard.vgaEnabled) ? (byte) 1 : 0;
            case 0x3C4:
                return (videocard.sequencer.index);
            case 0x3C5:
                switch(videocard.sequencer.index) {
                    case 0:
                        logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Port [0x3C5] reads sequencer reset");
                        return (byte) (videocard.sequencer.aSynchReset | (videocard.sequencer.synchReset << 1));
                    case 1:
                        logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Port [0x3C5] reads sequencer clocking mode");
                        return (videocard.sequencer.clockingMode);
                    case 2:
                        return (videocard.sequencer.mapMask);
                    case 3:
                        return (videocard.sequencer.characterMapSelect);
                    case 4:
                        return ((byte) ((videocard.sequencer.extendedMemory << 1) | (videocard.sequencer.oddEvenDisable << 2) | (videocard.sequencer.chainFourEnable << 3)));
                    default:
                        logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " Port [0x3C5] reads unknown register 0x" + Integer.toHexString(videocard.sequencer.index).toUpperCase());
                        return 0;
                }
            case 0x3C6:
                return (videocard.colourRegister.pixelMask);
            case 0x3C7:
                return (videocard.colourRegister.dacState);
            case 0x3C8:
                return (videocard.colourRegister.dacWriteAddress);
            case 0x3C9:
                if (videocard.colourRegister.dacState == 0x03) {
                    switch(videocard.colourRegister.dacReadCounter) {
                        case 0:
                            returnValue = videocard.pixels[((int) videocard.colourRegister.dacReadAddress) & 0xFF].red;
                            break;
                        case 1:
                            returnValue = videocard.pixels[((int) videocard.colourRegister.dacReadAddress) & 0xFF].green;
                            break;
                        case 2:
                            returnValue = videocard.pixels[((int) videocard.colourRegister.dacReadAddress) & 0xFF].blue;
                            break;
                        default:
                            returnValue = 0;
                    }
                    videocard.colourRegister.dacReadCounter++;
                    if (videocard.colourRegister.dacReadCounter >= 3) {
                        videocard.colourRegister.dacReadCounter = 0;
                        videocard.colourRegister.dacReadAddress++;
                    }
                } else {
                    returnValue = 0x3F;
                }
                return (returnValue);
            case 0x3CC:
                return ((byte) (((videocard.miscOutputRegister.ioAddressSelect & 0x01) << 0) | ((videocard.miscOutputRegister.ramEnable & 0x01) << 1) | ((videocard.miscOutputRegister.clockSelect & 0x03) << 2) | ((videocard.miscOutputRegister.lowHighPage & 0x01) << 5) | ((videocard.miscOutputRegister.horizontalSyncPol & 0x01) << 6) | ((videocard.miscOutputRegister.verticalSyncPol & 0x01) << 7)));
            case 0x3CD:
                logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Port [0x3CD] read; unknown register, returned 0x00");
                return 0x00;
            case 0x3CE:
                return (byte) (videocard.graphicsController.index);
            case 0x3CF:
                switch(videocard.graphicsController.index) {
                    case 0:
                        return (videocard.graphicsController.setReset);
                    case 1:
                        return (videocard.graphicsController.enableSetReset);
                    case 2:
                        return (videocard.graphicsController.colourCompare);
                    case 3:
                        return ((byte) (((videocard.graphicsController.dataOperation & 0x03) << 3) | ((videocard.graphicsController.dataRotate & 0x07) << 0)));
                    case 4:
                        return (videocard.graphicsController.readMapSelect);
                    case 5:
                        returnValue = (byte) (((videocard.graphicsController.shift256Reg & 0x03) << 5) | ((videocard.graphicsController.hostOddEvenEnable & 0x01) << 4) | ((videocard.graphicsController.readMode & 0x01) << 3) | ((videocard.graphicsController.writeMode & 0x03) << 0));
                        if (videocard.graphicsController.hostOddEvenEnable != 0 || videocard.graphicsController.shift256Reg != 0) logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " io read 0x3cf: reg 05 = " + returnValue);
                        return (returnValue);
                    case 6:
                        return ((byte) (((videocard.graphicsController.memoryMapSelect & 0x03) << 2) | ((videocard.graphicsController.hostOddEvenEnable & 0x01) << 1) | ((videocard.graphicsController.alphaNumDisable & 0x01) << 0)));
                    case 7:
                        return (videocard.graphicsController.colourDontCare);
                    case 8:
                        return (videocard.graphicsController.bitMask);
                    default:
                        logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " Port [0x3CF] reads unknown register 0x" + Integer.toHexString(videocard.graphicsController.index).toUpperCase());
                        return (0);
                }
            case 0x3D4:
                return (videocard.crtControllerRegister.index);
            case 0x3B5:
            case 0x3D5:
                if (videocard.crtControllerRegister.index > 0x18) {
                    logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Port [0x" + Integer.toHexString(portAddress).toUpperCase() + "] reads unknown register 0x" + Integer.toHexString(videocard.crtControllerRegister.index).toUpperCase());
                    return (0);
                }
                return videocard.crtControllerRegister.regArray[videocard.crtControllerRegister.index];
            case 0x3B4:
            case 0x3CB:
            default:
                logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " Port [0x" + Integer.toHexString(portAddress).toUpperCase() + "] read; unknown register, returned 0x00");
                return 0;
        }
    }

    /**
     * OUT instruction to video adapter<BR>
     * 
     * @param portAddress the target port<BR>
     * @param data  Value written to the selected port
     */
    public void setIOPortByte(int portAddress, byte data) throws ModuleException, ModuleUnknownPort {
        boolean needUpdate = false;
        if ((videocard.miscOutputRegister.ioAddressSelect != 0) && (portAddress >= 0x3B0) && (portAddress <= 0x03BF)) return;
        if ((videocard.miscOutputRegister.ioAddressSelect == 0) && (portAddress >= 0x03D0) && (portAddress <= 0x03DF)) return;
        switch(portAddress) {
            case 0x3BA:
                logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " I/O write port 0x3BA (Feature Control Register, monochrome): reserved");
                break;
            case 0x3C0:
                if (videocard.attributeController.dataAddressFlipFlop) {
                    switch(videocard.attributeController.index) {
                        case 0x00:
                        case 0x01:
                        case 0x02:
                        case 0x03:
                        case 0x04:
                        case 0x05:
                        case 0x06:
                        case 0x07:
                        case 0x08:
                        case 0x09:
                        case 0x0A:
                        case 0x0B:
                        case 0x0C:
                        case 0x0D:
                        case 0x0E:
                        case 0x0F:
                            if (data != videocard.attributeController.paletteRegister[videocard.attributeController.index]) {
                                videocard.attributeController.paletteRegister[videocard.attributeController.index] = data;
                                needUpdate = true;
                            }
                            break;
                        case 0x10:
                            byte oldLineGraphics = videocard.attributeController.modeControlReg.lineGraphicsEnable;
                            byte oldPaletteBitsSelect = videocard.attributeController.modeControlReg.paletteBitsSelect;
                            videocard.attributeController.modeControlReg.graphicsEnable = (byte) ((data >> 0) & 0x01);
                            videocard.attributeController.modeControlReg.monoColourEmu = (byte) ((data >> 1) & 0x01);
                            videocard.attributeController.modeControlReg.lineGraphicsEnable = (byte) ((data >> 2) & 0x01);
                            videocard.attributeController.modeControlReg.blinkIntensity = (byte) ((data >> 3) & 0x01);
                            videocard.attributeController.modeControlReg.pixelPanningMode = (byte) ((data >> 5) & 0x01);
                            videocard.attributeController.modeControlReg.colour8Bit = (byte) ((data >> 6) & 0x01);
                            videocard.attributeController.modeControlReg.paletteBitsSelect = (byte) ((data >> 7) & 0x01);
                            if (videocard.attributeController.modeControlReg.lineGraphicsEnable != oldLineGraphics) {
                                screen.updateCodePage(0x20000 + videocard.sequencer.charMapAddress);
                                videocard.vgaMemReqUpdate = true;
                            }
                            if (videocard.attributeController.modeControlReg.paletteBitsSelect != oldPaletteBitsSelect) {
                                needUpdate = true;
                            }
                            logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "I/O write port 0x3C0: Mode control: " + data);
                            break;
                        case 0x11:
                            videocard.attributeController.overscanColour = (byte) (data & 0x3f);
                            logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "I/O write port 0x3C0: Overscan colour = " + data);
                            break;
                        case 0x12:
                            videocard.attributeController.colourPlaneEnable = (byte) (data & 0x0f);
                            needUpdate = true;
                            logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "I/O write port 0x3C0: Colour plane enable = " + data);
                            break;
                        case 0x13:
                            videocard.attributeController.horizPixelPanning = (byte) (data & 0x0f);
                            needUpdate = true;
                            logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "I/O write port 0x3C0: Horiz. pixel panning = " + data);
                            break;
                        case 0x14:
                            videocard.attributeController.colourSelect = (byte) (data & 0x0f);
                            needUpdate = true;
                            logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "I/O write port 0x3C0: Colour select = " + videocard.attributeController.colourSelect);
                            break;
                        default:
                            logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + "I/O write port 0x3C0: Data mode (unknown register) " + videocard.attributeController.index);
                    }
                } else {
                    int oldPaletteAddressSource = videocard.attributeController.paletteAddressSource;
                    videocard.attributeController.paletteAddressSource = (byte) ((data >> 5) & 0x01);
                    logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "I/O write port 0x3C0: address mode = " + videocard.attributeController.paletteAddressSource);
                    if (videocard.attributeController.paletteAddressSource == 0) screen.clearScreen(); else if (!(oldPaletteAddressSource != 0)) {
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "found enable transition");
                        needUpdate = true;
                    }
                    data &= 0x1F;
                    videocard.attributeController.index = data;
                    switch(data) {
                        case 0x00:
                        case 0x01:
                        case 0x02:
                        case 0x03:
                        case 0x04:
                        case 0x05:
                        case 0x06:
                        case 0x07:
                        case 0x08:
                        case 0x09:
                        case 0x0A:
                        case 0x0B:
                        case 0x0C:
                        case 0x0D:
                        case 0x0E:
                        case 0x0F:
                            break;
                        default:
                            logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "I/O write port 0x3C0: Address mode reg = " + data);
                    }
                }
                videocard.attributeController.dataAddressFlipFlop = !videocard.attributeController.dataAddressFlipFlop;
                break;
            case 0x3C2:
                videocard.miscOutputRegister.ioAddressSelect = (byte) ((data >> 0) & 0x01);
                videocard.miscOutputRegister.ramEnable = (byte) ((data >> 1) & 0x01);
                videocard.miscOutputRegister.clockSelect = (byte) ((data >> 2) & 0x03);
                videocard.miscOutputRegister.lowHighPage = (byte) ((data >> 5) & 0x01);
                videocard.miscOutputRegister.horizontalSyncPol = (byte) ((data >> 6) & 0x01);
                videocard.miscOutputRegister.verticalSyncPol = (byte) ((data >> 7) & 0x01);
                logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " I/O write port 0x3C2:");
                logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "  I/O Address select  = " + videocard.miscOutputRegister.ioAddressSelect);
                logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "  Ram Enable          = " + videocard.miscOutputRegister.ramEnable);
                logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "  Clock Select        = " + videocard.miscOutputRegister.clockSelect);
                logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "  Low/High Page       = " + videocard.miscOutputRegister.lowHighPage);
                logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "  Horiz Sync Polarity = " + videocard.miscOutputRegister.horizontalSyncPol);
                logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "  Vert Sync Polarity  = " + videocard.miscOutputRegister.verticalSyncPol);
                break;
            case 0x3C3:
                videocard.vgaEnabled = (data & 0x01) == 1 ? true : false;
                logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " set I/O port 0x3C3: VGA Enabled = " + videocard.vgaEnabled);
                break;
            case 0x3C4:
                if (data > 4) {
                    logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " I/O write port 0x3C4: index > 4");
                }
                videocard.sequencer.index = data;
                break;
            case 0x3C5:
                switch(videocard.sequencer.index) {
                    case 0:
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " I/O write 0x3C5: Sequencer reset:  " + data);
                        if ((videocard.sequencer.aSynchReset != 0) && ((data & 0x01) == 0)) {
                            videocard.sequencer.characterMapSelect = 0;
                            videocard.sequencer.charMapAddress = 0;
                            screen.updateCodePage(0x20000 + videocard.sequencer.charMapAddress);
                            videocard.vgaMemReqUpdate = true;
                        }
                        videocard.sequencer.aSynchReset = (byte) ((data >> 0) & 0x01);
                        videocard.sequencer.synchReset = (byte) ((data >> 1) & 0x01);
                        break;
                    case 1:
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "I/O write port 0x3C5 (clocking mode): " + data);
                        videocard.sequencer.clockingMode = (byte) (data & 0x3D);
                        videocard.sequencer.dotClockRate = ((data & 0x08) > 0) ? (byte) 1 : 0;
                        break;
                    case 2:
                        videocard.sequencer.mapMask = (byte) (data & 0x0F);
                        for (int i = 0; i < 4; i++) videocard.sequencer.mapMaskArray[i] = (byte) ((data >> i) & 0x01);
                        break;
                    case 3:
                        videocard.sequencer.characterMapSelect = (byte) (data & 0x3F);
                        byte charSetA = (byte) (data & 0x13);
                        if (charSetA > 3) charSetA = (byte) ((charSetA & 3) + 4);
                        byte charSetB = (byte) ((data & 0x2C) >> 2);
                        if (charSetB > 3) charSetB = (byte) ((charSetB & 3) + 4);
                        if (videocard.crtControllerRegister.regArray[0x09] != 0) {
                            videocard.sequencer.charMapAddress = SequencerRegister.charMapOffset[charSetA];
                            screen.updateCodePage(0x20000 + videocard.sequencer.charMapAddress);
                            videocard.vgaMemReqUpdate = true;
                        }
                        if (charSetB != charSetA) logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + "Character map select: map #2 in block " + charSetB + " unused");
                        break;
                    case 4:
                        videocard.sequencer.extendedMemory = (byte) ((data >> 1) & 0x01);
                        videocard.sequencer.oddEvenDisable = (byte) ((data >> 2) & 0x01);
                        videocard.sequencer.chainFourEnable = (byte) ((data >> 3) & 0x01);
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " I/O write port 0x3C5 (memory mode):");
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "  Extended Memory  = " + videocard.sequencer.extendedMemory);
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "  Odd/Even disable = " + videocard.sequencer.oddEvenDisable);
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "  Chain 4 enable   = " + videocard.sequencer.chainFourEnable);
                        break;
                    default:
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "I/O write port 0x3C5: index " + videocard.sequencer.index + " unhandled");
                }
                break;
            case 0x3C6:
                videocard.colourRegister.pixelMask = data;
                if (videocard.colourRegister.pixelMask != (byte) 0xFF) logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " I/O write port 0x3C6: Pixel mask= " + data + " != 0xFF");
                break;
            case 0x3C7:
                videocard.colourRegister.dacReadAddress = data;
                videocard.colourRegister.dacReadCounter = 0;
                videocard.colourRegister.dacState = 0x03;
                break;
            case 0x3C8:
                videocard.colourRegister.dacWriteAddress = data;
                videocard.colourRegister.dacWriteCounter = 0;
                videocard.colourRegister.dacState = 0x00;
                break;
            case 0x3C9:
                switch(videocard.colourRegister.dacWriteCounter) {
                    case 0:
                        videocard.pixels[(((int) videocard.colourRegister.dacWriteAddress) & 0xFF)].red = data;
                        break;
                    case 1:
                        videocard.pixels[(((int) videocard.colourRegister.dacWriteAddress) & 0xFF)].green = data;
                        break;
                    case 2:
                        videocard.pixels[(((int) videocard.colourRegister.dacWriteAddress) & 0xFF)].blue = data;
                        needUpdate |= screen.setPaletteColour(videocard.colourRegister.dacWriteAddress, (videocard.pixels[(((int) videocard.colourRegister.dacWriteAddress) & 0xFF)].red) << 2, (videocard.pixels[(((int) videocard.colourRegister.dacWriteAddress) & 0xFF)].green) << 2, (videocard.pixels[(((int) videocard.colourRegister.dacWriteAddress) & 0xFF)].blue) << 2);
                        break;
                }
                videocard.colourRegister.dacWriteCounter++;
                if (videocard.colourRegister.dacWriteCounter >= 3) {
                    videocard.colourRegister.dacWriteCounter = 0;
                    videocard.colourRegister.dacWriteAddress++;
                }
                break;
            case 0x3CA:
                break;
            case 0x3CC:
                break;
            case 0x3CD:
                logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " I/O write to unknown port 0x3CD = " + data);
                break;
            case 0x3CE:
                if (data > 0x08) logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " /O write port 0x3CE: index > 8");
                videocard.graphicsController.index = data;
                break;
            case 0x3CF:
                switch(videocard.graphicsController.index) {
                    case 0:
                        videocard.graphicsController.setReset = (byte) (data & 0x0F);
                        break;
                    case 1:
                        videocard.graphicsController.enableSetReset = (byte) (data & 0x0F);
                        break;
                    case 2:
                        videocard.graphicsController.colourCompare = (byte) (data & 0x0F);
                        break;
                    case 3:
                        videocard.graphicsController.dataRotate = (byte) (data & 0x07);
                        videocard.graphicsController.dataOperation = (byte) ((data >> 3) & 0x03);
                        break;
                    case 4:
                        videocard.graphicsController.readMapSelect = (byte) (data & 0x03);
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "I/O write port 0x3CF (Read Map Select): " + data);
                        break;
                    case 5:
                        videocard.graphicsController.writeMode = (byte) (data & 0x03);
                        videocard.graphicsController.readMode = (byte) ((data >> 3) & 0x01);
                        videocard.graphicsController.hostOddEvenEnable = (byte) ((data >> 4) & 0x01);
                        videocard.graphicsController.shift256Reg = (byte) ((data >> 5) & 0x03);
                        if (videocard.graphicsController.hostOddEvenEnable != 0) logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "I/O write port 0x3CF (graphics mode): value = " + data);
                        if (videocard.graphicsController.shift256Reg != 0) logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "I/O write port 0x3CF (graphics mode): value = " + data);
                        break;
                    case 6:
                        byte oldAlphaNumDisable = videocard.graphicsController.alphaNumDisable;
                        byte oldMemoryMapSelect = videocard.graphicsController.memoryMapSelect;
                        videocard.graphicsController.alphaNumDisable = (byte) (data & 0x01);
                        videocard.graphicsController.chainOddEvenEnable = (byte) ((data >> 1) & 0x01);
                        videocard.graphicsController.memoryMapSelect = (byte) ((data >> 2) & 0x03);
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " I/O write port 0x3CF (Miscellaneous): " + data);
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "  Alpha Num Disable: " + videocard.graphicsController.alphaNumDisable);
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "  Memory map select: " + videocard.graphicsController.memoryMapSelect);
                        logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + "  Odd/Even enable  : " + videocard.graphicsController.hostOddEvenEnable);
                        if (oldMemoryMapSelect != videocard.graphicsController.memoryMapSelect) needUpdate = true;
                        if (oldAlphaNumDisable != videocard.graphicsController.alphaNumDisable) {
                            needUpdate = true;
                            oldScreenHeight = 0;
                        }
                        break;
                    case 7:
                        videocard.graphicsController.colourDontCare = (byte) (data & 0x0F);
                        break;
                    case 8:
                        videocard.graphicsController.bitMask = data;
                        break;
                    default:
                        logger.log(Level.WARNING, "[" + MODULE_TYPE + "]" + " I/O write port 0x3CF: index " + videocard.graphicsController.index + " unhandled");
                }
                break;
            case 0x3B4:
            case 0x3D4:
                videocard.crtControllerRegister.index = (byte) (data & 0x7F);
                if (videocard.crtControllerRegister.index > 0x18) logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " I/O write port 0x3(B|D)4: invalid CRTC register " + videocard.crtControllerRegister.index + " selected");
                break;
            case 0x3B5:
            case 0x3D5:
                if (videocard.crtControllerRegister.index > 0x18) {
                    logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + "  I/O write port 0x3(B|D)5: invalid CRTC Register (" + videocard.crtControllerRegister.index + "); ignored");
                    return;
                }
                if ((videocard.crtControllerRegister.protectEnable) && (videocard.crtControllerRegister.index < 0x08)) {
                    if (videocard.crtControllerRegister.index == 0x07) {
                        videocard.crtControllerRegister.regArray[videocard.crtControllerRegister.index] &= ~0x10;
                        videocard.lineCompare &= 0x2ff;
                        videocard.crtControllerRegister.regArray[videocard.crtControllerRegister.index] |= (data & 0x10);
                        if ((videocard.crtControllerRegister.regArray[0x07] & 0x10) != 0) videocard.lineCompare |= 0x100;
                        needUpdate = true;
                        break;
                    } else {
                        return;
                    }
                }
                if (data != videocard.crtControllerRegister.regArray[videocard.crtControllerRegister.index]) {
                    videocard.crtControllerRegister.regArray[videocard.crtControllerRegister.index] = data;
                    switch(videocard.crtControllerRegister.index) {
                        case 0x07:
                            videocard.verticalDisplayEnd &= 0xFF;
                            videocard.lineCompare &= 0x2FF;
                            if ((videocard.crtControllerRegister.regArray[0x07] & 0x02) != 0) videocard.verticalDisplayEnd |= 0x100;
                            if ((videocard.crtControllerRegister.regArray[0x07] & 0x40) != 0) videocard.verticalDisplayEnd |= 0x200;
                            if ((videocard.crtControllerRegister.regArray[0x07] & 0x10) != 0) videocard.lineCompare |= 0x100;
                            needUpdate = true;
                            break;
                        case 0x08:
                            needUpdate = true;
                            break;
                        case 0x09:
                            videocard.crtControllerRegister.scanDoubling = ((data & 0x9F) > 0) ? (byte) 1 : 0;
                            videocard.lineCompare &= 0x1FF;
                            if ((videocard.crtControllerRegister.regArray[0x09] & 0x40) != 0) videocard.lineCompare |= 0x200;
                            needUpdate = true;
                            break;
                        case 0x0A:
                        case 0x0B:
                        case 0x0E:
                        case 0x0F:
                            videocard.vgaMemReqUpdate = true;
                            break;
                        case 0x0C:
                        case 0x0D:
                            if (videocard.graphicsController.alphaNumDisable != 0) {
                                needUpdate = true;
                            } else {
                                videocard.vgaMemReqUpdate = true;
                            }
                            break;
                        case 0x11:
                            videocard.crtControllerRegister.protectEnable = ((videocard.crtControllerRegister.regArray[0x11] & 0x80) > 0) ? true : false;
                            break;
                        case 0x12:
                            videocard.verticalDisplayEnd &= 0x300;
                            videocard.verticalDisplayEnd |= (((int) videocard.crtControllerRegister.regArray[0x12]) & 0xFF);
                            break;
                        case 0x13:
                        case 0x14:
                        case 0x17:
                            videocard.lineOffset = videocard.crtControllerRegister.regArray[0x13] << 1;
                            if ((videocard.crtControllerRegister.regArray[0x14] & 0x40) != 0) {
                                videocard.lineOffset <<= 2;
                            } else if ((videocard.crtControllerRegister.regArray[0x17] & 0x40) == 0) {
                                videocard.lineOffset <<= 1;
                            }
                            needUpdate = true;
                            break;
                        case 0x18:
                            videocard.lineCompare &= 0x300;
                            videocard.lineCompare |= (((short) videocard.crtControllerRegister.regArray[0x18]) & 0xFF);
                            needUpdate = true;
                            break;
                    }
                }
                break;
            case 0x3Da:
                logger.log(Level.CONFIG, "[" + MODULE_TYPE + "]" + " I/O write port 0x3DA (Feature Control Register, colour): reserved");
                break;
            case 0x03C1:
                break;
            default:
                logger.log(Level.INFO, "[" + MODULE_TYPE + "]" + " unsupported I/O write to port " + portAddress + ", data =" + data);
        }
        if (needUpdate) {
            setAreaForUpdate(0, 0, oldScreenWidth, oldScreenHeight);
        }
        return;
    }

    public byte[] getIOPortWord(int portAddress) throws ModuleException, ModuleUnknownPort, ModuleWriteOnlyPortException {
        return null;
    }

    public void setIOPortWord(int portAddress, byte[] dataWord) throws ModuleException, ModuleUnknownPort {
        setIOPortByte(portAddress, (byte) (dataWord[1] & 0xff));
        setIOPortByte(portAddress + 1, (byte) (dataWord[0] & 0xff));
        return;
    }

    public byte[] getIOPortDoubleWord(int portAddress) throws ModuleException, ModuleUnknownPort, ModuleWriteOnlyPortException {
        return null;
    }

    public void setIOPortDoubleWord(int portAddress, byte[] dataDoubleWord) throws ModuleException, ModuleUnknownPort {
        return;
    }

    public byte[] getVideoBuffer() {
        return this.videocard.vgaMemory;
    }

    public byte getVideoBufferByte(int index) {
        return this.videocard.vgaMemory[index];
    }

    public void setVideoBufferByte(int index, byte data) {
        this.videocard.vgaMemory[index] = data;
    }

    public byte getTextSnapshot(int index) {
        return this.videocard.textSnapshot[index];
    }

    public void setTextSnapshot(int index, byte data) {
        this.videocard.textSnapshot[index] = data;
    }

    /**
     * Determine the screen size in pixels
     * 
     * @return integer array containing [height, width] of screen in pixels
     */
    public int[] determineScreenSize() {
        int heightInPixels, widthInPixels;
        int horizontal, vertical;
        horizontal = ((((int) videocard.crtControllerRegister.regArray[1]) & 0xFF) + 1) * 8;
        vertical = ((((int) videocard.crtControllerRegister.regArray[18]) & 0xFF) | (((((int) videocard.crtControllerRegister.regArray[7]) & 0xFF) & 0x02) << 7) | (((((int) videocard.crtControllerRegister.regArray[7]) & 0xFF) & 0x40) << 3)) + 1;
        if (videocard.graphicsController.shift256Reg == 0) {
            widthInPixels = 640;
            heightInPixels = 480;
            if (videocard.crtControllerRegister.regArray[0x06] == (byte) 0xBF) {
                if (videocard.crtControllerRegister.regArray[0x17] == (byte) 0xA3 && videocard.crtControllerRegister.regArray[0x14] == (byte) 0x40 && videocard.crtControllerRegister.regArray[0x09] == (byte) 0x41) {
                    widthInPixels = 320;
                    heightInPixels = 240;
                } else {
                    if (videocard.sequencer.dotClockRate != 0) {
                        horizontal <<= 1;
                    }
                    widthInPixels = horizontal;
                    heightInPixels = vertical;
                }
            } else if ((horizontal >= 640) && (vertical >= 480)) {
                widthInPixels = horizontal;
                heightInPixels = vertical;
            }
        } else if (videocard.graphicsController.shift256Reg == 2) {
            if (videocard.sequencer.chainFourEnable != 0) {
                widthInPixels = horizontal;
                heightInPixels = vertical;
            } else {
                widthInPixels = horizontal;
                heightInPixels = vertical;
            }
        } else {
            if (videocard.sequencer.dotClockRate != 0) horizontal <<= 1;
            widthInPixels = horizontal;
            heightInPixels = vertical;
        }
        return new int[] { heightInPixels, widthInPixels };
    }

    /**
     *  Translate the text attribute/graphic colour input value into the CRT display colour
     */
    public byte getAttributePaletteRegister(int index) {
        return videocard.attributeController.paletteRegister[index];
    }

    /**
     * VGA memory Read Modes 0 and 1 functionality
     */
    public byte readMode(int address) {
        int i;
        int offset;
        int[] plane = new int[4];
        switch(videocard.graphicsController.memoryMapSelect) {
            case 1:
                if (address > 0xAFFFF) {
                    return (byte) 0xFF;
                }
                offset = address - 0xA0000;
                break;
            case 2:
                if ((address < 0xB0000) || (address > 0xB7FFF)) {
                    return (byte) 0xFF;
                }
                return videocard.vgaMemory[address - 0xB0000];
            case 3:
                if (address < 0xB8000) {
                    return (byte) 0xFF;
                }
                return videocard.vgaMemory[address - 0xB8000];
            default:
                return videocard.vgaMemory[address - 0xA0000];
        }
        if (videocard.sequencer.chainFourEnable != 0) {
            return videocard.vgaMemory[(offset & ~0x03) + (offset % 4) * 65536];
        }
        for (i = 0; i < 4; i++) {
            plane[i] = i << 16;
        }
        switch(videocard.graphicsController.readMode) {
            case 0:
                for (i = 0; i < 4; i++) {
                    videocard.graphicsController.latch[i] = videocard.vgaMemory[plane[i] + offset];
                }
                return (videocard.graphicsController.latch[videocard.graphicsController.readMapSelect]);
            case 1:
                {
                    byte colourCompare, colourDontCare;
                    byte[] latch = new byte[4];
                    byte returnValue;
                    colourCompare = (byte) (videocard.graphicsController.colourCompare & 0x0F);
                    colourDontCare = (byte) (videocard.graphicsController.colourDontCare & 0x0F);
                    for (i = 0; i < 4; i++) {
                        latch[i] = videocard.graphicsController.latch[i] = videocard.vgaMemory[plane[i] + offset];
                        latch[i] ^= GraphicsController.colourCompareTable[colourCompare][i];
                        latch[i] &= GraphicsController.colourCompareTable[colourDontCare][i];
                    }
                    returnValue = (byte) ~(latch[0] | latch[1] | latch[2] | latch[3]);
                    return returnValue;
                }
            default:
                return 0;
        }
    }

    /**
     * VGA memory Write Modes 0, 1, 2 and 3 functionality
     */
    public void writeMode(int address, byte value) {
        int offset;
        byte newValue[] = new byte[4];
        int startAddress;
        int plane0, plane1, plane2, plane3;
        switch(videocard.graphicsController.memoryMapSelect) {
            case 1:
                if (address > 0xAFFFF) return;
                offset = address - 0xA0000;
                break;
            case 2:
                if ((address < 0xB0000) || (address > 0xB7FFF)) return;
                offset = address - 0xB0000;
                break;
            case 3:
                if (address < 0xB8000) return;
                offset = address - 0xB8000;
                break;
            default:
                offset = address - 0xA0000;
        }
        startAddress = (videocard.crtControllerRegister.regArray[0x0C] << 8) | videocard.crtControllerRegister.regArray[0x0D];
        if (videocard.graphicsController.alphaNumDisable != 0) {
            if (videocard.graphicsController.memoryMapSelect == 3) {
                int x_tileno, x_tileno2, y_tileno;
                videocard.vgaMemory[offset] = value;
                offset -= startAddress;
                if (offset >= 0x2000) {
                    y_tileno = offset - 0x2000;
                    y_tileno /= (320 / 4);
                    y_tileno <<= 1;
                    y_tileno++;
                    x_tileno = (offset - 0x2000) % (320 / 4);
                    x_tileno <<= 2;
                } else {
                    y_tileno = offset / (320 / 4);
                    y_tileno <<= 1;
                    x_tileno = offset % (320 / 4);
                    x_tileno <<= 2;
                }
                x_tileno2 = x_tileno;
                if (videocard.graphicsController.shift256Reg == 0) {
                    x_tileno *= 2;
                    x_tileno2 += 7;
                } else {
                    x_tileno2 += 3;
                }
                if (videocard.sequencer.dotClockRate != 0) {
                    x_tileno /= (VideoCard.X_TILESIZE / 2);
                    x_tileno2 /= (VideoCard.X_TILESIZE / 2);
                } else {
                    x_tileno /= VideoCard.X_TILESIZE;
                    x_tileno2 /= VideoCard.X_TILESIZE;
                }
                if (videocard.crtControllerRegister.scanDoubling != 0) {
                    y_tileno /= (VideoCard.Y_TILESIZE / 2);
                } else {
                    y_tileno /= VideoCard.Y_TILESIZE;
                }
                videocard.vgaMemReqUpdate = true;
                videocard.setTileUpdate(x_tileno, y_tileno, true);
                if (x_tileno2 != x_tileno) {
                    videocard.setTileUpdate(x_tileno2, y_tileno, true);
                }
                return;
            } else if (videocard.graphicsController.memoryMapSelect != 1) {
                logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " mem_write: graphics: mapping = " + videocard.graphicsController.memoryMapSelect);
                return;
            }
            if (videocard.sequencer.chainFourEnable != 0) {
                int x_tileno, y_tileno;
                videocard.vgaMemory[(offset & ~0x03) + (offset % 4) * 65536] = value;
                if (videocard.lineOffset > 0) {
                    offset -= startAddress;
                    x_tileno = (offset % videocard.lineOffset) / (VideoCard.X_TILESIZE / 2);
                    if (videocard.crtControllerRegister.scanDoubling != 0) {
                        y_tileno = (offset / videocard.lineOffset) / (VideoCard.Y_TILESIZE / 2);
                    } else {
                        y_tileno = (offset / videocard.lineOffset) / VideoCard.Y_TILESIZE;
                    }
                    videocard.vgaMemReqUpdate = true;
                    videocard.setTileUpdate(x_tileno, y_tileno, true);
                }
                return;
            }
        }
        plane0 = 0 << 16;
        plane1 = 1 << 16;
        plane2 = 2 << 16;
        plane3 = 3 << 16;
        int i;
        switch(videocard.graphicsController.writeMode) {
            case 0:
                {
                    final byte bitmask = videocard.graphicsController.bitMask;
                    final byte set_reset = videocard.graphicsController.setReset;
                    final byte enable_set_reset = videocard.graphicsController.enableSetReset;
                    if (videocard.graphicsController.dataRotate != 0) {
                        value = (byte) ((value >> videocard.graphicsController.dataRotate) | (value << (8 - videocard.graphicsController.dataRotate)));
                    }
                    newValue[0] = (byte) (videocard.graphicsController.latch[0] & ~bitmask);
                    newValue[1] = (byte) (videocard.graphicsController.latch[1] & ~bitmask);
                    newValue[2] = (byte) (videocard.graphicsController.latch[2] & ~bitmask);
                    newValue[3] = (byte) (videocard.graphicsController.latch[3] & ~bitmask);
                    switch(videocard.graphicsController.dataOperation) {
                        case 0:
                            newValue[0] |= (((enable_set_reset & 1) != 0) ? (((set_reset & 1) != 0) ? bitmask : 0) : (value & bitmask));
                            newValue[1] |= (((enable_set_reset & 2) != 0) ? (((set_reset & 2) != 0) ? bitmask : 0) : (value & bitmask));
                            newValue[2] |= (((enable_set_reset & 4) != 0) ? (((set_reset & 4) != 0) ? bitmask : 0) : (value & bitmask));
                            newValue[3] |= (((enable_set_reset & 8) != 0) ? (((set_reset & 8) != 0) ? bitmask : 0) : (value & bitmask));
                            break;
                        case 1:
                            newValue[0] |= (((enable_set_reset & 1) != 0) ? (((set_reset & 1) != 0) ? (videocard.graphicsController.latch[0] & bitmask) : 0) : (value & videocard.graphicsController.latch[0]) & bitmask);
                            newValue[1] |= (((enable_set_reset & 2) != 0) ? (((set_reset & 2) != 0) ? (videocard.graphicsController.latch[1] & bitmask) : 0) : (value & videocard.graphicsController.latch[1]) & bitmask);
                            newValue[2] |= (((enable_set_reset & 4) != 0) ? (((set_reset & 4) != 0) ? (videocard.graphicsController.latch[2] & bitmask) : 0) : (value & videocard.graphicsController.latch[2]) & bitmask);
                            newValue[3] |= (((enable_set_reset & 8) != 0) ? (((set_reset & 8) != 0) ? (videocard.graphicsController.latch[3] & bitmask) : 0) : (value & videocard.graphicsController.latch[3]) & bitmask);
                            break;
                        case 2:
                            newValue[0] |= (((enable_set_reset & 1) != 0) ? (((set_reset & 1) != 0) ? bitmask : (videocard.graphicsController.latch[0] & bitmask)) : ((value | videocard.graphicsController.latch[0]) & bitmask));
                            newValue[1] |= (((enable_set_reset & 2) != 0) ? (((set_reset & 2) != 0) ? bitmask : (videocard.graphicsController.latch[1] & bitmask)) : ((value | videocard.graphicsController.latch[1]) & bitmask));
                            newValue[2] |= (((enable_set_reset & 4) != 0) ? (((set_reset & 4) != 0) ? bitmask : (videocard.graphicsController.latch[2] & bitmask)) : ((value | videocard.graphicsController.latch[2]) & bitmask));
                            newValue[3] |= (((enable_set_reset & 8) != 0) ? (((set_reset & 8) != 0) ? bitmask : (videocard.graphicsController.latch[3] & bitmask)) : ((value | videocard.graphicsController.latch[3]) & bitmask));
                            break;
                        case 3:
                            newValue[0] |= (((enable_set_reset & 1) != 0) ? (((set_reset & 1) != 0) ? (~videocard.graphicsController.latch[0] & bitmask) : (videocard.graphicsController.latch[0] & bitmask)) : (value ^ videocard.graphicsController.latch[0]) & bitmask);
                            newValue[1] |= (((enable_set_reset & 2) != 0) ? (((set_reset & 2) != 0) ? (~videocard.graphicsController.latch[1] & bitmask) : (videocard.graphicsController.latch[1] & bitmask)) : (value ^ videocard.graphicsController.latch[1]) & bitmask);
                            newValue[2] |= (((enable_set_reset & 4) != 0) ? (((set_reset & 4) != 0) ? (~videocard.graphicsController.latch[2] & bitmask) : (videocard.graphicsController.latch[2] & bitmask)) : (value ^ videocard.graphicsController.latch[2]) & bitmask);
                            newValue[3] |= (((enable_set_reset & 8) != 0) ? (((set_reset & 8) != 0) ? (~videocard.graphicsController.latch[3] & bitmask) : (videocard.graphicsController.latch[3] & bitmask)) : (value ^ videocard.graphicsController.latch[3]) & bitmask);
                            break;
                        default:
                            logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " vga_mem_write: write mode 0: op = " + videocard.graphicsController.dataOperation);
                    }
                }
                break;
            case 1:
                for (i = 0; i < 4; i++) {
                    newValue[i] = videocard.graphicsController.latch[i];
                }
                break;
            case 2:
                {
                    final byte bitmask = videocard.graphicsController.bitMask;
                    newValue[0] = (byte) (videocard.graphicsController.latch[0] & ~bitmask);
                    newValue[1] = (byte) (videocard.graphicsController.latch[1] & ~bitmask);
                    newValue[2] = (byte) (videocard.graphicsController.latch[2] & ~bitmask);
                    newValue[3] = (byte) (videocard.graphicsController.latch[3] & ~bitmask);
                    switch(videocard.graphicsController.dataOperation) {
                        case 0:
                            newValue[0] |= ((value & 1) != 0) ? bitmask : 0;
                            newValue[1] |= ((value & 2) != 0) ? bitmask : 0;
                            newValue[2] |= ((value & 4) != 0) ? bitmask : 0;
                            newValue[3] |= ((value & 8) != 0) ? bitmask : 0;
                            break;
                        case 1:
                            newValue[0] |= ((value & 1) != 0) ? (videocard.graphicsController.latch[0] & bitmask) : 0;
                            newValue[1] |= ((value & 2) != 0) ? (videocard.graphicsController.latch[1] & bitmask) : 0;
                            newValue[2] |= ((value & 4) != 0) ? (videocard.graphicsController.latch[2] & bitmask) : 0;
                            newValue[3] |= ((value & 8) != 0) ? (videocard.graphicsController.latch[3] & bitmask) : 0;
                            break;
                        case 2:
                            newValue[0] |= ((value & 1) != 0) ? bitmask : (videocard.graphicsController.latch[0] & bitmask);
                            newValue[1] |= ((value & 2) != 0) ? bitmask : (videocard.graphicsController.latch[1] & bitmask);
                            newValue[2] |= ((value & 4) != 0) ? bitmask : (videocard.graphicsController.latch[2] & bitmask);
                            newValue[3] |= ((value & 8) != 0) ? bitmask : (videocard.graphicsController.latch[3] & bitmask);
                            break;
                        case 3:
                            newValue[0] |= ((value & 1) != 0) ? (~videocard.graphicsController.latch[0] & bitmask) : (videocard.graphicsController.latch[0] & bitmask);
                            newValue[1] |= ((value & 2) != 0) ? (~videocard.graphicsController.latch[1] & bitmask) : (videocard.graphicsController.latch[1] & bitmask);
                            newValue[2] |= ((value & 4) != 0) ? (~videocard.graphicsController.latch[2] & bitmask) : (videocard.graphicsController.latch[2] & bitmask);
                            newValue[3] |= ((value & 8) != 0) ? (~videocard.graphicsController.latch[3] & bitmask) : (videocard.graphicsController.latch[3] & bitmask);
                            break;
                    }
                }
                break;
            case 3:
                {
                    final byte bitmask = (byte) (videocard.graphicsController.bitMask & value);
                    final byte set_reset = videocard.graphicsController.setReset;
                    if (videocard.graphicsController.dataRotate != 0) {
                        value = (byte) ((value >> videocard.graphicsController.dataRotate) | (value << (8 - videocard.graphicsController.dataRotate)));
                    }
                    newValue[0] = (byte) (videocard.graphicsController.latch[0] & ~bitmask);
                    newValue[1] = (byte) (videocard.graphicsController.latch[1] & ~bitmask);
                    newValue[2] = (byte) (videocard.graphicsController.latch[2] & ~bitmask);
                    newValue[3] = (byte) (videocard.graphicsController.latch[3] & ~bitmask);
                    value &= bitmask;
                    switch(videocard.graphicsController.dataOperation) {
                        case 0:
                            newValue[0] |= ((set_reset & 1) != 0) ? value : 0;
                            newValue[1] |= ((set_reset & 2) != 0) ? value : 0;
                            newValue[2] |= ((set_reset & 4) != 0) ? value : 0;
                            newValue[3] |= ((set_reset & 8) != 0) ? value : 0;
                            break;
                        case 1:
                            newValue[0] |= (((set_reset & 1) != 0) ? value : 0) & videocard.graphicsController.latch[0];
                            newValue[1] |= (((set_reset & 2) != 0) ? value : 0) & videocard.graphicsController.latch[1];
                            newValue[2] |= (((set_reset & 4) != 0) ? value : 0) & videocard.graphicsController.latch[2];
                            newValue[3] |= (((set_reset & 8) != 0) ? value : 0) & videocard.graphicsController.latch[3];
                            break;
                        case 2:
                            newValue[0] |= (((set_reset & 1) != 0) ? value : 0) | videocard.graphicsController.latch[0];
                            newValue[1] |= (((set_reset & 2) != 0) ? value : 0) | videocard.graphicsController.latch[1];
                            newValue[2] |= (((set_reset & 4) != 0) ? value : 0) | videocard.graphicsController.latch[2];
                            newValue[3] |= (((set_reset & 8) != 0) ? value : 0) | videocard.graphicsController.latch[3];
                            break;
                        case 3:
                            newValue[0] |= (((set_reset & 1) != 0) ? value : 0) ^ videocard.graphicsController.latch[0];
                            newValue[1] |= (((set_reset & 2) != 0) ? value : 0) ^ videocard.graphicsController.latch[1];
                            newValue[2] |= (((set_reset & 4) != 0) ? value : 0) ^ videocard.graphicsController.latch[2];
                            newValue[3] |= (((set_reset & 8) != 0) ? value : 0) ^ videocard.graphicsController.latch[3];
                            break;
                    }
                }
                break;
            default:
                logger.log(Level.SEVERE, "[" + MODULE_TYPE + "]" + " vga_mem_write: write mode " + videocard.graphicsController.writeMode + " ?");
        }
        if ((videocard.sequencer.mapMask & 0x0f) != 0) {
            videocard.vgaMemReqUpdate = true;
            if (videocard.sequencer.mapMaskArray[0] != 0) videocard.vgaMemory[plane0 + offset] = newValue[0];
            if (videocard.sequencer.mapMaskArray[1] != 0) videocard.vgaMemory[plane1 + offset] = newValue[1];
            if (videocard.sequencer.mapMaskArray[2] != 0) {
                if ((offset & 0xe000) == videocard.sequencer.charMapAddress) {
                    screen.setByteInCodePage((offset & 0x1fff), newValue[2]);
                }
                videocard.vgaMemory[plane2 + offset] = newValue[2];
            }
            if (videocard.sequencer.mapMaskArray[3] != 0) videocard.vgaMemory[plane3 + offset] = newValue[3];
            int x_tileno, y_tileno;
            if (videocard.graphicsController.shift256Reg == 2) {
                offset -= startAddress;
                x_tileno = (offset % videocard.lineOffset) * 4 / (VideoCard.X_TILESIZE / 2);
                if (videocard.crtControllerRegister.scanDoubling != 0) {
                    y_tileno = (offset / videocard.lineOffset) / (VideoCard.Y_TILESIZE / 2);
                } else {
                    y_tileno = (offset / videocard.lineOffset) / VideoCard.Y_TILESIZE;
                }
                videocard.setTileUpdate(x_tileno, y_tileno, true);
            } else {
                if (videocard.lineCompare < videocard.verticalDisplayEnd) {
                    if (videocard.lineOffset > 0) {
                        if (videocard.sequencer.dotClockRate != 0) {
                            x_tileno = (offset % videocard.lineOffset) / (VideoCard.X_TILESIZE / 16);
                        } else {
                            x_tileno = (offset % videocard.lineOffset) / (VideoCard.X_TILESIZE / 8);
                        }
                        if (videocard.crtControllerRegister.scanDoubling != 0) {
                            y_tileno = ((offset / videocard.lineOffset) * 2 + videocard.lineCompare + 1) / VideoCard.Y_TILESIZE;
                        } else {
                            y_tileno = ((offset / videocard.lineOffset) + videocard.lineCompare + 1) / VideoCard.Y_TILESIZE;
                        }
                        videocard.setTileUpdate(x_tileno, y_tileno, true);
                    }
                }
                if (offset >= startAddress) {
                    offset -= startAddress;
                    if (videocard.lineOffset > 0) {
                        if (videocard.sequencer.dotClockRate != 0) {
                            x_tileno = (offset % videocard.lineOffset) / (VideoCard.X_TILESIZE / 16);
                        } else {
                            x_tileno = (offset % videocard.lineOffset) / (VideoCard.X_TILESIZE / 8);
                        }
                        if (videocard.crtControllerRegister.scanDoubling != 0) {
                            y_tileno = (offset / videocard.lineOffset) / (VideoCard.Y_TILESIZE / 2);
                        } else {
                            y_tileno = (offset / videocard.lineOffset) / VideoCard.Y_TILESIZE;
                        }
                        videocard.setTileUpdate(x_tileno, y_tileno, true);
                    }
                }
            }
        }
    }
}
