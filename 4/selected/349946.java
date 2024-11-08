package blueprint4j.utils;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

public class LoggingSocket extends Logging implements Runnable {

    private SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd 'at' HH'h'mm.ss");

    private ServerSocket server_socket = null;

    public Vector sockets = new Vector();

    public LoggingSocket(int port) throws IOException {
        super(true, true, true, true, true, true);
        server_socket = new ServerSocket(port);
        new Thread(this).start();
    }

    public void write(String level, long thread_id, String description, String details) {
        String msg = "\r\n\r\n" + level + " [" + thread_id + "] " + date_format.format(new Date()) + "\r\n" + "TCE " + date_format.format(new Date()) + "\r\n" + "Description: " + description + "\r\n" + details + "\r\n" + "-------------------------------------";
        for (int i = 0; i < sockets.size(); i++) {
            ((SocketLog) sockets.get(i)).logs.add(msg);
        }
    }

    public void run() {
        while (ThreadSchedule.isStillAlive()) {
            try {
                SocketLog log = new SocketLog(this, server_socket.accept());
                sockets.add(log);
                new Thread(log).start();
            } catch (Throwable th) {
            }
        }
    }

    public LogDataUnitVector findLogs(Integer tid, java.util.Date from_date, java.util.Date to_date, Logging.Level levels[]) throws IOException {
        return null;
    }

    public static class SocketLog implements Runnable {

        private LoggingSocket logging_socket = null;

        private Socket socket = null;

        public Vector logs = new Vector();

        public SocketLog(LoggingSocket logging_socket, Socket socket) {
            this.logging_socket = logging_socket;
            this.socket = socket;
        }

        public void run() {
            try {
                while (true) {
                    if (logs.size() > 0) {
                        socket.getOutputStream().write(logs.get(0).toString().getBytes());
                        logs.remove(0);
                    }
                    Thread.sleep(250);
                }
            } catch (Throwable th) {
                logging_socket.sockets.remove(this);
            }
        }
    }
}
