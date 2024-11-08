package de.exilab.pixmgr.dialog.website;

import java.awt.Dimension;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import de.exilab.pixmgr.dialog.AbstractExportCtrl;
import de.exilab.pixmgr.gui.model.AlbumEntry;
import de.exilab.util.progress.ProgressDialog;
import de.exilab.util.progress.ProgressEvent;
import de.exilab.util.progress.ProgressListener;

/**
 * Controler for the website export dialog
 * @author <a href="andreas@exilab.de">Andreas Heidt</a>
 * @version $Revision: 1.4 $ - $Date: 2004/08/12 20:54:39 $
 */
public class WebsiteExportDialogCtrl extends AbstractExportCtrl implements Runnable, ProgressListener {

    /**
     * Static logger for this class
     */
    private static Logger log = Logger.getLogger(WebsiteExportDialogCtrl.class.getName());

    /**
     * Name of the default index pattern file
     */
    private static final String DEFAULT_INDEX_PATTERN = "resources/pattern/DefaultIndexPattern.html";

    /**
     * Name of the default image pattern file
     */
    private static final String DEFAULT_IMAGE_PATTERN = "resources/pattern/DefaultImagePattern.html";

    /**
     * Name of the default source directory
     */
    private static final String DEFAULT_SOURCE_DIR = "resources/pattern/";

    /**
     * The $THUMBNAILS$ tag
     */
    public static final String TAG_THUMBNAILS = "$THUMBNAILS$";

    /**
     * The $DESCRIPTION$ tag
     */
    public static final String TAG_DESCRIPTION = "$DESCRIPTION$";

    /**
     * The $IMAGE$ tag
     */
    public static final String TAG_IMAGE = "$IMAGE$";

    /**
     * The $INDEX$ tag
     */
    public static final String TAG_INDEX = "$INDEX$";

    /**
     * The $PREVIOUS$ tag
     */
    public static final String TAG_PREVIOUS = "$PREVIOUS$";

    /**
     * The $NEXT$ tag
     */
    public static final String TAG_NEXT = "$NEXT$";

    /**
     * An array with album entries to export
     */
    private AlbumEntry[] m_albumEntries;

    /**
     * Reference to the dialog
     */
    private WebsiteExportDialog m_dialog;

    /**
     * Reference to the parent frame
     */
    private Frame m_parentFrame;

    /**
     * Thread for the export
     */
    private Thread m_thread;

    /**
     * The progress dialog
     */
    private ProgressDialog m_dialogProgress;

    /**
     * Constructor of the class <code>WebsiteExportDialogCtrl</code>
     * @param parent Reference to the parent frame
     * @param entries Array with <code>AlbumEntries</code> to export
     */
    public WebsiteExportDialogCtrl(Frame parent, AlbumEntry[] entries) {
        m_albumEntries = entries;
        m_dialog = new WebsiteExportDialog(parent);
        m_parentFrame = parent;
    }

    /**
     * Shows the export dialog and exports all album entries to a
     * website.     
     */
    public void export() {
        m_dialog.setVisible(true);
        if (m_dialog.isCancelled()) {
            return;
        }
        m_dialogProgress = new ProgressDialog(m_parentFrame, "Exporting To Filesystem");
        m_dialogProgress.addProgressListener(this);
        m_dialogProgress.setLocation(m_parentFrame.getLocation().x + ((m_parentFrame.getSize().width - m_dialogProgress.getSize().width) / 2), m_parentFrame.getLocation().y + ((m_parentFrame.getSize().height - m_dialogProgress.getSize().height) / 2));
        m_thread = new Thread(this);
        m_thread.start();
        m_dialogProgress.setVisible(true);
    }

