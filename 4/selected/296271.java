package de.cowabuja.pawotag.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EventObject;
import javax.crypto.NoSuchPaddingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.swing.event.EventListenerList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import de.cowabuja.pawotag.exception.BackupException;
import de.cowabuja.pawotag.exception.FileAlreadyExistsException;
import de.cowabuja.pawotag.exception.GeneralEncryptionException;
import de.cowabuja.pawotag.exception.InvalidPasswordException;

public class Pawotag {

    public static final String DATABASE_FILE_END = ".pwf";

    public static final String BACKUP_FILE_END = "bak";

    public static final String[] HSQLDB_EXTENSIONS = { "backup", "data", "properties", "script", "log", "lck" };

    private static final Logger log = Logger.getLogger(Pawotag.class);

    private EntityManagerFactory entityManagerFactory = null;

    private final String JPACONFIG = "pawotag";

    private EventListenerList listeners = new EventListenerList();

    private Encryptor encryptor;

    private File database;

    private File databaseEncrypted;

    private File databaseHiddenDir;

    private File backupDir;

    private int maxBackupFiles = 10;

    private EntityManager entityManager;

    public Pawotag(String password, File database) throws InvalidPasswordException, GeneralEncryptionException {
        setDatabase(database);
        setDatabaseEncryptedFile();
        setDatabaseHiddenDir();
        setDatabaseConfig();
        createEncryptor(password.toCharArray());
        decodeDatabase(database);
        hideWindowsFile();
    }

