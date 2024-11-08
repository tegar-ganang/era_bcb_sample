package be.ac.fundp.infonet.econf.producer;

import java.util.*;
import java.net.*;
import java.io.*;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;
import be.ac.fundp.infonet.econf.util.*;
import be.ac.fundp.infonet.econf.resource.*;
import be.ac.fundp.infonet.econf.history.*;

/**
 * The RootParser adds a root file and all its dependencies to an archive. It
 * manages external links to allow better offline browsing.
 * @author Stephane Nicoll - Infonet FUNDP
 * @version 0.2
 */
class RootParser extends HTMLEditorKit.ParserCallback {

    /**
     * Logging object.
     */
    private static org.apache.log4j.Category m_logCat = org.apache.log4j.Category.getInstance(RootParser.class.getName());

    /**
     * The root file for this parser.
     */
    private HTTPRootFile root = null;

    /**
     * The archive for this parser.
     */
    private Archive a = null;

    /**
     * The prefix to use for session's files.
     */
    private String prefix = null;

    /**
     * The vector containing links to change. This vector contains a String arrays: <BR>
     * <LI>
     * <UL> The first element is the URI in the file</UL>
     * <UL> The second element is the new URI of the file</UL>
     * <UL> The third element is the full URL of the file</UL>
     * <UL> The last element is the path in the archive for te file</UL>
     * </LI>
     */
    private Vector links = null;

    /**
     * The String representation of this root file.
     */
    private String s = null;

    /**
     * Creates a new RootParser with the specified parameters.
     * @param root
     * The HTTPRootFile to work on
     * @param archive
     * The archive to use
     */
    public RootParser(HTTPRootFile root, Archive a, String prefix) {
        this.root = root;
        this.a = a;
        this.prefix = prefix;
        links = new Vector();
    }

    /**
     * Parses the HTML content of this root file to retrieve each SRC tags. Once such
     * a tag has been found, check whether the URL is abosolute or not. The following occurs <BR>: <UL>
     * <LI>If the URL is relative (such as images/img0.gif), the resource is simply added in the right place</LI>
     * <LI>If the URL is not relative (such as /img/img0.gif or http://www.sitea.com/img0.gif)
     * the URL is changed to a relative path and the resource is put right there.</LI>
     * </UL>
     */
    public Archive parse() {
        m_logCat.info("Processing " + root.getURL());
        WorkStatusEvent e2 = new WorkStatusEvent("Processing " + root.getURL(), WorkStatusEvent.STEP_RUNNING);
        WorkStatus.generateProgressionEvent(e2);
        if (!root.getContentType().equals(MimeType.TEXT_HTML)) {
            m_logCat.debug("The root file " + root.getURL() + " has not an HTML content");
            addFiles();
            return a;
        }
        try {
            File f = root.getFile();
            if (f != null) {
                m_logCat.info("Parsing " + root.getURL() + " for external dependencies ...");
                Reader reader = new FileReader(root.getFile());
                new ParserDelegator().parse(reader, this, false);
            }
        } catch (Exception e) {
            m_logCat.warn("Error while parsing", e);
        }
        loadRootFile();
        killBaseHref();
        String slideName = getHTMLTitle();
        Enumeration e = root.getDependencies().elements();
        String archivePath = null;
        HTTPDependencyFile f = null;
        int i = 0;
        while (e.hasMoreElements()) {
            f = (HTTPDependencyFile) e.nextElement();
            i = externalURL(f.getURL());
            if (i != -1) {
                String[] link = (String[]) links.elementAt(i);
                s = Utilities.replace(s, link[0], link[1].replace('\\', '/'), true);
                archivePath = link[3];
            } else {
                archivePath = prefix + f.getLocalPath();
            }
            a.add(f.getFile(), archivePath);
        }
        saveRootFile();
        a.add(root.getFile(), prefix + root.getLocalPath());
        addRootFile(slideName);
        m_logCat.info("Finished processing " + root.getURL());
        return a;
    }

