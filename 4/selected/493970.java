package papercase;

import com.pdfjet.A4;
import com.pdfjet.Font;
import com.pdfjet.Line;
import com.pdfjet.PDF;
import com.pdfjet.Page;
import com.pdfjet.Path;
import com.pdfjet.Point;
import com.pdfjet.TextLine;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author bluepoke
 */
public class PaperCaseGUI extends javax.swing.JFrame {

    private static Logger logger;

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private static final String USER_HOME = System.getProperty("user.home");

    private final double A4_PT_X = 595;

    private final double A4_PT_Y = 842;

    private final double A4_CM_X = 21;

    private final double A4_CM_Y = 29.7;

    private final double A4_RES_X = A4_PT_X / A4_CM_X;

    private final double A4_RES_Y = A4_PT_Y / A4_CM_Y;

    private double lineLength = 3;

    private double distanceFromMargin = 0.5;

    private final double SMALL_TITLE_Y = 9.855;

    private final double LINE_1_2_X = 4.2;

    private final double LINE_3_Y = 14.85;

    private final double LINE_3_EDGE_Y = A4_CM_Y - (A4_CM_Y - LINE_3_Y) / 2;

    private final double LINE_4_Y = 20.125;

    private final double LINE_5_Y = 18.95;

    private final double LINE_4_EDGE_Y = LINE_4_Y / 2;

    private final double LINE_5_EDGE_Y = LINE_5_Y / 2;

    private final double LINE_6_7_8_9_Y = A4_CM_Y - ((A4_CM_Y - LINE_3_Y) / 2);

    private final double LINE_6_7_8_9_X = LINE_1_2_X;

    private final double LINE_6_7_8_9_H = LINE_1_2_X;

    private final double SMALL_TITLE_SIZE = 8;

    private final double BIG_TITLE_SIZE = 12;

    private final double NORMAL_TEXT_SIZE = 10;

    private final double NORMAL_TEXT_LINE_SPACING = 1.2;

    private final double NUMBER_SIZE = 6;

    private final double HELVETICA_CAP_HEIGHT = 0.72;

    private final String DASHED_PATTERN = "--  ";

    private final String DASHED_PATTERN_BENDING = "--    ";

    private double sizeOfSymbols = 0.3;

    private double distanceOfSymbols = 0.2;

