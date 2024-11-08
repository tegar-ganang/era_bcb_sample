package com.lovehorsepower.imagemailerapp.dialogs;

import com.lovehorsepower.imagemailerapp.panels.MainTabPanel;
import com.lovehorsepower.imagemailerapp.workers.LoadGalleryThumbsWorker;
import com.lovehorsepower.imagemailerapp.workers.SendImagesWorker;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.jdesktop.application.Action;

/**
 *
 * @author  Joseph Obernberger
 */
public class GalleryDialog extends javax.swing.JDialog {

    public JScrollPane galleryScrollPane = null;

    public GridLayout gridLayout1 = new GridLayout();

    public String galleries[] = null;

    public String emailAddress = "";

    public ArrayList imageBeanList = null;

    public MainTabPanel mf = null;

    public DefaultMutableTreeNode rootNode = null;

    String categories[] = null;

    String categoriesList[] = null;

    String descriptionList[] = null;

    ArrayList catList = new ArrayList();

    JScrollPane treeScrollPane = null;

    JTree galleryTree = null;

    GalleryTreeRenderer gRenderer = null;

    String currentSelectedTreeNode = "";

    /** Creates new form GalleryDialog */
    public GalleryDialog(JFrame parent, String title, boolean modal, String galleries[], String categoriesList[], String emailAddress, ArrayList imageBeanList, String descriptionList[], java.util.List gDetails, MainTabPanel mf) {
        super(parent, title, modal);
        initComponents();
        this.galleries = galleries;
        this.emailAddress = emailAddress;
        this.imageBeanList = imageBeanList;
        this.categoriesList = categoriesList;
        this.descriptionList = descriptionList;
        this.mf = mf;
        Iterator it = null;
        int counter = 0;
        String detail = "";
        String category = "";
        System.out.println("Got gDetails: " + gDetails);
        if (gDetails != null) {
            it = gDetails.iterator();
            while (it != null && it.hasNext()) {
                detail = (String) it.next();
                if (detail.startsWith("$CAT$:")) {
                    category = detail.substring(detail.indexOf(":") + 1);
                    catList.add(category);
                }
            }
        }
        rootNode = new DefaultMutableTreeNode("<HTML><B>Galleries</B></HTML>");
        DefaultMutableTreeNode catNode = null;
        DefaultMutableTreeNode galNode = null;
        System.out.println("Catlist size is: " + catList.size());
        if (catList.size() > 0) {
            categories = new String[catList.size()];
            it = catList.iterator();
            counter = 0;
            while (it != null && it.hasNext()) {
                categories[counter] = new String((String) it.next());
                catNode = new DefaultMutableTreeNode("<HTML><B>" + categories[counter] + "</B></HTML>");
                for (int i = 0; i < galleries.length; i++) {
                    if (categoriesList[i].equals(categories[counter])) {
                        galNode = new DefaultMutableTreeNode(galleries[i]);
                        catNode.add(galNode);
                    }
                }
                rootNode.add(catNode);
                counter++;
            }
        }
        if (galleries != null) {
            for (int i = 0; i < galleries.length; i++) {
                if (categoriesList[i].equals("$$NO CAT$$")) {
                    rootNode.add(new DefaultMutableTreeNode(galleries[i]));
                }
            }
        }
        gRenderer = new GalleryTreeRenderer();
        galleryTree = new JTree(rootNode);
        galleryTree.setCellRenderer(gRenderer);
        for (int i = 0; i < galleryTree.getRowCount(); i++) {
            galleryTree.expandRow(i);
        }
        galleryTree.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {

            public void mouseMoved(java.awt.event.MouseEvent evt) {
                galleryTreeMouseMoved(evt);
            }
        });
        galleryTree.addMouseListener(new java.awt.event.MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                handleTreeMouseClick(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        treeScrollPane = new JScrollPane(galleryTree);
        this.centerPanel.add(treeScrollPane, BorderLayout.CENTER);
    }

    public void sendToGallery(String galleryName, String galleryType) {
        SendImagesWorker si = new SendImagesWorker(mf, galleryName, emailAddress, imageBeanList, galleryType);
        mf.startSendImages(si);
        dispose();
    }

    public void galleryButton_actionPerformed(ActionEvent actionEvent) {
        String galleryName = actionEvent.getActionCommand();
        sendToGallery(galleryName, "");
    }

    public byte[] read(String file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fc = fis.getChannel();
        byte[] data = new byte[(int) fc.size()];
        ByteBuffer bb = ByteBuffer.wrap(data);
        fc.read(bb);
        return data;
    }

    private void initComponents() {
        centerPanel = new javax.swing.JPanel();
        southPanel = new javax.swing.JPanel();
        makeNewButton = new javax.swing.JButton();
        changeOrderButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        centerPanel.setName("centerPanel");
        centerPanel.setLayout(new java.awt.BorderLayout());
        getContentPane().add(centerPanel, java.awt.BorderLayout.CENTER);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.lovehorsepower.imagemailerapp.ImageMailerApp.class).getContext().getResourceMap(GalleryDialog.class);
        southPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("southPanel.border.title")));
        southPanel.setName("southPanel");
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.lovehorsepower.imagemailerapp.ImageMailerApp.class).getContext().getActionMap(GalleryDialog.class, this);
        makeNewButton.setAction(actionMap.get("makeNewAction"));
        makeNewButton.setText(resourceMap.getString("makeNewButton.text"));
        makeNewButton.setName("makeNewButton");
        makeNewButton.setPreferredSize(new java.awt.Dimension(120, 23));
        southPanel.add(makeNewButton);
        changeOrderButton.setText(resourceMap.getString("changeOrderButton.text"));
        changeOrderButton.setName("changeOrderButton");
        changeOrderButton.setPreferredSize(new java.awt.Dimension(120, 23));
        changeOrderButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeOrderButtonActionPerformed(evt);
            }
        });
        southPanel.add(changeOrderButton);
        okButton.setText(resourceMap.getString("okButton.text"));
        okButton.setName("okButton");
        okButton.setPreferredSize(new java.awt.Dimension(120, 23));
        okButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        southPanel.add(okButton);
        cancelButton.setAction(actionMap.get("cancelAction"));
        cancelButton.setText(resourceMap.getString("cancelButton.text"));
        cancelButton.setName("cancelButton");
        cancelButton.setPreferredSize(new java.awt.Dimension(120, 23));
        southPanel.add(cancelButton);
        getContentPane().add(southPanel, java.awt.BorderLayout.SOUTH);
        pack();
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (currentSelectedTreeNode.startsWith("<HTML><B>")) {
            return;
        }
        if (currentSelectedTreeNode.length() == 0) {
            JOptionPane.showMessageDialog(this, "Please make a new gallery.", "No Category", JOptionPane.ERROR_MESSAGE);
            return;
        }
        sendToGallery(currentSelectedTreeNode, "");
    }

    private void changeOrderButtonActionPerformed(java.awt.event.ActionEvent evt) {
        OrderDialog orderDialog = null;
        orderDialog = new OrderDialog(this, "Change Image Order", true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = orderDialog.getSize();
        orderDialog.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        orderDialog.setVisible(true);
    }

    private void galleryTreeMouseMoved(java.awt.event.MouseEvent evt) {
        TreePath treePathLocation;
        Point loc = evt.getPoint();
        String mousedOverItem = "";
        String toolTipText = "";
        int index = 0;
        String htmlDescription = "";
        treePathLocation = galleryTree.getPathForLocation(loc.x, loc.y);
        if (treePathLocation != null) {
            mousedOverItem = "" + treePathLocation.getPath()[treePathLocation.getPath().length - 1];
            if (mousedOverItem.equals("<HTML><B>Galleries</B></HTML>")) {
                galleryTree.setToolTipText("");
                return;
            }
            for (int i = 0; i < categoriesList.length; i++) {
                if (mousedOverItem.equals(categoriesList[i])) {
                    galleryTree.setToolTipText("");
                    return;
                }
            }
            for (int i = 0; i < galleries.length; i++) {
                if (mousedOverItem.equals(galleries[i])) {
                    index = i;
                    break;
                }
            }
            htmlDescription = descriptionList[index].replaceAll("\n", "<BR>");
            toolTipText = "<html><B>" + mousedOverItem + "<BR>Description:</B>" + htmlDescription + "<BR><img src=\"http://www.lovehorsepower.com/galleries/" + Math.abs(emailAddress.hashCode()) + "/" + mousedOverItem + "/GalThumb/galThumb.jpg\"></html>";
            galleryTree.setToolTipText(toolTipText);
        }
    }

    public void doneLoadingThumbs() {
    }

    /**
     * buildGalleries
     */
    private void buildGalleries() {
        LoadGalleryThumbsWorker lg = new LoadGalleryThumbsWorker(this);
        lg.execute();
    }

    @Action
    public void cancelAction() {
        this.dispose();
    }

    @Action
    public void makeNewAction() {
        GalleryOptionsDialog galleryOptions = null;
        List serviceResultsList = null;
        Iterator it = null;
        String categories[] = null;
        String description = "";
        Timestamp created = null;
        String createdString = "";
        int counter = 0;
        Long createdLong = null;
        String cat = "";
        ArrayList categoryList = new ArrayList();
        String galleryType = "";
        try {
            com.lovehorsepower.emailimages.EIWSService service = new com.lovehorsepower.emailimages.EIWSService();
            com.lovehorsepower.emailimages.EIWS port = service.getEIWSPort();
            serviceResultsList = port.getGalleryDetails("", emailAddress);
            System.out.println("Got ServiceResultsList: " + serviceResultsList);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Web Service Error Loading Gallery Details.\nPlease try again later.\n" + ex, "Web Service Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        it = serviceResultsList.iterator();
        while (it != null && it.hasNext()) {
            if (counter == 0) {
                description = new String((String) it.next());
                if (description.startsWith("$CAT$:")) {
                    categoryList.add(description.substring(description.indexOf(":") + 1));
                    description = "";
                }
                if (description.startsWith("$TYPE$:")) {
                    galleryType = description.substring(description.indexOf(":") + 1);
                    description = "";
                }
                System.out.println("Got description: " + description);
            }
            if (counter == 1) {
                createdString = new String((String) it.next());
                if (createdString.startsWith("$CAT$:")) {
                    categoryList.add(createdString.substring(createdString.indexOf(":") + 1));
                    createdLong = new Long(System.currentTimeMillis());
                } else if (createdString.startsWith("$TYPE$:")) {
                    galleryType = createdString.substring(createdString.indexOf(":") + 1);
                } else {
                    createdLong = new Long(createdString);
                }
            }
            if (counter > 1) {
                cat = (String) it.next();
                if (cat.startsWith("$CAT$:")) {
                    categoryList.add(cat.substring(cat.indexOf(":") + 1));
                }
                if (cat.startsWith("$TYPE$:")) {
                    galleryType = cat.substring(cat.indexOf(":") + 1);
                }
            }
            counter++;
        }
        if (createdLong != null) {
            created = new Timestamp(createdLong.longValue());
        } else {
            created = new Timestamp(System.currentTimeMillis());
        }
        it = categoryList.iterator();
        if (categoryList.size() > 0) {
            categories = new String[categoryList.size()];
            counter = 0;
            while (it != null && it.hasNext()) {
                categories[counter] = new String((String) it.next());
                counter++;
            }
        }
        galleryOptions = new GalleryOptionsDialog(this, "Gallery Options", "", emailAddress, categories, created, description, galleryType, true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = galleryOptions.getSize();
        galleryOptions.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        galleryOptions.setVisible(true);
    }

    private javax.swing.JButton cancelButton;

    public javax.swing.JPanel centerPanel;

    private javax.swing.JButton changeOrderButton;

    private javax.swing.JButton makeNewButton;

    private javax.swing.JButton okButton;

    private javax.swing.JPanel southPanel;

    private String resizeImage(File filename) throws FileNotFoundException, IOException {
        Image image = Toolkit.getDefaultToolkit().getImage(filename.getAbsolutePath());
        MediaTracker mediaTracker = new MediaTracker(new Container());
        mediaTracker.addImage(image, 0);
        try {
            mediaTracker.waitForID(0);
        } catch (Exception ex) {
            System.out.println("Media tracker exception: " + ex);
        }
        int thumbWidth = 1024;
        int thumbHeight = 768;
        double thumbRatio = (double) thumbWidth / (double) thumbHeight;
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        double imageRatio = (double) imageWidth / (double) imageHeight;
        if (thumbRatio < imageRatio) {
            thumbHeight = (int) (thumbWidth / imageRatio);
        } else {
            thumbWidth = (int) (thumbHeight * imageRatio);
        }
        BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = thumbImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename.getName()));
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
        JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(thumbImage);
        int quality = 98;
        quality = Math.max(0, Math.min(quality, 100));
        param.setQuality((float) quality / 100.0f, false);
        encoder.setJPEGEncodeParam(param);
        encoder.encode(thumbImage);
        out.close();
        return filename.getName();
    }

    private void handleTreeMouseClick(MouseEvent e) {
        TreePath treePathLocation;
        Point loc = e.getPoint();
        treePathLocation = galleryTree.getPathForLocation(loc.x, loc.y);
        try {
            currentSelectedTreeNode = treePathLocation.getPath()[treePathLocation.getPath().length - 1].toString();
        } catch (Exception ex) {
            return;
        }
        if (e.getClickCount() != 2) {
            return;
        }
        if (treePathLocation != null) {
            if (treePathLocation.getPath() != null) {
                if (treePathLocation.getPath()[treePathLocation.getPath().length - 1].toString().startsWith("<HTML><B>")) {
                    return;
                } else {
                    sendToGallery(treePathLocation.getPath()[treePathLocation.getPath().length - 1].toString(), "");
                }
            }
        }
    }
}
