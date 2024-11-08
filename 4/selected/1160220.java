package plugins.confluence;

import com.tngtech.freemind2wiki.FM2ConfluenceConverter;
import freemind.extensions.ExportHook.ImageFilter;
import freemind.main.Tools;
import java.awt.Container;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class ExportConfluenceToFile extends ConfluenceExportHook {

    private static final String CONFLUENCE_FILE_EXTENSION = "txt";

    private CharsetFileChooser confluenceFileChooser;

    @Override
    public void startupMapHook() {
        super.startupMapHook();
        initConfluenceFileChooser();
        Container component = getController().getFrame().getContentPane();
        File mmFile = getController().getMap().getFile();
        if (mmFile != null) {
            String proposedName = mmFile.getAbsolutePath().replaceFirst("\\.[^.]*?$", "") + "." + CONFLUENCE_FILE_EXTENSION;
            confluenceFileChooser.setSelectedFile(new File(proposedName));
        }
        if (getController().getLastCurrentDir() != null) {
            confluenceFileChooser.setCurrentDirectory(getController().getLastCurrentDir());
        }
        if (confluenceFileChooser.showSaveDialog(component) == JFileChooser.APPROVE_OPTION) {
            File chosenFile = confluenceFileChooser.getSelectedFile();
            getController().setLastCurrentDir(chosenFile.getParentFile());
            String ext = Tools.getExtension(chosenFile.getName());
            if (!Tools.safeEqualsIgnoreCase(ext, CONFLUENCE_FILE_EXTENSION)) {
                chosenFile = new File(chosenFile.getParent(), chosenFile.getName() + "." + CONFLUENCE_FILE_EXTENSION);
            }
            if (chosenFile.exists()) {
                String overwriteText = MessageFormat.format(getController().getText("file_already_exists"), new Object[] { chosenFile.toString() });
                int overwriteMap = JOptionPane.showConfirmDialog(component, overwriteText, overwriteText, JOptionPane.YES_NO_OPTION);
                if (overwriteMap != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            getController().getFrame().setWaitingCursor(true);
            try {
                exportToFile(chosenFile, confluenceFileChooser.getSelectedCharset());
            } catch (IOException e) {
                freemind.main.Resources.getInstance().logException(e, "Failed to export mind-map to Confluence markup.");
                JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
            }
            getController().getFrame().setWaitingCursor(false);
        }
    }

    /**
     * Initializes the confluence file chooser if necessary.
     *
     * @return
     */
    private void initConfluenceFileChooser() {
        if (confluenceFileChooser == null) {
            Charset confluenceTargetCharset = PluginUtils.preselectCharset(getController(), "confluence.targetCharset", "8859_1");
            confluenceFileChooser = new CharsetFileChooser(confluenceTargetCharset);
            confluenceFileChooser.addChoosableFileFilter(new ImageFilter(CONFLUENCE_FILE_EXTENSION, null));
        }
    }

    /**
     * Exports the currently shown mind-map as Confluence markup to a file, using
     * the given charset.
     *
     * @param file
     * @param charset
     * @throws java.io.IOException
     */
    private void exportToFile(File file, Charset charset) throws IOException {
        Writer fileWriter = new OutputStreamWriter(new FileOutputStream(file), charset);
        FM2ConfluenceConverter fm2confl = new FM2ConfluenceConverter();
        fm2confl.convert(new StringReader(getMindMapXml()), fileWriter);
    }
}
