package multivalent.std.adaptor.pdf;

import java.io.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.color.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.*;
import multivalent.*;
import multivalent.node.LeafAscii;
import multivalent.node.FixedI;
import multivalent.node.FixedIHBox;
import multivalent.node.FixedLeafAscii;
import multivalent.node.FixedLeafAsciiKern;
import multivalent.node.FixedLeafShape;
import multivalent.node.FixedLeafImage;
import multivalent.node.FixedIClip;
import multivalent.gui.VCheckbox;
import multivalent.gui.VMenu;
import multivalent.std.span.StrokeSpan;
import phelps.awt.color.*;
import phelps.lang.Integers;
import phelps.lang.Booleans;

/**
	Parse a page of PDF and display with Java 2's 2D API.


	<h3 id='doctree'>Document Tree Construction</h3>

	<p>The PDF content stream is translated into a Multivalent document tree as follows.
	The tree is live: reformat.  Objects drawn as appear in content stream, which usually but not necessarily follows reading order,
	To see the document tree for any particular PDF page, turn on the Debug switch in the Help menu, then select Debug/View Document Tree.

	<ul>
	<li>Text blocks (<code>BT</code>..<code>ET</code>) have subtrees rooted at a {@link FixedI} with name "text".
	Under that can be any number of lines, which collect text that have been determined to share the same baseline in {@link FixedIHBox}s named "line".
	(Some PDF generators generate an inordinate number of BT..ET blocks, as for instance on version of pdfTeX generated a block
	for each dot in a table of contents between header and page number, but most generators use for meaningful blocks of text.)
	PDF text streams are normalized to word chunks in {@link FixedLeafAsciiKern}s, with special kerning between letters, whether from TJ or Tz or small TD/TM/...,  stored in the leaf.
	Text rendered in a Type 3 font are {@link FixedLeafType3}, with no kerning.
	Text is translated into Unicode, from whatever original encoding (Macintosh, Macintosh Expert, Windows, PDF, Adobe Standard).  However, if the encoding is nonstandard and found only in font tables, it is not translated.
	Text content is available from the node via {@link Node#getName()}.

	<li>Images are stored in {@link FixedLeafImage}s.  The {@link java.awt.image.BufferedImage} is available via {@link multivalent.node.LeafImage#getImage()},
	and the image's colorspace via {@link java.awt.image.BufferedImage#getColorModel()}.
	Images from XObjects have the reference <code>/Name</code> as the GI,
	and inline images (<code>BI</code>..<code>ID</code>..<code>EI</code>) have the GI "[inline]".

	<li>Paths are {@link FixedLeafShape}s, with fill and stroke flags.
	Paths are kept as simple {@link java.awt.geom.Line2D} with GI "line" or {@link java.awt.Rectangle} with GI "rect" if possible, else {@link java.awt.geom.GeneralPath} with GI "path".
	, paths as Rectangle "rect" if possible, else "line", else GeneralPath "path",

	<li>For all leaf types (text, image, path), positioning is available from {@link Node#bbox},
	but the command positioning it there (<code>cm</code>, <code>Td</code>, ...) is not maintained.
	Transformation matrices (<code>cm</code>, <code>Tm</code>) are reflected in final sizes and not maintained as separate objects.

	<li>Colors are maintained as {@link SpanPDF}s, and all colors are translated into RGB.
	Fonts (family, size, style), text rise (<code>Ts</code>), text rendering mode (<code>Tr</code>) are all maintained as {@link SpanPDF}s.
	Other attributes (line width, line cap style, line join style, miter limit, dash array, ...) are all maintained as {@link SpanPDF}s
	such that if several change at once they are batched in same span and if any of the group changes a new span is started,
	which means that only one span for these attributes is active at any point.
	Sometimes a PDF generator produces redundant color/font/attribute changes (pdfTeX sets the color to <code>1 1 1 1 K</code> and again immediately to <code>1 1 1 1 K</code)
	or useless changes (e.g., setting the color and then setting it to something else without drawing anything) --
	all redundent and useless changes are optimized away.

	<li>Marked points (<code>MP</code>/<code>DP</code>) are {@link Mark}s, with the point name as the Mark name.
	Marked regions (<code>BMC</code>/<code>BDC</code>..<code>EMC</code>) are simple {@link multivalent.Span}s, with the region name as the Span name and with any region attributes in span attributes.

	<li>Clipping regions (<code>W</code>/<code>W*</code>) are {@link FixedIClip}.
	Clipping regions cannot be enlarged (push the clip onto the graphics stack with <code>q</code>..<code>Q</code> to temporarily reduce it),
	but some PDF generators don't know this:  useless clipping changes are optimized away.

	<li>Shading patterns are {@link FixedLeafShade}.

	<li>If a large filled rectangle appears before any other drawing, its color extracted as the page background and put into the {@link Document} {@link StyleSheet}.

	<li>If the PDF is determined to be a scanned paper and has OCR (but hasn't replaced text with outline fonts), it is transformed.
	OCR text (which is drawn in invisible mode <code>Tr 3</code> or overdrawn with image)
	is associated with the corresponding image fragment and transformed into {@link multivalent.node.FixedLeafOCR}, and the independent image os removed.
	(This allows hybrid image-OCR PDFs to work as expected with other behaviors, such as select and paste and the Show OCR lens.)

	<li>Annotations such as hyperlinks, are sent as semantic events with message {@link Anno#MSG_CREATE}.
	Other behaviors can translate them into entities on the document tree, often spans.

	<!--li>PDF commands are not maintained explicitly: q..Q, BX..EX, comments, different text drawing commands (Tj, TJ, ', "), changes in transformation matrices (cm, Tm), Form XObject interpolated, -->

	</ul>


	<h3>See Also</h3>
	<ul>
	<li>{@link PDFReader}
	<li>{@link PDFWriter} to write new PDF data format from Java data structures
	</ul>

	<p>Other PDF viewers:
	<ul>
	<li><a href='http://www.adobe.com/products/acrviewer/main.html'>Adobe's Java version of Acrobat</a>.
		It was designed for Java 1.1.8
		It runs as an AWT component.
		Development has been abandoned with PDF 1.4 (Acrobat 5.0),
		so it can't read PDFs that use PDF 1.5's (Acrobat 6.0) the new fine-grained encryption
		or space-saving object streams or cross-reference streams.
	<li><a href='http://www.pdfgo.com/'>PDFGo</a> - another Java-based PDF viewer
		(no Type 3 fonts, slow, crashes, no text selection, costs US$450; but
		great font shaping and runs as applet or application)
	<li><a href='http://www.foolabs.com/xpdf/'>xpdf</a> (not Java)
	</ul>

	@version $Revision: 1115 $ $Date: 2008-08-25 19:12:03 -0400 (Mon, 25 Aug 2008) $
*/
public class PDF extends multivalent.std.adaptor.MediaAdaptorRandom {

    static final boolean DEBUG = false && Multivalent.DEVEL;

    static boolean Dump_ = false;

    /** Message "pdfSetGoFast": faster rendering if sometimes less accurate: arg=boolean or null to toggle. */
    public static final String MSG_GO_FAST = "pdfSetGoFast";

    /** Message of semantic event to set the user password so encrypted files can be read, with the password String passed in <tt>arg</tt>. */
    public static final String MSG_OWNER_PASSWORD = "pdfUserPassword";

    /** Message of semantic event to set the owner password so encrypted files can be read, with the password String passed in <tt>arg</tt>. */
    public static final String MSG_USER_PASSWORD = "pdfOwnerPassword";

    /** Message of semantic event to control dumping of uncompress and decrypted content stream to temporary file. */
    public static final String MSG_DUMP = "pdfDump";

    /**
	Optional content groups stored in {@link Document} under this key.
	The value there is a {@link java.util.Map} with names of optional content groups as keys and {@link #OCG_ON} and {@link #OCG_OFF} as values.
  */
    public static final String VAR_OCG = "PDFOptionalContentGroups";

    public static final String OCG_ON = "ON";

    public static final String OCG_OFF = "OFF";

    private static final Matcher ALL_WS = Pattern.compile("\\s+").matcher("");

    /** Metadata that may be in PDF and is useful to Multivalent. */
    private static final String[] METADATA = { "Author", "Title", "Keywords", "Subject", "Producer", "Creator" };

    static final String BLANK_PAGE = "";

    static final boolean[] WHITESPACE = PDFReader.WHITESPACE, WSDL = PDFReader.WSDL, OP = PDFReader.OP;

    static final Object OBJECT_NULL = PDFReader.OBJECT_NULL;

    static final Class CLASS_DICTIONARY = PDFReader.CLASS_DICTIONARY, CLASS_ARRAY = PDFReader.CLASS_ARRAY, CLASS_NAME = PDFReader.CLASS_NAME, CLASS_STRING = PDFReader.CLASS_STRING;

