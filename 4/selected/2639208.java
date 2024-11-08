package backend.tools.persistance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import backend.tools.persistance.RandomAccessFileManager.CachedRandomAccessFile;
import backend.tools.runnerworkers.MultiThreadQueue;

/**
 * Matt's very cool serialization manager
 * @author hindlem
 *
 * @param <E>
 */
public class ObjectSerialisationManager<E> {

    private static RandomAccessFileManager filemanager;

    private final MultiThreadQueue ioqueue;

    public ObjectSerialisationManager(final String directory, final String objectsLabel) {
        filemanager = new RandomAccessFileManager(directory, objectsLabel);
        ioqueue = new MultiThreadQueue(10000, this.getClass().getName());
    }

    private static final ConcurrentHashMap<String, FileObjectPointer> map = new ConcurrentHashMap<String, FileObjectPointer>();

    class WriteIO implements Runnable {

        private final E object;

        WriteIO(final E object) {
            this.object = object;
        }

        public void run() {
            try {
                final CachedRandomAccessFile readRandAccessFile = filemanager.getFileToWrite();
                String refToObject = object.toString();
                final ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
                final ObjectOutputStream obj_out = new ObjectOutputStream(byteOutStream);
                synchronized (object) {
                    obj_out.writeObject(object);
                }
                byte[] byteArr = byteOutStream.toByteArray();
                obj_out.close();
                byteOutStream.close();
                final Integer length = byteArr.length;
                final String path = readRandAccessFile.getFile().getPath();
                final Long filePointer = readRandAccessFile.length();
                readRandAccessFile.seek(filePointer);
                readRandAccessFile.write(byteArr);
                filemanager.returnFile(readRandAccessFile);
                byteArr = null;
                map.put(refToObject, new FileObjectPointer(path, filePointer, length));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public final void storePathway(final E object) {
        ioqueue.addRunnable(new WriteIO(object));
    }

    @SuppressWarnings("unchecked")
    public final synchronized E fetchObject(final String pathwayID) {
        if (pathwayID == null) throw new NullPointerException("pathwayID is null");
        E object = null;
        FileObjectPointer filePointer = map.get(pathwayID);
        if (filePointer == null) throw new NullPointerException("file has not been written for pathway " + pathwayID);
        CachedRandomAccessFile readRandAccessFile = filemanager.getFileToRead(filePointer.getFilename());
        if (readRandAccessFile == null) throw new NullPointerException("Rand access file could not be opened");
        final byte[] newbyteArr = new byte[filePointer.getLengthOfByteArray()];
        try {
            readRandAccessFile.seek(filePointer.getFilePositionPointer());
            readRandAccessFile.readFully(newbyteArr);
            filemanager.returnFile(readRandAccessFile);
            final ObjectInputStream obj_in = new ObjectInputStream(new ByteArrayInputStream(newbyteArr));
            object = (E) obj_in.readObject();
            obj_in.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (object == null) System.err.println("Object not found in pointer " + filePointer.getFilename() + " " + filePointer.getFilePositionPointer());
        return object;
    }

    private static class FileObjectPointer {

        private final String filename;

        private final long filePositionPointer;

        private final int lengthOfByteArray;

        /**
		 * 
		 * @param filename
		 * @param filePositionPointer
		 * @param length of the byte array to read
		 */
        private FileObjectPointer(String filename, long filePositionPointer, int lengthOfByteArray) {
            this.filename = filename;
            this.filePositionPointer = filePositionPointer;
            this.lengthOfByteArray = lengthOfByteArray;
        }

        public String getFilename() {
            return filename;
        }

        public long getFilePositionPointer() {
            return filePositionPointer;
        }

        public int getLengthOfByteArray() {
            return lengthOfByteArray;
        }
    }

    public void waitToFinish(String caller) {
        ioqueue.waitToFinish(caller);
    }
}
