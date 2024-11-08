package net.sf.orcc.interpreter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import net.sf.orcc.ir.ICommunicationFifo;

/**
 * A FIFO of object arrays for exchanging data between actors.
 * 
 * @author Pierre-Laurent Lagalaye
 * 
 */
public class CommunicationFifo implements ICommunicationFifo {

    private FileOutputStream fos;

    private OutputStreamWriter out;

    private Object[] queue;

    private int readPos;

    private int size;

    private int writePos;

    public CommunicationFifo(int size, boolean enableTraces, String fileName, String fifoName) throws Exception {
        this.size = size;
        queue = new Object[size];
        if (enableTraces) {
            File file = new File(fileName).getParentFile();
            try {
                fos = new FileOutputStream(new File(file, fifoName + "_traces.txt"));
                this.out = new OutputStreamWriter(fos, "UTF-8");
            } catch (FileNotFoundException e) {
                String msg = "file not found: \"" + fileName + "\"";
                throw new RuntimeException(msg, e);
            }
        } else {
            fos = null;
            out = null;
        }
    }

    public void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void get(Object[] target) {
        if (readPos + target.length <= size) {
            System.arraycopy(queue, readPos, target, 0, target.length);
            readPos += target.length;
            if (readPos == size) {
                readPos = 0;
            }
        } else {
            System.arraycopy(queue, readPos, target, 0, size - readPos);
            System.arraycopy(queue, 0, target, size - readPos, target.length + readPos - size);
            readPos = target.length + readPos - size;
        }
    }

    public boolean hasRoom(int n) {
        if (readPos > writePos) {
            return (readPos - writePos) > n;
        }
        return (size - writePos + readPos) > n;
    }

    public boolean hasTokens(int n) {
        if (writePos >= readPos) {
            return (writePos - readPos) >= n;
        } else {
            return (size - readPos + writePos) >= n;
        }
    }

    public void peek(Object[] target) {
        if (readPos + target.length <= size) {
            System.arraycopy(queue, readPos, target, 0, target.length);
        } else {
            System.arraycopy(queue, readPos, target, 0, size - readPos);
            System.arraycopy(queue, 0, target, size - readPos, target.length + readPos - size);
        }
    }

    public void put(Object[] source) {
        if (writePos + source.length <= size) {
            System.arraycopy(source, 0, queue, writePos, source.length);
            writePos += source.length;
            if (writePos == size) {
                writePos = 0;
            }
        } else {
            System.arraycopy(source, 0, queue, writePos, size - writePos);
            System.arraycopy(source, size - writePos, queue, 0, source.length + writePos - size);
            writePos = source.length + writePos - size;
        }
        if (out != null) {
            try {
                if (source[0] instanceof Boolean) {
                    for (int i = 0; i < source.length; i++) {
                        if ((Boolean) source[i]) {
                            out.write("1\n");
                        } else {
                            out.write("0\n");
                        }
                    }
                } else {
                    for (int i = 0; i < source.length; i++) {
                        out.write(source[i] + "\n");
                    }
                }
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
