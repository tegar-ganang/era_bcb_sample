package org.zodiak.document;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * This class provides an interface to an OpenDocument file, which is a zip
 * archive containing files which store segments of the document such as
 * content, style, etc.
 *
 * @author Steven R. Farley
 */
public class OpenDocumentFile {

    public static final String CONTENT = "content.xml";

    public static final String STYLES = "styles.xml";

    public static final String META = "meta.xml";

    public static final String SETTINGS = "settings.xml";

    public static final String MIMETYPE = "mimetype";

    private File docFile;

    private String mimeType;

    private Map<String, File> tempFiles;

    private static final List<String> ALL_FILE_NAMES = Arrays.asList(CONTENT, STYLES, META, SETTINGS, MIMETYPE);

    public OpenDocumentFile(File docFile) throws IOException {
        validate(docFile);
        this.docFile = docFile;
        this.tempFiles = new HashMap<String, File>();
        this.mimeType = readFile(MIMETYPE);
    }

    public String getMimeType() {
        return mimeType;
    }

    public OpenDocumentContent getContent() {
        return new OpenDocumentContent(getValidatedFile(CONTENT));
    }

    /**
   * Overwrites the OpenDocument file with modifications.
   * 
   * @throws IOException
   */
    public void save() throws IOException {
        save(null);
    }

    /**
   * Writes the OpenDocument file, with modifications, to another file.
   *  
   * @param saveFile the newly saved OpenDocument file
   * @throws IOException
   */
    public void save(File saveFile) throws IOException {
        boolean replace = false;
        if (saveFile == null) {
            replace = true;
            saveFile = File.createTempFile("zodiak-", ".zip");
        }
        ZipInputStream zIn = new ZipInputStream(new FileInputStream(docFile));
        ZipOutputStream zOut = new ZipOutputStream(new FileOutputStream(saveFile));
        ZipEntry entry = null;
        while ((entry = zIn.getNextEntry()) != null) {
            String name = entry.getName();
            zOut.putNextEntry(entry);
            if (tempFiles.containsKey(name)) {
                FileInputStream tmpIn = new FileInputStream(tempFiles.get(name));
                copy(tmpIn, zOut);
                tmpIn.close();
            } else {
                copy(zIn, zOut);
            }
            zIn.closeEntry();
            zOut.closeEntry();
        }
        zIn.close();
        zOut.close();
        if (replace) {
            saveFile.renameTo(docFile);
        }
        close();
    }

    public void close() {
        for (File file : tempFiles.values()) {
            file.delete();
        }
        tempFiles.clear();
    }

    private static void validate(File docFile) throws IOException {
        boolean valid = true;
        ZipFile file = new ZipFile(docFile);
        for (String name : ALL_FILE_NAMES) {
            if (file.getEntry(name) == null) {
                valid = false;
                break;
            }
        }
        if (!valid) {
            throw new IOException("'" + docFile + "' is not a valid OpenDocument file.");
        }
    }

    private File getValidatedFile(String name) {
        try {
            return getFile(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getFile(String name) throws IOException {
        if (ALL_FILE_NAMES.contains(name) && !tempFiles.containsKey(name)) {
            executeZipEntryAction(name, new ZipEntryAction() {

                public void execute(ZipFile file, ZipEntry entry) throws IOException {
                    OutputStream fileOut = null;
                    InputStream entryIn = null;
                    try {
                        String entryName = entry.getName();
                        entryIn = new BufferedInputStream(file.getInputStream(entry));
                        File tempFile = File.createTempFile("zodiak-", "-" + entryName);
                        fileOut = new BufferedOutputStream(new FileOutputStream(tempFile));
                        copy(entryIn, fileOut);
                        tempFiles.put(entryName, tempFile);
                    } finally {
                        if (fileOut != null) fileOut.close();
                        if (entryIn != null) entryIn.close();
                    }
                }
            });
        }
        return tempFiles.get(name);
    }

    private String readFile(String name) throws IOException {
        final StringWriter stringOut = new StringWriter();
        executeZipEntryAction(name, new ZipEntryAction() {

            public void execute(ZipFile file, ZipEntry entry) throws IOException {
                InputStream entryIn = null;
                try {
                    entryIn = new BufferedInputStream(file.getInputStream(entry));
                    int c;
                    while ((c = entryIn.read()) != -1) stringOut.write(c);
                } finally {
                    stringOut.close();
                    if (entryIn != null) entryIn.close();
                }
            }
        });
        return stringOut.toString();
    }

    private static void copy(InputStream inStream, OutputStream outStream) throws IOException {
        int b;
        while ((b = inStream.read()) != -1) outStream.write(b);
    }

    private void executeZipEntryAction(String entryName, ZipEntryAction action) throws IOException {
        ZipFile file = new ZipFile(docFile);
        ZipEntry entry = file.getEntry(entryName);
        try {
            if (entry != null) {
                action.execute(file, entry);
            } else {
                throw new IOException("File not found in Zip archive.");
            }
        } finally {
            file.close();
        }
    }

    private static interface ZipEntryAction {

        public void execute(ZipFile file, ZipEntry entry) throws IOException;
    }
}
