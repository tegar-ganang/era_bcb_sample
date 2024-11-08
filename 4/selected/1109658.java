package org.maveryx.jRobot.apparams;

import java.awt.Component;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileLock;
import javax.swing.JOptionPane;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;

public class FileUtility {

    protected String projectExtension, projectDescription;

    public static int YES_PRESSED = JOptionPane.YES_OPTION, NO_PRESSED = JOptionPane.NO_OPTION, CANCEL_PRESSED = JOptionPane.CANCEL_OPTION, OK_PRESSED = JOptionPane.OK_OPTION;

    public static String TAG_TILDE = "~";

    public static String TAG_TMP_SUFFIX = ".tmp";

    public static final String TAG_APPLICATION = "APPLICATION";

    protected FileLock fileLock = null;

    public static Component parentComponent = null;

    public FileUtility() {
    }

    public static Document xslTransform(Document in, File stSheet) throws TransformerException {
        if (stSheet == null) return in;
        Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(stSheet));
        JDOMResult out = new JDOMResult();
        transformer.transform(new JDOMSource(in), out);
        return out.getDocument();
    }

    public static Document xslTransform(Document in, InputStream stSheet) throws TransformerException {
        if (stSheet == null) return in;
        Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(stSheet));
        JDOMResult out = new JDOMResult();
        transformer.transform(new JDOMSource(in), out);
        return out.getDocument();
    }

    public static int showYesNoCancelDialog(String msg, String title, Component comp, boolean beep) {
        return showDialog(msg, title, comp, beep, JOptionPane.YES_NO_CANCEL_OPTION);
    }

    public static int showYesNoCancelDialog(String msg, String title) {
        return showYesNoCancelDialog(msg, title, parentComponent, false);
    }

    public static int showYesNoDialog(String msg, String title, Component comp, boolean beep) {
        return showDialog(msg, title, comp, beep, JOptionPane.YES_NO_OPTION);
    }

    public static int showYesNoDialog(String msg, String title) {
        return showYesNoDialog(msg, title, parentComponent, false);
    }

    public static void showErrorDialog(String msg, String title, Component comp, boolean beep) {
        if (beep) Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(comp, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showErrorDialog(String message, String title) {
        showErrorDialog(message, title, parentComponent, false);
    }

    public static void showMessageDialog(String msg, String title) {
        showMessageDialog(msg, title, parentComponent, false);
    }

    public static void showMessageDialog(String msg, String title, Component comp, boolean beep) {
        showMessageDialog(msg, title, comp, beep, JOptionPane.PLAIN_MESSAGE);
    }

    public static void showWarningDialog(String msg, String title) {
        showWarningDialog(msg, title, parentComponent, false);
    }

    public static void showWarningDialog(String msg, String title, Component comp, boolean beep) {
        showMessageDialog(msg, title, comp, beep, JOptionPane.WARNING_MESSAGE);
    }

    public static void showMessageDialog(String msg, String title, Component comp, boolean beep, int severity) {
        if (beep) Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(comp, msg, title, severity);
    }

    public static int showDialog(String msg, String title, Component component, boolean beep, int mode) {
        if (beep) Toolkit.getDefaultToolkit().beep();
        return JOptionPane.showConfirmDialog(component, msg, title, mode);
    }

    public static int askExistingFile(File file, Component c) {
        return existFile(file) ? showYesNoCancelDialog("File " + file.getName() + " already exists: overwrite it?", "Warning", c, true) : OK_PRESSED;
    }

    public static boolean existFile(File f) {
        return f != null && f.exists();
    }

    public static File openFile(String fileName, String path) {
        return new File(fileName, path);
    }

    public static String addExtension(String name, String extension) {
        return name += name.endsWith("." + extension) ? "" : "." + extension;
    }

    public static Element getRootElement(InputStream is) throws Exception {
        return getRootElement(getDocument(is), (File) null, (File) null);
    }

    public static Element getRootElement(InputStream is, File dtdFile) throws Exception {
        return getRootElement(getDocument(is), dtdFile, null);
    }

    public static Element getRootElement(InputStream is, File dtdFile, File xslFile) throws Exception {
        return getRootElement(getDocument(is), dtdFile, xslFile);
    }

    public static Element getRootElement(File xml, File dtd, File xsl) throws Exception {
        return getRootElement(getDocument(xml), dtd, xsl);
    }

    public static Element getRootElement(Document doc, File dtd, File xsl) throws Exception {
        return getRootElement(doc, dtd != null ? dtd.getName() : null, xsl);
    }

    public static Element getRootElement(Document doc, String dtdPathName, File xsl) throws Exception {
        File appFile = null;
        try {
            if (dtdPathName != null && !dtdPathName.equals("")) {
                appFile = new File("app");
                doc.setDocType(new DocType(doc.getRootElement().getName(), dtdPathName));
                FileOutputStream out = new FileOutputStream(appFile);
                new XMLOutputter().output(doc, out);
                out.close();
                SAXBuilder saxBuilder = new SAXBuilder();
                saxBuilder.setValidation(true);
                doc = saxBuilder.build(appFile);
                appFile.delete();
            }
            doc = xslTransform(doc, xsl);
        } catch (Exception e) {
            throw e;
        } finally {
            if (appFile != null) appFile.delete();
        }
        return doc.getRootElement();
    }

    public static Document getDocument(InputStream is) throws JDOMException, IOException {
        return new SAXBuilder().build(is);
    }

    public static Document getDocument(File xmlFile) throws JDOMException, IOException {
        try {
            return getDocument(new FileInputStream(xmlFile));
        } catch (FileNotFoundException e) {
            throw new JDOMException(e.getMessage());
        }
    }

    public static Element getRootElement(File xmlFile, File dtdFile) throws Exception {
        return getRootElement(xmlFile, dtdFile, null);
    }

    public static Element getRootElement(File xmlFile) throws Exception {
        return getRootElement(xmlFile, null);
    }

    public Element getRootAndLock(File xmlFile) {
        return getRootAndLock(xmlFile, true);
    }

    public Element getRootAndLock(File xmlFile, boolean throwsException) {
        releaseFileLock();
        Element el = null;
        try {
            el = getRootElement(xmlFile, null);
        } catch (Exception e) {
            if (throwsException) throw new RuntimeException(e);
        } finally {
            tryFileLock(xmlFile);
        }
        return el;
    }

    public static Element getRootElement(String xmlFileName) throws Exception {
        return getRootElement(new File(xmlFileName));
    }

    public static int showConfirmDialog(Component component, Object object, String string, int arg) {
        return JOptionPane.showConfirmDialog(component, object, string, arg);
    }

    public static String getContent(File f) throws IOException {
        FileReader reader = new FileReader(f);
        char[] inbuf = new char[16384];
        StringBuffer outbuf = new StringBuffer(0x10000);
        int size;
        while ((size = reader.read(inbuf)) > -1) outbuf.append(inbuf, 0, size);
        return outbuf.toString();
    }

    public String getHiddenFileName() {
        return TAG_TILDE + "test" + TAG_TMP_SUFFIX;
    }

    public File getHiddenFile() {
        return new File(getHiddenFileName());
    }

    public boolean isHiddenFile(File f) {
        return f.getAbsolutePath().endsWith(FileUtility.TAG_TMP_SUFFIX);
    }

    protected void tryFileLock() {
        if (getFileLock() != null) try {
            releaseFileLock();
            getFileLock().channel().tryLock();
        } catch (IOException e) {
        }
    }

    public void tryFileLock(File f) {
        if (f == null || f != null && !f.exists()) return;
        releaseFileLock();
    }

    public FileLock getFileLock() {
        return fileLock;
    }

    public void releaseFileLock(FileLock fl) {
        try {
            if (fl != null) {
                fl.release();
                fl.channel().close();
            }
        } catch (IOException e) {
        }
    }

    protected void releaseFileLock() {
        releaseFileLock(getFileLock());
    }

    public static String getUserMessage(Throwable t) {
        String msg = t.getMessage() != null ? t.getMessage() : "System Error";
        int indx = msg.lastIndexOf("Exception:");
        return indx > -1 ? msg.substring(indx + 10) : msg;
    }

    public String getProjectExtension() {
        return projectExtension;
    }

    public void setProjectExtension(String projectExtension) {
        this.projectExtension = projectExtension;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }
}
