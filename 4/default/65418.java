import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.Cursor;

public class Split extends JFrame implements ActionListener, WindowListener, Runnable {

    private static final long serialVersionUID = 5207061841592574954L;

    public static final int SPLIT = 0;

    public static final int MERGE = 1;

    public int type = SPLIT;

    JDialog dlg = null;

    JButton btnWait = new JButton("Abort");

    JProgressBar progBarPart = null;

    JProgressBar progBar = null;

    JLabel progMsg = null;

    boolean abort = false;

    String mergeFile = "";

    private JPanel panMain = null;

    private JTabbedPane tpnMain = null;

    private JPanel panSplit = null;

    private JPanel panMerge = null;

    private JLabel lblSFile = null;

    private JTextField txtSFile = null;

    private JButton btnSFile = null;

    private JTextField txtSBytes = null;

    private JLabel lblSHelpBytes = null;

    private JPanel panSHelp = null;

    private JTextField txtSHelp = null;

    private JLabel lblSHelpBytes2 = null;

    private JButton btnSHelp = null;

    private JButton btnSplit = null;

    private JLabel lblSMsg = null;

    private JComboBox cmbPriority = null;

    private JLabel lblMFile = null;

    private JTextField txtMFile = null;

    private JButton btnMFile = null;

    private JLabel lblMMsg = null;

    private JButton btnMerge = null;

    private JLabel lblMMsg2 = null;

    private JLabel lblSPriority = null;

    private JComboBox cmbSBytes = null;

    private JButton btnLicense = null;

    /**
	 * This method initializes tpnMain	
	 * 	
	 * @return javax.swing.JTabbedPane	
	 */
    private JTabbedPane getTpnMain() {
        if (tpnMain == null) {
            tpnMain = new JTabbedPane();
            tpnMain.addTab("Split", null, getPanSplit(), null);
            tpnMain.addTab("Merge", null, getPanMerge(), null);
        }
        return tpnMain;
    }

    /**
	 * This method initializes panSplit	
	 * 	
	 * @return javax.swing.JPanel	
	 */
    private JPanel getPanSplit() {
        if (panSplit == null) {
            GridBagConstraints gridBagConstraints15 = new GridBagConstraints();
            gridBagConstraints15.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints15.gridy = 2;
            gridBagConstraints15.weightx = 1.0;
            gridBagConstraints15.ipady = -5;
            gridBagConstraints15.insets = new Insets(0, 5, 5, 5);
            gridBagConstraints15.anchor = GridBagConstraints.WEST;
            gridBagConstraints15.gridx = 0;
            GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
            gridBagConstraints13.gridx = 1;
            gridBagConstraints13.insets = new Insets(0, 0, 5, 5);
            gridBagConstraints13.gridwidth = 2;
            gridBagConstraints13.gridy = 1;
            lblSMsg = new JLabel();
            lblSMsg.setText("Output file will be saved in same path with \".jfs###\" as suffix.");
            lblSMsg.setFont(new Font("Dialog", Font.PLAIN, 12));
            GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
            gridBagConstraints12.gridx = 0;
            gridBagConstraints12.insets = new Insets(15, 5, 5, 5);
            gridBagConstraints12.fill = GridBagConstraints.BOTH;
            gridBagConstraints12.gridwidth = 3;
            gridBagConstraints12.gridy = 6;
            GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
            gridBagConstraints8.gridx = 1;
            gridBagConstraints8.fill = GridBagConstraints.VERTICAL;
            gridBagConstraints8.insets = new Insets(0, 0, 5, 5);
            gridBagConstraints8.anchor = GridBagConstraints.WEST;
            gridBagConstraints8.gridy = 4;
            lblSHelpBytes = new JLabel();
            lblSHelpBytes.setText("I want to split file in");
            GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            gridBagConstraints6.gridy = 2;
            gridBagConstraints6.weightx = 1.0;
            gridBagConstraints6.anchor = GridBagConstraints.WEST;
            gridBagConstraints6.insets = new Insets(0, 0, 5, 5);
            gridBagConstraints6.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints6.gridwidth = 2;
            gridBagConstraints6.gridx = 1;
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.gridx = 2;
            gridBagConstraints4.insets = new Insets(5, 0, 5, 5);
            gridBagConstraints4.gridy = 0;
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints3.gridy = 0;
            gridBagConstraints3.weightx = 1.0;
            gridBagConstraints3.insets = new Insets(5, 0, 5, 5);
            gridBagConstraints3.gridx = 1;
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.gridx = 0;
            gridBagConstraints2.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints2.anchor = GridBagConstraints.EAST;
            gridBagConstraints2.gridy = 0;
            lblSFile = new JLabel();
            lblSFile.setText("File to split:");
            panSplit = new JPanel();
            panSplit.setLayout(new GridBagLayout());
            panSplit.add(lblSFile, gridBagConstraints2);
            panSplit.add(getTxtSFile(), gridBagConstraints3);
            panSplit.add(getBtnSFile(), gridBagConstraints4);
            panSplit.add(getTxtSBytes(), gridBagConstraints6);
            panSplit.add(getPanSHelp(), gridBagConstraints8);
            panSplit.add(getBtnSplit(), gridBagConstraints12);
            panSplit.add(lblSMsg, gridBagConstraints13);
            panSplit.add(getCmbSBytes(), gridBagConstraints15);
        }
        return panSplit;
    }

