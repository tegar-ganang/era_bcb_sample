package org.privale.utils.network.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Date;
import org.privale.utils.network.ChannelProcessor;
import org.privale.utils.network.ProcessorPool;

public class SimpleProcessor extends ChannelProcessor {

    private SimpleProcFactory Fac;

    private int BUFSIZE = 4096;

    private ByteBuffer Buffer;

    private long Size;

    private long CurSize;

    private File HoldFile;

    private FileChannel Chan;

    private boolean initstate = false;

    private boolean readstate = false;

    private boolean writestate = false;

    private boolean done = false;

    public SimpleProcessor(ProcessorPool p, SimpleProcFactory fac) {
        super(p);
        Fac = fac;
        Buffer = ByteBuffer.allocateDirect(BUFSIZE);
        initstate = false;
    }

    public void Init() {
        Buffer.clear();
        writestate = false;
        initstate = false;
        SimpleSocketChannelHandler hand = (SimpleSocketChannelHandler) getSocketChannelHandler();
        if (hand.State != null) {
            writestate = true;
            SimpleState s = hand.State;
            HoldFile = s.F;
            try {
                FileInputStream fis = new FileInputStream(HoldFile);
                Chan = fis.getChannel();
                Chan.read(Buffer);
                Buffer.flip();
            } catch (IOException e) {
                done = true;
                writestate = false;
                getSocketChannelHandler().Cancel();
                e.printStackTrace();
            }
        } else {
            Buffer.limit(Long.SIZE / Byte.SIZE);
            initstate = true;
        }
        readstate = false;
        done = false;
    }

    public void Process() {
        SocketChannel chan = getSocketChannelHandler().getSocketChannel();
        try {
            if (initstate) {
                System.out.println(" -- initstate");
                int read = chan.read(Buffer);
                if (read < 0) {
                    getSocketChannelHandler().Cancel();
                    Chan.close();
                    done = true;
                } else if (!Buffer.hasRemaining()) {
                    initstate = false;
                    Buffer.flip();
                    Size = Buffer.getLong();
                    System.out.println("SIZE READ: " + Size);
                    CurSize = 0;
                    readstate = true;
                    HoldFile = Fac.FM.createNewFile("tmp", "tmp");
                    FileOutputStream fos = new FileOutputStream(HoldFile);
                    Chan = fos.getChannel();
                }
            }
            if (readstate) {
                System.out.println(" -- readstate");
                Buffer.clear();
                int read = chan.read(Buffer);
                if (read < 0) {
                    getSocketChannelHandler().Cancel();
                    Chan.close();
                    done = true;
                } else {
                    System.out.println(" -- READ DONE!!");
                    CurSize += read;
                    System.out.println("CurSize! " + CurSize);
                    Buffer.flip();
                    Chan.write(Buffer);
                    SimpleSocketChannelHandler simp = (SimpleSocketChannelHandler) getSocketChannelHandler();
                    if (CurSize >= Size) {
                        Chan.close();
                        System.out.println("FINISHED READING FILE!! " + HoldFile.getPath());
                        readstate = false;
                        done = true;
                        SimpleState s = new SimpleState();
                        s.D = new Date();
                        s.F = HoldFile;
                        simp.State = s;
                        simp.Fac.AddToWaitList(simp);
                        simp.clearInterestOps();
                    } else {
                        simp.setInterestOps(SelectionKey.OP_READ);
                    }
                }
            }
            if (writestate) {
                System.out.println(" -- writing! --");
                SimpleSocketChannelHandler simp = (SimpleSocketChannelHandler) getSocketChannelHandler();
                if (!Buffer.hasRemaining()) {
                    Buffer.clear();
                    int len = Chan.read(Buffer);
                    if (len < 0) {
                        done = true;
                        simp.clearInterestOps();
                        Chan.close();
                    }
                    Buffer.flip();
                }
                if (!done) {
                    chan.write(Buffer);
                    simp.setInterestOps(SelectionKey.OP_WRITE);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            getSocketChannelHandler().Cancel();
            done = true;
        }
    }

    public boolean isComplete() {
        return done;
    }
}
