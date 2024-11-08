package wsserver;

import java.io.BufferedReader;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Node;

public class Util {

    public static void errorDialog(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static int confirmDialog(String title, String message) {
        return JOptionPane.showConfirmDialog(null, message, title, JOptionPane.OK_CANCEL_OPTION);
    }

    public static String selectPath(int di) {
        JFrame frame = new JFrame();
        String fileName = File.separator + "xml";
        JFileChooser fc = new JFileChooser(new File(fileName));
        int f = 0;
        File file = null;
        do {
            switch(di) {
                case (0):
                    fc.showSaveDialog(frame);
                    file = fc.getSelectedFile();
                    try {
                        if (file.exists()) {
                            f = confirmDialog("file`s already exist", "do you want to overwrite it?");
                        }
                    } catch (java.lang.NullPointerException e) {
                        f = 0;
                    }
                    break;
                case (1):
                    fc.showOpenDialog(frame);
                    file = fc.getSelectedFile();
                    break;
                case (2):
                    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    fc.showOpenDialog(frame);
                    file = fc.getSelectedFile();
                    break;
            }
        } while (f != 0);
        String path = "";
        try {
            if (di != 0) {
                path += file.getAbsoluteFile();
            } else {
                path = file.getAbsolutePath() + ".xml";
            }
        } catch (java.lang.NullPointerException ex) {
        }
        return path;
    }

    public static File[] getIngredients(String directory) {
        File dir = new File(directory);
        FilenameFilter filterXML = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        };
        File[] files = dir.listFiles(filterXML);
        return files;
    }

    public static String readTextFile(String path) {
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            String str;
            while ((str = in.readLine()) != null) {
                sb.append(str);
            }
            in.close();
        } catch (IOException e) {
            return "";
        }
        return sb.toString();
    }

    public static String toXml(Node node) {
        try {
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(node);
            trans.transform(source, result);
            String xmlString = sw.toString();
            return xmlString;
        } catch (TransformerException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static Vector filter(Vector table, int column) {
        Vector temp = ((Vector) ((Vector) table.get(column)).get(0));
        for (int i = 1; i < ((Vector) table.get(column)).size(); i++) {
            Vector result = new Vector();
            for (int q = 0; q < table.size(); q++) {
                result.addElement(new Vector());
            }
            Vector temp1 = ((Vector) ((Vector) table.get(column)).get(i));
            for (int j = 0; j < temp.size(); j++) {
                if (temp1.contains(temp.get(j))) {
                    ((Vector) result.get(column)).add(temp.get(j));
                    for (int q = 0; q < result.size(); q++) {
                        if (q != column) {
                            ((Vector) result.get(q)).add(((Vector) ((Vector) table.get(q)).get(i)).get(j));
                        }
                    }
                }
            }
            temp = result;
            result.removeAllElements();
        }
        return temp;
    }
}
