package org.fpdev.util;

import java.awt.Dimension;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.fpdev.core.basenet.BNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Collection of static utility methods for use throughout the Five Points
 * project.
 * 
 * @author demory
 */
public abstract class FPUtil {

    public static final int AM = 1;

    public static final int PM = 2;

    public static double magnitude(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static Point2D.Double closestPointOnSegment(double x, double y, double x1, double y1, double x2, double y2) {
        double lineMag = FPUtil.magnitude(x1, y1, x2, y2);
        double u = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / (lineMag * lineMag);
        if (u > Double.MIN_VALUE && u <= 1) {
            double ix = x1 + u * (x2 - x1);
            double iy = y1 + u * (y2 - y1);
            return new Point2D.Double(ix, iy);
        } else if (u > 1) return new Point2D.Double(x2, y2); else return new Point2D.Double(x1, y1);
    }

    public static double distToSegment(double x, double y, double x1, double y1, double x2, double y2) {
        double lineMag = FPUtil.magnitude(x1, y1, x2, y2);
        double u = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / (lineMag * lineMag);
        if (u > Double.MIN_VALUE && u <= 1) {
            double ix = x1 + u * (x2 - x1);
            double iy = y1 + u * (y2 - y1);
            return FPUtil.magnitude(ix, iy, x, y);
        }
        return Double.MAX_VALUE;
    }

    /**
   * Returns theta in radians, representing the angle between the positive
   * x-axis and the segment extending to the point (x,y). Ranges from 0 to 2*pi.
   * 
   * @param x
   * @param y
   * @return theta (in radians)
   */
    public static double getTheta(double x, double y) {
        double h = Math.sqrt(x * x + y * y);
        double theta = Math.acos(Math.abs(x) / h);
        if (x <= 0 && y > 0) theta = Math.PI - theta;
        if (x < 0 && y <= 0) theta = Math.PI + theta;
        if (x >= 0 && y < 0) theta = 2 * Math.PI - theta;
        return theta;
    }

    public static String currentTimeStr() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss MM-dd-yyyy");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(cal.getTime());
    }

    public static String dtime(int sec) {
        return sec + "/" + sTimeToStr(sec) + (sec % 60);
    }

    public static String sTimeToStr(int sec) {
        return sTimeToStr(sec, false);
    }

    public static String sTimeToStr(int sec, boolean leadingZeroes) {
        int tmin = sec / 60;
        int hour = tmin / 60;
        int min = tmin % 60;
        String ampm = "";
        if (hour <= 7) {
            hour += 4;
            ampm = "a";
        } else if (hour == 8) {
            hour = 12;
            ampm = "p";
        } else if (hour > 8 && hour <= 19) {
            hour -= 8;
            ampm = "p";
        } else if (hour == 20) {
            hour = 12;
            ampm = "a";
        } else {
            hour -= 20;
            ampm = "a";
        }
        return (leadingZeroes && hour < 10 ? "0" : "") + hour + ":" + (min < 10 ? "0" : "") + min + ampm;
    }

