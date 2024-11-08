package de.jtdev.jfilenotify.polling;

import java.io.File;

/**
 * A little helper class that is used to store informations about a file for 
 * later comparison.
 */
public class FileStats {

    public File file;

    public long lastModified;

    public boolean isDirectory;

    public boolean readable;

    public boolean writeable;

    public boolean hidden;

    public String toString() {
        return file.getPath() + "  lastModified: " + lastModified + "  isDirectory: " + isDirectory + "  readable: " + readable + "  writeable: " + writeable + "  hidden: " + hidden;
    }
}
