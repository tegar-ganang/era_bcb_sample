package org.xactor.installer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xactor.ws.DOMUtils;

/**
 * The base installer for XActor.
 * 
 * @author <a href="mailto:ivanneto@gmail.com">Ivan Neto</a>
 */
public class Installer {

    /**
    * What application server XActor will be installed on.
    */
    private String serverIdentifier;

    /**
    * The application server directory.
    */
    private String serverDir;

    /**
    * Shall we install the IIOP plug-in?
    */
    private boolean installIIOP;

    /**
    * Shall we install the SOAP plug-in?
    */
    private boolean installSOAP;

    /**
    * Shall we install the JBoss Remoting plug-in?
    */
    private boolean installJBRem;

    /**
    * To load resources when the installer is not running within a .jar.
    */
    private String expandedInstallerDir;

    /**
    * The backup directory. The installer will save backup files here.
    */
    private String backupDir;

    /**
    * The server profile directory.
    */
    private String profileDir;

    /**
    * The classloader used to load resources within a .jar.
    */
    protected ClassLoader cl;

    public Installer(String serverIdentifier, String serverDir, String serverProfile, boolean installIIOP, boolean installSOAP, boolean installJBRem, String expandedInstallerDir) {
        this.serverIdentifier = serverIdentifier;
        this.serverDir = serverDir;
        this.installIIOP = installIIOP;
        this.installSOAP = installSOAP;
        this.installJBRem = installJBRem;
        this.expandedInstallerDir = expandedInstallerDir;
        this.profileDir = serverDir + "/server/" + serverProfile;
        this.cl = Thread.currentThread().getContextClassLoader();
    }

    /**
    * Install XActor.
    * 
    * @return true if the installation was successful, false otherwise.
    */
    public void install() throws Exception {
        InputStream in = getResourceAsStream(serverIdentifier + ".xml", false);
        String s = toString(in);
        s = s.replaceAll("@server.dir@", serverDir);
        s = s.replaceAll("@server.profile.dir@", profileDir);
        s = s.replaceAll("@server.all.dir@", serverDir + "/server/all");
        Element root = DOMUtils.parse(s);
        NodeList l = root.getElementsByTagName("backup-dir");
        Node bkpNode = l.item(0);
        backupDir = bkpNode.getTextContent();
        l = root.getElementsByTagName("transaction-manager");
        Node tmNode = l.item(0);
        processSection(tmNode);
        if (installIIOP) {
            l = root.getElementsByTagName("iiop");
            Node iiopNode = l.item(0);
            if (iiopNode != null) processSection(iiopNode);
        }
        if (installSOAP) {
            l = root.getElementsByTagName("soap");
            Node soapNode = l.item(0);
            if (soapNode != null) processSection(soapNode);
        }
        if (installJBRem) {
            l = root.getElementsByTagName("jboss-remoting");
            Node jbremNode = l.item(0);
            if (jbremNode != null) processSection(jbremNode);
        }
    }

