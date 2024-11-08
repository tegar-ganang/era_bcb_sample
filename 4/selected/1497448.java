package net.sf.jadclipse;

import org.apache.log4j.Logger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author		V.Grishchenko
 */
public class JarClassExtractor {

    /**
 * Logger for this class
 */
    private static final Logger logger = Logger.getLogger(JarClassExtractor.class);

    /**
	 * extracts class files from jar/zip archive to specified path.
	 * See <code>IDecompiler</code> documentation for the format of
	 * pareameters.
	 */
    public static void extract(String archivePath, String packege, String className, boolean inner, String to) throws IOException {
        logger.info("extract() - start");
        ZipFile archive = new ZipFile(archivePath);
        logger.info("extract() - archivePath archivePath=" + archivePath);
        logger.info("extract() - archivePath className=" + className);
        logger.info("extract() - archivePath packege=" + packege);
        List entries = findRelevant(archive, packege, className, inner);
        InputStream in = null;
        OutputStream out = null;
        byte[] buffer = new byte[2048];
        ZipEntry entry;
        String outFile;
        int lastSep, amountRead;
        for (int i = 0; i < entries.size(); i++) {
            entry = (ZipEntry) entries.get(i);
            outFile = entry.getName();
            if ((lastSep = outFile.lastIndexOf('/')) != -1) outFile = outFile.substring(lastSep);
            try {
                in = archive.getInputStream(entry);
                if (in == null) throw new IOException("Zip file entry <" + entry.getName() + "> not found");
                out = new FileOutputStream(to + File.separator + outFile);
                while ((amountRead = in.read(buffer)) != -1) out.write(buffer, 0, amountRead);
            } finally {
                if (in != null) in.close();
                if (out != null) out.close();
            }
        }
        logger.info("extract() - end");
    }

    private static List findRelevant(ZipFile archive, String packege, String className, boolean inner) {
        logger.info("findRelevant() - start");
        String entryName = (packege.length() == 0) ? className : packege + "/" + className;
        String innerPrefix = entryName.substring(0, entryName.length() - 6) + "$";
        Enumeration entries = archive.entries();
        ZipEntry entry;
        String name;
        ArrayList relevant = new ArrayList();
        while (entries.hasMoreElements()) {
            entry = (ZipEntry) entries.nextElement();
            name = entry.getName();
            if (name.equals(entryName) || (name.startsWith(innerPrefix) && inner)) relevant.add(entry);
        }
        logger.info("findRelevant() - end");
        return relevant;
    }

    /**
	 * extracts class files from jar/zip archive to specified path.
	 * See <code>IDecompiler</code> documentation for the format of
	 * pareameters.
	 */
    public static void extract(String archivePath, String to) throws IOException {
        logger.info("extract() - start");
        ZipFile archive = new ZipFile(archivePath);
        logger.info("extract() - archivePath archivePath=" + archivePath);
        List entries = findRelevant(archive);
        InputStream in = null;
        OutputStream out = null;
        byte[] buffer = new byte[2048];
        ZipEntry entry;
        String outFile;
        int lastSep, amountRead;
        for (int i = 0; i < entries.size(); i++) {
            entry = (ZipEntry) entries.get(i);
            outFile = entry.getName();
            logger.info("extract() - ZipEntry outFile=" + outFile);
            if ((lastSep = outFile.lastIndexOf('/')) != -1) outFile = outFile.substring(lastSep);
            try {
                in = archive.getInputStream(entry);
                if (in == null) throw new IOException("Zip file entry <" + entry.getName() + "> not found");
                logger.info("extract() - ZipEntry entry=" + entry);
                logger.info("extract() - ZipEntry outFile=" + outFile);
                String outputfile = to + File.separator + outFile;
                logger.info("extract() - String outputfile=" + outputfile);
                out = new FileOutputStream(outputfile);
                while ((amountRead = in.read(buffer)) != -1) out.write(buffer, 0, amountRead);
            } finally {
                if (in != null) in.close();
                if (out != null) out.close();
            }
        }
        logger.info("extract() - end");
    }

    private static List findRelevant(ZipFile archive) {
        logger.info("findRelevant() - start");
        Enumeration entries = archive.entries();
        ZipEntry entry;
        String name;
        ArrayList relevant = new ArrayList();
        while (entries.hasMoreElements()) {
            entry = (ZipEntry) entries.nextElement();
            name = entry.getName();
            if (name.lastIndexOf("class") != -1) relevant.add(entry);
        }
        logger.info("findRelevant() - end");
        return relevant;
    }
}
