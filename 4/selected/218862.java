package com.gregor.rrd;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import com.gregor.util.TextUtils;

public class RRDPipe extends RRDTool {

    private static final long s_timeout_default = 1000 * 30;

    private String m_RRDTool = "rrdtool";

    private boolean m_isRunning = false;

    private Process m_process = null;

    private PrintWriter m_out = null;

    private BufferedInputStream m_in = null;

    private boolean interrupted = false;

    static final byte[] png_header = { -119, 80, 78, 71, 13, 10, 26, 10 };

    public static int byte2int(byte b) {
        return (b < 0) ? 256 + b : b;
    }

    public RRDPipe() {
    }

    public RRDPipe(String RRDTool) {
        m_RRDTool = RRDTool;
    }

    public String getRRDTool() {
        return m_RRDTool;
    }

    public void setRRDTool(String RRDTool) {
        m_RRDTool = RRDTool;
    }

    public synchronized void init() throws IOException, RRDException {
        if (m_isRunning) {
            return;
        }
        String[] s = new String[2];
        s[0] = m_RRDTool;
        s[1] = "-";
        try {
            m_process = Runtime.getRuntime().exec(s);
        } catch (IOException e) {
            throw new RRDException("Failed to exec rrdtool process: " + e);
        }
        m_out = new PrintWriter(m_process.getOutputStream());
        m_in = new BufferedInputStream(m_process.getInputStream());
        m_isRunning = true;
    }

    public synchronized void deInit() {
        if (!m_isRunning) {
            return;
        }
        Timer t = null;
        if (m_process != null) {
            t = new Timer();
            t.schedule(new TimerTask() {

                public void run() {
                    m_process.destroy();
                }
            }, s_timeout_default);
        }
        m_isRunning = false;
        if (m_out != null) {
            m_out.close();
            m_out = null;
        }
        if (m_in != null) {
            try {
                m_in.close();
            } catch (IOException e) {
            }
            m_in = null;
        }
        if (m_process != null) {
            try {
                m_process.waitFor();
            } catch (InterruptedException e) {
                m_process.destroy();
            }
        }
        if (t != null) {
            t.cancel();
        }
    }

    public String[] doCommand(String[] command, OutputStream out) throws RRDException, IOException {
        return doCommand(escapeAndJoinCommand(command), out);
    }

    public synchronized String[] doCommand(String command, OutputStream out) throws RRDException, IOException {
        List<String> output = new ArrayList<String>();
        String line;
        int lineNum;
        boolean isError = false;
        boolean isOk = false;
        String error = null;
        if (!m_isRunning) {
            init();
        }
        command = command.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        Timer t = new Timer();
        t.schedule(new TimerTask() {

            public void run() {
                interrupted = true;
                m_process.destroy();
                deInit();
            }
        }, s_timeout_default);
        try {
            m_out.println(command);
            m_out.flush();
            int i, count;
            boolean lastCR = false;
            lineNum = 1;
            while (true) {
                StringBuffer buf = new StringBuffer();
                boolean mightBePNG = true;
                for (count = 0; (i = m_in.read()) != -1; count++) {
                    if (count == 0 && lastCR && i == '\n') {
                        lastCR = false;
                        count--;
                        continue;
                    }
                    if (mightBePNG && byte2int(png_header[count]) != i) {
                        mightBePNG = false;
                    }
                    if (mightBePNG && count == 7) {
                        if (out != null) {
                            out.write(png_header);
                        }
                        doPNG(m_in, out);
                        buf = null;
                        break;
                    }
                    if (!mightBePNG && (i == '\n' || i == '\r')) {
                        if (i == '\r') {
                            lastCR = true;
                        }
                        break;
                    }
                    buf.append((char) i);
                }
                if (buf == null) {
                    continue;
                }
                line = buf.toString();
                if (line.startsWith("RRDtool ") && lineNum == 1) {
                    output.add(line);
                    error = null;
                    isError = true;
                } else if (line.startsWith("OK")) {
                    isOk = true;
                    break;
                } else if (line.startsWith("ERROR")) {
                    error = line.substring("ERROR:".length()).trim();
                    isError = true;
                } else {
                    output.add(line);
                }
                lineNum++;
            }
            if (!isOk) {
                deInit();
            }
            if (isError) {
                if (error == null) {
                    error = "Invalid command or your command was \"help\"\n" + TextUtils.join("\n", output.toArray(new String[output.size()])) + "\n";
                }
                throw new RRDException(error);
            }
        } catch (IOException e) {
            if (interrupted) {
                interrupted = false;
                throw new RRDException("timeout waiting for rrdtool command " + "to complete");
            }
            throw e;
        } catch (RRDException e) {
            if (interrupted) {
                interrupted = false;
                throw new RRDException("timeout waiting for rrdtool command " + "to complete");
            }
            throw e;
        }
        t.cancel();
        return output.toArray(new String[output.size()]);
    }

    public static void doPNG(InputStream in, OutputStream out) throws IOException, RRDException {
        byte[] input = new byte[1024];
        int read;
        int size;
        int to_read;
        boolean end = false;
        while (!end) {
            read = blockingRead(in, input, 8);
            if (read != 8) {
                throw new RRDException("Could not read enough characters " + "for a PNG chunk header.  Wanted 8, " + "got " + read);
            }
            if (out != null) {
                out.write(input, 0, read);
            }
            size = (byte2int(input[0]) << 24) + (byte2int(input[1]) << 16) + (byte2int(input[2]) << 8) + byte2int(input[3]);
            if (input[4] == 'I' && input[5] == 'E' && input[6] == 'N' && input[7] == 'D') {
                end = true;
            }
            size += 4;
            while (size > 0) {
                to_read = (size > input.length) ? input.length : size;
                read = blockingRead(in, input, to_read);
                if (read != to_read) {
                    throw new RRDException("Could not read enough " + "characters for a PNG data " + "chunk.  Wanted " + to_read + ", got " + read);
                }
                if (out != null) {
                    out.write(input, 0, read);
                }
                size -= to_read;
            }
        }
        return;
    }

    static int blockingRead(InputStream in, byte[] input, int size) throws IOException {
        int offset = 0;
        int read;
        while (offset < size) {
            read = in.read();
            if (read == -1) {
                return -1;
            }
            input[offset] = (byte) read;
            offset++;
            read = in.read(input, offset, size - offset);
            if (read == -1) {
                return -1;
            }
            offset += read;
        }
        return size;
    }
}
