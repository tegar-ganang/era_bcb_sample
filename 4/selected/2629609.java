package com.bkoenig.photo.toolkit.utils;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import org.eclipse.swt.widgets.Group;
import org.jdom.CDATA;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;
import sun.misc.BASE64Encoder;

public class ObjectWebGallery extends Thread {

    private static final String NAME_CSS = "style.css";

    private static final String NAME_CACHE = "webcache.xml";

    private static final Document cache = new Document();

    private static String outputFolder = "./web/";

    public static int statsElementTotalCount = 0;

    public static int statsElementCompleted = 0;

    public static int statsElementSkipped = 0;

    public static long statsStartTimestamp = 0;

    public static String statsActualElementName = "";

    public ObjectWebGallery(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void run() {
        try {
            statsStartTimestamp = System.currentTimeMillis();
            create();
        } catch (Exception ex) {
            Logger.error(ex.getClass() + " " + ex.getMessage());
            for (int i = 0; i < ex.getStackTrace().length; i++) Logger.error("     " + ex.getStackTrace()[i].toString());
            ex.printStackTrace();
        }
    }

    public static void create() throws Exception {
        new File(outputFolder).mkdir();
        Element root = new Element("cache");
        cache.setRootElement(root);
        statsElementTotalCount = 3;
        statsElementTotalCount += (PhotoDB.photoWebFolders.length * 2);
        for (int i = 0; i < PhotoDB.photoWebFolders.length; i++) {
            statsElementTotalCount += (PhotoDB.photoWebFolderFiles.get(PhotoDB.photoWebFolders[i]).length * 3);
        }
        createCSS(outputFolder + NAME_CSS);
        createPage("My Photos", createContentIndex(PhotoDB.photoWebFolders, PhotoDB.photoWebFolderHashes, PhotoDB.photoWebFolderFiles, outputFolder), "", new File(outputFolder + "index.html"));
        createPage("My Photos", new Element("div").setText("[map]"), "", new File(outputFolder + "map.html"));
        createPages("My Photos", PhotoDB.photoWebFolders, PhotoDB.photoWebFolderHashes, PhotoDB.photoWebFolderFiles);
        File[] filesToCopy = new File[] { new File("objects/back.gif") };
        copyObjects(filesToCopy, outputFolder);
        XMLOutputter out = new XMLOutputter();
        out.setIndent("  ");
        out.setNewlines(true);
        out.setEncoding("iso-8859-1");
        out.output(cache, new FileOutputStream(new File(outputFolder + NAME_CACHE)));
        System.err.println(statsElementCompleted + " of " + statsElementTotalCount);
    }

    private static void createPages(String myTitle, String[] folderNames, String[] folderHashes, Hashtable<String, Photo[]> folderFiles) throws Exception {
        for (int i = 0; i < folderNames.length; i++) {
            String folderName = folderHashes[i];
            createPage("My Photos", createContentIndexFolder(folderFiles.get(folderNames[i])), folderNames[i], new File(outputFolder + folderName + ".html"));
            for (int j = 0; j < folderFiles.get(folderNames[i]).length; j++) {
                Photo tmp = folderFiles.get(folderNames[i])[j];
                String fileName = tmp.getImageHash() + ".html";
                String fileImgName = tmp.getImageHash() + "_full.jpg";
                Element img = new Element("img");
                img.setAttribute("src", fileImgName);
                img.setAttribute("alt", tmp.getImageName());
                resizeImage(tmp, outputFolder + fileImgName, 750, 600, false);
                createPage("My Photos", img, tmp.getImageName(), new File(outputFolder + fileName));
            }
        }
    }

    private static void createPage(String myTitle, Element content, String originalFileName, File outputFile) throws Exception {
        statsActualElementName = outputFile.getCanonicalPath();
        Element html = new Element("html");
        Document doc = new Document(html);
        Element head = new Element("head");
        html.addContent(head);
        Element title = new Element("title");
        title.setText(myTitle);
        head.addContent(title);
        Element meta = new Element("meta");
        meta.setAttribute("http-equiv", "Content-Type");
        meta.setAttribute("content", "text/html; charset=ISO-8859-1");
        head.addContent(meta);
        meta = new Element("meta");
        meta.setAttribute("name", "description");
        meta.setAttribute("content", "My Photos");
        head.addContent(meta);
        meta = new Element("meta");
        meta.setAttribute("name", "keywords");
        meta.setAttribute("content", "photos, icture, gallery, album");
        head.addContent(meta);
        Element link = new Element("link");
        link.setAttribute("rel", "stylesheet");
        link.setAttribute("type", "text/css");
        link.setAttribute("href", NAME_CSS);
        head.addContent(link);
        Element body = new Element("body");
        html.addContent(body);
        Element table = new Element("table");
        table.setAttribute("border", "0");
        table.setAttribute("cellpadding", "20");
        table.setAttribute("cellspacing", "0");
        table.setAttribute("width", "800");
        table.setAttribute("style", "background-color: #000000; border: #555555 solid 1px;");
        body.addContent(table);
        Element tr = new Element("tr");
        table.addContent(tr);
        Element td = new Element("td");
        td.setAttribute("align", "left");
        td.setAttribute("valign", "bottom");
        Element br = new Element("br");
        td.addContent(br);
        Element h1 = new Element("h1");
        h1.setText(myTitle);
        td.addContent(h1);
        tr.addContent(td);
        td = new Element("td");
        td.setAttribute("align", "right");
        td.setAttribute("valign", "bottom");
        if (outputFile.getCanonicalPath().endsWith("index.html")) {
            td.addContent("�bersicht");
        } else {
            Element a = new Element("a");
            a.setAttribute("href", "index.html");
            a.setAttribute("title", "�bersicht");
            a.setText("�bersicht");
            td.addContent(a);
        }
        td.addContent(" - ");
        if (outputFile.getCanonicalPath().endsWith("map.html")) {
            td.addContent("Karte");
        } else {
            Element a = new Element("a");
            a.setAttribute("href", "map.html");
            a.setAttribute("title", "Karte");
            a.setText("Karte");
            td.addContent(a);
        }
        tr.addContent(td);
        tr = new Element("tr");
        table.addContent(tr);
        td = new Element("td");
        td.setAttribute("colspan", "2");
        td.addContent(content);
        tr.addContent(td);
        tr = new Element("tr");
        table.addContent(tr);
        td = new Element("td");
        td.setAttribute("colspan", "2");
        td.setAttribute("align", "center");
        td.setAttribute("class", "bottom");
        td.addContent("� Bernhard K�nig - generated with ");
        Element a = new Element("a");
        a.setAttribute("class", "bottom");
        a.setAttribute("href", "http://photo-toolkit.geheimblog.de/");
        a.setAttribute("title", "Photo Toolkit project page");
        a.setAttribute("target", "_blank");
        a.setText("Photo Toolkit");
        td.addContent(a);
        tr.addContent(td);
        Element cacheFile = new Element("file");
        cacheFile.setAttribute("original", originalFileName);
        cacheFile.setAttribute("htmlfile", outputFile.getCanonicalPath());
        cacheFile.setAttribute("created", new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(System.currentTimeMillis())));
        cacheFile.setAttribute("timestamp", String.valueOf(System.currentTimeMillis()));
        cache.getRootElement().addContent(cacheFile);
        XMLOutputter out = new XMLOutputter();
        out.setIndent("  ");
        out.setNewlines(true);
        out.setEncoding("iso-8859-1");
        out.output(doc, new FileOutputStream(outputFile));
        statsElementCompleted++;
    }

