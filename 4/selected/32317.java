package cn.hansfly.shell;

import java.io.*;

public class jShell {

    Thread tIn;

    Thread tOut;

    Thread tErr;

    public jShell(String shellCommand) {
        Process child = null;
        try {
            child = Runtime.getRuntime().exec(shellCommand);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (child == null) {
            return;
        }
        final InputStream inputStream = child.getInputStream();
        final BufferedReader brOut = new BufferedReader(new InputStreamReader(inputStream));
        tOut = new Thread() {

            String line;

            int lineNumber = 0;

            public void run() {
                try {
                    while ((line = brOut.readLine()) != null) {
                        System.out.println(lineNumber + ". " + line);
                        lineNumber++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        final InputStream errorStream = child.getErrorStream();
        final BufferedReader brErr = new BufferedReader(new InputStreamReader(errorStream));
        tErr = new Thread() {

            String line;

            int lineNumber = 0;

            public void run() {
                try {
                    while ((line = brErr.readLine()) != null) {
                        System.out.println(lineNumber + ". " + line);
                        lineNumber++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        final OutputStream outputStream = child.getOutputStream();
        tIn = new Thread() {

            String line;

            public void run() {
                try {
                    while (true) {
                        outputStream.write((reader.readLine() + "\n").getBytes());
                        outputStream.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public void startIn() {
        if (tIn != null) {
            tIn.start();
        }
    }

    public void startErr() {
        if (tErr != null) {
            tErr.start();
        }
    }

    public void startOut() {
        if (tOut != null) {
            tOut.start();
        }
    }

    public void interruptIn() {
        if (tIn != null) {
            tIn.interrupt();
        }
    }

    public void interruptErr() {
        if (tErr != null) {
            tErr.interrupt();
        }
    }

    public void interruptOut() {
        if (tOut != null) {
            tOut.interrupt();
        }
    }
}
