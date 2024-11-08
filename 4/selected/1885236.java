package jdos.ints;

import jdos.Dosbox;
import jdos.hardware.IoHandler;
import jdos.hardware.Memory;
import jdos.hardware.VGA;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.types.SVGACards;

public class Int10_char {

    static void CGA2_CopyRow(short cleft, short cright, short rold, short rnew, int base) {
        short cheight = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT);
        int dest = base + ((Int10_modes.CurMode.twidth * rnew) * (cheight / 2) + cleft);
        int src = base + ((Int10_modes.CurMode.twidth * rold) * (cheight / 2) + cleft);
        int copy = (cright - cleft);
        int nextline = Int10_modes.CurMode.twidth;
        for (int i = 0; i < cheight / 2; i++) {
            Memory.MEM_BlockCopy(dest, src, copy);
            Memory.MEM_BlockCopy(dest + 8 * 1024, src + 8 * 1024, copy);
            dest += nextline;
            src += nextline;
        }
    }

    static void CGA4_CopyRow(short cleft, short cright, short rold, short rnew, int base) {
        short cheight = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT);
        int dest = base + ((Int10_modes.CurMode.twidth * rnew) * (cheight / 2) + cleft) * 2;
        int src = base + ((Int10_modes.CurMode.twidth * rold) * (cheight / 2) + cleft) * 2;
        int copy = (cright - cleft) * 2;
        int nextline = Int10_modes.CurMode.twidth * 2;
        for (int i = 0; i < cheight / 2; i++) {
            Memory.MEM_BlockCopy(dest, src, copy);
            Memory.MEM_BlockCopy(dest + 8 * 1024, src + 8 * 1024, copy);
            dest += nextline;
            src += nextline;
        }
    }

    static void TANDY16_CopyRow(short cleft, short cright, short rold, short rnew, int base) {
        short cheight = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT);
        int dest = base + ((Int10_modes.CurMode.twidth * rnew) * (cheight / 4) + cleft) * 4;
        int src = base + ((Int10_modes.CurMode.twidth * rold) * (cheight / 4) + cleft) * 4;
        int copy = (cright - cleft) * 4;
        int nextline = Int10_modes.CurMode.twidth * 4;
        for (int i = 0; i < cheight / 4; i++) {
            Memory.MEM_BlockCopy(dest, src, copy);
            Memory.MEM_BlockCopy(dest + 8 * 1024, src + 8 * 1024, copy);
            Memory.MEM_BlockCopy(dest + 16 * 1024, src + 16 * 1024, copy);
            Memory.MEM_BlockCopy(dest + 24 * 1024, src + 24 * 1024, copy);
            dest += nextline;
            src += nextline;
        }
    }

    static void EGA16_CopyRow(short cleft, short cright, short rold, short rnew, int base) {
        int src, dest;
        int copy;
        short cheight = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT);
        dest = base + (Int10_modes.CurMode.twidth * rnew) * cheight + cleft;
        src = base + (Int10_modes.CurMode.twidth * rold) * cheight + cleft;
        int nextline = Int10_modes.CurMode.twidth;
        IoHandler.IO_Write(0x3ce, 5);
        IoHandler.IO_Write(0x3cf, 1);
        IoHandler.IO_Write(0x3c4, 2);
        IoHandler.IO_Write(0x3c5, 0xf);
        int rowsize = (cright - cleft);
        copy = cheight;
        for (; copy > 0; copy--) {
            for (int x = 0; x < rowsize; x++) Memory.mem_writeb(dest + x, Memory.mem_readb(src + x));
            dest += nextline;
            src += nextline;
        }
        IoHandler.IO_Write(0x3ce, 5);
        IoHandler.IO_Write(0x3cf, 0);
    }

    static void VGA_CopyRow(short cleft, short cright, short rold, short rnew, int base) {
        int src, dest;
        int copy;
        short cheight = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT);
        dest = base + 8 * ((Int10_modes.CurMode.twidth * rnew) * cheight + cleft);
        src = base + 8 * ((Int10_modes.CurMode.twidth * rold) * cheight + cleft);
        int nextline = 8 * Int10_modes.CurMode.twidth;
        int rowsize = 8 * (cright - cleft);
        copy = cheight;
        for (; copy > 0; copy--) {
            for (int x = 0; x < rowsize; x++) Memory.mem_writeb(dest + x, Memory.mem_readb(src + x));
            dest += nextline;
            src += nextline;
        }
    }

    static void TEXT_CopyRow(short cleft, short cright, short rold, short rnew, int base) {
        int src, dest;
        src = base + (rold * Int10_modes.CurMode.twidth + cleft) * 2;
        dest = base + (rnew * Int10_modes.CurMode.twidth + cleft) * 2;
        Memory.MEM_BlockCopy(dest, src, (cright - cleft) * 2);
    }

    static void CGA2_FillRow(short cleft, short cright, short row, int base, short attr) {
        short cheight = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT);
        int dest = base + ((Int10_modes.CurMode.twidth * row) * (cheight / 2) + cleft);
        int copy = (cright - cleft);
        int nextline = Int10_modes.CurMode.twidth;
        attr = (short) ((attr & 0x3) | ((attr & 0x3) << 2) | ((attr & 0x3) << 4) | ((attr & 0x3) << 6));
        for (int i = 0; i < cheight / 2; i++) {
            for (int x = 0; x < copy; x++) {
                Memory.mem_writeb(dest + x, attr);
                Memory.mem_writeb(dest + 8 * 1024 + x, attr);
            }
            dest += nextline;
        }
    }

    static void CGA4_FillRow(short cleft, short cright, short row, int base, short attr) {
        short cheight = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT);
        int dest = base + ((Int10_modes.CurMode.twidth * row) * (cheight / 2) + cleft) * 2;
        int copy = (cright - cleft) * 2;
        int nextline = Int10_modes.CurMode.twidth * 2;
        attr = (short) ((attr & 0x3) | ((attr & 0x3) << 2) | ((attr & 0x3) << 4) | ((attr & 0x3) << 6));
        for (int i = 0; i < cheight / 2; i++) {
            for (int x = 0; x < copy; x++) {
                Memory.mem_writeb(dest + x, attr);
                Memory.mem_writeb(dest + 8 * 1024 + x, attr);
            }
            dest += nextline;
        }
    }

    static void TANDY16_FillRow(short cleft, short cright, short row, int base, short attr) {
        short cheight = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT);
        int dest = base + ((Int10_modes.CurMode.twidth * row) * (cheight / 4) + cleft) * 4;
        int copy = (cright - cleft) * 4;
        int nextline = Int10_modes.CurMode.twidth * 4;
        attr = (short) ((attr & 0xf) | (attr & 0xf) << 4);
        for (int i = 0; i < cheight / 4; i++) {
            for (int x = 0; x < copy; x++) {
                Memory.mem_writeb(dest + x, attr);
                Memory.mem_writeb(dest + 8 * 1024 + x, attr);
                Memory.mem_writeb(dest + 16 * 1024 + x, attr);
                Memory.mem_writeb(dest + 24 * 1024 + x, attr);
            }
            dest += nextline;
        }
    }

    static void EGA16_FillRow(short cleft, short cright, short row, int base, short attr) {
        IoHandler.IO_Write(0x3ce, 0x8);
        IoHandler.IO_Write(0x3cf, 0xff);
        IoHandler.IO_Write(0x3ce, 0x0);
        IoHandler.IO_Write(0x3cf, attr);
        IoHandler.IO_Write(0x3ce, 0x1);
        IoHandler.IO_Write(0x3cf, 0xf);
        short cheight = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT);
        int dest = base + (Int10_modes.CurMode.twidth * row) * cheight + cleft;
        int nextline = Int10_modes.CurMode.twidth;
        int copy = cheight;
        int rowsize = (cright - cleft);
        for (; copy > 0; copy--) {
            for (int x = 0; x < rowsize; x++) Memory.mem_writeb(dest + x, 0xff);
            dest += nextline;
        }
        IoHandler.IO_Write(0x3cf, 0);
    }

    static void VGA_FillRow(short cleft, short cright, short row, int base, short attr) {
        short cheight = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT);
        int dest = base + 8 * ((Int10_modes.CurMode.twidth * row) * cheight + cleft);
        int nextline = 8 * Int10_modes.CurMode.twidth;
        int copy = cheight;
        int rowsize = 8 * (cright - cleft);
        for (; copy > 0; copy--) {
            for (int x = 0; x < rowsize; x++) Memory.mem_writeb(dest + x, attr);
            dest += nextline;
        }
    }

    static void TEXT_FillRow(short cleft, short cright, short row, int base, short attr) {
        int dest;
        dest = base + (row * Int10_modes.CurMode.twidth + cleft) * 2;
        int fill = (attr << 8) + ' ';
        for (short x = 0; x < (cright - cleft); x++) {
            Memory.mem_writew(dest, fill);
            dest += 2;
        }
    }

    public static void INT10_ScrollWindow(short rul, short cul, short rlr, short clr, byte nlines, short attr, short page) {
        if (Int10_modes.CurMode.type != VGA.M_TEXT) page = 0xff;
        int ncols = Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_NB_COLS);
        int nrows = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_NB_ROWS) + 1;
        if (rul > rlr) return;
        if (cul > clr) return;
        if (rlr >= nrows) rlr = (short) (nrows - 1);
        if (clr >= ncols) clr = (short) (ncols - 1);
        clr++;
        if (page == 0xFF) page = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_PAGE);
        int base = Int10_modes.CurMode.pstart + page * Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_PAGE_SIZE);
        int start = 0, end = 0;
        int next = 0;
        boolean gotofilling = false;
        if (nlines > 0) {
            start = rlr - nlines + 1;
            end = rul;
            next = -1;
        } else if (nlines < 0) {
            start = rul - nlines - 1;
            end = rlr;
            next = 1;
        } else {
            nlines = (byte) (rlr - rul + 1);
            gotofilling = true;
        }
        if (!gotofilling) {
            while (start != end) {
                start += next;
                switch(Int10_modes.CurMode.type) {
                    case VGA.M_TEXT:
                        TEXT_CopyRow(cul, clr, (short) start, (short) (start + nlines), base);
                        break;
                    case VGA.M_CGA2:
                        CGA2_CopyRow(cul, clr, (short) start, (short) (start + nlines), base);
                        break;
                    case VGA.M_CGA4:
                        CGA4_CopyRow(cul, clr, (short) start, (short) (start + nlines), base);
                        break;
                    case VGA.M_TANDY16:
                        TANDY16_CopyRow(cul, clr, (short) start, (short) (start + nlines), base);
                        break;
                    case VGA.M_EGA:
                        EGA16_CopyRow(cul, clr, (short) start, (short) (start + nlines), base);
                        break;
                    case VGA.M_VGA:
                        VGA_CopyRow(cul, clr, (short) start, (short) (start + nlines), base);
                        break;
                    case VGA.M_LIN4:
                        if ((Dosbox.machine == MachineType.MCH_VGA) && (Dosbox.svgaCard == SVGACards.SVGA_TsengET4K) && (Int10_modes.CurMode.swidth <= 800)) {
                            EGA16_CopyRow(cul, clr, (short) start, (short) (start + nlines), base);
                            break;
                        }
                    default:
                        if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR, "Unhandled mode " + Int10_modes.CurMode.type + " for scroll");
                }
            }
        }
        if (nlines > 0) {
            start = rul;
        } else {
            nlines = (byte) -nlines;
            start = rlr - nlines + 1;
        }
        for (; nlines > 0; nlines--) {
            switch(Int10_modes.CurMode.type) {
                case VGA.M_TEXT:
                    TEXT_FillRow(cul, clr, (short) start, base, attr);
                    break;
                case VGA.M_CGA2:
                    CGA2_FillRow(cul, clr, (short) start, base, attr);
                    break;
                case VGA.M_CGA4:
                    CGA4_FillRow(cul, clr, (short) start, base, attr);
                    break;
                case VGA.M_TANDY16:
                    TANDY16_FillRow(cul, clr, (short) start, base, attr);
                    break;
                case VGA.M_EGA:
                    EGA16_FillRow(cul, clr, (short) start, base, attr);
                    break;
                case VGA.M_VGA:
                    VGA_FillRow(cul, clr, (short) start, base, attr);
                    break;
                case VGA.M_LIN4:
                    if ((Dosbox.machine == MachineType.MCH_VGA) && (Dosbox.svgaCard == SVGACards.SVGA_TsengET4K) && (Int10_modes.CurMode.swidth <= 800)) {
                        EGA16_FillRow(cul, clr, (short) start, base, attr);
                        break;
                    }
                default:
                    if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR, "Unhandled mode " + Int10_modes.CurMode.type + " for scroll");
            }
            start++;
        }
    }

    public static void INT10_SetActivePage(short page) {
        int mem_address;
        if (page > 7) if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR, "INT10_SetActivePage page " + page);
        if (Dosbox.IS_EGAVGA_ARCH() && (Dosbox.svgaCard == SVGACards.SVGA_S3Trio)) page &= 7;
        mem_address = page * Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_PAGE_SIZE);
        Memory.real_writew(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_START, mem_address);
        if (Dosbox.IS_EGAVGA_ARCH()) {
            if (Int10_modes.CurMode.mode < 8) mem_address >>= 1;
        } else {
            mem_address >>= 1;
        }
        int base = Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTC_ADDRESS);
        IoHandler.IO_Write(base, 0x0c);
        IoHandler.IO_Write(base + 1, (mem_address >> 8));
        IoHandler.IO_Write(base, 0x0d);
        IoHandler.IO_Write(base + 1, mem_address);
        Memory.real_writeb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_PAGE, page);
        short cur_row = Int10.CURSOR_POS_ROW(page);
        short cur_col = Int10.CURSOR_POS_COL(page);
        INT10_SetCursorPos(cur_row, cur_col, page);
    }

    private static void dowrite(short first, short last) {
        int base = Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTC_ADDRESS);
        IoHandler.IO_Write(base, 0xa);
        IoHandler.IO_Write(base + 1, first);
        IoHandler.IO_Write(base, 0xb);
        IoHandler.IO_Write(base + 1, last);
    }

    public static void INT10_SetCursorShape(short first, short last) {
        Memory.real_writew(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURSOR_TYPE, last | (first << 8));
        if (Dosbox.machine == MachineType.MCH_CGA) {
            dowrite(first, last);
            return;
        }
        if (Dosbox.IS_TANDY_ARCH()) {
            dowrite(first, last);
            return;
        }
        if ((Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_VIDEO_CTL) & 0x8) == 0) {
            if ((first & 0x60) == 0x20) {
                first = 0x1e;
                last = 0x00;
                {
                    dowrite(first, last);
                    return;
                }
            }
            if ((Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_VIDEO_CTL) & 0x1) == 0) {
                if ((first & 0xe0) != 0 || (last & 0xe0) != 0) {
                    dowrite(first, last);
                    return;
                }
                short cheight = (short) (Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT) - 1);
                if (last < first) {
                    if (last == 0) {
                        dowrite(first, last);
                        return;
                    }
                    first = last;
                    last = cheight;
                } else if (((first | last) >= cheight) || !(last == (cheight - 1)) || !(first == cheight)) {
                    if (last <= 3) {
                        dowrite(first, last);
                        return;
                    }
                    if (first + 2 < last) {
                        if (first > 2) {
                            first = (short) ((cheight + 1) / 2);
                            last = cheight;
                        } else {
                            last = cheight;
                        }
                    } else {
                        first = (short) ((first - last) + cheight);
                        last = cheight;
                        if (cheight > 0xc) {
                            first--;
                            last--;
                        }
                    }
                }
            }
        }
        dowrite(first, last);
    }

    public static void INT10_SetCursorPos(short row, short col, short page) {
        int address;
        if (page > 7) if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR, "INT10_SetCursorPos page " + page);
        Memory.real_writeb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURSOR_POS + page * 2, col);
        Memory.real_writeb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURSOR_POS + page * 2 + 1, row);
        short current = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_PAGE);
        if (page == current) {
            int ncols = Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_NB_COLS);
            address = (ncols * row) + col + Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_START) / 2;
            int base = Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTC_ADDRESS);
            IoHandler.IO_Write(base, 0x0e);
            IoHandler.IO_Write(base + 1, (address >> 8));
            IoHandler.IO_Write(base, 0x0f);
            IoHandler.IO_Write(base + 1, address);
        }
    }

    public static int ReadCharAttr(int col, int row, short page) {
        int fontdata;
        int x, y;
        short cheight = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT);
        boolean split_chr = false;
        switch(Int10_modes.CurMode.type) {
            case VGA.M_TEXT:
                {
                    int address = page * Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_PAGE_SIZE);
                    address += (row * Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_NB_COLS) + col) * 2;
                    int where = Int10_modes.CurMode.pstart + address;
                    return Memory.mem_readw(where);
                }
            case VGA.M_CGA4:
            case VGA.M_CGA2:
            case VGA.M_TANDY16:
                split_chr = true;
            default:
                for (int chr = 0; chr <= 255; chr++) {
                    if (!split_chr || (chr < 128)) fontdata = Memory.Real2Phys(Memory.RealGetVec(0x43)) + chr * cheight; else fontdata = Memory.Real2Phys(Memory.RealGetVec(0x1F)) + (chr - 128) * cheight;
                    x = 8 * col;
                    y = cheight * row;
                    boolean error = false;
                    for (short h = 0; h < cheight; h++) {
                        short bitsel = 128;
                        short bitline = Memory.mem_readb(fontdata++);
                        short res = 0;
                        short vidline = 0;
                        int tx = x;
                        while (bitsel != 0) {
                            res = Int10_put_pixel.INT10_GetPixel(tx, y, page);
                            if (res != 0) vidline |= bitsel;
                            tx++;
                            bitsel >>= 1;
                        }
                        y++;
                        if (bitline != vidline) {
                            error = true;
                            break;
                        }
                    }
                    if (!error) {
                        return chr;
                    }
                }
                Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR, "ReadChar didn't find character");
                return 0;
        }
    }

    public static int INT10_ReadCharAttr(short page) {
        if (page == 0xFF) page = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_PAGE);
        short cur_row = Int10.CURSOR_POS_ROW(page);
        short cur_col = Int10.CURSOR_POS_COL(page);
        return ReadCharAttr(cur_col, cur_row, page);
    }

    private static boolean warned_use = false;

    public static void WriteChar(int col, int row, short page, short chr, short attr, boolean useattr) {
        int fontdata;
        int x, y;
        short cheight = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CHAR_HEIGHT);
        switch(Int10_modes.CurMode.type) {
            case VGA.M_TEXT:
                {
                    int address = page * Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_PAGE_SIZE);
                    address += (row * Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_NB_COLS) + col) * 2;
                    int where = Int10_modes.CurMode.pstart + address;
                    Memory.mem_writeb(where, chr);
                    if (useattr) {
                        Memory.mem_writeb(where + 1, attr);
                    }
                }
                return;
            case VGA.M_CGA4:
            case VGA.M_CGA2:
            case VGA.M_TANDY16:
                if (chr < 128) fontdata = Memory.RealGetVec(0x43); else {
                    chr -= 128;
                    fontdata = Memory.RealGetVec(0x1f);
                }
                fontdata = Memory.RealMake(Memory.RealSeg(fontdata), Memory.RealOff(fontdata) + chr * cheight);
                break;
            default:
                fontdata = Memory.RealGetVec(0x43);
                fontdata = Memory.RealMake(Memory.RealSeg(fontdata), Memory.RealOff(fontdata) + chr * cheight);
                break;
        }
        if (!useattr) {
            if (!warned_use) {
                if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR, "writechar used without attribute in non-textmode " + String.valueOf((char) chr) + " " + Integer.toString(chr, 16));
                warned_use = true;
            }
            switch(Int10_modes.CurMode.type) {
                case VGA.M_CGA4:
                    attr = 0x3;
                    break;
                case VGA.M_CGA2:
                    attr = 0x1;
                    break;
                case VGA.M_TANDY16:
                case VGA.M_EGA:
                default:
                    attr = 0xf;
                    break;
            }
        }
        if ((Int10_modes.CurMode.mode == 0x6)) attr = (short) ((attr & 0x80) | 1);
        x = 8 * col;
        y = cheight * row;
        short xor_mask = (short) ((Int10_modes.CurMode.type == VGA.M_VGA) ? 0x0 : 0x80);
        if (Int10_modes.CurMode.type == VGA.M_EGA) {
            IoHandler.IO_Write(0x3c4, 0x2);
            IoHandler.IO_Write(0x3c5, 0xf);
        }
        for (short h = 0; h < cheight; h++) {
            short bitsel = 128;
            short bitline = Memory.mem_readb(Memory.Real2Phys(fontdata));
            fontdata = Memory.RealMake(Memory.RealSeg(fontdata), Memory.RealOff(fontdata) + 1);
            int tx = x;
            while (bitsel != 0) {
                if ((bitline & bitsel) != 0) Int10_put_pixel.INT10_PutPixel(tx, y, page, attr); else Int10_put_pixel.INT10_PutPixel(tx, y, page, (short) (attr & xor_mask));
                tx++;
                bitsel >>= 1;
            }
            y++;
        }
    }

    public static void INT10_WriteChar(short chr, short attr, short page, int count, boolean showattr) {
        if (Int10_modes.CurMode.type != VGA.M_TEXT) {
            showattr = true;
            switch(Dosbox.machine) {
                case MachineType.MCH_EGA:
                case MachineType.MCH_VGA:
                    page %= Int10_modes.CurMode.ptotal;
                    break;
                case MachineType.MCH_CGA:
                case MachineType.MCH_PCJR:
                    page = 0;
                    break;
            }
        }
        short cur_row = Int10.CURSOR_POS_ROW(page);
        short cur_col = Int10.CURSOR_POS_COL(page);
        int ncols = Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_NB_COLS);
        while (count > 0) {
            WriteChar(cur_col, cur_row, page, chr, attr, showattr);
            count--;
            cur_col++;
            if (cur_col == ncols) {
                cur_col = 0;
                cur_row++;
            }
        }
    }

    static void INT10_TeletypeOutputAttr(short chr, short attr, boolean useattr, short page) {
        int ncols = Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_NB_COLS);
        int nrows = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_NB_ROWS) + 1;
        short cur_row = Int10.CURSOR_POS_ROW(page);
        short cur_col = Int10.CURSOR_POS_COL(page);
        switch(chr) {
            case 7:
                break;
            case 8:
                if (cur_col > 0) cur_col--;
                break;
            case '\r':
                cur_col = 0;
                break;
            case '\n':
                cur_row++;
                break;
            case '\t':
                do {
                    INT10_TeletypeOutputAttr((short) ' ', attr, useattr, page);
                    cur_row = Int10.CURSOR_POS_ROW(page);
                    cur_col = Int10.CURSOR_POS_COL(page);
                } while ((cur_col % 8) != 0);
                break;
            default:
                WriteChar(cur_col, cur_row, page, chr, attr, useattr);
                cur_col++;
        }
        if (cur_col == ncols) {
            cur_col = 0;
            cur_row++;
        }
        if (cur_row == nrows) {
            short fill = (short) ((Int10_modes.CurMode.type == VGA.M_TEXT) ? 0x7 : 0);
            INT10_ScrollWindow((short) 0, (short) 0, (short) (nrows - 1), (short) (ncols - 1), (byte) -1, fill, page);
            cur_row--;
        }
        INT10_SetCursorPos(cur_row, cur_col, page);
    }

    public static void INT10_TeletypeOutputAttr(int chr, int attr, boolean useattr) {
        INT10_TeletypeOutputAttr((short) chr, (short) attr, useattr, Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_PAGE));
    }

    public static void INT10_TeletypeOutput(int chr, int attr) {
        INT10_TeletypeOutputAttr((short) chr, (short) attr, Int10_modes.CurMode.type != VGA.M_TEXT);
    }

    public static void INT10_WriteString(short row, short col, short flag, short attr, int string, int count, short page) {
        short cur_row = Int10.CURSOR_POS_ROW(page);
        short cur_col = Int10.CURSOR_POS_COL(page);
        if (row == 0xff) {
            row = cur_row;
            col = cur_col;
        }
        INT10_SetCursorPos(row, col, page);
        while (count > 0) {
            short chr = Memory.mem_readb(string);
            string++;
            if ((flag & 2) != 0) {
                attr = Memory.mem_readb(string);
                string++;
            }
            INT10_TeletypeOutputAttr(chr, attr, true, page);
            count--;
        }
        if ((flag & 1) == 0) {
            INT10_SetCursorPos(cur_row, cur_col, page);
        }
    }
}