    private static Element createContentIndex(String[] folderNames, String[] folderHashes, Hashtable<String, Photo[]> folderFiles, String outputFile) throws Exception {
        Element table = new Element("table");
        table.setAttribute("border", "0");
        table.setAttribute("cellpadding", "10");
        table.setAttribute("cellspacing", "0");
        Element tr = new Element("tr");
        for (int i = 0; i < folderNames.length; i++) {
            if (i % 5 == 0) {
                tr = new Element("tr");
                table.addContent(tr);
            }
            Element td = new Element("td");
            td.setAttribute("valign", "top");
            td.setAttribute("class", "bottom");
            String folderName = folderHashes[i];
            String folderImgName = folderHashes[i] + ".jpg";
            String[] names = Photo.generateFolderName(folderNames[i]);
            Element img = new Element("img");
            img.setAttribute("src", folderImgName);
            img.setAttribute("width", "120");
            img.setAttribute("height", "120");
            img.setAttribute("class", "large");
            img.setAttribute("alt", folderName);
            int imageNumber = folderFiles.get(folderNames[i]).length / 2;
            resizeImage(folderFiles.get(folderNames[i])[imageNumber], outputFile + folderImgName, 120, 120, true);
            Element a = new Element("a");
            a.setAttribute("href", folderName + ".html");
            a.setAttribute("title", folderNames[i]);
            a.addContent(img);
            td.addContent(a);
            Element br = new Element("br");
            td.addContent(br);
            a = new Element("a");
            a.setAttribute("href", folderName + ".html");
            a.setAttribute("title", folderNames[i]);
            a.setText(names[0]);
            td.addContent(a);
            br = new Element("br");
            td.addContent(br);
            if (names[1].length() > 10) {
                td.addContent(names[1].substring(0, 10));
                br = new Element("br");
                td.addContent(br);
            } else if (names[1].length() == 10) {
                td.addContent(names[1]);
                br = new Element("br");
                td.addContent(br);
            }
            td.addContent(folderFiles.get(folderNames[i]).length + " Photos");
            tr.addContent(td);
            if (i + 1 == folderNames.length) {
                for (int j = folderNames.length; j % 5 > 0; j++) {
                    td = new Element("td");
                    td.addContent(new Text("  "));
                    tr.addContent(td);
                }
            }
        }
        return table;
    }

