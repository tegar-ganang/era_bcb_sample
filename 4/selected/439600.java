package accessories.plugins;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import accessories.plugins.util.html.ClickableImageCreator;
import accessories.plugins.util.xslt.ExportDialog;
import freemind.extensions.ExportHook;
import freemind.main.Resources;
import freemind.main.Tools;
import freemind.modes.MindIcon;
import freemind.modes.MindMap;
import freemind.modes.MindMapNode;
import freemind.modes.ModeController;

/**
 * @author foltin
 *
 * Exports the map using an XSLT script. The parameterization is described
 * in the corresponding Export... .xml-file.
 */
public class ExportWithXSLT extends ExportHook {

    private static final String NAME_EXTENSION_PROPERTY = "name_extension";

    /**
	 * For test purposes. True=no error
	 */
    private boolean mTransformResultWithoutError = false;

    protected File chooseFile() {
        String nameExtension = null;
        if (getProperties().containsKey(NAME_EXTENSION_PROPERTY)) {
            nameExtension = getResourceString(NAME_EXTENSION_PROPERTY);
        }
        return chooseFile(getResourceString("file_type"), getTranslatableResourceString("file_description"), nameExtension);
    }

    /**
	 * 
	 */
    public ExportWithXSLT() {
        super();
    }

    public void startupMapHook() {
        super.startupMapHook();
        ModeController mc = getController();
        MindMap model = getController().getMap();
        if (Tools.safeEquals(getResourceString("file_type"), "user")) {
            if (model == null) return;
            if ((model.getFile() == null) || model.isReadOnly()) {
                if (mc.save()) {
                    export(model.getFile());
                    return;
                } else return;
            } else export(model.getFile());
        } else {
            File saveFile = chooseFile();
            if (saveFile == null) {
                return;
            }
            transform(saveFile);
        }
    }

