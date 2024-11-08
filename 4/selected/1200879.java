package com.bluebrim.solitarylayouteditor;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import com.bluebrim.gui.client.CoGUI;

/**
 * Instances of this class is used by objects for loading from and saving data in a file.
 * A <code>CoFileStoreSupport</code> interacts with its owner by the 
 * <code>CoFileable</code> interface and handles all issues concerning
 * loading and saving on file.
 * 
 * @author G�ran St�ck 2002-10-09
 */
public class CoFileStoreSupport {

    /**
     * Type value indicating that the <code>CoFileStoreSupport</code> supports an 
     * "Save" file operation.
     */
    public static final int SAVE = 0;

    /**
     * Type value indicating that the <code>CoFileStoreSupport</code> supports a
     * "Save as" file operation.
     */
    public static final int SAVE_AS = 1;

    /**
     * Return value if the save operation was succesful
     */
    public static final int SAVE_SUCCEEDED = 0;

    /**
     * Return value if the save operation was canceled.
     */
    public static final int SAVE_CANCELED = 1;

    private CoFileable m_fileable;

    private File m_file;

    private FileFilter m_fileFilter;

    private JFileChooser m_fileChooser;

    private boolean m_zip;

    public CoFileStoreSupport(FileFilter fileFilter, boolean zip) {
        m_zip = zip;
        m_fileFilter = fileFilter;
        m_fileChooser = new JFileChooser();
        m_fileChooser.setFileFilter(m_fileFilter);
    }

    public CoFileStoreSupport(FileFilter fileFilter) {
        this(fileFilter, false);
    }

    /**
	 * Convenience method that reduce code redundancy in the class
	 * that implements <code>CoFileable</code>
	 */
    public int save(int operationType) throws IOException, RuntimeException {
        if (operationType == SAVE) return save(); else return saveAs();
    }

    public int save() throws IOException, RuntimeException {
        if (m_file == null) return saveAs(); else return save(m_file);
    }

    /**
	 * This method is named after the common menu item with the same name
	 * that most application with file storage have. It gives the user a
	 * possibility to save the data under a different name. But the user
	 * is not prohibited to choose the file that the data was loaded from and
	 * if he do so, he will get the question: 
	 * "File already exsists. Do you want to replace it?"
	 */
    public int saveAs() throws IOException, RuntimeException {
        File file = chooseFileForSaving();
        if (file == null) return SAVE_CANCELED; else if (file.exists() && !CoGUI.confirm("Filen finns redan. Vill du ers�tta den?", m_fileable.getComponent())) return SAVE_CANCELED; else if (!file.exists() || file.canWrite()) save(file); else {
            CoGUI.error("Det �r inte till�tet att ers�tta: " + file.getName(), m_fileable.getComponent());
            return SAVE_CANCELED;
        }
        return SAVE_SUCCEEDED;
    }