    static {
        assert BasicStroke.CAP_BUTT == 0 && BasicStroke.CAP_ROUND == 1 && BasicStroke.CAP_SQUARE == 2;
        assert BasicStroke.JOIN_MITER == 0 && BasicStroke.JOIN_ROUND == 1 && BasicStroke.JOIN_BEVEL == 2;
    }

    static Map streamcmds_;

    static {
        String[] cmds = ("w/1 J/1 j/1 M/1 d/2 ri/1 i/1 gs/1   q/0 Q/0 cm/6   m/2 l/2 c/6 v/4 y/4 h/0 re/4   S/0 s/0 f/0 F/0 f*/0 B/0 B*/0 b/0 b*/0 n/0   W/0 W*/0" + " BT/0 ET/0   Tc/1 Tw/1 Tz/1 TL/1 Tf/2 Tr/1 Ts/1   Td/2 TD/2 Tm/6 T*/0   Tj/1 TJ/1 '/1 \"/3   d0/2 d1/6" + " CS/1 cs/1 SC/+ SCN/+ sc/+ scn/+ G/1 g/1 RG/3 rg/3 K/4 k/4   sh/1   BI/0 ID/0 EI/0   Do/1" + " MP/1 DP/2 BMC/1 BDC/2 EMC/0   BX/0 EX/0" + " %/0").split("\\s+");
        streamcmds_ = new HashMap(cmds.length * 2);
        for (int i = 0, imax = cmds.length; i < imax; i++) {
            String token = cmds[i];
            assert streamcmds_.get(token) == null && token.length() >= 1 + 2 && token.length() <= 3 + 2 : token;
            int x = token.indexOf('/');
            int arity = (token.charAt(x + 1) == '+' ? Integer.MAX_VALUE : token.charAt(x + 1) - '0');
            token = token.substring(0, x);
            assert "n".equals(token) || !token.startsWith("n");
            assert !Character.isDigit(token.charAt(0));
            streamcmds_.put(token, Integers.getInteger(arity));
        }
    }

    /** Go fast or be exactly correct. */
    public static boolean GoFast = true;

    private PDFReader pdfr_ = null;

    Rectangle cropbox_;

    private AffineTransform ctm_ = null;

    /** If encounter error, exception message placed here. */
    String fail_ = null;

    public boolean isAuthorized() {
        return getReader().isAuthorized();
    }

    public void setPassword(String pw) {
        getReader().setPassword(pw);
    }

    public PDFReader getReader() {
        return pdfr_;
    }

    public Rectangle getCropBox() {
        return new Rectangle(cropbox_);
    }

    public AffineTransform getTransform() {
        return new AffineTransform(ctm_);
    }

    static void getDoubles(Object[] ops, double[] d, int cnt) {
        assert ops != null && d != null && cnt > 0 && cnt <= d.length;
        for (int i = 0; i < cnt; i++) d[i] = ((Number) ops[i]).doubleValue();
    }

    static void getFloats(Object[] ops, float[] f, int cnt) {
        assert ops != null && f != null && cnt <= f.length;
        for (int i = 0; i < cnt; i++) f[i] = ((Number) ops[i]).floatValue();
    }

    /**
	Parse content stream of operators for <var>pagenum</var> into document tree.
	Pages are numbered 1 .. {@link #getPageCnt()}, inclusive.
	See PDF Reference 1.4, page 134.
	Colors and fonts are transformed into Span's.
	@param ctm  initial transform with scaling/zoom, to which the method adds page rotation and conversion to Java coordinates (Y goes down)
  */
    void buildPage(int pagenum, INode pageroot, AffineTransform ctm) throws IOException, ParseException {
        PDFReader pdfr = pdfr_;
        assert pagenum >= 1 && pagenum <= pdfr.getPageCnt() : pagenum + " >= " + pdfr.getPageCnt() + " (1-based)";
        assert pageroot != null;
        if (Multivalent.MONITOR) System.out.print(pagenum + ".  ");
        IRef pageref = pdfr.getPage(pagenum);
        Map page = (Map) pdfr.getObject(pageref);
        Number rotate = (Number) page.get("Rotate");
        int rot = rotate == null ? 0 : rotate.intValue() % 360;
        if (rot < 0) rot += 360;
        assert rot % 90 == 0 && rot >= 0 && rot < 360;
        ctm.rotate(Math.toRadians(-rot));
        Rectangle mediabox = PDFReader.array2Rectangle((Object[]) pdfr.getObject(page.get("MediaBox")), ctm, true);
        cropbox_ = (page.get("CropBox") != null ? PDFReader.array2Rectangle((Object[]) pdfr.getObject(page.get("CropBox")), ctm, true) : mediabox);
        double pw = (double) cropbox_.width, ph = (double) cropbox_.height;
        AffineTransform tmpat = new AffineTransform();
        if (rot == 0) tmpat.setToIdentity(); else if (rot == 90) tmpat.setToTranslation(0.0, ph); else if (rot == 180) tmpat.setToTranslation(pw, ph); else {
            assert rot == 270 : rot;
            tmpat.setToTranslation(pw, 0.0);
        }
        ctm.preConcatenate(tmpat);
        AffineTransform pdf2java = new AffineTransform(1.0, 0.0, 0.0, -1.0, 0.0 - cropbox_.x, ph + (rot == 0 || rot == 180 ? cropbox_.y : -cropbox_.y));
        ctm.preConcatenate(pdf2java);
        cropbox_.setLocation(0, 0);
        ctm_ = new AffineTransform(ctm);
        Object o = page.get("Contents");
        CompositeInputStream in = (o != null ? pdfr.getInputStream(o, true) : null);
        if (Dump_ && in != null) try {
            System.out.println("Contents dict = " + o + "/" + pdfr.getObject(o));
            File tmpf = File.createTempFile("pdf", ".stream");
            tmpf.deleteOnExit();
            FileOutputStream out = new FileOutputStream(tmpf);
            for (int c; (c = in.read()) != -1; ) out.write(c);
            in.close();
            out.close();
            System.out.println("wrote PDF content " + pageref + " to " + tmpf);
            in = pdfr.getInputStream(o, true);
        } catch (IOException ignore) {
            System.err.println("error writing stream: " + ignore);
        }
        List ocrimgs = new ArrayList(10);
        if (in != null) {
            Rectangle clipshape = new Rectangle(cropbox_);
            clipshape.translate(-clipshape.x, -clipshape.y);
            FixedIClip clipp = new FixedIClip("crop", null, pageroot, clipshape, new Rectangle(cropbox_));
            try {
                buildStream(page, clipp, ctm, in, ocrimgs);
            } catch (IOException ioe) {
                throw ioe;
            } catch (Exception pe) {
                pe.printStackTrace();
                throw new ParseException("corrupt content stream: " + pe.toString());
            } finally {
                in.close();
            }
        }
        if (pageroot.getFirstLeaf() == null) {
            pageroot.removeAllChildren();
            new FixedLeafAscii(BLANK_PAGE, null, pageroot);
        }
        assert checkTree("content stream", pageroot);
        OCR.extractBackground(pageroot, this);
        if (pageroot.size() == 0) new FixedLeafAscii("", null, pageroot);
        assert checkTree("bg", pageroot);
        OCR.transform(pageroot, ocrimgs, this);
        assert ocrimgs.size() == 0 || checkTree("OCR", pageroot);
        createAnnots(page, pageroot);
        assert page.get("Annots") == null || checkTree("annos", pageroot);
    }

