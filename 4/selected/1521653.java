package org.verus.ngl.lucene.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.logging.Logger;
import org.jdom.Element;
import org.verus.ngl.utilities.NGLXMLUtility;

/**
 *
 * @author root
 */
public class NGLIndexUtility {

    private static NGLIndexUtility nGLIndexUtility = null;

    public static final int BIBLIOGRAPHIC_INDEX = 1;

    public static final int BIBLIOGRAPHIC_TEMP_INDEX = 2;

    public static final int AUTHORITY_INDEX = 3;

    public static final int AUTHORITY_TEMP_INDEX = 4;

    public static final int BIBLIOGRAPHIC_INDEX_BACKUP = 5;

    public static final int BIBLIOGRAPHIC_TEMP_INDEX_BACKUP = 6;

    public static final int AUTHORITY_INDEX_BACKUP = 7;

    public static final int AUTHORITY_TEMP_INDEX_BACKUP = 8;

    private int indexType = 0;

    /** Creates a new instance of NGLIndexUtility */
    private NGLIndexUtility() {
    }

    public static NGLIndexUtility getInstance() {
        if (nGLIndexUtility == null) {
            nGLIndexUtility = new NGLIndexUtility();
        }
        return nGLIndexUtility;
    }

    @Deprecated
    private String getIndexFolderPath(int indexType) {
        String path = "";
        this.indexType = indexType;
        try {
            java.util.Properties prop = new java.util.Properties();
            String os = System.getProperty("os.name");
            System.out.println("Os name Identified");
            if (os.toUpperCase().indexOf("WINDOWS") != -1) {
                prop.load(new java.io.FileInputStream("c:/NGLv3/SystemFiles/ENV_VAR.txt"));
            } else if (os.toUpperCase().indexOf("LINUX") != -1) {
                prop.load(new java.io.FileInputStream("/usr/NGLv3/SystemFiles/ENV_VAR.txt"));
            } else {
                prop.load(new java.io.FileInputStream("c:/NGLv3/SystemFiles/ENV_VAR.txt"));
            }
            if (this.indexType == this.BIBLIOGRAPHIC_INDEX) {
                path = prop.getProperty("BIBLIOGRAPHIC_INDEX");
            } else if (indexType == this.BIBLIOGRAPHIC_TEMP_INDEX) {
                path = prop.getProperty("BIBLIOGRAPHIC_TEMP_INDEX");
            } else if (indexType == this.AUTHORITY_INDEX) {
                path = prop.getProperty("AUTHORITY_INDEX");
            } else if (indexType == this.AUTHORITY_TEMP_INDEX) {
                path = prop.getProperty("AUTHORITY_TEMP_INDEX");
            } else if (this.indexType == this.BIBLIOGRAPHIC_INDEX_BACKUP) {
                path = prop.getProperty("BIBLIOGRAPHIC_INDEX_BACKUP");
            } else if (indexType == this.BIBLIOGRAPHIC_TEMP_INDEX_BACKUP) {
                path = prop.getProperty("BIBLIOGRAPHIC_TEMP_INDEX_BACKUP");
            } else if (indexType == this.AUTHORITY_INDEX_BACKUP) {
                path = prop.getProperty("AUTHORITY_INDEX_BACKUP");
            } else if (indexType == this.AUTHORITY_TEMP_INDEX_BACKUP) {
                path = prop.getProperty("AUTHORITY_TEMP_INDEX_BACKUP");
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("path in get index path  " + path);
        return path;
    }

    public String getIndexFolderPath(Integer indexType, String databaseId) {
        this.indexType = indexType;
        try {
            String os = System.getProperty("os.name");
            System.out.println("Os name Identified");
            String envPath = "";
            if (os.toUpperCase().indexOf("WINDOWS") != -1) {
                envPath = "c:/NGLv3/" + databaseId + "/DB_ENV_VAR.xml";
            } else if (os.toUpperCase().indexOf("LINUX") != -1) {
                envPath = "/usr/NGLv3/" + databaseId + "/DB_ENV_VAR.xml";
            } else {
                envPath = "c:/NGLv3/" + databaseId + "/DB_ENV_VAR.xml";
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(envPath)));
            String xml = "";
            while (br.ready()) {
                xml += br.readLine() + "\n";
            }
            Element root = NGLXMLUtility.getInstance().getRootElementFromXML(xml);
            if (indexType == NGLIndexUtility.BIBLIOGRAPHIC_INDEX) {
                return root.getChildText("LuceneIndexDir");
            } else if (indexType == NGLIndexUtility.AUTHORITY_INDEX) {
                return root.getChildText("LuceneAFIndexDir");
            } else if (indexType == NGLIndexUtility.AUTHORITY_INDEX_BACKUP) {
                return root.getChildText("LuceneAFIndexBackupDir");
            }
        } catch (Exception e) {
            System.out.println("Exception in " + this.getClass().getCanonicalName() + " : " + e);
        }
        return "";
    }

    @Deprecated
    public boolean restoreLuceneIndex(int backupLocation, int indexLocation) {
        boolean result = false;
        try {
            System.out.println("Lucene restoring started");
            String backupPath = this.getIndexFolderPath(backupLocation);
            String indexPath = this.getIndexFolderPath(indexLocation);
            File dirBackup = new File(backupPath);
            File dirIndex = new File(indexPath);
            boolean flag = true;
            if (dirBackup.exists() && dirBackup.isDirectory()) {
                File files[] = dirBackup.listFiles();
                if (files != null && files.length > 0) {
                    File lMDir = this.getLastModifiedDirectory(files);
                    if (lMDir != null && lMDir.exists() && lMDir.isDirectory()) {
                        System.out.println("Last Modified date Dir : " + lMDir.getName());
                        if (!dirIndex.exists()) dirIndex.mkdir();
                        flag = this.copyFiles(lMDir, dirIndex);
                    }
                }
            }
            result = flag;
        } catch (Exception e) {
            System.out.println("Exception in restoreLuceneIndex Method : " + e);
        }
        return result;
    }

    private boolean copyFiles(File sourceDir, File destinationDir) {
        boolean result = false;
        try {
            if (sourceDir != null && destinationDir != null && sourceDir.exists() && destinationDir.exists() && sourceDir.isDirectory() && destinationDir.isDirectory()) {
                File sourceFiles[] = sourceDir.listFiles();
                if (sourceFiles != null && sourceFiles.length > 0) {
                    File destFiles[] = destinationDir.listFiles();
                    if (destFiles != null && destFiles.length > 0) {
                        for (int i = 0; i < destFiles.length; i++) {
                            if (destFiles[i] != null) {
                                destFiles[i].delete();
                            }
                        }
                    }
                    for (int i = 0; i < sourceFiles.length; i++) {
                        if (sourceFiles[i] != null && sourceFiles[i].exists() && sourceFiles[i].isFile()) {
                            String fileName = destFiles[i].getName();
                            File destFile = new File(destinationDir.getAbsolutePath() + "/" + fileName);
                            if (!destFile.exists()) destFile.createNewFile();
                            FileInputStream in = new FileInputStream(sourceFiles[i]);
                            FileOutputStream out = new FileOutputStream(destFile);
                            FileChannel fcIn = in.getChannel();
                            FileChannel fcOut = out.getChannel();
                            fcIn.transferTo(0, fcIn.size(), fcOut);
                        }
                    }
                }
            }
            result = true;
        } catch (Exception e) {
            System.out.println("Exception in copyFiles Method : " + e);
        }
        return result;
    }

    private File getLastModifiedDirectory(File files[]) {
        File file = null;
        try {
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i] != null && files[i].exists() && files[i].isDirectory()) {
                        file = files[i];
                        break;
                    }
                }
                for (int i = 0; i < files.length; i++) {
                    if (files[i] != null && files[i].exists() && files[i].isDirectory()) {
                        if (files[i].lastModified() > file.lastModified()) {
                            file = files[i];
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception in getLastModifiedDirectory Method : " + e);
        }
        return file;
    }

    @Deprecated
    public boolean backupLuceneIndex(int indexLocation, int backupLocation) {
        boolean result = false;
        try {
            System.out.println("lucene backup started");
            String indexPath = this.getIndexFolderPath(indexLocation);
            String backupPath = this.getIndexFolderPath(backupLocation);
            File inDir = new File(indexPath);
            boolean flag = true;
            if (inDir.exists() && inDir.isDirectory()) {
                File filesList[] = inDir.listFiles();
                if (filesList != null) {
                    File parDirBackup = new File(backupPath);
                    if (!parDirBackup.exists()) parDirBackup.mkdir();
                    String date = this.getDate();
                    backupPath += "/" + date;
                    File dirBackup = new File(backupPath);
                    if (!dirBackup.exists()) dirBackup.mkdir(); else {
                        File files[] = dirBackup.listFiles();
                        if (files != null) {
                            for (int i = 0; i < files.length; i++) {
                                if (files[i] != null) {
                                    files[i].delete();
                                }
                            }
                        }
                        dirBackup.delete();
                        dirBackup.mkdir();
                    }
                    for (int i = 0; i < filesList.length; i++) {
                        if (filesList[i].isFile()) {
                            try {
                                File destFile = new File(backupPath + "/" + filesList[i].getName());
                                if (!destFile.exists()) destFile.createNewFile();
                                FileInputStream in = new FileInputStream(filesList[i]);
                                FileOutputStream out = new FileOutputStream(destFile);
                                FileChannel fcIn = in.getChannel();
                                FileChannel fcOut = out.getChannel();
                                fcIn.transferTo(0, fcIn.size(), fcOut);
                            } catch (FileNotFoundException ex) {
                                System.out.println("FileNotFoundException ---->" + ex);
                                flag = false;
                            } catch (IOException excIO) {
                                System.out.println("IOException ---->" + excIO);
                                flag = false;
                            }
                        }
                    }
                }
            }
            System.out.println("lucene backup finished");
            System.out.println("flag ========= " + flag);
            if (flag) {
                result = true;
            }
        } catch (Exception e) {
            System.out.println("Exception in backupLuceneIndex Method : " + e);
            e.printStackTrace();
        }
        return result;
    }

    private String getDate() {
        String time = new Date().toString();
        String date = time.substring(4, 7);
        date += time.substring(8, 10);
        date += time.substring(24, 28);
        return date;
    }
}
