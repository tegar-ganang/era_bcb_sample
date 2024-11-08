package net.sf.borg.ui.address;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import net.sf.borg.common.Errmsg;
import net.sf.borg.common.PrefName;
import net.sf.borg.common.Resource;
import net.sf.borg.model.AddressModel;
import net.sf.borg.model.beans.Address;
import net.sf.borg.ui.DockableView;
import net.sf.borg.ui.ResourceHelper;
import net.sf.borg.ui.link.LinkPanel;
import net.sf.borg.ui.util.GridBagConstraintsFactory;
import com.toedter.calendar.JDateChooser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileFilter;
import net.sf.borg.common.Prefs;
import net.sf.borg.control.systemLogin.BorgNameLoginSystem;

/**
 * AddressView class
 *
 * AddressView contains functionality for a specific view in BORG which allows
 * the user to edit contacts.
 *
 * @author Mike Berger
 *
 * This class has been modified by UF Software Engineering Summer
2009
 * Developer: Darren Goldfarb, Drew Goldfarb, Dylan Moore
 */
public class AddressView extends DockableView {

    private Address addr_;

    private javax.swing.JButton addAnotherGroup;

    private LinkPanel attPanel;

    private JDateChooser bdchooser;

    private javax.swing.JTextField cntext;

    private javax.swing.JTextField cntext1;

    private javax.swing.JTextField comptext;

    private javax.swing.JTextField cttext;

    private javax.swing.JTextField cttext1;

    private javax.swing.JTextField emtext;

    private javax.swing.JTextField fntext;

    private javax.swing.JTextField fxtext;

    private javax.swing.JTextField hptext;

    private javax.swing.JButton jButton2;

    private javax.swing.JButton cancelButton;

    private javax.swing.JButton jButton_email;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel10;

    private javax.swing.JLabel jLabel11;

    private javax.swing.JLabel jLabel12;

    private javax.swing.JLabel jLabel13;

    private javax.swing.JLabel jLabel14;

    private javax.swing.JLabel jLabel15;

    private javax.swing.JLabel jLabel16;

    private javax.swing.JLabel jLabel17;

    private javax.swing.JLabel jLabel18;

    private javax.swing.JLabel jLabel19;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel20;

    private javax.swing.JLabel jLabel21;

    private javax.swing.JLabel jLabel22;

    private javax.swing.JButton jButton_picture;

    private javax.swing.JLabel jLabel124;

    private File picFile;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JLabel jLabel8;

    private javax.swing.JLabel jLabel9;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel5;

    private javax.swing.JPanel jPanel6;

    private javax.swing.JTabbedPane jTabbedPane1;

    private javax.swing.JTextField lntext;

    private javax.swing.JTextField nntext;

    private javax.swing.JTextArea notestext;

    private javax.swing.JTextField pgtext;

    private javax.swing.JTextField satext;

    private javax.swing.JTextField satext1;

    private javax.swing.JTextField sntext;

    private javax.swing.JTextField sttext;

    private javax.swing.JTextField sttext1;

    private javax.swing.JTextField wbtext;

    private javax.swing.JTextField wptext;

    private javax.swing.JTextField zctext;

    private javax.swing.JTextField zctext1;

    private ArrayList<javax.swing.JComboBox> groupComboBoxes;

    private ArrayList<javax.swing.JButton> groupRemoveButtons;

    private int gridBagYCount = 13;

    private ArrayList<GridBagConstraints> gridBagConstraints_groupComboBoxes;

    private ArrayList<GridBagConstraints> gridBagConstraints_groupRemoveButtons;

    private javax.swing.JLabel jLabel23;

    private javax.swing.JPanel jPanel_Checkboxes;

    private JCheckBox jCheckBox_Nickname;

    private JCheckBox jCheckBox_ScreenName;

    private JCheckBox jCheckBox_HomePhone;

    private JCheckBox jCheckBox_WorkPhone;

    private JCheckBox jCheckBox_Pager;

    private JCheckBox jCheckBox_Fax;

    private JCheckBox jCheckBox_Email;

    private JCheckBox jCheckBox_WebPage;

    private JCheckBox jCheckBox_Company;

    private JCheckBox jCheckBox_Birthday;

    private JCheckBox jCheckBox_Picture;

    private JPanel jPanel_pic = new JPanel();

    private JPanel jPanel_frame = new JPanel();

    public AddressView(Address addr) {
        super();
        addr_ = addr;
        addModel(AddressModel.getReference());
        initComponents();
        showaddr();
    }

    public PrefName getFrameSizePref() {
        return PrefName.ADDRVIEWSIZE;
    }

    public String getFrameTitle() {
        return Resource.getPlainResourceString("Address_Book_Entry");
    }

    private JPanel getJPanel6() {
        if (jPanel6 == null) {
            GridBagConstraints gridBagConstraints57 = new GridBagConstraints();
            jPanel6 = new JPanel();
            jPanel6.setLayout(new java.awt.GridBagLayout());
            gridBagConstraints57.gridx = 1;
            gridBagConstraints57.gridy = 0;
            gridBagConstraints57.gridwidth = 0;
            gridBagConstraints57.weightx = 1.0;
            gridBagConstraints57.weighty = 1.0;
            gridBagConstraints57.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints57.insets = new java.awt.Insets(4, 4, 4, 4);
            jPanel6.add(notestext, gridBagConstraints57);
        }
        return jPanel6;
    }

    public JMenuBar getMenuForFrame() {
        return null;
    }