    /**
	Used by buildPage(), recursively by Form XObject, and by Type 3 fonts.
	<!--
	This is a very long method (over 1000 lines), but it's awkward to break parts into their own methods
	as for most PDF commands there is a lot of state to pass and not much computation on it.
	-->

	@return number of commands processed
  */
    int buildStream(Map page, FixedIClip clipp, AffineTransform ctm, CompositeInputStream in, List ocrimgs) throws IOException, ParseException {
        PDFReader pdfr = pdfr_;
        Object[] ops = new Object[6];
        int opsi = 0;
        GraphicsState gs = new GraphicsState();
        List gsstack = new ArrayList(10);
        AffineTransform Tm = new AffineTransform(), Tlm = new AffineTransform(), tmpat = new AffineTransform();
        double Tc = 0.0, Tw = 0.0, Tz = 100.0, TL = 0.0, Ts = 0.0;
        int Tr = 0;
        GeneralPath path = new GeneralPath();
        Color color = Color.BLACK, fcolor = Color.BLACK;
        ColorSpace fCS = ColorSpace.getInstance(ColorSpace.CS_GRAY), sCS = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        float[] cscomp = new float[4];
        List markedseq = new ArrayList(5);
        double curx = 0.0, cury = 0.0;
        Map resources = (Map) pdfr.getObject(page.get("Resources") != null ? page.get("Resources") : new HashMap(1)), xores = (Map) pdfr.getObject(resources.get("XObject")), fontres = (Map) pdfr.getObject(resources.get("Font")), csres = (Map) pdfr.getObject(resources.get("ColorSpace")), patres = (Map) pdfr.getObject(resources.get("Pattern")), shres = (Map) pdfr.getObject(resources.get("Shading")), propres = (Map) pdfr.getObject(resources.get("Properties"));
        Document doc = clipp.getDocument();
        Layer scratchLayer = doc != null ? doc.getLayer(Layer.SCRATCH) : null;
        Point2D srcpt = new Point2D.Double(), transpt = new Point2D.Double();
        double[] d = new double[6];
        INode textp = null;
        FixedIHBox linep = null;
        double baseline = Double.MIN_VALUE;
        FontPDF tf = null;
        Object[] Tja = new Object[1];
        double spacew = 0.0, concatthreshold = 0.0;
        Rectangle2D maxr = null;
        double lastX = 0.0, totalW = 0.0;
        boolean fconcat = false;
        SpanPDF fontspan = null, sspan = null, fillspan = null, Trspan = null;
        StrokeSpan strokespan = null;
        Node lastleaf = clipp;
        boolean fnewfont = true, fnewline = true;
        boolean fstroke = false, ffill = false;
        boolean fvalidpath = false;
        Color newcolor = color, newfcolor = fcolor;
        int newTr = Tr;
        Map fontdict = gs.fontdict = null;
        double pointsize = gs.pointsize = 1.0;
        float linewidth = gs.linewidth = Context.DEFAULT_STROKE.getLineWidth();
        int linecap = gs.linecap = Context.DEFAULT_STROKE.getEndCap(), linejoin = gs.linejoin = Context.DEFAULT_STROKE.getLineJoin();
        float miterlimit = gs.miterlimit = Context.DEFAULT_STROKE.getMiterLimit();
        float[] dasharray = gs.dasharray = Context.DEFAULT_STROKE.getDashArray();
        float dashphase = gs.dashphase = Context.DEFAULT_STROKE.getDashPhase();
        Rectangle pathrect = null;
        Line2D pathline = null;
        boolean fshowshape = (getHints() & MediaAdaptor.HINT_NO_SHAPE) == 0;
        int cmdcnt = 0, leafcnt = 0, spancnt = 0, vspancnt = 0, concatcnt = 0;
        int pathcnt = 0, pathlen = 0;
        int[] pathlens = new int[5000];
        long start = System.currentTimeMillis();
        PDFReader.eatSpace(in);
        for (int c, peek = -1; (c = in.peek()) != -1; ) {
            if (OP[c]) {
                if (opsi >= 6) throw new ParseException("too many operands: " + ops[0] + " " + ops[1] + " ... " + ops[5] + " + more");
                ops[opsi++] = PDFReader.readObject(in);
            } else {
                c = in.read();
                int c2 = in.read(), c3 = -1, c2c3;
                if (c2 == -1 || WSDL[c2] || c == '%') {
                    peek = c2;
                    c2c3 = ' ';
                } else if ((c3 = in.read()) == -1 || WSDL[c3]) {
                    peek = c3;
                    c2c3 = c2;
                } else {
                    c2c3 = (c2 << 8) + c3;
                    peek = in.read();
                    if (peek != -1 && !WSDL[peek]) {
                        if (c == 'e' && c2 == 'n' && c3 == 'd' && peek == 's') break; else throw new ParseException("bad command or no trailing whitespace " + (char) c + (char) c2 + (char) c3 + " + " + peek);
                    }
                }
                cmdcnt++;
                if (DEBUG) {
                    StringBuffer scmd = new StringBuffer(3);
                    scmd.append((char) c);
                    if (c2c3 != ' ') {
                        scmd.append((char) c2);
                        if (c2c3 != c2) scmd.append((char) c3);
                    }
                    Integer arity = (Integer) streamcmds_.get(scmd.toString());
                    boolean ok = arity != null && (arity.intValue() == opsi || (arity.intValue() == Integer.MAX_VALUE && opsi > 0));
                    if (!ok) {
                        System.out.print((arity == null ? "unknown command" : ("bad arity " + opsi + " not " + arity)) + ": |" + scmd + "| [" + c + " " + c2 + "] ");
                        for (int i = 0; i < opsi; i++) System.out.println("\t" + ops[i]);
                        if (DEBUG) assert false;
                        return cmdcnt;
                    }
                }
                if (c != '%') while (peek != -1 && WHITESPACE[peek]) peek = in.read();
                in.unread(peek);
                switch(c) {
                    case 'B':
                        if (c2c3 == ' ') {
                            path.setWindingRule(GeneralPath.WIND_NON_ZERO);
                            ffill = fstroke = true;
                        } else if (c2c3 == '*') {
                            path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
                            ffill = fstroke = true;
                        } else if (c2c3 == 'T') {
                            if (clipp.size() > 0 && textp == clipp.getLastChild() && Math.abs(ctm.getTranslateX() - Tm.getTranslateX()) < 5.0 && Math.abs(ctm.getTranslateX() - Tm.getTranslateX()) < 0.001) {
                            } else {
                                textp = new FixedI("text", null, clipp);
                                linep = new FixedIHBox("line", null, textp);
                                fconcat = false;
                                baseline = Double.MIN_VALUE;
                            }
                            Tm.setTransform(ctm);
                            Tlm.setTransform(Tm);
                        } else if (c2c3 == 'I') {
                            BufferedImage img = Images.createScaledInline(in, csres, ctm, newfcolor, pdfr);
                            lastleaf = appendImage("[inline]", clipp, img, ctm);
                            leafcnt++;
                        } else if (c2c3 == ('M' << 8) + 'C' || c2c3 == ('D' << 8) + 'C') {
                            Map attrs = null;
                            if (c2 == 'D') attrs = (Map) (ops[1].getClass() == CLASS_DICTIONARY ? ops[1] : pdfr.getObject(propres.get(ops[1])));
                            Span seq = (Span) Behavior.getInstance((String) ops[0], "multivalent.Span", attrs, scratchLayer);
                            seq.open(lastleaf);
                            markedseq.add(seq);
                        } else if (c2c3 == 'X') {
                        }
                        break;
                    case 'b':
                        if (c2c3 == ' ') {
                            assert fvalidpath : "b";
                            if (fvalidpath) {
                                if (pathrect == null) path.closePath();
                                path.setWindingRule(GeneralPath.WIND_NON_ZERO);
                                ffill = fstroke = true;
                            }
                        } else if (c2c3 == '*') {
                            assert fvalidpath : "b*";
                            if (fvalidpath) {
                                if (pathrect == null) path.closePath();
                                path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
                                ffill = fstroke = true;
                            }
                        }
                        break;
                    case 'C':
                        if (c2c3 == 'S') {
                            sCS = pdfr.getColorSpace(ops[0], csres, patres);
                            assert sCS != null : "CS stroke " + pdfr.getObject(ops[0]) + " in " + csres;
                        }
                        break;
                    case 'c':
                        if (c2c3 == ' ') {
                            getDoubles(ops, d, 6);
                            ctm.transform(d, 0, d, 0, 3);
                            path.curveTo((float) d[0], (float) d[1], (float) d[2], (float) d[3], (float) (curx = d[4]), (float) (cury = d[5]));
                            pathlen += 100;
                        } else if (c2c3 == 'm') {
                            getDoubles(ops, d, 6);
                            tmpat.setTransform(d[0], d[1], d[2], d[3], d[4], d[5]);
                            if (!tmpat.isIdentity()) {
                                ctm.concatenate(tmpat);
                                if (tmpat.getType() != AffineTransform.TYPE_TRANSLATION) fnewfont = true;
                            }
                        } else if (c2c3 == 's') {
                            fCS = pdfr.getColorSpace(ops[0], csres, patres);
                            assert fCS != null : "cs fill " + pdfr.getObject(ops[0]) + " in " + csres;
                        }
                        break;
                    case 'D':
                        if (c2c3 == 'o') {
                            Leaf l = cmdDo((String) ops[0], xores, resources, ctm, newfcolor, clipp, d, ocrimgs);
                            if (l != null) {
                                lastleaf = l;
                                leafcnt++;
                            }
                        } else if (c2c3 == 'P') {
                            if (lastleaf.isLeaf()) new Mark((Leaf) lastleaf, lastleaf.size());
                        }
                        break;
                    case 'd':
                        if (c2c3 == ' ') {
                            Object[] oa = (Object[]) ops[0];
                            if (oa == OBJECT_NULL || oa.length == 0) gs.dasharray = null; else getFloats(oa, gs.dasharray = new float[oa.length], oa.length);
                            gs.dashphase = ((Number) ops[1]).floatValue();
                            fnewline = true;
                        } else if (c2c3 == '0') {
                            clipp.bbox.width = ((Number) ops[0]).intValue();
                        } else if (c2c3 == '1') {
                            clipp.bbox.width = ((Number) ops[0]).intValue();
                        }
                        break;
                    case 'E':
                        if (c2c3 == 'T') {
                            if (linep.size() == 0) {
                                if (linep != lastleaf) linep.remove(); else new FixedLeafAscii("", null, linep).getIbbox().setBounds((int) Math.round(Tm.getTranslateX()), (int) Math.round(Tm.getTranslateY()), 0, 0);
                            }
                            if (textp.size() == 0) textp.remove();
                        } else if (c2c3 == 'I') {
                            assert false;
                        } else if (c2c3 == ('M' << 8) + 'C') {
                            if (markedseq.size() > 0) {
                                Span seq = (Span) markedseq.remove(markedseq.size() - 1);
                                seq.close(lastleaf);
                            }
                        } else if (c2c3 == 'X') {
                        }
                        break;
                    case 'F':
                        assert c2c3 == ' ';
                    case 'f':
                        if (c2c3 == ' ') {
                            path.setWindingRule(GeneralPath.WIND_NON_ZERO);
                            ffill = true;
                        } else if (c2c3 == '*') {
                            path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
                            ffill = true;
                        }
                        break;
                    case 'G':
                        if (c2c3 == ' ') {
                            sCS = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                            float gray = ((Number) ops[0]).floatValue();
                            newcolor = (gray == 0f ? Color.BLACK : gray == 1f ? Color.WHITE : new Color(gray, gray, gray, 1f));
                        }
                        break;
                    case 'g':
                        if (c2c3 == ' ') {
                            fCS = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                            float gray = ((Number) ops[0]).floatValue();
                            newfcolor = (gray == 0f ? Color.BLACK : gray == 1f ? Color.WHITE : new Color(gray, gray, gray, 1f));
                        } else if (c2c3 == 's') {
                            Map gsdicts = (Map) pdfr.getObject(resources.get("ExtGState"));
                            Map gsdict = (Map) pdfr.getObject(gsdicts.get(ops[0]));
                            cmdgs(gsdict, fontres, ctm, d, gs);
                            if (gsdict.get("Font") != null) fnewfont = true;
                            fnewline = true;
                        }
                        break;
                    case 'h':
                        if (c2c3 == ' ') {
                            assert fvalidpath : "h";
                            if (fvalidpath) {
                                if (pathrect == null) path.closePath();
                            }
                        }
                        break;
                    case 'I':
                        if (c2c3 == 'D') {
                            assert false;
                        }
                        break;
                    case 'i':
                        if (c2c3 == ' ') {
                            gs.flatness = ((Number) ops[0]).intValue();
                        }
                        break;
                    case 'J':
                        if (c2c3 == ' ') {
                            gs.linecap = ((Number) ops[0]).intValue();
                            fnewline = true;
                        }
                        break;
                    case 'j':
                        if (c2c3 == ' ') {
                            gs.linejoin = ((Number) ops[0]).intValue();
                            fnewline = true;
                        }
                        break;
                    case 'K':
                        if (c2c3 == ' ') {
                            sCS = ColorSpaceCMYK.getInstance();
                            getFloats(ops, cscomp, 4);
                            float r = cscomp[0], g = cscomp[1], b = cscomp[2], k = cscomp[3];
                            newcolor = (r == 0f && g == 0f && b == 0f && k == 0f ? Color.WHITE : r + k >= 1f && g + k >= 1f && b + k >= 1f ? Color.BLACK : new Color(sCS, cscomp, 1f));
                        }
                        break;
                    case 'k':
                        if (c2c3 == ' ') {
                            fCS = ColorSpaceCMYK.getInstance();
                            getFloats(ops, cscomp, 4);
                            float r = cscomp[0], g = cscomp[1], b = cscomp[2], k = cscomp[3];
                            newfcolor = (r == 0f && g == 0f && b == 0f && k == 0f ? Color.WHITE : r + k >= 1f && g + k >= 1f && b + k >= 1f ? Color.BLACK : new Color(fCS, cscomp, 1f));
                        }
                        break;
                    case 'l':
                        if (c2c3 == ' ') {
                            getDoubles(ops, d, 2);
                            assert pathline == null : d[0] + " " + d[1];
                            ctm.transform(d, 0, d, 0, 1);
                            if (pathlen == 1 && (((peek = in.peek()) < '0' || peek > '9') && peek != '.' && peek != '-')) pathline = new Line2D.Double(curx, cury, d[0], d[1]); else path.lineTo((float) (curx = d[0]), (float) (cury = d[1]));
                            pathlen += 1000;
                        }
                        break;
                    case 'M':
                        if (c2c3 == ' ') {
                            gs.miterlimit = ((Number) ops[0]).intValue();
                            fnewline = true;
                        } else if (c2c3 == 'P') {
                            if (lastleaf.isLeaf()) new Mark((Leaf) lastleaf, lastleaf.size());
                        }
                        break;
                    case 'm':
                        if (c2c3 == ' ') {
                            assert pathrect == null && pathline == null;
                            getDoubles(ops, d, 2);
                            ctm.transform(d, 0, d, 0, 1);
                            path.moveTo((float) (curx = d[0]), (float) (cury = d[1]));
                            pathlen++;
                            fvalidpath = true;
                        }
                        break;
                    case 'n':
                        if (c2c3 == ' ') {
                            path.reset();
                            pathlen = 0;
                            pathrect = null;
                            pathline = null;
                            fvalidpath = false;
                        }
                        break;
                    case 'Q':
                        if (c2c3 == ' ') {
                            if (gsstack.size() > 0) gs = (GraphicsState) gsstack.remove(gsstack.size() - 1);
                            fnewfont = true;
                            newTr = gs.Tr;
                            Tc = gs.Tc;
                            Tw = gs.Tw;
                            Tz = gs.Tz;
                            TL = gs.TL;
                            Ts = gs.Ts;
                            fCS = gs.fCS;
                            sCS = gs.sCS;
                            newcolor = gs.strokecolor;
                            newfcolor = gs.fillcolor;
                            fnewline = true;
                            ctm = gs.ctm;
                            if (clipp != gs.clip && clipp.size() == 0) clipp.remove();
                            clipp = gs.clip;
                            fvalidpath = false;
                        }
                        break;
                    case 'q':
                        if (c2c3 == ' ') {
                            gs.Tr = newTr;
                            gs.Tc = Tc;
                            gs.Tw = Tw;
                            gs.Tz = Tz;
                            gs.TL = TL;
                            gs.Ts = Ts;
                            gs.fCS = fCS;
                            gs.sCS = sCS;
                            gs.strokecolor = newcolor;
                            gs.fillcolor = newfcolor;
                            gs.ctm = ctm;
                            gs.clip = clipp;
                            gsstack.add(new GraphicsState(gs));
                        }
                        break;
                    case 'R':
                        if (c2c3 == 'G') {
                            sCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                            getFloats(ops, cscomp, 3);
                            float r = cscomp[0], g = cscomp[1], b = cscomp[2];
                            newcolor = (r == 0f && g == 0f && b == 0f ? Color.BLACK : r == 1f && g == 1f && b == 1f ? Color.WHITE : new Color(r, g, b, 1f));
                        }
                        break;
                    case 'r':
                        if (c2c3 == 'e') {
                            assert pathrect == null && pathline == null;
                            getDoubles(ops, d, 4);
                            ctm.transform(d, 0, d, 0, 1);
                            ctm.deltaTransform(d, 2, d, 2, 1);
                            double x = curx = d[0], y = cury = d[1], w = d[2], h = d[3];
                            if (w < 0.0) {
                                x += w;
                                w = -w;
                            }
                            if (w < 1.0) w = 1.0;
                            if (h < 0.0) {
                                y += h;
                                h = -h;
                            }
                            if (h < 1.0) h = 1.0;
                            Rectangle r = new Rectangle((int) x, (int) (y), (int) Math.round(w), (int) Math.round(h));
                            if (!fvalidpath && (((peek = in.peek()) < '0' || peek > '9') && peek != '.' && peek != '-')) {
                                pathrect = r;
                                pathlen = 1;
                            } else {
                                path.append(r, false);
                                pathlen += 4;
                            }
                            fvalidpath = true;
                        } else if (c2c3 == 'g') {
                            fCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                            getFloats(ops, cscomp, 3);
                            float r = cscomp[0], g = cscomp[1], b = cscomp[2];
                            newfcolor = (r == 0f && g == 0f && b == 0f ? Color.BLACK : r == 1f && g == 1f && b == 1f ? Color.WHITE : new Color(r, g, b, 1f));
                        } else if (c2c3 == 'i') {
                            gs.renderingintent = (String) ops[0];
                        }
                        break;
                    case 'S':
                        if (c2c3 == ' ') {
                            fstroke = true;
                        } else if (c2c3 == 'C' || c2c3 == ('C' << 8) + 'N') {
                            if (opsi > 0 && ops[opsi - 1].getClass() == CLASS_NAME) {
                                assert c2c3 == ('C' << 8) + 'N';
                                sCS = pdfr.getColorSpace(ops[opsi - 1], csres, patres);
                                opsi--;
                            }
                            if (opsi > 0) {
                                getFloats(ops, cscomp, Math.min(opsi, 4));
                                float[] rgb = sCS.toRGB(cscomp);
                                newcolor = new Color(rgb[0], rgb[1], rgb[2], 1f);
                            }
                        }
                        break;
                    case 's':
                        if (c2c3 == ' ') {
                            assert fvalidpath : "s";
                            if (fvalidpath) {
                                if (pathrect == null) path.closePath();
                                fstroke = true;
                            }
                        } else if (c2c3 == 'c' || c2c3 == ('c' << 8) + 'n') {
                            if (opsi > 0 && ops[opsi - 1].getClass() == CLASS_NAME) {
                                assert c2c3 == ('c' << 8) + 'n';
                                fCS = pdfr.getColorSpace(ops[opsi - 1], csres, patres);
                                opsi--;
                            }
                            if (opsi > 0) {
                                getFloats(ops, cscomp, Math.min(opsi, 4));
                                float[] rgb = fCS.toRGB(cscomp);
                                newfcolor = new Color(rgb[0], rgb[1], rgb[2], 1f);
                            }
                        } else if (c2c3 == 'h') {
                            Map shdict = (Map) pdfr.getObject(shres.get(ops[0]));
                            ColorSpace cs = pdfr.getColorSpace(shdict.get("ColorSpace"), csres, patres);
                            Object[] oa = (Object[]) pdfr.getObject(shdict.get("Bbox"));
                            Rectangle bbox = (oa != null ? PDFReader.array2Rectangle(oa, ctm, false) : clipp.getCrop());
                            FixedLeafShade l = FixedLeafShade.getInstance(shdict, cs, bbox, clipp, pdfr);
                            lastleaf = l;
                            leafcnt++;
                            l.getBbox().setBounds(l.getIbbox());
                            l.setValid(true);
                        }
                        break;
                    case '"':
                        if (c2c3 == ' ') {
                            getDoubles(ops, d, 2);
                            Tw = d[0];
                            Tc = d[1];
                            ops[0] = ops[2];
                        }
                    case '\'':
                        if (c2c3 == ' ') {
                            Tlm.translate(0.0, -TL);
                            Tm.setTransform(Tlm);
                            c2c3 = 'j';
                        }
                    case 'T':
                        if (c2c3 == 'j' || c2c3 == 'J') {
                            if (lastleaf.isStruct()) lastleaf = linep;
                            float newsize = 0f;
                            if (fnewfont) {
                                srcpt.setLocation(gs.pointsize, 0.0);
                                Tm.deltaTransform(srcpt, transpt);
                                double zx = transpt.getX(), zy = transpt.getY();
                                newsize = (float) Math.abs(zy == 0.0 ? zx : zx == 0.0 ? zy : Math.sqrt(zx * zx + zy * zy));
                                if (fontdict == gs.fontdict && Math.abs(newsize - tf.getSize2D()) < 0.0001) {
                                    fnewfont = false;
                                }
                            }
                            if (fnewfont) {
                                if (fontspan != null) {
                                    fontspan.close(lastleaf);
                                    assert !fshowshape || fontspan.isSet() : "can't add font span " + getName() + "  " + fontspan.getStart().leaf + " .. " + lastleaf;
                                    spancnt++;
                                }
                                fontdict = gs.fontdict;
                                pointsize = gs.pointsize;
                                tf = pdfr.getFont(fontdict, pointsize, newsize, page, Tm, this);
                                maxr = tf.getMaxCharBounds();
                                fontspan = (SpanPDF) Behavior.getInstance((DEBUG ? tf.getFamily() + " " + tf.getSize2D() : tf.getFamily()), "multivalent.std.adaptor.pdf.SpanPDF", null, scratchLayer);
                                fontspan.font = tf.getFont();
                                fontspan.open(lastleaf);
                                spacew = tf.measureText(' ') / Tm.getScaleX();
                                concatthreshold = spacew * Tm.getScaleX() / 4.0;
                                fnewfont = false;
                            }
                            if (!tf.canRender()) break;
                            boolean fType3 = tf instanceof FontType3;
                            if (fType3) ((FontType3) tf).setPage(page);
                            if (newTr != Tr) {
                                if (Trspan != null) {
                                    Trspan.close(lastleaf);
                                    spancnt++;
                                }
                                Tr = newTr;
                                if (Tr == 0) {
                                    Trspan = null;
                                    vspancnt++;
                                } else {
                                    Trspan = (SpanPDF) Behavior.getInstance("Tr", "multivalent.std.adaptor.pdf.SpanPDF", null, scratchLayer);
                                    Trspan.Tr = Tr;
                                    Trspan.open(lastleaf);
                                }
                            }
                            double newbaseline = Tm.getTranslateY();
                            if (newbaseline != baseline && Math.abs(baseline - newbaseline) > 0.1) {
                                if (linep.size() > 0) {
                                    linep = new FixedIHBox("line", null, textp);
                                    fconcat = false;
                                }
                                baseline = newbaseline;
                            }
                            Object[] oa;
                            if (c2c3 == 'j') {
                                oa = Tja;
                                oa[0] = ops[0];
                            } else oa = (Object[]) ops[0];
                            double sTc = Tc * Tm.getScaleX(), kern1 = (sTc >= 0.0 ? Math.floor(sTc) : Math.ceil(sTc));
                            boolean fspace1 = false;
                            boolean frot = Tm.getShearX() != 0.0;
                            FontRenderContext frc = (frot ? new FontRenderContext(Tm, true, true) : null);
                            for (int i = 0, imax = oa.length; i < imax; i++) {
                                Object o = oa[i];
                                if (o instanceof Number) {
                                    double kern = ((Number) o).doubleValue() / 1000.0 * pointsize;
                                    Tm.translate(-kern, 0.0);
                                } else {
                                    assert o.getClass() == CLASS_STRING;
                                    StringBuffer txt8 = (StringBuffer) o;
                                    String txt = tf.translate(txt8);
                                    for (int s = 0, smax = txt.length(), e; s < smax; s = e) {
                                        e = txt.indexOf(' ', s);
                                        if (e == -1) e = smax; else if (e == 0 && i == 0 && s == 0 && ALL_WS.reset(txt).matches()) {
                                            e = 1;
                                            fspace1 = true;
                                        }
                                        if (s < e) {
                                            String sub = txt.substring(s, e);
                                            double kern = kern1 * sub.length();
                                            double fw = tf.measureText(txt8, s, e) + kern;
                                            int bw = (int) Math.ceil(fw), bh = (int) Math.ceil(maxr.getHeight()), ascent = (int) Math.ceil(-maxr.getY());
                                            double dx = Tm.getTranslateX() - lastX;
                                            if (frot && false) {
                                                GlyphVector gv = tf.getFont().createGlyphVector(frc, sub);
                                                Shape txtshp = gv.getOutline();
                                                Rectangle2D r2d = gv.getVisualBounds();
                                                Rectangle r = gv.getPixelBounds(frc, (float) Tm.getTranslateX(), (float) Tm.getTranslateY());
                                                FixedLeafShape lshp = new FixedLeafShape("glyphs", null, linep, txtshp, true, true);
                                                lastleaf = lshp;
                                                leafcnt++;
                                                lshp.getIbbox().setBounds(r);
                                                lshp.getBbox().setBounds(r);
                                            } else if (fType3) {
                                                FontType3 tf3 = (FontType3) tf;
                                                FixedLeafType3 l = new FixedLeafType3(sub, null, linep, tf3);
                                                lastleaf = l;
                                                leafcnt++;
                                                int w = 0, minascent = 0, maxdescent = 0;
                                                for (int j = 0, jmax = txt.length(); j < jmax; j++) {
                                                    Node glyph = tf3.getGlyph(txt.charAt(j));
                                                    Rectangle bbox = glyph.bbox;
                                                    w += bbox.width;
                                                    minascent = Math.min(minascent, bbox.y);
                                                    maxdescent = Math.max(maxdescent, bbox.height + bbox.y);
                                                }
                                                l.getIbbox().setBounds((int) Math.round(Tm.getTranslateX()), (int) Math.round(Tm.getTranslateY() + Ts * Tm.getScaleY() + minascent), w, maxdescent - minascent);
                                                l.getBbox().setBounds(l.getIbbox());
                                                l.baseline = -minascent;
                                            } else if (fconcat && Math.abs(dx) < concatthreshold && Ts == 0.0) {
                                                FixedLeafAsciiKern l = (FixedLeafAsciiKern) linep.getLastChild();
                                                assert l == lastleaf;
                                                l.appendText(sub, (byte) kern1);
                                                l.setKernAt(l.size() - 1, (byte) (l.getKernAt(l.size() - 1) + dx));
                                                Rectangle ibbox = l.getIbbox();
                                                totalW += fw;
                                                ibbox.width = (int) Math.ceil(totalW);
                                                ibbox.height = Math.max(ibbox.height, bh);
                                                l.bbox.setSize(ibbox.width, ibbox.height);
                                                concatcnt++;
                                            } else {
                                                FixedLeafAsciiKern l = new FixedLeafAsciiKern(sub, null, linep, (byte) kern1);
                                                lastleaf = l;
                                                leafcnt++;
                                                l.getIbbox().setBounds((int) Math.round(Tm.getTranslateX()), (int) Math.round(Tm.getTranslateY() + maxr.getY() + Ts * Tm.getScaleY()), bw, bh);
                                                l.bbox.setBounds(l.getIbbox());
                                                l.baseline = ascent;
                                                totalW = fw;
                                            }
                                            lastleaf.setValid(true);
                                            tmpat.setToTranslation(fw + (sTc * sub.length() - kern), 0.0);
                                            Tm.preConcatenate(tmpat);
                                            lastX = Tm.getTranslateX();
                                            fconcat = !frot && !fType3 && sub.charAt(sub.length() - 1) < 128;
                                        }
                                        if (fspace1) {
                                            Tm.translate(Tw, 0.0);
                                            fspace1 = false;
                                            fconcat = false;
                                        }
                                        if (e < smax && txt.charAt(e) == ' ') {
                                            int spacecnt = 0;
                                            do {
                                                spacecnt++;
                                                e++;
                                            } while (e < smax && txt.charAt(e) == ' ');
                                            Tm.translate(spacecnt * (spacew + Tw + Tc), 0.0);
                                            fconcat = false;
                                        }
                                    }
                                }
                            }
                        } else if (c2c3 == 'd') {
                            getDoubles(ops, d, 2);
                            Tlm.translate(d[0], d[1]);
                            Tm.setTransform(Tlm);
                        } else if (c2c3 == 'D') {
                            getDoubles(ops, d, 2);
                            TL = -d[1];
                            Tlm.translate(d[0], d[1]);
                            Tm.setTransform(Tlm);
                        } else if (c2c3 == 'm') {
                            double m00 = Tm.getScaleX(), m01 = Tm.getShearX(), m10 = Tm.getShearY(), m11 = Tm.getScaleY();
                            getDoubles(ops, d, 6);
                            tmpat.setTransform(d[0], d[1], d[2], d[3], d[4], d[5]);
                            Tm.setTransform(ctm);
                            Tm.concatenate(tmpat);
                            Tlm.setTransform(Tm);
                            if (m00 != Tm.getScaleX() || m01 != Tm.getShearX() || m10 != Tm.getShearY() || m11 != Tm.getScaleY()) fnewfont = true;
                        } else if (c2c3 == '*') {
                            Tlm.translate(0.0, -TL);
                            Tm.setTransform(Tlm);
                        } else if (c2c3 == 'c') {
                            getDoubles(ops, d, 1);
                            Tc = d[0];
                        } else if (c2c3 == 'w') {
                            getDoubles(ops, d, 1);
                            Tw = d[0];
                        } else if (c2c3 == 'z') {
                            getDoubles(ops, d, 1);
                            Tz = d[0];
                        } else if (c2c3 == 'L') {
                            getDoubles(ops, d, 1);
                            TL = d[0];
                        } else if (c2c3 == 'f') {
                            assert fontres != null : page;
                            gs.fontdict = (Map) pdfr.getObject(fontres.get(ops[0]));
                            assert gs.fontdict != null : ops[0] + " not in " + fontres;
                            gs.pointsize = ((Number) ops[1]).doubleValue();
                            fnewfont = true;
                        } else if (c2c3 == 'r') {
                            newTr = ((Number) ops[0]).intValue();
                        } else if (c2c3 == 's') {
                            getDoubles(ops, d, 1);
                            d[1] = 0.0;
                            Tm.deltaTransform(d, 0, d, 0, 1);
                            Ts = Math.abs(d[0]);
                        }
                        break;
                    case 'v':
                        if (c2c3 == ' ') {
                            getDoubles(ops, d, 4);
                            ctm.transform(d, 0, d, 0, 2);
                            path.curveTo((float) curx, (float) cury, (float) d[0], (float) d[1], (float) (curx = d[2]), (float) (cury = d[3]));
                            pathlen += 100;
                        }
                        break;
                    case 'W':
                        if (c2c3 == '*' || c2c3 == ' ') {
                            if (fvalidpath) {
                                Rectangle bounds;
                                if (pathrect != null) {
                                    bounds = new Rectangle(pathrect.x, pathrect.y, pathrect.width + 1, pathrect.height + 1);
                                    Shape oldshape = clipp.getClip();
                                    if (!(oldshape instanceof Rectangle) || !pathrect.contains((Rectangle) oldshape)) clipp = new FixedIClip(c2c3 == '*' ? "W*" : "W", null, clipp, new Rectangle(0, 0, bounds.width, bounds.height), bounds);
                                } else if (pathline != null) {
                                    bounds = null;
                                } else {
                                    GeneralPath wpath;
                                    if (in.peek() == 'n') {
                                        wpath = path;
                                        path = new GeneralPath();
                                    } else {
                                        wpath = (GeneralPath) path.clone();
                                    }
                                    wpath.closePath();
                                    wpath.setWindingRule(c2c3 == '*' ? GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO);
                                    bounds = wpath.getBounds();
                                    tmpat.setToTranslation(-bounds.x, -bounds.y);
                                    wpath.transform(tmpat);
                                    clipp = new FixedIClip(c2c3 == '*' ? "W*" : "W", null, clipp, wpath, bounds);
                                }
                            }
                        }
                        break;
                    case 'w':
                        if (c2c3 == ' ') {
                            d[0] = ((Number) ops[0]).doubleValue();
                            d[1] = 0.0;
                            ctm.deltaTransform(d, 0, d, 0, 1);
                            gs.linewidth = (float) Math.abs(d[1] == 0.0 ? d[0] : d[0] == 0.0 ? d[1] : Math.sqrt(d[0] * d[0] + d[1] * d[1]));
                            fnewline = true;
                        }
                        break;
                    case 'y':
                        if (c2c3 == ' ') {
                            getDoubles(ops, d, 4);
                            ctm.transform(d, 0, d, 0, 2);
                            path.curveTo((float) d[0], (float) d[1], (float) d[2], (float) d[3], (float) (curx = d[2]), (float) (cury = d[3]));
                            pathlen += 100;
                        }
                        break;
                    case '%':
                        while ((c = in.read()) != -1 && c != '\r' && c != '\n') {
                        }
                        PDFReader.eatSpace(in);
                        break;
                    default:
                        assert false : (char) c + " / " + c;
                        throw new ParseException("invalid command: " + (char) c + "...");
                }
                opsi = 0;
                if (fstroke || ffill) {
                    if (fnewline && ((gs.linewidth = (gs.linewidth < 1f ? 1f : gs.linewidth)) != linewidth || gs.linecap != linecap || gs.linejoin != linejoin || gs.miterlimit != miterlimit || gs.dashphase != dashphase || !Arrays.equals(gs.dasharray, dasharray))) {
                        fnewline = false;
                        if (strokespan != null) {
                            strokespan.close(lastleaf);
                            spancnt++;
                        }
                        linewidth = gs.linewidth;
                        linecap = gs.linecap;
                        linejoin = gs.linejoin;
                        miterlimit = gs.miterlimit;
                        dasharray = gs.dasharray;
                        dashphase = gs.dashphase;
                        if (dasharray != null) {
                            int dai = 0;
                            for (int i = 0, imax = dasharray.length; i < imax; i++) if (dasharray[i] > 0f) dasharray[dai++] = dasharray[i];
                            if (dai < dasharray.length) {
                                float[] newda = new float[dai];
                                System.arraycopy(dasharray, 0, newda, 0, dai);
                                dasharray = newda;
                            }
                        }
                        BasicStroke bs = new BasicStroke(linewidth, linecap, linejoin, miterlimit, dasharray, dashphase);
                        if (Context.DEFAULT_STROKE.equals(bs)) {
                            strokespan = null;
                            vspancnt++;
                        } else {
                            strokespan = (StrokeSpan) Behavior.getInstance("width" + linejoin, "multivalent.std.span.StrokeSpan", null, scratchLayer);
                            strokespan.setStroke(bs);
                            strokespan.open(lastleaf);
                        }
                    }
                    if (!fshowshape) {
                        pathrect = null;
                        pathline = null;
                        path.reset();
                    } else if (fvalidpath) {
                        Shape shape;
                        Rectangle bounds;
                        String name;
                        if (pathrect != null) {
                            assert pathlen == 1 : pathlen;
                            bounds = pathrect;
                            shape = new Rectangle(0, 0, pathrect.width, pathrect.height);
                            name = "rect";
                            pathrect = null;
                            assert pathline == null : pathline;
                        } else if (pathline != null) {
                            double x1 = pathline.getX1(), y1 = pathline.getY1(), x2 = pathline.getX2(), y2 = pathline.getY2(), xmin, ymin, w2d, h2d;
                            if (x1 <= x2) {
                                xmin = x1;
                                w2d = x2 - x1;
                            } else {
                                xmin = x2;
                                w2d = x1 - x2;
                            }
                            if (y1 <= y2) {
                                ymin = y1;
                                h2d = y2 - y1;
                            } else {
                                ymin = y2;
                                h2d = y1 - y2;
                            }
                            bounds = new Rectangle((int) Math.round(xmin), (int) Math.round(ymin), (w2d > 1.0 ? (int) Math.ceil(w2d) : 1), (h2d > 1.0 ? (int) Math.ceil(h2d) : 1));
                            shape = new Line2D.Double(x1 - xmin, y1 - ymin, x2 - xmin, y2 - ymin);
                            name = (DEBUG ? "line" + (pathcnt) : "line");
                            pathline = null;
                            path.reset();
                        } else {
                            Rectangle2D r2d = path.getBounds2D();
                            double w2d = r2d.getWidth(), h2d = r2d.getHeight();
                            bounds = new Rectangle((int) Math.round(r2d.getX()), (int) Math.round(r2d.getY()), (w2d > 1.0 ? (int) Math.ceil(w2d) : 1), (h2d > 1.0 ? (int) Math.ceil(h2d) : 1));
                            tmpat.setToTranslation(-r2d.getX(), -r2d.getY());
                            path.transform(tmpat);
                            shape = path;
                            name = (DEBUG ? "path" + pathcnt : "path");
                            path = new GeneralPath();
                        }
                        if (shape != null) {
                            FixedLeafShape l = new FixedLeafShape(name, null, clipp, shape, fstroke, ffill);
                            lastleaf = l;
                            leafcnt++;
                            l.getIbbox().setBounds(bounds);
                            l.getBbox().setBounds(bounds);
                            l.setValid(true);
                            pathlens[pathcnt] = pathlen;
                            pathlen = 0;
                            if (pathcnt + 1 < pathlens.length) pathcnt++;
                        }
                    } else {
                        if (lastleaf instanceof FixedLeafShape) {
                            FixedLeafShape l = (FixedLeafShape) lastleaf;
                            if (fstroke) l.setStroke(true); else l.setFill(true);
                        }
                    }
                    fvalidpath = false;
                    fstroke = ffill = false;
                }
                if (color != newcolor && !color.equals(newcolor)) {
                    if (sspan != null) {
                        if (sspan.close(lastleaf)) spancnt++; else sspan.destroy();
                    }
                    color = newcolor;
                    assert color != null : color;
                    if (Color.BLACK.equals(color)) {
                        sspan = null;
                        vspancnt++;
                    } else {
                        sspan = (SpanPDF) Behavior.getInstance((DEBUG ? "stroke " + Integer.toHexString(color.getRGB()) : "stroke"), "multivalent.std.adaptor.pdf.SpanPDF", null, scratchLayer);
                        sspan.stroke = color;
                        sspan.open(lastleaf);
                    }
                }
                if (fcolor != newfcolor && !fcolor.equals(newfcolor)) {
                    if (fillspan != null) {
                        if (fillspan.close(lastleaf)) spancnt++; else fillspan.destroy();
                    }
                    fcolor = newfcolor;
                    assert fcolor != null : fcolor;
                    if (Color.BLACK.equals(fcolor)) {
                        fillspan = null;
                        vspancnt++;
                    } else {
                        fillspan = (SpanPDF) Behavior.getInstance((DEBUG ? "fill " + Integer.toHexString(fcolor.getRGB()) : "fill"), "multivalent.std.adaptor.pdf.SpanPDF", null, scratchLayer);
                        fillspan.fill = fcolor;
                        fillspan.open(lastleaf);
                    }
                }
            }
        }
        spancnt += Span.closeAll(clipp);
        if (Multivalent.MONITOR && ocrimgs != null) {
            System.out.println(cmdcnt + " cmds, " + leafcnt + " leaves, " + spancnt + " spans (" + vspancnt + " saved), " + concatcnt + " concats, " + pathcnt + " paths, time=" + (System.currentTimeMillis() - start));
        }
        return cmdcnt;
    }

