import java.net.*;
import java.io.*;

public class TCPProxy {

    static final int BUFSIZE = 4096;

    String m_targetHost;

    int m_targetPort;

    int m_connectionsCount;

    public TCPProxy(String[] args) {
        try {
            if (args.length < 3) {
                displayHelp();
                System.exit(1);
            }
            int inPort = Integer.parseInt(args[0]);
            m_targetHost = args[1];
            m_targetPort = Integer.parseInt(args[2]);
            ServerSocket ss = new ServerSocket(inPort);
            for (; ; ) {
                Socket cs = ss.accept();
                ProxyService s = new ProxyService(cs);
                s.start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private synchronized void incrementConnectionCount() {
        m_connectionsCount++;
        displayConnectionCount();
    }

    private synchronized void decrementConnectionCount() {
        m_connectionsCount--;
        displayConnectionCount();
    }

    private void displayConnectionCount() {
        System.out.println(m_connectionsCount + " connections established.");
    }

    class ProxyService extends Thread {

        Socket m_inboundSocket;

        public ProxyService(Socket s) {
            m_inboundSocket = s;
        }

        public void run() {
            try {
                Socket outboundSocket = new Socket(m_targetHost, m_targetPort);
                incrementConnectionCount();
                Pipe pipeForward = new Pipe(m_inboundSocket, outboundSocket);
                Pipe pipeBackward = new Pipe(outboundSocket, m_inboundSocket);
                Thread threadForward = new Thread(pipeForward);
                threadForward.start();
                pipeBackward.run();
                try {
                    threadForward.join();
                } catch (InterruptedException ex) {
                }
                outboundSocket.close();
                decrementConnectionCount();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                m_inboundSocket.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        class Pipe implements Runnable {

            byte m_buf[];

            Socket m_inSock;

            Socket m_outSock;

            InputStream m_is;

            OutputStream m_os;

            Pipe m_coupledPipe;

            public Pipe(Socket inSock, Socket outSock) {
                m_buf = new byte[BUFSIZE];
                m_inSock = inSock;
                m_outSock = outSock;
            }

            public void run() {
                int fromPort;
                String fromAddr;
                int toPort;
                String toAddr;
                String pipeName = "unknown";
                try {
                    fromPort = m_inSock.getPort();
                    fromAddr = m_inSock.getInetAddress().getHostName();
                    int myInboundPort = m_inSock.getLocalPort();
                    int myOutboundPort = m_outSock.getLocalPort();
                    toPort = m_outSock.getPort();
                    toAddr = m_outSock.getInetAddress().getHostName();
                    m_is = m_inSock.getInputStream();
                    m_os = m_outSock.getOutputStream();
                    pipeName = "[" + fromAddr + ":" + fromPort + "]->[:" + myInboundPort + "->:" + myOutboundPort + "]->[" + toAddr + ":" + toPort + "]";
                    System.out.println("Pipe " + pipeName + " established.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                try {
                    while (!interrupted()) {
                        int readSize = m_is.read(m_buf);
                        if (readSize <= 0) break;
                        m_os.write(m_buf, 0, readSize);
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
                try {
                    m_inSock.shutdownInput();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                try {
                    m_outSock.shutdownOutput();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.out.println("Pipe " + pipeName + " discarded.");
            }
        }
    }

    static void main(String[] args) {
        TCPProxy inetProxy = new TCPProxy(args);
    }

    static void displayHelp() {
        System.out.println("usage: java TCPProxy <local port> <remote host> <remote port>");
        System.out.println();
        System.out.println("  This is a simple TCP proxy.  Redirects any incomming connection");
        System.out.println("  on <local port> to <remote host>:<remote port>.");
    }
}
