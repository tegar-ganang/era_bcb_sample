package ru.pit.tools.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class Runner {

    static FileChannel out;

    static Calendar startDate = new GregorianCalendar();

    static boolean done = false;

    public static void main(String[] args) throws IOException, InterruptedException {
        SimpleDateFormat sdf_log = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
        if (args.length < 2) {
            System.out.println("Error. Worng number of args.");
            return;
        }
        String id = Long.toHexString(startDate.getTimeInMillis());
        StringBuilder sb = new StringBuilder();
        FileOutputStream _out = new FileOutputStream("./log/runner." + id + ".log");
        out = _out.getChannel();
        File workDir = new File(args[0]);
        List command = new ArrayList();
        for (int i = 1; i < args.length; i++) {
            command.add(args[i]);
            sb.append(args[i] + " ");
        }
        prn("Work dir: " + workDir.getPath());
        prn("Command line: " + sb.toString());
        prn("Start date/time: " + sdf_log.format(startDate.getTime()));
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.directory(workDir);
        final Process p = builder.start();
        Thread waiter = new Thread() {

            public void run() {
                try {
                    p.waitFor();
                } catch (InterruptedException ex) {
                    return;
                }
                done = true;
            }
        };
        waiter.start();
        BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = null;
        prn("======================================");
        while (!done && ((line = is.readLine()) != null)) {
            prn(line);
        }
        prn("======================================");
        prn("Program terminated!");
    }

    private static void prn(String o) {
        o = o + "\n";
        System.out.print(o);
        ByteBuffer buf = ByteBuffer.wrap(o.getBytes());
        try {
            out.write(buf);
            out.force(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
