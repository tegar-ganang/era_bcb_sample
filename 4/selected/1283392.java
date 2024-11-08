package net.sourceforge.jhelpdev.action;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import net.sourceforge.jhelpdev.ConfigHolder;
import net.sourceforge.jhelpdev.ExtensionFileFilter;
import net.sourceforge.jhelpdev.JHelpDevAboutBox;
import net.sourceforge.jhelpdev.JHelpDevFrame;
import net.sourceforge.jhelpdev.settings.FileName;
import net.sourceforge.jhelpdev.settings.Settings;
import org.znerd.xmlenc.XMLOutputter;

/**
 * Saves the configuration settings for a project as a JHelpDev project file.
 * 
 * @author <a href="mailto:mk@mk-home.de">Markus Kraetzig</a>
 */
public final class SaveConfigAction extends AbstractAction {

    /**
     * The version of the XML project file.
     */
    public static final String DTD_VERSION = "1.2";

    /**
     * The filename of the dtd file for JHelpDev projects.
     */
    public static final String DTD_FILE = "jhelpdev_1_2.dtd";

    /**
     * The location of the dtd file in the web.
     */
    public static final String DTD_URL = "http://jhelpdev.sourceforge.net/dtd/" + DTD_FILE;

    private static boolean isCanceled = true;

    private static ExtensionFileFilter projectFileFilter = new ExtensionFileFilter("xml", "JHelpDev project files");

    private static JFileChooser fileChooser = null;

    /**
     * CreateMapAction constructor comment.
     */
    public SaveConfigAction() {
        super();
        putValue(NAME, "Save Project As");
    }

    /**
     * CreateMapAction constructor comment.
     * 
     * @param name
     *            java.lang.String
     */
    public SaveConfigAction(String name) {
        super(name);
    }

    /**
     * CreateMapAction constructor comment.
     * 
     * @param name
     *            java.lang.String
     * @param icon
     *            javax.swing.Icon
     */
    public SaveConfigAction(String name, javax.swing.Icon icon) {
        super(name, icon);
    }

    /**
     * Invokes save action.
     */
    public void actionPerformed(java.awt.event.ActionEvent arg1) {
        doIt();
    }

    /**
     * @see AbstractHelperAction
     */
    public static void doIt() {
        isCanceled = true;
        File file = null;
        int ret = getFileChooser().showSaveDialog(JHelpDevFrame.getAJHelpDevToolFrame());
        if (ret == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
            String fileName = file.toString();
            if (fileName.indexOf(".", fileName.lastIndexOf(File.separator)) < 0) {
                fileName = fileName + ".xml";
                file = new File(fileName);
            }
            if (new File(fileName).isFile()) {
                int option = JOptionPane.showConfirmDialog(JHelpDevFrame.getAJHelpDevToolFrame(), "File " + fileName + " already exists.\nDo you want to overwrite it?", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (option == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            saveProjectToFile(fileName);
            Settings.getInstance().setLastDirectory(new File(fileName).getParent().toString());
            Settings.getInstance().insertRecentFile(new FileName(fileName));
            isCanceled = false;
        }
        return;
    }

    /**
     * Lazy initializes the filechooser.
     */
    private static JFileChooser getFileChooser() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Project as...");
            fileChooser.addChoosableFileFilter(projectFileFilter);
            fileChooser.setFileFilter(projectFileFilter);
        }
        String lastDirectory = Settings.getInstance().getLastDirectory();
        if (lastDirectory != null) fileChooser.setCurrentDirectory(new File(lastDirectory));
        return fileChooser;
    }

    /**
     * Returns whether action was interrupted or cancelled.
     * 
     * @return boolean
     */
    public static boolean isCanceled() {
        return isCanceled;
    }

    /**
     * Writes the project settings to the selected XML file.
     */
    public static void saveProjectToFile(String fileName) {
        BufferedWriter out = null;
        try {
            FileOutputStream targetStream = new FileOutputStream(fileName);
            OutputStreamWriter textWriter = new OutputStreamWriter(targetStream, "UTF-8");
            out = new BufferedWriter(textWriter);
            String encoding = "utf-8";
            if (ConfigHolder.CONF.getXmlEncoding() != null) encoding = ConfigHolder.CONF.getXmlEncoding();
            XMLOutputter xmlEnc = new XMLOutputter(out, encoding);
            xmlEnc.setLineBreak(AbstractXMLHandler.LINE_BREAK);
            xmlEnc.setIndentation(AbstractXMLHandler.INDENTATION);
            xmlEnc.declaration();
            xmlEnc.dtd("jhelpdev", "-//jhelpdev.sourcefore.net//JHelpDev Configuration Settings " + DTD_VERSION, DTD_URL);
            xmlEnc.comment("generated by JHelpDev " + JHelpDevAboutBox.VERSION + ", see jhelpdev.sourceforge.org");
            xmlEnc.startTag("jhelpdev");
            xmlEnc.attribute("version", DTD_VERSION);
            xmlEnc.startTag("config");
            xmlEnc.startTag("project");
            xmlEnc.pcdata(ConfigHolder.CONF.getProjectName());
            xmlEnc.endTag();
            xmlEnc.startTag("projectdir");
            xmlEnc.pcdata(ConfigHolder.CONF.getProjectDir());
            xmlEnc.endTag();
            xmlEnc.startTag("startpage");
            xmlEnc.pcdata(ConfigHolder.CONF.getTopTarget());
            xmlEnc.endTag();
            xmlEnc.startTag("popupicon");
            xmlEnc.pcdata(ConfigHolder.CONF.getPopupImage());
            xmlEnc.endTag();
            xmlEnc.endTag();
            xmlEnc.startTag("view");
            xmlEnc.startTag("helptitle");
            xmlEnc.pcdata(ConfigHolder.CONF.getHelpTitle());
            xmlEnc.endTag();
            xmlEnc.startTag("toc");
            xmlEnc.attribute("showing", ConfigHolder.CONF.isTOC() ? "yes" : "no");
            xmlEnc.attribute("label", ConfigHolder.CONF.getTocName());
            xmlEnc.endTag();
            xmlEnc.startTag("index");
            xmlEnc.attribute("showing", ConfigHolder.CONF.isIndex() ? "yes" : "no");
            xmlEnc.attribute("label", ConfigHolder.CONF.getIndexName());
            xmlEnc.endTag();
            xmlEnc.startTag("search");
            xmlEnc.attribute("showing", ConfigHolder.CONF.isSearch() ? "yes" : "no");
            xmlEnc.attribute("label", ConfigHolder.CONF.getSearchName());
            xmlEnc.endTag();
            xmlEnc.endTag();
            for (int i = 0; i < ConfigHolder.CONF.getSubHelpSets().length; i++) {
                xmlEnc.startTag("subhelpset");
                xmlEnc.attribute("location", ConfigHolder.CONF.getSubHelpSets()[i].toString());
                xmlEnc.endTag();
            }
            xmlEnc.startTag("encoding");
            xmlEnc.attribute("value", ConfigHolder.CONF.getXmlEncoding());
            xmlEnc.endDocument();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ConfigHolder.CONF.setSaveFileName(fileName);
        System.out.println("Project saved to " + fileName + ".");
        return;
    }
}