    private void hideWindowsFile() {
        if (SystemUtils.IS_OS_WINDOWS == true) {
            if (databaseHiddenDir.exists() == false) {
                databaseHiddenDir.mkdir();
            }
            ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "attrib", "+h", databaseHiddenDir.getPath());
            try {
                builder.start();
            } catch (IOException e) {
                log.error(e.toString(), e);
            }
        }
    }

    private void setDatabaseHiddenDir() {
        databaseHiddenDir = new File(getDatabase().getParent() + File.separator + "." + getDatabase().getName());
    }

    private File getDatabaseHiddenDir() {
        return databaseHiddenDir;
    }

    /**
	 * backup the database with default dest File
	 * 
	 * @throws BackupException
	 */
    public void backupDatabase() throws BackupException {
        backupDatabase(DateManager.getDate(new Date(), DateManager.FULL_DATE_TIME_1));
    }

    /**
	 * @param backupName
	 *            , your defined backupName for database
	 * @throws BackupException
	 */
    public void backupDatabase(String backupName) throws BackupException {
        try {
            cleanBackupDir();
            encryptDatabase(new File(getBackupDir() + File.separator + backupName + "." + BACKUP_FILE_END), false);
        } catch (GeneralEncryptionException e) {
            throw new BackupException(e);
        }
    }

    public void restoreBackup(File backupFile, File destDir, boolean replaceExistingFile) throws BackupException, FileAlreadyExistsException {
        try {
            File databaseEncryptionFile = new File(destDir.getPath() + File.separator + getDatabase() + DATABASE_FILE_END);
            if (databaseEncryptionFile.exists() == true && replaceExistingFile == false) {
                throw new FileAlreadyExistsException("File " + databaseEncryptionFile.getName() + "already exists");
            }
            IOUtils.copy(FileUtils.openInputStream(backupFile), FileUtils.openOutputStream(databaseEncryptionFile));
        } catch (FileNotFoundException e) {
            throw new BackupException(e);
        } catch (IOException e) {
            throw new BackupException(e);
        }
    }

    /**
	 * delete old backup files. maxBackupFiles define your count for old files
	 */
    @SuppressWarnings("unchecked")
    private void cleanBackupDir() {
        String[] backupFileEnd = { BACKUP_FILE_END };
        Collection<File> backupFileCollection = FileUtils.listFiles(getBackupDir(), backupFileEnd, false);
        File[] backupFiles = backupFileCollection.toArray(new File[backupFileCollection.size()]);
        if (backupFiles.length > maxBackupFiles) {
            Arrays.sort(backupFiles, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            for (int i = maxBackupFiles; i < backupFiles.length - 1; i++) {
                backupFiles[i].delete();
            }
        }
    }

    /**
	 * 
	 * @return default backup directory e.c. /home/../test_backup
	 */
    private File getBackupDir() {
        if (backupDir == null) {
            backupDir = new File(getDatabase().getPath() + "_backup");
            backupDir.mkdirs();
        }
        return backupDir;
    }

    /**
	 * Is to set, if you not want the default backupDir
	 * 
	 * @param backupDir
	 *            directory for your backup dir
	 */
    public void setBackupDir(File backupDir) {
        this.backupDir = backupDir;
        this.backupDir.mkdirs();
    }

    public EntityManager getEntityManager() {
        if (entityManager == null) {
            if (entityManagerFactory == null || entityManagerFactory.isOpen() == false) {
                entityManagerFactory = Persistence.createEntityManagerFactory(JPACONFIG);
            }
            entityManager = new PawotagEntityManager(entityManagerFactory.createEntityManager(), this);
        }
        return entityManager;
    }

    public void close() {
        getEntityManager().close();
        if (entityManagerFactory != null && entityManagerFactory.isOpen() == true) {
            entityManagerFactory.close();
        }
    }

    public void addDataUpdateListener(DataUpdateListener listener) {
        listeners.add(DataUpdateListener.class, listener);
    }

    public synchronized void notifyDataUpdate(EventObject event) {
        for (DataUpdateListener listener : listeners.getListeners(DataUpdateListener.class)) {
            listener.dataUpdated(event);
        }
    }

    private void createEncryptor(char[] password) throws GeneralEncryptionException {
        try {
            this.encryptor = new Encryptor(password);
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralEncryptionException(e);
        } catch (NoSuchPaddingException e) {
            throw new GeneralEncryptionException(e);
        } catch (InvalidKeySpecException e) {
            throw new GeneralEncryptionException(e);
        }
    }

    /**
	 * 
	 * @param database
	 * @throws InvalidPasswordException
	 * @throws GeneralEncryptionException
	 */
    private void decodeDatabase(File database) throws InvalidPasswordException, GeneralEncryptionException {
        if (isPawotagPasswordFile(database) == true && database.exists()) {
            File tmpFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "pawotag.tmp");
            try {
                this.encryptor.decodeFile(database, tmpFile, false);
            } catch (InvalidKeyException e) {
                throw new GeneralEncryptionException(e);
            } catch (InvalidAlgorithmParameterException e) {
                throw new GeneralEncryptionException(e);
            } catch (IOException e) {
                throw new GeneralEncryptionException(e);
            }
            try {
                Archivator.extractTarGzArchive(tmpFile, getDatabaseHiddenDir(), true);
            } catch (IOException e) {
                throw new InvalidPasswordException();
            } finally {
                tmpFile.delete();
            }
        } else {
            log.debug("####### neue Datenbank angelegt...");
        }
    }

    public void encryptDatabase() throws GeneralEncryptionException, BackupException {
        encryptDatabase(getDatabaseEncryptionFile(), true);
    }

    /**
	 * 
	 * @throws GeneralEncryptionException
	 * @throws BackupException
	 */
    @SuppressWarnings("unchecked")
    private void encryptDatabase(File dest, boolean deleteDatabaseFilesAfterArchivate) throws GeneralEncryptionException {
        Collection<File> files = FileUtils.listFiles(getDatabaseHiddenDir(), HSQLDB_EXTENSIONS, true);
        ArrayList<File> databaseList = new ArrayList<File>();
        for (File file : files) {
            databaseList.add(file);
        }
        File tmpFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "pawotag.tmp");
        try {
            Archivator.createTarGzipArchive(databaseList, tmpFile, deleteDatabaseFilesAfterArchivate, deleteDatabaseFilesAfterArchivate);
            getEncryptor().encodeFile(tmpFile, dest, true);
        } catch (IOException e) {
            throw new GeneralEncryptionException(e);
        } catch (InvalidKeyException e) {
            throw new GeneralEncryptionException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new GeneralEncryptionException(e);
        }
    }

    public Encryptor getEncryptor() {
        return encryptor;
    }

    private void setDatabaseEncryptedFile() {
        if (isPawotagPasswordFile(getDatabase()) == true) {
            this.databaseEncrypted = getDatabase();
        } else {
            this.databaseEncrypted = new File(getDatabase().getPath() + DATABASE_FILE_END);
        }
    }

    private File getDatabaseEncryptionFile() {
        return this.databaseEncrypted;
    }

    private void setDatabaseConfig() {
        System.setProperty("hibernate.connection.url", "jdbc:hsqldb:file:" + getDatabaseHiddenDir().getPath() + File.separator + "data" + ";shutdown=true");
    }

    private void setDatabase(File file) {
        if (isPawotagPasswordFile(file) == true) {
            this.database = new File(file.getPath().substring(0, file.getPath().lastIndexOf(DATABASE_FILE_END)));
        } else {
            this.database = file;
        }
    }

    private File getDatabase() {
        return this.database;
    }

    public static boolean isPawotagPasswordFile(File passwordFile) {
        if (passwordFile != null && passwordFile.getName().endsWith(DATABASE_FILE_END)) {
            return true;
        }
        return false;
    }
}
