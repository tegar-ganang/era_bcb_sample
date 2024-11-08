package org.plugin.ig.components;

import java.awt.Graphics2D;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.Vector;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.swing.ImageIcon;
import tico.components.resources.TFileUtils;
import org.plugin.ig.actions.TIGModifyImageAction;
import org.plugin.ig.dialogs.TIGNewImageDataDialog;
import org.plugin.ig.db.TIGDataBase;
import tico.editor.TEditor;
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
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class TIGExportTask {

    private int lengthOfTask;

    private int current = 0;

    private String statMessage;

    private TEditor myEditor;

    private TIGDataBase myDataBase;

    private String myDirectoryPath;

    private boolean stop = false;

    private Document doc;

    private TIGExportTask myTask = this;

    public TIGExportTask() {
        lengthOfTask = 1000;
    }

    public void go(TEditor editor, TIGDataBase dataBase, String directoryPath, String filePath) {
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

        public ActualTask(TEditor editor, TIGDataBase dataBase, String directoryPath) {
            File myDirectory = new File(directoryPath);
            int i;
            Vector images = new Vector();
            images = dataBase.allImageSearch();
            lengthOfTask = images.size() * 2;
            String directory = directoryPath + "Images" + myDirectory.separator;
            File newDirectoryFolder = new File(directory);
            newDirectoryFolder.mkdirs();
            try {
                DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
                doc = domBuilder.newDocument();
            } catch (Exception exc) {
                System.out.println(exc.getMessage());
                System.out.println(exc.toString());
            }
            Element dbElement = doc.createElement("dataBase");
            for (i = 0; ((i < images.size()) && !stop); i++) {
                current = i;
                String element = (String) images.elementAt(i);
                String pathSrc = "Images" + File.separator + element.substring(0, 1).toUpperCase() + File.separator + element;
                String name = pathSrc.substring(pathSrc.lastIndexOf(myDirectory.separator) + 1, pathSrc.length());
                String pathDst = directory + name;
                try {
                    FileChannel srcChannel = new FileInputStream(pathSrc).getChannel();
                    FileChannel dstChannel = new FileOutputStream(pathDst).getChannel();
                    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                    srcChannel.close();
                    dstChannel.close();
                } catch (IOException exc) {
                    System.out.println(exc.getMessage());
                    System.out.println(exc.toString());
                }
                Vector keyWords = new Vector();
                keyWords = dataBase.asociatedConceptSearch((String) images.elementAt(i));
                Element imageElement = doc.createElement("image");
                Element imageNameElement = doc.createElement("name");
                imageNameElement.appendChild(doc.createTextNode(name));
                imageElement.appendChild(imageNameElement);
                for (int j = 0; j < keyWords.size(); j++) {
                    Element keyWordElement = doc.createElement("keyWord");
                    keyWordElement.appendChild(doc.createTextNode((String) keyWords.elementAt(j)));
                    imageElement.appendChild(keyWordElement);
                }
                dbElement.appendChild(imageElement);
            }
            try {
                doc.appendChild(dbElement);
                File dst = new File(directory.concat("Images"));
                BufferedWriter bufferWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dst), "UTF-8"));
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(bufferWriter);
                transformer.transform(source, result);
                bufferWriter.close();
            } catch (Exception exc) {
                System.out.println(exc.getMessage());
                System.out.println(exc.toString());
            }
            current = lengthOfTask;
        }
    }
}