    /**
     * Call back method for the HTML parser.
     */
    public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        if ((t.equals(HTML.Tag.IMG)) || (t.equals(HTML.Tag.FRAMESET))) {
            Enumeration e = a.getAttributeNames();
            while (e.hasMoreElements()) {
                Object name = e.nextElement();
                if (name.toString().equalsIgnoreCase("src")) {
                    String uri = a.getAttribute(name).toString();
                    checkDependency(uri);
                }
            }
        }
    }

    /**
     * Checks if the URI is relative or absolute:<BR>
     * <LI>
     * <UL> If the URI starts with / then its resolved to a subdir</UL>
     * <UL> If the URI is an URL from another site, its resolved to a subdir</UL>
     * <UL> Otherwise, the URI remains unchanged</UL>
     * </LI>
     * @param uri
     * The URI to work on
     */
    private void checkDependency(String uri) {
        m_logCat.info("Checking " + uri + " to see if it is an external link");
        if ((uri.startsWith("/")) || (Utilities.isFullURL(uri))) {
            URL u = be.ac.fundp.infonet.econf.util.Utilities.rebuildURL(uri, root.getURL());
            m_logCat.info("Checking " + u + " int the repository");
            HTTPFile f = Repository.getFile(u);
            if (f != null) {
                String path = root.getLocalPath();
                path = path.substring(0, path.lastIndexOf(File.separator) + 1);
                String localPath = getFileName(root.getURL()) + "_files" + File.separator;
                path = prefix + path + localPath;
                String[] link = new String[4];
                link[0] = uri;
                link[1] = localPath + Utilities.getFileName(u);
                link[2] = u.toString();
                link[3] = path + Utilities.getFileName(u);
                m_logCat.info("Adding info for " + link[0] + ": new path - " + link[1] + "; archive path - " + link[3]);
                links.add(link);
            } else {
                m_logCat.warn("The following uri: " + uri + " has not been found -- " + u);
            }
        }
    }

    /**
     * Add the current root file to the slides list.
     * @param slideName
     * The slide's name
     */
    private void addRootFile(String slideName) {
        String path = (prefix + root.getLocalPath()).replace('\\', '/');
        Slide sl = new Slide(path, slideName, root.getDuration(), Slide.MSEC);
        a.addRoot(sl);
    }

    /**
     * Loads the root file in a String representation. This is done before parsing
     * the file for absolute or external references.
     */
    private void loadRootFile() {
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(root.getFile()));
            StringWriter out = new StringWriter();
            int b;
            while ((b = in.read()) != -1) out.write(b);
            out.flush();
            out.close();
            in.close();
            s = out.toString();
        } catch (IOException ie) {
            m_logCat.info("An error occured while loading " + root.getFile(), ie);
        }
    }

    /**
     * Saves the root file in the original file. This is done before adding it
     * to the archive.
     */
    private void saveRootFile() {
        String path = root.getFile().toString();
        root.getFile().delete();
        File res = new File(path);
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(res));
            StringReader in = new StringReader(s);
            int b;
            while ((b = in.read()) != -1) out.write(b);
            out.flush();
            out.close();
            in.close();
            root.setFile(res);
        } catch (IOException ie2) {
            m_logCat.info("An error occured while saving " + path, ie2);
        }
    }

    /**
     * This method is used to add all files to the archive without parsing the root file.
     * @param root
     * The root (non HTML) file.
     * @param a
     * The archive
     */
    private void addFiles() {
        Enumeration e = root.getDependencies().elements();
        while (e.hasMoreElements()) {
            HTTPFile f = (HTTPFile) e.nextElement();
            a.add(f.getFile(), prefix + f.getLocalPath());
        }
        a.add(root.getFile(), prefix + root.getLocalPath());
        addRootFile("");
    }

    /**
     * Returns the index if this URL has to be changed or -1 if nothing has
     * to be done.
     * @param u
     * The url to work on
     * @return true if it must be changed (external links)
     */
    private int externalURL(URL u) {
        if ((links == null) || (links.size() == 0)) return -1;
        for (int i = 0; i < links.size(); i++) {
            String[] s = (String[]) links.elementAt(i);
            if (s[2].equals(u.toString())) return i;
        }
        return -1;
    }

    /**
     * Returns the file name of this URL.
     * If the URL ends with "/" then the default name is added.
     * @param u The URL
     * @return the file name of u (e.g. index.html)
     */
    public static String getFileName(URL u) {
        try {
            if (u.toString().endsWith("/")) u = new URL(u.toString() + CacheManager.getInstance().getDefaultFilename());
            String anURL = u.toString();
            int i = anURL.lastIndexOf("/");
            return (anURL.substring(i + 1));
        } catch (Exception e) {
            return ("");
        }
    }

    /**
     * Clean all base href references to avoid offline browsing problems.
     */
    private void killBaseHref() {
        if (s == null) return;
        int i = s.indexOf("<BASE HREF");
        if (i != -1) {
            m_logCat.info("Base HREF found ... cleaning");
            String firstPart = s.substring(0, i - 1);
            s = s.substring(i, s.length());
            i = s.indexOf(">");
            String secondPart = s.substring(i + 1, s.length());
            s = firstPart + secondPart;
            killBaseHref();
        } else m_logCat.info("Cleaning is done");
    }

    /**
     * Returns the TITLE information.
     */
    private String getHTMLTitle() {
        if (s == null) return null;
        int i = indexIgnoreCaseOf(s, "<TITLE>");
        if (i != -1) {
            m_logCat.debug("<TITLE> tag found ... Trying to retrieve title information");
            int j = indexIgnoreCaseOf(s, "</TITLE>");
            if (j != -1) {
                m_logCat.debug("</TITLE> tag found ...");
                String title = s.substring(i + 7, j);
                return title;
            }
        }
        return null;
    }

    private static int indexIgnoreCaseOf(String s, String of) {
        int i = s.indexOf(of.toLowerCase());
        int j = s.indexOf(of.toUpperCase());
        if (i == -1) return j; else if (j == -1) return i; else {
            if (i < j) return i; else return j;
        }
    }
}
