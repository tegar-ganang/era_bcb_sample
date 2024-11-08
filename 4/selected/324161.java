package edu.ucsd.ncmir.spl.io;

import edu.ucsd.ncmir.spl.utilities.PID;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 *
 * @author spl
 */
public class Logger extends PrintStream {

    private String _text_out = "";

    private FileChannel _file_channel;

    private static FileOutputStream _fos;

    private String _pid;

    public Logger(File file) throws FileNotFoundException, IOException {
        super(Logger._fos = new FileOutputStream(file, true));
        this._file_channel = Logger._fos.getChannel();
        this._pid = PID.getPIDString();
    }

    public Logger(String file_name) throws FileNotFoundException, IOException {
        this(new File(file_name));
    }

    @Override
    public void print(boolean b) {
        this.print(b ? "true" : "false");
    }

    @Override
    public void print(char c) {
        this.print(String.valueOf(c));
    }

    @Override
    public void print(char[] s) {
        for (char c : s) this.print(c);
    }

    @Override
    public void print(double d) {
        this.print(String.valueOf(d));
    }

    @Override
    public void print(float f) {
        this.print(String.valueOf(f));
    }

    @Override
    public void print(int i) {
        this.print(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        this.print(String.valueOf(l));
    }

    @Override
    public void print(Object obj) {
        this.print(String.valueOf(obj));
    }

    @Override
    public void print(String text) {
        try {
            FileLock lock = this._file_channel.lock();
            this._text_out += text;
            if (this._text_out.contains("\n")) {
                for (String s : this._text_out.split("\n")) {
                    Date date = new Date();
                    SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    String timestamp = date_format.format(date);
                    super.print(timestamp + " " + this._pid + " " + s + "\n");
                }
                this._text_out = "";
            }
            lock.release();
        } catch (Throwable t) {
        }
    }

    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        this.print(String.format(l, format, args));
        return this;
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        this.print(String.format(format, args));
        return this;
    }

    @Override
    public void println() {
        this.print("\n");
    }

    @Override
    public void println(boolean x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(char x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(char[] x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(double x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(float x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(int x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(long x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(Object x) {
        this.print(x);
        this.println();
    }

    @Override
    public void println(String x) {
        this.print(x);
        this.println();
    }
}
