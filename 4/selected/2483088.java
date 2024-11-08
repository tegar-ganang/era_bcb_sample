package jdvi;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.applet.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.net.*;
import java.lang.*;
import jdvi.font.*;
import javatex.Frame1;

/**
 * This class stores all document wide information
 */
public class JDviDocument {

    public URL document;

    JDviContext context;

    public static final double a4widthIn = 8.26;

    public static final double a4heightIn = 11.69;

    /**
     * paperWidth is the width of the paper to draw on in inch
     */
    public double paperWidth = a4widthIn;

    /**
     * paperHeight is the height of the paper to draw on in inch
     */
    public double paperHeight = a4heightIn;

    ArrayFile file = null;

    int trueDviDpi;

    String fontBase;

    public jdvi.font.Font font[] = new jdvi.font.Font[256];

    Page page[];

    /**
     * targetHash is a hashtable storing the names and pagenumbers of
     * href targets in the dvi file.
     */
    Hashtable targetHash = new Hashtable();

    public Color textColor;

    long num;

    long den;

    long mag;

    double scale;

    public int stackDepth;

    public JDviDocument(JDviContext c, String fontURL, int trueDpi) {
        fontBase = fontURL;
        trueDviDpi = trueDpi;
        context = c;
        paperWidth = getDouble("jdvi.paper.width", a4widthIn);
        paperHeight = getDouble("jdvi.paper.height", a4heightIn);
        if (context.getProperty("jdvi.font.alpha", "").equalsIgnoreCase("true")) {
            Glyph.v2 = true;
            Frame1.writelog("using alpha in Bitmap");
        } else {
            Frame1.writelog("using no alpha in Bitmap");
        }
    }

    public JDviDocument(JDviContext c, URL fileURL, String fontURL, int trueDpi) throws java.io.IOException {
        this(c, fontURL, trueDpi);
        document = fileURL;
        loadDviURL();
        initialize();
    }

    public JDviDocument(JDviContext c, ArrayFile aFile, String fontURL, int trueDpi) throws java.io.IOException {
        this(c, fontURL, trueDpi);
        document = null;
        file = aFile;
        initialize();
    }

    private void initialize() throws java.io.IOException {
        parsePre();
        if (context != null) {
            String fontCache = context.getProperty("jdvi.font.cache");
            if (fontCache != null) try {
                URL cacheURL = new URL(document, fontCache);
                InputStream istream = cacheURL.openStream();
                InflaterInputStream cacheIn = new InflaterInputStream(istream);
                ObjectInputStream fontIn = new ObjectInputStream(cacheIn);
                font = (jdvi.font.Font[]) fontIn.readObject();
                istream.close();
                Frame1.writelog("read fonts from cache file.");
            } catch (java.io.IOException e2) {
                System.err.println(e2);
            } catch (java.lang.ClassNotFoundException e3) {
                System.err.println(e3);
            }
        } else Frame1.writelog("context null!.");
        parsePost();
    }

    private void loadDviURL() throws java.io.IOException {
        URLConnection uc = document.openConnection();
        uc.connect();
        int length = uc.getContentLength();
        InputStream s = uc.getInputStream();
        file = new ArrayFile(length);
        for (int i = 0; i < length; i++) {
            file.data[i] = (byte) s.read();
        }
    }

    private void parsePre() throws java.io.EOFException {
        file.pos = 0;
        int head = file.read();
        int id = file.read();
        if ((head != 247) | (id != 2)) {
            Frame1.writelog("This semms to be no valid dvi file!");
            return;
        }
        num = file.readUnsignedInt();
        den = file.readUnsignedInt();
        mag = file.readUnsignedInt();
        scale = (double) mag * (double) num / ((double) den * 1000.0);
        id = file.read();
        byte idchars[] = new byte[id];
        file.read(idchars);
        Frame1.writelog("DVI comment: " + (new String(idchars)));
    }

