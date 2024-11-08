package boccaccio.andrea.myfilesutils.copier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * @author Andrea Boccaccio
 *
 */
public abstract class AbsFileCopier implements IFileCopier {

    private String algorithm;

    protected String getAlgorithm() {
        return algorithm;
    }

    protected void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void fileCopy(String in, File out) throws IOException {
        this.fileCopy(new File(in), out);
    }

    public void fileCopy(File in, String out) throws IOException {
        this.fileCopy(in, new File(out));
    }

    public void fileCopy(String in, String out) throws IOException {
        this.fileCopy(new File(in), new File(out));
    }

    protected void onlyFileCopy(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            int maxCount = (1024 * 1024 * 64) - (1024 * 32);
            long size = inChannel.size();
            long pos = 0;
            while (pos < size) {
                pos += inChannel.transferTo(pos, maxCount, outChannel);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    public abstract void fileCopy(File in, File out) throws IOException;
}
