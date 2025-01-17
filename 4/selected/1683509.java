package net.sourceforge.omov.core.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.sourceforge.omov.core.BusinessException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author christoph_pickl@users.sourceforge.net
 */
public final class ZipUtil {

    private static final Log LOG = LogFactory.getLog(ZipUtil.class);

    private static int BUFFER_SIZE = 8192;

    private ZipUtil() {
    }

    public static void unzip(File file, ZipFile zipFile, File targetDirectory) throws BusinessException {
        LOG.info("Unzipping zip file '" + file.getAbsolutePath() + "' to directory '" + targetDirectory.getAbsolutePath() + "'.");
        assert (file.exists() && file.isFile());
        if (targetDirectory.exists() == false) {
            LOG.debug("Creating target directory.");
            if (targetDirectory.mkdirs() == false) {
                throw new BusinessException("Could not create target directory at '" + targetDirectory.getAbsolutePath() + "'!");
            }
        }
        ZipInputStream zipin = null;
        try {
            zipin = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry = null;
            while ((entry = zipin.getNextEntry()) != null) {
                LOG.debug("Unzipping entry '" + entry.getName() + "'.");
                if (entry.isDirectory()) {
                    LOG.debug("Skipping directory.");
                    continue;
                }
                final File targetFile = new File(targetDirectory, entry.getName());
                final File parentTargetFile = targetFile.getParentFile();
                if (parentTargetFile.exists() == false) {
                    LOG.debug("Creating directory '" + parentTargetFile.getAbsolutePath() + "'.");
                    if (parentTargetFile.mkdirs() == false) {
                        throw new BusinessException("Could not create target directory at '" + parentTargetFile.getAbsolutePath() + "'!");
                    }
                }
                InputStream input = null;
                FileOutputStream output = null;
                try {
                    input = zipFile.getInputStream(entry);
                    if (targetFile.createNewFile() == false) {
                        throw new BusinessException("Could not create target file '" + targetFile.getAbsolutePath() + "'!");
                    }
                    output = new FileOutputStream(targetFile);
                    int readBytes = 0;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while ((readBytes = input.read(buffer, 0, buffer.length)) > 0) {
                        output.write(buffer, 0, readBytes);
                    }
                } finally {
                    FileUtil.closeCloseable(input);
                    FileUtil.closeCloseable(output);
                }
            }
        } catch (IOException e) {
            throw new BusinessException("Could not unzip file '" + file.getAbsolutePath() + "'!", e);
        } finally {
            FileUtil.closeCloseable(zipin);
        }
    }

    public static void zipDirectory(File sourceDirectory, File targetZipFile) throws BusinessException {
        LOG.info("Zipping directory '" + sourceDirectory.getAbsolutePath() + "' to file '" + targetZipFile.getAbsolutePath() + "'.");
        assert (sourceDirectory.exists() && sourceDirectory.isDirectory());
        assert (targetZipFile.exists() == false);
        ZipOutputStream zipout = null;
        boolean finishedSuccessfully = false;
        try {
            zipout = new ZipOutputStream(new FileOutputStream(targetZipFile));
            zipout.setLevel(9);
            zipFiles(zipout, sourceDirectory, sourceDirectory);
            zipout.finish();
            finishedSuccessfully = true;
        } catch (Exception e) {
            throw new BusinessException("Could not zip directory '" + sourceDirectory.getAbsolutePath() + "'!", e);
        } finally {
            FileUtil.closeCloseable(zipout);
            if (finishedSuccessfully == false) {
                if (targetZipFile.exists()) {
                    if (targetZipFile.delete() == false) {
                        LOG.warn("Could not delete zip file '" + targetZipFile.getAbsolutePath() + "'!");
                    }
                }
            }
        }
    }

    private static void zipFiles(ZipOutputStream zipout, File file, File sourceDirectory) throws IOException {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                zipFiles(zipout, subFile, sourceDirectory);
            }
        } else {
            final String entryName = getZipEntryName(file, sourceDirectory);
            LOG.debug("Zipping file '" + file.getAbsolutePath() + "' as entry '" + entryName + "'.");
            final ZipEntry entry = new ZipEntry(entryName);
            BufferedInputStream fileInput = null;
            try {
                fileInput = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
                byte[] buffer = new byte[BUFFER_SIZE];
                zipout.putNextEntry(entry);
                int count;
                while ((count = fileInput.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    zipout.write(buffer, 0, count);
                }
                zipout.closeEntry();
            } finally {
                FileUtil.closeCloseable(fileInput);
            }
        }
    }

    private static String getZipEntryName(File file, File sourceDirectory) {
        final String filePath = file.getAbsolutePath();
        return filePath.substring(sourceDirectory.getAbsolutePath().length() + 1, filePath.length());
    }

    public static void main(String[] args) throws BusinessException {
        File file = new File("/zip/asdf.script");
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(file);
        } catch (ZipException e) {
            System.out.println("invalid zip file");
            return;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return;
        }
        ZipUtil.unzip(file, zipFile, new File("/zip/unzippedCovers"));
    }
}