    private static Element createContentIndexFolder(Photo[] folderNames) throws Exception {
        Element table = new Element("table");
        table.setAttribute("border", "0");
        table.setAttribute("cellpadding", "2");
        table.setAttribute("cellspacing", "0");
        boolean drawFillCell = true;
        Element tr = new Element("tr");
        for (int i = 0; i < folderNames.length; i++) {
            if (i % 5 == 0) {
                tr = new Element("tr");
                table.addContent(tr);
            }
            if (i == 0) {
                Element td = new Element("td");
                td.setAttribute("valign", "top");
                td.setAttribute("width", "50%");
                td.setAttribute("rowspan", String.valueOf(folderNames.length / 5 + 1));
                Element a = new Element("a");
                a.setAttribute("href", "index.html");
                a.addContent("Zur�ck zur �bersicht");
                td.addContent(a);
                Element br = new Element("br");
                td.addContent(br);
                td.addContent("  ");
                br = new Element("br");
                td.addContent(br);
                td.addContent(String.valueOf(folderNames.length) + " Photos");
                tr.addContent(td);
            }
            if (drawFillCell && (folderNames.length % 5 > 0) && folderNames.length - i < 5) {
                drawFillCell = false;
                for (int j = ((folderNames.length - i) % 5); j < 5; j++) {
                    Element td = new Element("td");
                    td.addContent(new Text("  "));
                    tr.addContent(td);
                }
            }
            Element td = new Element("td");
            td.setAttribute("width", "10%");
            String folderName = folderNames[i].getImageHash();
            String folderImgName = folderNames[i].getImageHash() + ".jpg";
            Element img = new Element("img");
            img.setAttribute("src", folderImgName);
            img.setAttribute("width", "110");
            img.setAttribute("height", "110");
            img.setAttribute("class", "small");
            img.setAttribute("alt", folderNames[i].getImageName());
            resizeImage(folderNames[i], outputFolder + folderImgName, 110, 110, true);
            Element a = new Element("a");
            a.setAttribute("href", folderName + ".html");
            a.addContent(img);
            td.addContent(a);
            tr.addContent(td);
        }
        return table;
    }