    private Leaf cmdDo(String xname, Map xores, Map resources, AffineTransform ctm, Color newfcolor, FixedIClip clipp, double[] d, List ocrimgs) throws IOException, ParseException {
        Leaf l = null;
        PDFReader pdfr = pdfr_;
        IRef iref = (IRef) xores.get(xname);
        Map xobj = (Map) pdfr.getObject(iref);
        assert xobj != null : xname + " in " + xores + " -> " + iref;
        String subtype = (String) pdfr.getObject(xobj.get("Subtype"));
        if ("Image".equals(subtype)) {
            if ((getHints() & HINT_NO_IMAGE) == 0) {
                BufferedImage img = pdfr.getImage(iref, ctm, newfcolor);
                if (img != null) {
                    l = appendImage(xname, clipp, img, ctm);
                    String imgtype = Images.getFilter(xobj, pdfr);
                    if (("CCITTFaxDecode".equals(imgtype) || "JBIG2Decode".equals(imgtype)) && ocrimgs != null) ocrimgs.add(l);
                } else sampledata("null image " + iref + ": " + xname + " / " + xobj.get("Name") + ", subtype=" + subtype);
            }
        } else if ("Form".equals(subtype)) {
            AffineTransform formAT = new AffineTransform(ctm);
            Object[] oa = (Object[]) pdfr.getObject(xobj.get("Matrix"));
            if (oa != null) {
                assert oa.length == 6;
                getDoubles(oa, d, 6);
                formAT.concatenate(new AffineTransform(d[0], d[1], d[2], d[3], d[4], d[5]));
            }
            FixedIClip formClip = clipp;
            Object o = pdfr.getObject(xobj.get("BBox"));
            if (o != null) {
                assert o.getClass() == CLASS_ARRAY;
                Rectangle r = PDFReader.array2Rectangle((Object[]) o, formAT, false);
                formClip = new FixedIClip(xname, null, clipp, new Rectangle(0, 0, r.width, r.height), r);
            }
            if (xobj.get("Resources") == null) xobj.put("Resources", resources);
            CompositeInputStream formin = pdfr.getInputStream(iref);
            buildStream(xobj, formClip, formAT, formin, ocrimgs);
            formin.close();
            l = formClip.getLastLeaf();
            if (l == null && formClip != clipp) formClip.remove();
        } else if ("PS".equals(subtype)) {
        } else {
            assert false : subtype;
        }
        return l;
    }

