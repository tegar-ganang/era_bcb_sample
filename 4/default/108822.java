import java.util.*;
import java.io.*;
import drcl.data.*;
import drcl.comp.*;
import drcl.net.*;
import drcl.inet.*;
import drcl.inet.socket.*;

/**
 * This component implements a uni-directional file transfer protocol.
 *
 * When transmitting a file, it first sends the size of the file in a
 * 8-byte long integer and then follows the content of the file in 
 * continuous byte stream.
 * 
 * To start a transmission, use {@link #setup setup} to set up the file to be
 * transmitted and the segment size, and then {@link #run run} it.
 */
public class ftp2 extends drcl.inet.socket.SocketApplication implements ActiveComponent {

    int remotePort;

    long localAddress, remoteAddress;

    String remoteFile;

    File file;

    int bufferSize;

    long progress;

    public ftp2() {
        super();
    }

    public ftp2(String id_) {
        super(id_);
    }

    public void setup(String infile_, int bufferSize_, String remoteFile_) throws IOException {
        bufferSize = bufferSize_;
        file = new File(infile_);
        remoteFile = remoteFile_;
    }

    public void reset() {
        progress = 0;
    }

    public void duplicate(Object source_) {
        super.duplicate(source_);
        bufferSize = ((ftp2) source_).bufferSize;
    }

    public void bind(long localAddr_, long remoteAddr_, int remotePort_) {
        localAddress = localAddr_;
        remoteAddress = remoteAddr_;
        remotePort = remotePort_;
    }

    protected void _start() {
        if (file == null) {
            error("start()", "no file is set up to send");
            return;
        }
        progress = 0;
        long fileSize_ = file.length();
        try {
            InetSocket socket_ = socketMaster.newSocket();
            socketMaster.bind(socket_, localAddress, 0);
            socketMaster.connect(socket_, remoteAddress, remotePort);
            FileInputStream in_ = new FileInputStream(file);
            DataOutputStream out_ = new DataOutputStream(socket_.getOutputStream());
            if (isDebugEnabled()) debug("Start transmitting a file of size " + fileSize_);
            byte[] remoteFileName_ = remoteFile.getBytes();
            out_.writeInt(remoteFileName_.length);
            out_.write(remoteFileName_);
            out_.writeLong(fileSize_);
            byte[] buf_ = new byte[bufferSize];
            while (progress <= fileSize_) {
                int nread_ = in_.read(buf_, 0, buf_.length);
                if (nread_ <= 0) break;
                progress += nread_;
                out_.write(buf_, 0, nread_);
            }
            in_.close();
            out_.close();
            socketMaster.close(socket_);
            if (isDebugEnabled()) debug("Done with '" + file.getName() + "'");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            error("start()", ioe);
        }
    }

    public String info() {
        StringBuffer sb_ = new StringBuffer(super.info() + "peer: " + remoteAddress + "/" + remotePort + "\n");
        if (file == null) return sb_.toString() + "No in file is set up.\n"; else return sb_.toString() + "File read: " + file + "\n" + "BufferSize = " + bufferSize + "\n" + "Remote file: " + remoteFile + "\n" + "Progress: " + progress + "/" + file.length() + "\n";
    }
}
