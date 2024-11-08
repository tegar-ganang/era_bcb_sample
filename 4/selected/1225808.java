package com.skruk.elvis.admin.manage.marc21.gui;

import com.skruk.elvis.admin.gui.WizardPanel;
import com.skruk.elvis.admin.i18n.ResourceFormatter;
import com.skruk.elvis.admin.manage.marc21.Marc21Controlfield;
import com.skruk.elvis.admin.manage.marc21.Marc21Datafield;
import com.skruk.elvis.admin.manage.marc21.Marc21Description;
import com.skruk.elvis.admin.manage.marc21.Marc21Field;
import com.skruk.elvis.admin.plugin.Marc21Plugin;
import com.skruk.elvis.admin.registry.BooleanState;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 * This code was generated using CloudGarden's Jigloo
 * SWT/Swing GUI Builder, which is free for non-commercial
 * use. If Jigloo is being used commercially (ie, by a
 * for-profit company or business) then you should purchase
 * a license - please visit www.cloudgarden.com for details.
 *
 * @author     skruk
 * @created    5 kwiecień 2004
 */
public class BinaryMarcLoadPanel extends WizardPanel {

    /**  Description of the Field */
    private JPanel jPanel5;

    /**  Description of the Field */
    private JButton jbProcess;

    /**  Description of the Field */
    private JPanel jPanel4;

    /**  Description of the Field */
    private JButton jbSelectMarc;

    /**  Description of the Field */
    private JPanel jPanel3;

    /**  Description of the Field */
    private JTextPane jtpBinaryMarc;

    /**  Description of the Field */
    private JScrollPane jScrollPane1;

    /**  Description of the Field */
    private JPanel jPanel2;

    /**  Description of the Field */
    private JPanel jPanel1;

    /**  Description of the Field */
    private JLabel jlHead;

    /**  Description of the Field */
    private JFileChooser jfcBinMarc = new JFileChooser();

    /**  Description of the Field */
    static ResourceFormatter formater = null;

    /**  Description of the Field */
    private static final int TEXT_WIDTH = 48;

    /**  Description of the Field */
    private byte[] bmarc = null;

    /**  Description of the Field */
    private String marc = null;

    /**  Description of the Field */
    private String marcxml = null;

    /**  Description of the Field */
    private BooleanState processed = new BooleanState(false);

    /**Constructor for the BinaryMarcLoadPanel object */
    public BinaryMarcLoadPanel() {
        synchronized (BinaryMarcLoadPanel.class) {
            if (formater == null) {
                formater = Marc21Plugin.getInstance().getFormater();
            }
        }
        initGUI();
        xInitComponents();
        this.initStylesForTextPane(this.jtpBinaryMarc);
    }

    /**  Description of the Method */
    protected void xInitComponents() {
        this.jlHead.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        this.jlHead.setText(formater.getText("import_marc21_upload_title"));
        this.jbProcess.setText(formater.getText("import_marc21_process_text"));
        this.jbProcess.setIcon(formater.getIcon("import_marc21_process_icon", 24, ""));
        this.jbSelectMarc.setText(formater.getText("import_marc21_select_file_text"));
        this.jbSelectMarc.setIcon(formater.getIcon("import_marc21_select_file_icon", 24, ""));
    }

