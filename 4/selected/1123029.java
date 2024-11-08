package com.lovehorsepower.imagemailerapp.dialogs;

import com.lovehorsepower.imagemailerapp.panels.GalleryManagerPanel;
import com.lovehorsepower.imagemailerapp.panels.MainTabPanel;
import com.lovehorsepower.imagemailerapp.workers.ImageBean;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.openide.util.Exceptions;

/**
 *
 * @author obernbergerj
 */
public class CommentsDialog extends javax.swing.JDialog {

    MainTabPanel mp = null;

    ImageIcon imageIcon = null;

    int currentImageCount = 0;

    HashMap commentsMap = null;

    JCheckBox imageCBox = null;

    GalleryManagerPanel gp = null;

    String galleryType = "";

    /** Creates new form CommentsDialog */
    public CommentsDialog(MainTabPanel mp, String title, boolean modal) {
        super(mp.mf.getFrame(), title, modal);
        this.mp = mp;
        this.gp = null;
        this.currentImageCount = mp.currentWheelCount;
        commentsMap = new HashMap();
        initComponents();
        try {
            readMap();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        setupDialogMP();
    }

    public CommentsDialog(GalleryManagerPanel gp, String title, boolean modal) {
        super(gp.mf.getFrame(), title, modal);
        this.mp = null;
        this.gp = gp;
        Iterator it = null;
        String detail = "";
        String lastImageName = "";
        String comment = "";
        commentsMap = new HashMap();
        initComponents();
        if (gp.gDetails != null) {
            it = gp.gDetails.iterator();
            while (it != null && it.hasNext()) {
                detail = (String) it.next();
                if (detail.startsWith("$IMG$:")) {
                    lastImageName = detail.substring(6);
                }
                if (detail.startsWith("$COMMENT$:")) {
                    comment = detail.substring(10);
                    System.out.println("Adding to commentsMap: " + lastImageName + " and value: " + comment);
                    commentsMap.put(lastImageName, comment);
                }
                if (detail.startsWith("$TYPE$:")) {
                    galleryType = detail.substring(7);
                }
            }
        }
        currentImageCount = 0;
        setupDialogGP();
    }

    private void setupDialogMP() {
        ImageBean ib = null;
        if (currentImageCount == 0) {
            prevButton.setEnabled(false);
        } else {
            prevButton.setEnabled(true);
        }
        ib = (ImageBean) mp.imageBeanList.get(currentImageCount);
        imageLabel.setIcon(ib.getImageLabel().getIcon());
        infoPanel.removeAll();
        imageCBox = new JCheckBox(ib.getCheckBox().getText().trim());
        imageCBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                handleCheckboxChange();
            }
        });
        imageCBox.setSelected(ib.getCheckBox().isSelected());
        infoPanel.add(imageCBox, BorderLayout.WEST);
        infoPanel.updateUI();
        if (commentsMap.get(ib.getCheckBox().getText()) != null) {
            commentTextArea.setText((String) commentsMap.get(ib.getCheckBox().getText().trim()));
        } else {
            commentTextArea.setText("");
        }
        commentTextArea.grabFocus();
    }

    private void setupDialogGP() {
        JLabel imageNameLabel = null;
        String iName = "";
        Iterator it = null;
        int counter = 0;
        ImageBean ib = null;
        if (currentImageCount == 0) {
            iName = gp.lastLabel.getName().substring(gp.lastLabel.getName().lastIndexOf("/") + 1);
            it = gp.galleryImageBeanList.iterator();
            while (it != null && it.hasNext()) {
                ib = (ImageBean) it.next();
                if (ib.getCheckBox().getText().trim().equals(iName.trim())) {
                    break;
                }
                counter++;
            }
            currentImageCount = counter;
        } else {
            ib = (ImageBean) gp.galleryImageBeanList.get(currentImageCount);
        }
        if (currentImageCount == 0) {
            prevButton.setEnabled(false);
        } else {
            prevButton.setEnabled(true);
        }
        imageLabel.setIcon(ib.getImageLabel().getIcon());
        infoPanel.removeAll();
        imageNameLabel = new JLabel(iName);
        infoPanel.add(imageNameLabel, BorderLayout.WEST);
        infoPanel.updateUI();
        System.out.println("Checking: " + ib.getCheckBox().getText());
        if (commentsMap.get(ib.getCheckBox().getText().trim()) != null) {
            commentTextArea.setText((String) commentsMap.get(ib.getCheckBox().getText().trim()));
        } else {
            commentTextArea.setText("");
        }
        commentTextArea.grabFocus();
    }

    private void handleCheckboxChange() {
        ImageBean ib = null;
        Iterator it = null;
        it = mp.imageBeanList.iterator();
        while (it != null && it.hasNext()) {
            ib = (ImageBean) it.next();
            if (ib.getCheckBox().getText().trim().equals(imageCBox.getText())) {
                ib.getCheckBox().setSelected(imageCBox.isSelected());
            }
        }
    }

    private void readMap() throws IOException {
        BufferedReader in = null;
        String str = "";
        StringBuffer lineBuff = null;
        String imageName = "";
        int index = 0;
        Iterator it = null;
        try {
            in = new BufferedReader(new FileReader("comments.properties." + mp.currentDir.getName()));
        } catch (FileNotFoundException ex) {
            return;
        }
        while ((str = in.readLine()) != null) {
            if (str.endsWith("\\")) {
                str = str.substring(0, str.length() - 1);
            }
            if (str.contains("=")) {
                if (lineBuff != null) {
                    System.out.println("Adding in: " + imageName + " with value: " + lineBuff.toString());
                    commentsMap.put(imageName, lineBuff.toString());
                }
                index = str.indexOf("=");
                imageName = str.substring(0, index);
                System.out.println("Got imageName: " + imageName);
                lineBuff = new StringBuffer();
                lineBuff.append(str.substring(index + 1));
            } else {
                lineBuff.append("\n" + str);
            }
        }
        if (lineBuff != null) {
            commentsMap.put(imageName, lineBuff.toString());
        }
        it = commentsMap.keySet().iterator();
        while (it != null && it.hasNext()) {
            str = (String) it.next();
            System.out.println("Key: " + str + " value: " + commentsMap.get(str));
        }
        in.close();
    }

    private void writeMap() throws IOException {
        BufferedWriter out = null;
        Iterator it = null;
        String line = "";
        String key = "";
        String tmpString = "";
        if (gp != null) {
            out = new BufferedWriter(new FileWriter("comments.properties.tmp"));
        } else if (mp != null) {
            out = new BufferedWriter(new FileWriter("comments.properties." + mp.currentDir.getName()));
        }
        it = commentsMap.keySet().iterator();
        while (it != null && it.hasNext()) {
            key = (String) it.next();
            tmpString = (String) commentsMap.get(key);
            if (tmpString.contains("\n")) {
                tmpString = tmpString.replaceAll("\\n", "\\\\\n");
            }
            line = new String(key + "=" + tmpString);
            out.write(line + "\n");
        }
        out.close();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        prevButton = new javax.swing.JButton();
        textScrollPane = new javax.swing.JScrollPane();
        commentTextArea = new javax.swing.JTextArea();
        imageLabel = new javax.swing.JLabel();
        infoPanel = new javax.swing.JPanel();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.lovehorsepower.imagemailerapp.ImageMailerApp.class).getContext().getResourceMap(CommentsDialog.class);
        cancelButton.setText(resourceMap.getString("cancelButton.text"));
        cancelButton.setName("cancelButton");
        cancelButton.setPreferredSize(new java.awt.Dimension(100, 23));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        okButton.setText(resourceMap.getString("okButton.text"));
        okButton.setName("okButton");
        okButton.setPreferredSize(new java.awt.Dimension(100, 23));
        okButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        nextButton.setText(resourceMap.getString("nextButton.text"));
        nextButton.setName("nextButton");
        nextButton.setPreferredSize(new java.awt.Dimension(100, 23));
        nextButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });
        prevButton.setText(resourceMap.getString("prevButton.text"));
        prevButton.setName("prevButton");
        prevButton.setPreferredSize(new java.awt.Dimension(100, 23));
        prevButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prevButtonActionPerformed(evt);
            }
        });
        textScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("textScrollPane.border.title")));
        textScrollPane.setName("textScrollPane");
        commentTextArea.setColumns(20);
        commentTextArea.setRows(5);
        commentTextArea.setName("commentTextArea");
        textScrollPane.setViewportView(commentTextArea);
        imageLabel.setText(resourceMap.getString("imageLabel.text"));
        imageLabel.setName("imageLabel");
        imageLabel.setPreferredSize(new java.awt.Dimension(160, 120));
        infoPanel.setName("infoPanel");
        infoPanel.setLayout(new java.awt.BorderLayout());
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(infoPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE).addComponent(textScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addComponent(prevButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))).addGroup(layout.createSequentialGroup().addGap(125, 125, 125).addComponent(imageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(imageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(infoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(textScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(prevButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        pack();
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {
        ImageBean ib = null;
        if (mp != null) {
            ib = (ImageBean) mp.imageBeanList.get(currentImageCount);
        } else {
            ib = (ImageBean) gp.galleryImageBeanList.get(currentImageCount);
        }
        if (commentTextArea.getText().length() > 0) {
            commentsMap.put(ib.getCheckBox().getText().trim(), commentTextArea.getText());
        }
        if (mp != null) {
            mp.moveSelectedImage(1);
        }
        currentImageCount++;
        if (mp != null) {
            if (currentImageCount >= mp.imageBeanList.size()) {
                currentImageCount--;
                nextButton.setEnabled(false);
                return;
            }
        } else if (gp != null) {
            if (currentImageCount >= gp.galleryImageBeanList.size()) {
                currentImageCount--;
                nextButton.setEnabled(false);
                return;
            }
        }
        nextButton.setEnabled(true);
        if (mp != null) {
            setupDialogMP();
        }
        if (gp != null) {
            setupDialogGP();
        }
    }

    private void prevButtonActionPerformed(java.awt.event.ActionEvent evt) {
        ImageBean ib = null;
        if (mp != null) {
            ib = (ImageBean) mp.imageBeanList.get(currentImageCount);
        } else {
            ib = (ImageBean) gp.galleryImageBeanList.get(currentImageCount);
        }
        if (commentTextArea.getText().length() > 0) {
            commentsMap.put(ib.getCheckBox().getText().trim(), commentTextArea.getText());
        }
        if (mp != null) {
            mp.moveSelectedImage(-1);
        }
        currentImageCount--;
        if (currentImageCount < 0) {
            currentImageCount++;
            return;
        }
        nextButton.setEnabled(true);
        if (mp != null) {
            setupDialogMP();
        }
        if (gp != null) {
            setupDialogGP();
        }
    }

    private void handleOKForMP() {
        ImageBean ib = null;
        ib = (ImageBean) mp.imageBeanList.get(currentImageCount);
        if (commentTextArea.getText().length() > 0) {
            commentsMap.put(ib.getCheckBox().getText().trim(), commentTextArea.getText());
        }
        try {
            writeMap();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error writting comments to disk.\n" + ex, "Disk Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        dispose();
    }

    private void handleOKForGP() {
        File commentsFile = null;
        byte imageData[] = null;
        ImageBean ib = null;
        ib = (ImageBean) gp.galleryImageBeanList.get(currentImageCount);
        if (commentTextArea.getText().length() > 0) {
            commentsMap.put(ib.getCheckBox().getText().trim(), commentTextArea.getText());
        }
        try {
            writeMap();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error writting comments to disk.\n" + ex, "Disk Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        commentsFile = new File("comments.properties.tmp");
        if (commentsFile.exists()) {
            try {
                com.lovehorsepower.emailimages.EIWSService service = new com.lovehorsepower.emailimages.EIWSService();
                com.lovehorsepower.emailimages.EIWS port = service.getEIWSPort();
                imageData = read(commentsFile.getAbsolutePath());
                java.lang.String result = port.sendImages(gp.mf.prefs.getEmailAddress(), gp.currentGalleryName, "comments.properties", imageData);
                System.out.println("Result from sending comments = " + result);
                if (result.startsWith("ERROR")) {
                    JOptionPane.showMessageDialog(gp, "Error sending comment data to web service.\nPlease try again later.\n" + result, "Web Service Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(gp, "Error sending comment data to web service.\nPlease try again later.\n" + ex, "Web Service Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                com.lovehorsepower.emailimages.EIWSService service = new com.lovehorsepower.emailimages.EIWSService();
                com.lovehorsepower.emailimages.EIWS port = service.getEIWSPort();
                java.lang.String result = port.buildGallery(gp.mf.prefs.getEmailAddress(), gp.currentGalleryName, galleryType);
                System.out.println("Result = " + result);
                JOptionPane.showMessageDialog(gp, "Your gallery is being rebuilt.\nYour changes will show up shortly.", "Rebuilding Gallery", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(gp, "Error sending rebuild command to web service.\nPlease try again later.\n" + ex, "Web Service Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        dispose();
    }

    private byte[] read(String file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fc = fis.getChannel();
        byte[] data = new byte[(int) fc.size()];
        ByteBuffer bb = ByteBuffer.wrap(data);
        fc.read(bb);
        return data;
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (mp != null) {
            handleOKForMP();
        }
        if (gp != null) {
            handleOKForGP();
        }
    }

    private javax.swing.JButton cancelButton;

    private javax.swing.JTextArea commentTextArea;

    private javax.swing.JLabel imageLabel;

    private javax.swing.JPanel infoPanel;

    private javax.swing.JButton nextButton;

    private javax.swing.JButton okButton;

    private javax.swing.JButton prevButton;

    private javax.swing.JScrollPane textScrollPane;
}
