package plugins.confluence;

import com.tngtech.freemind2wiki.FM2ConfluenceConverter;
import freemind.extensions.ExportHook;
import freemind.extensions.ExportHook.ImageFilter;
import freemind.main.Tools;
import java.awt.Container;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 */
public class ExportToConfluenceMarkup extends ExportHook {

    private static Charset selectedCharset;

    /**
     * 
     */
    public ExportToConfluenceMarkup() {
        super();
    }

    public void startupMapHook() {
        super.startupMapHook();
        if (selectedCharset == null) {
            selectedCharset = PluginUtils.preselectCharset(getController(), "confluence.targetCharset", "8859_1");
        }
        String type = "txt";
        Container component = getController().getFrame().getContentPane();
        CharsetFileChooser chooser = null;
        chooser = new CharsetFileChooser(selectedCharset);
        File mmFile = getController().getMap().getFile();
        if (mmFile != null) {
            String proposedName = mmFile.getAbsolutePath().replaceFirst("\\.[^.]*?$", "") + "." + type;
            chooser.setSelectedFile(new File(proposedName));
        }
        if (getController().getLastCurrentDir() != null) {
            chooser.setCurrentDirectory(getController().getLastCurrentDir());
        }
        chooser.addChoosableFileFilter(new ImageFilter(type, null));
        int returnVal = chooser.showSaveDialog(component);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosenFile = chooser.getSelectedFile();
        getController().setLastCurrentDir(chosenFile.getParentFile());
        String ext = Tools.getExtension(chosenFile.getName());
        if (!Tools.safeEqualsIgnoreCase(ext, type)) {
            chosenFile = new File(chosenFile.getParent(), chosenFile.getName() + "." + type);
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
            exportToConfluenceMarkup(chosenFile, chooser.getSelectedCharset());
        } catch (IOException e) {
            freemind.main.Resources.getInstance().logException(e);
        }
        getController().getFrame().setWaitingCursor(false);
    }

    public boolean exportToConfluenceMarkup(File file, Charset charset) throws IOException {
        StringWriter xmlWriter = new StringWriter();
        getController().getMap().getFilteredXml(xmlWriter);
        xmlWriter.close();
        Writer fileWriter = new OutputStreamWriter(new FileOutputStream(file), charset);
        FM2ConfluenceConverter fm2confl = new FM2ConfluenceConverter();
        fm2confl.convert(new StringReader(xmlWriter.toString()), fileWriter);
        return true;
    }
}
