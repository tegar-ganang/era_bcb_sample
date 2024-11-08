package pdfdb.indexing.pdf;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import org.pdfbox.exceptions.CryptographyException;
import org.pdfbox.pdmodel.encryption.BadSecurityHandlerException;
import pdfdb.data.db.PropertyProvider;
import pdfdb.data.db.RegionProvider;
import pdfdb.data.*;
import pdfdb.structure.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.encryption.DecryptionMaterial;
import org.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import pdfdb.data.db.IndexProvider;
import pdfdb.gui.frames.AppDialog;
import pdfdb.gui.frames.MainFrame;
import pdfdb.gui.frames.PasswordRequestFrame;
import pdfdb.indexing.plugins.ThumbnailCapableIndexer;
import pdfdb.parsing.MultiAttemptParser;
import pdfdb.settings.IgnoredFileManager;
import pdfdb.settings.UserSettingsManager;

/** A solid implementation of the Indexer interface and a plugin into the
 * services framework. Responsible for indexing all PDF files.
 * @author ug22cmg */
public class PdfIndexer extends ThumbnailCapableIndexer {

    private String path = null;

    private String title, author, moduleCode, subtitle;

    private String summary, body, text;

    private PasswordRequestFrame dialog;

    private Region[] defaults;

    /** Performs main indexing operation.
     * @param path The path to index.
     * @return The region array generated from parsing. */
    @Override
    public Region[] index(String path) {
        MainFrame mainFrame = MainFrame.getMainFrame();
        try {
            this.path = path;
            this.dialog = new PasswordRequestFrame(path, mainFrame);
            return saveIndex();
        } catch (Throwable e) {
            return null;
        } finally {
            System.gc();
        }
    }

