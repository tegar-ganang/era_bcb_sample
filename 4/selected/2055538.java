package org.pachyderm.apollo.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSPathUtilities;

public class CXLocalVolume extends CXVolume {

    private String _basePath;

    public CXLocalVolume(String path) {
        super(path);
        if (path == null || path.length() == 0) {
            _basePath = "/";
        } else {
            _basePath = path;
        }
    }

    public boolean createDirectoryAtPath(String path, NSDictionary attribtutes) {
        File file = _fileForPath(path);
        if (file == null) {
            return false;
        }
        return file.mkdirs();
    }

    @SuppressWarnings("deprecation")
    public boolean copyPathToPath(String source, String destination, Object handler) {
        boolean didCopy = false;
        if ((source == null) || (destination == null)) {
            return false;
        }
        if (NSPathUtilities.fileExistsAtPath(_fullPathForPath(source))) {
            File sourceFile = _fileForPath(source);
            if (sourceFile.isDirectory()) {
                if (!NSPathUtilities.fileExistsAtPath(_fullPathForPath(destination))) {
                    createDirectoryAtPath(destination, null);
                }
                String[] dirContents = sourceFile.list();
                for (int i = 0; i < dirContents.length; i++) {
                    String childSource = dirContents[i];
                    String childSourcePath = source + "/" + childSource;
                    String childDestinationPath = destination + childSource;
                    if (fileIsDirectoryAtPath(_fullPathForPath(childSourcePath))) {
                        childDestinationPath += "/";
                    }
                    didCopy = copyPathToPath(childSourcePath, childDestinationPath, handler);
                }
            } else {
                File destFile = _fileForPath(destination);
                if (destFile.isDirectory()) {
                    if (!(destFile.exists())) {
                        destFile.mkdirs();
                    }
                    didCopy = _copyPath(source, destination, handler);
                } else {
                    if (NSPathUtilities.fileExistsAtPath(_fullPathForPath(destination))) {
                        File oldDest = _fileForPath(destination + ".old");
                        destFile.renameTo(oldDest);
                        didCopy = _copyPath(source, destination, handler);
                        if (didCopy) {
                            if (fileExistsAtPath(destination + ".old")) {
                                File deadFile = _fileForPath(destination + ".old");
                                deadFile.delete();
                            }
                        } else {
                            File undoFile = _fileForPath(destination);
                            oldDest.renameTo(undoFile);
                        }
                    } else {
                        didCopy = _copyPath(source, destination, handler);
                    }
                }
            }
        }
        return didCopy;
    }

    private boolean _copyPath(String source, String destination, Object handler) {
        try {
            FileInputStream fis = new FileInputStream(_fullPathForPath(source));
            FileOutputStream fos = new FileOutputStream(_fullPathForPath(destination));
            byte[] buffer = new byte[fis.available()];
            int read;
            for (read = fis.read(buffer); read >= 0; read = fis.read(buffer)) {
                fos.write(buffer, 0, read);
            }
            fis.close();
            fos.close();
            return true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
    }

    public boolean movePathToPath(String sourcePath, String destinationPath, Object handler) {
        File source = _fileForPath(sourcePath);
        File destination = _fileForPath(destinationPath);
        if (source == null || destination == null) {
            return false;
        }
        return source.renameTo(destination);
    }

    public boolean createFileAtPath(String path, NSData contents, NSDictionary attributes) {
        File file = _fileForPath(path);
        if (file == null || contents == null) {
            return false;
        }
        try {
            FileOutputStream fos = new FileOutputStream(file, false);
            contents.writeToStream(fos);
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeFileAtPath(String path, Object handler) {
        File file = _fileForPath(path);
        if (file == null) {
            return false;
        }
        return file.delete();
    }

    public NSData contentsAtPath(String path) {
        URL url = _urlForPath(path);
        NSData data;
        try {
            data = new NSData(url);
        } catch (Exception e) {
            data = null;
        }
        return data;
    }

    public boolean fileExistsAtPath(String path) {
        URL url = _urlForPath(path);
        return NSPathUtilities.fileExistsAtPathURL(url);
    }

    public boolean fileIsDirectoryAtPath(String path) {
        File file = _fileForPath(path);
        if (file != null) {
            return file.isDirectory();
        }
        return false;
    }

    public boolean isReadableFileAtPath(String path) {
        File file = _fileForPath(path);
        if (file != null) {
            return file.canRead();
        }
        return false;
    }

    public boolean isWritableFileAtPath(String path) {
        File file = _fileForPath(path);
        if (file != null) {
            return file.canWrite();
        }
        return false;
    }

    public boolean isDeletableFileAtPath(String path) {
        return false;
    }

    public NSDictionary getFileAttributesAtPath(String path) {
        return NSDictionary.EmptyDictionary;
    }

    public boolean modFileAttributesAtPath(NSDictionary attributes, String path) {
        return false;
    }

    public NSArray directoryContentsAtPath(String path) {
        File file = _fileForPath(path);
        if (file != null) {
            try {
                String[] items = file.list();
                return new NSArray(items);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String _fullPathForPath(String path) {
        NSArray comps = CXFileManager.pathComponents(path);
        return CXFileManager.pathByAppendingPathComponents(_basePath, comps);
    }

    private URL _urlForPath(String path) {
        path = _fullPathForPath(path);
        URL url;
        try {
            url = new URL("file://" + path);
        } catch (Exception e) {
            url = null;
        }
        return url;
    }

    private File _fileForPath(String path) {
        path = _fullPathForPath(path);
        try {
            return new File(path);
        } catch (Exception e) {
            return null;
        }
    }
}
