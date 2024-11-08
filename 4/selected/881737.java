package gov.nasa.jpf.network.alphabet;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class AlphabetServer {

    static class WorkerThread extends Thread {

        Socket sock;

        WorkerThread(Socket s) {
            sock = s;
        }

        public void run() {
            InputStream is = null;
            OutputStream os = null;
            int in, out;
            boolean finished = false;
            try {
                is = sock.getInputStream();
                os = sock.getOutputStream();
                while (!finished) {
                    System.out.println("[SERVER] threadID " + getId() + " reading ...");
                    in = is.read();
                    if (in == -1) finished = true; else {
                        System.out.println("[SERVER] threadID " + getId() + " reads " + (char) in);
                        out = in - '0' + 'a';
                        System.out.println("[SERVER] threadID " + getId() + " writes " + (char) out);
                        os.write(out);
                    }
                }
            } catch (IOException e) {
            }
            try {
                assert (is != null && os != null && sock != null);
                is.close();
                os.close();
                sock.close();
            } catch (IOException e) {
            }
            System.out.println("[AlphabetServer.WorkerThread : run() threadID=" + getId() + "] end of stream");
        }
    }

    public static final int SERVER_PORT = 18586;

    public static void main(String[] args) throws IOException {
        Socket[] sock;
        ServerSocket receiver = new ServerSocket(SERVER_PORT);
        int maxConnection;
        maxConnection = Integer.parseInt(args[0]);
        if (maxConnection > 0) {
            sock = new Socket[maxConnection];
            System.out.println("[AlphabetServer : main(String[])] starts");
            for (int i = 0; i < maxConnection; i++) {
                sock[i] = receiver.accept();
                System.out.println("[AlphabetServer : main(String[])] accept a connection");
                new WorkerThread(sock[i]).start();
            }
        } else {
            System.out.println("[AlphabetServer : main(String[])] starts");
            while (true) {
                Socket s = receiver.accept();
                System.out.println("[AlphabetServer : main(String[])] accept a connection");
                new WorkerThread(s).start();
            }
        }
    }
}
