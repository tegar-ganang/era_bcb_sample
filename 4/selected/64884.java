package test.banking.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class BankingClient {

    private static int port;

    private static String host;

    private OutputStream os;

    private InputStream is;

    private boolean showtime = true;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            host = "localhost";
            port = 80;
        }
        BankingClient client = new BankingClient();
        client.run();
    }

    private void run() throws IOException {
        Socket sock = new Socket(host, port);
        os = sock.getOutputStream();
        is = sock.getInputStream();
        startReaderThread();
        startWriterThread();
    }

    private void startReaderThread() {
        Runnable reader = new Runnable() {

            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                while (showtime) {
                    try {
                        String line = reader.readLine();
                        System.out.println(line);
                    } catch (IOException e) {
                        System.out.println(e);
                    }
                }
            }
        };
        new Thread(reader, "Reader Thread").start();
    }

    private void startWriterThread() {
        Runnable writerThread = new Runnable() {

            public void run() {
                PrintWriter serverStream = new PrintWriter(os);
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                while (showtime) {
                    try {
                        String line = reader.readLine();
                        if (line.equals("exit")) {
                            showtime = false;
                        }
                        serverStream.println(line);
                        serverStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        new Thread(writerThread, "Writer Thread").start();
    }
}