    /**
     * Performs the export
     *
     */
    public void run() {
        File indexPattern = null;
        File imagePattern = null;
        File sourceDir = null;
        File targetDir = null;
        Dimension scaleSize = null;
        if (m_dialog.getCheckCustomize().isSelected()) {
            indexPattern = new File(m_dialog.getTextIndexPattern().getText());
            imagePattern = new File(m_dialog.getTextImagePattern().getText());
            sourceDir = new File(m_dialog.getTextSourceDir().getText());
        } else {
            indexPattern = new File(DEFAULT_INDEX_PATTERN);
            imagePattern = new File(DEFAULT_IMAGE_PATTERN);
            sourceDir = new File(DEFAULT_SOURCE_DIR);
        }
        if (!indexPattern.exists() || !imagePattern.exists() || !sourceDir.exists()) {
            JOptionPane.showMessageDialog(m_dialog, "You have to specify the source directory and the image-\n" + " and index pattern files", "Error", JOptionPane.ERROR_MESSAGE);
            m_dialogProgress.setVisible(false);
            m_thread = null;
            return;
        }
        targetDir = this.getTargetDirectory(m_dialog.getPanelDirectory());
        if (targetDir == null) {
            m_dialogProgress.setVisible(false);
            m_thread = null;
            return;
        }
        if (m_dialog.getPanelExport().getCheckScale().isSelected()) {
            scaleSize = this.getScaleSize(m_dialog.getPanelExport());
        } else {
            scaleSize = null;
        }
        try {
            m_dialogProgress.setCurrentAction("Copying source files..", 2);
            m_dialogProgress.click();
            copySourceDir(sourceDir, targetDir, indexPattern, imagePattern);
            m_dialogProgress.click();
        } catch (IOException e) {
            if (log.isLoggable(Level.SEVERE)) {
                log.log(Level.SEVERE, "Failed to copy HTML source folder: " + e.getMessage(), e);
            }
            JOptionPane.showMessageDialog(m_dialog, "Failed to copy HHTML source folder\n" + "(See logfile for more details)", "Error", JOptionPane.ERROR_MESSAGE);
            m_dialogProgress.setVisible(false);
            m_thread = null;
            return;
        }
        try {
            copyImages(m_albumEntries, scaleSize, targetDir);
        } catch (IOException e) {
            if (log.isLoggable(Level.SEVERE)) {
                log.log(Level.SEVERE, "Failed to copy images to the target dir: " + e.getMessage(), e);
            }
            JOptionPane.showMessageDialog(m_dialog, "Failed to copy images to the target directory\n" + "(See logfile for more details)", "Error", JOptionPane.ERROR_MESSAGE);
            m_dialogProgress.setVisible(false);
            m_thread = null;
            return;
        }
        if (m_thread == null) {
            m_dialogProgress.setVisible(false);
            m_thread = null;
            return;
        }
        try {
            createIndexFile(m_albumEntries, indexPattern, targetDir);
        } catch (IOException e) {
            if (log.isLoggable(Level.SEVERE)) {
                log.log(Level.SEVERE, "Failed to create index file: " + e.getMessage(), e);
            }
            JOptionPane.showMessageDialog(m_dialog, "Failed to create index file\n" + "(See logfile for more details)", "Error", JOptionPane.ERROR_MESSAGE);
            m_dialogProgress.setVisible(false);
            m_thread = null;
            return;
        }
        try {
            m_dialogProgress.setCurrentAction("Creating HTML files..", m_albumEntries.length);
            for (int i = 0; i < m_albumEntries.length; i++) {
                m_dialogProgress.click();
                createImageFile(m_albumEntries[i], (i == 0) ? null : m_albumEntries[i - 1], (i + 1 >= m_albumEntries.length) ? null : m_albumEntries[i + 1], imagePattern, targetDir);
                if (m_thread == null) {
                    m_dialogProgress.setVisible(false);
                    m_thread = null;
                    return;
                }
            }
        } catch (IOException e) {
            if (log.isLoggable(Level.SEVERE)) {
                log.log(Level.SEVERE, "Failed to create image html files: " + e.getMessage(), e);
            }
            JOptionPane.showMessageDialog(m_dialog, "Failed to create image html files\n" + "(See logfile for more details)", "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            m_dialogProgress.setVisible(false);
            m_thread = null;
        }
    }

    /**
     * Copies the source directory to the target directory an excludes the
     * index and image pattern files
     * @param source The source directory
     * @param target The target directory
     * @param indexPattern The index pattern file
     * @param imagePattern The image pattern file
     * @throws IOException If an error has occured
     */
    private void copySourceDir(File source, File target, File indexPattern, File imagePattern) throws IOException {
        File[] list = source.listFiles();
        for (int i = 0; i < list.length; i++) {
            if (list[i].isFile()) {
                if (!list[i].equals(indexPattern) && !list[i].equals(imagePattern)) {
                    copyFile(list[i], target);
                }
            } else {
                File dir = new File(target.getAbsolutePath() + File.separator + list[i].getName());
                dir.mkdir();
                copySourceDir(new File(list[i].getAbsolutePath()), dir, indexPattern, imagePattern);
            }
        }
    }

    /**
     * Copies a file to a directory
     * @param file The file to copy
     * @param dir The directory where the file should be copied to
     * @throws IOException If an error has occured
     */
    private void copyFile(File file, File dir) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        BufferedWriter out = new BufferedWriter(new FileWriter(new File(dir.getAbsolutePath() + File.separator + file.getName())));
        char[] buffer = new char[512];
        int read = -1;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();
    }

