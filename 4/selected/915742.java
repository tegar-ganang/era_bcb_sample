package edu.whitman.halfway.jigs;

import edu.whitman.halfway.util.*;
import org.apache.log4j.*;
import java.io.*;

public class ExtractorThread extends Thread implements MediaItemFunction {

    private Logger log = Logger.getLogger(ExtractorThread.class.getName());

    private Album root;

    private boolean overwrite;

    private boolean keepSearching;

    private InfoStatusInterface isi;

    public ExtractorThread(InfoStatusInterface parent, Album alb, boolean owrite) {
        isi = parent;
        root = alb;
        overwrite = owrite;
        keepSearching = true;
    }

    public ExtractorThread(InfoStatusInterface parent, File f, boolean owrite) {
        isi = parent;
        root = new BasicAlbum(f);
        overwrite = owrite;
        keepSearching = true;
    }

    public void kill() {
        keepSearching = false;
    }

    public void run() {
        isi.setTotalNumTasks(AlbumUtil.getNumPicsRecursive(root));
        try {
            AlbumUtil.recursiveMediaItemWalk(root, this);
            isi.allTasksCompleted();
        } catch (StopProcessException e) {
        }
    }

    public void process(MediaItem mi) {
        if (!keepSearching) {
            throw new StopProcessException();
        }
        if (mi instanceof Picture) {
            Picture pic = (Picture) mi;
            isi.setTaskStatus("Processing " + pic.getName());
            log.debug("Processing " + pic.getName());
            PictureDescriptionInfo pdi = pic.getPictureDescriptionInfo();
            pdi.readExifData(overwrite);
            isi.anotherTaskCompleted();
            pdi.saveFile();
        }
    }
}

/** Exception to throw if the user requests that we interrupt
 * processing.  XXX Extends Error because process(MediaItem mi)
 * doesn't throw any exceptions, and we need to still implement that
 * interface.*/
class StopProcessException extends Error {
}
