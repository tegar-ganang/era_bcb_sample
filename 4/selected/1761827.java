package cz.langteacher.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Go through specified direcotry and return all subdirs and files
 * @author libor
 *
 */
public final class FileUtils implements IFileUtils {

    private IConnectionStreamUtils connStreamUtils = new ConnectionStreamUtils();

    /**
	 * Demonstrate use.
	 * 
	 * @param aArgs - <tt>aArgs[0]</tt> is the full name of an existing 
	 * directory that can be read.
	 */
    public static void main(String... aArgs) throws FileNotFoundException {
        File startingDirectory = new File(aArgs[0]);
        List<File> files = new FileUtils().getFileListing(startingDirectory);
        for (File file : files) {
            System.out.println(file);
        }
    }

    @Override
    public List<File> getFileListing(File aStartingDir) throws FileNotFoundException {
        validateDirectory(aStartingDir);
        List<File> result = getFileListingNoSort(aStartingDir);
        Collections.sort(result);
        return result;
    }

    private List<File> getFileListingNoSort(File aStartingDir) throws FileNotFoundException {
        List<File> result = new ArrayList<File>();
        File[] filesAndDirs = aStartingDir.listFiles();
        List<File> filesDirs = Arrays.asList(filesAndDirs);
        for (File file : filesDirs) {
            result.add(file);
            if (!file.isFile()) {
                List<File> deeperList = getFileListingNoSort(file);
                result.addAll(deeperList);
            }
        }
        return result;
    }

    /**
	 * Directory is valid if it exists, does not represent a file, and can be read.
	 */
    private void validateDirectory(File aDirectory) throws FileNotFoundException {
        if (aDirectory == null) {
            throw new IllegalArgumentException("Directory should not be null.");
        }
        if (!aDirectory.exists()) {
            throw new FileNotFoundException("Directory does not exist: " + aDirectory);
        }
        if (!aDirectory.isDirectory()) {
            throw new IllegalArgumentException("Is not a directory: " + aDirectory);
        }
        if (!aDirectory.canRead()) {
            throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
        }
    }

    @Override
    public void createCopy(File sourceFile, File destinnationFile) throws IOException {
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destinnationFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public String getContentOfZipFileEntry(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            InputStream in = zipFile.getInputStream(zipEntry);
            if (in == null) {
                return "";
            }
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            connStreamUtils.closeStreamNoError(reader, "Cannot close reader on zip file entry.");
        }
        return sb.toString();
    }

    public void updateZipEntry(File file, String updateEntryName, String updateEntryContent) throws ZipException, IOException {
        File out = File.createTempFile("zip", "update", new File("temp"));
        ZipOutputStream zos = null;
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            zos = new ZipOutputStream(new FileOutputStream(out));
            ZipEntry entry;
            boolean updated = false;
            InputStream is;
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                if (!updated && entry.getName().equals(updateEntryName)) {
                    zos.putNextEntry(new ZipEntry(updateEntryName));
                    zos.write(updateEntryContent.getBytes());
                    zos.closeEntry();
                    updated = true;
                } else {
                    zos.putNextEntry(entry);
                    is = zipFile.getInputStream(entry);
                    try {
                        byte[] readBuffer = new byte[4096];
                        int bytesIn = 0;
                        while ((bytesIn = is.read(readBuffer)) != -1) {
                            zos.write(readBuffer, 0, bytesIn);
                        }
                        zos.closeEntry();
                    } finally {
                        connStreamUtils.closeStreamNoError(is, "Cannot close input stream.");
                    }
                }
            }
            if (!updated) {
                zos.putNextEntry(new ZipEntry(updateEntryName));
                zos.write(updateEntryContent.getBytes());
                zos.closeEntry();
            }
        } finally {
            connStreamUtils.closeStreamNoError(zos, "Cannot close output stream.");
            zipFile.close();
        }
        file.delete();
        createCopy(out, file);
        if (!out.delete()) {
        }
    }

    public byte[] convertFileToByteArray(File file) throws IOException {
        if (file == null || !file.isFile() || file.length() < 1) {
            throw new IOException("File " + file + " is empty or is not a file.");
        }
        long length = file.length();
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
        } finally {
            connStreamUtils.closeStreamNoError(is, "Cannot close stream");
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }

    public void convertByteArrayToFile(File file, byte[] bytes) throws IOException {
        BufferedOutputStream bos = null;
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bytes);
        } finally {
            connStreamUtils.closeStreamNoError(bos, "");
        }
    }
}
