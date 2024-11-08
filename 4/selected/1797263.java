package framework.IOStream;

import framework.log.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

public class FileCache {

    private HashMap<String, byte[]> files = new HashMap<String, byte[]>();

    private boolean caching;

    public FileCache() {
        this(true);
    }

    public FileCache(boolean cacheFiles) {
        setCaching(cacheFiles);
    }

    private byte[] addBinaryFile(String fileName) {
        if (fileName == null) throw new IllegalArgumentException("null fileName");
        return addBinaryFile(new File(fileName));
    }

    @SuppressWarnings("empty-statement")
    private byte[] addBinaryFile(File file) {
        if (file == null) throw new IllegalArgumentException("null fileName");
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException exception) {
            Log.out("File not found - '" + file.getAbsolutePath() + "'");
            return null;
        }
        FileChannel fileChannel = fileInputStream.getChannel();
        byte[] buffer = null;
        try {
            buffer = new byte[(int) fileChannel.size()];
        } catch (IOException exception) {
            Log.out("IOException reading file '" + file.getAbsolutePath() + "' - " + exception.getMessage());
            return null;
        }
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            while (fileChannel.read(byteBuffer) > 0) ;
            fileChannel.close();
        } catch (IOException exception) {
            Log.out("IOException reading file '" + file.getAbsolutePath() + "' - " + exception.getMessage());
            return null;
        }
        if (caching) files.put(file.getAbsolutePath(), buffer);
        Log.out("Loaded file '" + file.getAbsolutePath() + "'");
        return buffer;
    }

    public byte[] getBinaryFile(String fileName) {
        if (fileName == null) throw new IllegalArgumentException("null fileName");
        byte[] buffer = files.get(fileName);
        if (buffer == null) buffer = addBinaryFile(fileName);
        return buffer;
    }

    public byte[] getBinaryFile(File file) {
        if (file == null) throw new IllegalArgumentException("null file");
        byte[] buffer = files.get(file.getAbsoluteFile());
        if (buffer == null) buffer = addBinaryFile(file);
        return buffer;
    }

    public byte[] getBinaryResource(String resourceName) {
        byte[] buffer = files.get(resourceName);
        if (buffer != null) return buffer;
        InputStream resource = getClass().getResourceAsStream(resourceName);
        if (resource == null) return null;
        return getBinaryResource(resourceName, resource);
    }

    public byte[] getBinaryResource(String resourceName, InputStream resource) {
        if (resourceName == null) throw new IllegalArgumentException("null resourceName");
        if (resource == null) throw new IllegalArgumentException("null resource");
        byte[] cached = files.get(resourceName);
        if (cached != null) return cached;
        byte[] toReturn;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int read = 0;
            byte[] buffer = new byte[resource.available()];
            while ((resource.available() > 0) && (read > -1)) {
                if (resource.available() > buffer.length) buffer = new byte[resource.available()];
                read = resource.read(buffer);
                out.write(buffer, 0, read);
            }
            toReturn = out.toByteArray();
        } catch (IOException exception) {
            Log.out("IOException reading resource '" + resource + "' - " + exception.getMessage());
            return null;
        }
        if (caching) files.put(resourceName, toReturn);
        return toReturn;
    }

    public String getTextFile(String fileName) {
        if (fileName == null) throw new IllegalArgumentException("null fileName");
        byte[] buffer = files.get(fileName);
        if (buffer == null) buffer = addBinaryFile(fileName);
        if (buffer == null) return null;
        return new String(buffer);
    }

    public String getTextFile(File textFile) {
        if (textFile == null) throw new IllegalArgumentException("null textFile");
        byte[] buffer = files.get(textFile.getAbsolutePath());
        if (buffer == null) buffer = addBinaryFile(textFile);
        if (buffer == null) return null;
        return new String(buffer);
    }

    public String getTextResource(String resourceName) {
        return getTextResource(resourceName, null);
    }

    public String getTextResource(String resourceName, InputStream resource) {
        if (resourceName == null) throw new IllegalArgumentException("null resourceName");
        byte[] buffer = files.get(resourceName);
        if (buffer == null) if (resource != null) buffer = getBinaryResource(resourceName, resource); else buffer = getBinaryResource(resourceName);
        if (buffer == null) return null;
        return new String(buffer);
    }

    public void setCaching(Boolean caching) {
        this.caching = caching;
        if (!caching) files.clear();
    }
}
