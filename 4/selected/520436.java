package org.privale.utils.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class FileSendTrans extends LinkedTransaction {

    private File Data;

    private boolean Complete;

    private boolean Started;

    private long CurPos;

    private boolean DeleteOnComplete;

    public FileSendTrans(QueueHandler hand, File data, Transaction next) {
        super(hand, next);
        Setup(data);
        setDeleteOnComplete(true);
    }

    public void Setup(File data) {
        Data = data;
        Complete = false;
        Started = false;
    }

    public void Reset() {
        Complete = false;
        Started = false;
    }

    public int getInterestOps() {
        return SelectionKey.OP_WRITE;
    }

    public void Init() throws IOException {
        if (!Started) {
            Started = true;
            Complete = false;
            CurPos = 0;
        }
    }

    public int Process(SocketChannel chan, ByteBuffer Buffer) throws IOException {
        System.out.println(" Processing " + this.toString() + " Processing!");
        int len = -1;
        FileInputStream fis = new FileInputStream(Data);
        FileChannel fic = fis.getChannel();
        fic.position(CurPos);
        Buffer.clear();
        int filelen = fic.read(Buffer);
        Buffer.flip();
        do {
            if (!Buffer.hasRemaining()) {
                Buffer.clear();
                filelen = fic.read(Buffer);
                Buffer.flip();
            }
            len = chan.write(Buffer);
        } while (len > 0 && filelen > 0);
        CurPos = fic.position() - Buffer.remaining();
        fic.close();
        if (CurPos == Data.length()) {
            System.out.println("FILE SENT " + this.toString());
            if (this.isDeleteOnComplete()) {
                Data.delete();
            }
            Complete = true;
            Started = false;
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

    public void setDeleteOnComplete(boolean deleteOnComplete) {
        DeleteOnComplete = deleteOnComplete;
    }

    public boolean isDeleteOnComplete() {
        return DeleteOnComplete;
    }
}
