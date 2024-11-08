package acide.files.bytes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JOptionPane;
import acide.language.AcideLanguageManager;
import acide.log.AcideLog;

/**
 * ACIDE - A Configurable IDE byte file manager.
 * 
 * @version 0.8
 */
public class AcideByteFileManager {

    /**
	 * ACIDE - A Configurable IDE byte file manager unique class instance.
	 */
    private static AcideByteFileManager _instance;

    /**
	 * Returns the ACIDE - A Configurable IDE byte file manager unique class
	 * instance.
	 * 
	 * @return the ACIDE - A Configurable IDE byte file manager unique class
	 *         instance.
	 */
    public static AcideByteFileManager getInstance() {
        if (_instance == null) _instance = new AcideByteFileManager();
        return _instance;
    }

    /**
	 * Creates a new ACIDE - A Configurable IDE byte file manager.
	 */
    public AcideByteFileManager() {
    }

    /**
	 * Copies the content from the source file to the target file.
	 * 
	 * @param sourcePath
	 *            source file path.
	 * @param targetPath
	 *            target file path.
	 * @throws IOException.
	 */
    public void copy(String sourcePath, String targetPath) throws IOException {
        File sourceFile = new File(sourcePath);
        File targetFile = new File(targetPath);
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileInputStream = new FileInputStream(sourceFile);
            fileOutputStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) fileOutputStream.write(buffer, 0, bytesRead);
        } finally {
            if (fileInputStream != null) try {
                fileInputStream.close();
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(null, AcideLanguageManager.getInstance().getLabels().getString("s265") + sourcePath, AcideLanguageManager.getInstance().getLabels().getString("s266"), JOptionPane.ERROR_MESSAGE);
                AcideLog.getLog().error(exception.getMessage());
            }
            if (fileOutputStream != null) try {
                fileOutputStream.close();
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(null, AcideLanguageManager.getInstance().getLabels().getString("s267") + targetPath, AcideLanguageManager.getInstance().getLabels().getString("268"), JOptionPane.ERROR_MESSAGE);
                AcideLog.getLog().error(exception.getMessage());
            }
        }
    }

    /**
	 * Reallocates the source path into the target path.
	 * 
	 * @param source
	 *            source path.
	 * @param target
	 *            target path.
	 * 
	 * @return true if the operation succeed and false in other case.
	 */
    public boolean reallocateFile(String source, String target) {
        File sourceFile = new File(source);
        if (!sourceFile.exists()) return false;
        File targetFile = new File(target);
        if (targetFile.exists()) targetFile.delete();
        try {
            targetFile.createNewFile();
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(null, AcideLanguageManager.getInstance().getLabels().getString("s211"), AcideLanguageManager.getInstance().getLabels().getString("s210"), JOptionPane.ERROR_MESSAGE);
            AcideLog.getLog().error(AcideLanguageManager.getInstance().getLabels().getString("s210") + ": " + AcideLanguageManager.getInstance().getLabels().getString("s211"));
            exception.printStackTrace();
        }
        boolean saved = false;
        try {
            AcideByteFileManager.getInstance().copy(source, target);
            saved = true;
        } catch (IOException exception) {
            saved = false;
            JOptionPane.showMessageDialog(null, exception.getMessage(), AcideLanguageManager.getInstance().getLabels().getString("s945"), JOptionPane.ERROR_MESSAGE);
            AcideLog.getLog().error(exception.getMessage());
            exception.printStackTrace();
        }
        if (saved) AcideLog.getLog().info(AcideLanguageManager.getInstance().getLabels().getString("s212") + target); else AcideLog.getLog().error(AcideLanguageManager.getInstance().getLabels().getString("s213") + target);
        boolean deleted = sourceFile.delete();
        if (deleted) AcideLog.getLog().info(AcideLanguageManager.getInstance().getLabels().getString("s214") + source); else AcideLog.getLog().error(AcideLanguageManager.getInstance().getLabels().getString("s215") + source);
        return (saved && deleted);
    }
}
