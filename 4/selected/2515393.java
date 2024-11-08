package de.sciss.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 26-Sep-07
 */
public class AudioFileCacheInfo {

    private static final int VERSION = 1;

    private final int model, numChannels;

    private final long lastModified, numFrames;

    private final String name;

    private AudioFileCacheInfo(String name, long lastModified, int model, int numChannels, long numFrames) {
        this.name = name;
        this.lastModified = lastModified;
        this.model = model;
        this.numChannels = numChannels;
        this.numFrames = numFrames;
    }

    public AudioFileCacheInfo(InterleavedStreamFile f, int model, long numFrames) throws IOException {
        this(f.getFile().getName(), f.getFile().lastModified(), model, f.getChannelNum(), numFrames);
    }

    public static AudioFileCacheInfo decode(byte[] appCode) {
        final long lastModified, numFrames;
        final int numChannels, model;
        final String name;
        final DataInputStream dis;
        try {
            dis = new DataInputStream(new ByteArrayInputStream(appCode));
            if (dis.readInt() != VERSION) return null;
            name = dis.readUTF();
            lastModified = dis.readLong();
            model = dis.readInt();
            numChannels = dis.readInt();
            numFrames = dis.readLong();
            return new AudioFileCacheInfo(name, lastModified, model, numChannels, numFrames);
        } catch (IOException e1) {
            return null;
        }
    }

    public long getNumFrames() {
        return numFrames;
    }

    public long getNumChannels() {
        return numChannels;
    }

    public boolean equals(Object o) {
        if (o instanceof AudioFileCacheInfo) {
            AudioFileCacheInfo ci = (AudioFileCacheInfo) o;
            return (this.name.equals(ci.name) && (this.lastModified == ci.lastModified) && (this.numChannels == ci.numChannels) && (this.numFrames == ci.numFrames) && (this.model == ci.model));
        }
        return false;
    }

    public int hashCode() {
        return (name.hashCode() ^ model ^ -numChannels ^ (int) lastModified ^ (int) numFrames);
    }

    public byte[] encode() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(64);
        final DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(VERSION);
        dos.writeUTF(name);
        dos.writeLong(lastModified);
        dos.writeInt(model);
        dos.writeInt(numChannels);
        dos.writeLong(numFrames);
        while ((dos.size() & 3) != 0) dos.write(0);
        return baos.toByteArray();
    }
}