    /**
	 * @param saveFile 
     * 
     */
    public void transform(File saveFile) {
        try {
            mTransformResultWithoutError = true;
            boolean create_image = Tools.safeEquals(getResourceString("create_html_linked_image"), "true");
            String areaCode = getAreaCode(create_image);
            String xsltFileName = getResourceString("xslt_file");
            boolean success = transformMapWithXslt(xsltFileName, saveFile, areaCode);
            if (!success) {
                JOptionPane.showMessageDialog(null, getResourceString("error_applying_template"), "Freemind", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (success && Tools.safeEquals(getResourceString("create_dir"), "true")) {
                String directoryName = saveFile.getAbsolutePath() + "_files";
                success = createDirectory(directoryName);
                if (success) {
                    String files = getResourceString("files_to_copy");
                    String filePrefix = getResourceString("file_prefix");
                    copyFilesFromResourcesToDirectory(directoryName, files, filePrefix);
                }
                if (success && Tools.safeEquals(getResourceString("copy_icons"), "true")) {
                    success = copyIcons(directoryName);
                }
                if (success && Tools.safeEquals(getResourceString("copy_map"), "true")) {
                    success = copyMap(directoryName);
                }
                if (success && create_image) {
                    createImageFromMap(directoryName);
                }
            }
            if (!success) {
                JOptionPane.showMessageDialog(null, getResourceString("error_creating_directory"), "Freemind", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (Tools.safeEquals(getResourceString("load_file"), "true")) {
                getController().getFrame().openDocument(Tools.fileToUrl(saveFile));
            }
        } catch (Exception e) {
            freemind.main.Resources.getInstance().logException(e);
            mTransformResultWithoutError = false;
        }
    }

    private boolean copyMap(String pDirectoryName) throws IOException {
        boolean success = true;
        BufferedWriter fileout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pDirectoryName + File.separator + "map.mm")));
        getController().getMap().getFilteredXml(fileout);
        return success;
    }

    /**
     */
    private boolean copyIcons(String directoryName) {
        boolean success;
        String iconDirectoryName = directoryName + File.separatorChar + "icons";
        success = createDirectory(iconDirectoryName);
        if (success) {
            copyIconsToDirectory(iconDirectoryName);
        }
        return success;
    }

    /**
     */
    private void createImageFromMap(String directoryName) {
        if (getController().getView() == null) return;
        BufferedImage image = createBufferedImage();
        try {
            FileOutputStream out = new FileOutputStream(directoryName + File.separator + "image.png");
            ImageIO.write(image, "png", out);
            out.close();
        } catch (IOException e1) {
            freemind.main.Resources.getInstance().logException(e1);
        }
    }

    /**
     */
    private void copyIconsToDirectory(String directoryName2) {
        Vector iconNames = MindIcon.getAllIconNames();
        for (int i = 0; i < iconNames.size(); ++i) {
            String iconName = ((String) iconNames.get(i));
            MindIcon myIcon = MindIcon.factory(iconName);
            copyFromResource(MindIcon.getIconsPath(), myIcon.getIconBaseFileName(), directoryName2);
        }
        File iconDir = new File(Resources.getInstance().getFreemindDirectory(), "icons");
        if (iconDir.exists()) {
            String[] userIconArray = iconDir.list(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.matches(".*\\.png");
                }
            });
            for (int i = 0; i < userIconArray.length; ++i) {
                String iconName = userIconArray[i];
                if (iconName.length() == 4) {
                    continue;
                }
                copyFromFile(iconDir.getAbsolutePath(), iconName, directoryName2);
            }
        }
    }

    /**
     */
    private void copyFilesFromResourcesToDirectory(String directoryName, String files, String filePrefix) {
        StringTokenizer tokenizer = new StringTokenizer(files, ",");
        while (tokenizer.hasMoreTokens()) {
            String next = tokenizer.nextToken();
            copyFromResource(filePrefix, next, directoryName);
        }
    }

    /**
     */
    private boolean createDirectory(String directoryName) {
        File dir = new File(directoryName);
        if (!dir.exists()) {
            return dir.mkdir();
        }
        return true;
    }

    /**
     * @throws IOException
     */
    private boolean transformMapWithXslt(String xsltFileName, File saveFile, String areaCode) throws IOException {
        StringWriter writer = getMapXml();
        StringReader reader = new StringReader(writer.getBuffer().toString());
        URL xsltUrl = getResource(xsltFileName);
        if (xsltUrl == null) {
            logger.severe("Can't find " + xsltFileName + " as resource.");
            throw new IllegalArgumentException("Can't find " + xsltFileName + " as resource.");
        }
        InputStream xsltFile = xsltUrl.openStream();
        return transform(new StreamSource(reader), xsltFile, saveFile, areaCode);
    }

    /**
     * @throws IOException
     */
    private StringWriter getMapXml() throws IOException {
        StringWriter writer = new StringWriter();
        getController().getMap().getFilteredXml(writer);
        return writer;
    }

    /**
     * @param create_image
     */
    private String getAreaCode(boolean create_image) {
        String areaCode = "";
        if (create_image) {
            MindMapNode root = getController().getMap().getRootNode();
            ClickableImageCreator creator = new ClickableImageCreator(root, getController(), getResourceString("link_replacement_regexp"));
            areaCode = creator.generateHtml();
        }
        return areaCode;
    }

    private void export(File file) {
        ExportDialog exp = new ExportDialog(file, getController());
        exp.setVisible(true);
    }

    public boolean transform(Source xmlSource, InputStream xsltStream, File resultFile, String areaCode) {
        Source xsltSource = new StreamSource(xsltStream);
        Result result = new StreamResult(resultFile);
        try {
            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer(xsltSource);
            trans.setParameter("destination_dir", resultFile.getName() + "_files/");
            trans.setParameter("area_code", areaCode);
            trans.setParameter("folding_type", getController().getFrame().getProperty("html_export_folding"));
            trans.transform(xmlSource, result);
        } catch (Exception e) {
            freemind.main.Resources.getInstance().logException(e);
            return false;
        }
        ;
        return true;
    }

    public boolean isTransformResultWithoutError() {
        return mTransformResultWithoutError;
    }
}
