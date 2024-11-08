package org.rockaa.saving.xml;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.rockaa.ImageTools;
import org.rockaa.RockAA;
import org.rockaa.gui.swing.SwingGUI;
import org.rockaa.saving.AbstractSaveFormat;
import org.rockaa.saving.SaveFormatContainer;
import org.rockaa.saving.SavingException;
import org.rockaa.translation.Translator;

public class XMLSaveFormat extends AbstractSaveFormat {

    private static final Pattern FOLDER_SCAN_PATTERN = Pattern.compile(".*<FolderScan>.*");

    private static final String BASE_PATH = XMLSaveFormat.getBasePath();

    static {
        XMLSaveFormat.createBaseDir();
    }

    private String description;

    private boolean directorySaveFormat;

    private Dimension targetSize;

    private String fileName;

    private ImageFormat format;

    private boolean resize;

    private boolean dead = false;

    private File file;

    public XMLSaveFormat() {
        this.description = "";
        this.directorySaveFormat = true;
        this.targetSize = new Dimension(130, 130);
        this.fileName = "";
        this.format = ImageFormat.BMP;
        this.resize = true;
        SaveFormatContainer.getInstance().addItem(this);
    }

    public XMLSaveFormat(final File file) throws JDOMException, IOException {
        this.file = file;
        this.load(file);
        SaveFormatContainer.getInstance().addItem(this);
    }

    public static void copyDefaultXMLFormats() throws IOException {
        final XMLSaveFormat rockboxFile = new XMLSaveFormat();
        rockboxFile.setDescription(Translator.translate("sansa_rockbox_file"));
        rockboxFile.setDirectorySaveFormat(false);
        rockboxFile.setFormat(ImageFormat.BMP);
        rockboxFile.setTargetSize(new Dimension(130, 130));
        rockboxFile.setResize(true);
        rockboxFile.save(true);
        final XMLSaveFormat rockboxDirectory = new XMLSaveFormat();
        rockboxDirectory.setDescription(Translator.translate("sansa_rockbox_directory"));
        rockboxDirectory.setDirectorySaveFormat(true);
        rockboxDirectory.setFileName("cover.bmp");
        rockboxDirectory.setFormat(ImageFormat.BMP);
        rockboxDirectory.setTargetSize(new Dimension(130, 130));
        rockboxDirectory.setResize(true);
        rockboxDirectory.save(true);
        final XMLSaveFormat sansaOFDirectory = new XMLSaveFormat();
        sansaOFDirectory.setDescription(Translator.translate("sansa_of_directory"));
        sansaOFDirectory.setDirectorySaveFormat(true);
        sansaOFDirectory.setFileName("Album Art.jpg");
        sansaOFDirectory.setFormat(ImageFormat.JPG);
        sansaOFDirectory.setTargetSize(new Dimension(130, 130));
        sansaOFDirectory.setResize(true);
        sansaOFDirectory.save(true);
    }

    private static void copyFile(final File from, final File to) throws IOException {
        final FileChannel in = new FileInputStream(from).getChannel();
        final FileChannel out = new FileOutputStream(to).getChannel();
        in.transferTo(0, in.size(), out);
    }

    public static void createBaseDir() {
        final File dir = new File(XMLSaveFormat.BASE_PATH);
        if (!dir.exists()) dir.mkdirs();
    }

    public static void exportXMLSaveFormat(final XMLSaveFormat format, final File directory) throws IOException {
        XMLSaveFormat.copyFile(format.getFile(), new File(RockAA.doDirectoryWorkaround(directory.getAbsolutePath()) + RockAA.FILE_SEPERATOR + format.getDescriptionForGUI() + ".xml"));
    }

