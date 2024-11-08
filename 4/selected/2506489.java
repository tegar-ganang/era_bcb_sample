package tico.imageGallery.actions;

import java.awt.event.ActionEvent;
import java.util.Vector;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import com.sun.image.codec.jpeg.*;
import java.awt.image.*;
import java.awt.*;
import java.io.*;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import tico.components.resources.TFileUtils;
import tico.configuration.TLanguage;
import tico.editor.TEditor;
import tico.imageGallery.dataBase.TIGDataBase;

/**
 * Action which exists from the editor application.
 * 
 * @author Patricia M. Jaray
 * @version 2.2 Apr 2, 2008
 */
public class TIGInsertImageAction extends TIGAbstractAction {

    private static final int PREVIEW_WIDTH = 125;

    private static final int PREVIEW_HEIGHT = 125;

    private ImageIcon image;

    protected TIGDataBase dataBase;

    protected Vector theConcepts;

    protected String path;

    protected String directoryPath;

    protected String imageName;

    protected String imagePath;

    protected String imagePathThumb;

    protected String name;

    protected String myImagesBehaviour;

    /**
	 * Constructor for TEditorExitAction.
	 * 
	 * @param editor The boards' editor
	 */
    public TIGInsertImageAction(TEditor editor, Vector concepts, String image, TIGDataBase dataBase, String behaviour) {
        super(editor);
        theConcepts = (Vector) concepts.clone();
        path = image;
        myImagesBehaviour = behaviour;
        this.dataBase = dataBase;
    }

    public void actionPerformed(ActionEvent e) {
        if (path.compareTo("") != 0) {
            imageName = (path.substring(path.lastIndexOf(File.separator) + 1, path.length()));
            String name = imageName.substring(0, imageName.lastIndexOf('.'));
            String extension = imageName.substring(imageName.lastIndexOf('.') + 1, imageName.length());
            File imageFile = new File(path);
            directoryPath = "images" + File.separator + imageName.substring(0, 1).toUpperCase();
            File directory = new File(directoryPath);
            directory.mkdirs();
            imagePath = "." + File.separator + "images" + File.separator + imageName.substring(0, 1).toUpperCase() + File.separator + imageName;
            File newFile = new File(imagePath);
            if (myImagesBehaviour.equals(TLanguage.getString("TIGManageGalleryDialog.REPLACE_IMAGE"))) {
                Vector<Vector<String>> aux = TIGDataBase.imageSearchByName(name);
                if (aux.size() != 0) {
                    int idImage = TIGDataBase.imageKeySearchName(name);
                    TIGDataBase.deleteAsociatedOfImage(idImage);
                }
            }
            if (myImagesBehaviour.equals(TLanguage.getString("TIGManageGalleryDialog.ADD_IMAGE"))) {
                int i = 1;
                while (newFile.exists()) {
                    imagePath = "." + File.separator + "images" + File.separator + imageName.substring(0, 1).toUpperCase() + File.separator + imageName.substring(0, imageName.lastIndexOf('.')) + "_" + i + imageName.substring(imageName.lastIndexOf('.'), imageName.length());
                    name = name + "_" + i;
                    newFile = new File(imagePath);
                    i++;
                }
            }
            imagePathThumb = (imagePath.substring(0, imagePath.lastIndexOf("."))).concat("_th.jpg");
            imageName = name + "." + extension;
            try {
                FileChannel srcChannel = new FileInputStream(path).getChannel();
                FileChannel dstChannel = new FileOutputStream(imagePath).getChannel();
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                srcChannel.close();
                dstChannel.close();
            } catch (IOException exc) {
                System.out.println(exc.getMessage());
                System.out.println(exc.toString());
            }
            TIGDataBase.insertDB(theConcepts, imageName, imageName.substring(0, imageName.lastIndexOf('.')));
            image = null;
            if (imageFile != null) {
                if (TFileUtils.isJAIRequired(imageFile)) {
                    RenderedOp src = JAI.create("fileload", imageFile.getAbsolutePath());
                    BufferedImage bufferedImage = src.getAsBufferedImage();
                    image = new ImageIcon(bufferedImage);
                } else {
                    image = new ImageIcon(imageFile.getAbsolutePath());
                }
                if (image.getImageLoadStatus() == MediaTracker.ERRORED) {
                    int choosenOption = JOptionPane.NO_OPTION;
                    choosenOption = JOptionPane.showConfirmDialog(null, TLanguage.getString("TIGInsertImageAction.MESSAGE"), TLanguage.getString("TIGInsertImageAction.NAME"), JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
                } else {
                    createThumbnail();
                }
            }
        }
    }

    private void createThumbnail() {
        try {
            int thumbWidth = PREVIEW_WIDTH;
            int thumbHeight = PREVIEW_HEIGHT;
            double thumbRatio = (double) thumbWidth / (double) thumbHeight;
            int imageWidth = image.getIconWidth();
            int imageHeight = image.getIconHeight();
            double imageRatio = (double) imageWidth / (double) imageHeight;
            if (thumbRatio < imageRatio) {
                thumbHeight = (int) (thumbWidth / imageRatio);
            } else {
                thumbWidth = (int) (thumbHeight * imageRatio);
            }
            BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics2D = thumbImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.drawImage(image.getImage(), 0, 0, thumbWidth, thumbHeight, null);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(imagePathThumb));
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
            JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(thumbImage);
            int quality = 100;
            quality = Math.max(0, Math.min(quality, 100));
            param.setQuality(quality / 100.0f, false);
            encoder.setJPEGEncodeParam(param);
            encoder.encode(thumbImage);
            out.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.out.println(ex.toString());
        }
    }
}