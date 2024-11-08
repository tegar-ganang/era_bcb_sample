package com.intel.bluetooth;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import com.intel.bluetooth.emu.DeviceCommand;

/**
 * 
 */
class EmulatorCommandReceiver extends Thread {

    private EmulatorLocalDevice localDevice;

    private boolean stoped = false;

    EmulatorCommandReceiver(EmulatorLocalDevice localDevice) {
        super("BlueCoveEmulatorCommandReceiver");
        this.localDevice = localDevice;
    }

    void shutdownReceiver() {
        stoped = true;
    }

    public void run() {
        while (!stoped) {
            DeviceCommand cmd = localDevice.getDeviceManagerService().pollCommand(this.localDevice.getAddress());
            if (cmd == null) {
                break;
            }
            execute(cmd);
        }
    }

    private void execute(DeviceCommand command) {
        switch(command.getType()) {
            case keepAlive:
                break;
            case chagePowerState:
                localDevice.setLocalDevicePower((Boolean) command.getParameters()[0]);
                break;
            case updateLocalDeviceProperties:
                localDevice.updateLocalDeviceProperties();
                break;
            case createThreadDumpStdOut:
                threadDump(false);
                break;
            case createThreadDumpFile:
                threadDump(true);
                break;
            case shutdownJVM:
                System.exit(0);
                break;
        }
    }

    static void threadDump(boolean useFile) {
        SimpleDateFormat fmt = new SimpleDateFormat("MM-dd_HH-mm-ss");
        OutputStreamWriter out = null;
        try {
            File file = null;
            if (useFile) {
                file = new File("ThreadDump-" + fmt.format(new Date()) + ".log");
                out = new FileWriter(file);
            } else {
                out = new OutputStreamWriter(System.out);
            }
            Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
            for (Iterator<Map.Entry<Thread, StackTraceElement[]>> iterator = traces.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<Thread, StackTraceElement[]> entry = iterator.next();
                Thread thread = entry.getKey();
                out.write("Thread= " + thread.getName() + " " + (thread.isDaemon() ? "daemon" : "") + " prio=" + thread.getPriority() + "id=" + thread.getId() + " " + thread.getState());
                out.write("\n");
                StackTraceElement[] ste = entry.getValue();
                for (int i = 0; i < ste.length; i++) {
                    out.write("\t");
                    out.write(ste[i].toString());
                    out.write("\n");
                }
                out.write("---------------------------------\n");
            }
            out.close();
            out = null;
            if (useFile) {
                System.err.println("Full ThreadDump created " + file.getAbsolutePath());
            }
        } catch (IOException ignore) {
        } finally {
            try {
                out.close();
            } catch (IOException ignore) {
            }
        }
    }
}
