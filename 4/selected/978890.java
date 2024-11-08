package com.textflex.jarajar.common;

import java.awt.*;
import java.net.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import java.util.zip.*;
import java.util.*;

/**A collection of methods relevant to Jar Ajar code.
 * All of these methods are static, allowing any Jar Ajar class to access
 * them.  Methods that are useful to more than one class often should reside
 * in this class.
 */
public class LibJarAjar {

    public static final String JAJ_VER = "0.5.0";

    private static final String NEWLINE = System.getProperty("line.separator");

    /**Adds a new component to the <code>GridBagLayout</code> manager.
	   @param c component to add
	   @param constraints layout constraints object
	   @param x column number
	   @param y row number
	   @param w number of columns to span
	   @param h number of rows to span
	   @param wx column weight
	   @param wy row weight
	   @param pane the pane that will accept the new component
	*/
    public static void addGridBagComponent(Component c, GridBagConstraints constraints, int x, int y, int w, int h, int wx, int wy, Container pane) {
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = w;
        constraints.gridheight = h;
        constraints.weightx = wx;
        constraints.weighty = wy;
        pane.add(c, constraints);
    }

    /**Creates an image icon.
	 * Retrieves the image file from a jar archive.
	 * @param path image file location relative to TextTrix.class
	 */
    public static ImageIcon makeIcon(String path) {
        URL iconURL = LibJarAjar.class.getResource(path);
        return (iconURL != null) ? new ImageIcon(iconURL) : null;
    }

    /**Enable button rollover icon change.
	 * @param button <code>JButton</code> to display icon rollover change
	 * @param iconPath location of icon to change to
	 */
    public static void setRollover(JButton button, String iconPath) {
        button.setRolloverIcon(makeIcon(iconPath));
        button.setRolloverEnabled(true);
    }

    /**Enable button rollover icon change.
	 * @param button <code>JButton</code> to display icon rollover change
	 * @param icon location of icon to change to
	 */
    public static void setRollover(JButton button, ImageIcon icon) {
        button.setRolloverIcon(icon);
        button.setRolloverEnabled(true);
    }

    /**Set an action's properties.
	 * @param action action to set
	 * @param description tool tip
	 * @param mnemonic menu shortcut
	 * @param keyStroke accelerator key shortcut
	 */
    public static void setAcceleratedAction(Action action, String description, char mnemonic, KeyStroke keyStroke) {
        action.putValue(Action.SHORT_DESCRIPTION, description);
        action.putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
        action.putValue(Action.ACCELERATOR_KEY, keyStroke);
    }

    /**Sets an action's properties.
	 * @param action action to set
	 * @param description tool tip
	 * @param mnemonic menu shortcut
	 */
    public static void setAction(Action action, String description, char mnemonic) {
        action.putValue(Action.SHORT_DESCRIPTION, description);
        action.putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
    }

    /**Sets an action's properties.
	 * @param action action to set
	 * @param description tool tip
	 */
    public static void setAction(Action action, String description) {
        action.putValue(Action.SHORT_DESCRIPTION, description);
    }

    /** Sets an action's properties.
	Assumes that the action has a name, and that the name is used
	in the menu that will house the action.
	@param action action to set
	@param name the action's name, from which to get the mnemonic
	@param description tool tip
	@param charsUnavailable a string of characters unavailable to use
	as mnemonics, as in those already in the menu where the action
	will be placed
	 */
    public static String setAction(Action action, String name, String description, String charsUnavailable) {
        char mnemonic = 0;
        int i = 0;
        for (i = 0; i < name.length() && charsUnavailable.indexOf((mnemonic = name.charAt(i))) != -1; i++) ;
        if (i < name.length()) {
            action.putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
            charsUnavailable += mnemonic;
        }
        action.putValue(Action.SHORT_DESCRIPTION, description);
        return charsUnavailable;
    }

