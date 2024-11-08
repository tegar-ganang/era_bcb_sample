package org.matsim.utils.vis.netvis.streaming;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author gunnar
 * 
 */
public abstract class BufferedStateA implements StateI {

    /**
     * This ByteArrayOutputStream's internal buffer also serves as this
     * BufferedStateA's buffer.
     */
    protected final BAOS baos;

    /**
     * This ByteArrayInputStream is linked to <code>baos</code>. Whenever it
     * is used to provide data, it obtains it from <code>baos</code>.
     */
    protected final BAIS bais;

    protected BufferedStateA() {
        this.baos = new BAOS(4);
        this.bais = new BAIS(baos);
    }

    public abstract void writeMyself(DataOutputStream out) throws IOException;

    public abstract void readMyself(DataInputStream in) throws IOException;

    public final void writeToStream(DataOutputStream stream) throws IOException {
        stream.writeInt(baos.getCount());
        stream.write(baos.getBuffer(), 0, baos.getCount());
    }

    public final void readFromStream(DataInputStream stream) throws IOException {
        int cnt = stream.readInt();
        baos.reset();
        for (int i = 0; i < cnt; i++) {
            baos.write(stream.readByte());
        }
    }

    public static final void skip(DataOutputStream stream) throws IOException {
        stream.writeInt(0);
    }

    public static final void skip(DataInputStream stream) throws IOException {
        int length = stream.readInt();
        stream.skipBytes(length);
    }

    public void getState() throws IOException {
        baos.reset();
        writeMyself(new DataOutputStream(baos));
    }

    public void setState() throws IOException {
        bais.reset();
        readMyself(new DataInputStream(bais));
    }

    protected class BAOS extends ByteArrayOutputStream {

        private BAOS(int size) {
            super(size);
        }

        private byte[] getBuffer() {
            return super.buf;
        }

        private int getCount() {
            return super.count;
        }
    }

    protected class BAIS extends ByteArrayInputStream {

        private BAOS source;

        private BAIS(BAOS baos) {
            super(baos.getBuffer(), 0, baos.getCount());
            this.source = baos;
        }

        @Override
        public void reset() {
            super.buf = source.getBuffer();
            super.count = source.getCount();
            super.pos = 0;
            super.mark = 0;
        }
    }
}