    public static void setPreferredColumnWidths(JTable table, double[] percentages) {
        if (table.getColumnCount() != percentages.length) {
            System.out.println("Error: column count mismatch in FPUtil::setPreferredColumnWidths()");
            return;
        }
        Dimension tableDim = table.getPreferredSize();
        double total = 0;
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            total += percentages[i];
        }
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            int w = (int) (tableDim.width * (percentages[i] / total));
            column.setPreferredWidth(w);
        }
    }

    public static int strToSTime(String timeStr) {
        int hour = new Integer(timeStr.substring(0, 2)).intValue();
        int min = new Integer(timeStr.substring(3, 5)).intValue();
        char ampm = timeStr.charAt(5);
        if (hour == 0) {
            hour = 12;
        }
        if (ampm == 'a') {
            return getSTime(hour, min, AM);
        } else if (ampm == 'p') {
            return getSTime(hour, min, PM);
        }
        return -1;
    }

    public static int getSTime(int hour, int min, int ampm) {
        int time = -1;
        if (ampm == AM) {
            if (hour == 12) {
                time = 20 * 3600 + min * 60;
            } else if (hour < 4) {
                time = (20 + hour) * 3600 + min * 60;
            } else {
                time = (hour - 4) * 3600 + min * 60;
            }
        } else if (ampm == PM) {
            if (hour == 12) {
                time = 8 * 3600 + min * 60;
            } else {
                time = (8 + hour) * 3600 + min * 60;
            }
        }
        return time;
    }

    public static String elapsedTimeStr(int sec) {
        int tmin = sec / 60;
        int hour = tmin / 60;
        int min = tmin % 60;
        if (hour > 0) {
            return hour + " hr, " + min + " min";
        }
        return min + " min";
    }

    public static String distanceStr(int ft) {
        if (ft < 528) {
            return ft + " ft";
        }
        java.text.DecimalFormat df = new java.text.DecimalFormat();
        df.setMaximumFractionDigits(2);
        return df.format((double) (ft / 5280.0)) + " mi";
    }

    public static final int NE = 1;

    public static final int SE = 2;

    public static final int SW = 3;

    public static final int NW = 4;

    public static int getQuadrant(String streetName) {
        if (streetName.toLowerCase().endsWith(" nw")) {
            return NW;
        }
        if (streetName.toLowerCase().endsWith(" ne")) {
            return NE;
        }
        if (streetName.toLowerCase().endsWith(" sw")) {
            return SW;
        }
        if (streetName.toLowerCase().endsWith(" se")) {
            return SE;
        }
        return 0;
    }

    public static String getQuadrant(int quad) {
        switch(quad) {
            case NW:
                return "nw";
            case NE:
                return "ne";
            case SW:
                return "sw";
            case SE:
                return "se";
        }
        return "";
    }

    public static String stripQuadrant(String streetName) {
        if (streetName.toLowerCase().endsWith(" nw") || streetName.toLowerCase().endsWith(" ne") || streetName.toLowerCase().endsWith(" sw") || streetName.toLowerCase().endsWith(" se")) {
            return streetName.substring(0, streetName.length() - 3);
        }
        return streetName;
    }

    public static String standardizeStreetName(String streetName) {
        return standardizeStreetName(streetName, true);
    }

    public static String standardizeStreetName(String streetName, boolean stripQuad) {
        String str = stripQuad ? stripQuadrant(streetName.toLowerCase().trim()) : streetName.toLowerCase().trim();
        str = " " + str + " ";
        str = str.replaceAll(" street ", " st ");
        str = str.replaceAll(" road ", " rd ");
        str = str.replaceAll(" drive ", " dr ");
        str = str.replaceAll(" avenue", " ave ");
        str = str.replaceAll(" circle ", " cir ");
        str = str.replaceAll(" boulevard ", " blvd ");
        str = str.replaceAll(" connector ", " conn ");
        str = str.replaceAll(" parkway ", " pkwy ");
        str = str.replaceAll(" highway ", " hwy ");
        str = str.replaceAll(" freeway ", " fwy ");
        str = str.replaceAll(" trail ", " trl ");
        str = str.replaceAll(" terrace ", " ter ");
        str = str.replaceAll(" lane ", " ln ");
        str = str.replaceAll(" west ", " w ");
        str = str.replaceAll(" east ", " e ");
        str = str.replaceAll(" south ", " s ");
        str = str.replaceAll(" north ", " n ");
        str = str.replaceAll("\\.", "");
        str = str.replaceAll("'", "");
        str = str.replaceAll("-", " ");
        return str.trim();
    }

    public static boolean isValidStreetType(String type) {
        String t = type.trim().toLowerCase();
        if (t.compareTo("st") == 0 || t.compareTo("rd") == 0 || t.compareTo("dr") == 0 || t.compareTo("ave") == 0 || t.compareTo("blvd") == 0 || t.compareTo("cir") == 0 || t.compareTo("pkwy") == 0 || t.compareTo("pwy") == 0 || t.compareTo("hwy") == 0 || t.compareTo("fwy") == 0 || t.compareTo("frwy") == 0 || t.compareTo("conn") == 0 || t.compareTo("way") == 0 || t.compareTo("ln") == 0 || t.compareTo("pl") == 0 || t.compareTo("tr") == 0 || t.compareTo("ter") == 0 || t.compareTo("trl") == 0 || t.compareTo("ct") == 0) {
            return true;
        }
        return false;
    }

    public static int getAddressNumber(String addr) {
        int i = 0;
        while (addr.charAt(i) != ' ') {
            i++;
        }
        int number = new Integer(addr.substring(0, i)).intValue();
        return number;
    }

    public static String getAddressStreet(String addr) {
        int i = 0;
        while (addr.charAt(i) != ' ') {
            i++;
        }
        return addr.substring(i + 1, addr.length());
    }

    public static boolean nodeInRange(BNode node, double x1, double y1, double x2, double y2) {
        return (node.getX() >= x1 && node.getY() >= y1 && node.getX() <= x2 && node.getY() <= y2);
    }

    public static String encodeHtml(String text) {
        text = text.replaceAll("&", "&amp;");
        return text;
    }

    public static boolean isInteger(String str) {
        try {
            int i = Integer.parseInt(str, 10);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static boolean isDouble(String str) {
        try {
            double d = Double.parseDouble(str);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static String directionStr(double dx, double dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            return (dx < 0 ? "WB" : "EB");
        } else {
            return (dy < 0 ? "SB" : "NB");
        }
    }

    public static void readPropertiesFile(Properties props, String filename) {
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = docBuilder.parse(filename);
            NodeList docNodes = doc.getChildNodes();
            Node thisNode = docNodes.item(0);
            if (docNodes.getLength() != 1 && !thisNode.getNodeName().equals("properties")) {
                System.out.println("Not a valid properties file");
                return;
            }
            NodeList propNodes = thisNode.getChildNodes();
            for (int i = 0; i < propNodes.getLength(); i++) {
                Node propNode = propNodes.item(i);
                if (propNode.getNodeName().compareTo("property") == 0) {
                    String name = propNode.getAttributes().getNamedItem("name").getNodeValue();
                    System.out.println("prop name=" + name + " val=" + propNode.getTextContent());
                    props.setProperty(name, propNode.getTextContent());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void zipFile(String filename, ZipOutputStream zos, String zipname) {
        try {
            byte[] readBuffer = new byte[1024];
            int bytesIn = 0;
            File f = new File(filename);
            if (f.isDirectory()) {
                return;
            }
            FileInputStream fis = new FileInputStream(f);
            ZipEntry anEntry = new ZipEntry(zipname);
            zos.putNextEntry(anEntry);
            while ((bytesIn = fis.read(readBuffer)) != -1) {
                zos.write(readBuffer, 0, bytesIn);
            }
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void zipDir(String dirPath, ZipOutputStream zos, int truncLen) {
        zipDir(dirPath, zos, truncLen, "");
    }

    public static void zipDir(String dirPath, ZipOutputStream zos, int truncLen, String prefix) {
        try {
            File zipDir = new File(dirPath);
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[1024];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    zipDir(filePath, zos, truncLen, prefix);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                ZipEntry anEntry = new ZipEntry(prefix + f.getPath().substring(truncLen));
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean deleteDirectory(String path) {
        return deleteDirectory(new File(path));
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
        in.close();
        out.close();
    }

    public static final String fixFilename(String str) {
        String str2 = "";
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '/' || str.charAt(i) == '\\') {
                str2 += File.separator;
            } else {
                str2 += str.charAt(i);
            }
        }
        return str2;
    }

    public static Line2D.Double createNormalizedVector(Point2D p1, Point2D p2, double mult) {
        double len = FPUtil.magnitude(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        return new Line2D.Double(p1, new Point2D.Double(p1.getX() + mult * (p2.getX() - p1.getX()) / len, p1.getY() + mult * (p2.getY() - p1.getY()) / len));
    }
}
