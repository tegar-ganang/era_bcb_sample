import java.io.*;
import java.util.*;
import java.text.*;
import java.util.zip.*;

class OOFileHandler extends FileHandler {

    public OOFileHandler() {
        super("OpenOffice", "sxw");
        m_tagList = new LinkedList();
        m_preNT = new LinkedList();
        m_fdList = new LinkedList();
        m_postNT = new LinkedList();
    }

    public String formatString(String text) {
        char[] car = new char[text.length()];
        text.getChars(0, text.length(), car, 0);
        char c;
        int num = 0;
        String s = null;
        char shortcut = 0;
        int state = 0;
        boolean close = false;
        OOTag tag = null;
        LBuffer tagBuf = new LBuffer(8);
        LBuffer outBuf = new LBuffer(text.length() * 2);
        for (int i = 0; i < car.length; i++) {
            c = car[i];
            if (c == '<') {
                if (state == 1) {
                    outBuf.append("&lt;");
                    tagBuf.reset();
                    state = 0;
                } else if (state > 1) {
                    state = -1;
                } else {
                    tagBuf.append(c);
                    state = 1;
                }
            } else if ((state == 1) && (c == '/')) {
                close = true;
            } else if ((state == 1) && (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')))) {
                state++;
                shortcut = c;
                if (shortcut < 'a') shortcut += ('a' - 'A');
                tagBuf.append(c);
            } else if (((state == 2) || (state == 3)) && ((c >= '0') && (c <= '9'))) {
                state++;
                num = num * 10 + (c - '0');
                tagBuf.append(c);
            } else if (((state == 3) || (state == 4)) && (c == '>')) {
                tag = null;
                if ((num >= 0) && (num < m_tagList.size())) {
                    tag = (OOTag) m_tagList.get(num);
                    if (tag.shortcut() != shortcut) tag = null;
                }
                if (tag == null) {
                    state = -1;
                } else if (close == false) {
                    outBuf.append('<');
                    outBuf.append(tag.verbatum());
                    outBuf.append('>');
                } else {
                    outBuf.append("</");
                    outBuf.append(tag.name());
                    outBuf.append('>');
                }
                state = 0;
                num = 0;
                tagBuf.reset();
                close = false;
            } else if (state >= 0) {
                state = -1;
            } else {
                s = OOParser.convert(c);
                if (s == null) outBuf.append(c); else {
                    outBuf.append('&');
                    outBuf.append(s);
                    outBuf.append(';');
                }
            }
            if (state < 0) {
                tagBuf.append(c);
                outBuf.append(OOParser.convertAll(tagBuf));
                tagBuf.reset();
                state = 0;
            }
        }
        return outBuf.string();
    }

    public void reset() {
        super.reset();
        m_tagList.clear();
        m_preNT.clear();
        m_postNT.clear();
        m_fdList.clear();
        m_ec = 0;
        m_ws = false;
        m_hasText = false;
    }

    public BufferedReader createInputStream(String filename) throws IOException {
        File ifp = new File(filename);
        ZipInputStream zis = new ZipInputStream(new FileInputStream(ifp));
        InputStreamReader isr = new InputStreamReader(zis, "UTF8");
        BufferedReader br = new BufferedReader(isr);
        ZipEntry zit = null;
        while ((zit = zis.getNextEntry()) != null) {
            if (zit.getName().equals("content.xml")) break;
        }
        if (zit == null) return null; else return br;
    }

    public BufferedWriter createOutputStream(String inFile, String outFile) throws IOException {
        int k_blockSize = 1024;
        int byteCount;
        char[] buf = new char[k_blockSize];
        File ofp = new File(outFile);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(ofp));
        zos.setMethod(ZipOutputStream.DEFLATED);
        OutputStreamWriter osw = new OutputStreamWriter(zos, "ISO-8859-1");
        BufferedWriter bw = new BufferedWriter(osw);
        ZipEntry zot = null;
        File ifp = new File(inFile);
        ZipInputStream zis = new ZipInputStream(new FileInputStream(ifp));
        InputStreamReader isr = new InputStreamReader(zis, "ISO-8859-1");
        BufferedReader br = new BufferedReader(isr);
        ZipEntry zit = null;
        while ((zit = zis.getNextEntry()) != null) {
            if (zit.getName().equals("content.xml")) {
                continue;
            }
            zot = new ZipEntry(zit.getName());
            zos.putNextEntry(zot);
            while ((byteCount = br.read(buf, 0, k_blockSize)) >= 0) bw.write(buf, 0, byteCount);
            bw.flush();
            zos.closeEntry();
        }
        zos.putNextEntry(new ZipEntry("content.xml"));
        bw.flush();
        osw = new OutputStreamWriter(zos, "UTF8");
        bw = new BufferedWriter(osw);
        return bw;
    }

