package org.colombbus.tangara.util;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.Validate;

/**
 *
 * @version $Id$
 */
public class PrintWriterComposite extends PrintWriter {

    private ReadWriteLock writerListLock = new ReentrantReadWriteLock();

    private Collection<PrintWriter> writerList = new HashSet<PrintWriter>();

    public PrintWriterComposite() {
        super(new NullOutputStream());
    }

    public void addPrintWriter(PrintWriter writer) {
        Validate.notNull(writer, "writer argument is null");
        try {
            writerListLock.writeLock().lock();
            writerList.add(writer);
        } finally {
            writerListLock.writeLock().unlock();
        }
    }

    public void removePrintWriter(PrintWriter writer) {
        Validate.notNull(writer, "writer argument is null");
        try {
            writerListLock.writeLock().lock();
            writerList.remove(writer);
        } finally {
            writerListLock.writeLock().unlock();
        }
    }

    @Override
    public PrintWriter append(char c) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.append(c);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
        return this;
    }

    @Override
    public PrintWriter append(CharSequence csq, int start, int end) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.append(csq, start, end);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
        return this;
    }

    @Override
    public PrintWriter append(CharSequence csq) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.append(csq);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
        return this;
    }

    @Override
    public void close() {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.close();
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void flush() {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.flush();
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public PrintWriter format(Locale l, String format, Object... args) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.format(l, format, args);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
        return this;
    }

    @Override
    public PrintWriter format(String format, Object... args) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.format(format, args);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
        return this;
    }

    @Override
    public void print(boolean b) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.print(b);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void print(char c) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.print(c);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void print(char[] s) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.print(s);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void print(double d) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.print(d);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void print(float f) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.print(f);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void print(int i) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.print(i);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void print(long l) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.print(l);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void print(Object obj) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.print(obj);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void print(String s) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.print(s);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public PrintWriter printf(Locale l, String format, Object... args) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.printf(l, format, args);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
        return this;
    }

    @Override
    public PrintWriter printf(String format, Object... args) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.printf(format, args);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
        return this;
    }

    @Override
    public void println() {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.println();
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void println(boolean x) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.println(x);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void println(char x) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.println(x);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void println(char[] x) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.println(x);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void println(double x) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.println(x);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void println(float x) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.println(x);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void println(int x) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.println(x);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void println(long x) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.println(x);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void println(Object x) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.println(x);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void println(String x) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.println(x);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void write(char[] buf, int off, int len) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.write(buf, off, len);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void write(char[] buf) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.write(buf);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void write(int c) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.write(c);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void write(String s, int off, int len) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.write(s, off, len);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }

    @Override
    public void write(String s) {
        try {
            writerListLock.readLock().lock();
            for (PrintWriter writer : writerList) {
                writer.write(s);
            }
        } finally {
            writerListLock.readLock().unlock();
        }
    }
}
