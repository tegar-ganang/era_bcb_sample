package proj.zoie.impl.indexing.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import proj.zoie.api.DirectoryManager;
import proj.zoie.api.impl.util.ChannelUtil;
import proj.zoie.impl.indexing.internal.ZoieIndexDeletionPolicy.Snapshot;

/**
 * @author ymatsuda
 *
 */
public class DiskIndexSnapshot {

    private DirectoryManager _dirMgr;

    private IndexSignature _sig;

    private Snapshot _snapshot;

    public DiskIndexSnapshot(DirectoryManager dirMgr, IndexSignature sig, Snapshot snapshot) {
        _dirMgr = dirMgr;
        _sig = sig;
        _snapshot = snapshot;
    }

    public void close() {
        _snapshot.close();
    }

    public DirectoryManager getDirecotryManager() {
        return _dirMgr;
    }

    public long writeTo(WritableByteChannel channel) throws IOException {
        long amount = 0;
        amount += ChannelUtil.writeInt(channel, 1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        _sig.save(baos);
        byte[] sigBytes = baos.toByteArray();
        amount += ChannelUtil.writeLong(channel, (long) sigBytes.length);
        amount += channel.write(ByteBuffer.wrap(sigBytes));
        Collection<String> fileNames = _snapshot.getFileNames();
        amount += ChannelUtil.writeInt(channel, fileNames.size());
        for (String fileName : fileNames) {
            amount += ChannelUtil.writeString(channel, fileName);
            amount += _dirMgr.transferFromFileToChannel(fileName, channel);
        }
        return amount;
    }

    public static void readSnapshot(ReadableByteChannel channel, DirectoryManager dirMgr) throws IOException {
        int formatVersion = ChannelUtil.readInt(channel);
        if (formatVersion != 1) {
            throw new IOException("snapshot format version mismatch [" + formatVersion + "]");
        }
        if (!dirMgr.transferFromChannelToFile(channel, DirectoryManager.INDEX_DIRECTORY)) {
            throw new IOException("bad snapshot file");
        }
        int numFiles = ChannelUtil.readInt(channel);
        if (numFiles < 0) {
            throw new IOException("bad snapshot file");
        }
        while (numFiles-- > 0) {
            String fileName = ChannelUtil.readString(channel);
            if (fileName == null) {
                throw new IOException("bad snapshot file");
            }
            if (!dirMgr.transferFromChannelToFile(channel, fileName)) {
                throw new IOException("bad snapshot file");
            }
        }
    }
}