    private void cmdgs(Map gsdict, Map fontres, AffineTransform ctm, double[] d, GraphicsState gs) throws IOException {
        PDFReader pdfr = pdfr_;
        Object o = pdfr.getObject(gsdict.get("Type"));
        assert o == null || "ExtGState".equals(o);
        if ((o = pdfr.getObject(gsdict.get("Font"))) != null) {
            Object[] oa = (Object[]) o;
            gs.fontdict = (Map) pdfr.getObject(fontres.get(oa[0]));
            assert gs.fontdict != null : oa[0] + " not in " + fontres;
            gs.pointsize = ((Number) oa[1]).doubleValue();
        }
        if ((o = pdfr.getObject(gsdict.get("LW"))) != null) {
            d[0] = ((Number) o).doubleValue();
            d[1] = 0.0;
            ctm.deltaTransform(d, 0, d, 0, 1);
            gs.linewidth = (float) Math.abs(d[1] == 0.0 ? d[0] : d[0] == 0.0 ? d[1] : Math.sqrt(d[0] * d[0] + d[1] * d[1]));
        }
        if ((o = pdfr.getObject(gsdict.get("LC"))) != null) gs.linecap = ((Number) o).intValue();
        if ((o = pdfr.getObject(gsdict.get("LJ"))) != null) gs.linejoin = ((Number) o).intValue();
        if ((o = pdfr.getObject(gsdict.get("ML"))) != null) gs.miterlimit = ((Number) o).floatValue();
        if ((o = pdfr.getObject(gsdict.get("D"))) != null) {
            Object[] oa0 = (Object[]) o, oa = (Object[]) oa0;
            if (oa == OBJECT_NULL || oa.length == 0) gs.dasharray = null; else getFloats(oa, gs.dasharray = new float[oa.length], oa.length);
            gs.dashphase = ((Number) oa0[1]).floatValue();
        }
        if ((o = pdfr.getObject(gsdict.get("RI"))) != null) gs.renderingintent = (String) o;
        if ((o = pdfr.getObject(gsdict.get("TR2"))) != null || (o = pdfr.getObject(gsdict.get("TR"))) != null) {
        }
        if ((o = pdfr.getObject(gsdict.get("HT"))) != null) {
        }
        if ((o = pdfr.getObject(gsdict.get("FL"))) != null) gs.flatness = ((Number) o).intValue();
        if ((o = pdfr.getObject(gsdict.get("SM"))) != null) gs.smoothness = ((Number) o).doubleValue();
        if ((o = pdfr.getObject(gsdict.get("SA"))) != null) {
        }
        if ((o = pdfr.getObject(gsdict.get("BM"))) != null) {
        }
        if ((o = pdfr.getObject(gsdict.get("SMask"))) != null) {
        }
        if ((o = pdfr.getObject(gsdict.get("CA"))) != null) {
            gs.alphastroke = ((Number) o).floatValue();
            if (gs.alphastroke != 1.0) PDF.sampledata("transparency (CA - stroking alpha)");
        }
        if ((o = pdfr.getObject(gsdict.get("ca"))) != null) {
            gs.alphanonstroke = ((Number) o).floatValue();
            if (gs.alphanonstroke != 1.0) PDF.sampledata("transparency (ca - nonstroking alpha)");
        }
        if ((o = pdfr.getObject(gsdict.get("AIS"))) != null) {
        }
        if ((o = pdfr.getObject(gsdict.get("TK"))) != null) {
        }
    }

