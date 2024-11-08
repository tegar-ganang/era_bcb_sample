package org.privale.utils.network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.privale.utils.FileManager;

public class FileReadTrans extends LinkedTransaction {

    private ReadComplete RC;

    private boolean Complete;

    private boolean Started;

    private long FileLen;

    private FileManager FM;

    private File CurFile;

    public FileReadTrans(QueueHandler hand, Transaction next, ReadComplete comp, long len, FileManager fm) {
        super(hand, next);
        FM = fm;
        Setup(len);
        setReadComplete(comp);
    }

    public void Setup(long len) {
        FileLen = len;
        Complete = false;
        Started = false;
        if (FileLen <= 0) {
            System.out.println("ERROR: Can not setup FileReadTrans with <= 0 expected length!");
        }
    }

    public void Reset() {
        Complete = false;
        Started = false;
    }

    public void setReadComplete(ReadComplete comp) {
        RC = comp;
    }

    public int getInterestOps() {
        return SelectionKey.OP_READ;
    }

    public void Init() throws IOException {
        if (!Started) {
            CurFile = FM.createNewFile("fileread", "trans");
            System.out.println("Reading file: " + CurFile.getPath() + " for " + this.toString());
            Started = true;
            Complete = false;
        }
    }

    public int Process(SocketChannel chan, ByteBuffer Buffer) throws IOException {
        int len = -1;
        long remain = FileLen - CurFile.length();
        if (remain > 0) {
            FileOutputStream fos = new FileOutputStream(CurFile, true);
            FileChannel foc = fos.getChannel();
            do {
                Buffer.clear();
                if (remain < Buffer.remaining()) {
                    Buffer.limit((int) remain);
                }
                len = chan.read(Buffer);
                Buffer.flip();
                foc.write(Buffer);
                if (len > 0) {
                    remain -= len;
                }
            } while (len > 0 && remain > 0);
            foc.close();
        }
        if (remain <= 0) {
            Complete = true;
            Started = false;
            RC.Complete(this, CurFile);
            Next();
        }
        return len;
    }

    public boolean holdProcessor() {
        return false;
    }

    public boolean isComplete() {
        return Complete;
    }

    public boolean isStarted() {
        return Started;
    }
}
