package pt.igeo.snig.mig.editor.ui.recordEditor.htmlView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JOptionPane;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.log4j.Logger;
import pt.igeo.snig.mig.editor.config.ConfigManager;
import pt.igeo.snig.mig.editor.constants.Constants;
import pt.igeo.snig.mig.editor.i18n.StringsManager;
import pt.igeo.snig.mig.editor.record.Record;
import pt.igeo.snig.mig.editor.record.filter.SimpleFileFilter;
import fi.mmm.yhteinen.swing.core.YModel;
import fi.mmm.yhteinen.swing.core.tools.YUIToolkit;

/**
 * Model for the html view component.
 * 
 * @author Ant�nio Silva
 * @version $Revision: 9249 $
 * @since 1.0
 */
public class HtmlViewModel extends YModel {

    /** Logger for this class */
    private Logger logger = Logger.getLogger(HtmlViewModel.class);

    /** the tree to select forms */
    private Record currentRecord = null;

    /** Maps the records to the current selected xsls */
    private File selectedTrans = null;

    /** collection of transformers */
    private Collection<File> transformers = null;

    /** stores the initial transformation to set from config file */
    private File initTrans = null;

    /**
	 * returns the choice tree
	 * 
	 * @return the tree POJO
	 */
    public Record getRecord() {
        return currentRecord;
    }

    /**
	 * Set's the record
	 * 
	 * @param record
	 */
    public void setRecord(Record record) {
        if (currentRecord == null) {
            selectedTrans = initTrans;
        }
        currentRecord = record;
        notifyObservers();
    }

    /**
	 * Initializes the transformer collection
	 * 
	 * @param dirname
	 */
    private void initializeTransformers(String dirname) {
        transformers = new ArrayList<File>();
        File dir = new File(dirname);
        File[] files = dir.listFiles(new SimpleFileFilter(false, "xsl"));
        String selected = ConfigManager.getInstance().getLastXslt();
        logger.debug("Selected transformer was " + selected);
        for (int i = 0; i < files.length; i++) {
            logger.debug("FILE: " + files[i].getName());
            if (files[i].getName().equals(selected)) {
                initTrans = files[i];
            }
            transformers.add(files[i]);
        }
    }

    /**
	 * 
	 * @return the collection with existing transformers
	 */
    public Collection<File> getTransformers() {
        if (transformers == null) {
            initializeTransformers(Constants.transformerDir);
        }
        return transformers;
    }

    /**
	 * transforms current xml
	 * 
	 * @param f the xslt file
	 * @return the absolute path of the generated html file, or null if no file has been generated
	 */
    public String updateTransform(File f) {
        logger.debug("Transforming using: " + f.getName());
        File temp = new File(Constants.transformerTempDir + File.separator + Constants.defaultTempHtml);
        if (currentRecord != null) {
            try {
                logger.debug("Deleting " + Constants.transformerTempDir + " if exists...");
                if (new File(Constants.transformerTempDir).exists()) {
                    logger.debug("Dir " + Constants.transformerTempDir + " already in use. Clean temp contents");
                    if (deleteDirContents(new File(Constants.transformerTempDir), Constants.defaultTempHtml, 0) == false) throw new IOException("Problem erasing dir");
                } else {
                    logger.debug("Creating " + Constants.transformerTempDir + " directory");
                    new File(Constants.transformerTempDir).mkdir();
                }
                Transformer tx = TransformerFactory.newInstance().newTransformer(new StreamSource(f));
                tx.transform(new DOMSource(currentRecord.getDocument()), new StreamResult(temp));
                String resourcesDir = Constants.transformerDir + "/" + getFileNameWithoutExtension(f);
                logger.debug("Copying directory " + resourcesDir + " contents into " + Constants.transformerTempDir);
                copyFiles(new File(resourcesDir), new File(Constants.transformerTempDir));
                logger.debug("Update transform finished well.");
                return temp.getAbsolutePath();
            } catch (Exception e) {
                temp.delete();
                logger.debug("Conversion problem: " + e);
            }
        }
        return null;
    }

    /**
	 * 
	 * @param item
	 */
    public void setSelectedTransformer(Object item) {
        if (currentRecord != null) {
            ConfigManager.getInstance().setLastXslt(((File) item).getName());
            selectedTrans = (File) item;
        }
    }

    /**
	 * 
	 * @return the selected transformer
	 */
    public Object getSelectedTransformer() {
        if (currentRecord != null) {
            return selectedTrans;
        } else {
            return null;
        }
    }

