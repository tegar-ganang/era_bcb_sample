package packet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.File;

public class PacketMemManagedVec implements Serializable {

    private static int globalInstanceCount = 0;

    private static final int elementsInMemory = 128;

    private static String tmpPath;

    private PacketArray readArray;

    private PacketArray readBuffer;

    private PacketArray writeArray;

    private PacketArray writeBuffer;

    private int currentInstanceCount;

    private long totalRemovedSize = 0;

    private long totalAddedSize = 0;

    private long currentWritePosition = 0;

    public static void setPath(String path) {
        tmpPath = path + "arraytmp/";
        if (!(new File(tmpPath)).isDirectory()) {
            boolean success = (new File(tmpPath)).mkdirs();
            if (!success) {
                System.err.println("Directory creation failed: " + tmpPath);
                System.exit(1);
            }
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(globalInstanceCount);
        out.writeObject(tmpPath);
        for (long i = (totalRemovedSize / elementsInMemory) + 2; i <= ((totalAddedSize - 1) / elementsInMemory) - 1; i++) {
            String filename = tmpPath + "arrayInst" + currentInstanceCount + "_tmpNum" + i + ".pma";
            out.writeObject(readArrayFromFile(filename));
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        globalInstanceCount = in.readInt();
        tmpPath = (String) in.readObject();
        for (long i = (totalRemovedSize / elementsInMemory) + 2; i <= ((totalAddedSize - 1) / elementsInMemory) - 1; i++) {
            String filename = tmpPath + "arrayInst" + currentInstanceCount + "_tmpNum" + i + ".pma";
            writeArrayToFile((PacketArray) in.readObject(), filename);
        }
    }

    private PacketArray readArrayFromFile(String filename) {
        PacketArray returnArray = null;
        try {
            FileInputStream in = new FileInputStream(filename);
            ObjectInputStream s = new ObjectInputStream(in);
            returnArray = (PacketArray) s.readObject();
            in.close();
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
            System.err.println("Cannot load file, aborting...");
            System.exit(0);
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            System.err.println("Aborting...");
            System.exit(0);
        } catch (ClassNotFoundException e) {
            System.err.println("ClassNotFoundException: " + e.getMessage());
            System.err.println("Aborting...");
            System.exit(0);
        }
        if (returnArray == null) {
            System.err.println("Null array...this shouldn't happen.");
        }
        return returnArray;
    }

    private void writeArrayToFile(PacketArray packetArray, String filename) {
        try {
            FileOutputStream out = new FileOutputStream(filename);
            ObjectOutputStream s = new ObjectOutputStream(out);
            s.writeObject(packetArray);
            s.close();
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    public PacketMemManagedVec() {
        currentInstanceCount = globalInstanceCount;
        globalInstanceCount++;
        readArray = new PacketArray(elementsInMemory);
        readBuffer = new PacketArray(elementsInMemory);
        writeArray = new PacketArray(elementsInMemory);
        writeBuffer = new PacketArray(elementsInMemory);
    }

    public void add(Packet f) {
        if (currentWritePosition < elementsInMemory) {
            readArray.set((int) currentWritePosition, f);
            currentWritePosition++;
            totalAddedSize++;
        } else if (currentWritePosition < 2 * elementsInMemory) {
            readBuffer.set((int) (currentWritePosition % elementsInMemory), f);
            totalAddedSize++;
            currentWritePosition++;
        } else {
            if ((currentWritePosition % elementsInMemory == 0) && (currentWritePosition != 2 * elementsInMemory)) {
                writeBuffer.clear();
                PacketArray temp = writeBuffer;
                writeBuffer = writeArray;
                writeArray = temp;
                long arrayNum = (totalAddedSize / elementsInMemory) - 1;
                String filename = tmpPath + "arrayInst" + currentInstanceCount + "_tmpNum" + arrayNum + ".pma";
                writeArrayToFile(writeBuffer, filename);
            }
            writeArray.set((int) (currentWritePosition % elementsInMemory), f);
            currentWritePosition++;
            totalAddedSize++;
        }
    }

    public Packet remove() {
        Packet returnPacket = elementAt(0);
        totalRemovedSize++;
        if ((totalRemovedSize % elementsInMemory) == 0) {
            if (totalRemovedSize != 0) {
                readArray.clear();
                PacketArray temp = readArray;
                readArray = readBuffer;
                readBuffer = temp;
                if (currentWritePosition < 2 * elementsInMemory) {
                    ;
                } else if (currentWritePosition <= 3 * elementsInMemory) {
                    readBuffer.clear();
                    temp = readBuffer;
                    readBuffer = writeArray;
                    writeArray = temp;
                } else {
                    long arrayNum = (totalRemovedSize / elementsInMemory) + 1;
                    String filename = tmpPath + "arrayInst" + currentInstanceCount + "_tmpNum" + arrayNum + ".pma";
                    readBuffer = readArrayFromFile(filename);
                    (new File(filename)).delete();
                }
                currentWritePosition -= elementsInMemory;
            }
        }
        if (returnPacket == null) {
        }
        return returnPacket;
    }

    public Packet elementAt(int i) {
        if (i != 0) {
            System.err.println("PacketMemManagedVec.elementAt(int i) only implemented at i = 0.");
            return null;
        } else {
            return readArray.get((int) (totalRemovedSize % elementsInMemory));
        }
    }

    public long size() {
        return (totalAddedSize - totalRemovedSize);
    }

    public boolean isEmpty() {
        return ((totalAddedSize - totalRemovedSize) == 0);
    }

    public void destroy() {
        totalAddedSize = 0;
        totalRemovedSize = 0;
        currentWritePosition = 0;
    }
}
