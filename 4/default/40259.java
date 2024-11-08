import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import javax.swing.*;
import com.sun.pdfview.*;

public class JD extends JFrame {

    static final int WIDTH = 400;

    static final int HEIGHT = 400;

    static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    enum InsIndex {

        AALOAD(1), AASTORE(2, 2), ACONST_NULL(4), ALOAD(5), ALOAD_0(6), ALOAD_1(7), ALOAD_2(8), ALOAD_3(9), ANEWARRAY(10), ARETURN(11), ARRAYLENGTH(12), ASTORE(13), ASTORE_0(14), ASTORE_1(15), ASTORE_2(16), ASTORE_3(17), ATHROW(18), BALOAD(19), BASTORE(20), BIPUSH(21), CALOAD(22), CASTORE(23), CHECKCAST(24, 2), D2F(26), D2I(27), D2L(28), DADD(29), DALOAD(30), DASTORE(31), DCMPG(32), DCMPL(33), DCONST_0(34), DCONST_1(35), DDIV(36), DLOAD(37), DLOAD_0(38), DLOAD_1(39), DLOAD_2(40), DLOAD_3(41), DMUL(42), DNEG(43), DREM(44), DRETURN(45), DSTORE(46), DSTORE_0(47), DSTORE_1(48), DSTORE_2(49), DSTORE_3(50), DSUB(51), DUP(52), DUP_X1(53), DUP_X2(54), DUP2(55), DUP2_X1(56), DUP2_X2(57), F2D(58), F2I(59), F2L(60), FADD(61), FALOAD(62), FASTORE(63), FCMPG(64), FCMPL(65), FCONST_0(66), FCONST_1(67), FCONST_2(68), FDIV(69), FLOAD(70), FLOAD_0(71), FLOAD_1(72), FLOAD_2(73), FLOAD_3(74), FMUL(75), FNEG(76), FREM(77), FRETURN(78), FSTORE(79), FSTORE_0(80), FSTORE_1(81), FSTORE_2(82), FSTORE_3(83), FSUB(84), GETFIELD(85), GETSTATIC(86), GOTO(87), GOTO_W(88), I2B(89), I2C(90), I2D(91), I2F(92), I2L(93), I2S(94), IADD(95), IALOAD(96), IAND(97), IASTORE(98), ICONST_0(99), ICONST_1(100), ICONST_2(101), ICONST_3(102), ICONST_4(103), ICONST_5(104), ICONST_M1(105), IDIV(106), IF_ACMPEQ(107), IF_ACMPNE(108), IF_ICMPEQ(109), IF_ICMPGE(110), IF_ICMPGT(111), IF_ICMPLE(112), IF_ICMPLT(113), IF_ICMPNE(114), IFEQ(115), IFGE(116), IFGT(117), IFLE(118), IFLT(119), IFNE(120), IFNONNULL(121), IFNULL(122), IINC(123), ILOAD(124), ILOAD_0(125), ILOAD_1(126), ILOAD_2(127), ILOAD_3(128), IMUL(129), INEG(130), INSTANCEOF(131, 2), INVOKEINTERFACE(133, 2), INVOKESPECIAL(135, 3), INVOKESTATIC(138, 2), INVOKEVIRTUAL(140, 2), IOR(142), IREM(143), IRETURN(144), ISHL(145), ISHR(146), ISTORE(147), ISTORE_0(148), ISTORE_1(149), ISTORE_2(150), ISTORE_3(151), ISUB(152), IUSHR(153), IXOR(154), JSR(155), JSR_W(156), L2D(157), L2F(158), L2I(159), LADD(160), LALOAD(161), LAND(162), LASTORE(163), LCMP(164), LCONST_0(165), LCONST_1(166), LDC(167), LDC_W(168), LDC2_W(169), LDIV(170), LLOAD(171), LLOAD_0(172), LLOAD_1(173), LLOAD_2(174), LLOAD_3(175), LMUL(176), LNEG(177), LOOKUPSWITCH(178), LOR(179), LREM(180), LRETURN(181), LSHL(182), LSHR(183), LSTORE(184), LSTORE_0(185), LSTORE_1(186), LSTORE_2(187), LSTORE_3(188), LSUB(189), LUSHR(190), LXOR(191), MONITORENTER(192), MONITOREXIT(193), MULTIANEWARRAY(194), NEW(195), NEWARRAY(196), NOP(197), POP(198), POP2(199), PUTFIELD(200), PUTSTATIC(201), RET(202), RETURN(203), SALOAD(204), SASTORE(205), SIPUSH(206), SWAP(207), TABLESWITCH(208), WIDE(209);

        final int startPage;

        final int numPages;

        InsIndex(int startPage) {
            this(startPage, 1);
        }

        InsIndex(int startPage, int numPages) {
            this.startPage = startPage;
            this.numPages = numPages;
        }
    }

    JList listing;

    String[] lines;

