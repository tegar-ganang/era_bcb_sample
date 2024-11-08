package com.qaessentials.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import com.qaessentials.utils.FileUtils.Permissions;

/**
 * @author rwall
 *
 */
public class Exec {

    private Process process = null;

    private boolean running = true;

    public Exec() {
    }

    public Exec(String cmd) throws Exception {
        exec(cmd, true);
    }

    public Exec(String[] cmd) throws Exception {
        exec(cmd, true);
    }

    public Exec(String cmd, boolean wait) throws Exception {
        exec(cmd, wait);
    }

    public Exec(String[] cmd, boolean wait) throws Exception {
        exec(cmd, wait);
    }

    public String exec(String cmd) throws Exception {
        return exec(cmd, true);
    }

    public String exec(String cmd, boolean wait) throws Exception {
        final String CMD = cmd;
        if (!wait) {
            new Thread() {

                public void run() {
                    try {
                        exec(CMD.split("\\s+"));
                    } catch (Exception E) {
                        E.printStackTrace();
                    }
                }
            }.start();
        } else {
            return exec(cmd.split("\\s+"), wait);
        }
        return null;
    }

    public String exec(String[] cmd) throws Exception {
        return exec(cmd, true);
    }

    public String exec(String[] cmd, boolean wait) throws Exception {
        StringBuffer tmp = new StringBuffer();
        for (String c : cmd) System.out.print(c + " ");
        System.out.print("\n");
        process = Runtime.getRuntime().exec(cmd);
        BufferedReader k = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader y = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        Thread t = new Thread() {

            public void run() {
                try {
                    process.waitFor();
                    running = false;
                } catch (Exception E) {
                }
            }
        };
        t.start();
        while ((y.ready() || k.ready()) || running) {
            String writeTmp = null;
            if (y.ready()) System.out.println(y.readLine());
            if (k.ready()) {
                writeTmp = k.readLine();
                tmp.append(writeTmp);
                System.out.println(writeTmp);
            }
        }
        t = null;
        return tmp.toString();
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        System.out.println(new Exec().exec("ls -la"));
    }

    public boolean isRunning() {
        return running;
    }
}
