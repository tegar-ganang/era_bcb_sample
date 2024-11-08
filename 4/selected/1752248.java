package bman.filecopy;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;
import bman.tools.net.ByteReceiver;
import bman.tools.net.ByteStation;
import bman.tools.net.Radio;
import bman.tools.net.Receiver;
import bman.tools.net.SocketSession;

public class FileSysRemote implements Receiver, ByteReceiver {

    Logger log = Logger.getLogger(this.getClass().getName());

    Radio radio;

    MessageReceiver receiver;

    ByteStation bt;

    String address;

    public FileSysRemote(String address, MessageReceiver receiver) {
        try {
            this.receiver = receiver;
            radio = new Radio(this);
            bt = new ByteStation(this);
            this.address = address;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void receive(Socket sourceSocket, byte[] message) {
        log.info("Recieved: " + new String(message));
        receiver.receive(new String(message));
    }

    public boolean send(String cmd) {
        if (cmd.startsWith("cp")) {
            cmd = "cp " + this.bt.getPort() + " " + cmd.substring(3);
        }
        return radio.send(address, cmd.getBytes());
    }

    public void receive(SocketSession session) {
        SocketChannel channel = session.getChannel();
        try {
            int l = session.readInt();
            log.info("FileName length: " + l);
            String fileName = new String(session.read(l));
            log.info("FileName: " + fileName);
            long size = session.readLong();
            log.info("File size: " + size);
            File f = new File(fileName);
            FileSizeTracker fst = new FileSizeTracker(f, size, receiver);
            fst.start();
            long bytesWritten = session.receiveFileAndSaveTo(size, f);
            log.info("Bytes written: " + bytesWritten);
            fst.run = false;
            log.info("FileSysRemote receive complete");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
