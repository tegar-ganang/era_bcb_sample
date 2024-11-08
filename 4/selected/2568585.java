package Modelo;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.net.URLEncoder;
import org.w3c.dom.Node;

public class Util {

    private static String ingredientsDirectory = "ingredients/";

    private static String howtosDirectory = "howtos/";

    private static String server;

    public static void errorDialog(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void informDialog(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static int confirmDialog(String title, String message) {
        return JOptionPane.showConfirmDialog(null, message, title, JOptionPane.OK_CANCEL_OPTION);
    }

    public static String getIP() {
        InetAddress ia = null;
        try {
            ia = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String chunk = ia.toString();
        String[] host = new String[2];
        host = chunk.split("/");
        return host[1];
    }

    public static String getUser() {
        InetAddress ia = null;
        try {
            ia = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String chunk = ia.toString();
        String[] host = new String[2];
        host = chunk.split("/");
        return host[0];
    }

    public static int PATH_GRABAR = 0;

    public static int PATH_INGRED_DIR = 1;

    public static int PATH_FOLDER_SELECT = 2;

    public static int PATH_HOWTO_DIR = 3;

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
                    fc = new JFileChooser(new File(Util.getIngredientsDirectory()));
                    fc.showOpenDialog(frame);
                    file = fc.getSelectedFile();
                    break;
                case (2):
                    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    fc.showOpenDialog(frame);
                    file = fc.getSelectedFile();
                    break;
                case (3):
                    fc = new JFileChooser(new File(Util.getHowtosDirectory()));
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

    public static void setServer(String server) {
        File dir = new File("lib");
        File archivo;
        Util.server = server;
        try {
            if (!dir.isDirectory()) {
                dir.mkdir();
            }
            archivo = new File(dir, "server.conf");
            FileOutputStream f = new FileOutputStream(archivo);
            DataOutputStream escribir = new DataOutputStream(f);
            escribir.writeUTF(Util.server);
        } catch (Exception e) {
        }
    }

    public static void readServer() {
        File archivo;
        File dir = new File("lib");
        if (dir.isDirectory()) {
            try {
                archivo = new File(dir, "server.conf");
                FileInputStream f = new FileInputStream(archivo);
                DataInputStream leer = new DataInputStream(f);
                Util.server = leer.readUTF();
            } catch (Exception e) {
                Util.errorDialog("Error", "The server has not yet been configured.");
            }
        } else {
            Util.errorDialog("Error", "Error reading server configuration file");
        }
    }

    public static String getServer() {
        return Util.server;
    }

    public static File[] getIngredients() {
        File dir = new File(Util.getIngredientsDirectory());
        FilenameFilter filterXML = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        };
        File[] files = dir.listFiles(filterXML);
        return files;
    }

    public static String getIngredientsDirectory() {
        File dir = new File(Util.ingredientsDirectory);
        try {
            if (!dir.isDirectory()) {
                dir.mkdir();
            }
        } catch (Exception e) {
        }
        return Util.ingredientsDirectory;
    }

    public static String getHowtosDirectory() {
        File dir = new File(Util.howtosDirectory);
        try {
            if (!dir.isDirectory()) {
                dir.mkdir();
            }
        } catch (Exception e) {
        }
        return Util.howtosDirectory;
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

    public static String node2String(Node node) {
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
        Vector temp = new Vector();
        for (int i = 0; i < table.size(); i++) {
            temp.addElement(((Vector) ((Vector) table.get(0)).get(i)));
        }
        for (int i = 1; i < ((Vector) table.get(0)).size(); i++) {
            Vector result = new Vector();
            for (int q = 0; q < table.size(); q++) {
                result.addElement(new Vector());
            }
            Vector temp1 = ((Vector) ((Vector) table.get(i)).get(column));
            for (int j = 0; j < ((Vector) temp.get(column)).size(); j++) {
                if (temp1.contains(((Vector) temp.get(column)).get(j))) {
                    ((Vector) result.get(column)).add(((Vector) temp.get(column)).get(j));
                    for (int q = 0; q < result.size(); q++) {
                        if (q != column) {
                            ((Vector) result.get(q)).add(((Vector) ((Vector) table.get(q)).get(i)).get(j));
                        }
                    }
                }
            }
            temp = (Vector) result.clone();
            result.removeAllElements();
        }
        return temp;
    }

    public static void transformar(File file) {
    }
}