    /**
	 * This method initializes panMerge	
	 * 	
	 * @return javax.swing.JPanel	
	 */
    private JPanel getPanMerge() {
        if (panMerge == null) {
            GridBagConstraints gridBagConstraints20 = new GridBagConstraints();
            gridBagConstraints20.gridx = 1;
            gridBagConstraints20.insets = new Insets(0, 0, 5, 5);
            gridBagConstraints20.gridwidth = 2;
            gridBagConstraints20.gridy = 3;
            lblMMsg2 = new JLabel();
            lblMMsg2.setFont(new Font("Dialog", Font.PLAIN, 12));
            lblMMsg2.setText("All file \".jsf###\" must be in same directory!");
            GridBagConstraints gridBagConstraints19 = new GridBagConstraints();
            gridBagConstraints19.gridx = 0;
            gridBagConstraints19.insets = new Insets(15, 5, 5, 5);
            gridBagConstraints19.gridwidth = 3;
            gridBagConstraints19.fill = GridBagConstraints.BOTH;
            gridBagConstraints19.gridy = 4;
            GridBagConstraints gridBagConstraints18 = new GridBagConstraints();
            gridBagConstraints18.gridx = 1;
            gridBagConstraints18.insets = new Insets(0, 0, 5, 5);
            gridBagConstraints18.gridwidth = 2;
            gridBagConstraints18.gridy = 2;
            lblMMsg = new JLabel();
            lblMMsg.setFont(new Font("Dialog", Font.PLAIN, 12));
            lblMMsg.setText("Specify file \".jsf000\". Output file will be saved in same path.");
            GridBagConstraints gridBagConstraints17 = new GridBagConstraints();
            gridBagConstraints17.gridx = 2;
            gridBagConstraints17.insets = new Insets(5, 0, 5, 5);
            gridBagConstraints17.gridy = 0;
            GridBagConstraints gridBagConstraints16 = new GridBagConstraints();
            gridBagConstraints16.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints16.gridy = 0;
            gridBagConstraints16.weightx = 1.0;
            gridBagConstraints16.insets = new Insets(5, 0, 5, 5);
            gridBagConstraints16.gridx = 1;
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.gridx = 0;
            gridBagConstraints1.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints1.gridy = 0;
            lblMFile = new JLabel();
            lblMFile.setText("File to merge:");
            panMerge = new JPanel();
            panMerge.setLayout(new GridBagLayout());
            panMerge.add(lblMFile, gridBagConstraints1);
            panMerge.add(getTxtMFile(), gridBagConstraints16);
            panMerge.add(getBtnMFile(), gridBagConstraints17);
            panMerge.add(lblMMsg, gridBagConstraints18);
            panMerge.add(getBtnMerge(), gridBagConstraints19);
            panMerge.add(lblMMsg2, gridBagConstraints20);
        }
        return panMerge;
    }

    /**
	 * This method initializes txtSFile	
	 * 	
	 * @return javax.swing.JTextField	
	 */
    private JTextField getTxtSFile() {
        if (txtSFile == null) {
            txtSFile = new JTextField();
        }
        return txtSFile;
    }

    /**
	 * This method initializes btnSFile	
	 * 	
	 * @return javax.swing.JButton	
	 */
    private JButton getBtnSFile() {
        if (btnSFile == null) {
            btnSFile = new JButton();
            btnSFile.setText("...");
            btnSFile.setPreferredSize(new Dimension(22, 22));
            btnSFile.setMargin(new Insets(0, 0, 0, 0));
            btnSFile.addActionListener(this);
        }
        return btnSFile;
    }

    /**
	 * This method initializes txtSBytes	
	 * 	
	 * @return javax.swing.JTextField	
	 */
    private JTextField getTxtSBytes() {
        if (txtSBytes == null) {
            txtSBytes = new JTextField();
        }
        return txtSBytes;
    }

    /**
	 * This method initializes panSHelp	
	 * 	
	 * @return javax.swing.JPanel	
	 */
    private JPanel getPanSHelp() {
        if (panSHelp == null) {
            GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            gridBagConstraints11.gridx = 0;
            gridBagConstraints11.insets = new Insets(0, 0, 0, 5);
            gridBagConstraints11.gridy = 0;
            GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
            gridBagConstraints10.gridx = 3;
            gridBagConstraints10.insets = new Insets(0, 0, 0, 5);
            gridBagConstraints10.gridy = 0;
            lblSHelpBytes2 = new JLabel();
            lblSHelpBytes2.setText("parts...");
            GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
            gridBagConstraints9.gridy = 0;
            gridBagConstraints9.insets = new Insets(0, 5, 0, 5);
            gridBagConstraints9.gridx = 2;
            GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
            gridBagConstraints7.gridx = 1;
            gridBagConstraints7.gridy = 0;
            panSHelp = new JPanel();
            panSHelp.setLayout(new GridBagLayout());
            panSHelp.add(lblSHelpBytes, gridBagConstraints7);
            panSHelp.add(getTxtSHelp(), gridBagConstraints9);
            panSHelp.add(lblSHelpBytes2, gridBagConstraints10);
            panSHelp.add(getBtnSHelp(), gridBagConstraints11);
        }
        return panSHelp;
    }

    /**
	 * This method initializes txtSHelp	
	 * 	
	 * @return javax.swing.JTextField	
	 */
    private JTextField getTxtSHelp() {
        if (txtSHelp == null) {
            txtSHelp = new JTextField();
            txtSHelp.setColumns(2);
        }
        return txtSHelp;
    }

