package org.privale.utils.network.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import org.privale.utils.FileManager;

public class Client {

    private ByteBuffer Buffer;

    private static FileManager FM;

    public FileManager getTmp() {
        if (FM == null) {
            try {
                FM = FileManager.getDir("client");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return FM;
    }

    public Client() {
        Buffer = ByteBuffer.allocate(4096);
    }

    public void SendFile(File testfile) {
        try {
            SocketChannel sock = SocketChannel.open(new InetSocketAddress("127.0.0.1", 1234));
            sock.configureBlocking(true);
            while (!sock.finishConnect()) {
                System.out.println("NOT connected!");
            }
            System.out.println("CONNECTED!");
            FileInputStream fis = new FileInputStream(testfile);
            FileChannel fic = fis.getChannel();
            long len = fic.size();
            Buffer.clear();
            Buffer.putLong(len);
            Buffer.flip();
            sock.write(Buffer);
            long cnt = 0;
            while (cnt < len) {
                Buffer.clear();
                int add = fic.read(Buffer);
                cnt += add;
                Buffer.flip();
                while (Buffer.hasRemaining()) {
                    sock.write(Buffer);
                }
            }
            fic.close();
            File tmpfile = getTmp().createNewFile("tmp", "tmp");
            FileOutputStream fos = new FileOutputStream(tmpfile);
            FileChannel foc = fos.getChannel();
            int mlen = -1;
            do {
                Buffer.clear();
                mlen = sock.read(Buffer);
                Buffer.flip();
                if (mlen > 0) {
                    foc.write(Buffer);
                }
            } while (mlen > 0);
            foc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        int numsenders = 20;
        File intfile = new File(args[0]);
        Client.SendThread[] tary = new Client.SendThread[numsenders];
        for (int cnt = 0; cnt < numsenders; cnt++) {
            Client c = new Client();
            SendThread t = c.new SendThread(c, intfile);
            tary[cnt] = t;
        }
        for (int cnt = 0; cnt < numsenders; cnt++) {
            tary[cnt].Go();
        }
    }

    public class SendThread implements Runnable {

        private Client C;

        private File OutFile;

        private Thread T;

        public SendThread(Client c, File f) {
            C = c;
            OutFile = f;
            T = new Thread(this);
        }

        public void Go() {
            T.start();
        }

        public void run() {
            C.SendFile(OutFile);
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