    /**
     * read the postamble, initialize the page(offset)s and read the fonts.
     * Then buid the hashtable that stores the hyperlink targets.
     */
    private void parsePost() throws java.io.EOFException {
        file.pos = file.data.length - 1;
        while (file.read() == 223) {
            file.pos--;
            file.pos--;
        }
        file.pos -= 5;
        file.pos = (int) file.readUnsignedInt();
        Frame1.writelog("This should be 248: " + file.read());
        int prevPage = (int) file.readUnsignedInt();
        file.pos += 20;
        stackDepth = file.readUnsignedShort();
        page = new Page[file.readUnsignedShort()];
        Frame1.writelog(page.length + " pages");
        int head = file.read();
        double size;
        int num;
        String name;
        int l;
        while (head != 249) {
            switch(head) {
                case 138:
                    break;
                case 243:
                    num = file.read();
                    file.pos += 4;
                    size = (0.001 * file.readUnsignedInt() * mag * trueDviDpi / file.readUnsignedInt());
                    l = file.read();
                    l += file.read();
                    name = new String(file.data, file.pos, l);
                    file.pos += l;
                    if ((font[num] == null) || !(font[num].name.equals(name)) || !(font[num].size == size)) {
                        font[num] = null;
                        int i = 0;
                        while (i < 2 && font[num] == null) {
                            String template = context.getProperty("jdvi.font.nameformat", "%name%.%dpi%pk");
                            StringTokenizer tok = new StringTokenizer(template, "%");
                            String fname = "";
                            String tmp = null;
                            while (tok.hasMoreTokens()) {
                                tmp = tok.nextToken();
                                if (tmp.equals("name")) fname = fname + name; else if (tmp.equals("dpi")) fname = fname + ((int) (size) + i); else fname = fname + tmp;
                            }
                            font[num] = jdvi.font.Font.loadFont(fontBase, fname);
                            if (font[num] == null) {
                                if (i == 1) context.inform("Font " + num + ":" + fname + " not found.");
                            } else {
                                context.inform("Loaded font " + num + ":" + fname);
                                font[num].name = name;
                                font[num].size = size;
                                break;
                            }
                            i++;
                        }
                    }
                    break;
            }
            head = file.read();
        }
        int i = page.length - 1;
        textColor = JDviColor.getColor(context, "textColor", Color.black);
        while (prevPage != -1) {
            page[i] = new Page(prevPage, textColor);
            i--;
            file.pos = prevPage + 41;
            prevPage = (int) file.readUnsignedInt();
        }
        for (int j = 0; j < this.page.length; j++) scanPage(j);
    }

    Stack scanStack;

    Color scanCc;

    Color scanBg = Color.white;

    /**
     * This method parses a page to get the backgroud color,
     * the first text color, and the link targets.
     * @param n the number of the page to scan.
     */
    private void scanPage(int n) {
        int tmp = 0;
        ArrayFile file = new ArrayFile(this.file);
        Page page = this.page[n];
        file.pos = page.pos;
        scanStack = (Stack) page.textColors.clone();
        scanCc = (Color) scanStack.pop();
        page.setBackground(scanBg);
        try {
            int head = file.read();
            while (head != 140) {
                if (head > 127) switch(head) {
                    case 138:
                    case 141:
                    case 142:
                    case 147:
                    case 152:
                    case 161:
                    case 166:
                        break;
                    case 128:
                    case 133:
                    case 143:
                    case 148:
                    case 153:
                    case 157:
                    case 162:
                    case 167:
                    case 235:
                        file.pos++;
                        break;
                    case 129:
                    case 134:
                    case 144:
                    case 149:
                    case 154:
                    case 158:
                    case 163:
                    case 168:
                    case 236:
                        file.pos += 2;
                        break;
                    case 130:
                    case 135:
                    case 145:
                    case 150:
                    case 155:
                    case 159:
                    case 164:
                    case 169:
                    case 237:
                        file.pos += 3;
                        break;
                    case 131:
                    case 136:
                    case 146:
                    case 151:
                    case 156:
                    case 160:
                    case 165:
                    case 170:
                    case 238:
                        file.pos += 4;
                        break;
                    case 132:
                    case 137:
                        file.pos += 8;
                        break;
                    case 239:
                        tmp = file.read();
                        scanSpecial(new String(file.data, file.pos, tmp), page, n);
                        file.pos += tmp;
                        break;
                    case 240:
                        tmp = file.readUnsignedShort();
                        scanSpecial(new String(file.data, file.pos, tmp), page, n);
                        file.pos += tmp;
                        break;
                    case 241:
                        tmp = file.read3();
                        scanSpecial(new String(file.data, file.pos, tmp), page, n);
                        file.pos += tmp;
                        break;
                    case 242:
                        tmp = file.readInt();
                        scanSpecial(new String(file.data, file.pos, tmp), page, n);
                        file.pos += tmp;
                        break;
                    case 246:
                        file.pos++;
                    case 245:
                        file.pos++;
                    case 244:
                        file.pos++;
                    case 243:
                        file.pos += 13;
                        tmp = file.read();
                        tmp += file.read();
                        file.pos += tmp;
                        break;
                    case 247:
                        System.err.println("Found a preamble in page " + n);
                        break;
                    case 248:
                        System.err.println("Found a postamble in page " + n);
                        break;
                    case 249:
                        System.err.println("Found a post-post in page " + n);
                        break;
                    case 139:
                        page.c0 = file.readInt();
                        page.c1 = file.readInt();
                        page.c2 = file.readInt();
                        page.c3 = file.readInt();
                        page.c4 = file.readInt();
                        page.c5 = file.readInt();
                        page.c6 = file.readInt();
                        page.c7 = file.readInt();
                        page.c8 = file.readInt();
                        page.c9 = file.readInt();
                        file.readInt();
                        break;
                    case 171:
                        break;
                }
                head = file.read();
            }
        } catch (java.io.EOFException e) {
            Frame1.writelog(e + " occured while scanning page " + n);
        }
        scanStack.push(scanCc);
        if (this.page.length > n + 1) this.page[n + 1].setColorStack(scanStack);
    }