    private Leaf appendImage(String name, INode parent, BufferedImage img, AffineTransform ctm) {
        FixedLeafImage l = new FixedLeafImage(name, null, parent, img);
        double majorx = Math.abs(ctm.getScaleX()) > Math.abs(ctm.getShearX()) ? ctm.getScaleX() : ctm.getShearX(), majory = Math.abs(ctm.getScaleY()) > Math.abs(ctm.getShearY()) ? ctm.getScaleY() : ctm.getShearY();
        double left = ctm.getTranslateX() - (majorx < 0.0 ? img.getWidth() : 0.0), top = ctm.getTranslateY() - (majory < 0.0 ? img.getHeight() : 0.0);
        l.getIbbox().setBounds((int) left, (int) top, img.getWidth(), img.getHeight());
        return l;
    }

    /** Check that all leaves are valid and bbox.equals(ibbox). */
    private boolean checkTree(String id, INode pageroot) {
        for (Leaf l = pageroot.getFirstLeaf(), endl = (l != null ? pageroot.getLastLeaf().getNextLeaf() : null); l != endl; l = l.getNextLeaf()) {
            assert l.isValid() || !(l instanceof FixedLeafAsciiKern) : id + ": " + l + " " + l.getClass().getName() + " " + l.getBbox();
        }
        return true;
    }

