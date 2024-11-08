package org.plugin.ig.components;

import java.awt.Graphics2D;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Vector;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import tico.components.resources.TFileUtils;
import org.plugin.ig.actions.TIGModifyImageAction;
import org.plugin.ig.dialogs.TIGNewImageDataDialog;
import org.plugin.ig.db.TIGDataBase;
import tico.editor.TEditor;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class TIGImportTask {

    private int lengthOfTask;

    private int current = 0;

    private String statMessage;

    private TEditor myEditor;

    private TIGDataBase myDataBase;

    private String myDirectoryPath;

    private boolean stop = false;

    private File imageFile;

    private TIGImportTask myTask = this;

    public TIGImportTask() {
        lengthOfTask = 1000;
    }

    public void go(TEditor editor, TIGDataBase dataBase, String directoryPath) {
        current = 0;
        this.myEditor = editor;
        this.myDataBase = dataBase;
        this.myDirectoryPath = directoryPath;
        final SwingWorker worker = new SwingWorker() {

            public Object construct() {
                return new ActualTask(myEditor, myDataBase, myDirectoryPath);
            }
        };
    }

    public int getLengthOfTask() {
        return lengthOfTask;
    }

    public void setLengthOfTask(int num) {
        lengthOfTask = num;
    }

    public int getCurrent() {
        return current;
    }

    public void stop() {
        stop = true;
    }

    public boolean done() {
        if (current >= lengthOfTask) return true; else return false;
    }

    public String getMessage() {
        return statMessage;
    }

    public class ActualTask {

        private static final int PREVIEW_WIDTH = 125;

        private static final int PREVIEW_HEIGHT = 125;

        public ActualTask(TEditor editor, TIGDataBase dataBase, String directoryPath) {
            File myDirectory = new File(directoryPath);
            String[] list = myDirectory.list();
            int i;
            for (i = 0; ((i < list.length) && !stop); i++) {
                current = i;
                if ((list[i].compareTo("Images") != 0) && ((list[i].substring(list[i].lastIndexOf('.'), list[i].length()).toLowerCase().compareTo(".jpg") == 0) || (list[i].substring(list[i].lastIndexOf('.'), list[i].length()).toLowerCase().compareTo(".bmp") == 0) || (list[i].substring(list[i].lastIndexOf('.'), list[i].length()).toLowerCase().compareTo(".png") == 0))) {
                    String name = list[i];
                    String pathSrc = directoryPath.concat(list[i]);
                    name = name.replace(' ', '_').replace(',', '-').replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u').replace('Á', 'A').replace('É', 'E').replace('Í', 'I').replace('Ó', 'O').replace('Ú', 'U');
                    String pathDst = directoryPath.concat(name);
                    Vector aux = new Vector();
                    aux = dataBase.imageSearch(name.substring(0, name.lastIndexOf('.')));
                    if (aux.size() != 0) pathDst = pathDst.substring(0, pathDst.lastIndexOf('.')) + '_' + aux.size() + ".png";
                    File src = new File(pathSrc);
                    File absPath = new File("");
                    String nameSrc = '.' + src.separator + "Images" + src.separator + name.substring(0, 1).toUpperCase() + src.separator + pathDst.substring(pathDst.lastIndexOf(src.separator) + 1, pathDst.length());
                    String newDirectory = '.' + src.separator + "Images" + src.separator + name.substring(0, 1).toUpperCase();
                    String imagePathThumb = (nameSrc.substring(0, nameSrc.lastIndexOf("."))).concat("_th.jpg");
                    ImageIcon image = null;
                    if (src != null) {
                        if (TFileUtils.isJAIRequired(src)) {
                            RenderedOp src_aux = JAI.create("fileload", src.getAbsolutePath());
                            BufferedImage bufferedImage = src_aux.getAsBufferedImage();
                            image = new ImageIcon(bufferedImage);
                        } else {
                            image = new ImageIcon(src.getAbsolutePath());
                        }
                        if (image.getImageLoadStatus() == MediaTracker.ERRORED) {
                            System.out.print("Error al insertar imagen: ");
                            System.out.println(pathDst);
                        } else {
                            int option = 0;
                            imageFile = new File(directoryPath + "Images");
                            if (!imageFile.exists()) {
                                TIGNewImageDataDialog dialog = new TIGNewImageDataDialog(editor, dataBase, image, nameSrc.substring(nameSrc.lastIndexOf(File.separator) + 1, nameSrc.length()), list[i].substring(0, list[i].lastIndexOf('.')), myTask);
                                option = dialog.getOption();
                                if (option != 0) {
                                    File newDirectoryFolder = new File(newDirectory);
                                    newDirectoryFolder.mkdirs();
                                    try {
                                        FileChannel srcChannel = new FileInputStream(pathSrc).getChannel();
                                        FileChannel dstChannel = new FileOutputStream(nameSrc).getChannel();
                                        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                                        srcChannel.close();
                                        dstChannel.close();
                                    } catch (IOException exc) {
                                        System.out.println(exc.getMessage());
                                        System.out.println(exc.toString());
                                    }
                                }
                            }
                            if (imageFile.exists()) {
                                dataBase.insertImageDB(list[i].substring(0, list[i].lastIndexOf('.')), nameSrc.substring(nameSrc.lastIndexOf(File.separator) + 1, nameSrc.length()));
                                File newDirectoryFolder = new File(newDirectory);
                                newDirectoryFolder.mkdirs();
                                try {
                                    FileChannel srcChannel = new FileInputStream(pathSrc).getChannel();
                                    FileChannel dstChannel = new FileOutputStream(nameSrc).getChannel();
                                    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                                    srcChannel.close();
                                    dstChannel.close();
                                } catch (IOException exc) {
                                    System.out.println(exc.getMessage());
                                    System.out.println(exc.toString());
                                }
                            }
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
                                param.setQuality((float) quality / 100.0f, false);
                                encoder.setJPEGEncodeParam(param);
                                encoder.encode(thumbImage);
                                out.close();
                            } catch (Exception ex) {
                                System.out.println(ex.getMessage());
                                System.out.println(ex.toString());
                            }
                        }
                    }
                }
            }
            if (imageFile.exists() && !stop) {
                try {
                    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                    Document doc = docBuilder.parse(imageFile);
                    Element dataBaseElement = doc.getDocumentElement();
                    if (dataBaseElement.getTagName().equals("dataBase")) {
                        NodeList imageNodeList = dataBaseElement.getElementsByTagName("image");
                        for (int j = 0; j < imageNodeList.getLength(); j++) {
                            current++;
                            Node imageNode = imageNodeList.item(j);
                            NodeList lista = imageNode.getChildNodes();
                            Node nameNode = imageNode.getChildNodes().item(0);
                            String imageName = nameNode.getChildNodes().item(0).getNodeValue();
                            int imageKey = dataBase.imageKeySearchName(imageName.substring(0, imageName.lastIndexOf('.')));
                            if (imageKey != -1) {
                                for (int k = 1; k < imageNode.getChildNodes().getLength(); k++) {
                                    Node keyWordNode = imageNode.getChildNodes().item(k);
                                    String keyWord = keyWordNode.getChildNodes().item(0).getNodeValue();
                                    int conceptKey = dataBase.conceptKeySearch(keyWord);
                                    if (conceptKey == -1) {
                                        dataBase.insertConceptDB(keyWord);
                                        conceptKey = dataBase.conceptKeySearch(keyWord);
                                    }
                                    dataBase.insertAsociatedDB(conceptKey, imageKey);
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    System.out.println(ex.toString());
                }
            }
            current = lengthOfTask;
        }
    }
}