    /**
     * Copies all images from the album into the target directory and
     * scales them on the fly
     * @param entry An array with all album entries
     * @param target The target directory
     * @throws IOException If an error has occured
     */
    private void copyImages(AlbumEntry[] entry, Dimension scaleSize, File target) throws IOException {
        m_dialogProgress.setCurrentAction("Exporting images..", entry.length);
        for (int i = 0; i < entry.length; i++) {
            m_dialogProgress.click();
            copyImage(m_dialog, entry[i].getFilename(), scaleSize, target);
            if (m_thread == null) {
                m_dialogProgress.setVisible(false);
            }
        }
    }

    /**
     * Creates the index file for the website
     * @param entry An array with all album entries 
     * @param indexPattern The index pattern file
     * @param targetDir The target directory
     * @throws IOException If an error has occured
     */
    private void createIndexFile(AlbumEntry[] entry, File indexPattern, File targetDir) throws IOException {
        RandomAccessFile out = new RandomAccessFile(targetDir.getAbsolutePath() + File.separator + "index.html", "rw");
        RandomAccessFile in = new RandomAccessFile(indexPattern, "r");
        String line = null;
        while ((line = in.readLine()) != null) {
            line = substituteThumbnails(line);
            out.writeBytes(line + "\n");
        }
        out.close();
        in.close();
    }

    /**
     * Substitute the thumbnails
     * @param line The string to parse for the $THUMBNAILS$ tag
     * @return String with substituted thumbnails if tag was available
     */
    private String substituteThumbnails(String line) {
        StringBuffer buffer = new StringBuffer(line);
        int start = buffer.indexOf(TAG_THUMBNAILS);
        if (start < 0) {
            return line;
        }
        StringBuffer insert = new StringBuffer("\n");
        for (int i = 0; i < m_albumEntries.length; i++) {
            String href = m_albumEntries[i].getFilename();
            href = href.substring(href.lastIndexOf(File.separator) + 1);
            String img = null;
            if (href.toLowerCase().indexOf(".jpg") < 0) {
                img = href.substring(0, href.lastIndexOf(".")) + ".jpg";
            } else {
                img = href;
            }
            href = href.substring(0, href.lastIndexOf(".")) + ".html";
            String description = m_albumEntries[i].getDescription();
            if (description == null || description.length() == 0) {
                description = "";
            }
            insert.append("<a href=\"" + href + "\">");
            insert.append("<img src=\"" + img + "\" width=\"120\"");
            insert.append(" alt=\"" + description + "\"></a>\n");
        }
        buffer.delete(start, start + TAG_THUMBNAILS.length());
        buffer.insert(start, insert.toString());
        return buffer.toString();
    }

    /**
     * Substitute the decsription of an image
     * @param line The string to parse for the $DESCRIPTION$ tag
     * @param albumEntry The current album entry
     * @return String with substituted description if tag was available
     */
    private String substituteDescription(String line, AlbumEntry entry) {
        StringBuffer buffer = new StringBuffer(line);
        int start = buffer.indexOf(TAG_DESCRIPTION);
        if (start < 0) {
            return line;
        }
        StringBuffer insert = new StringBuffer("\n");
        String name = entry.getDescription();
        if (name == null || name.length() == 0) {
            name = entry.getFilename();
            name = name.substring(name.lastIndexOf(File.separator) + 1);
            name = name.substring(0, name.lastIndexOf(".")) + ".jpg";
        }
        insert.append(name);
        buffer.delete(start, start + TAG_DESCRIPTION.length());
        buffer.insert(start, insert.toString());
        return buffer.toString();
    }

    /**
     * Substitute the image
     * @param line The string to parse for the $IMAGE$ tag
     * @param albumEntry The current album entry
     * @return String with substituted image if tag was available
     */
    private String substituteImage(String line, AlbumEntry entry) {
        StringBuffer buffer = new StringBuffer(line);
        int start = buffer.indexOf(TAG_IMAGE);
        if (start < 0) {
            return line;
        }
        StringBuffer insert = new StringBuffer("\n");
        String href = entry.getFilename();
        href = href.substring(href.lastIndexOf(File.separator) + 1);
        String img = null;
        if (href.toLowerCase().indexOf(".jpg") < 0) {
            img = href.substring(0, href.lastIndexOf(".")) + ".jpg";
        } else {
            img = href;
        }
        String description = entry.getDescription();
        if (description == null || description.length() == 0) {
            description = "";
        }
        insert.append("<img src=\"" + img + "\"");
        insert.append(" alt=\"" + description + "\">\n");
        buffer.delete(start, start + TAG_IMAGE.length());
        buffer.insert(start, insert.toString());
        return buffer.toString();
    }

