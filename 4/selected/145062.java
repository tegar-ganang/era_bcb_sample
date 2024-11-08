package drcl.inet.application;

import java.util.*;
import java.io.*;
import drcl.data.*;
import drcl.comp.*;
import drcl.net.*;
import drcl.inet.*;

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
public class ftp extends SApplication implements ActiveComponent {

    File file;

    int bufferSize;

    long progress;

    public ftp() {
        super();
    }

    public ftp(String id_) {
        super(id_);
    }

    public void setup(String infile_, int bufferSize_) throws IOException {
        bufferSize = bufferSize_;
        file = new File(infile_);
    }

    public void reset() {
        super.reset();
        progress = 0;
    }

    public void duplicate(Object source_) {
        super.duplicate(source_);
        bufferSize = ((ftp) source_).bufferSize;
    }

    protected void _start() {
        if (file == null) {
            error("start()", "no file is set up to send");
            return;
        }
        progress = 0;
        long fileSize_ = file.length();
        try {
            FileInputStream in_ = new FileInputStream(file);
            DataOutputStream out_ = new DataOutputStream(getOutputStream());
            if (isDebugEnabled()) debug("Start transmitting a file of size " + fileSize_);
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
            if (isDebugEnabled()) debug("Done with '" + file.getName() + "'");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            error("start()", ioe);
        }
    }

    public String info() {
        if (file == null) return "No in file is set up.\n"; else return "File read: " + file + "\n" + "BufferSize = " + bufferSize + "\n" + "Progress: " + progress + "/" + file.length() + "\n" + super.info();
    }
}
