package jtelnet;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

public class VT100Telnet extends jTelnet {

    static final int maxInt = 2147483647;

    int begScroll, endScroll;

    int[] tabStop;

    static byte ansiUp[] = { 27, 91, 65 };

    static byte ansiDown[] = { 27, 91, 66 };

    static byte ansiRight[] = { 27, 91, 67 };

    static byte ansiLeft[] = { 27, 91, 68 };

    public boolean keyUp(Event evt, int key) {
        if (key < 1000) {
            byte msg[] = { (byte) key };
            try {
                sOut.write(msg);
            } catch (Exception e) {
            }
            ;
        } else {
            byte msg2[] = {};
            switch(key) {
                case 1004:
                    msg2 = ansiUp;
                    break;
                case 1005:
                    msg2 = ansiDown;
                    break;
                case 1006:
                    msg2 = ansiLeft;
                    break;
                case 1007:
                    msg2 = ansiRight;
                    break;
                default:
                    System.out.println("key...");
                    System.out.println(key);
            }
            try {
                sOut.write(msg2);
            } catch (Exception e) {
            }
            ;
        }
        return (true);
    }

    void VTReset() {
        initProcess();
        begScroll = 0;
        nparam = 0;
        saveX = 0;
        saveY = 0;
        scstype = 0;
        curss = 0;
        curgl = 0;
        curgr = 0;
    }

    void initProcess() {
        wrap = false;
        parseState = groundtable;
        param = new int[10];
        for (int i = 0; i < 10; i++) {
            param[i] = DEFAULT;
        }
        tabStop = new int[100];
        for (int i = 0; i < columns / 8; i++) {
            tabStop[i] = i * 8;
        }
        for (int i = columns / 8; i < 100; i++) {
            tabStop[i] = maxInt;
        }
        endScroll = lines;
    }

    public VT100Telnet(String host, int port, String login, String pass) throws IOException {
        super(null, host, port, login, pass, null);
        interact = true;
    }

    public void tabClear() {
        for (int i = 0; i < 99; i++) {
            tabStop[i] = maxInt;
        }
    }

    public void tabSet() {
        int i = 0;
        while (xloc > tabStop[i]) i++;
        if (xloc == tabStop[i]) return;
        if (tabStop[i] < maxInt) for (int j = 99; j < i; j--) {
            tabStop[j] = tabStop[j - 1];
        }
        tabStop[i] = xloc;
    }

    public void tabZonk() {
        tabClear();
    }

    public int tabNext(int col) {
        int i = 0;
        while (col >= tabStop[i]) i++;
        int x = columns - 1 < tabStop[i] ? columns - 1 : tabStop[i];
        if (debug) System.out.println(x);
        return (x);
    }

    void scrollRegionDown(int beg, int end) {
        for (int j = end; j > beg; j--) {
            screen[j] = screen[j - 1];
            screenfg[j] = screenfg[j - 1];
            screenbg[j] = screenbg[j - 1];
            lineRedraw[j] = true;
        }
        screen[beg] = new char[columns];
        screenfg[beg] = new Color[columns];
        screenbg[beg] = new Color[columns];
        for (int j = 0; j < columns; j++) {
            screenfg[beg][j] = fgcolor;
            screenbg[beg][j] = bgcolor;
        }
        lineRedraw[beg] = true;
    }

    void scrollRegionUp(int beg, int end) {
        for (int j = beg; j < end; j++) {
            screen[j] = screen[j + 1];
            screenfg[j] = screenfg[j + 1];
            screenbg[j] = screenbg[j + 1];
            lineRedraw[j] = true;
        }
        screen[end] = new char[columns];
        screenfg[end] = new Color[columns];
        screenbg[end] = new Color[columns];
        for (int j = 0; j < columns; j++) {
            screenfg[end][j] = fgcolor;
            screenbg[end][j] = bgcolor;
        }
        lineRedraw[end] = true;
    }

    public void insertLine(int n) {
        if (!(yloc > begScroll && yloc < endScroll)) return;
        for (; 0 < n; n--) scrollRegionDown(begScroll, endScroll);
    }

    public void deleteLine(int n) {
        if (!(yloc > begScroll && yloc < endScroll)) return;
        for (; 0 < n; n--) scrollRegionUp(begScroll, endScroll);
    }

    public void deleteChar(int n) {
        int tmp = xloc;
        n = n + xloc < columns ? n + xloc : columns;
        while (xloc < n) normal((byte) 32);
        xloc = tmp;
    }

