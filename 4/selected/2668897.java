package tit.gui.icon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * IconFacory which reads tis icons from a zip file.
 * @author Bart Sas
 */
public class ZipIconFactory implements IconFactory {

    /**
     * The default icons.
     */
    private static final IconFactory DEFAULT_ICONS = new DefaultIconFactory();

    /**
     * The zip file from which the icons are read.
     */
    private ZipFile zip;

    /**
     * Constructs a new <code>ZipIconFactory</code>.
     * @param zipinit The zip file from which the icons are read.
     */
    public ZipIconFactory(ZipFile zipinit) {
        zip = zipinit;
    }

    /**
     * Constructs a new <code>ZipIconFactory</code>.
     * @param file The file from which the icons are read.
     * @throws IOException Is thrown if an I/O error occurrs.
     * @throws ZipException Is thrown when a ZIP format error occurrs.
     */
    public ZipIconFactory(File file) throws ZipException, IOException {
        zip = new ZipFile(file);
    }

    /**
     * Constructs a new <code>ZipIconFactory</code>.
     * @param name The name of the file from which the icons are read.
     * @throws IOException Is thrown if an I/O error occurrs.
     * @throws ZipException Is thrown when a ZIP format error occurrs.
     */
    public ZipIconFactory(String name) throws ZipException, IOException {
        zip = new ZipFile(name);
    }

    /**
     * Reads all the bytes in an input-stream, and returns them as an array of <code>byte</code>s.
     * @param input The input stream from which the bytes are read.
     * @return An array of bytes containing the contents of <code>input</code>.
     * @throws IOException Is thrown when something goes wrong while reading.
     */
    private static byte[] readAllBytesFrom(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        for (int read = input.read(chunk); read > 0; read = input.read(chunk)) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    /**
     * The name of the new-icon.
     */
    private static final String NEW_ICON = "new.png";

    /**
     * Returns the new icon.
     * @return The new icon.
     */
    public Icon getNewIcon() {
        try {
            return new ImageIcon(readAllBytesFrom(zip.getInputStream(zip.getEntry(NEW_ICON))));
        } catch (IOException exception) {
            return DEFAULT_ICONS.getNewIcon();
        }
    }

    /**
     * The name of the save-icon.
     */
    private static final String SAVE_ICON = "save.png";

    /**
     * Returns the save icon.
     * @return The save icon.
     */
    public Icon getSaveIcon() {
        try {
            return new ImageIcon(readAllBytesFrom(zip.getInputStream(zip.getEntry(SAVE_ICON))));
        } catch (IOException exception) {
            return DEFAULT_ICONS.getSaveIcon();
        }
    }

    /**
     * The name of the load-icon.
     */
    private static final String LOAD_ICON = "load.png";

    /**
     * Returns the load icon.
     * @return The load icon.
     */
    public Icon getLoadIcon() {
        try {
            return new ImageIcon(readAllBytesFrom(zip.getInputStream(zip.getEntry(LOAD_ICON))));
        } catch (IOException exception) {
            return DEFAULT_ICONS.getLoadIcon();
        }
    }

    /**
     * The name of the import-icon.
     */
    private static final String IMPORT_ICON = "import.png";

    /**
     * Returns the import icon.
     * @return The import icon.
     */
    public Icon getImportIcon() {
        try {
            return new ImageIcon(readAllBytesFrom(zip.getInputStream(zip.getEntry(IMPORT_ICON))));
        } catch (IOException exception) {
            return DEFAULT_ICONS.getImportIcon();
        }
    }

    /**
     * The name of the export-icon.
     */
    private static final String EXPORT_ICON = "export.png";

    /**
     * Returns the export icon.
     * @return The export icon.
     */
    public Icon getExportIcon() {
        try {
            return new ImageIcon(readAllBytesFrom(zip.getInputStream(zip.getEntry(EXPORT_ICON))));
        } catch (IOException exception) {
            return DEFAULT_ICONS.getExportIcon();
        }
    }

    /**
     * The name of the edit-icon.
     */
    private static final String EDIT_ICON = "edit.png";

    /**
     * Returns the edit icon.
     * @return The edit icon.
     */
    public Icon getEditIcon() {
        try {
            return new ImageIcon(readAllBytesFrom(zip.getInputStream(zip.getEntry(EDIT_ICON))));
        } catch (IOException exception) {
            return DEFAULT_ICONS.getEditIcon();
        }
    }

    /**
     * The name of the add-icon.
     */
    private static final String ADD_ICON = "add.png";

    /**
     * Returns the add icon.
     * @return The add icon.
     */
    public Icon getAddIcon() {
        try {
            return new ImageIcon(readAllBytesFrom(zip.getInputStream(zip.getEntry(ADD_ICON))));
        } catch (IOException exception) {
            return DEFAULT_ICONS.getAddIcon();
        }
    }

    /**
     * The name of the remove-icon.
     */
    private static final String REMOVE_ICON = "remove.png";

    /**
     * Returns the remove icon.
     * @return The remove icon.
     */
    public Icon getRemoveIcon() {
        try {
            return new ImageIcon(readAllBytesFrom(zip.getInputStream(zip.getEntry(REMOVE_ICON))));
        } catch (IOException exception) {
            return DEFAULT_ICONS.getRemoveIcon();
        }
    }

    /**
     * The name of the info-icon.
     */
    private static final String INFO_ICON = "info.png";

    /**
     * Returns the info icon.
     * @return The info icon.
     */
    public Icon getInfoIcon() {
        try {
            return new ImageIcon(readAllBytesFrom(zip.getInputStream(zip.getEntry(INFO_ICON))));
        } catch (IOException exception) {
            return DEFAULT_ICONS.getInfoIcon();
        }
    }

    /**
     * The name of the exit-icon.
     */
    private static final String EXIT_ICON = "exit.png";

    /**
     * Returns the exit icon.
     * @return The exit icon.
     */
    public Icon getExitIcon() {
        try {
            return new ImageIcon(readAllBytesFrom(zip.getInputStream(zip.getEntry(EXIT_ICON))));
        } catch (IOException exception) {
            return DEFAULT_ICONS.getExitIcon();
        }
    }

    /**
     * The name of the preferences-icon.
     */
    private static final String PEFERENCES_ICON = "preferences.png";

    /**
     * Returns the exit icon.
     * @return The exit icon.
     */
    public Icon getPreferencesIcon() {
        try {
            return new ImageIcon(readAllBytesFrom(zip.getInputStream(zip.getEntry(PEFERENCES_ICON))));
        } catch (IOException exception) {
            return DEFAULT_ICONS.getPreferencesIcon();
        }
    }
}
