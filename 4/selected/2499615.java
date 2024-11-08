package de.cinek.rssview.images;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.ImageIcon;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Loads icons from a zip file. Extracts key to file mappings from an
 * xml file that has to be named "icons.xml". Make sure that no other
 * resources are present in the zip file. An Exception could occur.
 * Resources ending with ".txt" are ignored.
 * 
 * Place licens, user info, readme in files ending with .txt!
 * 
 * @author saintedlama
 */
public class ZipIconSet implements IconSet {

    private Map files;

    private Map keys;

    public ZipIconSet(String file) {
        files = new HashMap();
        keys = new HashMap();
        SAXBuilder builder = new SAXBuilder(false);
        try {
            ZipFile zipFile = new ZipFile(file);
            Enumeration entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if ("icons.xml".equals(entry.getName())) {
                    InputStream in = zipFile.getInputStream(entry);
                    Document doc = builder.build(in);
                    Element root = doc.getRootElement();
                    List icons = root.getChildren("icon");
                    for (Iterator iter = icons.iterator(); iter.hasNext(); ) {
                        Element iconElement = (Element) iter.next();
                        String key = iconElement.getAttributeValue("key");
                        String fileName = iconElement.getAttributeValue("file");
                        keys.put(key, fileName);
                    }
                } else if (entry.getName().endsWith(".txt")) {
                } else {
                    ImageIcon icon = loadImage(zipFile.getInputStream(entry));
                    files.put(entry.getName(), icon);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JDOMException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @param entry
	 * @throws IOException
	 */
    private ImageIcon loadImage(InputStream resourceIn) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = -1;
        while ((read = resourceIn.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        ImageIcon icon = new ImageIcon(out.toByteArray());
        resourceIn.close();
        return icon;
    }

    /**
	 * @see de.cinek.rssview.images.IconSet#getIcon(java.lang.String)
	 */
    public ImageIcon getIcon(String key) {
        Object file = keys.get(key);
        return (ImageIcon) files.get(file);
    }
}
