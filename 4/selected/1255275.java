package net.hanjava.alole.util;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.BorderFactory;
import javax.swing.JTextField;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentNode;
import org.apache.poi.poifs.filesystem.EntryNode;
import org.apache.poi.poifs.filesystem.POIFSDocumentPath;

public class Utility {

    private Utility() {
    }

    public static void copyStreamContents(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[1024];
        int readSize = -1;
        while ((readSize = is.read(buf)) > 0) {
            os.write(buf, 0, readSize);
        }
        is.close();
        os.close();
    }

    public static String getNodePath(EntryNode node) {
        String result = null;
        if (node instanceof DocumentNode) {
            DocumentNode docNode = (DocumentNode) node;
            DirectoryNode parentNode = (DirectoryNode) docNode.getParent();
            POIFSDocumentPath dirPath = parentNode.getPath();
            result = dirPath.toString() + '\\' + docNode.getName();
        } else if (node instanceof DirectoryNode) {
            DirectoryNode dirNode = (DirectoryNode) node;
            result = dirNode.getPath().toString();
        } else {
            throw new IllegalArgumentException("Unknown Node Type : " + node);
        }
        return result;
    }

    public static Component createLabel(String title, String text) {
        JTextField label = new JTextField(text);
        label.setEditable(false);
        label.setBorder(BorderFactory.createTitledBorder(title));
        return label;
    }
}
