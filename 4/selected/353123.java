package org.retro.gis;

import java.io.*;
import java.net.*;
import java.util.*;

public class InputThread extends Thread {

    private PircBot _bot = null;

    private Socket _socket = null;

    private BufferedReader _breader = null;

    private BufferedWriter _bwriter = null;

    private boolean _isConnected = true;

    private boolean _disposed = false;

    public static final int MAX_LINE_LENGTH = 512;

    public InputThread(PircBot bot, Socket socket, BufferedReader breader, BufferedWriter bwriter) {
        _bot = bot;
        _socket = socket;
        _breader = breader;
        _bwriter = bwriter;
        this.setName(this.getClass() + "-Thread");
    }

    public void sendRawLine(String line) {
        OutputThread.sendRawLine(_bot, _bwriter, line);
    }

    public boolean isConnected() {
        return _isConnected;
    }

    public void run() {
        try {
            boolean running = true;
            while (running) {
                try {
                    String line = null;
                    while ((line = _breader.readLine()) != null) {
                        try {
                            _bot.handleLine(line);
                        } catch (Throwable t) {
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            t.printStackTrace(pw);
                            pw.flush();
                            StringTokenizer tokenizer = new StringTokenizer(sw.toString(), "\r\n");
                            synchronized (_bot) {
                                _bot.log("###[Pirc-Input] Your implementation of PircBot is faulty and you have");
                                _bot.log("###[Pirc-Input] allowed an uncaught Exception or Error to propagate in your");
                                _bot.log("###[Pirc-Input] code. It may be possible for PircBot to continue operating");
                                _bot.log("###[Pirc-Input] normally. Here is the stack trace that was produced: -");
                                _bot.log("###[Pirc-Input] ");
                                while (tokenizer.hasMoreTokens()) {
                                    _bot.log("###[Pirc-Input] " + tokenizer.nextToken());
                                }
                            }
                        }
                    }
                    if (line == null) {
                        running = false;
                    }
                } catch (InterruptedIOException iioe) {
                    this.sendRawLine("PING " + (System.currentTimeMillis() / 1000));
                }
            }
        } catch (Exception e) {
        }
        try {
            _socket.close();
        } catch (Exception e) {
        }
        if (!_disposed) {
            _bot.log("*** Disconnected.");
            _isConnected = false;
            _bot.onDisconnect();
        }
    }

    public void dispose() {
        try {
            _disposed = true;
            _socket.close();
            _socket = null;
        } catch (Exception e) {
        }
    }
}
