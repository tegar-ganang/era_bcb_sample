package de.cabanis.unific.library.media;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import de.cabanis.unific.library.media.meta.Song;

/**
 * @author Nicolas Cabanis
 */
public abstract class MediaFile {

    private Song metaData;

    private File sourceFile;

    private long fileSize = -1;

    protected MediaFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public static MediaFile create(File sourceFile) {
        return new MP3MediaFile(sourceFile);
    }

    public abstract String mediaName();

    public static boolean isMediaFile(File underlyingFile) {
        return underlyingFile.getPath().endsWith(".mp3");
    }

    public Song getMetaData() {
        return this.metaData;
    }

    public void setMetaData(Song metaData) {
        this.metaData = metaData;
    }

    public String getName() {
        return sourceFile.getName();
    }

    public String getNameWithoutExtension() {
        return sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf("."));
    }

    public String getExtension() {
        throw new RuntimeException("Method implementation still untested.");
    }

    public String getPath() {
        return sourceFile.getPath();
    }

    public FileReader getFileReader() throws FileNotFoundException {
        return new FileReader(sourceFile);
    }

    public FileWriter getFileWriter() throws IOException {
        return new FileWriter(sourceFile);
    }

    public FileInputStream getFileInputStream() throws FileNotFoundException {
        return new FileInputStream(sourceFile);
    }

    /**
     * Attention: this is a very time consuming method, because
     * the file must be opened to rethrieve the size, but after
     * the first call, the value is cached.
     * @return file size in byte
     */
    public long getSize() throws IOException {
        if (fileSize == -1) {
            fileSize = getFileInputStream().getChannel().size();
        }
        return fileSize;
    }

    public boolean equals(Object obj) {
        if (obj instanceof MediaFile) {
            return ((MediaFile) obj).sourceFile.equals(this.sourceFile);
        }
        return false;
    }

    public int hashCode() {
        return this.sourceFile.hashCode();
    }
}
