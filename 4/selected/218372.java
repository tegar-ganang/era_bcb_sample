package plugins.confluence;

import plugins.confluence.ui.CharsetFileChooser;
import com.tngtech.freemind2wiki.FM2ConfluenceConverter;
import freemind.extensions.ExportHook.ImageFilter;
import freemind.main.Tools;
import freemind.modes.ModeController;
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

/**
 * Exports the currently viewed mind-map to a file. The user may interact by
 * specifying export options.
 *
 * @see FileExportHook
 */
public class FileExporter extends ConfluenceExporter {

    private static final String CONFLUENCE_FILE_EXTENSION = "txt";

    private CharsetFileChooser fileChooser;

    /**
     *
     * @param controller 
     */
    public void doExport(ModeController controller) {
        initFileChooser(controller);
        Container component = controller.getFrame().getContentPane();
        File mmFile = controller.getMap().getFile();
        if (mmFile != null) {
            String proposedName = mmFile.getAbsolutePath().replaceFirst("\\.[^.]*?$", "") + "." + CONFLUENCE_FILE_EXTENSION;
            fileChooser.setSelectedFile(new File(proposedName));
        }
        if (controller.getLastCurrentDir() != null) {
            fileChooser.setCurrentDirectory(controller.getLastCurrentDir());
        }
        if (fileChooser.showSaveDialog(component) == JFileChooser.APPROVE_OPTION) {
            File chosenFile = fileChooser.getSelectedFile();
            controller.setLastCurrentDir(chosenFile.getParentFile());
            String ext = Tools.getExtension(chosenFile.getName());
            if (!Tools.safeEqualsIgnoreCase(ext, CONFLUENCE_FILE_EXTENSION)) {
                chosenFile = new File(chosenFile.getParent(), chosenFile.getName() + "." + CONFLUENCE_FILE_EXTENSION);
            }
            if (chosenFile.exists()) {
                String overwriteText = MessageFormat.format(controller.getText("file_already_exists"), new Object[] { chosenFile.toString() });
                int overwriteMap = JOptionPane.showConfirmDialog(component, overwriteText, overwriteText, JOptionPane.YES_NO_OPTION);
                if (overwriteMap != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            controller.getFrame().setWaitingCursor(true);
            try {
                Writer fileWriter = new OutputStreamWriter(new FileOutputStream(fileChooser.getSelectedFile()), fileChooser.getSelectedCharset());
                FM2ConfluenceConverter fm2confl = getFM2ConfluenceConverter();
                fm2confl.convert(new StringReader(getMindMapXml(controller.getMap())), fileWriter);
            } catch (IOException e) {
                freemind.main.Resources.getInstance().logException(e, PluginUtils.getString("message.export_failure"));
                JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
            } finally {
                controller.getFrame().setWaitingCursor(false);
            }
        }
    }

    /**
     * Initializes the confluence file chooser if necessary.
     *
     * @return
     */
    private void initFileChooser(ModeController controller) {
        if (fileChooser == null) {
            Charset confluenceTargetCharset = PluginUtils.preselectCharset(controller, "confluence.targetCharset", "8859_1");
            fileChooser = new CharsetFileChooser(confluenceTargetCharset);
            fileChooser.addChoosableFileFilter(new ImageFilter(CONFLUENCE_FILE_EXTENSION, null));
            fileChooser.addAccessoryItem(getExportSettingsPanel());
        }
    }
}
