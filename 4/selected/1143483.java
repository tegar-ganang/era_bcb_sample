package jamm;

import jamm.utils.JammEnum;
import jamm.utils.WriteLog;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Class to initialize data for JAMM
 * 
 * @author Andreas Freitag (nexxx85)
 * 
 */
public class Initialize {

    private Options options;

    private WriteLog logger;

    public Initialize(Options options) {
        this.options = options;
        logger = new WriteLog(options);
    }

    public void initialize() {
        checkOptionsFolderFiles();
        options.readOptions();
        options.updateFolderPath();
        checkLogFolderFiles();
        checkDbFolderFiles();
        if (options.getAccounts_file().exists()) {
            options.readArray(JammEnum.READ_ACCOUNTS);
        }
        setLookAndFeel();
        if (!(options.getCurrent_locale() == null) && !options.getCurrent_locale().isEmpty()) {
            Locale.setDefault(new Locale(options.getCurrent_locale()));
        }
        logger.writeLog(JammEnum.INFO, "---------------------");
        logger.writeLog(JammEnum.INFO, "Initialize finished");
        logger.writeLog(JammEnum.INFO, "---------------------");
    }

    private void checkLogFolderFiles() {
        boolean logfolder_created = false;
        boolean logfile_renamed = false;
        if (options.getLog_file_folder().isDirectory() == false) {
            options.getLog_file_folder().mkdirs();
            logfolder_created = true;
        }
        if (options.getLog_file().exists()) {
            logfile_renamed = true;
            options.getLog_file().renameTo(new File(options.getLog_file() + ".old"));
        }
        if (logfolder_created) {
            logger.writeLog(JammEnum.WARNING, "Jamm Logfolder not existing");
            logger.writeLog(JammEnum.INFO, "Jamm Logfolder created");
        }
        if (logfile_renamed) {
            logger.writeLog(JammEnum.WARNING, "Logfile already existing");
            logger.writeLog(JammEnum.INFO, "Old Logfile renamed to *.log.bak");
        }
    }

    private void checkOptionsFolderFiles() {
        if (options.getOptions_folder().exists() == false) {
            options.getOptions_folder().mkdirs();
            logger.writeLog(JammEnum.WARNING, "Jamm Optionsfolder not existing");
            logger.writeLog(JammEnum.INFO, "Jamm Optionsfolder created");
        }
        if (options.getOptions_file().exists() == false) {
            logger.writeLog(JammEnum.WARNING, "Options file does not exist");
            options.writeOptions();
            logger.writeLog(JammEnum.INFO, "New Options file with default values created");
        }
    }

    private void checkDbFolderFiles() {
        if (options.getDb_folder().exists() == false) {
            options.getDb_folder().mkdirs();
            logger.writeLog(JammEnum.WARNING, "Jamm database-Folder not existing");
            logger.writeLog(JammEnum.INFO, "Jamm database-Folder created");
        }
        if (options.getDb_file().exists() == false) {
            logger.writeLog(JammEnum.WARNING, "Database does not exist");
            try {
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(options.getDb_file())));
                int c;
                while ((c = options.getDb_backupFile().read()) != -1) {
                    out.writeByte(c);
                }
                options.getDb_backupFile().close();
                out.close();
            } catch (IOException e) {
                System.err.println("Error Writing/Reading Streams.");
            }
            logger.writeLog(JammEnum.INFO, "Database created");
        }
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
            logger.writeLog(JammEnum.INFO, "LookAndFeel set to Nimbus");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            logger.writeLog(JammEnum.WARNING, "Nimbus LookAndFeel not found. Using standard LookAndFeel instead");
            e.printStackTrace();
        }
    }
}
