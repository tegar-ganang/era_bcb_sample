package com.aelitis.azureus.core.devices.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;

public abstract class TranscodePipe {

    private final int BUFFER_SIZE = 128 * 1024;

    private final int BUFFER_CACHE_SIZE = BUFFER_SIZE * 3;

    protected volatile boolean paused;

    protected volatile boolean destroyed;

    protected volatile int bytes_available;

    protected volatile int max_bytes_per_sec;

    protected List<Socket> sockets = new ArrayList<Socket>();

    private ServerSocket server_socket;

    private AEThread2 refiller;

    private LinkedList<bufferCache> buffer_cache = new LinkedList<bufferCache>();

    private int buffer_cache_size;

    private Average connection_speed = Average.getInstance(1000, 10);

    private Average write_speed = Average.getInstance(1000, 10);

    private errorListener error_listener;

    protected TranscodePipe(errorListener _error_listener) throws IOException {
        error_listener = _error_listener;
        server_socket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        new AEThread2("TranscodePipe", true) {

            public void run() {
                while (!destroyed) {
                    try {
                        final Socket socket = server_socket.accept();
                        connection_speed.addValue(1);
                        new AEThread2("TranscodePipe", true) {

                            public void run() {
                                handleSocket(socket);
                            }
                        }.start();
                    } catch (Throwable e) {
                        if (!destroyed) {
                            destroy();
                        }
                        break;
                    }
                }
            }
        }.start();
    }

    public long getConnectionRate() {
        return (connection_speed.getAverage());
    }

    public long getWriteSpeed() {
        return (write_speed.getAverage());
    }

    protected abstract void handleSocket(Socket socket);

    protected void handlePipe(final InputStream is, final OutputStream os) {
        new AEThread2("TranscodePipe:c", true) {

            public void run() {
                final int BUFFER_SIZE = 128 * 1024;
                byte[] buffer = new byte[BUFFER_SIZE];
                while (!destroyed) {
                    try {
                        int limit;
                        if (paused) {
                            Thread.sleep(250);
                            limit = 1;
                        } else {
                            if (max_bytes_per_sec > 0) {
                                limit = bytes_available;
                                if (limit <= 0) {
                                    Thread.sleep(25);
                                    continue;
                                }
                                limit = Math.min(BUFFER_SIZE, limit);
                            } else {
                                limit = BUFFER_SIZE;
                            }
                        }
                        int len = is.read(buffer, 0, limit);
                        if (len <= 0) {
                            break;
                        }
                        if (max_bytes_per_sec > 0) {
                            bytes_available -= len;
                        }
                        os.write(buffer, 0, len);
                        write_speed.addValue(len);
                    } catch (Throwable e) {
                        break;
                    }
                }
                try {
                    os.flush();
                } catch (Throwable e) {
                }
                try {
                    is.close();
                } catch (Throwable e) {
                }
                try {
                    os.close();
                } catch (Throwable e) {
                }
            }
        }.start();
    }

    protected RandomAccessFile reserveRAF() throws IOException {
        throw (new IOException("Not implemented"));
    }

    protected void releaseRAF(RandomAccessFile raf) {
    }