    /**Displays the Jar Ajar "about" dialog, which in turn can display
	 * a license dialog.
	 * @param owner the parent component; can be null
	 * @param aboutIcon the icon to displayin the about dialog
	 * @param accessAsResource specifies whether the files, "about.txt"
	 * and "license.txt", should be accessed via 
	 * <code>class.getResourceAsStream</code> or directly, such as when
	 * the files reside in a location outside of the calling method's
	 * JAR file
	 * @throws exception if the files cannot be read
	 * @see #msgDialog
	*/
    public static void aboutDialog(JFrame owner, ImageIcon aboutIcon, boolean accessAsResource) throws IOException {
        String text = "";
        text = readText("/docs/about.txt", accessAsResource, true);
        if (text == null) {
            text = "Jar Ajar" + "\ncopyright (c) 2002-8 Text Flex" + "\nPlease see http://textflex.com/jarajar" + "\nfor more details, as the \"about.txt\"" + "\nfile appears to be missing from this package.";
        }
        String title = "About Jar Ajar";
        String[] options = { "View License", "Close" };
        int choice = JOptionPane.showOptionDialog(owner, text, title, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, aboutIcon, options, options[0]);
        if (choice == 0) {
            text = readText("/docs/license.txt", accessAsResource, true);
            if (text == null) {
                text = "Please see http://textflex.com/jarajar" + "\nfor more details, as the \"license.txt\"" + "\nfile appears to be missing from this package.";
            }
            msgDialog(owner, null, text, "License.", JOptionPane.INFORMATION_MESSAGE, null, 450, 400, false);
        }
    }

