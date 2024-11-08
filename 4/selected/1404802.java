package org.yjchun.hanghe.device.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yjchun.hanghe.Global;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/**
 * Execute a native application for the source of I/O.
 * This class uses seriali.exe that should exist at lib/native directory.
 * @author yjchun
 *
 */
public class ExecDeviceIO extends SerialDeviceIO {

    protected static final Logger log = LoggerFactory.getLogger(ExecDeviceIO.class);

    Process process;

    ProcessBuilder procBuilder;

    boolean disablePermanently = false;

    protected static String getExecPath() {
        String execpath = Global.config.getString("device.exec.path");
        if (execpath == null) execpath = "seriali";
        if (SystemUtils.IS_OS_WINDOWS) {
            if (!StringUtils.endsWithIgnoreCase(execpath, "exe")) execpath = execpath + ".exe";
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            if (!StringUtils.contains(execpath, "macosx")) execpath = execpath + "-macosx";
        } else if (SystemUtils.IS_OS_LINUX) {
            if (!StringUtils.contains(execpath, "linux")) execpath = execpath + "-linux-x86-32";
        }
        if (execpath.startsWith("/") || (SystemUtils.IS_OS_WINDOWS && execpath.charAt(1) == ':')) {
        } else {
            File f = new File("lib" + File.separator + "native", execpath);
            if (f.exists()) {
                execpath = f.getPath();
            }
        }
        return execpath;
    }

    public ExecDeviceIO(String portname) {
        super(portname);
    }

    @Override
    public boolean open(int timeout) {
        if (isOpen) return true;
        if (disablePermanently) return false;
        try {
            String execpath = getExecPath();
            log.debug("Opening executable device: {} (for port {})", execpath, portName);
            procBuilder = new ProcessBuilder(execpath, "-p", portName, "-s", "" + baudrate);
            process = procBuilder.start();
            Thread.sleep(300);
            try {
                int exitvalue = process.exitValue();
                log.debug("Executable failed with exit code {}", exitvalue);
                return false;
            } catch (IllegalThreadStateException e) {
            }
            in = process.getInputStream();
            out = process.getOutputStream();
            isOpen = true;
            onOpen(this);
        } catch (InterruptedException e1) {
        } catch (Exception e) {
            log.warn("Failed to execute process: {}", e.toString());
            disablePermanently = true;
        }
        return isOpen;
    }

    @Override
    public boolean canOpen() {
        return open();
    }

    /**
	 * Set speed of this port (e.g. 4800, 9600, 19200, 38400, ...)
	 * Should be called before open()
	 * @param speed
	 * @return
	 */
    public boolean setBaudRate(int speed) {
        baudrate = speed;
        return !isOpen;
    }

    public int getBoudRate() {
        if (!isOpen) return -1;
        return baudrate;
    }

    @Override
    public void close() {
        if (!isOpen) return;
        isOpen = false;
        try {
            process.exitValue();
        } catch (Exception e) {
            try {
                in.close();
                out.close();
                process.destroy();
            } catch (Exception e1) {
                log.warn("Error closing: {}", e.toString());
            }
        }
        log.debug("Serial port {} is closed", portName);
        onClose(this);
    }

    public static HashSet<String> getAvailablePortList() {
        HashSet<String> portlist = new HashSet<String>();
        String execpath = getExecPath();
        ProcessBuilder procBuilder = new ProcessBuilder(execpath, "-L");
        Process process;
        try {
            process = procBuilder.start();
            InputStream in = process.getInputStream();
            OutputStream out = process.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            out.write('\n');
            out.flush();
            while (true) {
                String s = reader.readLine();
                if (s == null) break;
                portlist.add(s);
            }
        } catch (IOException e) {
            log.debug("getAvailablePortList() failed to create process: {}", e.getMessage());
        }
        return portlist;
    }

    public static void main(String[] args) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger(LoggerContext.ROOT_NAME).setLevel(Level.toLevel("DEBUG"));
        log.debug("Available ports: {}", ExecDeviceIO.getAvailablePortList());
        ExecDeviceIO serial = new ExecDeviceIO("COM3");
        if (!serial.open()) return;
        InputStream in = serial.getInputStream();
        try {
            while (true) {
                byte[] buf = new byte[1];
                int nread = in.read(buf);
                if (nread < 0) break;
                System.out.write(buf, 0, nread);
            }
        } catch (Exception e) {
            log.error("Error: {}", e.toString());
        }
    }
}
