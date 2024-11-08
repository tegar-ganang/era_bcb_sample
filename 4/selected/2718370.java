package org.fao.waicent.kids.giews.communication.providermodule.uploadmodule;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.List;
import org.fao.waicent.kids.giews.communication.utility.DownloadInfo;
import org.fao.waicent.kids.giews.communication.utility.MyDebug;
import org.fao.waicent.kids.giews.communication.utility.message.Message;

/**
 * <p>Title: UploadThread</p>
 *
 *
 * @author A. Tamburo
 * @version 1
 * @since 1
*/
public class UploadThread extends Thread {

    private List pool;

    private int id;

    private DownloadInfo di;

    private byte state;

    private int bandwidth;

    private PooledUpload poolUp;

    private MyDebug debug;

    private int TIMEOUT = 60 * 1000;

    private boolean running;

    private DataInputStream inFromClient = null;

    private DataOutputStream outToClient = null;

    /**
       * UploadThread cotruttore
       *
       * @version 1, last modified by A. Tamburo, 19/01/06
       */
    public UploadThread(List pool, int id, PooledUpload poolUp, MyDebug debug) {
        super("UploadThread");
        this.pool = pool;
        this.poolUp = poolUp;
        this.id = id;
        this.bandwidth = poolUp.getBandwith();
        this.debug = debug;
        this.running = true;
    }

    /**
       * run 
       *
       * @version 1, last modified by A. Tamburo, 19/01/06
       */
    public void run() {
        File file;
        FileInputStream fileStream = null;
        byte[] buffer = new byte[this.bandwidth];
        while (this.running) {
            this.state = PooledUpload.WAITING;
            synchronized (pool) {
                while (pool.isEmpty()) {
                    try {
                        if (this.running == false) {
                            break;
                        }
                        pool.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (pool.size() <= 0) {
                    continue;
                }
                di = (DownloadInfo) pool.remove(0);
            }
            this.state = PooledUpload.RUNNABLE;
            Date now = new Date(System.currentTimeMillis());
            String str = "";
            str += "ID=" + di.getIDString();
            str += "\nIDResource=" + di.getResource().getIDString();
            str += "\nName=" + di.getResource().getName();
            str += "\nSize=" + di.getResource().getSize();
            str += "\nBytesResidues=" + di.getByteResidue();
            str += "\nSource=" + di.getAddrSource().getCanonicalHostName() + ":" + di.getTCPPortSource();
            debug.println("Upload: start upload {\n " + str + "}");
            try {
                inFromClient = di.getInputStream();
                outToClient = di.getOutputStream();
                file = new File(di.getResource().getPath());
                if (!file.exists()) {
                    debug.println("Upload: upload " + di.getIDString() + " aborted, file not found " + file.getPath());
                    outToClient.writeByte(Message.FALSE);
                    outToClient.flush();
                    this.closeStream();
                    continue;
                }
                outToClient.writeByte(Message.TRUE);
                outToClient.flush();
                fileStream = new FileInputStream(file);
                int offset = this.di.getResource().getSize() - this.di.getByteResidue();
                if (offset > 0) {
                    fileStream.getChannel().position(offset);
                }
                boolean readAgain = true;
                int byteToRead, byteRead, totalByteRead = 0, calcSpeed = 0;
                long startTime, sTimeCalcSpeed, endTime, diffTime;
                float bitRate = 0;
                sTimeCalcSpeed = System.currentTimeMillis();
                while (readAgain) {
                    startTime = System.currentTimeMillis();
                    byteToRead = this.poolUp.getSpeed(this.id);
                    if (byteToRead == 0) {
                        byteToRead = 1;
                    }
                    byteRead = fileStream.read(buffer, 0, byteToRead);
                    totalByteRead += byteRead;
                    if (byteRead < 0) {
                        outToClient.writeInt(byteRead);
                        readAgain = false;
                        fileStream.close();
                        continue;
                    }
                    outToClient.writeInt(byteRead);
                    outToClient.flush();
                    outToClient.write(buffer, 0, byteRead);
                    outToClient.flush();
                    int b = inFromClient.readInt();
                    this.di.decrementByteResidue(byteRead);
                    endTime = System.currentTimeMillis();
                    diffTime = endTime - startTime;
                    try {
                        if (b == 1) {
                            this.poolUp.incrementSpeed(this.id);
                        } else if (b > 1) {
                            this.poolUp.decrementSpeed(this.id, this.poolUp.getSpeed(id) - b);
                        }
                        if (diffTime < 1000) {
                            Thread.sleep(1000 - diffTime);
                        }
                    } catch (InterruptedException e) {
                        throw new java.io.IOException("");
                    }
                    calcSpeed++;
                    if (calcSpeed == 5) {
                        endTime = System.currentTimeMillis();
                        bitRate = ((totalByteRead) / (float) (endTime - sTimeCalcSpeed)) * 1000;
                        this.di.setSpeed((8 * bitRate) / 1024);
                        sTimeCalcSpeed = System.currentTimeMillis();
                        totalByteRead = 0;
                        calcSpeed = 0;
                    }
                }
                this.closeStream();
                fileStream.close();
            } catch (java.net.SocketTimeoutException e) {
                debug.println("Upload: upload " + di.getIDString() + " aborted, timeout expires");
                this.closeStream();
                continue;
            } catch (java.io.IOException e) {
                debug.println("Upload: upload " + di.getIDString() + " aborted," + e.getMessage());
                this.closeStream();
                continue;
            }
            now = new Date(System.currentTimeMillis());
            debug.println("Upload: end upload " + di.getIDString());
        }
    }

    /**
       * stopThread
       *
       * @version 1, last modified by A. Tamburo, 30/01/06
       */
    public void stopThread() {
        this.closeStream();
    }

    /**
       * clearThread
       *
       * @version 1, last modified by A. Tamburo, 11/09/06
       */
    public void clearThread() {
        this.running = false;
    }

    public void closeStream() {
        if (inFromClient != null) {
            try {
                inFromClient.close();
            } catch (java.io.IOException e) {
            }
        }
        if (outToClient != null) {
            try {
                outToClient.close();
            } catch (java.io.IOException e) {
            }
        }
    }

    /**
       * getUploadInfo
       *
       * @version 1, last modified by A. Tamburo, 19/01/06
       */
    public DownloadInfo getUploadInfo() {
        return this.di;
    }

    /**
       * getStateThread
       *
       * @version 1, last modified by A. Tamburo, 19/01/06
       */
    public byte getStateThread() {
        return state;
    }

    /**
       * matchDownload
       *
       * @version 1, last modified by A. Tamburo, 19/01/06
       */
    public boolean matchDownload(byte[] id) {
        return DownloadInfo.matching(this.di.getID(), id);
    }
}
