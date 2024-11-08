package com.tscribble.bitleech.core.io;

import static com.tscribble.bitleech.core.download.State.BUILDING;
import static com.tscribble.bitleech.core.download.State.COMPLETED;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.tscribble.bitleech.core.download.State;

public class PartBuilder {

    /**
	 * Logger for this class
	 */
    private static final Logger log = Logger.getLogger("PartBuilder");

    private long progress;

    private long written;

    private long length;

    private static File dir;

    private String prefix;

    private File dest;

    private List<File> parts;

    private State state;

    public PartBuilder() {
    }

    public void setTmpDir(File tmp) {
        dir = tmp;
    }

    public void setTmpDir(String tmp) {
        dir = new File(tmp);
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setDestFile(String file) {
        dest = new File(file);
    }

    public void setDestFile(File file) {
        dest = file;
    }

    public void build() {
        parts = getParts();
        streamBuild();
        setState(dest.getName(), COMPLETED);
        deleteParts();
    }

    private List<File> getParts() {
        int partCount = getPieceCount(dir, prefix);
        parts = new ArrayList<File>(partCount);
        for (int i = 0; i < partCount; i++) {
            File part = new File(dir, prefix + "." + i);
            length += part.length();
            parts.add(part);
        }
        return parts;
    }

    private void deleteParts() {
        for (File part : parts) {
            part.deleteOnExit();
        }
    }

    private int getPieceCount(File srcDir, final String prefix) {
        return srcDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith(prefix);
            }
        }).length;
    }

    private void nioBuild() {
        try {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 4);
            final FileChannel out = new FileOutputStream(dest).getChannel();
            for (File part : parts) {
                setState(part.getName(), BUILDING);
                FileChannel in = new FileInputStream(part).getChannel();
                while (in.read(buffer) > 0) {
                    buffer.flip();
                    written += out.write(buffer);
                    buffer.clear();
                }
                in.close();
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void streamBuild() {
        try {
            final OutputStream out = new FileOutputStream(dest);
            final byte[] buff = new byte[1024 * 4];
            final int len = buff.length;
            for (File part : parts) {
                setState(part.getName(), BUILDING);
                InputStream in = new FileInputStream(part);
                int count = 0;
                while ((count = in.read(buff, 0, len)) > 0) {
                    out.write(buff, 0, count);
                    written += count;
                }
                in.close();
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addBuilderListener(IPartBuilderListener l) {
    }

    public long getProgress() {
        progress = (length < 1) ? 0 : (written * 100) / length;
        return progress;
    }

    synchronized void setState(String txt, State st) {
        state = st;
        log.debug(txt + ": " + state);
    }
}