    /**
	 * This method initializes btnSHelp	
	 * 	
	 * @return javax.swing.JButton	
	 */
    private JButton getBtnSHelp() {
        if (btnSHelp == null) {
            btnSHelp = new JButton();
            btnSHelp.setText("^");
            btnSHelp.setPreferredSize(new Dimension(22, 22));
            btnSHelp.setMargin(new Insets(0, 0, 0, 0));
            btnSHelp.addActionListener(this);
        }
        return btnSHelp;
    }

    /**
	 * This method initializes btnSplit	
	 * 	
	 * @return javax.swing.JButton	
	 */
    private JButton getBtnSplit() {
        if (btnSplit == null) {
            btnSplit = new JButton();
            btnSplit.setText("Split!");
            btnSplit.setFont(new Font("Dialog", Font.BOLD, 24));
            btnSplit.addActionListener(this);
        }
        return btnSplit;
    }

    /**
	 * This method initializes cmbPriority	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
    private JComboBox getCmbPriority() {
        if (cmbPriority == null) {
            cmbPriority = new JComboBox();
            cmbPriority.setModel(new DefaultComboBoxModel());
            cmbPriority.addItem("Minimum priority");
            cmbPriority.addItem("Normal priority");
            cmbPriority.addItem("Maximum priority");
            cmbPriority.setSelectedIndex(1);
        }
        return cmbPriority;
    }

    /**
	 * This method initializes txtMFile	
	 * 	
	 * @return javax.swing.JTextField	
	 */
    private JTextField getTxtMFile() {
        if (txtMFile == null) {
            txtMFile = new JTextField();
        }
        return txtMFile;
    }

    /**
	 * This method initializes btnMFile	
	 * 	
	 * @return javax.swing.JButton	
	 */
    private JButton getBtnMFile() {
        if (btnMFile == null) {
            btnMFile = new JButton();
            btnMFile.setText("...");
            btnMFile.setPreferredSize(new Dimension(22, 22));
            btnMFile.setMargin(new Insets(0, 0, 0, 0));
            btnMFile.addActionListener(this);
        }
        return btnMFile;
    }

    /**
	 * This method initializes btnMerge	
	 * 	
	 * @return javax.swing.JButton	
	 */
    private JButton getBtnMerge() {
        if (btnMerge == null) {
            btnMerge = new JButton();
            btnMerge.setFont(new Font("Dialog", Font.BOLD, 24));
            btnMerge.setText("Merge");
            btnMerge.addActionListener(this);
        }
        return btnMerge;
    }