    public void insertChar(int n) {
        for (int j = xloc; (j < n) && (j < columns - 1); j++) normal((byte) 32);
    }

    public void trackMouse(int func, int startrow, int startcol, int firstrow, int lastrow) {
    }

    public void cursorSet(int y, int x) {
        yloc = y;
        xloc = x;
        if (yloc >= lines) yloc = lines - 1;
        if (xloc >= columns) xloc = columns - 1;
        if (yloc < 0) yloc = 0;
        if (xloc < 0) xloc = 0;
    }

    static final int CASE_GROUND_STATE = 0;

    static final int CASE_IGNORE_STATE = 1;

    static final int CASE_IGNORE_ESC = 2;

    static final int CASE_IGNORE = 3;

    static final int CASE_BELL = 4;

    static final int CASE_BS = 5;

    static final int CASE_CR = 6;

    static final int CASE_ESC = 7;

    static final int CASE_VMOT = 8;

    static final int CASE_TAB = 9;

    static final int CASE_SI = 10;

    static final int CASE_SO = 11;

    static final int CASE_SCR_STATE = 12;

    static final int CASE_SCS0_STATE = 13;

    static final int CASE_SCS1_STATE = 14;

    static final int CASE_SCS2_STATE = 15;

    static final int CASE_SCS3_STATE = 16;

    static final int CASE_ESC_IGNORE = 17;

    static final int CASE_ESC_DIGIT = 18;

    static final int CASE_ESC_SEMI = 19;

    static final int CASE_DEC_STATE = 20;

    static final int CASE_ICH = 21;

    static final int CASE_CUU = 22;

    static final int CASE_CUD = 23;

    static final int CASE_CUF = 24;

    static final int CASE_CUB = 25;

    static final int CASE_CUP = 26;

    static final int CASE_ED = 27;

    static final int CASE_EL = 28;

    static final int CASE_IL = 29;

    static final int CASE_DL = 30;

    static final int CASE_DCH = 31;

    static final int CASE_DA1 = 32;

    static final int CASE_TRACK_MOUSE = 33;

    static final int CASE_TBC = 34;

    static final int CASE_SET = 35;

    static final int CASE_RST = 36;

    static final int CASE_SGR = 37;

    static final int CASE_CPR = 38;

    static final int CASE_DECSTBM = 39;

    static final int CASE_DECREQTPARM = 40;

    static final int CASE_DECSET = 41;

    static final int CASE_DECRST = 42;

    static final int CASE_DECALN = 43;

    static final int CASE_GSETS = 44;

    static final int CASE_DECSC = 45;

    static final int CASE_DECRC = 46;

    static final int CASE_DECKPAM = 47;

    static final int CASE_DECKPNM = 48;

    static final int CASE_IND = 49;

    static final int CASE_NEL = 50;

    static final int CASE_HTS = 51;

    static final int CASE_RI = 52;

    static final int CASE_SS2 = 53;

    static final int CASE_SS3 = 54;

    static final int CASE_CSI_STATE = 55;

    static final int CASE_OSC = 56;

    static final int CASE_RIS = 57;

    static final int CASE_LS2 = 58;

    static final int CASE_LS3 = 59;

    static final int CASE_LS3R = 60;

    static final int CASE_LS2R = 61;

    static final int CASE_LS1R = 62;

    static final int CASE_PRINT = 63;

    static final int CASE_XTERM_SAVE = 64;

    static final int CASE_XTERM_RESTORE = 65;

    static final int CASE_XTERM_TITLE = 66;

    static final int CASE_DECID = 67;

    static final int CASE_HP_MEM_LOCK = 68;

    static final int CASE_HP_MEM_UNLOCK = 69;

    static final int CASE_HP_BUGGY_LL = 70;

    static final int CASE_NUL = 71;

    static final int DEFAULT = -1;

    int nparam;

    int param[];

    int parseState[];

    int row, col, top, bot;

    int saveX, saveY;

    int scstype;

    int curss;

    int curgl;

    int curgr;