    public JD(String title) {
        super(title);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JMenu menu = new JMenu("File");
        JMenuItem mi = new JMenuItem("Open...");
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                String[] newLines = doOpen();
                if (newLines != null) {
                    lines = newLines;
                    listing.setListData(lines);
                }
            }
        });
        menu.add(mi);
        menu.addSeparator();
        mi = new JMenuItem("Exit");
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                dispose();
            }
        });
        menu.add(mi);
        JMenuBar mb = new JMenuBar();
        mb.add(menu);
        setJMenuBar(mb);
        listing = new JList();
        listing.setCellRenderer(new NoHiliteCellRenderer(FONT));
        listing.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent me) {
                if (lines == null) return;
                doShowHelp(me.getX(), me.getY());
            }
        });
        setContentPane(new JScrollPane(listing));
        setSize(WIDTH, HEIGHT);
        setVisible(true);
    }

    void doShowHelp(int x, int y) {
        FontMetrics fm = listing.getFontMetrics(FONT);
        int row = y / fm.getHeight();
        int col = x / fm.charWidth(' ');
        if (row >= lines.length || col >= lines[row].length()) return;
        if (lines[row].charAt(col) != ' ') {
            int sc = col;
            while (sc > 0 && lines[row].charAt(sc - 1) != ' ') sc--;
            int ec = col;
            while (ec < lines[row].length() - 1 && lines[row].charAt(ec + 1) != ' ') ec++;
            String text = lines[row].substring(sc, ec + 1);
            for (InsIndex insIndexEntry : InsIndex.values()) if (insIndexEntry.name().equalsIgnoreCase(text)) {
                try {
                    new HelpViewer(this, insIndexEntry);
                } catch (IOException ioe) {
                    JOptionPane.showMessageDialog(this, ioe.getMessage());
                }
                break;
            }
        }
    }

    String[] doOpen() {
        JFileChooser fcOpen = new JFileChooser();
        fcOpen.setCurrentDirectory(new File(System.getProperty("user.dir")));
        fcOpen.setAcceptAllFileFilterUsed(false);
        fcOpen.setFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String s = f.getName();
                int i = s.lastIndexOf('.');
                if (i > 0 && i < s.length() - 1) {
                    String ext;
                    ext = s.substring(i + 1).toLowerCase();
                    if (ext.equals("class")) return true;
                }
                return false;
            }

            public String getDescription() {
                return "Accepts .class files";
            }
        });
        if (fcOpen.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        File file = fcOpen.getSelectedFile();
        String name = file.getName();
        int i = name.lastIndexOf('.');
        if (i != -1) name = name.substring(0, i);
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("javap -v " + name);
            ArrayList<String> linesBuffer = new ArrayList<String>();
            InputStream is = process.getInputStream();
            StringBuffer sb = new StringBuffer();
            int ch;
            while ((ch = is.read()) != -1) if (ch == '\r') continue; else if (ch == '\n') {
                if (sb.length() == 0) sb.append(' ');
                linesBuffer.add(sb.toString());
                sb.setLength(0);
            } else sb.append((char) ch);
            String[] lines = linesBuffer.toArray(new String[0]);
            for (i = 0; i < lines.length; i++) lines[i] = lines[i].replace('\t', ' ');
            return lines;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        Runnable r = new Runnable() {

            public void run() {
                new JD("Java Disassembler");
            }
        };
        EventQueue.invokeLater(r);
    }
}

class HelpViewer extends JDialog {

    private Image[] images;

    private int index;

    private JD.InsIndex entry;

    HelpViewer(JFrame f, JD.InsIndex entry) throws IOException {
        super(f, "Java Disassembler Help -- " + entry.name().toLowerCase(), true);
        this.entry = entry;
        getImages();
        createGUI(f);
    }

    private void createGUI(JFrame f) {
        setLayout(new BorderLayout());
        final JLabel label = new JLabel(new ImageIcon(images[index]));
        final JScrollPane sp = new JScrollPane(label);
        add(sp, BorderLayout.CENTER);
        JPanel pnlControl = new JPanel();
        final JButton btnPrev = new JButton("<");
        final JButton btnNext = new JButton(">");
        btnPrev.setEnabled(false);
        if (entry.numPages != 1) {
            ActionListener al;
            al = new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    btnNext.setEnabled(true);
                    label.setIcon(new ImageIcon(images[--index]));
                    sp.getVerticalScrollBar().setValue(0);
                    if (index == 0) btnPrev.setEnabled(false);
                }
            };
            btnPrev.addActionListener(al);
        }
        if (entry.numPages == 1) btnNext.setEnabled(false); else {
            ActionListener al;
            al = new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    btnPrev.setEnabled(true);
                    label.setIcon(new ImageIcon(images[++index]));
                    sp.getVerticalScrollBar().setValue(0);
                    if (index == entry.numPages - 1) btnNext.setEnabled(false);
                }
            };
            btnNext.addActionListener(al);
        }
        pnlControl.add(btnPrev);
        pnlControl.add(btnNext);
        add(pnlControl, BorderLayout.EAST);
        setSize(600, 600);
        setVisible(true);
    }

    private void getImages() throws IOException {
        RandomAccessFile raf = new RandomAccessFile("jvmins.pdf", "r");
        FileChannel fc = raf.getChannel();
        ByteBuffer buf = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        PDFFile pdfFile = new PDFFile(buf);
        images = new Image[entry.numPages];
        for (int i = 0; i < entry.numPages; i++) {
            PDFPage page = pdfFile.getPage(entry.startPage + i);
            Rectangle2D r2d = page.getBBox();
            r2d.setRect(r2d.getX() + 36, r2d.getY() + 36, r2d.getWidth() - 72, r2d.getHeight() - 72);
            Dimension dim = page.getUnstretchedSize(700, 700, r2d);
            images[i] = page.getImage(dim.width, dim.height, r2d, null, true, true);
        }
    }
}

class NoHiliteCellRenderer extends JLabel implements ListCellRenderer {

    private Font font;

    NoHiliteCellRenderer(Font font) {
        this.font = font;
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        setFont(font);
        setText(value.toString());
        setForeground(Color.black);
        setBackground(Color.white);
        return this;
    }
}