    /**
	 * Exports the current transformation to an HTML file
	 * 
	 * @param destDir
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public void exportHtml(File destDir) throws FileNotFoundException, IOException {
        String destPath = destDir.toString();
        String oldMainFile = destPath + File.separator + Constants.defaultTempHtml;
        String newMainFile = destPath + File.separator + destDir.getName() + ".html";
        if ((new File(destPath)).exists()) {
            String title = StringsManager.getInstance().getFormattedStringPlain("exportHtmlReplaceWarningTitle", destDir.getName());
            String text = StringsManager.getInstance().getString("exportHtmlReplaceWarningMessage");
            Object[] options = { StringsManager.getInstance().getString("yes"), StringsManager.getInstance().getString("no") };
            int answer = JOptionPane.showOptionDialog(YUIToolkit.getCurrentWindow(), text, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (answer == 0) {
                logger.debug("User chose to replace dir contents, deleting it.");
                if (deleteDir(new File(destPath)) == false) throw new IOException("Problem erasing dir");
            } else {
                logger.debug("User cancelled replacing of dir.");
                return;
            }
        }
        logger.debug("Creating " + destPath + " directory");
        new File(destPath).mkdir();
        logger.debug("Copying contents of " + Constants.transformerTempDir + " dir into " + destPath);
        copyFiles(new File(Constants.transformerTempDir), new File(destPath));
        logger.debug("Renaming " + oldMainFile + " into " + newMainFile);
        new File(oldMainFile).renameTo(new File(newMainFile));
        logger.debug("Successful export!");
    }

    /**
	 * Exports the current transformation to an HTML file
	 * 
	 * @param destDir
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public void exportXSL(File destDir) throws FileNotFoundException, IOException {
        String destPath = destDir.toString();
        if ((new File(destPath)).exists()) {
            String title = StringsManager.getInstance().getFormattedStringPlain("exportHtmlReplaceWarningTitle", destDir.getName());
            String text = StringsManager.getInstance().getString("exportHtmlReplaceWarningMessage");
            Object[] options = { StringsManager.getInstance().getString("yes"), StringsManager.getInstance().getString("no") };
            int answer = JOptionPane.showOptionDialog(YUIToolkit.getCurrentWindow(), text, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (answer == 0) {
                logger.debug("User chose to replace dir contents, deleting it.");
                if (deleteDir(new File(destPath)) == false) throw new IOException("Problem erasing dir");
            } else {
                logger.debug("User cancelled replacing of dir.");
                return;
            }
        }
        logger.debug("Creating " + destPath + " directory");
        new File(destPath).mkdir();
        logger.debug("Copying contents of " + Constants.transformerTempDir + File.separator + destDir.getName() + " dir into " + destPath);
        copyFiles(new File(Constants.transformerDir + File.separator + destDir.getName()), new File(destPath));
        copyFiles(new File(Constants.transformerDir + File.separator + destDir.getName() + ".xsl"), new File(destPath + File.separator + destDir.getName() + ".xsl"));
        logger.debug("Successful export!");
    }

    /**
	 * Example: xcv.txt returns xcv
	 * @param file source file
	 * @return the file's name without extension
	 */
    private String getFileNameWithoutExtension(File file) {
        String name = file.getName();
        int i = name.lastIndexOf('.');
        return name.substring(0, i);
    }

    /**
	 * Copies directories recursively
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
    public static void copyFiles(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            dest.mkdirs();
            for (String f : src.list()) {
                String df = dest.getPath() + File.separator + f;
                String sf = src.getPath() + File.separator + f;
                copyFiles(new File(sf), new File(df));
            }
        } else {
            FileInputStream fin = new FileInputStream(src);
            FileOutputStream fout = new FileOutputStream(dest);
            int c;
            while ((c = fin.read()) >= 0) fout.write(c);
            fin.close();
            fout.close();
        }
    }

    /**
	 * Deletes directory, even if not empty
	 * @param dir directory do delete
	 * @return true if all the files were successfully deleted
	 */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            for (String child : dir.list()) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**
	 * Same as previous, omit one specified file (due to file locking)
	 * @param dir
	 * @param ignoredFile
	 * @param level should be called with 0, so the own directory isn't deleted
	 * @return true if operation went well
	 */
    public static boolean deleteDirContents(File dir, String ignoredFile, int level) {
        if (dir.isDirectory()) {
            for (String child : dir.list()) {
                boolean success = deleteDirContents(new File(dir, child), ignoredFile, level + 1);
                if (!success) {
                    return false;
                }
            }
        }
        if (dir.getName().equals(ignoredFile)) return true;
        if (level == 0) return true;
        return dir.delete();
    }

    /**
	 * Import the xsl file into MIG workspace
	 * @param absolutePath
	 * @param name
	 * @param destDirName 
	 * @param destFileName 
	 * @throws IOException 
	 */
    public void importXSL(String absolutePath, String name, String destDirName, String destFileName) throws IOException {
        File destDir = new File(destDirName);
        File destFile = new File(destFileName);
        if (destDir.exists()) {
            if (deleteDir(destDir) == false) {
                throw new IOException("Could not delete old directory.");
            } else {
                for (File tempFile : transformers) {
                    if (tempFile.getName().equals(destFile.getName())) {
                        transformers.remove(tempFile);
                    }
                }
            }
        }
        if (destFile.exists()) {
            if (destFile.delete() == false) {
                throw new IOException("Could not delete old file.");
            }
        }
        logger.debug("Creating " + destDirName + " directory");
        destDir.mkdir();
        String origDir = absolutePath.substring(0, absolutePath.lastIndexOf('.'));
        File oldDir = new File(origDir);
        if (oldDir.exists() && oldDir.isDirectory()) {
            logger.debug("Copying contents of " + origDir + " dir into " + destDirName);
            copyFiles(oldDir, destDir);
        } else {
            logger.debug("Creating empty directory " + destDirName);
            if (oldDir.exists()) {
                deleteDir(oldDir);
            }
            oldDir.mkdir();
        }
        logger.debug("Copying " + absolutePath + " into " + destFileName);
        copyFiles(new File(absolutePath), destFile);
        transformers.add(destFile);
        notifyObservers();
        logger.debug("Successful import!");
    }
}
