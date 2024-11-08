package powervr;

import gui.Emu;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.swing.JFrame;
import memory.Memory;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.EXTBgra;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import sh4.Intc;
import sh4.Sh4Context;

public class PowerVR {

    public static int pvr_fb_r_ctrl = (1 << 23) | (1 << 2) | 1;

    private static final int pvrRegs[] = new int[0xFFFF];

    private static framebuffer FrameBufferFormat;

    private static final int FRAMEBUFFER_ARGB0555 = 0;

    private static final int FRAMEBUFFER_RGB565 = 1;

    private static final int FRAMEBUFFER_RGB888 = 2;

    private static final int FRAMEBUFFER_ARGB0888 = 3;

    private static int SPG_LOAD = 0x020C0359;

    private static int SPG_HBLANK = 0x007E0345;

    private static int SPG_VBLANK = 0x00280208;

    private static int SPG_WIDTH = 0x03F1933F;

    private static int SPG_CONTROL = 0x00000100;

    private static int SPG_VPOS1_IRQ = 21;

    private static int SPG_VPOS2_IRQ = 510;

    private static int maxScanlines = 524;

    private static int cyclesPerLine = ((200 * 1000 * 1000) / maxScanlines) / 60;

    private static int countCycles = cyclesPerLine;

    private static final int PVR_ID = 0x0000;

    private static final int PVR_REVISION = 0x0004;

    private static final int PVR_RESET = 0x0008;

    private static final int PVR_ISP_START = 0x0014;

    private static final int PVR_UNK_0018 = 0x0018;

    private static final int PVR_ISP_VERTBUF_ADDR = 0x0020;

    private static final int PVR_ISP_TILEMAT_ADDR = 0x002c;

    private static final int PVR_SPANSORT_CFG = 0x0030;

    private static final int PVR_FB_CFG_1 = 0x0044;

    private static final int PVR_FB_CFG_2 = 0x0048;

    private static final int PVR_RENDER_MODULO = 0x004c;

    private static final int PVR_RENDER_ADDR = 0x0060;

    private static final int PVR_RENDER_ADDR_2 = 0x0064;

    private static final int PVR_PCLIP_X = 0x0068;

    private static final int PVR_PCLIP_Y = 0x006c;

    private static final int PVR_CHEAP_SHADOW = 0x0074;

    private static final int PVR_OBJECT_CLIP = 0x0078;

    private static final int PVR_UNK_007C = 0x007c;

    private static final int PVR_UNK_0080 = 0x0080;

    private static final int PVR_TEXTURE_CLIP = 0x0084;

    private static final int PVR_BGPLANE_Z = 0x0088;

    private static final int PVR_BGPLANE_CFG = 0x008c;

    private static final int PVR_UNK_0098 = 0x0098;

    private static final int PVR_UNK_00A0 = 0x00a0;

    private static final int PVR_UNK_00A8 = 0x00a8;

    private static final int PVR_FOG_TABLE_COLOR = 0x00b0;

    private static final int PVR_FOG_VERTEX_COLOR = 0x00b4;

    private static final int PVR_FOG_DENSITY = 0x00b8;

    private static final int PVR_COLOR_CLAMP_MAX = 0x00bc;

    private static final int PVR_COLOR_CLAMP_MIN = 0x00c0;

    private static final int PVR_GUN_POS = 0x00c4;

    private static final int PVR_UNK_00C8 = 0x00c8;

    private static final int PVR_VPOS_IRQ = 0x00cc;

    private static final int PVR_TEXTURE_MODULO = 0x00e4;

    private static final int PVR_VIDEO_CFG = 0x00e8;

    private static final int PVR_SCALER_CFG = 0x00f4;

    private static final int PVR_PALETTE_CFG = 0x0108;

    private static final int PVR_SYNC_STATUS = 0x010c;

    private static final int PVR_UNK_0110 = 0x0110;

    private static final int PVR_UNK_0114 = 0x0114;

    private static final int PVR_UNK_0118 = 0x0118;

    private static final int PVR_TA_OPB_START = 0x0124;

    private static final int PVR_TA_VERTBUF_START = 0x0128;