    /**
     * this is called by scanPage(n) when a special occurs.
     * @param s the special as a string
     * @param page the page on which  t he special occured.
     * @param n the pagenumber.
     */
    private final void scanSpecial(String s, Page thePage, int n) {
        StringTokenizer st = new StringTokenizer(s, " \"=<>,");
        String token = st.nextToken();
        if (token.equalsIgnoreCase("background")) {
            thePage.setBackground(JDviColor.parseColor(context, st));
            scanBg = thePage.background;
            return;
        }
        if (token.startsWith("html")) {
            token = st.nextToken();
            if (token.equalsIgnoreCase("a")) {
                token = st.nextToken();
                if (token.equalsIgnoreCase("name")) {
                    String sss = st.nextToken("\"");
                    sss = st.nextToken();
                    targetHash.put(sss, new Integer(n));
                }
            }
            return;
        }
        if (token.equalsIgnoreCase("color")) {
            token = st.nextToken();
            if (token.equalsIgnoreCase("pop")) {
                scanCc = (Color) scanStack.pop();
                return;
            }
            if (token.equalsIgnoreCase("push")) {
                scanStack.push(scanCc);
                scanCc = JDviColor.parseColor(context, st);
                return;
            }
        }
        if (token.equalsIgnoreCase("papersize")) {
            token = st.nextToken("=,");
            double d = parseDim(token);
            if (d > 0) {
                paperWidth = d;
            }
            token = st.nextToken();
            d = parseDim(token);
            if (d > 0) {
                paperHeight = d;
            }
            return;
        }
    }

    /**
     * gives the argumets TeX dimension as an double in inch (in)
     * @param s the string to be parsed. Should be a floating point number with any
     * of the following units : in (inch), pt (point), bp (big point), pc (pica),
     * cm (centimeter), mm (millimeter), dd (did^ot), cc (cicero), sp (scaled point)
     * appended.
     */
    public static double parseDim(String s) {
        double d = -1.;
        try {
            if (s.endsWith("in")) d = (new Double(s.substring(0, s.length() - 2))).doubleValue(); else if (s.endsWith("pt")) d = (new Double(s.substring(0, s.length() - 2))).doubleValue() / 72.27; else if (s.endsWith("bp")) d = (new Double(s.substring(0, s.length() - 2))).doubleValue() / 72.; else if (s.endsWith("pc")) d = (new Double(s.substring(0, s.length() - 2))).doubleValue() / (12 * 72.27); else if (s.endsWith("cm")) d = (new Double(s.substring(0, s.length() - 2))).doubleValue() / 2.5322355; else if (s.endsWith("mm")) d = (new Double(s.substring(0, s.length() - 2))).doubleValue() / 25.322355; else if (s.endsWith("dd")) d = (new Double(s.substring(0, s.length() - 2))).doubleValue() / (1238 * 72.27); else if (s.endsWith("cc")) d = (new Double(s.substring(0, s.length() - 2))).doubleValue() / (12 * 1238 * 72.27); else if (s.endsWith("sp")) d = (new Double(s.substring(0, s.length() - 2))).doubleValue() / (65536 * 72.27);
        } finally {
            return d;
        }
    }

    private double getDouble(String name, double dflt) {
        double d = dflt;
        try {
            d = (new Double(context.getProperty(name, "xxx"))).doubleValue();
        } catch (java.lang.NumberFormatException e) {
            Frame1.writelog("Formaterror while parsing " + name + "=" + context.getProperty(name) + " as a double using default " + dflt + " insead");
            d = dflt;
        } finally {
            return d;
        }
    }
}