    void process(byte c) {
        if (debug) {
            if (parseState[((int) c) & 0xFF] != CASE_PRINT) {
                System.out.print("parseState: ");
                System.out.print(parseState[((int) c) & 0xFF]);
                System.out.print(" ");
                System.out.println((char) c);
            } else if ((int) c < 32) {
                System.out.print("parseState: ");
                System.out.print(parseState[((int) c) & 0xFF]);
                System.out.print(" ");
                System.out.println((int) c);
            }
        }
        switch(parseState[((int) c) & 0xFF]) {
            case CASE_PRINT:
                normal(c);
                break;
            case CASE_GROUND_STATE:
                parseState = groundtable;
                break;
            case CASE_IGNORE_STATE:
                parseState = igntable;
                break;
            case CASE_IGNORE_ESC:
                parseState = iestable;
                break;
            case CASE_IGNORE:
                break;
            case CASE_BELL:
                break;
            case CASE_BS:
                xloc--;
                if (xloc < 0) {
                    yloc--;
                    xloc = columns - 1;
                    if (yloc < 0) yloc = 0;
                }
                break;
            case CASE_CR:
                xloc = 0;
                parseState = groundtable;
                break;
            case CASE_NUL:
                break;
            case CASE_ESC:
                parseState = esctable;
                break;
            case CASE_VMOT:
                yloc++;
                scrValid();
                lineRedraw[yloc] = true;
                if (debug) System.out.println("$");
                parseState = groundtable;
                break;
            case CASE_TAB:
                xloc = tabNext(xloc);
                break;
            case CASE_SI:
                curgl = 0;
                break;
            case CASE_SO:
                curgl = 1;
                break;
            case CASE_SCR_STATE:
                parseState = scrtable;
                break;
            case CASE_SCS0_STATE:
                scstype = 0;
                parseState = scstable;
                break;
            case CASE_SCS1_STATE:
                scstype = 1;
                parseState = scstable;
                break;
            case CASE_SCS2_STATE:
                scstype = 2;
                parseState = scstable;
                break;
            case CASE_SCS3_STATE:
                scstype = 3;
                parseState = scstable;
                break;
            case CASE_ESC_IGNORE:
                parseState = eigtable;
                break;
            case CASE_ESC_DIGIT:
                if ((row = param[nparam - 1]) == DEFAULT) row = 0;
                param[nparam - 1] = 10 * row + (c - 48);
                break;
            case CASE_ESC_SEMI:
                param[nparam++] = DEFAULT;
                break;
            case CASE_DEC_STATE:
                parseState = dectable;
                break;
            case CASE_ICH:
                if ((row = param[0]) < 1) row = 1;
                insertChar(row);
                parseState = groundtable;
                break;
            case CASE_CUU:
                if ((param[0]) < 1) yloc--; else yloc -= param[0];
                if (yloc < 0) yloc = 0;
                parseState = groundtable;
                break;
            case CASE_CUD:
                if ((param[0]) < 1) yloc++; else yloc += param[0];
                if (yloc >= lines) yloc = lines - 1;
                parseState = groundtable;
                break;
            case CASE_CUF:
                if ((param[0]) < 1) xloc++; else xloc += param[0];
                if (xloc >= columns) xloc = columns - 1;
                parseState = groundtable;
                break;
            case CASE_CUB:
                if ((param[0]) < 1) xloc--; else xloc -= param[0];
                if (xloc < 0) xloc = 0;
                parseState = groundtable;
                break;
            case CASE_CUP:
                if (debug) {
                    System.out.println("CASE_CUP");
                    System.out.print(param[0]);
                    System.out.print(" ");
                    System.out.println(param[1]);
                }
                if ((row = param[0]) < 1) row = 1;
                if (nparam < 2 || (col = param[1]) < 1) col = 1;
                cursorSet(row - 1, col - 1);
                parseState = groundtable;
                break;
            case CASE_HP_BUGGY_LL:
                cursorSet(lines - 1, 0);
                parseState = groundtable;
                break;
            case CASE_ED:
                switch(param[0]) {
                    case DEFAULT:
                    case 0:
                        clearLines(yloc, lines - 1);
                        break;
                    case 1:
                        clearLines(0, yloc - 1);
                        break;
                    case 2:
                        clearLines(0, lines - 1);
                        xloc = 0;
                        yloc = 0;
                        break;
                }
                parseState = groundtable;
                break;
            case CASE_EL:
                switch(param[0]) {
                    case DEFAULT:
                    case 0:
                        clearLine(xloc, columns - 1);
                        break;
                    case 1:
                        clearLine(0, xloc - 1);
                        break;
                    case 2:
                        clearLine(0, columns - 1);
                        break;
                }
                parseState = groundtable;
                break;
            case CASE_IL:
                if ((row = param[0]) < 1) row = 1;
                insertLine(row);
                parseState = groundtable;
                break;
            case CASE_DL:
                if ((row = param[0]) < 1) row = 1;
                deleteLine(row);
                parseState = groundtable;
                break;
            case CASE_DCH:
                if ((row = param[0]) < 1) row = 1;
                deleteChar(row);
                parseState = groundtable;
                break;
            case CASE_TRACK_MOUSE:
                trackMouse(param[0], param[2] - 1, param[1] - 1, param[3] - 1, param[4] - 2);
                break;
            case CASE_DECID:
                param[0] = -1;
            case CASE_DA1:
                if (param[0] <= 0) {
                }
                parseState = groundtable;
                break;
            case CASE_TBC:
                if ((row = param[0]) <= 0) tabClear(); else if (row == 3) tabZonk();
                parseState = groundtable;
                break;
            case CASE_SET:
                parseState = groundtable;
                break;
            case CASE_RST:
                parseState = groundtable;
                break;
            case CASE_SGR:
                rendition(param, nparam);
                parseState = groundtable;
                break;
            case CASE_CPR:
                if ((row = param[0]) == 5) {
                } else if (row == 6) {
                }
                parseState = groundtable;
                break;
            case CASE_HP_MEM_LOCK:
            case CASE_HP_MEM_UNLOCK:
                if (parseState[c] == CASE_HP_MEM_LOCK) begScroll = yloc; else begScroll = 0;
                parseState = groundtable;
                break;
            case CASE_DECSTBM:
                if ((top = param[0]) < 1) top = 1;
                if (nparam < 2 || (bot = param[1]) == DEFAULT || bot > lines + 1 || bot == 0) bot = lines + 1;
                if (bot > top) {
                    begScroll = top - 1;
                    endScroll = bot - 1;
                    cursorSet(0, 0);
                }
                parseState = groundtable;
                break;
            case CASE_DECREQTPARM:
                if ((row = param[0]) == DEFAULT) row = 0;
                if (row == 0 || row == 1) {
                }
                parseState = groundtable;
                break;
            case CASE_DECSET:
                parseState = groundtable;
                break;
            case CASE_DECRST:
                parseState = groundtable;
                break;
            case CASE_DECALN:
                parseState = groundtable;
                break;
            case CASE_GSETS:
                parseState = groundtable;
                break;
            case CASE_DECSC:
                saveX = xloc;
                saveY = yloc;
                parseState = groundtable;
                break;
            case CASE_DECRC:
                xloc = saveX;
                yloc = saveY;
                parseState = groundtable;
                break;
            case CASE_DECKPAM:
                parseState = groundtable;
                break;
            case CASE_DECKPNM:
                parseState = groundtable;
                break;
            case CASE_IND:
                parseState = groundtable;
                break;
            case CASE_NEL:
                parseState = groundtable;
                break;
            case CASE_HTS:
                tabSet();
                parseState = groundtable;
                break;
            case CASE_RI:
                parseState = groundtable;
                break;
            case CASE_SS2:
                curss = 2;
                parseState = groundtable;
                break;
            case CASE_SS3:
                curss = 3;
                parseState = groundtable;
                break;
            case CASE_CSI_STATE:
                nparam = 1;
                param[0] = DEFAULT;
                parseState = csitable;
                break;
            case CASE_OSC:
                parseState = groundtable;
                break;
            case CASE_RIS:
                VTReset();
                parseState = groundtable;
                break;
            case CASE_LS2:
                curgl = 2;
                parseState = groundtable;
                break;
            case CASE_LS3:
                curgl = 3;
                parseState = groundtable;
                break;
            case CASE_LS3R:
                curgr = 3;
                parseState = groundtable;
                break;
            case CASE_LS2R:
                curgr = 2;
                parseState = groundtable;
                break;
            case CASE_LS1R:
                curgr = 1;
                parseState = groundtable;
                break;
            case CASE_XTERM_SAVE:
                parseState = groundtable;
                break;
            case CASE_XTERM_RESTORE:
                parseState = groundtable;
                break;
        }
    }

