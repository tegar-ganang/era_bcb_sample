package gov.nasa.jpf.jvm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.DynamicObjectArray;
import java.io.*;
import java.nio.channels.FileChannel;

/**
 * native peer for file descriptors, which are our basic interface to
 * access file contents. The implementation used here just forwards
 * to FileInputStreams, which is terribly inefficient for frequent
 * restores (in which case a simple byte[] buffer would be more efficient)
 */
public class JPF_java_io_FileDescriptor {

    static final int FD_READ = 0;

    static final int FD_WRITE = 1;

    static final int FD_NEW = 0;

    static final int FD_OPENED = 1;

    static final int FD_CLOSED = 2;

    static int count = 2;

    static DynamicObjectArray<Object> content;

    public static void init(Config conf) {
        content = new DynamicObjectArray<Object>();
        count = 2;
    }

    public static int open__Ljava_lang_String_2I__I(MJIEnv env, int objref, int fnameRef, int mode) {
        String fname = env.getStringObject(fnameRef);
        if (mode == FD_READ) {
            return openRead(fname);
        } else if (mode == FD_WRITE) {
            return openWrite(fname);
        } else {
            env.throwException("java.io.IOException", "illegal open mode: " + mode);
            return -1;
        }
    }

    public static int openRead(String fname) {
        File file = new File(fname);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                fis.getChannel();
                count++;
                content.set(count, fis);
                return count;
            } catch (IOException x) {
            }
        }
        return -1;
    }

    public static int openWrite(String fname) {
        File file = new File(fname);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.getChannel();
            count++;
            content.set(count, fos);
            return count;
        } catch (IOException x) {
        }
        return -1;
    }

    public static void close0(MJIEnv env, int objref) {
        int fd = env.getIntField(objref, "fd");
        try {
            Object fs = content.get(fd);
            if (fs != null) {
                if (fs instanceof FileInputStream) {
                    ((FileInputStream) fs).close();
                } else {
                    ((FileOutputStream) fs).close();
                }
            }
            content.set(fd, null);
        } catch (ArrayIndexOutOfBoundsException aobx) {
            env.throwException("java.io.IOException", "file not open");
        } catch (IOException iox) {
            env.throwException("java.io.IOException", iox.getMessage());
        }
    }

    static void reopen(MJIEnv env, int objref) throws IOException {
        int fd = env.getIntField(objref, "fd");
        long off = env.getLongField(objref, "off");
        if (content.get(fd) == null) {
            int mode = env.getIntField(objref, "mode");
            int fnRef = env.getReferenceField(objref, "fileName");
            String fname = env.getStringObject(fnRef);
            if (mode == FD_READ) {
                FileInputStream fis = new FileInputStream(fname);
                FileChannel fc = fis.getChannel();
                fc.position(off);
                content.set(fd, fis);
            } else if (mode == FD_WRITE) {
                FileOutputStream fos = new FileOutputStream(fname);
                FileChannel fc = fos.getChannel();
                fc.position(off);
                content.set(fd, fos);
            } else {
                env.throwException("java.io.IOException", "illegal mode: " + mode);
            }
        }
    }

    public static void write__I__(MJIEnv env, int objref, int b) {
        int fd = env.getIntField(objref, "fd");
        long off = env.getLongField(objref, "off");
        try {
            Object fs = content.get(fd);
            if (fs != null) {
                if (fs instanceof FileOutputStream) {
                    FileOutputStream fos = (FileOutputStream) fs;
                    FileChannel fc = fos.getChannel();
                    fc.position(off);
                    fos.write(b);
                    env.setLongField(objref, "off", fc.position());
                } else {
                    env.throwException("java.io.IOException", "write attempt on file opened for read access");
                }
            } else {
                if (env.getIntField(objref, "state") == FD_OPENED) {
                    reopen(env, objref);
                    write__I__(env, objref, b);
                } else {
                    env.throwException("java.io.IOException", "write attempt on closed file");
                }
            }
        } catch (ArrayIndexOutOfBoundsException aobx) {
            env.throwException("java.io.IOException", "file not open");
        } catch (IOException iox) {
            env.throwException("java.io.IOException", iox.getMessage());
        }
    }

    public static void write___3BII__(MJIEnv env, int objref, int bref, int offset, int len) {
        int fd = env.getIntField(objref, "fd");
        long off = env.getLongField(objref, "off");
        try {
            Object fs = content.get(fd);
            if (fs != null) {
                if (fs instanceof FileOutputStream) {
                    FileOutputStream fos = (FileOutputStream) fs;
                    FileChannel fc = fos.getChannel();
                    fc.position(off);
                    byte[] buf = new byte[len];
                    for (int i = 0, j = offset; i < len; i++, j++) {
                        buf[i] = env.getByteArrayElement(bref, j);
                    }
                    fos.write(buf);
                    env.setLongField(objref, "off", fc.position());
                } else {
                    env.throwException("java.io.IOException", "write attempt on file opened for read access");
                }
            } else {
                if (env.getIntField(objref, "state") == FD_OPENED) {
                    reopen(env, objref);
                    write___3BII__(env, objref, bref, offset, len);
                } else {
                    env.throwException("java.io.IOException", "write attempt on closed file");
                }
            }
        } catch (ArrayIndexOutOfBoundsException aobx) {
            env.throwException("java.io.IOException", "file not open");
        } catch (IOException iox) {
            env.throwException("java.io.IOException", iox.getMessage());
        }
    }

    public static int read____I(MJIEnv env, int objref) {
        int fd = env.getIntField(objref, "fd");
        long off = env.getLongField(objref, "off");
        try {
            Object fs = content.get(fd);
            if (fs != null) {
                if (fs instanceof FileInputStream) {
                    FileInputStream fis = (FileInputStream) fs;
                    FileChannel fc = fis.getChannel();
                    fc.position(off);
                    int r = fis.read();
                    env.setLongField(objref, "off", fc.position());
                    return r;
                } else {
                    env.throwException("java.io.IOException", "read attempt on file opened for write access");
                    return -1;
                }
            } else {
                if (env.getIntField(objref, "state") == FD_OPENED) {
                    reopen(env, objref);
                    return read____I(env, objref);
                } else {
                    env.throwException("java.io.IOException", "read attempt on closed file");
                    return -1;
                }
            }
        } catch (ArrayIndexOutOfBoundsException aobx) {
            env.throwException("java.io.IOException", "file not open");
            return -1;
        } catch (IOException iox) {
            env.throwException("java.io.IOException", iox.getMessage());
            return -1;
        }
    }

    public static int read___3BII__I(MJIEnv env, int objref, int bufref, int offset, int len) {
        int fd = env.getIntField(objref, "fd");
        long off = env.getLongField(objref, "off");
        try {
            Object fs = content.get(fd);
            if (fs != null) {
                if (fs instanceof FileInputStream) {
                    FileInputStream fis = (FileInputStream) fs;
                    FileChannel fc = fis.getChannel();
                    fc.position(off);
                    byte[] buf = new byte[len];
                    int r = fis.read(buf);
                    for (int i = 0, j = offset; i < len; i++, j++) {
                        env.setByteArrayElement(bufref, j, buf[i]);
                    }
                    env.setLongField(objref, "off", fc.position());
                    return r;
                } else {
                    env.throwException("java.io.IOException", "read attempt on file opened for write access");
                    return -1;
                }
            } else {
                if (env.getIntField(objref, "state") == FD_OPENED) {
                    reopen(env, objref);
                    return read___3BII__I(env, objref, bufref, offset, len);
                } else {
                    env.throwException("java.io.IOException", "read attempt on closed file");
                    return -1;
                }
            }
        } catch (ArrayIndexOutOfBoundsException aobx) {
            env.throwException("java.io.IOException", "file not open");
            return -1;
        } catch (IOException iox) {
            env.throwException("java.io.IOException", iox.getMessage());
            return -1;
        }
    }

    public static long skip__J__J(MJIEnv env, int objref, long nBytes) {
        int fd = env.getIntField(objref, "fd");
        long off = env.getLongField(objref, "off");
        try {
            Object fs = content.get(fd);
            if (fs != null) {
                if (fs instanceof FileInputStream) {
                    FileInputStream fis = (FileInputStream) fs;
                    FileChannel fc = fis.getChannel();
                    fc.position(off);
                    long r = fis.skip(nBytes);
                    env.setLongField(objref, "off", fc.position());
                    return r;
                } else {
                    env.throwException("java.io.IOException", "skip attempt on file opened for write access");
                    return -1;
                }
            } else {
                env.throwException("java.io.IOException", "skip attempt on closed file");
                return -1;
            }
        } catch (ArrayIndexOutOfBoundsException aobx) {
            env.throwException("java.io.IOException", "file not open");
            return -1;
        } catch (IOException iox) {
            env.throwException("java.io.IOException", iox.getMessage());
            return -1;
        }
    }

    public static void sync____(MJIEnv env, int objref) {
        int fd = env.getIntField(objref, "fd");
        try {
            Object fs = content.get(fd);
            if (fs != null) {
                if (fs instanceof FileOutputStream) {
                    ((FileOutputStream) fs).flush();
                } else {
                }
            } else {
                env.throwException("java.io.IOException", "sync attempt on closed file");
            }
        } catch (ArrayIndexOutOfBoundsException aobx) {
            env.throwException("java.io.IOException", "file not open");
        } catch (IOException iox) {
            env.throwException("java.io.IOException", iox.getMessage());
        }
    }

    public static int available____I(MJIEnv env, int objref) {
        int fd = env.getIntField(objref, "fd");
        long off = env.getLongField(objref, "off");
        try {
            Object fs = content.get(fd);
            if (fs != null) {
                if (fs instanceof FileInputStream) {
                    FileInputStream fis = (FileInputStream) fs;
                    FileChannel fc = fis.getChannel();
                    fc.position(off);
                    return fis.available();
                } else {
                    env.throwException("java.io.IOException", "available() on file opened for write access");
                    return -1;
                }
            } else {
                env.throwException("java.io.IOException", "available() on closed file");
                return -1;
            }
        } catch (ArrayIndexOutOfBoundsException aobx) {
            env.throwException("java.io.IOException", "file not open");
            return -1;
        } catch (IOException iox) {
            env.throwException("java.io.IOException", iox.getMessage());
            return -1;
        }
    }
}