    /**
     * Substitute the index page
     * @param line The string to parse for the $INDEX tag     
     * @return String with substituted index page if tag was available
     */
    private String substituteIndex(String line) {
        StringBuffer buffer = new StringBuffer(line);
        int start = buffer.indexOf(TAG_INDEX);
        if (start < 0) {
            return line;
        }
        StringBuffer insert = new StringBuffer("\n");
        insert.append("<a href=\"index.html\">");
        insert.append("&lt;index&gt;");
        insert.append("</a>");
        buffer.delete(start, start + TAG_INDEX.length());
        buffer.insert(start, insert.toString());
        return buffer.toString();
    }

    /**
     * Substitutes the previous image link
     * @param line The string to parse for the $PREVIOUS$ tag     
     * @param previous The previous album entry
     * @return String with substitutet previous link if tag was available
     */
    private String substitutePrevious(String line, AlbumEntry previous) {
        StringBuffer buffer = new StringBuffer(line);
        int start = buffer.indexOf(TAG_PREVIOUS);
        if (start < 0) {
            return line;
        }
        StringBuffer insert = new StringBuffer("\n");
        if (previous == null) {
            insert.append("");
        } else {
            String link = previous.getFilename();
            link = link.substring(link.lastIndexOf(File.separator) + 1);
            link = link.substring(0, link.lastIndexOf(".")) + ".html";
            insert.append("<a href=\"" + link + "\">");
            insert.append("&lt&lt;&lt;previous&gt;");
            insert.append("</a>");
        }
        buffer.delete(start, start + TAG_PREVIOUS.length());
        buffer.insert(start, insert.toString());
        return buffer.toString();
    }

    /**
     * Substitutes the next image link
     * @param line The string to parse for the $NEXT$ tag     
     * @param previous The next album entry
     * @return String with substitutet next link if tag was available
     */
    private String substituteNext(String line, AlbumEntry next) {
        StringBuffer buffer = new StringBuffer(line);
        int start = buffer.indexOf(TAG_NEXT);
        if (start < 0) {
            return line;
        }
        StringBuffer insert = new StringBuffer("\n");
        if (next == null) {
            insert.append("");
        } else {
            String link = next.getFilename();
            link = link.substring(link.lastIndexOf(File.separator) + 1);
            link = link.substring(0, link.lastIndexOf(".")) + ".html";
            insert.append("<a href=\"" + link + "\">");
            insert.append("&lt;next&gt;&gt;&gt;");
            insert.append("</a>");
        }
        buffer.delete(start, start + TAG_NEXT.length());
        buffer.insert(start, insert.toString());
        return buffer.toString();
    }

    /**
     * Creates the image file for an <code>AlbumEntry</code>
     * @param entry The <code>AlbumEntry</code>
     * @param previousEntry The previous album entry
     * @param nextEntry The next album entry 
     * @param imagePattern The image pattern file to use as a template
     * @param targetDir The target directory
     * @throws IOException If an error has occured
     */
    private void createImageFile(AlbumEntry entry, AlbumEntry previousEntry, AlbumEntry nextEntry, File imagePattern, File targetDir) throws IOException {
        String outFileName = entry.getFilename().substring(entry.getFilename().lastIndexOf(File.separator) + 1);
        outFileName = outFileName.substring(0, outFileName.lastIndexOf(".")) + ".html";
        RandomAccessFile out = new RandomAccessFile(new File(targetDir.getAbsolutePath() + File.separator + outFileName), "rw");
        RandomAccessFile in = new RandomAccessFile(imagePattern, "r");
        String line = null;
        while ((line = in.readLine()) != null) {
            line = substituteDescription(line, entry);
            line = substituteImage(line, entry);
            line = substituteIndex(line);
            line = substitutePrevious(line, previousEntry);
            line = substituteNext(line, nextEntry);
            out.writeBytes(line + "\n");
        }
        out.close();
        in.close();
    }

    public void processCancelled(ProgressEvent event) {
        m_dialogProgress.setVisible(false);
        m_thread = null;
        return;
    }
}