    private static String getBasePath() {
        final StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("user.home"));
        if (!sb.toString().endsWith(RockAA.FILE_SEPERATOR)) sb.append(RockAA.FILE_SEPERATOR);
        sb.append(".rockaa");
        sb.append(RockAA.FILE_SEPERATOR);
        return sb.toString();
    }

    public static void importXMLSaveFormat(final File file) throws IOException {
        XMLSaveFormat.copyFile(file, new File(XMLSaveFormat.BASE_PATH + file.getName()));
    }

    public static void initXMLSaveFormats() {
        final File root = new File(XMLSaveFormat.BASE_PATH);
        for (final File f : root.listFiles()) if (f.getName().toLowerCase().endsWith("xml")) try {
            XMLSaveFormat.loadXMLFormat(f);
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final JDOMException e) {
            JOptionPane.showMessageDialog(SwingGUI.getInstance(), Translator.translate("err_invalid_save_format") + ": " + e.getMessage(), Translator.translate("err_invalid_save_format"), JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static boolean isFolderScan(final File file) throws IOException {
        boolean found = false;
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null && !found) found = XMLSaveFormat.FOLDER_SCAN_PATTERN.matcher(line).matches();
        reader.close();
        return found;
    }

    private static XMLSaveFormat loadXMLFormat(final File file) throws IOException, JDOMException {
        if (XMLSaveFormat.isFolderScan(file)) return new XMLFolderScanSaveFormat(file); else return new XMLSaveFormat(file);
    }

    private File createTargetFile(final File file) {
        if (this.isDirectorySaveFormat()) {
            String directory = RockAA.doDirectoryWorkaround(file.getAbsolutePath());
            if (!directory.endsWith(RockAA.FILE_SEPERATOR)) directory += RockAA.FILE_SEPERATOR;
            final String extension = this.getImageFormatExtension();
            if (this.fileName.toLowerCase().endsWith(extension.toLowerCase())) return new File(directory + this.fileName); else return new File(directory + this.fileName + this.getImageFormatExtension());
        } else {
            String directory = file.getParent();
            if (!directory.endsWith(RockAA.FILE_SEPERATOR)) directory += RockAA.FILE_SEPERATOR;
            String fileName = file.getName();
            if (fileName.contains(".")) fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            return new File(directory + fileName + this.getImageFormatExtension());
        }
    }

    public void delete() {
        if (this.file != null) this.file.delete();
        this.dead = true;
        SaveFormatContainer.getInstance().removeSaveFormat(this);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof XMLSaveFormat) {
            final XMLSaveFormat other = (XMLSaveFormat) obj;
            if (this.description != null && other.description != null) return this.getDescriptionForGUI().equals(other.getDescriptionForGUI());
        }
        return false;
    }

    private void extractImageFormat(final String strFormat) {
        if ("BMP".equals(strFormat)) this.format = ImageFormat.BMP; else if ("JPG".equals(strFormat) || "JPEG".equals(strFormat)) this.format = ImageFormat.JPG; else if ("GIF".equals(strFormat)) this.format = ImageFormat.GIF; else if ("PNG".equals(strFormat)) this.format = ImageFormat.PNG;
    }

    @Override
    public String getDescriptionForGUI() {
        return this.description;
    }

    public File getFile() {
        return this.file;
    }

    public String getFileName() {
        return this.fileName;
    }

    public ImageFormat getFormat() {
        return this.format;
    }

    private String getFormatName() {
        if (this.format == ImageFormat.BMP) return "bmp"; else if (this.format == ImageFormat.GIF) return "gif"; else if (this.format == ImageFormat.JPG) return "jpg"; else return "png";
    }

    private String getImageFormatExtension() {
        if (this.format == ImageFormat.BMP) return ".bmp"; else if (this.format == ImageFormat.GIF) return ".gif"; else if (this.format == ImageFormat.JPG) return ".jpg"; else return ".png";
    }

    public Dimension getTargetSize() {
        return this.targetSize;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.description == null ? 0 : this.description.hashCode());
        return result;
    }

    public boolean isDead() {
        return this.dead;
    }

    @Override
    public boolean isDirectorySaveFormat() {
        return this.directorySaveFormat;
    }

    public boolean isResize() {
        return this.resize;
    }

    private void load(final File file) throws JDOMException, IOException {
        try {
            final Document doc = new SAXBuilder().build(file);
            final Element root = doc.getRootElement();
            final Element def = root.getChild("Default");
            this.description = def.getChildTextNormalize("Description");
            this.directorySaveFormat = Boolean.valueOf(def.getChildText("IsDirectorySaveFormat"));
            this.fileName = def.getChildTextNormalize("FileName");
            final String strFormat = def.getChildText("ImageFormat").toUpperCase();
            this.extractImageFormat(strFormat);
            this.resize = Boolean.valueOf(def.getChildTextNormalize("Resize"));
            this.targetSize = new Dimension(Double.valueOf(def.getChildTextNormalize("TargetWidth")).intValue(), Double.valueOf(def.getChildTextNormalize("TargetHeight")).intValue());
        } catch (final NullPointerException e) {
            throw new JDOMException(Translator.translate("err_incomplete_save_format") + " (" + file.toString() + ")", e.getCause());
        }
        if (this.description == null || this.targetSize == null || this.fileName == null || this.format == null) throw new JDOMException(Translator.translate("incomplete_xml_file"));
    }

    public void save(final boolean ignoreFolderScan) throws IOException {
        final File oldFile = this.getFile();
        if (oldFile != null && oldFile.exists()) this.delete();
        final Element root = new Element("RockAASaveFormat");
        final Document doc = new Document(root);
        final Element def = new Element("Default");
        root.addContent(def);
        final Element description = new Element("Description");
        description.addContent(this.getDescriptionForGUI());
        def.addContent(description);
        final Element directorySaveFormat = new Element("IsDirectorySaveFormat");
        directorySaveFormat.addContent(String.valueOf(this.isDirectorySaveFormat()));
        def.addContent(directorySaveFormat);
        final Element fileName = new Element("FileName");
        fileName.addContent(String.valueOf(this.getFileName()));
        def.addContent(fileName);
        final Element imageFormat = new Element("ImageFormat");
        imageFormat.addContent(this.getFormat().toString());
        def.addContent(imageFormat);
        final Element targetWidth = new Element("TargetWidth");
        targetWidth.addContent(String.valueOf(this.getTargetSize().getWidth()));
        def.addContent(targetWidth);
        final Element targetHeight = new Element("TargetHeight");
        targetHeight.addContent(String.valueOf(this.getTargetSize().getHeight()));
        def.addContent(targetHeight);
        final Element resize = new Element("Resize");
        resize.addContent(String.valueOf(this.isResize()));
        def.addContent(resize);
        if (!ignoreFolderScan && this instanceof XMLFolderScanSaveFormat) {
            final XMLFolderScanSaveFormat fssf = (XMLFolderScanSaveFormat) this;
            final Element folderScan = new Element("FolderScan");
            root.addContent(folderScan);
            final Element dimensionOptional = new Element("IsDimensionOptional");
            dimensionOptional.addContent(String.valueOf(fssf.isDimensionOptional()));
            folderScan.addContent(dimensionOptional);
            final Element fileNamePattern = new Element("FileNamePattern");
            fileNamePattern.addContent(fssf.getFileNamePattern().pattern());
            folderScan.addContent(fileNamePattern);
            final Element maxWidth = new Element("MaximumWidth");
            maxWidth.addContent(String.valueOf(fssf.getMaximumSize().getWidth()));
            folderScan.addContent(maxWidth);
            final Element maxHeight = new Element("MaximumHeight");
            maxHeight.addContent(String.valueOf(fssf.getMaximumSize().getHeight()));
            folderScan.addContent(maxHeight);
            final Element minWidth = new Element("MinimumWidth");
            minWidth.addContent(String.valueOf(fssf.getMinimumSize().getWidth()));
            folderScan.addContent(minWidth);
            final Element minHeight = new Element("MinimumHeight");
            minHeight.addContent(String.valueOf(fssf.getMinimumSize().getHeight()));
            folderScan.addContent(minHeight);
        }
        final XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        final File file = new File(XMLSaveFormat.BASE_PATH + this.getDescriptionForGUI() + ".xml");
        final FileOutputStream fos = new FileOutputStream(file);
        out.output(doc, fos);
        fos.flush();
        fos.close();
        this.setFile(file);
    }

    @Override
    public void save(final Image image, final File[] files) throws SavingException {
        final RenderedImage resizedImage = this.resize ? ImageTools.renderImage(ImageTools.resizeImage(image, (int) this.targetSize.getWidth(), (int) this.targetSize.getHeight())) : ImageTools.renderImage(image);
        for (final File file : files) this.save(resizedImage, this.createTargetFile(file));
    }

    private void save(final RenderedImage image, final File file) throws SavingException {
        try {
            ImageIO.write(image, this.getFormatName(), file);
        } catch (final IOException e) {
            throw new SavingException(Translator.translate("saving_exception"), e.getCause(), e.getStackTrace());
        }
    }

    public void setDead(final boolean dead) {
        this.dead = dead;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setDirectorySaveFormat(final boolean directorySaveFormat) {
        this.directorySaveFormat = directorySaveFormat;
    }

    public void setFile(final File file) {
        this.file = file;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public void setFormat(final ImageFormat format) {
        this.format = format;
    }

    public void setResize(final boolean resize) {
        this.resize = resize;
    }

    public void setTargetSize(final Dimension targetSize) {
        this.targetSize = targetSize;
    }

    @Override
    public String toString() {
        return this.getDescriptionForGUI();
    }
}