    void rendition(int mode[], int count) {
        if (count <= 0) {
            mode[0] = 0;
            count = 1;
        }
        for (int j = 0; j < count; j++) {
            if (debug) {
                System.out.print("mode: ");
                System.out.print(mode[j]);
                System.out.print(" ");
                System.out.println(j);
            }
            if (mode[j] < 10) {
                switch(mode[j]) {
                    case -1:
                    case 0:
                        fgcolor = Color.black;
                        bgcolor = Color.white;
                        break;
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                        break;
                    case 7:
                        Color tmp = fgcolor;
                        fgcolor = bgcolor;
                        bgcolor = tmp;
                        break;
                    case 8:
                        fgcolor = bgcolor;
                }
            } else if (mode[j] < 40) {
                switch(mode[j]) {
                    case 27:
                    case 30:
                    case 31:
                    case 32:
                    case 33:
                    case 34:
                    case 35:
                    case 36:
                    case 37:
                }
            } else switch(mode[j]) {
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
            }
        }
    }

    void clearLines(int beg, int end) {
        for (int j = beg; j <= end; j++) {
            lineRedraw[j] = true;
            for (int i = 0; i < columns; i++) {
                screen[j][i] = ' ';
                screenfg[j][i] = fgcolor;
                screenbg[j][i] = bgcolor;
            }
        }
    }