    /**Read in text from a file and return the text as a string.
	 * Differs from <code>displayFile(String path)</code> because
	 * allows editing.
	 * @param reader text file stream
	 * @return text from file
	 */
    public static String readText(BufferedReader reader) {
        String text = "";
        String line;
        try {
            while ((line = reader.readLine()) != null) text = text + line + "\n";
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return text;
    }

    /**Reads text from a given path.
	 * @param path the path from which to read the text
	 * @param accessAsResource specifies whether to access the file
	 * via <code>class.getResourceAsStream(String)</code>, such as when
	 * the file is in a JAR file, or directly
	 * @param relativeToBaseFile specifies whether to treat the <code>path</code>
	 * as relative to the base file; if accessing the file as a resource, 
	 * "/" is prepended to the path, while <code>getBaseFile()</code> is 
	 * used as the base file when accessing the file directly
	 * @return the file's text
	 */
    public static String readText(String path, boolean accessAsResource, boolean relativeToBaseFile) {
        String text = "";
        BufferedReader reader = null;
        if (path == null || path.equals("")) return text;
        try {
            if (accessAsResource) {
                if (relativeToBaseFile) {
                    if (!path.startsWith("/")) path = "/" + path;
                }
                InputStream in = LibJarAjar.class.getResourceAsStream(path);
                reader = new BufferedReader(new InputStreamReader(in));
            } else {
                File file = null;
                if (relativeToBaseFile) {
                    file = new File(getBaseFile(), path);
                } else {
                    file = new File(path);
                }
                reader = new BufferedReader(new FileReader(file));
            }
            String line;
            while ((line = reader.readLine()) != null) text = text + line + "\n";
        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return text;
    }

    /**Reads a file directly from a given file.
	 * 
	 * @param file the file to read
	 * @return the file's text
	 */
    public static String readText(File file) {
        String text = "";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) text = text + line + "\n";
        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return text;
    }

    /**Saves an XML document to a given path and with the given identifiers.
	 * 
	 * @param path the output destination path
	 * @param systemIdentifier the "doctype-system" information
	 * @param publicIdentifier the "doctype-public" information
	 * @param doc the XML document data to save
	 * @throws TransformerException
	 * @throws IOException
	 */
    public static void saveXML(String path, String systemIdentifier, String publicIdentifier, org.w3c.dom.Document doc) throws TransformerException, IOException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("doctype-system", systemIdentifier);
        transformer.setOutputProperty("doctype-public", publicIdentifier);
        File file = new File(getBaseFile(), path);
        transformer.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(file)));
    }

    /**Parses a Jar Ajar XML document based on the <code>jarajar.dtd</code>
	 * document-type specification.
	 * 
	 * @param elt the root element
	 * @return an object containing all the XML text node data
	 */
    public static JarAjarXML parseExtractor(org.w3c.dom.Element elt) {
        String welcomeMsg = "";
        String packageName = "";
        String verNum = "";
        String zipPath = "";
        String logoPath = "";
        String outputDirPath = "";
        String textData = "";
        String runAppStr = "";
        String readmePath = "";
        String licensePath = "";
        NodeList nodes = elt.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof org.w3c.dom.Element) {
                org.w3c.dom.Element nodeElt = (org.w3c.dom.Element) node;
                if (nodeElt.getTagName().equals("welcome")) {
                    NodeList welcomeNodes = nodeElt.getChildNodes();
                    for (int j = 0; j < welcomeNodes.getLength(); j++) {
                        org.w3c.dom.Element paragraph = (org.w3c.dom.Element) welcomeNodes.item(j);
                        Text textNode = (Text) paragraph.getFirstChild();
                        if (textNode != null) {
                            welcomeMsg = welcomeMsg + textNode.getData().trim();
                        }
                        if (j != welcomeNodes.getLength() - 1) {
                            welcomeMsg = welcomeMsg + "\n";
                        }
                    }
                } else if (nodeElt.getTagName().equals("package")) {
                    NodeList packageNodes = nodeElt.getChildNodes();
                    org.w3c.dom.Element path = (org.w3c.dom.Element) packageNodes.item(0);
                    Text textNode = (Text) path.getFirstChild();
                    if (textNode != null) {
                        packageName = textNode.getData().trim();
                    }
                } else if (nodeElt.getTagName().equals("version")) {
                    NodeList packageNodes = nodeElt.getChildNodes();
                    org.w3c.dom.Element path = (org.w3c.dom.Element) packageNodes.item(0);
                    Text textNode = (Text) path.getFirstChild();
                    if (textNode != null) {
                        verNum = textNode.getData().trim();
                    }
                } else if (nodeElt.getTagName().equals("zip")) {
                    NodeList zipNodes = nodeElt.getChildNodes();
                    org.w3c.dom.Element path = (org.w3c.dom.Element) zipNodes.item(0);
                    Text textNode = (Text) path.getFirstChild();
                    if (textNode != null) {
                        zipPath = textNode.getData().trim();
                    }
                } else if (nodeElt.getTagName().equals("logo")) {
                    NodeList logoNodes = nodeElt.getChildNodes();
                    org.w3c.dom.Element path = (org.w3c.dom.Element) logoNodes.item(0);
                    Text textNode = (Text) path.getFirstChild();
                    if (textNode != null) {
                        logoPath = textNode.getData().trim();
                    }
                } else if (nodeElt.getTagName().equals("outputDir")) {
                    NodeList logoNodes = nodeElt.getChildNodes();
                    org.w3c.dom.Element path = (org.w3c.dom.Element) logoNodes.item(0);
                    Text textNode = (Text) path.getFirstChild();
                    if (textNode != null) {
                        outputDirPath = textNode.getData().trim();
                    }
                } else if (nodeElt.getTagName().equals("runAppStr")) {
                    NodeList logoNodes = nodeElt.getChildNodes();
                    org.w3c.dom.Element path = (org.w3c.dom.Element) logoNodes.item(0);
                    Text textNode = (Text) path.getFirstChild();
                    if (textNode != null) {
                        runAppStr = textNode.getData().trim();
                    }
                } else if (nodeElt.getTagName().equals("readmePath")) {
                    NodeList logoNodes = nodeElt.getChildNodes();
                    org.w3c.dom.Element path = (org.w3c.dom.Element) logoNodes.item(0);
                    Text textNode = (Text) path.getFirstChild();
                    if (textNode != null) {
                        readmePath = textNode.getData().trim();
                    }
                } else if (nodeElt.getTagName().equals("licensePath")) {
                    NodeList logoNodes = nodeElt.getChildNodes();
                    org.w3c.dom.Element path = (org.w3c.dom.Element) logoNodes.item(0);
                    Text textNode = (Text) path.getFirstChild();
                    if (textNode != null) {
                        licensePath = textNode.getData().trim();
                    }
                }
            }
        }
        return new JarAjarXML(welcomeMsg, packageName, verNum, zipPath, logoPath, outputDirPath, runAppStr, readmePath, licensePath);
    }

    /** Gets the base directory, the root directory of the program.
	 *
	 * <p>The code has a relatively elaborate mechanism to locate the plugins
	 * folder and its JAR files. Why not use the URL that the Text Trix
	 * class supplies? Text Trix needs to locate each JAR plugin's absolute
	 * path and name. Text Trix's URL must be truncated to its root
	 * directory's location and built back up through the plugins directory.
	 * Using getParentFile() to the program's root and appending the rest of
	 * the path to the plugins allows one to use URLClassLoader directly
	 * with the resulting URL.
	 * 
	 * <p>Unfortunately, some systems do not locate local files with this
	 * method. The following elaborate system works around this apparent JRE
	 * bug by further breaking the URL into a normal path and loading a file
	 * from it.
	 * 
	 * <p>Unfortunately again, a new feature from JRE v.1.4 causes spaces in
	 * URL strings to be converted to "%20" turning URL's into strings. The
	 * JRE cannot load files with "%20" in them, however; for example,
	 * "c:\Program Files\texttrix-x.y.z\plugins" never gets loaded. The
	 * workaround is to replace all "%20"'s in the string with " ". Along
	 * with v.1.4 comes new String regex tools to make the operation simple,
	 * but prior versions crash after a NoSuchMethodError. The replacement
	 * must be done manually.
	 *
	 * @return <code>plugins</code> folder
	 */
    public static File getBaseFile() {
        String relClassLoc = "com/textflex/jarajar/common/LibJarAjar.class";
        URL urlClassDir = ClassLoader.getSystemResource(relClassLoc);
        if (urlClassDir == null) return new File("/");
        String strClassDir = urlClassDir.getPath();
        File fileClassDir = new File(urlClassDir.getPath());
        File baseDir = null;
        if (strClassDir.indexOf(".jar!/" + relClassLoc) != -1) {
            baseDir = fileClassDir.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
        } else {
            baseDir = fileClassDir.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
        }
        String strBaseDir = baseDir.toString();
        int space = 0;
        while ((space = strBaseDir.indexOf("%20")) != -1) {
            if (strBaseDir.length() > space + 3) {
                strBaseDir = strBaseDir.substring(0, space) + " " + strBaseDir.substring(space + 3);
            } else {
                strBaseDir = strBaseDir.substring(0, space) + " ";
            }
        }
        baseDir = new File(strBaseDir);
        String basePath = baseDir.getPath();
        String protocol = "file:";
        int pathStart = basePath.indexOf(protocol);
        if (pathStart != -1) basePath = basePath.substring(pathStart + protocol.length());
        baseDir = new File(basePath);
        if (!baseDir.exists()) {
            baseDir = new File("\\" + basePath);
        }
        return baseDir;
    }

    /**Replaces a substring in a string with another substring.
	 * Handles backslashes ("\") and other characters that 
	 * <code>String.replaceAll(String, String)<code> evidently cannot.
	 * Note that all instances of <code>regex</code> are replaced 
	 * 
	 * @param str non-null string
	 * @param regex substring to locate
	 * @param replacement substring to replace <code>regex</code>
	 * @return the resulting string
	 */
    public static String replaceSubstring(String str, String regex, String replacement) {
        int found = 0;
        int start = 0;
        int len = regex.length();
        if (str == "") return "";
        while ((found = str.indexOf(regex, found)) != -1) {
            if (found + len >= str.length()) {
                return str.substring(0, found) + replacement;
            } else {
                str = str.substring(0, found) + replacement + str.substring(found + len);
            }
            found += len;
        }
        return str;
    }

    /**Adds a directory to a zip output stream.
	 * Assumes that all files will be included.
	 * 
	 * @param zout the output stream
	 * @param dir the directory to add
	 * @param basePath the lowest directory to not include in the zip
	 * @param recursive <code>true</code> signals the method to include sub-
	 * directories
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @see #addZipDirectory(ZipOutputStream, File, String, boolean, String[])
	 */
    public static void addZipDirectory(ZipOutputStream zout, File dir, String basePath, boolean recursive) throws FileNotFoundException, IOException {
        addZipDirectory(zout, dir, basePath, recursive, null);
    }

    /**Adds a directory to a zip output stream.
	 * 
	 * @param zout the output stream
	 * @param dir the directory to add
	 * @param basePath the lowest directory to not include in the zip
	 * @param recursive <code>true</code> signals the method to include sub-
	 * directories
	 * @param ignoreFiles array of file types not to be included, indicated
	 * by extensions.  Note that the \".\" is not necessary, but recommended
	 * to avoid excluding wanted extensions that have the same ending as that of
	 * extensions not included.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @see #addZipDirectory(ZipOutputStream, File, String, boolean)
	 */
    public static void addZipDirectory(ZipOutputStream zout, File dir, String basePath, boolean recursive, String[] ignoreFiles) throws FileNotFoundException, IOException {
        String zipEntryName = "";
        String[] files = dir.list();
        for (int i = 0; i < files.length; i++) {
            File file = new File(dir, files[i]);
            if (ignoreFiles != null && ignoreFile(file, ignoreFiles)) {
            } else if (file.isDirectory()) {
                if (recursive) {
                    addZipDirectory(zout, file, basePath, true);
                }
            } else {
                zipEntryName = file.getAbsolutePath().substring(basePath.length() + 1);
                addZipEntry(zout, file, zipEntryName);
            }
        }
    }

    /**Indicates whether the file's extension is in the ignore list.
	 * 
	 * @param file file to check
	 * @param extensions array of extensions to ignore.  Note that the \".\" is not necessary, but recommended
	 * to avoid excluding wanted extensions that have the same ending as that of
	 * extensions not included.
	 * @return <code>true</code> if the file's extension appears in the list
	 */
    public static boolean ignoreFile(File file, String[] extensions) {
        String name = file.getName();
        for (int i = 0; i < extensions.length; i++) {
            if (name.endsWith(extensions[i])) return true;
        }
        return false;
    }

    /**Adds a single file to a zip output stream.
	 * 
	 * @param zout the output stream for writing
	 * @param file the file to add
	 * @param zipEntryName the name of the new entry
	 * @return true if the zip file was successfully added
	 * @throws IOException
	 */
    public static boolean addZipEntry(ZipOutputStream zout, File file, String zipEntryName) throws IOException {
        try {
            BufferedInputStream bin = new BufferedInputStream(new FileInputStream(file));
            if (zipEntryName == null) {
                zipEntryName = file.getName();
            }
            addZipEntry(zout, bin, zipEntryName);
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("The file \"" + zipEntryName + "\"" + NEWLINE + "could not be found for adding to the package." + NEWLINE + "Please check the file and try again.");
        }
        return false;
    }

    /**Adds a single file to a zip output stream.
	 * Assumes that the name of the entry is the same as the name of the file.
	 * 
	 * @param zout the output stream for writing
	 * @param file the file to add
	 * @return true if the zip file was successfully added
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static boolean addZipEntry(ZipOutputStream zout, File file) throws FileNotFoundException, IOException {
        return addZipEntry(zout, file, null);
    }

    /**Adds the contents of an input stream to a zip output stream.
	 * 
	 * @param zout the output stream for writing
	 * @param bin the input stream from which to write the zip entry
	 * @param zipEntryName the name of the new entry
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static void addZipEntry(ZipOutputStream zout, BufferedInputStream bin, String zipEntryName) throws FileNotFoundException, IOException {
        byte[] bytes = new byte[4096];
        int bytesRead = 0;
        zipEntryName = LibJarAjar.replaceSubstring(zipEntryName, File.separator, "/");
        ZipEntry entry = new ZipEntry(zipEntryName);
        zout.putNextEntry(entry);
        System.out.println("Adding: " + zipEntryName);
        while ((bytesRead = bin.read(bytes)) != -1) {
            zout.write(bytes, 0, bytesRead);
        }
        if (bin != null) bin.close();
        zout.closeEntry();
    }

    /** Unzips all files from a zip input stream to a given install path, while
	 * providing feedback via a text pane.
	 * @param zin a zip input stream
	 * @param installPath the path to which all files will be installed
	 * @param pane a text pane for providing updates to the user about the extraction
	 * @param closeStream if true, the stream will be closed at the end of the 
	 * extraction
	 * @param fileList array list for storing the extracted files
	 * @return true if the unzip process completed successfully
	 */
    public static boolean unzip(ZipInputStream zin, String installPath, JTextPane pane, boolean closeStream, ArrayList fileList) {
        ZipEntry entry = null;
        FileOutputStream fout = null;
        byte[] bytes = new byte[4096];
        int bytesRead = 0;
        boolean cont = true;
        boolean alwaysYes = false;
        boolean alwaysNo = false;
        try {
            while (cont && (entry = zin.getNextEntry()) != null) {
                File file = new File(LibJarAjar.fileSeparatorConverter(installPath + File.separator + entry.getName()));
                File dir = file.getParentFile();
                if (entry.isDirectory()) {
                    if (!file.exists()) cont = file.mkdirs();
                } else if (!dir.exists()) {
                    cont = dir.mkdirs();
                }
                if (!cont) {
                    return false;
                }
                if (cont && !entry.isDirectory()) {
                    String path = file.getPath();
                    int choice = -1;
                    if (file.exists() && !(alwaysYes || alwaysNo)) {
                        choice = overwriteOption(pane, path, true);
                    }
                    if (choice == 2) {
                        alwaysYes = true;
                    } else if (choice == 3) {
                        alwaysNo = true;
                    }
                    if (!file.exists() || alwaysYes || choice == 0) {
                        appendToNotice("Inflating " + path + "\n", pane, true);
                        fout = new FileOutputStream(path);
                        while ((bytesRead = zin.read(bytes)) != -1) {
                            fout.write(bytes, 0, bytesRead);
                        }
                        if (fout != null) {
                            fout.close();
                        }
                        fileList.add(path);
                    } else if (alwaysNo || choice == 1) {
                        appendToNotice(path + " skipped\n", pane, true);
                    } else if (choice == 4 || choice == JOptionPane.CLOSED_OPTION) {
                        appendToNotice("Unzip cancelled.\n", pane, true);
                        cont = false;
                    }
                }
            }
            return cont;
        } catch (IOException e) {
            try {
                if (fout != null) fout.close();
            } catch (IOException ez) {
                return false;
            }
            return false;
        } finally {
            try {
                zin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**Creates a dialog window to query whether to overwrite a file.
	 * The choices are <code>Yes, No, Always yes, Always no, Canel</code>,
	 * with a return value of 0-4, in that order.
	 * 
	 * @param path path to file about to be overwritten
	 * @return an integer value indicating the user's respones
	 */
    public static int overwriteOption(Component owner, String path, boolean always) {
        String msg = path + "\nalready exists.  Overwrite?";
        String[] options = { "Yes", "No", "Always yes", "Always no", "Cancel" };
        if (!always) {
            String[] altOptions = { "Yes", "No", "Cancel" };
            options = altOptions;
        }
        int choice = JOptionPane.showOptionDialog(owner, msg, "Overwrite?", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return choice;
    }

    /**Appends a string to the notice pane.
	 * 
	 * @param text text to append
	 * @param pane the text pane to which the text should be added
	 * @param careToEnd if true, places the caret at the end of the pane
	 */
    public static void appendToNotice(String text, JTextPane pane, boolean careToEnd) {
        if (pane == null) {
            System.out.println(text);
            return;
        }
        javax.swing.text.Document doc = pane.getDocument();
        try {
            doc.insertString(doc.getLength(), text, null);
            if (careToEnd) {
                pane.setCaretPosition(doc.getLength());
            }
        } catch (javax.swing.text.BadLocationException e) {
            System.out.println(text);
        }
    }

    public static void appendToNotice(String text, JTextPane pane) {
        appendToNotice(text, pane, false);
    }

    /** Searches backward in a text to find a given normal-oriented string.
	 * For example, in the text, text = "Mr. Smith went to the door went out,"
	 * reverseIndexOf(text, "went", 14) would return 10. 
	 * @param str text to search
	 * @param searchStr string to find
	 * @param offset index of first character not included in the search
	 * @return index of found string; -1 if not found
	 */
    public static int reverseIndexOf(String str, String searchStr, int offset) {
        int i = offset - 1;
        while (i >= 0 && !str.substring(i, i + searchStr.length()).equals(searchStr)) {
            i--;
        }
        return i;
    }

    /**Converts file separators into the current system's file separator.
	 * 
	 * @param s a string, such as the path to a file
	 * @return a new string with the system's file separators
	 */
    public static String fileSeparatorConverter(String s) {
        String altSeps = "/\\";
        int len = s.length();
        StringBuffer buf = new StringBuffer(len);
        char c = 0;
        for (int i = 0; i < len; i++) {
            c = s.charAt(i);
            if (altSeps.indexOf(c) == -1) {
                buf.append(c);
            } else {
                buf.append(File.separator);
            }
        }
        return buf.toString();
    }

    /**Displays a message dialog with a scrollable text pane and 
	 * an icon.
	 * @param owner the parent component; can be null
	 * @param aboutIcon the icon to displayin the about dialog
	 * @param msg the message to display in the text pane
	 * @param title the title header for the window
	 * @param msgType the JOptionPane message type
	 * @param icon the icon
	 * @param width the width of the window
	 * @param height the height of the window
	 * @param html true if hyperlinks in the window should open
	 * a new browser window
	*/
    public static void msgDialog(Component owner, ImageIcon aboutIcon, String msg, String title, int msgType, Icon icon, int width, int height, boolean html) {
        JEditorPane textPane = new JEditorPane();
        JScrollPane scrollPane = makeScrollPane(textPane);
        textPane.setDocument(textPane.getEditorKit().createDefaultDocument());
        if (html) {
            textPane.setContentType("text/html");
            textPane.addHyperlinkListener(new EditorPaneHyperlinkListener());
        }
        textPane.setPreferredSize(new Dimension(width, height));
        textPane.setText(msg);
        textPane.setCaretPosition(0);
        JOptionPane.showMessageDialog(owner, scrollPane, title, msgType, icon);
    }

    /**Displays a message dialog with a scrollable text pane and 
	 * an icon.
	 * Creates a default window of 30x200 and assumes that
	 * html hyperlinks should open a browser window.
	 * @param owner the parent component; can be null
	 * @param aboutIcon the icon to displayin the about dialog
	 * @param msg the message to display in the text pane
	 * @param title the title header for the window
	 * @param msgType the JOptionPane message type
	 * @param icon the icon
	*/
    public static void msgDialog(Component owner, ImageIcon aboutIcon, String msg, String title, int msgType, Icon icon) {
        msgDialog(owner, aboutIcon, msg, title, msgType, icon, 30, 200, true);
    }

    /** Makes a scroll pane for a text component.
	 * @param textPane the text component that the scroll pane will house
	 * @return the scroll pane
	 */
    public static JScrollPane makeScrollPane(JTextComponent textPane) {
        textPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    /** Adds a hyperlink listener to an editor pane.
	 * @param comp the editor pane to which the listener will be added
	 */
    public static void addEditorPaneHyperlinkListener(JEditorPane comp) {
        comp.addHyperlinkListener(new EditorPaneHyperlinkListener());
    }

    /** A listener for hyperlinks that opens them in a new, custom-built
	 * browser window.
	 */
    private static class EditorPaneHyperlinkListener implements HyperlinkListener {

        /** Opens a new browser window to the given hyperlink when
		 * it is pressed.
		 * @param evt the event
		 */
        public void hyperlinkUpdate(HyperlinkEvent evt) {
            if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                JEditorPane pane = (JEditorPane) evt.getSource();
                JFrame frame = new JFrame();
                final JEditorPane browser = new JEditorPane();
                JScrollPane scrollPane = makeScrollPane(browser);
                frame.setSize(1000, 600);
                browser.addHyperlinkListener(new HyperlinkListener() {

                    public void hyperlinkUpdate(HyperlinkEvent evt1) {
                        if (evt1.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            try {
                                browser.setPage(evt1.getURL());
                            } catch (IOException e) {
                            }
                        }
                    }
                });
                try {
                    browser.setPage(evt.getURL());
                    browser.setSize(new Dimension(1000, 600));
                } catch (IOException e) {
                }
                frame.add(scrollPane);
                frame.validate();
                frame.setVisible(true);
            }
        }
    }
}