    protected void handleRAF(final OutputStream os, final long position, final long length) {
        new AEThread2("TranscodePipe:c", true) {

            public void run() {
                RandomAccessFile raf = null;
                try {
                    raf = reserveRAF();
                    long pos = position;
                    long rem = length;
                    while (!destroyed && rem > 0) {
                        int limit;
                        if (paused) {
                            Thread.sleep(250);
                            limit = 1;
                        } else {
                            if (max_bytes_per_sec > 0) {
                                limit = bytes_available;
                                if (limit <= 0) {
                                    Thread.sleep(25);
                                    continue;
                                }
                                limit = Math.min(BUFFER_SIZE, limit);
                            } else {
                                limit = BUFFER_SIZE;
                            }
                            limit = (int) Math.min(rem, limit);
                        }
                        int read_length = 0;
                        int buffer_start = 0;
                        byte[] buffer = null;
                        synchronized (TranscodePipe.this) {
                            int c_num = 0;
                            Iterator<bufferCache> it = buffer_cache.iterator();
                            while (it.hasNext()) {
                                bufferCache b = it.next();
                                long rel_offset = pos - b.offset;
                                if (rel_offset >= 0) {
                                    byte[] data = b.data;
                                    long avail = data.length - rel_offset;
                                    if (avail > 0) {
                                        read_length = (int) Math.min(avail, limit);
                                        buffer = data;
                                        buffer_start = (int) rel_offset;
                                        if (c_num > 0) {
                                            it.remove();
                                            buffer_cache.addFirst(b);
                                        }
                                        break;
                                    }
                                }
                                c_num++;
                            }
                            if (buffer == null) {
                                buffer = new byte[limit];
                                raf.seek(pos);
                                read_length = raf.read(buffer);
                                if (read_length != limit) {
                                    Debug.out("eh?");
                                    throw (new IOException("Inconsistent"));
                                }
                                bufferCache b = new bufferCache(pos, buffer);
                                buffer_cache.addFirst(b);
                                buffer_cache_size += limit;
                                while (buffer_cache_size > BUFFER_CACHE_SIZE) {
                                    b = buffer_cache.removeLast();
                                    buffer_cache_size -= b.data.length;
                                }
                            }
                        }
                        if (read_length <= 0) {
                            break;
                        }
                        rem -= read_length;
                        pos += read_length;
                        if (max_bytes_per_sec > 0) {
                            bytes_available -= read_length;
                        }
                        os.write(buffer, buffer_start, read_length);
                        write_speed.addValue(read_length);
                    }
                    os.flush();
                } catch (Throwable e) {
                    if (raf != null) {
                        try {
                            synchronized (TranscodePipe.this) {
                                raf.seek(0);
                                raf.read(new byte[1]);
                            }
                        } catch (Throwable f) {
                            reportError(e);
                        }
                    }
                } finally {
                    try {
                        os.close();
                    } catch (Throwable e) {
                    }
                    if (raf != null) {
                        releaseRAF(raf);
                    }
                }
            }
        }.start();
    }

    protected void pause() {
        paused = true;
    }

    protected void resume() {
        paused = false;
    }

    public void setMaxBytesPerSecond(int max) {
        if (max == max_bytes_per_sec) {
            return;
        }
        max_bytes_per_sec = max;
        synchronized (this) {
            if (refiller == null) {
                refiller = new AEThread2("refiller", true) {

                    public void run() {
                        int count = 0;
                        while (!destroyed) {
                            if (max_bytes_per_sec == 0) {
                                synchronized (this) {
                                    if (max_bytes_per_sec == 0) {
                                        refiller = null;
                                        break;
                                    }
                                }
                            }
                            count++;
                            bytes_available += max_bytes_per_sec / 10;
                            if (count % 10 == 0) {
                                bytes_available += max_bytes_per_sec % 10;
                            }
                            try {
                                Thread.sleep(100);
                            } catch (Throwable e) {
                                Debug.printStackTrace(e);
                                break;
                            }
                        }
                    }
                };
                refiller.start();
            }
        }
    }

    protected int getPort() {
        return (server_socket.getLocalPort());
    }

    protected boolean destroy() {
        synchronized (this) {
            if (destroyed) {
                return (false);
            }
            destroyed = true;
        }
        for (Socket s : sockets) {
            try {
                s.close();
            } catch (Throwable e) {
            }
        }
        sockets.clear();
        try {
            server_socket.close();
        } catch (Throwable e) {
            Debug.printStackTrace(e);
        }
        return (true);
    }

    protected void reportError(Throwable error) {
        if (error_listener != null) {
            error_listener.error(error);
        } else {
            Debug.out(error);
        }
    }

    private class bufferCache {

        private long offset;

        private byte[] data;

        protected bufferCache(long _offset, byte[] _data) {
            offset = _offset;
            data = _data;
        }
    }

    protected interface errorListener {

        public void error(Throwable e);
    }
}