    public File chooseFileForLoading(Component parent) {
        int result = m_fileChooser.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) return null; else {
            m_file = m_fileChooser.getSelectedFile();
            return m_file;
        }
    }

    /**
	 * The zip file that the user chooses is unziped in
	 * a temporary directory. The archive is supposed to 
	 * contain one file named to the CoFileable.getNameInZipArchive.
	 * That file is returned. The caller is responsible for
	 * deleting all the files and the parent dictionary after
	 * they have been processed in the loading operation
	 */
    public File chooseZipFileForLoading(Component parent, String nameInZipArchive) {
        File selectedZipFile = null;
        File mainFile = null;
        int result = m_fileChooser.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) return null; else {
            selectedZipFile = m_fileChooser.getSelectedFile();
            try {
                mainFile = unZip(selectedZipFile, nameInZipArchive);
            } catch (ZipException e) {
                CoGUI.error("Fel vid uppackning av " + selectedZipFile.getName());
                return null;
            } catch (IOException e) {
                CoGUI.error("Fel vid l�sning av " + selectedZipFile.getName());
                return null;
            }
        }
        m_file = selectedZipFile;
        return mainFile;
    }

    private File chooseFileForSaving() {
        if (m_file == null) m_fileChooser.setSelectedFile(new File(m_fileable.getName())); else m_fileChooser.setCurrentDirectory(m_file.getParentFile());
        int result = m_fileChooser.showSaveDialog(m_fileable.getComponent());
        if (result != JFileChooser.APPROVE_OPTION) return null; else return m_fileChooser.getSelectedFile();
    }

    /**
	 * Delete all files in the directory and the directory as well
	 */
    private void deleteDirectory(File dir) {
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
        ;
        dir.delete();
    }

    public String getFileName() {
        if (m_file != null) return m_file.getName(); else return "";
    }

    public boolean hasFile() {
        return (m_file != null);
    }

    /**	
	 * First the data is written to a temporary file and then renamed to the specified file.
	 * If any exception occurs during the execution of writeToStream, the renaming operation
	 * is not performed, and thus does not corrupt the specified file.
	 * This operation can generate more than one file when for example the CoFileable is exporting
	 * xml with images that is written to separate files. Use the saveZip-method if you want a singel file
	 * in these cases.
	 */
    private int save(File file) throws IOException, RuntimeException {
        if (m_zip) return saveZip(file);
        File tempFile = File.createTempFile("tmp" + System.currentTimeMillis(), null, file.getParentFile());
        tempFile.deleteOnExit();
        FileOutputStream stream = new FileOutputStream(tempFile);
        List attachements = new ArrayList();
        try {
            m_fileable.writeToStream(stream, attachements);
        } catch (Exception e) {
            stream.close();
            tempFile.delete();
            throw new RuntimeException(e);
        }
        stream.close();
        copyAttachementFiles(attachements, file.getParentFile());
        renameTempFile(file, tempFile);
        return SAVE_SUCCEEDED;
    }

    /**
	 * Copy the files in the list to the specified directory
	 * @param attachements list with the files to copy
	 * @param file directory where the copies is created
	 */
    private void copyAttachementFiles(List attachements, File directory) {
        Iterator iter = attachements.iterator();
        while (iter.hasNext()) {
            File attachement = (File) iter.next();
            File copy = new File(directory, attachement.getName());
            copyFile(attachement, copy);
        }
    }

    /**
	 * Convenience utility to copy file
	 * @param src The source file to copy from
	 * @param dest The destination file to copy to
	 */
    public static void copyFile(File src, File dest) {
        try {
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dest);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

    /**
	 * Write data to a file that is zipped together with the attachment files to
	 * a singel file that is named as specified in file argument. <br>
	 * Without redesigning the xml framework we can't save attachment files
	 * directly to zip stream. That's bad but together with the fact that 
	 * <code>CoImageContent</code> also creates a temporary file when creating
	 * the attachement file makes the whole thing realy bad. 
	 */
    private int saveZip(File file) throws IOException, RuntimeException {
        File tempFile = File.createTempFile("tmp" + System.currentTimeMillis(), null, file.getParentFile());
        tempFile.deleteOnExit();
        FileOutputStream stream = new FileOutputStream(tempFile);
        ZipOutputStream zipStream = new ZipOutputStream(stream);
        zipStream.setLevel(Deflater.NO_COMPRESSION);
        zipStream.putNextEntry(new ZipEntry(m_fileable.getNameInZipArchive()));
        List attachements = new ArrayList();
        try {
            m_fileable.writeToStream(zipStream, attachements);
        } catch (Exception e) {
            zipStream.close();
            throw new RuntimeException(e);
        }
        zipAttachments(attachements, zipStream);
        zipStream.close();
        renameTempFile(file, tempFile);
        return SAVE_SUCCEEDED;
    }

    /**
	 * Create a directory at the place as the specified file. Unzip the
	 * file and put all unzipped files in directory. Return the file with 
	 * the specified internal zip file name.
	 */
    private File unZip(File file, String nameInZipArchive) throws ZipException, IOException {
        File tempDir = new File(file.getParentFile(), "tmp" + System.currentTimeMillis());
        tempDir.deleteOnExit();
        if (!tempDir.mkdir()) throw new IOException("Could not create temporary directory in" + file.getParent());
        return unzipArchive(file, tempDir, nameInZipArchive);
    }

    private void streamToFile(InputStream in, File dest) throws IOException {
        byte[] buffer = new byte[8192];
        FileOutputStream out = new FileOutputStream(dest);
        int length;
        while ((length = in.read(buffer, 0, buffer.length)) != -1) out.write(buffer, 0, length);
        out.close();
    }

    private void zipAttachments(List attachments, ZipOutputStream zipStream) throws IOException, FileNotFoundException {
        byte[] buf = new byte[1024];
        Iterator iter = attachments.iterator();
        while (iter.hasNext()) {
            File attachement = (File) iter.next();
            if (attachement.getName().equals(m_fileable.getNameInZipArchive())) continue;
            zipStream.putNextEntry(new ZipEntry(attachement.getName()));
            FileInputStream in = new FileInputStream(attachement);
            int len;
            while ((len = in.read(buf)) > 0) {
                zipStream.write(buf, 0, len);
            }
            zipStream.closeEntry();
            in.close();
        }
    }

    /**
	 * This method is called with prior confirmation from the user that
	 * it is ok to replace the file.
	 */
    private void renameTempFile(File file, File tmpFile) throws IOException {
        if (file.exists()) if (!file.delete()) throw new IOException("Unable to delete: " + file.getName());
        if (!tmpFile.renameTo(file)) throw new IOException("Unable to rename: " + tmpFile.getName() + " to " + file.getName());
        m_file = file;
    }

    public void setFileable(CoFileable fileable) {
        m_fileable = fileable;
    }

    /**
	 * Unzip all files in the zipArchive and put the files in the specified 
	 * outDir. Return the file with the specified internal zip file name.
	 */
    private File unzipArchive(File zipArchive, File outDir, String nameInZipArchive) throws IOException {
        File mainFile = null;
        ZipEntry entry = null;
        ZipInputStream zis = new ZipInputStream(new FileInputStream((zipArchive)));
        FileOutputStream fos = null;
        byte buffer[] = new byte[4096];
        int bytesRead;
        while ((entry = zis.getNextEntry()) != null) {
            File outFile = new File(outDir, entry.getName());
            if (entry.getName().equals(nameInZipArchive)) mainFile = outFile;
            fos = new FileOutputStream(outFile);
            while ((bytesRead = zis.read(buffer)) != -1) fos.write(buffer, 0, bytesRead);
            fos.close();
        }
        zis.close();
        return mainFile;
    }
}