    /** Creates new form PaperCaseGUI */
    public PaperCaseGUI() {
        logger = Logger.getLogger(this.getClass().getName());
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        lblTitle = new javax.swing.JLabel();
        txfTitle = new javax.swing.JTextField();
        btnCreatePDF = new javax.swing.JButton();
        cbxDrawSymbols = new javax.swing.JCheckBox();
        cbxDrawNumbers = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        txaDescription = new javax.swing.JTextArea();
        lblDescription = new javax.swing.JLabel();
        cbxDrawBendingLines = new javax.swing.JCheckBox();
        menuBar = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuItemFileQuit = new javax.swing.JMenuItem();
        menuOptions = new javax.swing.JMenu();
        menuHelp = new javax.swing.JMenu();
        menuItemHelpAbout = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        lblTitle.setText("Title:");
        btnCreatePDF.setText("Create PDF");
        btnCreatePDF.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreatePDFActionPerformed(evt);
            }
        });
        cbxDrawSymbols.setSelected(true);
        cbxDrawSymbols.setText("draw symbols");
        cbxDrawNumbers.setSelected(true);
        cbxDrawNumbers.setText("draw numbers");
        txaDescription.setColumns(20);
        txaDescription.setRows(5);
        jScrollPane1.setViewportView(txaDescription);
        lblDescription.setText("Description:");
        cbxDrawBendingLines.setText("draw full bending lines");
        menuFile.setText("File");
        menuItemFileQuit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        menuItemFileQuit.setText("Quit");
        menuItemFileQuit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemFileQuitActionPerformed(evt);
            }
        });
        menuFile.add(menuItemFileQuit);
        menuBar.add(menuFile);
        menuOptions.setText("Options");
        menuBar.add(menuOptions);
        menuHelp.setText("Help");
        menuItemHelpAbout.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        menuItemHelpAbout.setText("About...");
        menuHelp.add(menuItemHelpAbout);
        menuBar.add(menuHelp);
        setJMenuBar(menuBar);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblTitle).addComponent(lblDescription)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(cbxDrawBendingLines).addComponent(cbxDrawSymbols).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(cbxDrawNumbers).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 119, Short.MAX_VALUE).addComponent(btnCreatePDF)).addComponent(txfTitle, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblTitle).addComponent(txfTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(71, 71, 71).addComponent(cbxDrawBendingLines).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(cbxDrawSymbols).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btnCreatePDF).addComponent(cbxDrawNumbers))).addComponent(lblDescription)).addContainerGap()));
        pack();
    }

    private void menuItemFileQuitActionPerformed(java.awt.event.ActionEvent evt) {
        System.exit(0);
    }

    private void btnCreatePDFActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser jfc = new JFileChooser();
        jfc.setMultiSelectionEnabled(false);
        jfc.setSelectedFile(new File(USER_HOME + FILE_SEPARATOR + "papercase.pdf"));
        int jfcButton = jfc.showSaveDialog(this);
        if (jfcButton == JFileChooser.APPROVE_OPTION) {
            if (jfc.getSelectedFile().exists()) {
                int option = JOptionPane.showConfirmDialog(this, "The selected File already exists.\nDo you want to overwrite it?", "Overwrite?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (option == JOptionPane.CANCEL_OPTION) {
                    return;
                } else if (option == JOptionPane.NO_OPTION) {
                    btnCreatePDFActionPerformed(evt);
                    return;
                }
            }
            try {
                boolean drawBendingLines = cbxDrawBendingLines.isSelected();
                boolean drawSymbols = cbxDrawSymbols.isSelected();
                boolean drawNumbers = cbxDrawNumbers.isSelected();
                double x, y, w, h;
                PDF pdf = new PDF();
                Page page = new Page(pdf, A4.PORTRAIT);
                Font smallTitleFont = new Font(pdf, "Helvetica");
                smallTitleFont.setSize(SMALL_TITLE_SIZE);
                Font bigTitleFont = new Font(pdf, "Helvetica");
                bigTitleFont.setSize(BIG_TITLE_SIZE);
                Font numberFont = new Font(pdf, "Helvetica");
                numberFont.setSize(NUMBER_SIZE);
                Font normalFont = new Font(pdf, "Helvetica");
                normalFont.setSize(NORMAL_TEXT_SIZE);
                TextLine smallTitleText = new TextLine(smallTitleFont, txfTitle.getText());
                x = A4_CM_X / 2 * A4_RES_X - smallTitleFont.stringWidth(txfTitle.getText()) / 2;
                y = SMALL_TITLE_Y * A4_RES_Y;
                smallTitleText.setPosition(x, y);
                smallTitleText.drawOn(page);
                TextLine frontTitleText = new TextLine(bigTitleFont, txfTitle.getText());
                x = (LINE_1_2_X + distanceFromMargin) * A4_RES_X;
                y = (LINE_4_Y / 2 + distanceFromMargin) * A4_RES_Y + HELVETICA_CAP_HEIGHT * BIG_TITLE_SIZE;
                frontTitleText.setPosition(x, y);
                frontTitleText.drawOn(page);
                TextLine backTitleText = new TextLine(bigTitleFont, txfTitle.getText());
                x = (A4_CM_X - LINE_1_2_X - distanceFromMargin) * A4_RES_X;
                y = (LINE_5_Y / 2 - distanceFromMargin) * A4_RES_Y - HELVETICA_CAP_HEIGHT * BIG_TITLE_SIZE;
                backTitleText.setPosition(x, y);
                backTitleText.setTextDirection(180);
                backTitleText.drawOn(page);
                x = (LINE_1_2_X + distanceFromMargin) * A4_RES_X;
                y = (LINE_4_Y / 2 + distanceFromMargin) * A4_RES_Y + BIG_TITLE_SIZE + NORMAL_TEXT_SIZE;
                String[] descLines = txaDescription.getText().split("\n");
                for (int i = 0; i < descLines.length; i++) {
                    String l = descLines[i];
                    double yl = y + i * NORMAL_TEXT_SIZE * NORMAL_TEXT_LINE_SPACING;
                    TextLine tl = new TextLine(normalFont, l);
                    tl.setPosition(x, yl);
                    tl.drawOn(page);
                }
                x = LINE_1_2_X * A4_RES_X;
                y = 0;
                w = 0;
                h = lineLength * A4_RES_Y;
                Line l1t = new Line(x, y, x + w, y + h);
                l1t.drawOn(page);
                x = LINE_1_2_X * A4_RES_X;
                y = (A4_CM_Y - lineLength) * A4_RES_Y;
                w = 0;
                h = lineLength * A4_RES_Y;
                Line l1b = new Line(x, y, x + w, y + h);
                l1b.drawOn(page);
                TextLine number1 = new TextLine(numberFont, "1");
                x = (LINE_1_2_X - distanceOfSymbols) * A4_RES_X - numberFont.stringWidth(number1.getText());
                y = l1b.getStartPoint().getY() + NUMBER_SIZE * HELVETICA_CAP_HEIGHT;
                number1.setPosition(x, y);
                if (drawNumbers) {
                    number1.drawOn(page);
                }
                x = (LINE_1_2_X - distanceOfSymbols) * A4_RES_X - numberFont.stringWidth(number1.getText());
                y = l1t.getEndPoint().getY();
                number1.setPosition(x, y);
                if (drawNumbers) {
                    number1.drawOn(page);
                }
                if (drawBendingLines) {
                    x = LINE_1_2_X * A4_RES_X;
                    y = 0;
                    w = 0;
                    h = A4_PT_Y;
                    Line l1d = new Line(x, y, x + w, y + h);
                    l1d.setPattern(DASHED_PATTERN_BENDING);
                    l1d.drawOn(page);
                }
                x = (A4_CM_X - LINE_1_2_X) * A4_RES_X;
                y = 0;
                w = 0;
                h = lineLength * A4_RES_Y;
                Line l2t = new Line(x, y, x + w, y + h);
                l2t.drawOn(page);
                x = (A4_CM_X - LINE_1_2_X) * A4_RES_X;
                y = (A4_CM_Y - lineLength) * A4_RES_Y;
                w = 0;
                h = lineLength * A4_RES_Y;
                Line l2b = new Line(x, y, x + w, y + h);
                l2b.drawOn(page);
                TextLine number2 = new TextLine(numberFont, "2");
                x = l2b.getStartPoint().getX() + distanceOfSymbols * A4_RES_X;
                y = l2b.getStartPoint().getY() + NUMBER_SIZE * HELVETICA_CAP_HEIGHT;
                number2.setPosition(x, y);
                if (drawNumbers) {
                    number2.drawOn(page);
                }
                x = l2t.getEndPoint().getX() + distanceOfSymbols * A4_RES_X;
                y = l2t.getEndPoint().getY();
                number2.setPosition(x, y);
                if (drawNumbers) {
                    number2.drawOn(page);
                }
                if (drawBendingLines) {
                    x = (A4_CM_X - LINE_1_2_X) * A4_RES_X;
                    y = 0;
                    w = 0;
                    h = A4_PT_Y;
                    Line l2d = new Line(x, y, x + w, y + h);
                    l2d.setPattern(DASHED_PATTERN_BENDING);
                    l2d.drawOn(page);
                }
                x = 0;
                y = LINE_3_Y * A4_RES_Y;
                w = lineLength * A4_RES_X;
                h = 0;
                Line l3l = new Line(x, y, x + w, y + h);
                l3l.drawOn(page);
                x = (A4_CM_X - lineLength) * A4_RES_X;
                y = LINE_3_Y * A4_RES_Y;
                w = lineLength * A4_RES_X;
                h = 0;
                Line l3r = new Line(x, y, x + w, y + h);
                l3r.drawOn(page);
                Path p3l = drawTriangle(l3l.getEndPoint().getX() - (sizeOfSymbols / 2 + distanceOfSymbols) * A4_RES_X, l3l.getEndPoint().getY() + distanceOfSymbols * A4_RES_Y, sizeOfSymbols, true);
                if (drawSymbols) {
                    p3l.drawOn(page);
                }
                TextLine number3 = new TextLine(numberFont, "3");
                x = l3l.getEndPoint().getX() - (sizeOfSymbols + 2 * distanceOfSymbols) * A4_RES_X - numberFont.stringWidth(number3.getText());
                y = l3l.getEndPoint().getY() + (distanceOfSymbols + sizeOfSymbols) * A4_RES_Y;
                number3.setPosition(x, y);
                if (drawNumbers) {
                    number3.drawOn(page);
                }
                Path p3r = drawTriangle(l3r.getStartPoint().getX() + (sizeOfSymbols / 2 + distanceOfSymbols) * A4_RES_X, l3r.getStartPoint().getY() + distanceOfSymbols * A4_RES_Y, sizeOfSymbols, true);
                if (drawSymbols) {
                    p3r.drawOn(page);
                }
                x = l3r.getStartPoint().getX() + (sizeOfSymbols + 2 * distanceOfSymbols) * A4_RES_X;
                y = l3r.getStartPoint().getY() + (distanceOfSymbols + sizeOfSymbols) * A4_RES_Y;
                number3.setPosition(x, y);
                if (drawNumbers) {
                    number3.drawOn(page);
                }
                if (drawBendingLines) {
                    x = LINE_1_2_X * A4_RES_X;
                    y = LINE_3_EDGE_Y * A4_RES_Y;
                    w = (A4_CM_X - 2 * LINE_1_2_X) * A4_RES_X;
                    h = 0;
                    Line l3d = new Line(x, y, x + w, y + h);
                    l3d.setPattern(DASHED_PATTERN_BENDING);
                    l3d.drawOn(page);
                }
                x = 0;
                y = LINE_4_Y * A4_RES_Y;
                w = lineLength * A4_RES_X;
                h = 0;
                Line l4l = new Line(x, y, x + w, y + h);
                l4l.drawOn(page);
                x = (A4_CM_X - lineLength) * A4_RES_X;
                y = LINE_4_Y * A4_RES_Y;
                w = lineLength * A4_RES_X;
                h = 0;
                Line l4r = new Line(x, y, x + w, y + h);
                l4r.drawOn(page);
                Path p4l = drawTriangle(l4l.getEndPoint().getX() - (sizeOfSymbols / 2 + distanceOfSymbols) * A4_RES_X, l4l.getEndPoint().getY() - distanceOfSymbols * A4_RES_Y, sizeOfSymbols, false);
                if (drawSymbols) {
                    p4l.drawOn(page);
                }
                TextLine number4 = new TextLine(numberFont, "4");
                x = l4l.getEndPoint().getX() - (sizeOfSymbols + 2 * distanceOfSymbols) * A4_RES_X - numberFont.stringWidth(number4.getText());
                y = l4l.getEndPoint().getY() - (distanceOfSymbols + sizeOfSymbols) * A4_RES_Y + NUMBER_SIZE * HELVETICA_CAP_HEIGHT;
                number4.setPosition(x, y);
                if (drawNumbers) {
                    number4.drawOn(page);
                }
                Path p4r = drawTriangle(l4r.getStartPoint().getX() + (sizeOfSymbols / 2 + distanceOfSymbols) * A4_RES_X, l4r.getStartPoint().getY() - distanceOfSymbols * A4_RES_Y, sizeOfSymbols, false);
                if (drawSymbols) {
                    p4r.drawOn(page);
                }
                x = l4r.getStartPoint().getX() + (sizeOfSymbols + 2 * distanceOfSymbols) * A4_RES_X;
                y = l4r.getStartPoint().getY() - (distanceOfSymbols + sizeOfSymbols) * A4_RES_Y + NUMBER_SIZE * HELVETICA_CAP_HEIGHT;
                number4.setPosition(x, y);
                if (drawNumbers) {
                    number4.drawOn(page);
                }
                if (drawBendingLines) {
                    x = LINE_1_2_X * A4_RES_X;
                    y = LINE_4_EDGE_Y * A4_RES_Y;
                    w = (A4_CM_X - 2 * LINE_1_2_X) * A4_RES_X;
                    h = 0;
                    Line l4d = new Line(x, y, x + w, y + h);
                    l4d.setPattern(DASHED_PATTERN_BENDING);
                    l4d.drawOn(page);
                }
                x = 0;
                y = LINE_5_Y * A4_RES_Y;
                w = lineLength * A4_RES_X;
                h = 0;
                Line l5l = new Line(x, y, x + w, y + h);
                l5l.drawOn(page);
                x = (A4_CM_X - lineLength) * A4_RES_X;
                y = LINE_5_Y * A4_RES_Y;
                w = lineLength * A4_RES_X;
                h = 0;
                Line l5r = new Line(x, y, x + w, y + h);
                l5r.drawOn(page);
                Path p5l = drawTriangle(l5l.getEndPoint().getX() - (sizeOfSymbols / 2 + distanceOfSymbols) * A4_RES_X, l5l.getEndPoint().getY() - distanceOfSymbols * A4_RES_Y, sizeOfSymbols, false);
                if (drawSymbols) {
                    p5l.drawOn(page);
                }
                TextLine number5 = new TextLine(numberFont, "5");
                x = l5l.getEndPoint().getX() - (sizeOfSymbols + 2 * distanceOfSymbols) * A4_RES_X - numberFont.stringWidth(number5.getText());
                y = l5l.getEndPoint().getY() - (distanceOfSymbols + sizeOfSymbols) * A4_RES_Y + NUMBER_SIZE * HELVETICA_CAP_HEIGHT;
                number5.setPosition(x, y);
                if (drawNumbers) {
                    number5.drawOn(page);
                }
                Path p5r = drawTriangle(l5r.getStartPoint().getX() + (sizeOfSymbols / 2 + distanceOfSymbols) * A4_RES_X, l5r.getStartPoint().getY() - distanceOfSymbols * A4_RES_Y, sizeOfSymbols, false);
                if (drawSymbols) {
                    p5r.drawOn(page);
                }
                x = l5r.getStartPoint().getX() + (sizeOfSymbols + 2 * distanceOfSymbols) * A4_RES_X;
                y = l5r.getStartPoint().getY() - (distanceOfSymbols + sizeOfSymbols) * A4_RES_Y + NUMBER_SIZE * HELVETICA_CAP_HEIGHT;
                number5.setPosition(x, y);
                if (drawNumbers) {
                    number5.drawOn(page);
                }
                if (drawBendingLines) {
                    x = LINE_1_2_X * A4_RES_X;
                    y = LINE_5_EDGE_Y * A4_RES_Y;
                    w = (A4_CM_X - 2 * LINE_1_2_X) * A4_RES_X;
                    h = 0;
                    Line l5d = new Line(x, y, x + w, y + h);
                    l5d.setPattern(DASHED_PATTERN_BENDING);
                    l5d.drawOn(page);
                }
                x = (A4_CM_X - LINE_6_7_8_9_X) * A4_RES_X;
                y = LINE_6_7_8_9_Y * A4_RES_Y;
                w = LINE_6_7_8_9_X * A4_RES_X;
                h = LINE_6_7_8_9_H * A4_RES_Y;
                Line l6 = new Line(x, y, x + w, y + h);
                l6.setPattern(DASHED_PATTERN);
                l6.drawOn(page);
                TextLine number6 = new TextLine(numberFont, "6");
                x = l6.getStartPoint().getX() + LINE_6_7_8_9_X / 3 * A4_RES_X;
                y = l6.getStartPoint().getY() + LINE_6_7_8_9_H / 3 * A4_RES_Y - distanceOfSymbols * A4_RES_Y;
                number6.setPosition(x, y);
                if (drawNumbers) {
                    number6.drawOn(page);
                }
                x = l6.getStartPoint().getX();
                y = l6.getStartPoint().getY();
                w = LINE_6_7_8_9_X * A4_RES_X;
                h = -LINE_6_7_8_9_H * A4_RES_Y;
                Line l7 = new Line(x, y, x + w, y + h);
                l7.setPattern(DASHED_PATTERN);
                l7.drawOn(page);
                TextLine number7 = new TextLine(numberFont, "7");
                x = l7.getStartPoint().getX() + LINE_6_7_8_9_X / 3 * A4_RES_X;
                y = l7.getStartPoint().getY() - LINE_6_7_8_9_H / 3 * A4_RES_Y + distanceOfSymbols * A4_RES_Y + HELVETICA_CAP_HEIGHT * NUMBER_SIZE;
                number7.setPosition(x, y);
                if (drawNumbers) {
                    number7.drawOn(page);
                }
                x = LINE_6_7_8_9_X * A4_RES_X;
                y = LINE_6_7_8_9_Y * A4_RES_Y;
                w = -LINE_6_7_8_9_X * A4_RES_X;
                h = LINE_6_7_8_9_H * A4_RES_Y;
                Line l8 = new Line(x, y, x + w, y + h);
                l8.setPattern(DASHED_PATTERN);
                l8.drawOn(page);
                TextLine number8 = new TextLine(numberFont, "8");
                x = l8.getStartPoint().getX() - LINE_6_7_8_9_X / 3 * A4_RES_X - numberFont.stringWidth(number8.getText());
                y = l8.getStartPoint().getY() + LINE_6_7_8_9_H / 3 * A4_RES_Y - distanceOfSymbols * A4_RES_Y;
                number8.setPosition(x, y);
                if (drawNumbers) {
                    number8.drawOn(page);
                }
                x = l8.getStartPoint().getX();
                y = l8.getStartPoint().getY();
                w = -LINE_6_7_8_9_X * A4_RES_X;
                h = -LINE_6_7_8_9_H * A4_RES_Y;
                Line l9 = new Line(x, y, x + w, y + h);
                l9.setPattern(DASHED_PATTERN);
                l9.drawOn(page);
                TextLine number9 = new TextLine(numberFont, "9");
                x = l9.getStartPoint().getX() - LINE_6_7_8_9_X / 3 * A4_RES_X - numberFont.stringWidth(number9.getText());
                y = l9.getStartPoint().getY() - LINE_6_7_8_9_H / 3 * A4_RES_Y + distanceOfSymbols * A4_RES_Y + HELVETICA_CAP_HEIGHT * NUMBER_SIZE;
                number9.setPosition(x, y);
                if (drawNumbers) {
                    number9.drawOn(page);
                }
                pdf.wrap();
                FileOutputStream fos = new FileOutputStream(jfc.getSelectedFile());
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                pdf.getData().writeTo(bos);
                bos.close();
                JOptionPane.showMessageDialog(this, "Succesfully saved " + jfc.getSelectedFile().getName(), "Saving successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                logger.severe(ex.getMessage());
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException ex) {
                    logger.severe(ex.getMessage());
                } catch (InstantiationException ex) {
                    logger.severe(ex.getMessage());
                } catch (IllegalAccessException ex) {
                    logger.severe(ex.getMessage());
                } catch (UnsupportedLookAndFeelException ex) {
                    logger.severe(ex.getMessage());
                }
                PaperCaseGUI pcgui = new PaperCaseGUI();
                pcgui.setTitle("PaperCase");
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                pcgui.setBounds((screen.width - pcgui.getWidth()) / 2, (screen.height - pcgui.getHeight()) / 2, pcgui.getWidth(), pcgui.getHeight());
                pcgui.setVisible(true);
            }
        });
    }

    private javax.swing.JButton btnCreatePDF;

    private javax.swing.JCheckBox cbxDrawBendingLines;

    private javax.swing.JCheckBox cbxDrawNumbers;

    private javax.swing.JCheckBox cbxDrawSymbols;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JLabel lblDescription;

    private javax.swing.JLabel lblTitle;

    private javax.swing.JMenuBar menuBar;

    private javax.swing.JMenu menuFile;

    private javax.swing.JMenu menuHelp;

    private javax.swing.JMenuItem menuItemFileQuit;

    private javax.swing.JMenuItem menuItemHelpAbout;

    private javax.swing.JMenu menuOptions;

    private javax.swing.JTextArea txaDescription;

    private javax.swing.JTextField txfTitle;

    /**
     * Creates a Path representing a triangle who's apex is pointing upwards
     * and it's width and height is of the given size.
     * @param posx Horizontal position of triangle's apex
     * @param posy Vertical position of triangle's apex
     * @param size Width and Height of the triangle, in cm
     * @param upwards Direction of the triangle. true for pointing upwards, false for pointing downwards
     * @return A Path representing a Triangle pointing upwards that can be drawn on a page
     */
    private Path drawTriangle(double posx, double posy, double size, boolean upwards) {
        Point p1 = new Point(posx, posy);
        Point p2;
        if (upwards) {
            p2 = new Point(p1.getX() + size / 2 * A4_RES_X, p1.getY() + size * A4_RES_Y);
        } else {
            p2 = new Point(p1.getX() + size / 2 * A4_RES_X, p1.getY() - size * A4_RES_Y);
        }
        Point p3 = new Point(p2.getX() - size * A4_RES_X, p2.getY());
        Path p = new Path();
        p.add(p1);
        p.add(p2);
        p.add(p3);
        p.add(p1);
        return p;
    }
}