    /**
	 * Copies a file from one location to another.
	 *
	 * Method created by UF Software Engineering, Summer 2009
	 * Developer: Dylan Moore
	 * @param filenameFrom
	 * @param filenameTo
	 */
    private void fileCopier(String filenameFrom, String filenameTo) {
        FileInputStream fromStream = null;
        FileOutputStream toStream = null;
        try {
            fromStream = new FileInputStream(new File(filenameFrom));
            if (new File(filenameTo).exists()) {
                new File(filenameTo).delete();
            }
            File dirr = new File(getContactPicPath());
            if (!dirr.exists()) {
                dirr.mkdir();
            }
            toStream = new FileOutputStream(new File(filenameTo));
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fromStream.read(buffer)) != -1) toStream.write(buffer, 0, bytesRead);
        } catch (FileNotFoundException e) {
            Errmsg.errmsg(e);
        } catch (IOException e) {
            Errmsg.errmsg(e);
        } finally {
            try {
                if (fromStream != null) {
                    fromStream.close();
                }
                if (toStream != null) {
                    toStream.close();
                }
            } catch (IOException e) {
                Errmsg.errmsg(e);
            }
        }
    }

    /**
	 * This method initializes all the components used on the
	 * interface.
	 * 
	 * Method edited by UF Software Engineering Summer 2009
	 * Developer: Dave Brosnan, Darren Goldfarb, Drew Goldfarb,
	 * Jubal Ledden, Gonzalo Medina, Dylan Moore
	 * Change Summary: Contact pictures directory and picFile initialization; added contact group components.
	 * 
	 * 
	 * @author Mike Berger
	 */
    private void initComponents() {
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        fntext = new javax.swing.JTextField();
        lntext = new javax.swing.JTextField();
        nntext = new javax.swing.JTextField();
        sntext = new javax.swing.JTextField();
        hptext = new javax.swing.JTextField();
        wptext = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        pgtext = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        fxtext = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        emtext = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        wbtext = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        comptext = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        bdchooser = new JDateChooser();
        jLabel23 = new javax.swing.JLabel();
        jLabel124 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        satext = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        cttext = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        sttext = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        cntext = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        zctext = new javax.swing.JTextField();
        jPanel5 = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        zctext1 = new javax.swing.JTextField();
        cntext1 = new javax.swing.JTextField();
        sttext1 = new javax.swing.JTextField();
        cttext1 = new javax.swing.JTextField();
        satext1 = new javax.swing.JTextField();
        addAnotherGroup = new javax.swing.JButton();
        groupComboBoxes = new ArrayList<javax.swing.JComboBox>();
        groupRemoveButtons = new ArrayList<javax.swing.JButton>();
        jPanel_Checkboxes = new JPanel();
        jCheckBox_Nickname = new JCheckBox();
        jCheckBox_ScreenName = new JCheckBox();
        jCheckBox_HomePhone = new JCheckBox();
        jCheckBox_WorkPhone = new JCheckBox();
        jCheckBox_Pager = new JCheckBox();
        jCheckBox_Fax = new JCheckBox();
        jCheckBox_Email = new JCheckBox();
        jCheckBox_WebPage = new JCheckBox();
        jCheckBox_Company = new JCheckBox();
        jCheckBox_Birthday = new JCheckBox();
        jCheckBox_Picture = new JCheckBox();
        jPanel_frame = new JPanel();
        GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints14 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints15 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints16 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints17 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints18 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints19 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints20 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints22 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints23 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints24 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints25 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints26 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints27 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints28 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints29 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints29a = new GridBagConstraints();
        GridBagConstraints gridBagConstraints30 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints32 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints33 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints34 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints35 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints36 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints37 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints38 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints39 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints40 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints41 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints42 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints43 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints45 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints46 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints47 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints49 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints50 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints51 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints52 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints53 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints54 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints55 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints56 = new GridBagConstraints();
        gridBagConstraints_groupComboBoxes = new ArrayList<GridBagConstraints>();
        gridBagConstraints_groupRemoveButtons = new ArrayList<GridBagConstraints>();
        GridBagConstraints gridBagConstraints_emailButton = new GridBagConstraints();
        GridBagConstraints gridBagConstraints_picture = new GridBagConstraints();
        Border picFrame = BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLoweredBevelBorder());
        jPanel_pic.setLayout(new BorderLayout());
        jPanel_frame.setBorder(picFrame);
        jButton_picture = new javax.swing.JButton();
        jButton_picture.setText("Upload Picture");
        jButton_picture.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JFileChooser fileopen = new JFileChooser();
                FileFilter filter = new FileNameExtensionFilter("JPEG files", "jpg", "jpeg");
                fileopen.addChoosableFileFilter(filter);
                int ret = fileopen.showDialog(null, "Open");
                if (ret == JFileChooser.APPROVE_OPTION) {
                    picFile = fileopen.getSelectedFile();
                    if (addr_.getKey() != -1) {
                        copyPhotoToDir();
                    }
                }
                refreshPic();
            }

            private void copyPhotoToDir() {
                String photoKeyName = String.valueOf(addr_.getKey()) + ".jpg";
                String contactPicsPath = Prefs.getPref(PrefName.HSQLDBDIR) + File.separator + BorgNameLoginSystem.getInstance().getUsername() + File.separator + Prefs.getPref(PrefName.CONTACTPHOTOSDIR) + File.separator;
                fileCopier(picFile.getAbsolutePath(), contactPicsPath + photoKeyName);
            }
        });
        notestext = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jButton_email = new javax.swing.JButton();
        jTabbedPane1.setPreferredSize(new java.awt.Dimension(540, 400));
        jPanel1.setLayout(new java.awt.GridBagLayout());
        ResourceHelper.setText(jLabel1, "First_Name:");
        jLabel1.setLabelFor(fntext);
        ResourceHelper.setText(jLabel2, "Last_Name:");
        jLabel2.setLabelFor(lntext);
        ResourceHelper.setText(jLabel3, "Nickname:");
        jLabel3.setLabelFor(nntext);
        ResourceHelper.setText(jLabel4, "Screen_Name:");
        jLabel4.setLabelFor(sntext);
        ResourceHelper.setText(jLabel5, "Home_Phone:");
        jLabel5.setLabelFor(hptext);
        ResourceHelper.setText(jLabel6, "Work_Phone:");
        jLabel6.setLabelFor(wptext);
        ResourceHelper.setText(jLabel7, "Pager:");
        jLabel7.setLabelFor(pgtext);
        ResourceHelper.setText(jLabel8, "Fax:");
        jLabel8.setLabelFor(fxtext);
        ResourceHelper.setText(jLabel9, "Email:");
        jLabel9.setLabelFor(emtext);
        ResourceHelper.setText(jLabel14, "Web_Page:");
        jLabel14.setLabelFor(wbtext);
        ResourceHelper.setText(jLabel21, "Company");
        jLabel21.setLabelFor(comptext);
        ResourceHelper.setText(jLabel22, "Birthday");
        jLabel22.setLabelFor(bdchooser);
        ResourceHelper.setText(jLabel23, "contact_group");
        ResourceHelper.setText(jCheckBox_Nickname, "nickname");
        ResourceHelper.setText(jCheckBox_ScreenName, "Screen_Name");
        ResourceHelper.setText(jCheckBox_HomePhone, "Home_Phone");
        ResourceHelper.setText(jCheckBox_WorkPhone, "Work_Phone");
        ResourceHelper.setText(jCheckBox_Pager, "pager");
        ResourceHelper.setText(jCheckBox_Fax, "fax");
        ResourceHelper.setText(jCheckBox_Email, "Email");
        ResourceHelper.setText(jCheckBox_WebPage, "webpage");
        ResourceHelper.setText(jCheckBox_Company, "Company");
        ResourceHelper.setText(jCheckBox_Birthday, "Birthday");
        ResourceHelper.setText(jCheckBox_Picture, "picture");
        jCheckBox_Nickname.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (jCheckBox_Nickname.isSelected()) {
                    addr_.setPref("nickname", 1);
                    jPanel1.add(jLabel3, GridBagConstraintsFactory.create(0, 3, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.add(nntext, GridBagConstraintsFactory.create(1, 3, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.updateUI();
                } else {
                    if (!nntext.getText().trim().isEmpty()) {
                        jCheckBox_Nickname.setSelected(true);
                    } else {
                        addr_.setPref("nickname", 0);
                        jPanel1.remove(jLabel3);
                        jPanel1.remove(nntext);
                        jPanel1.updateUI();
                    }
                }
            }
        });
        jCheckBox_ScreenName.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (jCheckBox_ScreenName.isSelected()) {
                    addr_.setPref("screenname", 1);
                    jPanel1.add(jLabel4, GridBagConstraintsFactory.create(0, 4, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.add(sntext, GridBagConstraintsFactory.create(1, 4, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.updateUI();
                } else {
                    if (!sntext.getText().trim().isEmpty()) {
                        jCheckBox_ScreenName.setSelected(true);
                    } else {
                        addr_.setPref("screenname", 0);
                        jPanel1.remove(jLabel4);
                        jPanel1.remove(sntext);
                        jPanel1.updateUI();
                    }
                }
            }
        });
        jCheckBox_HomePhone.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (jCheckBox_HomePhone.isSelected()) {
                    addr_.setPref("homephone", 1);
                    jPanel1.add(jLabel5, GridBagConstraintsFactory.create(0, 5, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.add(hptext, GridBagConstraintsFactory.create(1, 5, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.updateUI();
                } else {
                    if (!hptext.getText().trim().isEmpty()) {
                        jCheckBox_HomePhone.setSelected(true);
                    } else {
                        addr_.setPref("homephone", 0);
                        jPanel1.remove(jLabel5);
                        jPanel1.remove(hptext);
                        jPanel1.updateUI();
                    }
                }
            }
        });
        jCheckBox_WorkPhone.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (jCheckBox_WorkPhone.isSelected()) {
                    addr_.setPref("workphone", 1);
                    jPanel1.add(jLabel6, GridBagConstraintsFactory.create(0, 6, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.add(wptext, GridBagConstraintsFactory.create(1, 6, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.updateUI();
                } else {
                    if (!wptext.getText().trim().isEmpty()) {
                        jCheckBox_WorkPhone.setSelected(true);
                    } else {
                        addr_.setPref("workphone", 0);
                        jPanel1.remove(jLabel6);
                        jPanel1.remove(wptext);
                        jPanel1.updateUI();
                    }
                }
            }
        });
        jCheckBox_Pager.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (jCheckBox_Pager.isSelected()) {
                    addr_.setPref("pager", 1);
                    jPanel1.add(jLabel7, GridBagConstraintsFactory.create(0, 7, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.add(pgtext, GridBagConstraintsFactory.create(1, 7, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.updateUI();
                } else {
                    if (!pgtext.getText().trim().isEmpty()) {
                        jCheckBox_Pager.setSelected(true);
                    } else {
                        addr_.setPref("pager", 0);
                        jPanel1.remove(jLabel7);
                        jPanel1.remove(pgtext);
                        jPanel1.updateUI();
                    }
                }
            }
        });
        jCheckBox_Fax.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (jCheckBox_Fax.isSelected()) {
                    addr_.setPref("fax", 1);
                    jPanel1.add(jLabel8, GridBagConstraintsFactory.create(0, 8, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.add(fxtext, GridBagConstraintsFactory.create(1, 8, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.updateUI();
                } else {
                    if (!fxtext.getText().trim().isEmpty()) {
                        jCheckBox_Fax.setSelected(true);
                    } else {
                        addr_.setPref("fax", 0);
                        jPanel1.remove(jLabel8);
                        jPanel1.remove(fxtext);
                        jPanel1.updateUI();
                    }
                }
            }
        });
        jCheckBox_Email.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (jCheckBox_Email.isSelected()) {
                    addr_.setPref("email", 1);
                    jPanel1.add(jLabel9, GridBagConstraintsFactory.create(0, 9, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.add(emtext, GridBagConstraintsFactory.create(1, 9, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.updateUI();
                } else {
                    if (!emtext.getText().trim().isEmpty()) {
                        jCheckBox_Email.setSelected(true);
                    } else {
                        addr_.setPref("email", 0);
                        jPanel1.remove(jLabel9);
                        jPanel1.remove(emtext);
                        jPanel1.updateUI();
                    }
                }
            }
        });
        jCheckBox_WebPage.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (jCheckBox_WebPage.isSelected()) {
                    addr_.setPref("webpage", 1);
                    jPanel1.add(jLabel14, GridBagConstraintsFactory.create(0, 10, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.add(wbtext, GridBagConstraintsFactory.create(1, 10, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.updateUI();
                } else {
                    if (!wbtext.getText().trim().isEmpty()) {
                        jCheckBox_WebPage.setSelected(true);
                    } else {
                        addr_.setPref("webpage", 0);
                        jPanel1.remove(jLabel14);
                        jPanel1.remove(wbtext);
                        jPanel1.updateUI();
                    }
                }
            }
        });
        jCheckBox_Company.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (jCheckBox_Company.isSelected()) {
                    addr_.setPref("company", 1);
                    jPanel1.add(jLabel21, GridBagConstraintsFactory.create(0, 11, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.add(comptext, GridBagConstraintsFactory.create(1, 11, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.updateUI();
                } else {
                    if (!comptext.getText().trim().isEmpty()) {
                        jCheckBox_Company.setSelected(true);
                    } else {
                        addr_.setPref("company", 0);
                        jPanel1.remove(jLabel21);
                        jPanel1.remove(comptext);
                        jPanel1.updateUI();
                    }
                }
            }
        });
        jCheckBox_Birthday.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (jCheckBox_Birthday.isSelected()) {
                    addr_.setPref("birthday", 1);
                    jPanel1.add(jLabel22, GridBagConstraintsFactory.create(0, 12, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.add(bdchooser, GridBagConstraintsFactory.create(1, 12, GridBagConstraints.HORIZONTAL, 1));
                    jPanel1.updateUI();
                } else {
                    if (bdchooser.getDate() != null) {
                        jCheckBox_Birthday.setSelected(true);
                    } else {
                        addr_.setPref("birthday", 0);
                        jPanel1.remove(jLabel22);
                        jPanel1.remove(bdchooser);
                        jPanel1.updateUI();
                    }
                }
            }
        });
        jCheckBox_Picture.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (jCheckBox_Picture.isSelected()) {
                    addr_.setPref("picture", 1);
                    jPanel1.add(jPanel_pic, GridBagConstraintsFactory.create(2, 1, 11, 2, java.awt.GridBagConstraints.CENTER));
                    jPanel1.updateUI();
                } else {
                    File eff = new File(getFullContactPicName());
                    if (eff.exists()) {
                        jCheckBox_Picture.setSelected(true);
                    } else {
                        addr_.setPref("picture", 0);
                        jPanel1.remove(jPanel_pic);
                        jPanel1.updateUI();
                    }
                }
            }
        });
        jPanel_Checkboxes.add(jCheckBox_Nickname, GridBagConstraintsFactory.create(0, 0, GridBagConstraints.HORIZONTAL));
        jPanel_Checkboxes.add(jCheckBox_ScreenName, GridBagConstraintsFactory.create(1, 0, GridBagConstraints.HORIZONTAL));
        jPanel_Checkboxes.add(jCheckBox_HomePhone, GridBagConstraintsFactory.create(2, 0, GridBagConstraints.HORIZONTAL));
        jPanel_Checkboxes.add(jCheckBox_WorkPhone, GridBagConstraintsFactory.create(3, 0, GridBagConstraints.HORIZONTAL));
        jPanel_Checkboxes.add(jCheckBox_Pager, GridBagConstraintsFactory.create(4, 0, GridBagConstraints.HORIZONTAL));
        jPanel_Checkboxes.add(jCheckBox_Fax, GridBagConstraintsFactory.create(5, 0, GridBagConstraints.HORIZONTAL));
        jPanel_Checkboxes.add(jCheckBox_Email, GridBagConstraintsFactory.create(6, 0, GridBagConstraints.HORIZONTAL));
        jPanel_Checkboxes.add(jCheckBox_WebPage, GridBagConstraintsFactory.create(7, 0, GridBagConstraints.HORIZONTAL));
        jPanel_Checkboxes.add(jCheckBox_Company, GridBagConstraintsFactory.create(8, 0, GridBagConstraints.HORIZONTAL));
        jPanel_Checkboxes.add(jCheckBox_Birthday, GridBagConstraintsFactory.create(9, 0, GridBagConstraints.HORIZONTAL));
        GridBagConstraints jPanel_Checkboxes_GridBagConstraints = new GridBagConstraints();
        jPanel_Checkboxes_GridBagConstraints.gridx = 1;
        jPanel_Checkboxes_GridBagConstraints.gridy = 0;
        jPanel1.add(jPanel_Checkboxes, jPanel_Checkboxes_GridBagConstraints);
        ResourceHelper.setText(addAnotherGroup, "Add_Another_Group");
        addAnotherGroup.addActionListener(new java.awt.event.ActionListener() {

            /**
			 * Adds a combobox representing a contact group.
			 * 
			 * Method added by UF Software Engineering, Summer 2009
			 * Developer: Dylan Moore
			 */
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (gridBagYCount == 14 && groupComboBoxes.get(0).getSelectedItem() == "") {
                } else if (groupComboBoxes.size() < AddressModel.getReference().getGroups().size()) {
                    addComboBoxAndRemoveButton();
                }
                jPanel1.updateUI();
            }
        });
        jPanel1.setName(Resource.getResourceString("contact"));
        GridBagConstraints jPane1_GridBagConstraints = new GridBagConstraints();
        jPane1_GridBagConstraints.anchor = GridBagConstraints.PAGE_START;
        jTabbedPane1.add(jPanel1, jPane1_GridBagConstraints);
        jPanel2.setLayout(new java.awt.GridBagLayout());
        jPanel3.setLayout(new java.awt.GridBagLayout());
        jPanel3.setBorder(new javax.swing.border.TitledBorder(Resource.getResourceString("HomeAddress")));
        ResourceHelper.setText(jLabel10, "Home_Street_Address");
        jLabel10.setLabelFor(satext);
        ResourceHelper.setText(jLabel11, "Home_City:");
        jLabel11.setLabelFor(cttext);
        ResourceHelper.setText(jLabel12, "Home_State:");
        jLabel12.setLabelFor(sttext1);
        ResourceHelper.setText(jLabel13, "Home_Country:");
        jLabel13.setLabelFor(cntext1);
        ResourceHelper.setText(jLabel15, "Home_Zip_Code:");
        jLabel15.setLabelFor(zctext1);
        jPanel5.setLayout(new java.awt.GridBagLayout());
        jPanel5.setBorder(new javax.swing.border.TitledBorder(Resource.getResourceString("WorkAddress")));
        ResourceHelper.setText(jLabel16, "Work_Street_Address");
        jLabel16.setLabelFor(satext1);
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        ResourceHelper.setText(jLabel17, "Work_City:");
        jLabel17.setLabelFor(cttext1);
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel17.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        ResourceHelper.setText(jLabel18, "Work_State:");
        jLabel18.setLabelFor(sttext);
        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        ResourceHelper.setText(jLabel19, "Work_Zip_Code:");
        jLabel19.setLabelFor(zctext);
        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        ResourceHelper.setText(jLabel20, "Work_Country:");
        jLabel20.setLabelFor(cntext);
        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        satext1.setMinimumSize(new java.awt.Dimension(4, 50));
        jTabbedPane1.addTab(Resource.getResourceString("Address"), jPanel2);
        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resource/Save16.gif")));
        ResourceHelper.setText(jButton2, "Save");
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
                AddrListView.getReference().refresh();
            }
        });
        ResourceHelper.setText(cancelButton, "Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        jButton_email.setText("Send Email");
        Desktop desktop1 = Desktop.getDesktop();
        jButton_email.setEnabled(false);
        if (desktop1.isSupported(Desktop.Action.MAIL)) {
            jButton_email.setEnabled(true);
        }
        jButton_email.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onLaunchMail(evt);
            }

            private void onLaunchMail(ActionEvent evt) {
                String mailTo = emtext.getText();
                Desktop desktop = Desktop.getDesktop();
                try {
                    if (mailTo.length() > 0) {
                        URI uriMailTo = new URI("mailto", mailTo, null);
                        desktop.mail(uriMailTo);
                    } else desktop.mail();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (URISyntaxException use) {
                    use.printStackTrace();
                }
            }
        });
        jPanel4.add(jButton2);
        jPanel4.add(cancelButton);
        gridBagConstraints8.gridx = 0;
        gridBagConstraints8.gridy = 1;
        gridBagConstraints8.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints8.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints9.gridx = 0;
        gridBagConstraints9.gridy = 2;
        gridBagConstraints9.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints9.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints10.gridx = 0;
        gridBagConstraints10.gridy = 3;
        gridBagConstraints10.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints10.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints11.gridx = 0;
        gridBagConstraints11.gridy = 4;
        gridBagConstraints11.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints11.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints12.gridx = 0;
        gridBagConstraints12.gridy = 5;
        gridBagConstraints12.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints12.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints13.gridx = 0;
        gridBagConstraints13.gridy = 6;
        gridBagConstraints13.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints13.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints14.gridx = 0;
        gridBagConstraints14.gridy = 7;
        gridBagConstraints14.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints14.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints15.gridx = 0;
        gridBagConstraints15.gridy = 8;
        gridBagConstraints15.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints15.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints16.gridx = 0;
        gridBagConstraints16.gridy = 9;
        gridBagConstraints16.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints16.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints17.gridx = 0;
        gridBagConstraints17.gridy = 10;
        gridBagConstraints17.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints17.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints18.gridx = 0;
        gridBagConstraints18.gridy = 11;
        gridBagConstraints18.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints18.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints19.gridx = 0;
        gridBagConstraints19.gridy = 12;
        gridBagConstraints19.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints19.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints20.gridx = 1;
        gridBagConstraints20.gridy = 1;
        gridBagConstraints20.weightx = 1.0;
        gridBagConstraints20.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints21.gridx = 1;
        gridBagConstraints21.gridy = 10;
        gridBagConstraints21.weightx = 1.0;
        gridBagConstraints21.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints22.gridx = 1;
        gridBagConstraints22.gridy = 7;
        gridBagConstraints22.weightx = 1.0;
        gridBagConstraints22.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints23.gridx = 1;
        gridBagConstraints23.gridy = 11;
        gridBagConstraints23.weightx = 1.0;
        gridBagConstraints23.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints24.gridx = 1;
        gridBagConstraints24.gridy = 2;
        gridBagConstraints24.weightx = 1.0;
        gridBagConstraints24.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints25.gridx = 1;
        gridBagConstraints25.gridy = 3;
        gridBagConstraints25.weightx = 1.0;
        gridBagConstraints25.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints26.gridx = 1;
        gridBagConstraints26.gridy = 6;
        gridBagConstraints26.weightx = 1.0;
        gridBagConstraints26.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints27.gridx = 1;
        gridBagConstraints27.gridy = 8;
        gridBagConstraints27.weightx = 1.0;
        gridBagConstraints27.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints28.gridx = 1;
        gridBagConstraints28.gridy = 5;
        gridBagConstraints28.weightx = 1.0;
        gridBagConstraints28.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints29.gridx = 1;
        gridBagConstraints29.gridy = 12;
        gridBagConstraints29.weightx = 1.0;
        gridBagConstraints29.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints29a.gridx = 2;
        gridBagConstraints29a.gridy = 12;
        gridBagConstraints29a.weightx = 0.06;
        gridBagConstraints29a.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints30.gridx = 1;
        gridBagConstraints30.gridy = 9;
        gridBagConstraints30.weightx = 1.0;
        gridBagConstraints30.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints31.gridx = 1;
        gridBagConstraints31.gridy = 4;
        gridBagConstraints31.weightx = 1.0;
        gridBagConstraints31.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints32.gridx = 0;
        gridBagConstraints32.gridy = 2;
        gridBagConstraints32.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints32.weightx = 1.0D;
        gridBagConstraints32.weighty = 1.0D;
        gridBagConstraints33.gridx = 0;
        gridBagConstraints33.gridy = 1;
        gridBagConstraints33.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints33.weightx = 1.0D;
        gridBagConstraints33.weighty = 1.0D;
        gridBagConstraints34.gridx = 0;
        gridBagConstraints34.gridy = 1;
        gridBagConstraints34.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints35.gridx = 0;
        gridBagConstraints35.gridy = 2;
        gridBagConstraints35.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints36.gridx = 0;
        gridBagConstraints36.gridy = 3;
        gridBagConstraints36.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints36.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints37.gridx = 0;
        gridBagConstraints37.gridy = 4;
        gridBagConstraints37.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints38.gridx = 0;
        gridBagConstraints38.gridy = 5;
        gridBagConstraints38.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints39.gridx = 0;
        gridBagConstraints39.gridy = 1;
        gridBagConstraints39.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints40.gridx = 0;
        gridBagConstraints40.gridy = 2;
        gridBagConstraints40.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints41.gridx = 0;
        gridBagConstraints41.gridy = 3;
        gridBagConstraints41.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints42.gridx = 0;
        gridBagConstraints42.gridy = 4;
        gridBagConstraints42.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints43.gridx = 0;
        gridBagConstraints43.gridy = 5;
        gridBagConstraints43.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints45.gridx = 1;
        gridBagConstraints45.gridy = 3;
        gridBagConstraints45.weightx = 1.0;
        gridBagConstraints45.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints46.gridx = 1;
        gridBagConstraints46.gridy = 2;
        gridBagConstraints46.weightx = 1.0;
        gridBagConstraints46.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints47.gridx = 1;
        gridBagConstraints47.gridy = 5;
        gridBagConstraints47.weightx = 1.0;
        gridBagConstraints47.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints49.gridx = 1;
        gridBagConstraints49.gridy = 3;
        gridBagConstraints49.weightx = 1.0;
        gridBagConstraints49.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints50.gridx = 1;
        gridBagConstraints50.gridy = 2;
        gridBagConstraints50.weightx = 1.0;
        gridBagConstraints50.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints51.gridx = 1;
        gridBagConstraints51.gridy = 4;
        gridBagConstraints51.weightx = 1.0;
        gridBagConstraints51.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints52.gridx = 1;
        gridBagConstraints52.gridy = 4;
        gridBagConstraints52.weightx = 1.0;
        gridBagConstraints52.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints53.gridx = 1;
        gridBagConstraints53.gridy = 5;
        gridBagConstraints53.weightx = 1.0;
        gridBagConstraints53.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints54.gridx = 1;
        gridBagConstraints54.gridy = 1;
        gridBagConstraints54.weightx = 1.0;
        gridBagConstraints54.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints55.gridx = 1;
        gridBagConstraints55.gridy = 1;
        gridBagConstraints55.weightx = 1.0;
        gridBagConstraints55.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints56.gridx = 0;
        gridBagConstraints56.gridy = 13;
        gridBagConstraints56.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints56.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints_emailButton.gridx = 2;
        gridBagConstraints_emailButton.gridy = 9;
        gridBagConstraints_emailButton.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints_picture.gridx = 2;
        gridBagConstraints_picture.gridy = 1;
        gridBagConstraints_picture.gridheight = 11;
        gridBagConstraints_picture.gridwidth = 2;
        gridBagConstraints_picture.anchor = java.awt.GridBagConstraints.CENTER;
        jLabel12.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel22.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel23.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel124.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jPanel1.add(jLabel1, gridBagConstraints8);
        jPanel2.add(jPanel5, gridBagConstraints32);
        jPanel3.add(jLabel16, gridBagConstraints34);
        jPanel5.add(jLabel10, gridBagConstraints39);
        if (addr_.getKey() == -1) {
            jPanel_pic.add(jPanel_frame, BorderLayout.PAGE_START);
            jPanel_pic.add(jButton_picture, BorderLayout.PAGE_END);
            setVisible(true);
            jPanel1.add(jPanel_pic, GridBagConstraintsFactory.create(2, 1, 11, 2, java.awt.GridBagConstraints.CENTER));
            jPanel1.add(jLabel3, gridBagConstraints10);
            jPanel1.add(nntext, gridBagConstraints25);
            jPanel1.add(jLabel4, gridBagConstraints11);
            jPanel1.add(sntext, gridBagConstraints31);
            jPanel1.add(jLabel5, gridBagConstraints12);
            jPanel1.add(hptext, gridBagConstraints28);
            jPanel1.add(jLabel6, gridBagConstraints13);
            jPanel1.add(wptext, gridBagConstraints26);
            jPanel1.add(jLabel7, gridBagConstraints14);
            jPanel1.add(pgtext, gridBagConstraints22);
            jPanel1.add(jLabel8, gridBagConstraints15);
            jPanel1.add(fxtext, gridBagConstraints27);
            jPanel1.add(jLabel9, gridBagConstraints16);
            jPanel1.add(emtext, gridBagConstraints30);
            jPanel1.add(jLabel14, gridBagConstraints17);
            jPanel1.add(wbtext, gridBagConstraints21);
            jPanel1.add(jLabel21, gridBagConstraints18);
            jPanel1.add(comptext, gridBagConstraints23);
            jPanel1.add(jLabel22, gridBagConstraints19);
            jPanel1.add(bdchooser, gridBagConstraints29);
        } else {
            if (addr_.getPref("picture") == 1) {
                jPanel_pic.add(jPanel_frame, BorderLayout.PAGE_START);
                jPanel_pic.add(jButton_picture, BorderLayout.PAGE_END);
                setVisible(true);
                jPanel1.add(jPanel_pic, GridBagConstraintsFactory.create(2, 1, 11, 2, java.awt.GridBagConstraints.CENTER));
            }
            if (addr_.getPref("nickname") == 1) {
                jPanel1.add(jLabel3, gridBagConstraints10);
                jPanel1.add(nntext, gridBagConstraints25);
            }
            if (addr_.getPref("screenname") == 1) {
                jPanel1.add(jLabel4, gridBagConstraints11);
                jPanel1.add(sntext, gridBagConstraints31);
            }
            if (addr_.getPref("homephone") == 1) {
                jPanel1.add(jLabel5, gridBagConstraints12);
                jPanel1.add(hptext, gridBagConstraints28);
            }
            if (addr_.getPref("workphone") == 1) {
                jPanel1.add(jLabel6, gridBagConstraints13);
                jPanel1.add(wptext, gridBagConstraints26);
            }
            if (addr_.getPref("pager") == 1) {
                jPanel1.add(jLabel7, gridBagConstraints14);
                jPanel1.add(pgtext, gridBagConstraints22);
            }
            if (addr_.getPref("fax") == 1) {
                jPanel1.add(jLabel8, gridBagConstraints15);
                jPanel1.add(fxtext, gridBagConstraints27);
            }
            if (addr_.getPref("email") == 1) {
                jPanel1.add(jLabel9, gridBagConstraints16);
                jPanel1.add(emtext, gridBagConstraints30);
            }
            if (addr_.getPref("webpage") == 1) {
                jPanel1.add(jLabel14, gridBagConstraints17);
                jPanel1.add(wbtext, gridBagConstraints21);
            }
            if (addr_.getPref("company") == 1) {
                jPanel1.add(jLabel21, gridBagConstraints18);
                jPanel1.add(comptext, gridBagConstraints23);
            }
            if (addr_.getPref("birthday") == 1) {
                jPanel1.add(jLabel22, gridBagConstraints19);
                jPanel1.add(bdchooser, gridBagConstraints29);
            }
        }
        jPanel1.add(jLabel2, gridBagConstraints9);
        jPanel2.add(jPanel3, gridBagConstraints33);
        jPanel3.add(jLabel17, gridBagConstraints35);
        jPanel5.add(jLabel11, gridBagConstraints40);
        jPanel5.add(jLabel18, gridBagConstraints41);
        jTabbedPane1.addTab(Resource.getResourceString("Notes"), getJPanel6());
        attPanel = new LinkPanel();
        jTabbedPane1.addTab(Resource.getResourceString("links"), attPanel);
        jPanel3.add(jLabel12, gridBagConstraints36);
        jPanel3.add(jLabel13, gridBagConstraints37);
        jPanel5.add(jLabel20, gridBagConstraints42);
        jPanel3.add(jLabel15, gridBagConstraints38);
        jPanel5.add(jLabel19, gridBagConstraints43);
        jPanel3.add(zctext1, gridBagConstraints47);
        jPanel5.add(sttext, gridBagConstraints45);
        jPanel3.add(sttext1, gridBagConstraints49);
        jPanel5.add(cttext, gridBagConstraints46);
        jPanel3.add(cttext1, gridBagConstraints50);
        jPanel5.add(cntext, gridBagConstraints51);
        jPanel3.add(cntext1, gridBagConstraints52);
        jPanel5.add(zctext, gridBagConstraints53);
        jPanel3.add(satext1, gridBagConstraints54);
        jPanel5.add(satext, gridBagConstraints55);
        jPanel1.add(fntext, gridBagConstraints20);
        jPanel1.add(lntext, gridBagConstraints24);
        jPanel1.add(jLabel23, gridBagConstraints56);
        GridBagConstraints gridBagConstraints_addAnotherGroup = new GridBagConstraints();
        gridBagConstraints_addAnotherGroup.gridx = 3;
        gridBagConstraints_addAnotherGroup.gridy = 13;
        gridBagConstraints_addAnotherGroup.weightx = .048;
        gridBagConstraints_addAnotherGroup.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(addAnotherGroup, gridBagConstraints_addAnotherGroup);
        GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
        setLayout(new GridBagLayout());
        gridBagConstraints6.gridx = 0;
        gridBagConstraints6.gridy = 2;
        gridBagConstraints7.weightx = 1.0;
        gridBagConstraints7.weighty = 1.0;
        gridBagConstraints7.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints7.gridx = 0;
        gridBagConstraints7.gridy = 1;
        add(jPanel4, gridBagConstraints6);
        add(jTabbedPane1, gridBagConstraints7);
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        saveaddr();
    }

    public void refresh() {
    }

    /**
	 * Removes any changes made while editing a particular contact.
	 * Method added by UF Software Engineering, Summer 2009
	 * Developer: Jubal Ledden
	 * @return String
	 */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        this.getParent().remove(this);
    }

    /**
	 * Constructs the full file path to the current Address's contact picture.
	 * Method added by UF Software Engineering, Summer 2009
	 * Developer: Dylan Moore
	 * @return String
	 */
    private String getFullContactPicName() {
        String photoKeyName = String.valueOf(addr_.getKey()) + ".jpg";
        return getContactPicPath() + photoKeyName;
    }

    /**
	 * * Constructs the current user's contact picture path name
	 * (for storing contact pictures).
	 * 
	 * Method added by UF Software Engineering, Summer 2009
	 * Developer: Dylan Moore
	 * @return String
	 */
    private String getContactPicPath() {
        String contactPicsPath = Prefs.getPref(PrefName.HSQLDBDIR) + File.separator + BorgNameLoginSystem.getInstance().getUsername() + File.separator + Prefs.getPref(PrefName.CONTACTPHOTOSDIR) + File.separator;
        return contactPicsPath;
    }

    /**
	 * Copies the file stored by picFile into the proper location in this user's
	 * folder: <username>/<address_num>.jpg
	 * 
	 * This method has been added by UF Software Engineering Summer 2009
	 * Developer: Dylan Moore
	 */
    private void copyPhotoToDir() {
        fileCopier(picFile.getAbsolutePath(), getFullContactPicName());
    }

    /**
	    * Store contact with most recent changes in database
	    *
	    * Method edited by UF Software Engineering, Summer 2009
	    * Developer: Darren Goldfarb
	    * Change Summary: Added support for contact groups
	    */
    private void saveaddr() {
        if (fntext.getText().equals("") || lntext.getText().equals("")) {
            Errmsg.notice(Resource.getResourceString("First_and_Last_name_are_Required"));
            return;
        }
        addr_.setFirstName(fntext.getText());
        addr_.setLastName(lntext.getText());
        addr_.setNickname(nntext.getText());
        addr_.setEmail(emtext.getText());
        addr_.setScreenName(sntext.getText());
        addr_.setWorkPhone(wptext.getText());
        addr_.setHomePhone(hptext.getText());
        addr_.setFax(fxtext.getText());
        addr_.setPager(pgtext.getText());
        addr_.setWebPage(wbtext.getText());
        addr_.setNotes(notestext.getText());
        addr_.setStreetAddress(satext.getText());
        addr_.setCity(cttext.getText());
        addr_.setState(sttext.getText());
        addr_.setCountry(cntext.getText());
        addr_.setZip(zctext.getText());
        addr_.setWorkStreetAddress(satext1.getText());
        addr_.setWorkCity(cttext1.getText());
        addr_.setWorkState(sttext1.getText());
        addr_.setWorkCountry(cntext1.getText());
        addr_.setWorkZip(zctext1.getText());
        addr_.setCompany(comptext.getText());
        addr_.setBirthday(bdchooser.getDate());
        ArrayList<String> savedGroups = new ArrayList<String>();
        for (int i = 0; i < groupComboBoxes.size(); i++) {
            if (groupComboBoxes.get(i).getSelectedItem() != "") {
                savedGroups.add((String) groupComboBoxes.get(i).getSelectedItem());
            }
        }
        addr_.setGroups(savedGroups);
        try {
            boolean needToCopyFile = (addr_.getKey() == -1);
            AddressModel.getReference().saveAddress(addr_);
            if (needToCopyFile) {
                copyPhotoToDir();
            }
            if (fr_ != null) this.remove(); else this.getParent().remove(this);
        } catch (Exception e) {
            Errmsg.errmsg(e);
        }
    }

    /**
	    * Populates contact fields from database
	    *
	    * Method edited by UF Software Engineering, Summer 2009
	    * Developer: Darren Goldfarb
	    * Change Summary: Added support for contact groups
	    */
    private void showaddr() {
        if (addr_.getKey() == -1) {
            addr_.setPref("nickname", 1);
            jCheckBox_Nickname.setSelected(true);
            addr_.setPref("email", 1);
            jCheckBox_Email.setSelected(true);
            addr_.setPref("screenname", 1);
            jCheckBox_ScreenName.setSelected(true);
            addr_.setPref("workphone", 1);
            jCheckBox_WorkPhone.setSelected(true);
            addr_.setPref("homephone", 1);
            jCheckBox_HomePhone.setSelected(true);
            addr_.setPref("fax", 1);
            jCheckBox_Fax.setSelected(true);
            addr_.setPref("pager", 1);
            jCheckBox_Pager.setSelected(true);
            addr_.setPref("webpage", 1);
            jCheckBox_WebPage.setSelected(true);
            addr_.setPref("company", 1);
            jCheckBox_Company.setSelected(true);
            addr_.setPref("birthday", 1);
            jCheckBox_Birthday.setSelected(true);
            addr_.setPref("picture", 1);
            jCheckBox_Picture.setSelected(true);
            picFile = new File("res/resource/default_contact.jpg");
            try {
                BufferedImage pic = ImageIO.read(picFile);
                ImageIcon icon = scalePic(pic);
                jLabel124.setIcon(icon);
                jPanel_frame.add(jLabel124);
            } catch (IOException e) {
                try {
                    picFile = new File("res/resource/file_not_found.jpg");
                    BufferedImage pic = ImageIO.read(picFile);
                    ImageIcon icon = scalePic(pic);
                    jLabel124.setIcon(icon);
                    jPanel_frame.add(jLabel124);
                } catch (IOException exc) {
                    Errmsg.errmsg(e);
                }
            }
            jButton_picture.setVisible(true);
        } else {
            if (addr_.getPref("nickname").intValue() == 1) {
                jCheckBox_Nickname.setSelected(true);
                nntext.setText(addr_.getNickname());
            } else {
                jCheckBox_Nickname.setSelected(false);
            }
            if (addr_.getPref("email").intValue() == 1) {
                jCheckBox_Email.setSelected(true);
                emtext.setText(addr_.getEmail());
            } else {
                jCheckBox_Email.setSelected(false);
            }
            if (addr_.getPref("screenname").intValue() == 1) {
                jCheckBox_ScreenName.setSelected(true);
                sntext.setText(addr_.getScreenName());
            } else {
                jCheckBox_ScreenName.setSelected(false);
            }
            if (addr_.getPref("workphone").intValue() == 1) {
                jCheckBox_WorkPhone.setSelected(true);
                wptext.setText(addr_.getWorkPhone());
            } else {
                jCheckBox_WorkPhone.setSelected(false);
            }
            if (addr_.getPref("homephone").intValue() == 1) {
                jCheckBox_HomePhone.setSelected(true);
                hptext.setText(addr_.getHomePhone());
            } else {
                jCheckBox_HomePhone.setSelected(false);
            }
            if (addr_.getPref("fax").intValue() == 1) {
                jCheckBox_Fax.setSelected(true);
                fxtext.setText(addr_.getFax());
            } else {
                jCheckBox_Fax.setSelected(false);
            }
            if (addr_.getPref("pager").intValue() == 1) {
                jCheckBox_Pager.setSelected(true);
                pgtext.setText(addr_.getPager());
            } else {
                jCheckBox_Pager.setSelected(false);
            }
            if (addr_.getPref("webpage").intValue() == 1) {
                jCheckBox_WebPage.setSelected(true);
                wbtext.setText(addr_.getWebPage());
            } else {
                jCheckBox_WebPage.setSelected(false);
            }
            if (addr_.getPref("company").intValue() == 1) {
                jCheckBox_Company.setSelected(true);
                comptext.setText(addr_.getCompany());
            } else {
                jCheckBox_Company.setSelected(false);
            }
            if (addr_.getPref("birthday").intValue() == 1) {
                jCheckBox_Birthday.setSelected(true);
                Date bd = addr_.getBirthday();
                bdchooser.setDate(bd);
            } else {
                jCheckBox_Birthday.setSelected(false);
            }
            if (addr_.getPref("picture").intValue() == 1) {
                jCheckBox_Picture.setSelected(true);
                if (addr_.getPref("picture") == 1) {
                    if (!(new File(getContactPicPath()).isDirectory())) {
                        new File(getContactPicPath()).mkdir();
                    }
                    picFile = new File(getFullContactPicName());
                    if (picFile.exists() && picFile.isFile()) {
                        refreshPic();
                    }
                } else {
                    picFile = new File("res/resource/default_contact.jpg");
                }
                try {
                    BufferedImage pic = ImageIO.read(picFile);
                    ImageIcon icon = scalePic(pic);
                    jLabel124.setIcon(icon);
                    jPanel_frame.add(jLabel124);
                } catch (IOException e) {
                    try {
                        picFile = new File("res/resource/file_not_found.jpg");
                        BufferedImage pic = ImageIO.read(picFile);
                        ImageIcon icon = scalePic(pic);
                        jLabel124.setIcon(icon);
                        jPanel_frame.add(jLabel124);
                    } catch (IOException exc) {
                        Errmsg.errmsg(e);
                    }
                }
                jButton_picture = new javax.swing.JButton();
                jButton_picture.setText("Upload Picture");
                jButton_picture.setVisible(true);
            } else {
                jCheckBox_Picture.setSelected(false);
            }
            fntext.setText(addr_.getFirstName());
            lntext.setText(addr_.getLastName());
            notestext.setText(addr_.getNotes());
            satext.setText(addr_.getStreetAddress());
            cttext.setText(addr_.getCity());
            sttext.setText(addr_.getState());
            cntext.setText(addr_.getCountry());
            zctext.setText(addr_.getZip());
            satext1.setText(addr_.getWorkStreetAddress());
            cttext1.setText(addr_.getWorkCity());
            sttext1.setText(addr_.getWorkState());
            cntext1.setText(addr_.getWorkCountry());
            zctext1.setText(addr_.getWorkZip());
        }
        if (!addr_.getGroups().isEmpty()) {
            for (int i = 0; i < addr_.getGroups().size(); i++) {
                addComboBoxAndRemoveButton();
                if (i == 0) {
                    groupComboBoxes.get(0).addItem("");
                }
                groupComboBoxes.get(groupComboBoxes.size() - 1).setSelectedItem(addr_.getGroups().get(i));
            }
        } else {
            addComboBoxAndRemoveButton();
            groupComboBoxes.get(0).addItem("");
            groupComboBoxes.get(0).setSelectedItem("");
        }
        attPanel.setOwner(addr_);
    }

    /**
	    * This method adds a group remove button, included is the 
action listener for
	    * the button
	    * Method created by UF Software Engineering Summer 2009
	    * Developer: Darren Goldfarb
	    */
    public void addRemoveButton() {
        javax.swing.JButton newRB = new javax.swing.JButton();
        GridBagConstraints newGBC = new GridBagConstraints();
        newGBC.gridx = 2;
        newGBC.gridy = gridBagYCount;
        newGBC.weightx = .048;
        newGBC.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints_groupRemoveButtons.add(newGBC);
        ResourceHelper.setText(newRB, "Remove");
        newRB.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int x = 0;
                for (int i = 0; i < groupRemoveButtons.size(); i++) {
                    if (((JButton) groupRemoveButtons.get(i)).equals(evt.getSource())) {
                        x = i;
                    }
                }
                if (x == 0 && groupComboBoxes.size() == 1) {
                    groupComboBoxes.get(0).setSelectedItem("");
                } else {
                    removeGroupComboBox(x);
                    removeRemoveButton(x);
                    gridBagConstraints_groupComboBoxes.remove(x);
                    gridBagConstraints_groupRemoveButtons.remove(x);
                    for (int i = x; i < groupRemoveButtons.size(); i++) {
                        jPanel1.remove(groupComboBoxes.get(i));
                        jPanel1.remove(groupRemoveButtons.get(i));
                        jPanel1.updateUI();
                        gridBagConstraints_groupComboBoxes.get(i).gridy--;
                        gridBagConstraints_groupRemoveButtons.get(i).gridy--;
                        jPanel1.add(groupComboBoxes.get(i), gridBagConstraints_groupComboBoxes.get(i));
                        jPanel1.add(groupRemoveButtons.get(i), gridBagConstraints_groupRemoveButtons.get(i));
                    }
                    groupComboBoxes.get(groupComboBoxes.size() - 1).setEnabled(true);
                    gridBagYCount--;
                    jPanel1.updateUI();
                    if (x == 0) {
                        groupComboBoxes.get(0).addItem("");
                    }
                }
            }
        });
        groupRemoveButtons.add(newRB);
        jPanel1.add(groupRemoveButtons.get(groupRemoveButtons.size() - 1), newGBC);
    }

    /**
	    * This method removes a remove group button
	    * Method created by UF Software Engineering Summer 2009
	    * Developer: Darren Goldfarb
	    * @param index the index in the array list of the remove 
group button
	    * which is to be removed
	    */
    public JButton removeRemoveButton(int index) {
        jPanel1.remove(groupRemoveButtons.get(index));
        return groupRemoveButtons.remove(index);
    }

    /**
	    * This method adds a group combo box to the AddressView, 
placing it in
	    * proper position on the screen and populating it with group 
that have not been
	    * selected for that particular contact
	    * Method created by UF Software Engineering Summer 2009
	    * Developer: Darren Goldfarb
	    */
    public void addGroupComboBox() {
        javax.swing.JComboBox newGCB = new javax.swing.JComboBox();
        GridBagConstraints newGBC = new GridBagConstraints();
        newGBC.gridx = 1;
        newGBC.gridy = gridBagYCount;
        newGBC.weightx = 1;
        newGBC.fill = java.awt.GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < AddressModel.getReference().getGroups().size(); i++) {
            if (!alreadyUsed(AddressModel.getReference().getGroups().get(i))) {
                newGCB.addItem(AddressModel.getReference().getGroups().get(i));
            }
        }
        groupComboBoxes.add(newGCB);
        jPanel1.add(groupComboBoxes.get(groupComboBoxes.size() - 1), newGBC);
        gridBagConstraints_groupComboBoxes.add(newGBC);
        if (groupComboBoxes.size() > 1) {
            groupComboBoxes.get(groupComboBoxes.size() - 2).setEnabled(false);
        }
    }

    /**
	    * This method checks to see if any of the previous group 
combo boxes have
	    * have the given group selected
	    * Method created by UF Software Engineering Summer 2009
	    * Developer: Darren Goldfarb
	    * @param groupName the name of the group which is checked 
for
	    * selection in previous combo boxes
	    */
    public boolean alreadyUsed(String groupName) {
        for (int i = 0; i < groupComboBoxes.size(); i++) {
            if (groupName.compareTo((String) groupComboBoxes.get(i).getSelectedItem()) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
	    * This method removes a group combo box and enables the last 
remaining combo box
	    * Method created by UF Software Engineering Summer 2009
	    * Developer: Darren Goldfarb
	    * @param index the index in the array list of the combo box 
that is to be removed
	    */
    public JComboBox removeGroupComboBox(int index) {
        jPanel1.remove(groupComboBoxes.get(index));
        if (groupComboBoxes.get(index).isEnabled() == false) {
            for (int i = index + 1; i < groupComboBoxes.size(); i++) {
                groupComboBoxes.get(i).addItem(groupComboBoxes.get(index).getSelectedItem());
            }
        }
        if (index == groupComboBoxes.size() - 1 && groupComboBoxes.size() > 1) {
            groupComboBoxes.get(groupComboBoxes.size() - 2).setEnabled(true);
        }
        return groupComboBoxes.remove(index);
    }

    /**
	    * This method adds a group combo box paired with a remove 
group button
	    * to the AddressView
	    *
	    * Method created by UF Software Engineering Summer 2009
	    * Developer: Darren Goldfarb
	    */
    public void addComboBoxAndRemoveButton() {
        addGroupComboBox();
        addRemoveButton();
        gridBagYCount++;
        jPanel1.updateUI();
    }

    public void refreshPic() {
        try {
            BufferedImage pic = ImageIO.read(picFile);
            ImageIcon icon = scalePic(pic);
            jLabel124.setIcon(icon);
        } catch (IOException e) {
            Errmsg.errmsg(e);
        }
    }

    public ImageIcon scalePic(BufferedImage img) {
        BufferedImage scaledImg;
        int width = img.getWidth();
        int height = img.getHeight();
        ImageIcon icon;
        if ((width > 170) || (height > 170)) {
            double aspectRatio = width / height;
            double scaleFactor;
            if (aspectRatio > 1) {
                scaleFactor = 170 / ((double) width);
            } else {
                scaleFactor = 170 / ((double) height);
            }
            width = (int) Math.round(width * scaleFactor);
            height = (int) Math.round(height * scaleFactor);
            scaledImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        } else {
            scaledImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics gr = scaledImg.createGraphics();
        gr.drawImage(img, 0, 0, width, height, null);
        icon = new ImageIcon(scaledImg);
        return icon;
    }

    public void refreshFields() {
    }
}