    private void createAnnots(Map pagedict, INode root) throws IOException {
        Object[] annots = (Object[]) pdfr_.getObject(pagedict.get("Annots"));
        Browser br = getBrowser();
        if (annots != null && br != null) for (int i = 0, imax = annots.length; i < imax; i++) {
            br.event(new SemanticEvent(br, Anno.MSG_CREATE, pdfr_.getObject(annots[i]), this, root));
        }
    }

    /**
	Parses individual page indicated in {@link Document#ATTR_PAGE} of <var>parent</var>'s containing {@link Document}
	and returns formatted document tree rooted at <var>parent</var> as <a href='#doctree'>described</a> above.
	@return root of PDF subtree under <var>parent</var>
  */
    public Object parse(INode parent) throws IOException, ParseException {
        Document doc = parent.getDocument();
        if (pdfr_ == null) init(doc);
        if (fail_ != null) {
            new LeafAscii("Error opening PDF: " + fail_, null, parent);
            return parent;
        }
        StyleSheet ss = doc.getStyleSheet();
        CLGeneral cl = (CLGeneral) ss.get("pdf");
        if (cl == null) {
            cl = new CLGeneral();
            ss.put("pdf", cl);
        }
        cl.setStroke(Color.BLACK);
        cl.setBackground(Color.WHITE);
        cl.setForeground(Color.BLACK);
        PDFReader pdfr = pdfr_;
        pdfr.refresh();
        if (!isAuthorized()) {
            return new LeafAscii("can handle encrypted PDFs only with null password for now", null, doc);
        }
        if (doc.getAttr(Document.ATTR_PAGECOUNT) == null) {
            doc.putAttr(Document.ATTR_PAGECOUNT, Integer.toString(pdfr.getPageCnt()));
            Map info = pdfr.getInfo();
            if (info != null) {
                Object o = null;
                for (int i = 0, imax = METADATA.length; i < imax; i++) if ((o = pdfr.getObject(info.get(METADATA[i]))) != null) doc.putAttr(METADATA[i], o.toString());
            }
        }
        if (doc.getAttr(Document.ATTR_PAGE) == null) return new LeafAscii("Loading...", null, parent);
        INode pdf = new INode("pdf", null, parent);
        FixedI mediabox = new FixedI("MediaBox", null, pdf);
        Browser br = getBrowser();
        Object z = br != null ? br.callSemanticEvent("getZoom", null) : null;
        double zoom = (z instanceof Number ? ((Number) z).doubleValue() : 100.0) / 100.0;
        buildPage(Integers.parseInt(doc.getAttr(Document.ATTR_PAGE, "1"), 1), mediabox, AffineTransform.getScaleInstance(zoom, zoom));
        pdf.addObserver(this);
        pdfr.close();
        return pdf;
    }