    /**
	 * Initializes the GUI.
	 * Auto-generated code - any changes you make will disappear.
	 */
    public void initGUI() {
        try {
            preInitGUI();
            jPanel2 = new JPanel();
            jPanel4 = new JPanel();
            jbSelectMarc = new JButton();
            jbProcess = new JButton();
            jPanel5 = new JPanel();
            jPanel1 = new JPanel();
            jPanel3 = new JPanel();
            jScrollPane1 = new JScrollPane();
            jtpBinaryMarc = new JTextPane();
            jlHead = new JLabel();
            BorderLayout thisLayout = new BorderLayout();
            this.setLayout(thisLayout);
            thisLayout.setHgap(0);
            thisLayout.setVgap(0);
            this.setPreferredSize(new java.awt.Dimension(406, 211));
            this.setMinimumSize(new java.awt.Dimension(100, 10));
            BorderLayout jPanel2Layout = new BorderLayout();
            jPanel2.setLayout(jPanel2Layout);
            jPanel2Layout.setHgap(0);
            jPanel2Layout.setVgap(0);
            this.add(jPanel2, BorderLayout.SOUTH);
            FlowLayout jPanel4Layout = new FlowLayout();
            jPanel4.setLayout(jPanel4Layout);
            jPanel4Layout.setAlignment(FlowLayout.CENTER);
            jPanel4Layout.setHgap(5);
            jPanel4Layout.setVgap(5);
            jPanel2.add(jPanel4, BorderLayout.WEST);
            jbSelectMarc.setText("SelectFile");
            jPanel4.add(jbSelectMarc);
            jbSelectMarc.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    jbSelectMarcActionPerformed(evt);
                }
            });
            jbProcess.setText("Process");
            jPanel4.add(jbProcess);
            jbProcess.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    jbProcessActionPerformed(evt);
                }
            });
            BorderLayout jPanel5Layout = new BorderLayout();
            jPanel5.setLayout(jPanel5Layout);
            jPanel5Layout.setHgap(0);
            jPanel5Layout.setVgap(0);
            jPanel5.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
            jPanel2.add(jPanel5, BorderLayout.CENTER);
            BorderLayout jPanel1Layout = new BorderLayout();
            jPanel1.setLayout(jPanel1Layout);
            jPanel1Layout.setHgap(0);
            jPanel1Layout.setVgap(0);
            jPanel1.setPreferredSize(new java.awt.Dimension(60, 20));
            this.add(jPanel1, BorderLayout.CENTER);
            BorderLayout jPanel3Layout = new BorderLayout();
            jPanel3.setLayout(jPanel3Layout);
            jPanel3Layout.setHgap(0);
            jPanel3Layout.setVgap(0);
            jPanel3.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
            jPanel1.add(jPanel3, BorderLayout.CENTER);
            jPanel3.add(jScrollPane1, BorderLayout.CENTER);
            jtpBinaryMarc.setFont(new java.awt.Font("Monospaced", 0, 14));
            jScrollPane1.add(jtpBinaryMarc);
            jScrollPane1.setViewportView(jtpBinaryMarc);
            jlHead.setLayout(null);
            jlHead.setText("Head");
            jlHead.setHorizontalAlignment(SwingConstants.CENTER);
            jlHead.setHorizontalTextPosition(SwingConstants.CENTER);
            jlHead.setFont(new java.awt.Font("Dialog", 1, 18));
            jlHead.setPreferredSize(new java.awt.Dimension(584, 30));
            jlHead.setMinimumSize(new java.awt.Dimension(0, 30));
            jlHead.setMaximumSize(new java.awt.Dimension(1000, 30));
            this.add(jlHead, BorderLayout.NORTH);
            postInitGUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Add your pre-init code in here */
    public void preInitGUI() {
    }

    /** Add your post-init code in here */
    public void postInitGUI() {
    }

    /**
	 * Auto-generated main method
	 *
	 * @param  args  The command line arguments
	 */
    public static void main(String[] args) {
        showGUI();
    }

    /**
	 * Auto-generated code - any changes you make will disappear!!!
	 * This static method creates a new instance of this class and shows
	 * it inside a new JFrame, (unless it is already a JFrame).
	 */
    public static void showGUI() {
        try {
            javax.swing.JFrame frame = new javax.swing.JFrame();
            BinaryMarcLoadPanel inst = new BinaryMarcLoadPanel();
            frame.setContentPane(inst);
            frame.getContentPane().setSize(inst.getSize());
            frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 *  Description of the Method
	 *
	 * @param  sb  Description of the Parameter
	 * @return     Description of the Return Value
	 */
    public boolean canProceed(StringBuffer sb) {
        if (this.processed.getState()) {
            updateContext();
        } else {
            sb.append(formater.getText("import_marc21_proceed_error"));
        }
        return this.processed.getState();
    }

    /**
	 *  Description of the Method
	 *
	 * @param  sb  Description of the Parameter
	 * @return     Description of the Return Value
	 */
    public boolean canRetreat(StringBuffer sb) {
        return false;
    }

    /**
	 *  Description of the Method
	 *
	 * @param  sb  Description of the Parameter
	 * @return     Description of the Return Value
	 */
    public boolean canFinish(StringBuffer sb) {
        return false;
    }

    /**  Description of the Method */
    protected void updateContext() {
        this.context.addString("resource_marc", this.marc);
        this.context.addString("resource_marcxml", this.marcxml);
    }

    /**  Description of the Method */
    public void notifyEnter() {
    }

    /**
	 * Auto-generated event handler method
	 *
	 * @param  evt  Description of the Parameter
	 */
    protected void jbSelectMarcActionPerformed(ActionEvent evt) {
        if (jfcBinMarc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = jfcBinMarc.getSelectedFile();
                FileInputStream fis = new FileInputStream(f);
                FileChannel fch = fis.getChannel();
                ByteArrayOutputStream baos = new ByteArrayOutputStream((int) f.length());
                WritableByteChannel wbc = Channels.newChannel(baos);
                long pos = 0;
                long cnt = 0;
                while ((cnt = fch.transferTo(pos, f.length(), wbc)) > 0) {
                    pos += cnt;
                }
                fis.close();
                this.bmarc = baos.toByteArray();
                int lines = (int) Math.ceil((double) bmarc.length / TEXT_WIDTH);
                StringBuffer sb = new StringBuffer();
                System.out.println(bmarc.length);
                for (int i = 0; i < lines; i++) {
                    sb.append(new String(bmarc, i * TEXT_WIDTH, ((i + 1) * TEXT_WIDTH < bmarc.length) ? (TEXT_WIDTH) : (bmarc.length - i * TEXT_WIDTH), "US-ASCII"));
                    if (i < lines) {
                        sb.append("\n");
                    }
                }
                this.jtpBinaryMarc.setText(sb.toString());
            } catch (FileNotFoundException fnfex) {
            } catch (IOException ioex) {
            }
        }
    }

    /**
	 *  Description of the Method
	 *
	 * @exception  BadLocationException  Description of the Exception
	 */
    protected void processDocument() throws BadLocationException {
        Marc21Description description = new Marc21Description();
        description.setLeader(this.extract(0, 24, "bold"));
        description.setLength(Integer.valueOf(this.extract(0, 5, "green")).intValue());
        description.setDataStart(Integer.valueOf(this.extract(12, 5, "green")).intValue());
        int dstart = description.getDataStart();
        int i = 24;
        int shift = 2;
        while (i < dstart) {
            Marc21Field field;
            int tag = Integer.valueOf(this.extract(i, 3, "red")).intValue();
            if (tag < 10) {
                field = new Marc21Controlfield();
            } else {
                field = new Marc21Datafield();
            }
            field.setTag(tag);
            field.setLength(Integer.valueOf(this.extract(i + 3, 4, null)).intValue());
            field.setStart(Integer.valueOf(this.extract(i + 7, 5, null)).intValue());
            description.addField(field);
            i += 12;
            if (++shift == 4) {
                shift = 0;
                i++;
            }
        }
        Iterator it = description.fieldsIterator();
        while (it.hasNext()) {
            Marc21Field f = (Marc21Field) it.next();
            int start = f.getStart();
            int length = f.getLength();
            int shft = (int) Math.floor((double) (start + dstart) / TEXT_WIDTH);
            try {
                if (this.bmarc[dstart + start + length - 1] == '') {
                    f.setText(new String(this.bmarc, dstart + start, length - 1, "UTF-8"));
                } else {
                    f.setText(new String(this.bmarc, dstart + start, length, "UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            this.extract(dstart + start + shft, 1, "blue");
            if (f instanceof Marc21Datafield) {
                Marc21Datafield df = (Marc21Datafield) f;
                String ptr = new String(this.bmarc, dstart + start, 2);
                df.setPtr1(ptr.charAt(0));
                df.setPtr2(ptr.charAt(1));
                String[] sbfields = f.getText().split("");
                for (int j = 1; j < sbfields.length; j++) {
                    char key = sbfields[j].charAt(0);
                    int len = sbfields[j].length();
                    df.putSubfield(key, sbfields[j].substring(1, len));
                }
            }
        }
        this.marc = description.createMarc();
        this.marcxml = description.createXml(this.context.getString("resource_id"));
        this.processed.setTrue();
        updateContext();
    }

    /**
	 * Auto-generated event handler method
	 *
	 * @param  evt  Description of the Parameter
	 */
    protected void jbProcessActionPerformed(ActionEvent evt) {
        try {
            this.processDocument();
        } catch (BadLocationException ble) {
        }
    }

    /**
	 *  Description of the Method
	 *
	 * @param  textPane  Description of the Parameter
	 */
    protected void initStylesForTextPane(JTextPane textPane) {
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        Style regular = textPane.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "SansSerif");
        Style s = textPane.addStyle("bold", regular);
        StyleConstants.setBold(s, true);
        s = textPane.addStyle("green", regular);
        StyleConstants.setBold(s, true);
        StyleConstants.setForeground(s, Color.GREEN);
        s = textPane.addStyle("red", regular);
        StyleConstants.setForeground(s, Color.RED);
        s = textPane.addStyle("blue", regular);
        StyleConstants.setBold(s, true);
        StyleConstants.setForeground(s, Color.BLUE);
    }

    /**
	 *  Description of the Method
	 *
	 * @param  offset                    Description of the Parameter
	 * @param  length                    Description of the Parameter
	 * @param  style                     Description of the Parameter
	 * @return                           Description of the Return Value
	 * @exception  BadLocationException  Description of the Exception
	 */
    protected String extract(int offset, int length, String style) throws BadLocationException {
        String result = this.jtpBinaryMarc.getText(offset, length);
        if (style != null && !"regular".equals(style)) {
            this.jtpBinaryMarc.select(offset, offset + length);
            this.jtpBinaryMarc.setCharacterAttributes(this.jtpBinaryMarc.getStyle(style), true);
        }
        System.out.println(result);
        return result;
    }
}
