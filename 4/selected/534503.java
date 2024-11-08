package org.magnesia.client.gui.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import org.magnesia.OPCodes;
import org.magnesia.client.gui.ClientConnection.ProgressListener;

public class Downloader extends LongTermTask {

    private int i;

    private int transferred;

    private int toRead;

    private Collection<String> images;

    private ProgressListener pl;

    private String path;

    public Downloader(LongTermManager ltm, String path, Collection<String> images, ProgressListener pl) {
        super(ltm);
        this.images = images;
        this.pl = pl;
        this.path = path;
    }

    public int getStatus() {
        return transferred;
    }

    @Override
    public int getLength() {
        return toRead;
    }

    @Override
    public String getActiveText() {
        return "Downloading image " + i + " of " + images.size();
    }

    @Override
    public String getDescription() {
        return "Downloading images";
    }

    public void run() {
        try {
            ltm.lockConnection();
            ltm.getStream().writeOpCode(OPCodes.GET_IMAGES);
            ltm.getStream().readString();
            ltm.getStream().writeInt(images.size());
            for (String s : images) {
                ltm.getStream().writeString(s);
                if (pl != null) running = pl.toRead(toRead);
                ltm.getStream().writeBoolean(running);
                if (!running) break;
                toRead = ltm.getStream().readInt();
                byte buf[] = new byte[org.magnesia.Constants.CHUNK_SIZE];
                int read = 0;
                if (pl != null) pl.currentRead(0);
                int size = toRead;
                File out = new File(path + "/" + s.substring(s.lastIndexOf("/") + 1));
                FileOutputStream fos = new FileOutputStream(out);
                while (read >= 0 && toRead > 0) {
                    read = ltm.getStream().readData(buf, ((toRead >= buf.length) ? buf.length : toRead));
                    toRead -= read;
                    fos.write(buf, 0, read);
                    transferred = size - toRead;
                    if (pl != null) pl.currentRead(size - toRead);
                }
                fos.flush();
                fos.close();
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ltm.unlockConnection();
            running = false;
            finished = true;
        }
    }
}
