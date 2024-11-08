package org.whatsitcalled.webflange.file;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.util.file.Folder;
import org.apache.wicket.util.io.IOUtils;
import org.whatsitcalled.webflange.model.Chart;
import org.whatsitcalled.webflange.model.LoadTest;
import org.whatsitcalled.webflange.model.Script;

public class FileManager {

    private String baseDir = System.getProperty("user.home") + "/webFlange/filestore";

    private String archiveDir = "/tmp";

    private String decodeCharset = Charset.defaultCharset().name();

    private String encodeCharset = Charset.defaultCharset().name();

    Folder uploadFolder;

    Folder scriptFolder;

    Folder propertyFolder;

    Folder dataFolder;

    private Logger LOGGER = Logger.getLogger(FileManager.class);

    public void init() {
        if (uploadFolder == null) setUploadFolder(new Folder(baseDir + "/uploads"));
        uploadFolder.mkdirs();
        if (scriptFolder == null) setScriptFolder(new Folder(baseDir + "/scripts"));
        scriptFolder.mkdirs();
        if (propertyFolder == null) setPropertyFolder(new Folder(baseDir + "/properties"));
        propertyFolder.mkdirs();
        if (dataFolder == null) setDataFolder(new Folder(baseDir + "/data"));
        dataFolder.mkdirs();
    }

    public String getDecodeCharset() {
        return decodeCharset;
    }

    public void setDecodeCharset(String decodeCharset) {
        this.decodeCharset = decodeCharset;
    }

    public String getEncodeCharset() {
        return encodeCharset;
    }

    public void setEncodeCharset(String encodeCharset) {
        this.encodeCharset = encodeCharset;
    }

    /**
	 * Add a file to the filestore
	 * 
	 * @param key
	 * @param content
	 */
    private void saveFile(Folder folder, Object key, InputStream stream) throws FileManagerException {
        File file = new File(folder, key.toString());
        LOGGER.debug("Writing file: " + file.getAbsolutePath());
        Writer writer = null;
        Writer encodedWriter = null;
        try {
            encodedWriter = new OutputStreamWriter(new FileOutputStream(file), getEncodeCharset());
            IOUtils.copy(stream, encodedWriter, getDecodeCharset());
            LOGGER.info("saveFile(), decode charset: " + getDecodeCharset() + ", encode charset: " + getEncodeCharset());
        } catch (IOException e) {
            throw new FileManagerException("Unable to write to file: " + file.getAbsolutePath(), e);
        } finally {
            try {
                encodedWriter.close();
            } catch (IOException e) {
                throw new FileManagerException("Unable to write to file: " + file.getAbsolutePath(), e);
            }
        }
    }

    public void saveScriptFile(Script script, InputStream stream) throws FileManagerException {
        String name = getScriptFileName(script);
        saveFile(getScriptFolder(), name, stream);
    }

    public void saveUploadFile(Object key, InputStream stream) throws FileManagerException {
        saveFile(getUploadFolder(), key, stream);
    }

    public void savePropertyFile(LoadTest test, InputStream stream) throws FileManagerException {
        String name = getPropertyFileName(test);
        saveFile(getPropertyFolder(), name, stream);
    }

    /**
	 * Remove a file from the filestore
	 * 
	 * @param key
	 * @throws FileManagerException 
	 */
    private void deleteFile(Folder folder, Object key) throws FileManagerException {
        File file = new File(folder, key.toString());
        if (file.exists()) {
            LOGGER.debug("Deleting file: " + file.getAbsolutePath());
            try {
                FileUtils.forceDelete(file);
            } catch (IOException e1) {
                throw new FileManagerException("Unable to delete file: " + file.getAbsolutePath(), e1);
            }
        }
    }

    public void deleteUploadFile(Object key) throws FileManagerException {
        deleteFile(getUploadFolder(), key);
    }

    public void deleteScriptFile(Script script) throws FileManagerException {
        deleteFile(getScriptFolder(), getScriptFileName(script));
    }

    public void deletePropertyFile(LoadTest test) throws FileManagerException {
        deleteFile(getPropertyFolder(), getPropertyFileName(test));
    }

    public String getScriptFileContent(Script script) {
        File file = new File(getScriptFolder(), getScriptFileName(script));
        String content = new String();
        try {
            content = FileUtils.readFileToString(file);
        } catch (IOException e) {
            LOGGER.error("Unable to open file:" + file.getAbsolutePath(), e);
        }
        return content;
    }

    public File getPropertyFile(LoadTest test) {
        return new File(getPropertyFolder(), getPropertyFileName(test));
    }

    public String getScriptFileName(Script script) {
        return script.getId().toString() + ".py";
    }

    public String getPropertyFileName(LoadTest test) {
        return test.getId().toString() + ".properties";
    }

    public File getScriptFile(Script script) {
        return new File(getScriptFolder() + "/" + getScriptFileName(script));
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String dir) {
        this.baseDir = dir;
    }

    public Folder getUploadFolder() {
        return uploadFolder;
    }

    public void setUploadFolder(Folder uploadFolder) {
        this.uploadFolder = uploadFolder;
    }

    public Folder getPropertyFolder() {
        return propertyFolder;
    }

    public void setPropertyFolder(Folder propertyFolder) {
        this.propertyFolder = propertyFolder;
    }

    public Folder getScriptFolder() {
        return scriptFolder;
    }

    public void setScriptFolder(Folder scriptFolder) {
        this.scriptFolder = scriptFolder;
    }

    public Folder getDataFolder() {
        return dataFolder;
    }

    public void setDataFolder(Folder dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void deleteChartFile(Chart chart) throws FileManagerException {
        deleteFile(getDataFolder(), chart.getFileName());
    }

    public void saveChartFile(Chart chart, String type) throws FileManagerException {
        try {
            ImageIO.write(chart.getChartImage(), type, new File(getDataFolder(), chart.getFileName()));
        } catch (IOException e) {
            throw new FileManagerException("Unable to write: " + chart.getFileName(), e);
        }
    }

    public void loadChartFile(Chart chart) throws FileManagerException {
        BufferedImage bi;
        try {
            LOGGER.debug("Loading chart file for chart: " + chart.getId());
            bi = ImageIO.read(new File(getDataFolder(), chart.getFileName()));
            chart.setChartImage(bi);
        } catch (IOException e) {
            throw new FileManagerException("Unable to read: " + chart.getFileName(), e);
        }
    }

    public String getArchiveDir() {
        return archiveDir;
    }

    public void setArchiveDir(String archiveDir) {
        this.archiveDir = archiveDir;
    }
}
