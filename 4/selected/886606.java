package com.dsoft.telnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.log4j.Logger;
import com.dsoft.jca.driver.ConnectionException;
import com.dsoft.jca.driver.IConnectionDriver;

/**
 * @author Sadi Melbouci
 * 
 */
public class JTelnet extends TelnetClient implements IConnectionDriver {

    static Logger log = Logger.getLogger(TelnetClient.class);

    private String user = null;

    private String passwd = null;

    private String host = null;

    private int port = 23;

    private OutputStream output = null;

    private InputStream input = null;

    private OutputStream outData = null;

    private String prompt = null;

    public JTelnet(String hostname, String username, String password, int port) throws TelnetException {
        user = username;
        passwd = password;
        host = hostname;
        this.port = port;
    }

    public String send(String message) throws ConnectionException {
        String result = null;
        try {
            if (!isConnected()) connect();
            write(message);
            result = receive();
        } catch (Exception e) {
            log.error("Problem sending Data to " + host, e);
            throw new ConnectionException(e);
        }
        return result;
    }

    public String receive() throws ConnectionException {
        StringBuffer buffer = new StringBuffer();
        int bufsize = 2048;
        char[] data = null;
        try {
            int available = this.input.available();
            if (available > bufsize) bufsize = available;
            data = new char[bufsize];
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            for (; ; ) {
                System.out.println("Reading from InputStream");
                int size = in.read(data, 0, bufsize);
                if (size <= 0) break;
                String str = String.valueOf(data);
                System.out.println("Received: " + str);
                int index = str.lastIndexOf(prompt);
                if (index >= 0) {
                    buffer.append(str, 0, index);
                    break;
                }
                System.out.flush();
                buffer.append(data, 0, size);
                for (int i = 0; i < bufsize; i++) data[i] = '\0';
            }
        } catch (IOException ioe) {
            log.error("Socket is being closed", ioe);
            throw new ConnectionException(ioe);
        }
        return buffer.toString();
    }

    public void setTimeout(int timeout) {
        try {
            setSoTimeout(timeout);
        } catch (Exception ioe) {
            log.error("Error setting timeout", ioe);
        }
    }

    public void connect() throws ConnectionException {
        try {
            connect(host, port);
            output = getOutputStream();
            input = getInputStream();
        } catch (IOException ioe) {
            String msg = "Problem Connecting to " + host;
            log.error(msg, ioe);
            throw new ConnectionException(msg, ioe);
        }
        try {
            login(this.user, this.passwd);
        } catch (TelnetException te) {
            String msg = "Login Failed for " + this.user;
            log.error(msg, te);
            throw new ConnectionException(msg, te);
        }
    }

    private boolean read(String pattern) {
        BufferedReader in = new BufferedReader(new InputStreamReader(input));
        char[] data = new char[2048];
        try {
            while (true) {
                System.out.println("Reading Until: " + pattern);
                int size = in.read(data);
                String line = String.valueOf(data);
                System.out.println("Line : " + line);
                int index = line.indexOf(pattern);
                if (index >= 0) {
                    return true;
                }
                if (line.indexOf(prompt) >= 0) return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Can't Read Pattern: " + pattern);
            return false;
        }
    }

    public boolean readUntil(String pattern) {
        try {
            char lastChar = pattern.charAt(pattern.length() - 1);
            StringBuffer sb = new StringBuffer();
            char ch = (char) input.read();
            while (true) {
                sb.append(ch);
                if (ch == lastChar) {
                    if (sb.toString().endsWith(pattern)) {
                        System.out.println("Read: " + sb);
                        return true;
                    }
                }
                ch = (char) input.read();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void write(String msg) {
        PrintStream out = new PrintStream(output);
        out.println(msg);
        out.flush();
    }

    private void login(String user, String password) throws TelnetException {
        boolean pass = true;
        if (readUntil("login:")) write(user); else pass = false;
        if (readUntil("Password:") && pass) write(password);
        if (read("incorrect") || !pass) throw new TelnetException("Authentication Failed");
    }

    /**
     * @return the prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * @param prompt the prompt to set
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String execute(String command) throws ConnectionException {
        return send(command);
    }

    public void get(String local, String remote) throws ConnectionException {
        throw new ConnectionException("This is not implemented for Telnet");
    }

    public void put(String local, String remote) throws ConnectionException {
        throw new ConnectionException("This is not implemented for Telnet");
    }

    public String getHostname() {
        return null;
    }

    public String getPassword() {
        return null;
    }

    public int getPort() {
        return 0;
    }

    public int getTimeout() {
        return 0;
    }

    public String getUsername() {
        return null;
    }

    public void setHostname(String hostname) {
    }

    public void setPassword(String password) {
    }

    public void setPort(int port) {
    }

    public void setUsername(String username) {
    }
}