    void clearLine(int beg, int end) {
        lineRedraw[yloc] = true;
        for (int j = beg; j <= end; j++) {
            screen[yloc][j] = ' ';
            screenfg[yloc][j] = fgcolor;
            screenbg[yloc][j] = bgcolor;
        }
    }

    static int groundtable[] = { CASE_NUL, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_BELL, CASE_BS, CASE_TAB, CASE_VMOT, CASE_VMOT, CASE_VMOT, CASE_CR, CASE_SO, CASE_SI, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_ESC, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_GROUND_STATE, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT, CASE_PRINT };

    static int csitable[] = { CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_BELL, CASE_BS, CASE_TAB, CASE_VMOT, CASE_VMOT, CASE_VMOT, CASE_CR, CASE_SO, CASE_SI, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_ESC, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_IGNORE, CASE_ESC_SEMI, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_DEC_STATE, CASE_ICH, CASE_CUU, CASE_CUD, CASE_CUF, CASE_CUB, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_CUP, CASE_GROUND_STATE, CASE_ED, CASE_EL, CASE_IL, CASE_DL, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_DCH, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_TRACK_MOUSE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_DA1, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_CUP, CASE_TBC, CASE_SET, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_RST, CASE_SGR, CASE_CPR, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_DECSTBM, CASE_DECSC, CASE_GROUND_STATE, CASE_DECRC, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_DECREQTPARM, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE };

    static int dectable[] = { CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_BELL, CASE_BS, CASE_TAB, CASE_VMOT, CASE_VMOT, CASE_VMOT, CASE_CR, CASE_SO, CASE_SI, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_ESC, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_ESC_DIGIT, CASE_IGNORE, CASE_ESC_SEMI, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_DECSET, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_DECRST, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_XTERM_RESTORE, CASE_XTERM_SAVE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE };

    static int eigtable[] = { CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_BELL, CASE_BS, CASE_TAB, CASE_VMOT, CASE_VMOT, CASE_VMOT, CASE_CR, CASE_SO, CASE_SI, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_ESC, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE };

    static int esctable[] = { CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_BELL, CASE_BS, CASE_TAB, CASE_VMOT, CASE_VMOT, CASE_VMOT, CASE_CR, CASE_SO, CASE_SI, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_ESC, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_SCR_STATE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_SCS0_STATE, CASE_SCS1_STATE, CASE_SCS2_STATE, CASE_SCS3_STATE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_DECSC, CASE_DECRC, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_DECKPAM, CASE_DECKPNM, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_IND, CASE_NEL, CASE_HP_BUGGY_LL, CASE_GROUND_STATE, CASE_HTS, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_RI, CASE_SS2, CASE_SS3, CASE_IGNORE_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_XTERM_TITLE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_DECID, CASE_CSI_STATE, CASE_GROUND_STATE, CASE_OSC, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_RIS, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_HP_MEM_LOCK, CASE_HP_MEM_UNLOCK, CASE_LS2, CASE_LS3, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_LS3R, CASE_LS2R, CASE_LS1R, CASE_GROUND_STATE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE };

    static int iestable[] = { CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_GROUND_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE_STATE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE };

    static int igntable[] = { CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_GROUND_STATE, CASE_IGNORE, CASE_GROUND_STATE, CASE_IGNORE_ESC, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE };

    static int scrtable[] = { CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_BELL, CASE_BS, CASE_TAB, CASE_VMOT, CASE_VMOT, CASE_VMOT, CASE_CR, CASE_SO, CASE_SI, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_ESC, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_DECALN, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE };

    static int scstable[] = { CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_BELL, CASE_BS, CASE_TAB, CASE_VMOT, CASE_VMOT, CASE_VMOT, CASE_CR, CASE_SO, CASE_SI, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_ESC, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_ESC_IGNORE, CASE_GSETS, CASE_GSETS, CASE_GSETS, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GSETS, CASE_GSETS, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_IGNORE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE, CASE_GROUND_STATE };
}