    private static final int PVR_TA_OPB_END = 0x012c;

    private static final int PVR_TA_VERTBUF_END = 0x0130;

    private static final int PVR_TA_OPB_POS = 0x0134;

    private static final int PVR_TA_VERTBUF_POS = 0x0138;

    private static final int PVR_TILEMAT_CFG = 0x013c;

    private static final int PVR_OPB_CFG = 0x0140;

    private static final int PVR_TA_INIT = 0x0144;

    private static final int PVR_YUV_ADDR = 0x0148;

    private static final int PVR_YUV_CFG_1 = 0x014c;

    private static final int PVR_UNK_0160 = 0x0160;

    private static final int PVR_TA_OPB_INIT = 0x0164;

    private static final int PVR_FOG_TABLE_BASE = 0x0200;

    private static final int PVR_PALETTE_TABLE_BASE = 0x1000;

    private static final int PVR_TA_INPUT = 0x10000000;

    private static final int PVR_RAM_BASE = 0xa5000000;

    private static final int PVR_RAM_INT_BASE = 0xa4000000;

    private static final int PVR_RAM_SIZE = (8 * 1024 * 1024);

    private static final int PVR_RAM_TOP = (PVR_RAM_BASE + PVR_RAM_SIZE);

    private static final int PVR_RAM_INT_TOP = (PVR_RAM_INT_BASE + PVR_RAM_SIZE);

    private static int screenbits = 16;

    private static int screenformat = 1;

    private static boolean pvr_framebufferdisplay = true;

    private static int FrameBufferAddress;

    private static int screenwidth = 320;

    private static int screenheight = 480;

    private static int pvr_scanline = 0;

    private static float screentexwidth;

    private static float screentexheight;

    private static int BackgroundTextureID;

    private final Intc interruptController;

    private static IntBuffer buffer;

    private static int screenwidth32;

    static {
    }

    public PowerVR(Intc i) {
        interruptController = i;
        FrameBufferFormat = new framebuffer();
        initOGLDisplay();
        screeninit();
    }

    public void SyncVideoDisplay(int cycles) {
        countCycles -= cycles;
        if (countCycles < 0) {
            pvr_scanline++;
            countCycles = cyclesPerLine;
            if (pvr_scanline == SPG_VPOS1_IRQ) interruptController.addInterrupts(Intc.ASIC_EVT_PVR_SCANINT1); else if (pvr_scanline == SPG_VPOS2_IRQ) interruptController.addInterrupts(Intc.ASIC_EVT_PVR_SCANINT2); else if (pvr_scanline > maxScanlines) {
                if (pvr_framebufferdisplay) RenderFramebuffer();
                interruptController.addInterrupts(Intc.ASIC_EVT_PVR_VBLINT);
                pvr_scanline = 0;
            }
        }
    }