    private void processSection(Node sectionNode) throws FileNotFoundException, IOException {
        NodeList l = sectionNode.getChildNodes();
        for (int i = 0; i < l.getLength(); i++) {
            Node n = l.item(i);
            if ("copy".equals(n.getLocalName())) {
                NamedNodeMap attrs = n.getAttributes();
                String file = attrs.getNamedItem("file").getTextContent();
                String tofile = attrs.getNamedItem("tofile").getTextContent();
                Node namedItem = attrs.getNamedItem("backup");
                String backup = (namedItem == null ? null : namedItem.getTextContent());
                namedItem = attrs.getNamedItem("abspath");
                String abspath = (namedItem == null ? null : namedItem.getTextContent());
                InputStream from = getResourceAsStream(file, "yes".equals(abspath));
                File to = new File(tofile);
                if ("yes".equals(backup)) backupFileIfThereIsNoBackup(to);
                copy(from, to);
            } else if ("replaceregex".equals(n.getLocalName())) {
                NamedNodeMap attrs = n.getAttributes();
                String file = attrs.getNamedItem("file").getTextContent();
                File f = new File(file);
                Node namedItem = attrs.getNamedItem("backup");
                String backup = (namedItem == null ? null : namedItem.getTextContent());
                if ("yes".equals(backup)) backupFileIfThereIsNoBackup(f);
                String regex = null;
                String replacement = null;
                NodeList children = n.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if ("regex".equals(child.getLocalName())) {
                        NodeList grandchildren = child.getChildNodes();
                        for (int k = 0; k < grandchildren.getLength(); k++) {
                            Node grandchild = grandchildren.item(k);
                            if (Node.CDATA_SECTION_NODE == grandchild.getNodeType()) regex = grandchild.getTextContent();
                        }
                    } else if ("replacement".equals(child.getLocalName())) {
                        NodeList grandchildren = child.getChildNodes();
                        for (int k = 0; k < grandchildren.getLength(); k++) {
                            Node grandchild = grandchildren.item(k);
                            if (Node.CDATA_SECTION_NODE == grandchild.getNodeType()) replacement = grandchild.getTextContent();
                        }
                    }
                }
                replaceRegex(f, regex, replacement, "yes".equals(backup));
            } else if ("condition".equals(n.getLocalName())) {
                NamedNodeMap attrs = n.getAttributes();
                String file = attrs.getNamedItem("filedoesnotexist").getTextContent();
                File f = new File(file);
                if (!f.exists()) {
                    processSection(n);
                }
            } else if ("update-zip-file".equals(n.getLocalName())) {
                NamedNodeMap attrs = n.getAttributes();
                String file = attrs.getNamedItem("file").getTextContent();
                File f = new File(file);
                Node namedItem = attrs.getNamedItem("backup");
                String backup = (namedItem == null ? null : namedItem.getTextContent());
                if ("yes".equals(backup)) backupFileIfThereIsNoBackup(f);
                List<String> todirs = new ArrayList<String>();
                List<File> files = new ArrayList<File>();
                NodeList children = n.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if ("add".equals(child.getLocalName())) {
                        NamedNodeMap childAttrs = child.getAttributes();
                        String todir = childAttrs.getNamedItem("todir").getTextContent();
                        todirs.add(todir);
                        files.add(new File(child.getTextContent()));
                    }
                }
                addFilesToExistingZip(f, todirs.toArray(new String[0]), files.toArray(new File[0]));
            }
        }
    }

    private InputStream getResourceAsStream(String resource, boolean abspath) throws FileNotFoundException {
        if (abspath) return new FileInputStream(resource);
        InputStream in = cl.getResourceAsStream(resource);
        if (in == null && expandedInstallerDir != null) {
            in = new FileInputStream(expandedInstallerDir + "/" + resource);
        }
        return in;
    }

    /**
    * Create a backup of a file, but only if the backup does not already
    * exist.
    */
    private void backupFileIfThereIsNoBackup(File file) throws IOException {
        File dir = new File(backupDir);
        if (!dir.exists()) {
            dir.mkdir();
            FileWriter fw = new FileWriter(backupDir + "/README");
            fw.write("XActor saves backup files in this directory.\n");
            fw.close();
        }
        File backupFile = new File(dir + "/" + file.getName());
        if (!backupFile.exists()) copy(file, backupFile);
    }

    /**
    * Copy backupFile to destFile, replacing regex with the content of
    * the file replacementFile (this file should be acessible through the
    * getResourceAsStream(...) method).
    * @param b 
    */
    private void replaceRegex(File file, String regex, String replacement, boolean restoreFromBackup) throws IOException {
        File source = null;
        if (restoreFromBackup) source = new File(backupDir + "/" + file.getName()); else source = file;
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(toCharSequence(source));
        replacement = replacement.replace("$", "\\$");
        String destFileContent = matcher.replaceFirst(replacement);
        FileWriter fw = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fw);
        out.write(destFileContent);
        out.close();
    }

    /**
    * Add files to a zip file (e.g., a jar file). This method is slow: it does
    * not update the zip file, but creates a new one. This happens because
    * the JDK does now allow the update of zip files.   
    * 
    * @param zipFile the zip file.
    * @param path the folder where the added files will be placed within the
    *        zip file.
    * @param files the files to be added.
    * @throws IOException if something goes wrong.
    */
    private void addFilesToExistingZip(File zipFile, String[] dirs, File[] files) throws IOException {
        File tempFile = new File(zipFile.getAbsoluteFile() + ".temp");
        if (tempFile.exists()) tempFile.delete();
        boolean renameOk = zipFile.renameTo(tempFile);
        if (!renameOk) {
            throw new RuntimeException("Could not rename the file " + zipFile.getAbsolutePath() + " to " + tempFile.getAbsolutePath());
        }
        ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
        ZipEntry entry = zin.getNextEntry();
        byte[] buf = new byte[1024];
        while (entry != null) {
            String entryName = entry.getName();
            boolean inFiles = false;
            for (int i = 0; i < files.length; i++) {
                String fileName = dirs[i] + "/" + files[i].getName();
                if (fileName.equals(entryName)) {
                    inFiles = true;
                    break;
                }
            }
            if (!inFiles) {
                out.putNextEntry(new ZipEntry(entryName));
                int len;
                while ((len = zin.read(buf)) > 0) out.write(buf, 0, len);
            }
            entry = zin.getNextEntry();
        }
        zin.close();
        for (int i = 0; i < files.length; i++) {
            InputStream in = new FileInputStream(files[i]);
            out.putNextEntry(new ZipEntry(dirs[i] + "/" + files[i].getName()));
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            out.closeEntry();
            in.close();
        }
        out.close();
        tempFile.delete();
    }

    /**
    * Copy an InputStream to a File.
    */
    private void copy(InputStream in, File to) throws IOException {
        OutputStream out = new FileOutputStream(to);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /**
    * Copy a File.
    */
    private void copy(File from, File to) throws IOException {
        InputStream in = new FileInputStream(from);
        copy(in, to);
    }

    /**
    * Convert the content of a File to a CharSequence.
    */
    private CharSequence toCharSequence(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fc = fis.getChannel();
        ByteBuffer bbuf = fc.map(FileChannel.MapMode.READ_ONLY, 0, (int) fc.size());
        CharBuffer cbuf = Charset.forName("UTF-8").newDecoder().decode(bbuf);
        return cbuf;
    }

    /**
    * Convert an InputStream to a String. 
    */
    private String toString(InputStream in) throws IOException {
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }

    /**
    * Get the stack trace of an exception as a String. Useful for reporting
    * errors.
    */
    private String getStackTraceAsString(Exception e) {
        StringWriter writer = new StringWriter();
        PrintWriter pwriter = new PrintWriter(writer);
        e.printStackTrace(pwriter);
        return writer.toString();
    }
}
