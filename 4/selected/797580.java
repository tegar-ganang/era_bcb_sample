package archive.playground.david.vis;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.matsim.interfaces.networks.basicNet.BasicNet;
import org.matsim.utils.vis.netvis.streaming.StateI;
import org.matsim.utils.vis.netvis.visNet.DisplayNetStateReader;

public class DisplayCachedNetStateReader extends DisplayNetStateReader {

    public ByteBuffer bb = null;

    public DisplayCachedNetStateReader(BasicNet network, String filePrefix) {
        super(network, filePrefix);
    }

    private long endPos = 0;

    private FileChannel fc = null;

    public void updateBuffer(StateI target) throws IOException {
        for (int i = 0; i <= buffer.length; i++) {
            DisplayCachedNetState dbuffer = (DisplayCachedNetState) buffer[i];
            if ((dbuffer.pos == -1) && bb.hasRemaining()) dbuffer.readMyselfBB(bb);
            if (dbuffer == target) break;
        }
    }

    @Override
    protected void loadBuffer() throws IOException {
        if (buffer == null) throw new NullPointerException("Buffer is null, has reader been opened?");
        String fileName = streamConfig.getStreamFileName(bufferStartTime_s);
        FileInputStream fis = new FileInputStream(fileName);
        fc = fis.getChannel();
        endPos = fc.size();
        bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, endPos);
        for (int i = 0; i < buffer.length; i++) {
            DisplayCachedNetState dbuffer = (DisplayCachedNetState) buffer[i];
            dbuffer.pos = -1;
            dbuffer.myReader = this;
        }
        buffer[0].setState();
    }

    @Override
    protected StateI newState() {
        return new DisplayCachedNetState(getIndexConfig());
    }
}
