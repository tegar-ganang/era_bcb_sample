package com.prolix.editor.resourcemanager.zip;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.channels.FileChannel;
import org.jdom.Document;
import uk.ac.reload.jdom.XMLUtils;
import com.prolix.editor.resourcemanager.exceptions.GLMRessourceFileException;
import com.prolix.editor.resourcemanager.exceptions.GLMRessourceManagerException;

/**
 * Manages the Tempory Directory in which the files are stored as log the a
 * specific Learning Design is open
 * 
 * @author Susanne Neumann, Stefan Zander, Philipp Prenner
 */
public class FileManager {

    private String path;

    protected FileManager(String path) {
        this.path = path;
    }

    protected String getPath() {
        return path;
    }

    protected String getInternalFileContent(String filename) throws GLMRessourceManagerException {
        return getFileContent(new File(getInternalResourcesDirectory(), filename));
    }

    protected String getFileContent(String filename) throws GLMRessourceManagerException {
        return getFileContent(new File(path, filename));
    }

    private String getFileContent(File file) throws GLMRessourceManagerException {
        if (file == null || !file.exists()) throw new GLMRessourceFileException(1);
        int len = 0;
        byte[] buffer = ContentManager.getDefaultBuffer();
        try {
            BufferedInputStream buff_in = new BufferedInputStream(new FileInputStream(file));
            ByteArrayOutputStream byte_out = new ByteArrayOutputStream();
            while ((len = buff_in.read(buffer)) > 0) byte_out.write(buffer, 0, len);
            return new String(byte_out.toByteArray());
        } catch (Exception e) {
            throw new GLMRessourceFileException(2);
        }
    }

    protected Document loadInternalXML(String filename) throws GLMRessourceFileException {
        return loadXML(getInternalResourcesDirectory(), filename);
    }

    protected Document loadXML(String filename) throws GLMRessourceFileException {
        return loadXML(path, filename);
    }

    /**
	 * reads a xml file and returns the xml document
	 * 
	 * @param filename
	 * @return
	 */
    private Document loadXML(String basis, String filename) throws GLMRessourceFileException {
        try {
            return XMLUtils.readXMLFile(new File(path, filename));
        } catch (Exception e) {
            e.printStackTrace();
            throw new GLMRessourceFileException(3);
        }
    }

    protected void saveInternalXML(String filename, Document document) throws GLMRessourceFileException {
        saveXML(getInternalResourcesDirectory(), filename, document);
    }

    protected void saveXML(String filename, Document document) throws GLMRessourceFileException {
        saveXML(path, filename, document);
    }

    private void saveXML(String basis, String filename, Document document) throws GLMRessourceFileException {
        try {
            XMLUtils.write2XMLFile(document, new File(basis, filename));
        } catch (Exception e) {
            throw new GLMRessourceFileException(4);
        }
    }

    protected File getFile(String filename) throws GLMRessourceFileException {
        return getFile(path, filename);
    }

    protected File getInternalFile(String filename) throws GLMRessourceFileException {
        return getFile(getInternalResourcesDirectory(), filename);
    }

    private String getInternalResourcesDirectory() {
        return path + File.separator + ContentManager.INTERNAL_RESOURCES_DIRECTORY_NAME;
    }

    protected void saveInternalContentFile(String filename, String content) throws GLMRessourceFileException {
        saveContentFile(getInternalResourcesDirectory(), filename, content);
    }

    protected void saveContentFile(String filename, String content) throws GLMRessourceFileException {
        saveContentFile(path, filename, content);
    }

    private void saveContentFile(String basis, String filename, String content) throws GLMRessourceFileException {
        FileWriter writer;
        try {
            writer = new FileWriter(new File(basis, filename), false);
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new GLMRessourceFileException(5);
        }
    }

    protected void deleteFile(String filename) throws GLMRessourceFileException {
        deleteFile(path, filename);
    }

    protected void deleteInternalFile(String filename) throws GLMRessourceFileException {
        deleteFile(getInternalResourcesDirectory(), filename);
    }

    protected void deleteInternalDirectory() throws GLMRessourceFileException {
        deleteDirectory(getInternalResourcesDirectory());
    }

    protected void copyInternalFile(String inputPath, String filename) throws GLMRessourceFileException {
        copyFile(inputPath, getInternalResourcesDirectory(), filename);
    }

    protected void copyFile(String inputPath, String filename) throws GLMRessourceFileException {
        copyFile(inputPath, path, filename);
    }

    private void copyFile(String inputPath, String basis, String filename) throws GLMRessourceFileException {
        try {
            FileChannel inChannel = new FileInputStream(new File(inputPath)).getChannel();
            File target = new File(basis, filename);
            FileChannel outChannel = new FileOutputStream(target).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inChannel.close();
            outChannel.close();
        } catch (Exception e) {
            throw new GLMRessourceFileException(7);
        }
    }

    protected void dispose() throws GLMRessourceFileException {
        deleteDirectory(path);
    }

    protected static File getFile(String root, String filename) throws GLMRessourceFileException {
        File ret = new File(root, filename);
        if (ret == null || !ret.exists()) throw new GLMRessourceFileException(1);
        return ret;
    }

    protected static void createDirectory(String path) throws GLMRessourceManagerException {
        if (!(new File(path)).mkdir()) throw new GLMRessourceFileException(9);
    }

    protected static void deleteFile(String root, String filename) throws GLMRessourceFileException {
        deleteFile(new File(root, filename));
    }

    protected static void deleteAbspolutFile(String path) throws GLMRessourceFileException {
        deleteFile(new File(path));
    }

    protected static void deleteFile(File file) throws GLMRessourceFileException {
        if (file == null || !file.exists()) throw new GLMRessourceFileException(6);
        file.delete();
    }

    protected static void deleteDirectory(String path) throws GLMRessourceFileException {
        deleteDirectory(new File(path));
    }

    protected static void deleteDirectory(File dir) throws GLMRessourceFileException {
        if (dir == null || !dir.exists() || !dir.isDirectory()) throw new GLMRessourceFileException(8);
        File[] childs = dir.listFiles();
        for (int i = 0; i < childs.length; i++) if (!childs[i].isDirectory()) childs[i].delete(); else deleteDirectory(childs[i]);
        dir.delete();
    }
}