    /** Attempts to decrypt the document using the password specified.
     * @param doc The document to attempt to decrypt.
     * @param password The password to attempt to decrypt.
     * @return True if successful */
    private boolean tryDecrypt(PDDocument doc, String password) {
        try {
            DecryptionMaterial m = new StandardDecryptionMaterial(password);
            doc.openProtection(m);
            return true;
        } catch (BadSecurityHandlerException ex) {
            return false;
        } catch (CryptographyException ex) {
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    /** Gets the password repeatedly until the user either decides to
     *  cancel decryption, or so that doc.isEncrypted() == false.
     * @param doc The document to attempt to decrypt.
     * @return True if successful */
    private boolean getPasswordFromUser(PDDocument doc) {
        boolean carryOn = true;
        boolean decrypted = false;
        UserSettingsManager manager = UserSettingsManager.getInstance();
        boolean savePsw = Boolean.parseBoolean(manager.get("SAVE_PASSWORDS"));
        if (savePsw) {
            while (!decrypted && carryOn) {
                carryOn = showDialog(false);
                decrypted = tryDecrypt(doc, dialog.getPassword());
            }
        }
        if (decrypted) {
            try {
                if (PropertyProvider.propertyExists("PASSWORD", path)) {
                    PropertyProvider.setProperty("PASSWORD", dialog.getPassword(), path);
                } else {
                    PropertyProvider.addProperty("PASSWORD", "Password", dialog.getPassword(), path);
                }
            } catch (SQLException ex) {
            }
        }
        return decrypted;
    }

    /** Presents the user with the password decryption dialog once.
     * @param firstTime If this is the first time that the dialog
     * has been shown or if this is a retry.
     * @return True if the user clicked ok. */
    private boolean showDialog(boolean firstTime) {
        dialog.setIsRetry(!firstTime);
        dialog.setVisible(true);
        return this.dialog.getResult() != AppDialog.REJECTED;
    }

    /** Returns a document instance, decrypted if possible.
     * @param path The path to load.
     * @return The PDDocument instance or null if an error occured. */
    private PDDocument loadDocument(String path) {
        PDDocument doc = null;
        boolean decrypted = false;
        try {
            doc = PDDocument.load(path);
            if (doc.isEncrypted()) {
                try {
                    if (PropertyProvider.propertyExists("PASSWORD", path)) {
                        decrypted = tryDecrypt(doc, PropertyProvider.getProperty("PASSWORD", path));
                    }
                } catch (SQLException ex) {
                }
                if (!decrypted) {
                    decrypted = this.getPasswordFromUser(doc);
                }
                if (!decrypted) {
                    IgnoredFileManager ignoredManager = IgnoredFileManager.getInstance();
                    ignoredManager.set(path, "true");
                    ignoredManager.save();
                    return null;
                }
            }
            return doc;
        } catch (Throwable t) {
            if (doc != null) {
                try {
                    doc.close();
                } catch (IOException ex) {
                }
            }
            return null;
        }
    }

    /** Performs the main indexing operation and saves
     *  to the database. Any other operations required to
     *  successfully index are also performed. This is a 
     * computationally expensive operation.
     * @return The region array.
     * @throws java.io.IOException If an error occurs.
     * @throws java.sql.SQLException If an error occurs. */
    private Region[] saveIndex() throws IOException, SQLException {
        PDDocument doc = null;
        try {
            doc = loadDocument(path);
            if (doc == null) {
                throw new IOException();
            }
            MultiAttemptParser parser = new MultiAttemptParser();
            this.defaults = this.defaultRegions();
            addThumbnail(doc);
            parseContent(parser, doc);
            if (this.body == null || this.text == null) {
                return null;
            }
            saveIndexes();
            getMetaData(doc);
            saveProperties();
            return RegionProvider.getRegions(path);
        } finally {
            if (doc != null) {
                doc.close();
            }
            doc = null;
        }
    }

    /** Gets selected meta data.
     * @param doc The document to extract the data from. */
    private void getMetaData(PDDocument doc) {
        PDDocumentInformation info = doc.getDocumentInformation();
        if (info != null) {
            if (info.getAuthor() != null) {
                author = info.getAuthor();
            }
        }
    }

    /** Parses the content using the specified parser.
     * @param parser The parser to use.
     * @throws java.io.IOException If there is an error. */
    private void parseContent(MultiAttemptParser parser, PDDocument doc) throws IOException {
        this.text = parser.parse(doc);
        if (text != null) {
            this.title = parser.getEstimateAtTitle();
            this.summary = parser.getEstimateAtSummary();
            this.subtitle = parser.getEstimateAtSubTitle();
            this.body = parser.getEstimateAtBodyText();
            this.moduleCode = getModuleCode(text);
            if (this.body != null) {
                this.summary = this.body;
            }
        }
    }

    /** Saves indexes to database by transforming the
     *  values of the fields into arrays. Passing saving
     * off to the saveToDb method.
     * @throws java.sql.SQLException If an error occurs */
    private void saveIndexes() throws SQLException {
        String[] titleArr = this.title == null ? null : getIndexArray(this.title);
        String[] summaryArr = this.summary == null ? null : getIndexArray(this.summary);
        String[] bodyArr = getIndexArray(this.body);
        saveToDb(titleArr, bodyArr, summaryArr);
    }

    /** Saves the specified string arrays as indexes to the
     *  database.
     * @param title The title array.
     * @param body The body array.
     * @param summary The summary array.
     * @throws java.sql.SQLException If an error occurs */
    private void saveToDb(String[] title, String[] body, String[] summary) throws SQLException {
        Region titleRegion = getRegion(defaults, RegionType.TITLE);
        Region summaryRegion = getRegion(defaults, RegionType.SUMMARY);
        Region bodyRegion = getRegion(defaults, RegionType.BODY);
        if (title != null) {
            IndexProvider.addIndexes(title, titleRegion);
        }
        if (summary != null) {
            IndexProvider.addIndexes(summary, summaryRegion);
        }
        IndexProvider.addIndexes(body, bodyRegion);
    }

    /** Saves named properties to the abstract property system.
     * @throws java.sql.SQLException If an error occurs. */
    private void saveProperties() throws SQLException {
        if (isValidProperty(title)) {
            PropertyProvider.addProperty("TITLE", "Title", title, path);
        } else {
            String fileName = new File(path).getName();
            PropertyProvider.addProperty("TITLE", "Title", fileName, path);
        }
        if (isValidProperty(author)) {
            PropertyProvider.addProperty("AUTHOR", "Author", author, path);
        }
        if (isValidProperty(moduleCode)) {
            PropertyProvider.addProperty("MODULE_CODE", "Module Code", moduleCode, path);
        }
        if (isValidProperty(subtitle)) {
            PropertyProvider.addProperty("SUBTITLE", "Subtitle", subtitle, path);
        }
        if (isValidProperty(summary)) {
            PropertyProvider.addProperty("SUMMARY", "Summary", summary, path);
        }
    }

    /** Gets a formatted array representing the text to be
     *  added as indexes. 
     * @param text The input text.
     * @return A formatted array. */
    private String[] getIndexArray(String text) {
        List<String> list = new ArrayList<String>();
        String formatted = null;
        if (text == null) {
            throw new IllegalArgumentException();
        }
        formatted = removeDuplicateSpaces(text.trim());
        for (String s : formatted.split(" ")) {
            list.add(s);
        }
        return removeJunk(list);
    }

    /** Removes characters that are undesirable in the
     *  specified word list.
     * @param list The word list.
     * @return A formatted string array. */
    private String[] removeJunk(List<String> list) {
        String[] arr;
        int i = 0;
        while (i < list.size()) {
            String value = list.get(i);
            String validValue = "";
            for (int j = 0; j < value.length(); j++) {
                if (validChar(value.charAt(j))) {
                    validValue += value.charAt(j);
                }
            }
            if (validValue.equals("")) {
                list.remove(i);
                i--;
            } else {
                list.set(i, validValue);
            }
            i++;
        }
        arr = new String[list.size()];
        return list.toArray(arr);
    }

    /** Removes duplicate spaces from a string.
     * @param str The input string.
     * @return The string with duplicate spaces removed. */
    private String removeDuplicateSpaces(String str) {
        if (str == null) {
            throw new IllegalArgumentException();
        }
        return str.replace("(( ){2, }|\\n|\\t|\\r)", " ");
    }

    /** Gets whether the specified property is likely to
     *  be valid.
     * @param str The property value.
     * @return True if valid. */
    private boolean isValidProperty(String str) {
        if (str != null) {
            str = str.trim();
            boolean isLongEnough = str.length() >= 2;
            if (isLongEnough) {
                boolean autoGenerated = str.matches(".*\\.[a-z|A-Z]{3,}?.*");
                return isLongEnough && !autoGenerated;
            }
        }
        return false;
    }

    /** Gets whether the specified character should be included in an attempt
     *  to make an index.
     * @param c The character to check.
     * @return True if the character should be included. */
    private boolean validChar(char c) {
        return Character.isLetter(c) || Character.isDigit(c) || c == '-' || c == '\'';
    }

    /** Gets the the thumbnail string by rendering the pdf on an image and
     *  then passing this to the ThumbnailCapableIndexer thumbnail creation
     *  utility.
     * @param path The path to get the thumbnail for.
     * @return The thumbnail path to be added to the abstract property system.
     * @throws java.io.IOException If there is an error reading from the path. */
    private String getThumbnailForPdf(String path) throws IOException {
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(path), "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            PDFFile pdffile = new PDFFile(buf);
            PDFPage page = pdffile.getPage(0, true);
            Rectangle2D bb = page.getBBox();
            int width = bb == null ? (int) page.getWidth() : (int) bb.getWidth();
            int height = bb == null ? (int) page.getHeight() : (int) bb.getHeight();
            Rectangle rect = new Rectangle(0, 0, width, height);
            BufferedImage img = (BufferedImage) page.getImage(rect.width, rect.height, rect, null, true, true);
            return super.saveThumbnail(img);
        } catch (Throwable e) {
            System.gc();
            return null;
        }
    }

    /** Adds the required data as specified by the superclass to the
     * PropertyProvider.
     * @param doc The document to get the thumbnail of.
     * @throws java.io.IOException If an error occurs.
     * @throws java.sql.SQLException If an error occurs while saving. */
    private void addThumbnail(PDDocument doc) throws IOException, SQLException {
        String thumbString = getThumbnailForPdf(path);
        String key = "THUMBNAIL";
        String name = "Thumbnail";
        if (thumbString != null) {
            PropertyProvider.addProperty(key, name, thumbString, path);
        }
        System.gc();
    }

    /** Adds the default regions to the database and gets instance's of
     *  the database connected region objects with full ID's.
     * @return The region array. */
    private Region[] defaultRegions() {
        try {
            RegionProvider.addRegion(path, RegionType.TITLE);
            RegionProvider.addRegion(path, RegionType.SUMMARY);
            RegionProvider.addRegion(path, RegionType.BODY);
            return RegionProvider.getRegions(path);
        } catch (SQLException se) {
            return null;
        }
    }

    /** Identifies a specific region in the given region array. This method
     *  should be used to identify the given region type when the order of
     *  the region array is not guranateed such as when returned from the
     *  database.
     * @param regionArr The region array to search.
     * @param type The value to search for.
     * @return The region value. */
    private Region getRegion(Region[] regionArr, RegionType type) {
        if (regionArr == null) {
            throw new IllegalArgumentException("Region arr is null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type is null.");
        }
        for (Region region : regionArr) {
            if (region.getRegionType() == type) {
                return region;
            }
        }
        return null;
    }

    /** Gets the module code from the specified string. 
     * @param input The input string.
     * @return The module code part if exists, otherwise null. */
    private String getModuleCode(String input) {
        String regex = "[0-9]{2,2}-[0-9]{5,5}(\\.[0-9]{1,3}){0,1}";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(input);
        if (m.find()) {
            return m.group();
        } else {
            return null;
        }
    }
}