    public void doLoad() throws IOException {
        char c;
        int i;
        int ctr = 0;
        FormatData fd = null;
        LBuffer nt = new LBuffer(256);
        LBuffer t = new LBuffer(256);
        LBuffer tmp = new LBuffer(256);
        LBuffer esc = new LBuffer(16);
        try {
            while ((i = getNextChar()) >= 0) {
                ctr++;
                c = (char) i;
                if (c == '<') {
                    if (fd != null) {
                        fd = null;
                    }
                    handleTag();
                } else if ((c == 10) || (c == 13) || (c == ' ')) {
                    if ((m_ws == false) || (fd == null)) {
                        fd = new FormatData();
                        if (m_hasText) m_fdList.add(fd); else m_preNT.add(fd);
                    }
                    fd.appendOrig(c);
                    if (m_ws == true) continue;
                    m_ws = true;
                    fd.appendDisplay(' ');
                } else {
                    if ((m_hasText == false) || (m_ws == true) || (fd == null)) {
                        fd = new FormatData();
                        m_fdList.add(fd);
                    }
                    fd.appendOrig(c);
                    if (c == '&') {
                        c = getEscChar(fd);
                    }
                    if ((c == 160) && (!m_hasText)) {
                        fd.appendDisplay(c);
                        m_ws = true;
                        m_hasText = false;
                    } else {
                        fd.appendDisplay(c);
                        fd.setHasText(true);
                        m_hasText = true;
                        m_ws = false;
                    }
                }
            }
            writeEntry();
        } catch (IOException e1) {
            System.out.println("OO file write error: '" + m_file + "' at line " + line());
            fileWriteError(e1);
        } catch (ParseException e) {
            System.out.println("OO parse error: '" + m_file + "' at line " + (e.getErrorOffset() + line()));
            throw new IOException("parse error in file " + m_file + " at line " + (e.getErrorOffset() + line()) + " - " + e);
        }
    }

    protected char getEscChar(FormatData fd) throws IOException, ParseException {
        char c = 0;
        int i;
        int ctr = 0;
        int numeric = 0;
        int val = 0;
        markStream();
        LBuffer buf = new LBuffer(8);
        while ((i = getNextChar()) >= 0) {
            c = (char) i;
            fd.appendOrig(c);
            if (ctr == 0) {
                if ((c == 10) || (c == 13) || (c == 0) || (c == ' ')) {
                    fd.appendDisplay('&');
                    c = ' ';
                    break;
                }
                if (c == '#') {
                    numeric = 1;
                    ctr++;
                    continue;
                }
            } else if ((ctr == 1) && (numeric > 0)) {
                if ((c == 'x') || (c == 'X')) {
                    numeric = 2;
                    ctr++;
                    continue;
                }
            }
            if (numeric > 0) {
                if (c == ';') {
                    c = (char) val;
                    break;
                }
                if (numeric == 1) {
                    if ((c >= '0') && (c <= '9')) val = val * 10 + (c - '0'); else {
                        pushNextChar(c);
                        c = (char) val;
                        break;
                    }
                } else if (numeric == 2) {
                    if (c > 'Z') c -= 'a' - 'A';
                    if ((c >= '0') && (c <= '9')) val = val * 16 + (c - '0'); else if ((c >= 'A') && (c <= 'F')) val = val * 16 + (c - 'A'); else {
                        pushNextChar(c);
                        c = (char) val;
                        break;
                    }
                }
                ctr++;
                if (ctr > 10) throw new ParseException("&---; " + buf.string(), 0);
                continue;
            } else if ((c == 10) || (c == 13) || (ctr > 10)) {
                resetToMark();
                c = '&';
                break;
            }
            if (c == ';') {
                c = OOParser.convert(buf.string());
                break;
            }
            buf.append(c);
            ctr++;
        }
        if (c == 0) throw new IOException("unrecognized escape character" + " at line " + line());
        return c;
    }

    protected void handleTag() throws ParseException, IOException {
        OOTag tag = null;
        FormatData fd = null;
        tag = OOParser.identTag(this);
        fd = new FormatData(tag.close());
        fd.setOrig(tag.verbatum());
        if (tag.type() == OOTag.TAG_FORMAT) {
            if (m_hasText) {
                m_fdList.add(fd);
            } else {
                m_preNT.add(fd);
            }
            OOTag cand = null;
            if (tag.close()) {
                ListIterator it;
                it = m_tagList.listIterator(m_tagList.size());
                while (it.hasPrevious()) {
                    cand = (OOTag) it.previous();
                    if (tag.willPartner(cand)) {
                        cand.setPartner(true);
                        tag.setPartner(true);
                        fd.setTagData(cand.shortcut(), cand.num());
                        break;
                    }
                }
            } else {
                tag.setNum(m_tagList.size());
                m_tagList.add(tag);
                fd.setTagData(tag.shortcut(), tag.num());
            }
            fd.finalize();
        } else {
            m_postNT.add(fd);
            fd.finalize();
            writeEntry();
        }
    }

