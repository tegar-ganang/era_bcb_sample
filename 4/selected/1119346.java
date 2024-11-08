package base.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import base.ConfigurationManager;
import base.IdManager;

public class BackupFactory {

    private static final transient Logger logger = Logger.getLogger(BackupFactory.class.getName());

    public static Backup newBackup(String name, String description, Date date, String dbLocationPath, String zipLocationPath) {
        int id = IdManager.getInstance().getNextBackupId();
        return new Backup(id, name, description, date, dbLocationPath, zipLocationPath);
    }

    public static Backup newBackupFromZipArchive(File backupFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        File unzipFolder = new File(backupFile.getParent(), backupFile.getName().substring(0, backupFile.getName().lastIndexOf(".")));
        unzipFolder.mkdir();
        unZipFile(new ZipFile(backupFile), unzipFolder);
        File xmlDetailsFile = new File(unzipFolder, ConfigurationManager.BACKUP_DETAILS_FILE);
        Document document = docBuilder.parse(new FileInputStream(xmlDetailsFile));
        Backup backup = new Backup(document);
        backup.setZipLocationPath(backupFile.getAbsolutePath());
        return backup;
    }

    public static File createDetailsFile(Backup backup) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        doc.appendChild(backup.toXml(doc));
        String detailsfileName = ConfigurationManager.BACKUP_DETAILS_FILE;
        if (!detailsfileName.endsWith(".xml")) {
            detailsfileName += ".xml";
        }
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult streamResult = new StreamResult(new PrintStream(detailsfileName));
        transformer.transform(source, streamResult);
        return new File(detailsfileName);
    }

    public static void backupToZipArchive(Backup backup) throws Exception {
        String outFilename = backup.getName() + backup.getId() + ".zip";
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(backup.getZipLocationPath(), outFilename)));
        out.setComment(backup.getDescription());
        String dbFileName = backup.getDbLocationPath().substring(backup.getDbLocationPath().lastIndexOf(File.separatorChar), backup.getDbLocationPath().length());
        backup.setDbLocationPath(new File(backup.getZipLocationPath(), dbFileName).getAbsolutePath());
        File details = createDetailsFile(backup);
        zipFile(details, out);
        details.delete();
        File resourcesFolder = new File(ConfigurationManager.RESOURCES_DIRECTORY);
        zipDirectory(resourcesFolder, out);
        out.close();
    }

    public static void zipDirectory(File file, ZipOutputStream out) throws Exception {
        String[] dirList = file.list();
        byte[] readBuffer = new byte[1024];
        int bytesIn = 0;
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(file, dirList[i]);
            if (f.isDirectory()) {
                zipDirectory(f, out);
                continue;
            }
            FileInputStream fis = new FileInputStream(f);
            ZipEntry zipEntry = new ZipEntry(f.getPath());
            out.putNextEntry(zipEntry);
            while ((bytesIn = fis.read(readBuffer)) != -1) {
                out.write(readBuffer, 0, bytesIn);
            }
            fis.close();
        }
    }

    public static void zipFile(File file, ZipOutputStream out) throws Exception {
        byte[] buf = new byte[1024];
        out.putNextEntry(new ZipEntry(file.getName()));
        FileInputStream in = new FileInputStream(file);
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.closeEntry();
    }

    public static void unZipFile(ZipFile zipFile, File destinationDirectory) throws Exception {
        logger.debug("unZipFile: " + zipFile.getName() + " destination directory: " + destinationDirectory);
        Enumeration enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
            File file = new File(destinationDirectory.getCanonicalPath(), zipEntry.getName().replaceFirst(".." + File.separatorChar, ""));
            logger.debug("File: " + file);
            if (zipEntry.isDirectory()) {
                file.mkdirs();
            } else {
                InputStream is = zipFile.getInputStream(zipEntry);
                BufferedInputStream bis = new BufferedInputStream(is);
                if (file.getParent() != null) {
                    File dir = new File(file.getParent());
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                int c;
                while ((c = bis.read()) != -1) {
                    bos.write((byte) c);
                }
                bos.close();
                fos.close();
            }
            logger.debug(" unzipped.");
        }
    }

    public static void unzip(ZipInputStream zin, String zipEntry, File destinationFolder) throws IOException {
        System.out.println("unzipping " + zipEntry + " to " + destinationFolder);
        FileOutputStream out = new FileOutputStream(new File(destinationFolder, new File(zipEntry).getName()));
        byte[] b = new byte[1024];
        int len = 0;
        while ((len = zin.read(b)) != -1) {
            out.write(b, 0, len);
        }
        out.close();
    }

    public static long fileSizeInKB(File file) {
        long size = 0;
        if (file.isDirectory()) {
            File[] filelist = file.listFiles();
            for (int i = 0; i < filelist.length; i++) {
                if (filelist[i].isDirectory()) {
                    size += fileSizeInKB(filelist[i]);
                } else {
                    size += filelist[i].length();
                }
            }
        } else {
            size += file.length();
        }
        return size / 1024;
    }
}
