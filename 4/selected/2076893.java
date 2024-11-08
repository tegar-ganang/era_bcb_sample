package net.sf.ipodxtract.logic;

import net.sf.ipodxtract.ui.*;
import java.io.*;
import java.awt.event.WindowEvent;

/** This class is the controller for the main window.
 * It contains the logic for synchronizing the state of the app
 * with whatever is being displayed in the GUI.
 * 
 * @author Enrique Zamudio
 */
public class Controller extends java.awt.event.WindowAdapter {

    public static final int COPY_BLOCKSIZE = 262144;

    private MainWindow window;

    private File root;

    private SearchThread searchThread;

    public Controller(File rootFolder) {
        root = rootFolder;
        window = new MainWindow(this);
        window.show();
        window.addWindowListener(this);
    }

    public void beginSearch(String title, String artist, String album, String year, String genre, boolean all) {
        searchThread = new SearchThread(this, root);
        searchThread.setSearchParameters(artist, album, title, year, genre, all);
        searchThread.start();
    }

    public void stopSearch() {
        if (searchThread != null) {
            searchThread.stopSearching();
            searchThread = null;
        }
        window.stopSearch();
    }

    /** Adds a song to the list of songs that have been found. */
    public synchronized void songFound(Song song) {
        window.addSong(song);
    }

    public void windowClosing(WindowEvent ev) {
        if (searchThread != null) {
            stopSearch();
        }
    }

    /** Extracts a single song out to a directory selected by the user. */
    public void extractSong(Song s) {
        File dir = Dialogs.selectDirectory();
        if (dir == null) return;
        extractSong(s, dir);
    }

    /** Extracts a song to the specified directory. */
    public void extractSong(Song s, File dir) {
        FileInputStream fin = null;
        FileOutputStream fout = null;
        File dest = new File(dir, s.file.getName());
        if (dest.equals(s.file)) return;
        byte[] buf = new byte[COPY_BLOCKSIZE];
        try {
            fin = new FileInputStream(s.file);
            fout = new FileOutputStream(dest);
            int read = 0;
            do {
                read = fin.read(buf);
                if (read > 0) fout.write(buf, 0, read);
            } while (read > 0);
        } catch (IOException ex) {
            ex.printStackTrace();
            Dialogs.showErrorDialog("xtract.error");
        } finally {
            try {
                fin.close();
                fout.close();
            } catch (Exception ex) {
            }
        }
    }

    /** Extracts the specified songs out to a directory selected by the user. */
    public void extractSongs(Song[] songs) {
        File dir = Dialogs.selectDirectory();
        if (dir == null) return;
        for (int i = 0; i < songs.length; i++) {
            extractSong(songs[i], dir);
        }
        Dialogs.showExtractionSuccess();
    }

    /** Displays the specified percentage of progress in the progress bar. */
    public synchronized void displayProgress(int percent) {
        window.setProgress(percent);
    }

    public synchronized void displaySearchedFiles(int total) {
        window.displayFilesSearched(total);
    }
}