    private static void createCSS(String target) throws Exception {
        statsActualElementName = new File(target).getCanonicalPath();
        StringBuffer buffer = new StringBuffer();
        buffer.append("body { ");
        buffer.append("margin: 0; ");
        buffer.append("padding: 20px; ");
        buffer.append("background: #333333 url(\"back.gif\") repeat; ");
        buffer.append("color: #888888; ");
        buffer.append("font: normal 10px verdana, arial, tahoma, sans-serif; ");
        buffer.append("}\r\n");
        buffer.append("h1 { ");
        buffer.append("font-size: 22px; ");
        buffer.append("padding: 10px; ");
        buffer.append("display: inline; ");
        buffer.append("color: #CCCCCC; ");
        buffer.append("}\r\n");
        buffer.append("img.large { ");
        buffer.append("border: #BE690C solid 5px;");
        buffer.append("}\r\n");
        buffer.append("img.large:hover { ");
        buffer.append("border: #FFFFFF solid 5px; ");
        buffer.append("}\r\n");
        buffer.append("img.small { ");
        buffer.append("border: #BE690C solid 2px;");
        buffer.append("}\r\n");
        buffer.append("img.small:hover { ");
        buffer.append("border: #FFFFFF solid 2px; ");
        buffer.append("}\r\n");
        buffer.append("td { ");
        buffer.append("color: #888888; ");
        buffer.append("font-size: 12px; ");
        buffer.append("}\r\n");
        buffer.append("td.bottom { ");
        buffer.append("color: #888888; ");
        buffer.append("font-size: 10px; ");
        buffer.append("}\r\n");
        buffer.append("a { ");
        buffer.append("color: #BE690C; ");
        buffer.append("font-size: 12px; ");
        buffer.append("font-weight: bold; ");
        buffer.append("}\r\n");
        buffer.append("a:hover { ");
        buffer.append("color: #FFFFFF ");
        buffer.append("}\r\n");
        buffer.append("a.bottom { ");
        buffer.append("color: #888888; ");
        buffer.append("font-size: 10px; ");
        buffer.append("font-weight: normal; ");
        buffer.append("}\r\n");
        buffer.append("a.bottom:hover { ");
        buffer.append("color: #FFFFFF ");
        buffer.append("}\r\n");
        FileWriter fw = new FileWriter(target);
        fw.write(buffer.toString());
        fw.close();
        statsElementCompleted++;
    }