    /**
	If URI ref is to named destination, set intial page to that.

	<p>(<code>...#page=nnn</code> handled by {@link multivalent.std.ui.Multipage}.
	The Acrobat plug-in supports a highlight file referred to like so <code>http://www.adobe.com/a.pdf#xml=http://www.adobe.com/a.txt</code>;
	but that's awkward and nobody uses it, so it's not supported.)
  */
    private void init(Document doc) {
        String ref = docURI.getFragment();
        try {
            pdfr_ = new PDFReader(getFile());
        } catch (Exception fail) {
            fail.printStackTrace();
            System.out.println(fail);
            fail_ = fail.getLocalizedMessage();
            return;
        }
        if (ref != null && doc.getAttr(Document.ATTR_PAGE) == null) try {
            PDFReader pdfr = pdfr_;
            Object dest = Action.resolveNamedDest(new StringBuffer(ref), pdfr);
            if (dest == null) Action.resolveNamedDest(ref, pdfr);
            if (dest != null) {
                if (dest.getClass() == CLASS_DICTIONARY) dest = pdfr_.getObject(((Map) dest).get("D"));
                if (dest.getClass() == CLASS_ARRAY) {
                    Object page = pdfr_.getObject(((Object[]) dest)[0]);
                    if (page.getClass() == CLASS_DICTIONARY) doc.putAttr(Document.ATTR_PAGE, String.valueOf(pdfr.page2num((Map) page)));
                }
            }
        } catch (Exception ignore) {
        }
        GoFast = Booleans.parseBoolean(getPreference("pdfGoFast", "true"), true);
    }

    /** Enlarge content root to MediaBox. */
    public boolean formatAfter(Node node) {
        node.bbox.setBounds(0, 0, Math.max(node.bbox.x + node.bbox.width, cropbox_.x + cropbox_.width), Math.max(node.bbox.y + node.bbox.height, cropbox_.y + cropbox_.height));
        return super.formatAfter(node);
    }

    /** "Dump PDF to temp dir" in Debug menu. */
    public boolean semanticEventBefore(SemanticEvent se, String msg) {
        if (super.semanticEventBefore(se, msg)) return true; else if (multivalent.gui.VMenu.MSG_CREATE_VIEW == msg) {
            VCheckbox cb = (VCheckbox) createUI("checkbox", "Accelerate PDF (less accurate)", "event " + MSG_GO_FAST, (INode) se.getOut(), VMenu.CATEGORY_MEDIUM, false);
            cb.setState(GoFast);
        } else if (multivalent.devel.Debug.MSG_CREATE_DEBUG == msg) {
            VCheckbox cb = (VCheckbox) createUI("checkbox", "Dump PDF to temp dir", "event " + MSG_DUMP, (INode) se.getOut(), VMenu.CATEGORY_MEDIUM, false);
            cb.setState(Dump_);
        }
        return false;
    }

    /** Implements {@link #MSG_DUMP}, {@link #MSG_USER_PASSWORD}, {@link #MSG_OWNER_PASSWORD}. */
    public boolean semanticEventAfter(SemanticEvent se, String msg) {
        Object arg = se.getArg();
        if (MSG_DUMP == msg) {
            Dump_ = Booleans.parseBoolean(arg, !Dump_);
        } else if (MSG_USER_PASSWORD == msg) {
        } else if (MSG_OWNER_PASSWORD == msg) {
        } else if (MSG_GO_FAST == msg) GoFast = !GoFast; else if (Document.MSG_CLOSE == msg && pdfr_ != null) {
            try {
                pdfr_.close();
            } catch (IOException ignore) {
            }
            pdfr_ = null;
        }
        return super.semanticEventAfter(se, msg);
    }

    /** If document is encrypted with non-null password, throw up dialog requesting user to enter it. */
    void requestPassword() {
    }

    static String lastmsg = null;

    static void sampledata(String msg) {
        if (msg != lastmsg) {
            System.err.println("SAMPLE DATA: " + msg);
            lastmsg = msg;
        }
    }

    static void unsupported(String msg) {
        if (msg != lastmsg) {
            System.err.println("UNSUPPORTED: " + msg);
            lastmsg = msg;
        }
    }
}