    /**
	 * This method initializes cmbSBytes	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
    private JComboBox getCmbSBytes() {
        if (cmbSBytes == null) {
            cmbSBytes = new JComboBox();
            cmbSBytes.addItem("bytes");
            cmbSBytes.addItem("kilobytes");
            cmbSBytes.addItem("megabytes");
            cmbSBytes.addItem("gigabytes");
            cmbSBytes.setSelectedIndex(0);
        }
        return cmbSBytes;
    }

    long getBytes() {
        try {
            if (txtSHelp.getText().trim().length() > 0 && txtSBytes.getText().trim().length() < 1) {
                btnSHelp.doClick();
            }
            long bytes = Long.parseLong(txtSBytes.getText());
            switch(cmbSBytes.getSelectedIndex()) {
                case 3:
                    bytes *= 1024;
                case 2:
                    bytes *= 1024;
                case 1:
                    bytes *= 1024;
                default:
                    break;
            }
            return bytes;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
	 * This method initializes btnLicense	
	 * 	
	 * @return javax.swing.JButton	
	 */
    private JButton getBtnLicense() {
        if (btnLicense == null) {
            btnLicense = new JButton();
            btnLicense.setText("Read license");
            btnLicense.setMargin(new Insets(0, 3, 0, 3));
            btnLicense.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            btnLicense.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    JDialog dlgLicense = new JDialog(Split.this, "PJFileSplitter license", true);
                    dlgLicense.getContentPane().setLayout(new GridLayout());
                    JTextArea txt = new JTextArea();
                    txt.setText(getGPLText());
                    txt.setEditable(false);
                    txt.setFont(new Font("Monospaced", Font.BOLD, 12));
                    txt.setCaretPosition(0);
                    txt.setBorder(new EmptyBorder(5, 5, 5, 5));
                    dlgLicense.getContentPane().add(new JScrollPane(txt));
                    dlgLicense.pack();
                    dlgLicense.setSize(dlgLicense.getSize().width + 20, 400);
                    center(dlgLicense);
                    dlgLicense.setVisible(true);
                }
            });
        }
        return btnLicense;
    }

    private String getGPLText() {
        return "           GNU GENERAL PUBLIC LICENSE\n" + "              Version 2, June 1991\n" + "\n" + " Copyright (C) 1989, 1991 Free Software Foundation, Inc.,\n" + " 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA\n" + " Everyone is permitted to copy and distribute verbatim copies\n" + " of this license document, but changing it is not allowed.\n" + "\n" + "               Preamble\n" + "\n" + "  The licenses for most software are designed to take away your\n" + "freedom to share and change it.  By contrast, the GNU General Public\n" + "License is intended to guarantee your freedom to share and change free\n" + "software--to make sure the software is free for all its users.  This\n" + "General Public License applies to most of the Free Software\n" + "Foundation's software and to any other program whose authors commit to\n" + "using it.  (Some other Free Software Foundation software is covered by\n" + "the GNU Lesser General Public License instead.)  You can apply it to\n" + "your programs, too.\n" + "\n" + "  When we speak of free software, we are referring to freedom, not\n" + "price.  Our General Public Licenses are designed to make sure that you\n" + "have the freedom to distribute copies of free software (and charge for\n" + "this service if you wish), that you receive source code or can get it\n" + "if you want it, that you can change the software or use pieces of it\n" + "in new free programs; and that you know you can do these things.\n" + "\n" + "  To protect your rights, we need to make restrictions that forbid\n" + "anyone to deny you these rights or to ask you to surrender the rights.\n" + "These restrictions translate to certain responsibilities for you if you\n" + "distribute copies of the software, or if you modify it.\n" + "\n" + "  For example, if you distribute copies of such a program, whether\n" + "gratis or for a fee, you must give the recipients all the rights that\n" + "you have.  You must make sure that they, too, receive or can get the\n" + "source code.  And you must show them these terms so they know their\n" + "rights.\n" + "\n" + "  We protect your rights with two steps: (1) copyright the software, and\n" + "(2) offer you this license which gives you legal permission to copy,\n" + "distribute and/or modify the software.\n" + "\n" + "  Also, for each author's protection and ours, we want to make certain\n" + "that everyone understands that there is no warranty for this free\n" + "software.  If the software is modified by someone else and passed on, we\n" + "want its recipients to know that what they have is not the original, so\n" + "that any problems introduced by others will not reflect on the original\n" + "authors' reputations.\n" + "\n" + "  Finally, any free program is threatened constantly by software\n" + "patents.  We wish to avoid the danger that redistributors of a free\n" + "program will individually obtain patent licenses, in effect making the\n" + "program proprietary.  To prevent this, we have made it clear that any\n" + "patent must be licensed for everyone's free use or not licensed at all.\n" + "\n" + "  The precise terms and conditions for copying, distribution and\n" + "modification follow.\n" + "\n" + "           GNU GENERAL PUBLIC LICENSE\n" + "   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION\n" + "\n" + "  0. This License applies to any program or other work which contains\n" + "a notice placed by the copyright holder saying it may be distributed\n" + "under the terms of this General Public License.  The \"Program\", below,\n" + "refers to any such program or work, and a \"work based on the Program\"\n" + "means either the Program or any derivative work under copyright law:\n" + "that is to say, a work containing the Program or a portion of it,\n" + "either verbatim or with modifications and/or translated into another\n" + "language.  (Hereinafter, translation is included without limitation in\n" + "the term \"modification\".)  Each licensee is addressed as \"you\".\n" + "\n" + "Activities other than copying, distribution and modification are not\n" + "covered by this License; they are outside its scope.  The act of\n" + "running the Program is not restricted, and the output from the Program\n" + "is covered only if its contents constitute a work based on the\n" + "Program (independent of having been made by running the Program).\n" + "Whether that is true depends on what the Program does.\n" + "\n" + "  1. You may copy and distribute verbatim copies of the Program's\n" + "source code as you receive it, in any medium, provided that you\n" + "conspicuously and appropriately publish on each copy an appropriate\n" + "copyright notice and disclaimer of warranty; keep intact all the\n" + "notices that refer to this License and to the absence of any warranty;\n" + "and give any other recipients of the Program a copy of this License\n" + "along with the Program.\n" + "\n" + "You may charge a fee for the physical act of transferring a copy, and\n" + "you may at your option offer warranty protection in exchange for a fee.\n" + "\n" + "  2. You may modify your copy or copies of the Program or any portion\n" + "of it, thus forming a work based on the Program, and copy and\n" + "distribute such modifications or work under the terms of Section 1\n" + "above, provided that you also meet all of these conditions:\n" + "\n" + "    a) You must cause the modified files to carry prominent notices\n" + "    stating that you changed the files and the date of any change.\n" + "\n" + "    b) You must cause any work that you distribute or publish, that in\n" + "    whole or in part contains or is derived from the Program or any\n" + "    part thereof, to be licensed as a whole at no charge to all third\n" + "    parties under the terms of this License.\n" + "\n" + "    c) If the modified program normally reads commands interactively\n" + "    when run, you must cause it, when started running for such\n" + "    interactive use in the most ordinary way, to print or display an\n" + "    announcement including an appropriate copyright notice and a\n" + "    notice that there is no warranty (or else, saying that you provide\n" + "    a warranty) and that users may redistribute the program under\n" + "    these conditions, and telling the user how to view a copy of this\n" + "    License.  (Exception: if the Program itself is interactive but\n" + "    does not normally print such an announcement, your work based on\n" + "    the Program is not required to print an announcement.)\n" + "\n" + "These requirements apply to the modified work as a whole.  If\n" + "identifiable sections of that work are not derived from the Program,\n" + "and can be reasonably considered independent and separate works in\n" + "themselves, then this License, and its terms, do not apply to those\n" + "sections when you distribute them as separate works.  But when you\n" + "distribute the same sections as part of a whole which is a work based\n" + "on the Program, the distribution of the whole must be on the terms of\n" + "this License, whose permissions for other licensees extend to the\n" + "entire whole, and thus to each and every part regardless of who wrote it.\n" + "\n" + "Thus, it is not the intent of this section to claim rights or contest\n" + "your rights to work written entirely by you; rather, the intent is to\n" + "exercise the right to control the distribution of derivative or\n" + "collective works based on the Program.\n" + "\n" + "In addition, mere aggregation of another work not based on the Program\n" + "with the Program (or with a work based on the Program) on a volume of\n" + "a storage or distribution medium does not bring the other work under\n" + "the scope of this License.\n" + "\n" + "  3. You may copy and distribute the Program (or a work based on it,\n" + "under Section 2) in object code or executable form under the terms of\n" + "Sections 1 and 2 above provided that you also do one of the following:\n" + "\n" + "    a) Accompany it with the complete corresponding machine-readable\n" + "    source code, which must be distributed under the terms of Sections\n" + "    1 and 2 above on a medium customarily used for software interchange; or,\n" + "\n" + "    b) Accompany it with a written offer, valid for at least three\n" + "    years, to give any third party, for a charge no more than your\n" + "    cost of physically performing source distribution, a complete\n" + "    machine-readable copy of the corresponding source code, to be\n" + "    distributed under the terms of Sections 1 and 2 above on a medium\n" + "    customarily used for software interchange; or,\n" + "\n" + "    c) Accompany it with the information you received as to the offer\n" + "    to distribute corresponding source code.  (This alternative is\n" + "    allowed only for noncommercial distribution and only if you\n" + "    received the program in object code or executable form with such\n" + "    an offer, in accord with Subsection b above.)\n" + "\n" + "The source code for a work means the preferred form of the work for\n" + "making modifications to it.  For an executable work, complete source\n" + "code means all the source code for all modules it contains, plus any\n" + "associated interface definition files, plus the scripts used to\n" + "control compilation and installation of the executable.  However, as a\n" + "special exception, the source code distributed need not include\n" + "anything that is normally distributed (in either source or binary\n" + "form) with the major components (compiler, kernel, and so on) of the\n" + "operating system on which the executable runs, unless that component\n" + "itself accompanies the executable.\n" + "\n" + "If distribution of executable or object code is made by offering\n" + "access to copy from a designated place, then offering equivalent\n" + "access to copy the source code from the same place counts as\n" + "distribution of the source code, even though third parties are not\n" + "compelled to copy the source along with the object code.\n" + "\n" + "  4. You may not copy, modify, sublicense, or distribute the Program\n" + "except as expressly provided under this License.  Any attempt\n" + "otherwise to copy, modify, sublicense or distribute the Program is\n" + "void, and will automatically terminate your rights under this License.\n" + "However, parties who have received copies, or rights, from you under\n" + "this License will not have their licenses terminated so long as such\n" + "parties remain in full compliance.\n" + "\n" + "  5. You are not required to accept this License, since you have not\n" + "signed it.  However, nothing else grants you permission to modify or\n" + "distribute the Program or its derivative works.  These actions are\n" + "prohibited by law if you do not accept this License.  Therefore, by\n" + "modifying or distributing the Program (or any work based on the\n" + "Program), you indicate your acceptance of this License to do so, and\n" + "all its terms and conditions for copying, distributing or modifying\n" + "the Program or works based on it.\n" + "\n" + "  6. Each time you redistribute the Program (or any work based on the\n" + "Program), the recipient automatically receives a license from the\n" + "original licensor to copy, distribute or modify the Program subject to\n" + "these terms and conditions.  You may not impose any further\n" + "restrictions on the recipients' exercise of the rights granted herein.\n" + "You are not responsible for enforcing compliance by third parties to\n" + "this License.\n" + "\n" + "  7. If, as a consequence of a court judgment or allegation of patent\n" + "infringement or for any other reason (not limited to patent issues),\n" + "conditions are imposed on you (whether by court order, agreement or\n" + "otherwise) that contradict the conditions of this License, they do not\n" + "excuse you from the conditions of this License.  If you cannot\n" + "distribute so as to satisfy simultaneously your obligations under this\n" + "License and any other pertinent obligations, then as a consequence you\n" + "may not distribute the Program at all.  For example, if a patent\n" + "license would not permit royalty-free redistribution of the Program by\n" + "all those who receive copies directly or indirectly through you, then\n" + "the only way you could satisfy both it and this License would be to\n" + "refrain entirely from distribution of the Program.\n" + "\n" + "If any portion of this section is held invalid or unenforceable under\n" + "any particular circumstance, the balance of the section is intended to\n" + "apply and the section as a whole is intended to apply in other\n" + "circumstances.\n" + "\n" + "It is not the purpose of this section to induce you to infringe any\n" + "patents or other property right claims or to contest validity of any\n" + "such claims; this section has the sole purpose of protecting the\n" + "integrity of the free software distribution system, which is\n" + "implemented by public license practices.  Many people have made\n" + "generous contributions to the wide range of software distributed\n" + "through that system in reliance on consistent application of that\n" + "system; it is up to the author/donor to decide if he or she is willing\n" + "to distribute software through any other system and a licensee cannot\n" + "impose that choice.\n" + "\n" + "This section is intended to make thoroughly clear what is believed to\n" + "be a consequence of the rest of this License.\n" + "\n" + "  8. If the distribution and/or use of the Program is restricted in\n" + "certain countries either by patents or by copyrighted interfaces, the\n" + "original copyright holder who places the Program under this License\n" + "may add an explicit geographical distribution limitation excluding\n" + "those countries, so that distribution is permitted only in or among\n" + "countries not thus excluded.  In such case, this License incorporates\n" + "the limitation as if written in the body of this License.\n" + "\n" + "  9. The Free Software Foundation may publish revised and/or new versions\n" + "of the General Public License from time to time.  Such new versions will\n" + "be similar in spirit to the present version, but may differ in detail to\n" + "address new problems or concerns.\n" + "\n" + "Each version is given a distinguishing version number.  If the Program\n" + "specifies a version number of this License which applies to it and \"any\n" + "later version\", you have the option of following the terms and conditions\n" + "either of that version or of any later version published by the Free\n" + "Software Foundation.  If the Program does not specify a version number of\n" + "this License, you may choose any version ever published by the Free Software\n" + "Foundation.\n" + "\n" + "  10. If you wish to incorporate parts of the Program into other free\n" + "programs whose distribution conditions are different, write to the author\n" + "to ask for permission.  For software which is copyrighted by the Free\n" + "Software Foundation, write to the Free Software Foundation; we sometimes\n" + "make exceptions for this.  Our decision will be guided by the two goals\n" + "of preserving the free status of all derivatives of our free software and\n" + "of promoting the sharing and reuse of software generally.\n" + "\n" + "               NO WARRANTY\n" + "\n" + "  11. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY\n" + "FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW.  EXCEPT WHEN\n" + "OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES\n" + "PROVIDE THE PROGRAM \"AS IS\" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED\n" + "OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF\n" + "MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE ENTIRE RISK AS\n" + "TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE\n" + "PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING,\n" + "REPAIR OR CORRECTION.\n" + "\n" + "  12. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING\n" + "WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR\n" + "REDISTRIBUTE THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES,\n" + "INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING\n" + "OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED\n" + "TO LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY\n" + "YOU OR THIRD PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER\n" + "PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE\n" + "POSSIBILITY OF SUCH DAMAGES.\n" + "\n" + "            END OF TERMS AND CONDITIONS\n" + "\n" + "       How to Apply These Terms to Your New Programs\n" + "\n" + "  If you develop a new program, and you want it to be of the greatest\n" + "possible use to the public, the best way to achieve this is to make it\n" + "free software which everyone can redistribute and change under these terms.\n" + "\n" + "  To do so, attach the following notices to the program.  It is safest\n" + "to attach them to the start of each source file to most effectively\n" + "convey the exclusion of warranty; and each file should have at least\n" + "the \"copyright\" line and a pointer to where the full notice is found.\n" + "\n" + "    <one line to give the program's name and a brief idea of what it does.>\n" + "    Copyright (C) <year>  <name of author>\n" + "\n" + "    This program is free software; you can redistribute it and/or modify\n" + "    it under the terms of the GNU General Public License as published by\n" + "    the Free Software Foundation; either version 2 of the License, or\n" + "    (at your option) any later version.\n" + "\n" + "    This program is distributed in the hope that it will be useful,\n" + "    but WITHOUT ANY WARRANTY; without even the implied warranty of\n" + "    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" + "    GNU General Public License for more details.\n" + "\n" + "    You should have received a copy of the GNU General Public License along\n" + "    with this program; if not, write to the Free Software Foundation, Inc.,\n" + "    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.\n" + "\n" + "Also add information on how to contact you by electronic and paper mail.\n" + "\n" + "If the program is interactive, make it output a short notice like this\n" + "when it starts in an interactive mode:\n" + "\n" + "    Gnomovision version 69, Copyright (C) year name of author\n" + "    Gnomovision comes with ABSOLUTELY NO WARRANTY; for details type `show w'.\n" + "    This is free software, and you are welcome to redistribute it\n" + "    under certain conditions; type `show c' for details.\n" + "\n" + "The hypothetical commands `show w' and `show c' should show the appropriate\n" + "parts of the General Public License.  Of course, the commands you use may\n" + "be called something other than `show w' and `show c'; they could even be\n" + "mouse-clicks or menu items--whatever suits your program.\n" + "\n" + "You should also get your employer (if you work as a programmer) or your\n" + "school, if any, to sign a \"copyright disclaimer\" for the program, if\n" + "necessary.  Here is a sample; alter the names:\n" + "\n" + "  Yoyodyne, Inc., hereby disclaims all copyright interest in the program\n" + "  `Gnomovision' (which makes passes at compilers) written by James Hacker.\n" + "\n" + "  <signature of Ty Coon>, 1 April 1989\n" + "  Ty Coon, President of Vice\n" + "\n" + "This General Public License does not permit incorporating your program into\n" + "proprietary programs.  If your program is a subroutine library, you may\n" + "consider it more useful to permit linking proprietary applications with the\n" + "library.  If this is what you want to do, use the GNU Lesser General\n" + "Public License instead of this License.";
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                Split thisClass = new Split();
                thisClass.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                thisClass.setVisible(true);
            }
        });
    }

    /**
	 * This is the default constructor
	 */
    public Split() {
        super();
        initialize();
    }

    /**
	 * This method initializes this
	 * 
	 * @return void
	 */
    private void initialize() {
        this.setSize(482, 283);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(getPanMain());
        this.setTitle("PJFileSplitter - by PerezDeQueya80");
        this.addWindowListener(this);
        btnWait.addActionListener(this);
        txtSFile.requestFocus();
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("file_icon.png")));
    }

    public void setVisible(boolean b) {
        if (b) {
            center(this);
        }
        super.setVisible(b);
    }

    static void center(Window w) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = w.getSize();
        if (frameSize.height > screenSize.height) frameSize.height = screenSize.height;
        if (frameSize.width > screenSize.width) frameSize.width = screenSize.width;
        w.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    }

    public static String msToDescr(long time) {
        int _sec = 1000;
        int _min = _sec * 60;
        int _hour = _min * 60;
        int _day = _hour * 24;
        String sOut = "";
        long days = time / _day;
        if (days > 0) sOut += days + " day" + (days > 1 ? "s" : "");
        time = time - (days * _day);
        long hours = time / _hour;
        if (hours > 0) sOut += " " + hours + " hour" + (hours > 1 ? "s" : "");
        time = time - (hours * _hour);
        long mins = time / _min;
        if (mins > 0) sOut += " " + mins + " minute" + (mins > 1 ? "s" : "");
        time = time - (mins * _min);
        long sec = time / _sec;
        if (sec > 0) sOut += " " + sec + " second" + (sec > 1 ? "s" : "");
        time = time - (sec * _sec);
        long ms = time;
        return sOut;
    }

    /**
	 * This method initializes panMain
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getPanMain() {
        if (panMain == null) {
            GridBagConstraints gridBagConstraints110 = new GridBagConstraints();
            gridBagConstraints110.gridx = 2;
            gridBagConstraints110.insets = new Insets(0, 0, 5, 5);
            gridBagConstraints110.gridy = 1;
            GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
            gridBagConstraints21.fill = GridBagConstraints.BOTH;
            gridBagConstraints21.gridx = 1;
            gridBagConstraints21.gridy = 1;
            gridBagConstraints21.weightx = 1.0;
            gridBagConstraints21.ipady = -5;
            gridBagConstraints21.insets = new Insets(0, 0, 5, 5);
            GridBagConstraints gridBagConstraints14 = new GridBagConstraints();
            gridBagConstraints14.gridx = 0;
            gridBagConstraints14.insets = new Insets(0, 5, 5, 5);
            gridBagConstraints14.fill = GridBagConstraints.BOTH;
            gridBagConstraints14.gridy = 1;
            lblSPriority = new JLabel();
            lblSPriority.setText("Priority:");
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.gridx = 0;
            panMain = new JPanel();
            panMain.setLayout(new GridBagLayout());
            panMain.add(getTpnMain(), gridBagConstraints);
            panMain.add(lblSPriority, gridBagConstraints14);
            panMain.add(getCmbPriority(), gridBagConstraints21);
            panMain.add(getBtnLicense(), gridBagConstraints110);
        }
        return panMain;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSFile) {
            openFile(SPLIT);
        }
        if (e.getSource() == btnSHelp) {
            help();
        }
        if (e.getSource() == btnSplit) {
            startThread(SPLIT);
        }
        if (e.getSource() == btnWait) {
            abort();
        }
        if (e.getSource() == btnMFile) {
            openFile(MERGE);
        }
        if (e.getSource() == btnMerge) {
            startThread(MERGE);
        }
    }

    private void openFile(int type) {
        JFileChooser fc = new JFileChooser();
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (type == SPLIT) txtSFile.setText(file.getAbsolutePath()); else txtMFile.setText(file.getAbsolutePath());
        } else {
        }
    }

    private void help() {
        File f = new File(txtSFile.getText());
        if (!f.exists()) {
            JOptionPane.showMessageDialog(this, "Choose a correct file!", "Error", JOptionPane.ERROR_MESSAGE);
            txtSFile.requestFocus();
            return;
        }
        String parts = txtSHelp.getText();
        if (parts == null || parts.trim().length() < 1) {
            JOptionPane.showMessageDialog(this, "Specify number of parts!", "Error", JOptionPane.ERROR_MESSAGE);
            txtSHelp.requestFocus();
            return;
        } else {
            try {
                long l = f.length() / Long.parseLong(parts);
                l++;
                txtSBytes.setText("" + l);
                cmbSBytes.setSelectedIndex(0);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Specify a correct number!", "Error", JOptionPane.ERROR_MESSAGE);
                txtSHelp.requestFocus();
                txtSHelp.selectAll();
                return;
            }
        }
    }

    private void startThread(int type) {
        this.type = type;
        dlg = new JDialog(this, (type == SPLIT ? "Splitting" : "Merging") + "... please wait...", true);
        dlg.getContentPane().setLayout(new GridLayout(0, 1));
        dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progMsg = new JLabel();
        progMsg.setHorizontalTextPosition(JLabel.CENTER);
        progBarPart = new JProgressBar();
        progBarPart.setStringPainted(true);
        progBar = new JProgressBar();
        progBar.setStringPainted(true);
        dlg.getContentPane().add(progMsg);
        dlg.getContentPane().add(progBarPart);
        dlg.getContentPane().add(progBar);
        dlg.getContentPane().add(btnWait);
        dlg.pack();
        dlg.setSize(300, dlg.getSize().height);
        center(dlg);
        Thread t = new Thread(this);
        if (cmbPriority.getSelectedIndex() == 0) {
            t.setPriority(Thread.MIN_PRIORITY);
        }
        if (cmbPriority.getSelectedIndex() == 1) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        if (cmbPriority.getSelectedIndex() == 2) {
            t.setPriority(Thread.MAX_PRIORITY);
        }
        t.start();
        dlg.setVisible(true);
    }

    public void run() {
        try {
            if (type == SPLIT) split(); else merge();
        } finally {
            dlg.dispose();
            dlg = null;
        }
    }

    private void abort() {
        if (JOptionPane.showConfirmDialog(dlg, "Do you want to abort?", "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            abort = true;
        }
    }

    private void split() {
        try {
            long bytes = getBytes();
            if (bytes < 1) {
                throw new Exception("Specify a size to split file!");
            }
            split(txtSFile.getText(), txtSFile.getText(), bytes);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void merge() {
        try {
            mergeFile = txtMFile.getText();
            if (!mergeFile.endsWith(".jfs000")) {
                File f = new File(mergeFile + ".jfs000");
                if (!f.exists()) {
                    throw new Exception("Specify a file of \".jfs000\" type!");
                }
            } else {
                mergeFile = mergeFile.substring(0, mergeFile.length() - 7);
            }
            File f = new File(mergeFile);
            if (f.exists()) {
                if (JOptionPane.showConfirmDialog(dlg, "File " + mergeFile + " already exist. Overwrite it?", "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            merge(mergeFile, mergeFile);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    public void split(String sourceFilename, String destinationFilename, long bytes) throws java.io.IOException {
        progBarPart.setMinimum(0);
        progBarPart.setMaximum(long2int(bytes));
        progBar.setMinimum(0);
        progBar.setMaximum(long2int(new File(txtSFile.getText()).length()));
        abort = false;
        File source = new File(sourceFilename);
        java.io.BufferedInputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(source));
        long fileSize = source.length();
        int j = 0;
        File copied = new File(destinationFilename + ".jfs" + pad(j++));
        java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(new java.io.FileOutputStream(copied));
        try {
            int c;
            long i = 0;
            long i_t = 0;
            long oldTime = new Date().getTime();
            long oldSize = 0;
            long oldEta = 0;
            while ((c = in.read()) != -1 && !abort) {
                out.write(c);
                out.flush();
                i++;
                i_t++;
                if (i_t % 150079 == 0) {
                    String text = "   ETA: ";
                    long newTime = new Date().getTime();
                    long newSize = i_t;
                    long diffTime = newTime - oldTime;
                    long diffSize = newSize - oldSize;
                    if (diffTime != 0) {
                        long eta = ((fileSize - newSize) / diffSize) * diffTime;
                        text += msToDescr((eta + oldEta) / 2);
                        progMsg.setText(text);
                        oldEta = eta;
                    }
                    oldTime = newTime;
                    oldSize = newSize;
                }
                if (i_t % 20079 == 0) {
                    progBarPart.setValue(long2int(i));
                    progBarPart.setString(i + " / " + bytes);
                    progBar.setValue(long2int(i_t));
                    progBar.setString(i_t + " / " + fileSize);
                }
                if (i >= bytes) {
                    out.close();
                    copied = new File(destinationFilename + ".jfs" + pad(j++));
                    out = new java.io.BufferedOutputStream(new java.io.FileOutputStream(copied));
                    i = 0;
                }
            }
        } finally {
            in.close();
            out.close();
        }
    }

    public void merge(String sourceFilename, String destinationFilename) throws java.io.IOException {
        abort = false;
        int j = 0;
        int n_files = 0;
        long bytes = 0;
        long fileSizeTotal = 0;
        {
            File source;
            do {
                source = new File(sourceFilename + ".jfs" + pad(j++));
                if (source.exists()) {
                    fileSizeTotal += source.length();
                    n_files++;
                }
            } while (source != null && source.exists());
        }
        j = 0;
        File source = new File(sourceFilename + ".jfs" + pad(j++));
        java.io.BufferedInputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(source));
        File copied = new File(destinationFilename);
        java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(new java.io.FileOutputStream(copied));
        progBarPart.setMinimum(0);
        long fileSize = source.length();
        progBarPart.setMaximum(long2int(fileSize));
        progBar.setMinimum(0);
        progBar.setMaximum(long2int(fileSizeTotal));
        try {
            int c;
            long i = 0;
            long i_t = 0;
            long oldTime = new Date().getTime();
            long oldSize = 0;
            long oldEta = 0;
            while (source.exists() && !abort) {
                while ((c = in.read()) != -1 && !abort) {
                    i++;
                    i_t++;
                    if (i_t % 150079 == 0) {
                        String text = "   ETA: ";
                        long newTime = new Date().getTime();
                        long newSize = i_t;
                        long diffTime = newTime - oldTime;
                        long diffSize = newSize - oldSize;
                        if (diffTime != 0) {
                            long eta = ((fileSizeTotal - newSize) / diffSize) * diffTime;
                            text += msToDescr((eta + oldEta) / 2);
                            progMsg.setText(text);
                            oldEta = eta;
                        }
                        oldTime = newTime;
                        oldSize = newSize;
                    }
                    if (i_t % 20079 == 0) {
                        progBarPart.setValue(long2int(i));
                        progBarPart.setString(i + " / " + fileSize);
                        progBar.setValue(long2int(i_t));
                        progBar.setString(i_t + " / " + fileSizeTotal);
                    }
                    out.write(c);
                    out.flush();
                }
                if (!abort) {
                    in.close();
                    source = new File(sourceFilename + ".jfs" + pad(j++));
                    in = new java.io.BufferedInputStream(new java.io.FileInputStream(source));
                    fileSize = source.length();
                    progBarPart.setMaximum(long2int(fileSize));
                    i = 0;
                }
            }
        } catch (FileNotFoundException e) {
        } finally {
            in.close();
            out.close();
        }
    }

    static String pad(int what) {
        return pad("" + what, 3);
    }

    static String pad(String what, int len) {
        char[] chars = new char[len];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = '0';
        }
        String s = new String(chars);
        s += what;
        return s.substring(s.length() - chars.length);
    }

    int long2int(long l) {
        return (int) (l / 1000);
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
        txtSFile.requestFocus();
    }
}
