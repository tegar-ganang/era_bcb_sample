package edu.psu.its.lionshare.gui.search.results;

import com.limegroup.gnutella.downloader.FileExistsException;
import edu.psu.its.lionshare.database.Event;
import edu.psu.its.lionshare.search.SearchResult;
import edu.psu.its.lionshare.LionShareDataTransferStatus;
import edu.psu.its.lionshare.gui.events.EventUpdateTimer;
import edu.psu.its.lionshare.gui.events.FileTransferEventsMediator;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.settings.SharingSettings;
import edu.psu.its.lionshare.database.FileSelect;
import java.io.File;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SearchResultRunnableDownload implements Runnable {

    private static final Log LOG = LogFactory.getLog(SearchResultRunnableDownload.class);

    SearchResult[] results = null;

    public SearchResultRunnableDownload(SearchResult[] results) {
        this.results = results;
    }

    private void downloadSearchResult(boolean overWrite) {
        try {
            LionShareDataTransferStatus status = (LionShareDataTransferStatus) results[0].download(results, overWrite);
            java.io.File file = status.getFile();
            String path = "";
            if (file != null) {
                path = file.getAbsolutePath();
            }
            LOG.debug("PATH: " + path);
            Event event = new Event(results[0].getFilename(), "Download file from Gnutella Network", new Long(0), path, System.currentTimeMillis(), status);
            FileTransferEventsMediator.getInstance().showEventsWindow();
            EventUpdateTimer timer = new EventUpdateTimer(status, event);
            FileTransferEventsMediator.getInstance().addEvent(event, timer);
            timer.startTimer();
        } catch (FileExistsException feex) {
            int nChoice = JOptionPane.showConfirmDialog(GUIMediator.getAppFrame(), "<html>The file already exists in your shared directory, would<br>" + "you like to overwrite the existing file with this one?</html>", "File Already Exists", JOptionPane.YES_NO_OPTION);
            if (nChoice == JOptionPane.YES_OPTION) {
                downloadSearchResult(true);
            }
        } catch (Exception e) {
            Runnable run = new Runnable() {

                public void run() {
                    JOptionPane.showMessageDialog(GUIMediator.getAppFrame(), "There was an error downloading the file, file may " + "already be downloading ", "Download Error", JOptionPane.ERROR_MESSAGE);
                }
            };
            try {
                SwingUtilities.invokeLater(run);
            } catch (Exception eswing) {
            }
        }
    }

    public void run() {
        downloadSearchResult(false);
    }
}
