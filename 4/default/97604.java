import java.net.*;
import java.io.*;

class ServerThread extends Thread {

    private ServerThread otherThread;

    private int index;

    private Socket socket;

    private DataInputStream in;

    private DataOutputStream out;

    ServerThread(int index, Socket socket) {
        this.index = index;
        this.socket = socket;
    }

    synchronized void setOther(ServerThread otherThread) {
        this.otherThread = otherThread;
    }

    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            for (; ; ) {
                int i = in.readInt();
                int j = in.readInt();
                if (otherThread != null) {
                    writeMove(i, j);
                    otherThread.writeMove(i, j);
                }
            }
        } catch (Exception e) {
            System.out.println("Player #" + index + " has disconnected");
        } finally {
            try {
                in.close();
            } catch (Exception e2) {
                ;
            }
            try {
                out.close();
            } catch (Exception e2) {
                ;
            }
            try {
                socket.close();
            } catch (Exception e2) {
                ;
            }
            if (otherThread != null) otherThread.setOther(null);
        }
    }

    private synchronized void writeMove(int i, int j) throws IOException {
        out.writeInt(i);
        out.writeInt(j);
        out.flush();
    }
}