    public static void resizeImage(Photo photo, String fileOutputName, int maxWidth, int maxHeight, boolean crop) throws Exception {
        if (new File(fileOutputName).exists()) {
            Logger.debug("Resize for '" + photo.getImagePath() + "' alredy exists: " + new File(fileOutputName).getCanonicalPath());
            statsElementSkipped++;
            statsElementCompleted++;
        } else {
            statsActualElementName = photo.getImagePath();
            Image img = new ImageIcon(ImageIO.read(new File(photo.getImagePath()))).getImage();
            AffineTransform transform = null;
            int wNew = maxWidth;
            int hNew = maxHeight;
            boolean flip90 = false;
            if (photo.getImageFormat().equals("1")) transform = null; else if (photo.getImageFormat().equals("6")) {
                transform = AffineTransform.getRotateInstance(Math.toRadians(90), wNew / 2, hNew / 2);
                flip90 = true;
            } else if (photo.getImageFormat().equals("8")) {
                transform = AffineTransform.getRotateInstance(Math.toRadians(-90), wNew / 2, hNew / 2);
                flip90 = true;
            } else if (photo.getImageFormat().equals("3")) transform = AffineTransform.getRotateInstance(Math.toRadians(180), wNew / 2, hNew / 2); else throw new Exception("unknown Photo Orientation! Only 1,2,6 and 8 are valid values. Value is '" + photo.getImageFormat() + "'");
            Image scaledImage = null;
            if (crop) {
                if (img.getWidth(null) > img.getHeight(null) && !flip90) {
                    int tmpWidth = new Double(1d * img.getWidth(null) / img.getHeight(null) * wNew).intValue();
                    int tmpOffsetX = new Double(1d * (tmpWidth - wNew) / 2d).intValue();
                    scaledImage = new JFrame().createImage(new FilteredImageSource(img.getScaledInstance(tmpWidth, hNew, Image.SCALE_SMOOTH).getSource(), new CropImageFilter(tmpOffsetX, 0, wNew, hNew)));
                } else if (img.getWidth(null) > img.getHeight(null) && flip90) {
                    int tmpWidth = new Double(1d * img.getWidth(null) / img.getHeight(null) * wNew).intValue();
                    int tmpOffsetX = new Double(1d * (tmpWidth - wNew) / 2d).intValue();
                    scaledImage = new JFrame().createImage(new FilteredImageSource(img.getScaledInstance(tmpWidth, hNew, Image.SCALE_SMOOTH).getSource(), new CropImageFilter(tmpOffsetX, 0, wNew, hNew)));
                } else if (img.getWidth(null) < img.getHeight(null) && !flip90) {
                    int tmpHeight = new Double(1d * img.getHeight(null) / img.getWidth(null) * hNew).intValue();
                    int tmpOffsetY = new Double(1d * (tmpHeight - hNew) / 2d).intValue();
                    scaledImage = new JFrame().createImage(new FilteredImageSource(img.getScaledInstance(wNew, tmpHeight, Image.SCALE_SMOOTH).getSource(), new CropImageFilter(0, tmpOffsetY, wNew, hNew)));
                } else if (img.getWidth(null) < img.getHeight(null) && flip90) {
                } else {
                    scaledImage = img.getScaledInstance(wNew, hNew, Image.SCALE_SMOOTH);
                }
            } else {
                if (img.getWidth(null) > img.getHeight(null) && !flip90) {
                    int tmpHeight = new Double(1d * img.getHeight(null) / img.getWidth(null) * wNew).intValue();
                    hNew = tmpHeight;
                    scaledImage = new JFrame().createImage(new FilteredImageSource(img.getScaledInstance(wNew, tmpHeight, Image.SCALE_SMOOTH).getSource(), new CropImageFilter(0, 0, wNew, hNew)));
                } else if (img.getWidth(null) > img.getHeight(null) && flip90) {
                    int tmpHeight = new Double(1d * img.getHeight(null) / img.getWidth(null) * hNew).intValue();
                    hNew = tmpHeight;
                    scaledImage = img.getScaledInstance(tmpHeight, wNew, Image.SCALE_SMOOTH);
                } else if (img.getWidth(null) < img.getHeight(null) && !flip90) {
                    int tmpWidth = new Double(1d * img.getWidth(null) / img.getHeight(null) * hNew).intValue();
                    wNew = tmpWidth;
                    scaledImage = new JFrame().createImage(new FilteredImageSource(img.getScaledInstance(tmpWidth, hNew, Image.SCALE_SMOOTH).getSource(), new CropImageFilter(0, 0, wNew, hNew)));
                } else if (img.getWidth(null) < img.getHeight(null) && flip90) {
                } else {
                    scaledImage = img.getScaledInstance(wNew, hNew, Image.SCALE_SMOOTH);
                }
            }
            BufferedImage outImg = new BufferedImage(wNew, hNew, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = (Graphics2D) outImg.getGraphics();
            g2.drawImage(scaledImage, transform, null);
            g2.dispose();
            ImageIO.write(outImg, "jpeg", new File(fileOutputName));
            statsElementCompleted++;
        }
    }

    private static void copyObjects(File[] source, String target) {
        for (int i = 0; i < source.length; i++) {
            try {
                File inputFile = source[i];
                File outputFile = new File(target + source[i].getName());
                FileReader in = new FileReader(inputFile);
                FileWriter out = new FileWriter(outputFile);
                int c;
                while ((c = in.read()) != -1) out.write(c);
                in.close();
                out.close();
            } catch (Exception ex) {
                Logger.error(ex.getClass() + " " + ex.getMessage());
                for (int j = 0; j < ex.getStackTrace().length; j++) Logger.error("     " + ex.getStackTrace()[j].toString());
                ex.printStackTrace();
            }
        }
    }
}
