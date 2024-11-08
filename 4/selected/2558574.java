package nyagua;

import components.ImageFileView;
import components.ImageFilter;
import components.ImagePreview;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import nyagua.data.Setting;
import org.xml.sax.SAXException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 *Utility and service routines
 *
 * @author Rudi Giacomini Pilon
 * @version 1.0
 */
public class Util {

    /**
     * calculates exponent in base n
     * 
     * @param base
     * @param exp
     * @return exponent
     */
    public static int integerPower(int base, int exp) {
        int x = 1;
        for (int i = 0; i < exp; i++) {
            x *= base;
        }
        return x;
    }

    /**
     * Check a keytyped event 
     * if non numeric key is pressed the event is discarded
     * 
     * @param evt
     * @return the event if numeric
     */
    public static java.awt.event.KeyEvent checkNumericKey(java.awt.event.KeyEvent evt) {
        char key = evt.getKeyChar();
        if (Character.isDigit(key) || key == ',' || key == '.' || key == '-') {
            return evt;
        } else if (evt.isActionKey()) {
            return evt;
        } else if (evt.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            return evt;
        } else if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
            return evt;
        } else {
            evt.consume();
            return evt;
        }
    }

    /**
     * GUI to chose an image file
     *
     * @return the file or null if aborted
     */
    public static BufferedImage LoadImage(JLabel parent) {
        BufferedImage image = null;
        JFileChooser fc = null;
        if (fc == null) {
            fc = new JFileChooser();
            fc.addChoosableFileFilter(new ImageFilter());
            fc.setAcceptAllFileFilterUsed(false);
            fc.setFileView(new ImageFileView());
            fc.setAccessory(new ImagePreview(fc));
        }
        int result = fc.showDialog(parent, java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("OK"));
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                image = ImageIO.read(fc.getSelectedFile());
                fc.setSelectedFile(null);
                return image;
            } catch (IOException ex) {
                fc.setSelectedFile(null);
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        } else {
            fc.setSelectedFile(null);
            return null;
        }
    }

    public static void SaveImage(BufferedImage bi, String fileName) {
        try {
            File outputfile = new File(fileName);
            ImageIO.write(bi, "jpg", outputfile);
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Loads an image from file, resize it if too big 
     * and then place it in the container
     * 
     * @param container a JLabel were to place the image
     */
    public static void ImageLoadResize(JLabel container, int sizeLimit) {
        BufferedImage image = null;
        image = Util.LoadImage(container);
        if (image != null) {
            int imgw = image.getWidth();
            int imgh = image.getHeight();
            double imgRatio = (double) imgw / (double) imgh;
            if (imgh > container.getHeight()) {
                image = Util.resize(image, (int) (imgRatio * container.getHeight()), container.getHeight());
            }
            if (image.getWidth() > container.getWidth()) {
                if (container.getWidth() > sizeLimit) {
                    image = Util.resize(image, sizeLimit, (int) (sizeLimit / imgRatio));
                } else {
                    image = Util.resize(image, container.getWidth(), (int) (container.getWidth() / imgRatio));
                }
            }
            if (imgh > container.getHeight()) {
                image = Util.resize(image, (int) (imgRatio * container.getHeight()), container.getHeight());
            }
            container.setIcon(new javax.swing.ImageIcon(image));
        }
    }

    /**
     * Converts an immage into a byte array
     * 
     * @param image the image to convert
     * @return a byte array of image
     */
    public static byte[] image_byte_data(BufferedImage image) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", out);
        } catch (IOException ex) {
            Logger.getLogger(ImageFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return out.toByteArray();
    }

    /**
     * Converts a byte array into an image
     * 
     * @param buffer the byte array to convert
     * @return the image 
     * @throws IOException
     */
    public static BufferedImage byte_image_data(byte[] buffer) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(buffer);
        return ImageIO.read(in);
    }

    /**
     * Resize an image
     * @param img   The image to be resized
     * @param newW  new width
     * @param newH  new height
     * @return  resized image
     */
    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dimg = dimg = new BufferedImage(newW, newH, img.getType());
        Graphics2D g = dimg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, newW, newH, 0, 0, w, h, null);
        g.dispose();
        return dimg;
    }

    /**
     * Clean an array of text fields
     * setting all text values to  ""
     * (empty string)
     *
     * @param jtfList the list of text fields
     */
    public static void CleanTextFields(JTextField[] jtfList) {
        for (int i = 0; i < jtfList.length; i++) {
            jtfList[i].setText("");
        }
    }

    /**
     * Detects the Operative System
     * 
     * @return a standard string that identifies the OS (not the version)     * 
     */
    public static String OS_Detect() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0) {
            return "win";
        } else if (os.indexOf("mac") >= 0) {
            return "mac";
        } else if (os.indexOf("nix") >= 0) {
            return "unix";
        } else if (os.indexOf("nux") >= 0) {
            return "linux";
        } else {
            return "n/a";
        }
    }

    /**
     * Show a message to select an aquarium id before to
     * execute any operation were this id is required
     */
    public static void msgSelectAquarium() {
        JOptionPane.showMessageDialog(null, java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("SELECT_AN_AQUARIUM_BEFORE..."), java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("INFORMATION"), JOptionPane.WARNING_MESSAGE);
    }

    /**
     * gets the current date
     * 
     * @return a string formatted date with format yyyy/MM/dd HH:mm:ss
     */
    public static String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date d = new java.util.Date();
        String datetime = dateFormat.format(d);
        return datetime;
    }

    /**
     * Call the browser defined in settings 
     * (default for firefox) and show the given file
     * 
     * @param fileName  the file to be shown
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    public static void BrowseDocument(String fileName) throws ClassNotFoundException, SQLException, IOException {
        String cmd = null;
        Setting s = Setting.getInstance();
        String browser = s.getBrowser();
        cmd = browser + " " + fileName;
        Process p = Runtime.getRuntime().exec(cmd);
    }

    /**
     * Plots the given document
     * (works only in GNU/Linux)
     * [requires gnuplot installed]
     * 
     * @param fileName  the file to be plotted
     * 
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    public static void PlotDocument(String fileName) throws ClassNotFoundException, SQLException, IOException {
        String cmd = "gnuplot -persist " + fileName;
        if (Util.OS_Detect().matches("win")) {
            cmd = "gnuplot  \"" + fileName + "\" -";
        }
        Process p = Runtime.getRuntime().exec(cmd);
        try {
            p.waitFor();
        } catch (InterruptedException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Imports xml file saved from fishbase.org
     *
     * @param file  the selected file
     * @param jtfList   the right fields where to import data
     * @throws ParserConfigurationException
     */
    public static void ImportXML(File file, JTextField[] jtfList) throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = null;
        try {
            doc = db.parse(file);
        } catch (SAXException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
        doc.getDocumentElement().normalize();
        NodeList nodeLst = doc.getElementsByTagName("taxon");
        for (int s = 0; s < nodeLst.getLength(); s++) {
            Node fstNode = nodeLst.item(s);
            jtfList[2].setText(Util.getaNodeValue(fstNode, "dwc:Family"));
            jtfList[3].setText(Util.getaNodeValue(fstNode, "dwc:ScientificName"));
            NodeList innernodes = doc.getElementsByTagName("dataObject");
            for (int t = 0; t < innernodes.getLength(); t++) {
                Node nextNode = innernodes.item(t);
                String nv = Util.getaNodeValue(nextNode, "dc:identifier");
                if (nv.startsWith("FB-Distribution-")) {
                    jtfList[4].setText(Util.getaNodeValue(nextNode, "dc:description"));
                } else if (nv.startsWith("FB-Uses-")) {
                    jtfList[10].setText(Util.getaNodeValue(nextNode, "dc:description"));
                } else if (nv.startsWith("FB-TrophicStrategy-")) {
                    jtfList[6].setText(Util.getaNodeValue(nextNode, "dc:description"));
                } else if (nv.startsWith("FB-Size-")) {
                    jtfList[8].setText(Util.getaNodeValue(nextNode, "dc:description"));
                } else if (nv.startsWith("FB-Habitat-")) {
                    String element = Util.getaNodeValue(nextNode, "dc:description");
                    int i = element.indexOf(";");
                    jtfList[7].setText(element.substring(0, i));
                    element = element.substring(i + 1, element.length());
                    i = element.indexOf("pH range:");
                    element = element.substring(i + 1, element.length());
                    i = element.indexOf(":");
                    element = element.substring(i + 1, element.length());
                    i = element.indexOf(";");
                    int y = element.indexOf("-");
                    String PHmin = element.substring(1, y - 1);
                    String PHmax = element.substring(y + 2, i);
                    jtfList[11].setText(PHmin);
                    jtfList[12].setText(PHmax);
                    i = element.indexOf("dH range:");
                    element = element.substring(i + 1, element.length());
                    i = element.indexOf(":");
                    element = element.substring(i + 1, element.length());
                    y = element.indexOf("-");
                    String dHmin = element.substring(1, y - 1);
                    String dHmax = element.substring(y + 2, element.length());
                    jtfList[13].setText(dHmin);
                    jtfList[14].setText(dHmax);
                }
            }
        }
        Element rootElement = doc.getDocumentElement();
        NodeList rowElements = rootElement.getElementsByTagName("commonName");
        for (int i = 0; i < rowElements.getLength(); i++) {
            Element row = (Element) rowElements.item(i);
            System.out.println(i);
            System.out.println(row.getNodeName());
            System.out.println(row.getAttribute("xml:lang"));
            if (row.getAttribute("xml:lang").trim().startsWith("eng")) {
                jtfList[1].setText(row.getTextContent());
                System.out.println(row.getTextContent());
            }
        }
    }

    /**
     * Retrivies a given XML node value
     * 
     * @param fstNode   the node 
     * @param item      the key
     * @return  the value for given key
     */
    public static String getaNodeValue(Node fstNode, String item) {
        String element = "";
        if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
            Element fstElmnt = (Element) fstNode;
            NodeList fstNmElmntLst = fstElmnt.getElementsByTagName(item);
            Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
            NodeList fstNm = fstNmElmnt.getChildNodes();
            if (fstNm.getLength() > 0) {
                element = ((Node) fstNm.item(0)).getNodeValue();
            }
        }
        return element;
    }

    /**
     * Calculates one of the values from the 
     * two others with the needed formula
     * 
     * @param tfKH  the KH value text field
     * @param tfPH  the PH value text field
     * @param tfCO  the CO2 value text field
     */
    public static void Calc_co2(JTextField tfKH, JTextField tfPH, JTextField tfCO) {
        int numpar = 3;
        if (tfKH.getText().isEmpty()) {
            numpar--;
        }
        if (tfPH.getText().isEmpty()) {
            numpar--;
        }
        if (tfCO.getText().isEmpty()) {
            numpar--;
        }
        if (numpar != 2) {
            JOptionPane.showMessageDialog(null, numpar + java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("_VALUES_IN_INPUT!__2_VALUES_NEEDED_FOR_CALCULATION"), java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("ERROR"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (tfKH.getText().isEmpty()) {
            double PH = Double.valueOf(LocUtil.delocalizeDouble(tfPH.getText()));
            double CO = Double.valueOf(LocUtil.delocalizeDouble(tfCO.getText()));
            double KH = 0;
            if (PH <= 0 || CO <= 0) {
                JOptionPane.showMessageDialog(null, java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("_PH_AND_CO2_MUST_BE_POSITIVE_VALUES"), java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("ERROR"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            KH = CO / 3;
            KH = KH / Math.pow(10, 7 - PH);
            if (Global.khunit.matches("ppm")) {
                KH = KH / 0.056;
            }
            tfKH.setText(LocUtil.localizeDouble(KH));
        }
        if (tfPH.getText().isEmpty()) {
            double PH = 0;
            double CO = Double.valueOf(LocUtil.delocalizeDouble(tfCO.getText()));
            double KH = Double.valueOf(LocUtil.delocalizeDouble(tfKH.getText()));
            if (KH <= 0 || CO <= 0) {
                JOptionPane.showMessageDialog(null, " KH and CO2 must be positive values", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (Global.khunit.matches("ppm")) {
                KH = KH * 0.056;
            }
            PH = 7.5 + Math.log10(KH) - Math.log10(CO);
            tfPH.setText(LocUtil.localizeDouble(PH));
        }
        if (tfCO.getText().isEmpty()) {
            double PH = Double.valueOf(LocUtil.delocalizeDouble(tfPH.getText()));
            double CO = 0;
            double KH = Double.valueOf(LocUtil.delocalizeDouble(tfKH.getText()));
            if (KH <= 0 || PH <= 0) {
                JOptionPane.showMessageDialog(null, " KH and PH must be positive values", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (Global.khunit.matches("ppm")) {
                KH = KH * 0.056;
            }
            CO = 3 * KH * Math.pow(10, 7 - PH);
            tfCO.setText(LocUtil.localizeDouble(CO));
        }
    }

    public static long DateDiff(Date start, Date end) {
        long diff = end.getTime() - start.getTime();
        diff = Math.abs(diff);
        return Math.round(diff / 86400000);
    }

    /**
     * Gets two dates from the related fields via
     * two global vars and creates a subquery for
     * the date interval
     *
     * @return a subquery for given dates
     */
    public static String getPeriod() {
        String qry = "";
        if (Global.dFrom.isEmpty()) {
            if (Global.dTo.isEmpty()) {
                return qry;
            } else {
                qry = " AND Date <= '" + Global.dTo + "' ";
                return qry;
            }
        } else {
            if (Global.dTo.isEmpty()) {
                qry = " AND Date >= '" + Global.dFrom + "' ";
                return qry;
            } else {
                qry = " AND Date >= '" + Global.dFrom + "' ";
                qry = qry + " AND Date <= '" + Global.dTo + "' ";
                return qry;
            }
        }
    }

    /**
     * Gets two dates from the related fields via
     * two global vars and creates a subquery for
     * the date interval considering only year and month
     * (not day)
     * 
     * @return  a subquery for given dates
     */
    public static String getMidPeriod() {
        String qry = "";
        if (Global.dFrom.isEmpty()) {
            if (Global.dTo.isEmpty()) {
                return qry;
            } else {
                String y = Global.dTo.substring(0, 4);
                String m = Global.dTo.substring(5, 7);
                qry = " AND y <= '" + y + "' ";
                qry = qry + " AND m <= '" + m + "' ";
                return qry;
            }
        } else {
            if (Global.dTo.isEmpty()) {
                String y = Global.dFrom.substring(0, 4);
                String m = Global.dFrom.substring(5, 7);
                qry = " AND y >= '" + y + "' ";
                qry = qry + " AND m >= '" + m + "' ";
                return qry;
            } else {
                String y = Global.dFrom.substring(0, 4);
                String m = Global.dFrom.substring(5, 7);
                qry = " AND y >= '" + y + "' ";
                qry = qry + " AND m >= '" + m + "' ";
                y = Global.dTo.substring(0, 4);
                m = Global.dTo.substring(5, 7);
                qry = qry + " AND y <= '" + y + "' ";
                qry = qry + " AND m <= '" + m + "' ";
                return qry;
            }
        }
    }

    /**
     * Show an error message box
     *
     * @param sMsg error text
     */
    public static void showErrorMsg(String sMsg) {
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(null, sMsg, java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("INPUT_ERROR"), JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Show a dialog box to allow directory selection
     *
     * @param theForm from which the dialog is called
     * @return  the directory selected
     */
    public static File directorySelector(JPanel theForm) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle(java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("SELECT_BACKUP_DIRECTORY"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(theForm) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    /**
     * Copy one file
     *
     * @param source    source file
     * @param dest      destination file
     * @throws IOException
     */
    public static void fileCopy(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    /**
     * Execute the backup of application data file
     * 
     * @param jp the pane to bind the window
     */
    public static void backupFile(JPanel jp) {
        File backupDir = Util.directorySelector(jp);
        if (backupDir != null) {
            if (backupDir.exists()) {
                String backupFileName = Application.NAME;
                if (!(DB.getCurrent().isEmpty())) {
                    backupFileName = DB.getCurrent();
                }
                int backupNum = 0;
                File backupFile = null;
                String backupFullFileName = "";
                do {
                    backupFullFileName = backupDir.getAbsolutePath() + Application.FS + backupFileName + backupNum + ".bak";
                    backupFile = new File(backupFullFileName);
                    backupNum++;
                } while (backupFile.exists());
                File source = new File(Global.FullFileName);
                try {
                    Util.fileCopy(source, backupFile);
                    JOptionPane.showMessageDialog(null, java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("BACKUP_COMPLETED."), java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("INFO"), JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("AN_ERROR_OCCURRED_") + java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("DURING_BACKUP."), java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("ERROR"), JOptionPane.ERROR_MESSAGE);
                }
            } else {
                Util.showErrorMsg(java.util.ResourceBundle.getBundle("nyagua/Bundle").getString("ERROR_INVALID_DIRECTORY"));
            }
        }
    }
}