    public void RenderFramebuffer() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, BackgroundTextureID);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, screenwidth32);
        System.out.println("FrameBuffer Address " + Integer.toHexString(FrameBufferAddress));
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, FrameBufferFormat.internal_format, (int) screentexwidth, (int) screentexheight, 0, FrameBufferFormat.format, FrameBufferFormat.type, Memory.video);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, screenheight / screentexheight);
        GL11.glVertex3f(0.0f, (float) screenheight, -1);
        GL11.glTexCoord2f(screenwidth32 / screentexwidth, screenheight / screentexheight);
        GL11.glVertex3f((float) screenwidth32, (float) screenheight, -1);
        GL11.glTexCoord2f(screenwidth32 / screentexwidth, 0.0f);
        GL11.glVertex3f((float) screenwidth32, 0.0f, -1);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(0.0f, 0.0f, -1);
        GL11.glEnd();
        Display.update();
        for (int i = 0; i < Emu.connectedDevices; i++) {
            Emu.ports[i].ReadDeviceInput();
        }
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public static void gpuInputCommand(int address, int val) {
        System.out.println("Called @" + Integer.toHexString(address) + "with val " + val);
        pvrRegs[address] = val;
        switch(address) {
            case PVR_ISP_START:
                System.out.println("Called TA render");
                TileAccelarator.render();
                break;
            case PVR_OPB_CFG:
                TileAccelarator.ppblocksize(val);
                break;
            case PVR_TA_INIT:
                System.out.println("disabling framebuffer");
                pvr_framebufferdisplay = false;
                Memory.write32(0xa05f8138, Memory.read32(0xa05f8128));
                break;
            case 0x0044:
                {
                    boolean reinit = false;
                    pvr_fb_r_ctrl = val;
                    if ((val & 0x02) != 0) System.out.println("line doubling enable\r\n");
                    switch((val >> 2) & 0x3) {
                        case 0x00:
                            System.out.println("ARGB0555\r\n");
                            if (screenbits != 16 || screenformat != FRAMEBUFFER_ARGB0555) reinit = true;
                            screenbits = 16;
                            screenformat = FRAMEBUFFER_ARGB0555;
                            FrameBufferFormat.internal_format = GL11.GL_RGBA;
                            FrameBufferFormat.format = GL11.GL_RGBA;
                            FrameBufferFormat.type = GL12.GL_UNSIGNED_SHORT_5_5_5_1;
                            break;
                        case 0x01:
                            System.out.println("RGB565\r\n");
                            if (screenbits != 16 || screenformat != FRAMEBUFFER_RGB565) reinit = true;
                            screenbits = 16;
                            screenformat = FRAMEBUFFER_RGB565;
                            FrameBufferFormat.internal_format = GL11.GL_RGB;
                            FrameBufferFormat.format = GL11.GL_RGB;
                            FrameBufferFormat.type = GL12.GL_UNSIGNED_SHORT_5_6_5;
                            break;
                        case 0x02:
                            System.out.println("RGB888\r\n");
                            if (screenbits != 24 || screenformat != FRAMEBUFFER_RGB888) reinit = true;
                            screenbits = 24;
                            screenformat = FRAMEBUFFER_RGB888;
                            FrameBufferFormat.internal_format = GL11.GL_RGBA;
                            FrameBufferFormat.format = GL11.GL_RGBA;
                            FrameBufferFormat.type = GL11.GL_UNSIGNED_BYTE;
                            break;
                        case 0x03:
                            System.out.println("ARGB0888\r\n");
                            if (screenbits != 32 || screenformat != FRAMEBUFFER_ARGB0888) reinit = true;
                            screenbits = 32;
                            screenformat = FRAMEBUFFER_ARGB0888;
                            FrameBufferFormat.internal_format = GL11.GL_RGB;
                            FrameBufferFormat.format = EXTBgra.GL_BGRA_EXT;
                            FrameBufferFormat.type = GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
                            break;
                    }
                    if ((val & 0x00800000) != 0) System.out.println("pixel clock double enable\r\n");
                    System.out.println("screenbits: " + screenbits);
                    if ((val & 0x01) != 0) {
                        System.out.println("bitmap display enable\r\n");
                        pvr_framebufferdisplay = true;
                        if (reinit) screeninit();
                    } else pvr_framebufferdisplay = false;
                }
                break;
            case PVR_FB_CFG_2:
                switch(val & 0x7) {
                    case 0:
                        System.out.println("fb_packmode: 0555KRGB\n");
                        break;
                    case 1:
                        System.out.println("fb_packmode: 565RGB\n");
                        break;
                    case 2:
                        System.out.println("fb_packmode: 4444ARGB\n");
                        break;
                    case 3:
                        System.out.println("fb_packmode: 1555ARGB\n");
                        break;
                    case 4:
                        System.out.println("fb_packmode: 888RGB\n");
                        break;
                    case 5:
                        System.out.println("fb_packmode: 0888KRGB\n");
                        break;
                    case 6:
                        System.out.println("fb_packmode: 8888ARGB\n");
                        break;
                    case 7:
                        System.out.println("fb_packmode: reserved\n");
                        break;
                }
                break;
            case 0x0050:
                FrameBufferAddress = val;
                break;
            case 0x005C:
                screenwidth = (val & 0x3FF) + 1;
                System.out.println("Screenwidth (in 32 bits units) " + screenwidth);
                screenheight = ((val >> 10) & 0x3FF) + 1;
                System.out.println("screenheight: " + screenheight);
                if (screenheight == 0) {
                    System.out.println("Invalid value.Choosing default values.\n");
                    screenheight = 480;
                    screenwidth = 640;
                    screenbits = 16;
                }
                screeninit();
                break;
        }
    }

    private static boolean initOGLDisplay() {
        try {
            Display.setDisplayMode(new DisplayMode(640, 480));
            Display.setTitle("Esoteric - a Java Dreamcast Emulator ;) ");
            Display.create();
            buffer = ByteBuffer.allocateDirect(1 * 4).asIntBuffer();
            GL11.glGenTextures(buffer);
            BackgroundTextureID = buffer.get(0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, BackgroundTextureID);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glViewport(0, 0, 640, 480);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            TileAccelarator.init();
            return true;
        } catch (LWJGLException e) {
            System.out.println("Could not create Display");
            e.printStackTrace();
            return false;
        }
    }

    private static void screeninit() {
        System.out.println("screenbits " + screenbits);
        System.out.println("screenwidth before" + screenwidth);
        screenwidth32 = screenwidth * ((screenbits == 32) ? 1 : 2);
        if (screenwidth32 > 512) screentexwidth = 1024.0f; else if (screenwidth32 > 256) screentexwidth = 512.0f;
        if (screenheight > 512) screentexheight = 1024.0f; else if (screenheight > 256) screentexheight = 512.0f; else if (screenheight > 128) screentexheight = 256.0f;
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, screenwidth32, screenheight, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        System.out.println("ScreenWidth " + screenwidth + " Screenheight " + screenheight);
        System.out.println("ScreenTexWidth " + screentexwidth + " Screentexheight " + screentexheight);
    }

    public static int gpuReadAcess(int address) {
        System.out.println("PVR READ " + Integer.toHexString(address));
        switch(address) {
            case PVR_ID:
                System.out.println("pvr_read: COREID\r\n");
                return 0x17fd11db;
            case PVR_FB_CFG_1:
                System.out.println("pvr_read: FB_R_CTRL\r\n");
                return pvr_fb_r_ctrl;
            case PVR_VPOS_IRQ:
                System.out.println("pvr_read: SCANINTPOS\n");
                return SPG_VBLANK;
            case 0x00d0:
                System.out.println("pvr_read: SPG_CONTROL\n");
                return SPG_CONTROL;
            case 0x00d4:
                System.out.println("pvr_read: SPG_HBLANK\n");
                return SPG_HBLANK;
            case 0x00d8:
                System.out.println("pvr_read: SPG_LOAD\n");
                return SPG_LOAD;
            case 0x00dc:
                System.out.println("pvr_read: SPG_VBLANK\n");
                return SPG_VBLANK;
            case 0x00e0:
                System.out.println("pvr_read: SPG_WIDTH\n");
                return SPG_WIDTH;
            case PVR_VIDEO_CFG:
                System.out.println("pvr_read: BITMAPTYPE2\r\n");
                return 0x00160000;
            case 0x0050:
                return 0x00100203;
            case 0x010c:
                return pvr_scanline;
            case 0x0114:
                System.out.println("pvr_read: FB_C_SOF: framebuffer current read address\n");
                return FrameBufferAddress;
            case 0x0128:
                return TileAccelarator.pvr_ta_isp_base;
            case 0x0138:
                return TileAccelarator.pvr_ta_itp_current;
        }
        return pvrRegs[address];
    }

    class framebuffer {

        int internal_format;

        int format;

        int type;

        public framebuffer() {
            internal_format = GL11.GL_RGB;
            format = GL11.GL_RGB;
            type = GL12.GL_UNSIGNED_SHORT_5_6_5;
        }

        public String toString() {
            if (screenformat == FRAMEBUFFER_ARGB0555) return "FRAMEBUFFER_ARGB0555";
            if (screenformat == FRAMEBUFFER_ARGB0888) return "FRAMEBUFFER_ARGB0888";
            if (screenformat == FRAMEBUFFER_RGB565) return "FRAMEBUFFER_RGB565";
            if (screenformat == FRAMEBUFFER_RGB888) return "FRAMEBUFFER_RGB888";
            return "null";
        }
    }
}