    protected void writeEntry() throws IOException {
        ListIterator it;
        FormatData fd = null;
        LBuffer buf = null;
        compressOutputData();
        if ((m_fdList.size() == 0) && (m_outFile == null)) {
            if (m_outFile == null) {
                m_preNT.clear();
                m_postNT.clear();
            } else {
                it = m_postNT.listIterator(m_postNT.size());
                while (it.hasPrevious()) {
                    fd = (FormatData) it.previous();
                    m_preNT.add(fd);
                }
            }
            m_postNT.clear();
            m_fdList.clear();
            m_tagList.clear();
            m_ws = false;
            m_hasText = false;
            return;
        }
        if (m_outFile != null) {
            it = m_preNT.listIterator();
            while (it.hasNext()) {
                fd = (FormatData) it.next();
                buf = fd.getOrig();
                m_outFile.write(buf.getBuf(), 0, buf.size());
            }
        }
        if (m_fdList.size() > 0) {
            it = m_fdList.listIterator();
            LBuffer out = new LBuffer(256);
            while (it.hasNext()) {
                fd = (FormatData) it.next();
                out.append(fd.getDisplay());
            }
            processEntry(out, m_file);
        }
        if (m_outFile != null) {
            it = m_postNT.listIterator(m_postNT.size());
            while (it.hasPrevious()) {
                fd = (FormatData) it.previous();
                buf = fd.getOrig();
                m_outFile.write(buf.getBuf(), 0, buf.size());
            }
        }
        m_preNT.clear();
        m_postNT.clear();
        m_fdList.clear();
        m_tagList.clear();
        m_ws = false;
        m_hasText = false;
    }

    protected void compressOutputData() {
        boolean change = true;
        FormatData fd_head = null;
        FormatData fd_tail = null;
        ListIterator it;
        int ctr = 0;
        int len;
        FormatData fd = null;
        while (change) {
            if (m_fdList.size() == 0) break;
            fd_head = (FormatData) m_fdList.getFirst();
            fd_tail = (FormatData) m_fdList.getLast();
            if (fd_head.isWhiteSpace()) {
                m_preNT.add(fd_head);
                m_fdList.removeFirst();
                continue;
            }
            if (fd_tail.isWhiteSpace()) {
                m_postNT.add(fd_tail);
                m_fdList.removeLast();
                continue;
            }
            if (m_fdList.size() == 1) {
                if (fd_head.isTag()) {
                    m_preNT.add(fd_head);
                    m_fdList.clear();
                }
                break;
            }
            if (fd_tail.isTag() && !fd_tail.isCloseTag()) {
                m_postNT.add(fd_tail);
                m_fdList.removeLast();
                continue;
            }
            if (fd_head.isTag() && fd_head.isCloseTag()) {
                m_preNT.add(fd_head);
                m_fdList.removeFirst();
                continue;
            }
            if (fd_tail.isTag() && fd_head.isTag() && (fd_tail.tagData() == fd_head.tagData())) {
                if (m_fdList.size() == 1) break;
                m_postNT.add(fd_tail);
                m_preNT.add(fd_head);
                m_fdList.removeFirst();
                m_fdList.removeLast();
                continue;
            }
            if (fd_head.isTag()) {
                it = m_fdList.listIterator();
                ctr = 0;
                len = m_fdList.size();
                while (it.hasNext()) {
                    fd = (FormatData) it.next();
                    if (ctr > 0) {
                        if (fd_head.tagData() == fd.tagData()) break;
                    }
                    ctr++;
                }
                if (ctr < len) {
                    m_preNT.add(fd_head);
                    m_fdList.removeFirst();
                    continue;
                }
            }
            if (m_fdList.size() == 1) {
                if (fd_head.isTag()) {
                    m_preNT.add(fd_head);
                    m_fdList.clear();
                }
                break;
            }
            if (fd_tail.isTag()) {
                it = m_fdList.listIterator();
                ctr = 0;
                len = m_fdList.size();
                while (it.hasNext()) {
                    fd = (FormatData) it.next();
                    if (ctr < len) {
                        if (fd_tail.tagData() == fd.tagData()) break;
                    }
                    ctr++;
                }
                if (ctr < len) {
                    m_postNT.add(fd_tail);
                    m_fdList.removeLast();
                    continue;
                }
            }
            change = false;
        }
    }

    private LinkedList m_tagList = null;

    private LinkedList m_preNT;

    private LinkedList m_fdList;

    private LinkedList m_postNT;

    private int m_ec = 0;

    private boolean m_ws = false;

    private boolean m_hasText = false;

    public static void main(String[] args) {
        OOFileHandler txt = new OOFileHandler();
        CommandThread.core = new CommandThread(null);
        String s;
        if (args.length > 0) s = args[0]; else s = "samp.html";
        try {
            txt.setTestMode(true);
            txt.load(s);
            txt.write(s, "out.html");
        } catch (IOException e) {
            System.out.println("error - " + e);
        }
        System.exit(0);
    }
}
