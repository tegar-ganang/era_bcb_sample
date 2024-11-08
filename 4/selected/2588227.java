package com.ohua.checkpoint.framework.restart;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ohua.checkpoint.framework.operatorcheckpoints.AbstractCheckPoint;
import com.ohua.engine.exceptions.Assertion;
import com.ohua.engine.flowgraph.elements.operator.OperatorID;
import com.ohua.engine.utils.FileUtils;

public class StorageRoutines {

    public static String OPERATORS_ROOT_DIRECTORY = "test-output/operators";

    public static String OPERATOR_CHECKPOINT_DIRECTORY = "checkpoints";

    public static Logger _storageRoutinesLogger = Logger.getLogger("com.ohua.checkpoint.framework.restart.StorageRoutines");

    public static ByteBuffer serializeObjectIntoByteBuffer(Object toBeSerialized) {
        OptimizedByteArrayOutputStream byteArrayOutputStream = new OptimizedByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);
            out.writeObject(toBeSerialized);
            out.flush();
            out.close();
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            ByteBuffer buf = ByteBuffer.allocateDirect(byteArrayOutputStream.getCount());
            buf.put(byteArray, 0, byteArrayOutputStream.getCount());
            return buf;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("unhandled exception", e);
        }
    }

    public static AbstractCheckPoint deserializeByteBufferIntoObject(ByteBuffer toBeDeserialized) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(toBeDeserialized.array());
        try {
            ObjectInputStream in = new ObjectInputStream(byteArrayInputStream);
            Object obj = in.readObject();
            in.close();
            return (AbstractCheckPoint) obj;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("unhandled exception", e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("unhandled exception", e);
        }
    }

    /**
   * This function is meant for the reading during restart and the replay phase!
   * @param channel
   * @return
   */
    public static MappedByteBuffer createMemoryMappedFile(File file) {
        try {
            FileChannel rwChannel = new RandomAccessFile(file, "rw").getChannel();
            MappedByteBuffer buf = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, rwChannel.size());
            rwChannel.close();
            return buf;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("unhandled exception", e);
        }
    }

    public static MappedByteBuffer memoryMapFileChunk(File file, long filePositionStart, long filePositionEnd) {
        try {
            FileChannel rwChannel = new RandomAccessFile(file, "rw").getChannel();
            MappedByteBuffer buf = rwChannel.map(FileChannel.MapMode.READ_WRITE, filePositionStart, filePositionEnd - filePositionStart);
            rwChannel.close();
            return buf;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("unhandled exception", e);
        }
    }

    public static void appendByteBufferToFile(File file, ByteBuffer bbuf) {
        try {
            bbuf.position(0);
            FileChannel wChannel = new FileOutputStream(file, true).getChannel();
            wChannel.write(bbuf);
            wChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("unhandled exception", e);
        }
    }

    public static File createFile(File parentDirectory, String filename) {
        File file = new File(parentDirectory, filename);
        return file;
    }

    public static File createCheckpointFile(File parentDirectory, int checkPointID) {
        File file = new File(parentDirectory, constructCheckpointFileName(checkPointID));
        return file;
    }

    public static File createSubDirectory(File parentDirectory, OperatorID id) {
        File operatorCPsubdirectory = new File(parentDirectory, constructOperatorChckPtDirectoryName(id));
        boolean directoryCreated = operatorCPsubdirectory.mkdirs();
        if (!directoryCreated) {
            throw new RuntimeException("FAILED CREATING CHECKPOINT DIRECTORY FOR OPERATOR: " + id);
        }
        return operatorCPsubdirectory;
    }

    public static File createSubDirectory(File parentDirectory, String name) {
        File operatorCPsubdirectory = new File(parentDirectory, name);
        boolean directoryCreated = operatorCPsubdirectory.mkdirs();
        if (!directoryCreated) {
            throw new RuntimeException("FAILED CREATING CHECKPOINT DIRECTORY FOR OPERATOR: " + name);
        }
        return operatorCPsubdirectory;
    }

    public static File[] findOperatorSubdirectory(OperatorID id, File checkPtDir) {
        return findFileInDirectory(checkPtDir, constructOperatorChckPtDirectoryName(id));
    }

    public static File[] findCheckpointFile(int checkpointID, File checkPtDir) {
        return findFileInDirectory(checkPtDir, constructCheckpointFileName(checkpointID));
    }

    public static String constructOperatorChckPtDirectoryName(OperatorID id) {
        return "operator_" + id;
    }

    public static File retrieveOperatorsRootDirectory() {
        File checkPtDir = new File(OPERATORS_ROOT_DIRECTORY);
        return checkPtDir;
    }

    public static AbstractCheckPoint loadCheckpoint(File checkPtDir, OperatorID id, int checkPointID) {
        File opDirectory = new File(checkPtDir, constructOperatorChckPtDirectoryName(id));
        File opChckPtDirectory = new File(opDirectory, OPERATOR_CHECKPOINT_DIRECTORY);
        File checkPointToBeLoaded = new File(opChckPtDirectory, constructCheckpointFileName(checkPointID));
        Object checkpoint = loadFileFromDisk(checkPointToBeLoaded);
        return (AbstractCheckPoint) checkpoint;
    }

    public static String constructCheckpointFileName(int checkPointID) {
        return "checkpoint_" + checkPointID;
    }

    public static Object loadFileFromDisk(File checkPointToBeLoaded) {
        Object checkPtObj;
        try {
            _storageRoutinesLogger.log(Level.ALL, "retrieving checkpoint file: " + checkPointToBeLoaded);
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(checkPointToBeLoaded));
            checkPtObj = in.readObject();
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("unhandled exception", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("unhandled exception", e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("unhandled exception", e);
        }
        return checkPtObj;
    }

    /**
   * Returns either the latest checkpoint file according to modification time stamp or null, if
   * there are no checkpoints available for this operator.
   * @param opCheckPtDir
   * @return
   */
    public static File findLatestFileInDirectory(File opCheckPtDir, String filter) {
        File[] checkPoints = FileUtils.loadFiles(opCheckPtDir, filter);
        if (checkPoints.length < 1) {
            return null;
        }
        long newestChckPtDate = checkPoints[0].lastModified();
        File checkPointToBeLoaded = checkPoints[0];
        for (File checkPoint : checkPoints) {
            if (checkPoint.lastModified() > newestChckPtDate) {
                checkPointToBeLoaded = checkPoint;
                newestChckPtDate = checkPoint.lastModified();
            }
        }
        return checkPointToBeLoaded;
    }

    /**
   * This routine will look for a certain file or subdirectory. If it can not find it NO file is
   * created!
   * @param checkPtDir
   * @param id
   * @return
   */
    public static File[] findFileInDirectory(File checkPtDir, String filename) {
        final String file = filename;
        File[] dirContent = checkPtDir.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.getName().equals(file);
            }
        });
        assert dirContent.length < 2;
        if (dirContent.length == 0) {
            _storageRoutinesLogger.log(Level.ALL, "no file found with the name: " + filename);
        }
        return dirContent;
    }

    public static boolean deleteFileFromDirectory(File directory, String fileToDelete) {
        File toDelete = new File(directory, fileToDelete);
        return toDelete.delete();
    }

    public static boolean deleteCheckpoint(File directory, int checkpointID) {
        File toDelete = new File(directory, constructCheckpointFileName(checkpointID));
        if (!toDelete.exists()) {
            return true;
        }
        return toDelete.delete();
    }

    /**
   * Returns a byte array containing the two's-complement representation of the integer.<br>
   * The byte array will be in big-endian byte-order with a fixes length of 4 (the least
   * significant byte is in the 4th element).<br>
   * <br>
   * <b>Example:</b><br>
   * <code>intToByteArray(258)</code> will return { 0, 0, 1, 2 },<br>
   * <code>BigInteger.valueOf(258).toByteArray()</code> returns { 1, 2 }.
   * @param integer The integer to be converted.
   * @return The byte array of length 4.
   */
    public static byte[] intToByteArray(final int integer) {
        int byteNum = (40 - Integer.numberOfLeadingZeros(integer < 0 ? ~integer : integer)) / 8;
        byte[] byteArray = new byte[4];
        for (int n = 0; n < byteNum; n++) {
            byteArray[3 - n] = (byte) (integer >>> n * 8);
        }
        return byteArray;
    }

    public static void showBufferData(ByteBuffer buf, String name) {
        StringBuffer str = new StringBuffer();
        str.append("index: " + 0 + " entry: ");
        buf.rewind();
        _storageRoutinesLogger.log(Level.ALL, "Buffer data for " + name);
        System.out.println("Buffer data for " + name);
        int cnt = 0;
        while (buf.hasRemaining()) {
            str.append(buf.get() + " ");
            cnt++;
            if (cnt % 5 == 0) {
                _storageRoutinesLogger.log(Level.ALL, str.toString());
                System.out.println(str.toString());
                str.delete(0, str.length() - 1);
                str.append("index: " + (cnt + 1) + " entry: ");
            }
        }
    }

    public static MappedByteBuffer allocateNewMemory(int requiredMemory, MappedByteBuffer buffer, File file) {
        int position = buffer.position();
        ByteBuffer buf = ByteBuffer.allocate(requiredMemory);
        StorageRoutines.appendByteBufferToFile(file, buf);
        buffer = StorageRoutines.memoryMapFileChunk(file, position, position + requiredMemory);
        return buffer;
    }

    public static MappedByteBuffer allocateNewMemory(int requiredMemory, File file) {
        ByteBuffer buf = ByteBuffer.allocate(requiredMemory);
        StorageRoutines.appendByteBufferToFile(file, buf);
        MappedByteBuffer buffer = StorageRoutines.memoryMapFileChunk(file, file.length() + 1, file.length() + 1 + requiredMemory);
        return buffer;
    }

    public static File findLatestCheckpointInDirectory(File opChckPtDir) {
        File[] checkPoints = FileUtils.loadFiles(opChckPtDir, "checkpoint_*");
        if (checkPoints.length < 1) {
            return null;
        }
        Pattern p = Pattern.compile("(checkpoint_)(\\d+\\b)");
        File latestCP = null;
        int latestCPIndex = -1;
        for (File operatorCheckpoint : checkPoints) {
            Matcher matcher = p.matcher(operatorCheckpoint.getName());
            Assertion.invariant(matcher.find());
            String cpIndex = matcher.group(2);
            int cpIndexInt = Integer.parseInt(cpIndex);
            if (cpIndexInt > latestCPIndex) {
                latestCPIndex = cpIndexInt;
                latestCP = operatorCheckpoint;
            }
        }
        return latestCP;
    }
}
