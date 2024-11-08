package sf2.vm.impl.emu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import sf2.log.Logging;
import sf2.vm.VMException;
import sf2.vm.VMImage;
import sf2.vm.VMSnapShot;

public class EmuVMSnapShot implements VMSnapShot, EmuCommon {

    protected Logging logging = Logging.getInstance();

    protected String path;

    public void load(String path) throws VMException {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public boolean isLoaded() {
        return path != null;
    }

    public void merge(VMImage image, VMSnapShot another) throws VMException {
        if (path == null || another.getPath() == null) throw new VMException("EmuVMSnapShot is NULL!");
        logging.debug(LOG_NAME, "merge images  " + path + " and " + another.getPath());
        File target = new File(path);
        File src = new File(another.getPath());
        if (target.isDirectory() || src.isDirectory()) return;
        try {
            FileInputStream in = new FileInputStream(another.getPath());
            FileChannel inChannel = in.getChannel();
            FileOutputStream out = new FileOutputStream(path, true);
            FileChannel outChannel = out.getChannel();
            outChannel.transferFrom(inChannel, 0, inChannel.size());
            outChannel.close();
            inChannel.close();
        } catch (IOException e) {
            throw new VMException(e);
        }
    }

    public void remove() {
        logging.debug(LOG_NAME, "remove image  " + path);
        if (path != null) {
            File target = new File(path);
            if (target.exists()) target.delete();
        }
    }
}
