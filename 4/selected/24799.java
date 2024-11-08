package ch.comtools.jsch;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public abstract class Channel implements Runnable {

    static int index = 0;

    private static java.util.Vector pool = new java.util.Vector();

    static Channel getChannel(String type) {
        if (type.equals("session")) {
            return new ChannelSession();
        }
        if (type.equals("shell")) {
            return new ChannelShell();
        }
        if (type.equals("exec")) {
            return new ChannelExec();
        }
        if (type.equals("x11")) {
            return new ChannelX11();
        }
        if (type.equals("auth-agent@openssh.com")) {
            return new ChannelAgentForwarding();
        }
        if (type.equals("direct-tcpip")) {
            return new ChannelDirectTCPIP();
        }
        if (type.equals("forwarded-tcpip")) {
            return new ChannelForwardedTCPIP();
        }
        if (type.equals("sftp")) {
            return new ChannelSftp();
        }
        if (type.equals("subsystem")) {
            return new ChannelSubsystem();
        }
        return null;
    }

    static Channel getChannel(int id, Session session) {
        synchronized (pool) {
            for (int i = 0; i < pool.size(); i++) {
                Channel c = (Channel) (pool.elementAt(i));
                if (c.id == id && c.session == session) return c;
            }
        }
        return null;
    }

    static void del(Channel c) {
        synchronized (pool) {
            pool.removeElement(c);
        }
    }

    int id;

    int recipient = -1;

    byte[] type = "foo".getBytes();

    int lwsize_max = 0x100000;

    int lwsize = lwsize_max;

    int lmpsize = 0x4000;

    int rwsize = 0;

    int rmpsize = 0;

    IO io = null;

    Thread thread = null;

    boolean eof_local = false;

    boolean eof_remote = false;

    boolean close = false;

    boolean connected = false;

    int exitstatus = -1;

    int reply = 0;

    int connectTimeout = 0;

    Session session;

    Channel() {
        synchronized (pool) {
            id = index++;
            pool.addElement(this);
        }
    }

    void setRecipient(int foo) {
        this.recipient = foo;
    }

    int getRecipient() {
        return recipient;
    }

    void init() {
    }

    public void connect() throws JSchException {
        connect(0);
    }

    public void connect(int connectTimeout) throws JSchException {
        if (!session.isConnected()) {
            throw new JSchException("session is down");
        }
        this.connectTimeout = connectTimeout;
        try {
            Buffer buf = new Buffer(100);
            Packet packet = new Packet(buf);
            packet.reset();
            buf.putByte((byte) 90);
            buf.putString(this.type);
            buf.putInt(this.id);
            buf.putInt(this.lwsize);
            buf.putInt(this.lmpsize);
            session.write(packet);
            int retry = 1000;
            long start = System.currentTimeMillis();
            long timeout = connectTimeout;
            while (this.getRecipient() == -1 && session.isConnected() && retry > 0) {
                if (timeout > 0L) {
                    if ((System.currentTimeMillis() - start) > timeout) {
                        retry = 0;
                        continue;
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (Exception ee) {
                }
                retry--;
            }
            if (!session.isConnected()) {
                throw new JSchException("session is down");
            }
            if (retry == 0) {
                throw new JSchException("channel is not opened.");
            }
            connected = true;
            start();
        } catch (Exception e) {
            connected = false;
            if (e instanceof JSchException) throw (JSchException) e;
        }
    }

    public void setXForwarding(boolean foo) {
    }

    public void start() throws JSchException {
    }

    public boolean isEOF() {
        return eof_remote;
    }

    void getData(Buffer buf) {
        setRecipient(buf.getInt());
        setRemoteWindowSize(buf.getInt());
        setRemotePacketSize(buf.getInt());
    }

    public void setInputStream(InputStream in) {
        io.setInputStream(in, false);
    }

    public void setInputStream(InputStream in, boolean dontclose) {
        io.setInputStream(in, dontclose);
    }

    public void setOutputStream(OutputStream out) {
        io.setOutputStream(out, false);
    }

    public void setOutputStream(OutputStream out, boolean dontclose) {
        io.setOutputStream(out, dontclose);
    }

    public void setExtOutputStream(OutputStream out) {
        io.setExtOutputStream(out, false);
    }

    public void setExtOutputStream(OutputStream out, boolean dontclose) {
        io.setExtOutputStream(out, dontclose);
    }

    public InputStream getInputStream() throws IOException {
        PipedInputStream in = new MyPipedInputStream(32 * 1024);
        io.setOutputStream(new PassiveOutputStream(in), false);
        return in;
    }

    public InputStream getExtInputStream() throws IOException {
        PipedInputStream in = new MyPipedInputStream(32 * 1024);
        io.setExtOutputStream(new PassiveOutputStream(in), false);
        return in;
    }

    public OutputStream getOutputStream() throws IOException {
        final Channel channel = this;
        OutputStream out = new OutputStream() {

            private int dataLen = 0;

            private Buffer buffer = null;

            private Packet packet = null;

            private boolean closed = false;

            private void init() {
                buffer = new Buffer(rmpsize);
                packet = new Packet(buffer);
            }

            byte[] b = new byte[1];

            public void write(int w) throws java.io.IOException {
                b[0] = (byte) w;
                write(b, 0, 1);
            }

            public void write(byte[] buf, int s, int l) throws java.io.IOException {
                if (packet == null) {
                    init();
                }
                if (closed) {
                    throw new java.io.IOException("Already closed");
                }
                byte[] _buf = buffer.buffer;
                int _bufl = _buf.length;
                while (l > 0) {
                    int _l = l;
                    if (l > _bufl - (14 + dataLen) - 32 - 20) {
                        _l = _bufl - (14 + dataLen) - 32 - 20;
                    }
                    if (_l <= 0) {
                        flush();
                        continue;
                    }
                    System.arraycopy(buf, s, _buf, 14 + dataLen, _l);
                    dataLen += _l;
                    s += _l;
                    l -= _l;
                }
            }

            public void flush() throws java.io.IOException {
                if (closed) {
                    throw new java.io.IOException("Already closed");
                }
                if (dataLen == 0) return;
                packet.reset();
                buffer.putByte((byte) Session.SSH_MSG_CHANNEL_DATA);
                buffer.putInt(recipient);
                buffer.putInt(dataLen);
                buffer.skip(dataLen);
                try {
                    int foo = dataLen;
                    dataLen = 0;
                    session.write(packet, channel, foo);
                } catch (Exception e) {
                    close();
                    throw new java.io.IOException(e.toString());
                }
            }

            public void close() throws java.io.IOException {
                if (packet == null) {
                    init();
                }
                if (closed) {
                    return;
                }
                if (dataLen > 0) {
                    flush();
                }
                channel.eof();
                closed = true;
            }
        };
        return out;
    }

    class MyPipedInputStream extends PipedInputStream {

        MyPipedInputStream() throws IOException {
            super();
        }

        MyPipedInputStream(int size) throws IOException {
            super();
            buffer = new byte[size];
        }

        MyPipedInputStream(PipedOutputStream out) throws IOException {
            super(out);
        }

        MyPipedInputStream(PipedOutputStream out, int size) throws IOException {
            super(out);
            buffer = new byte[size];
        }
    }

    void setLocalWindowSizeMax(int foo) {
        this.lwsize_max = foo;
    }

    void setLocalWindowSize(int foo) {
        this.lwsize = foo;
    }

    void setLocalPacketSize(int foo) {
        this.lmpsize = foo;
    }

    synchronized void setRemoteWindowSize(int foo) {
        this.rwsize = foo;
    }

    synchronized void addRemoteWindowSize(int foo) {
        this.rwsize += foo;
    }

    void setRemotePacketSize(int foo) {
        this.rmpsize = foo;
    }

    public void run() {
    }

    void write(byte[] foo) throws IOException {
        write(foo, 0, foo.length);
    }

    void write(byte[] foo, int s, int l) throws IOException {
        try {
            io.put(foo, s, l);
        } catch (NullPointerException e) {
        }
    }

    void write_ext(byte[] foo, int s, int l) throws IOException {
        try {
            io.put_ext(foo, s, l);
        } catch (NullPointerException e) {
        }
    }

    void eof_remote() {
        eof_remote = true;
        try {
            if (io.out != null) {
                io.out.close();
                io.out = null;
            }
        } catch (NullPointerException e) {
        } catch (IOException e) {
        }
    }

    void eof() {
        if (close) return;
        if (eof_local) return;
        eof_local = true;
        try {
            Buffer buf = new Buffer(100);
            Packet packet = new Packet(buf);
            packet.reset();
            buf.putByte((byte) Session.SSH_MSG_CHANNEL_EOF);
            buf.putInt(getRecipient());
            session.write(packet);
        } catch (Exception e) {
        }
    }

    void close() {
        if (close) return;
        close = true;
        eof_local = eof_remote = true;
        try {
            Buffer buf = new Buffer(100);
            Packet packet = new Packet(buf);
            packet.reset();
            buf.putByte((byte) Session.SSH_MSG_CHANNEL_CLOSE);
            buf.putInt(getRecipient());
            session.write(packet);
        } catch (Exception e) {
        }
    }

    public boolean isClosed() {
        return close;
    }

    static void disconnect(Session session) {
        Channel[] channels = null;
        int count = 0;
        synchronized (pool) {
            channels = new Channel[pool.size()];
            for (int i = 0; i < pool.size(); i++) {
                try {
                    Channel c = ((Channel) (pool.elementAt(i)));
                    if (c.session == session) {
                        channels[count++] = c;
                    }
                } catch (Exception e) {
                }
            }
        }
        for (int i = 0; i < count; i++) {
            channels[i].disconnect();
        }
    }

    public void disconnect() {
        synchronized (this) {
            if (!connected) {
                return;
            }
            connected = false;
        }
        close();
        eof_remote = eof_local = true;
        thread = null;
        try {
            if (io != null) {
                io.close();
            }
        } catch (Exception e) {
        }
        io = null;
        Channel.del(this);
    }

    public boolean isConnected() {
        if (this.session != null) {
            return session.isConnected() && connected;
        }
        return false;
    }

    public void sendSignal(String foo) throws Exception {
        RequestSignal request = new RequestSignal();
        request.setSignal(foo);
        request.request(session, this);
    }

    class PassiveInputStream extends MyPipedInputStream {

        PipedOutputStream out;

        PassiveInputStream(PipedOutputStream out, int size) throws IOException {
            super(out, size);
            this.out = out;
        }

        PassiveInputStream(PipedOutputStream out) throws IOException {
            super(out);
            this.out = out;
        }

        public void close() throws IOException {
            if (out != null) {
                this.out.close();
            }
            out = null;
        }
    }

    class PassiveOutputStream extends PipedOutputStream {

        PassiveOutputStream(PipedInputStream in) throws IOException {
            super(in);
        }
    }

    void setExitStatus(int foo) {
        exitstatus = foo;
    }

    public int getExitStatus() {
        return exitstatus;
    }

    void setSession(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public int getId() {
        return id;
    }
}
