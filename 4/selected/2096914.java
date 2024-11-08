package buttress.rss.saver;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.util.Vector;
import javax.swing.JOptionPane;
import buttress.gui.ConsoleDialog;
import buttress.gui.StatusBar;
import buttress.main.Buttress;
import buttress.rss.Rss;

/**
 * Run loadRsses() and it'll try to load all the Rsses from the save file and
 * return them in a vector.  If the Rss signature has changed, it'll try to
 * recover the Rss name and url, but it'll lose all other data.
 * 
 * @author Nick Daly
 * @version 0.0.0.0
 * <DT><B>
 * Date:
 * </B></DT><DD>
 * YY-MM-DD - Jan 13, 2005; 11:55:23 AM
 * </DD>
 */
public class RssLoader implements RssFileMaker {

    private boolean updateClassChanged = false;

    private Vector backupVersions = new Vector();

    private Vector versionsAvailable = new Vector();

    /**
	 * tries to read the save file and load the rsses.
	 * @return a vector of Rsses, or null if it failed for some reason.
	 */
    public static Vector loadRsses() {
        return new RssLoader().load();
    }

    public Vector load() {
        ObjectInputStream ourBuffer;
        FileInputStream ourStream = null;
        try {
            ourStream = new FileInputStream(SAVE_FILE_NAME);
        } catch (FileNotFoundException exc) {
            return null;
        }
        StatusBar state = Buttress.getButtress().getMyGui().status;
        state.setMessage("Loading Saved Feeds and Filters...");
        state.setValue(1, 3);
        try {
            ourBuffer = new ObjectInputStream(ourStream);
        } catch (Exception exc) {
            ConsoleDialog.writeError("Couldn't create the save file input " + "stream.", exc);
            return null;
        }
        state.setMessage("Reading Save File...");
        state.setValue(2, 3);
        try {
            Object loaded;
            while (true) {
                loaded = ourBuffer.readObject();
                if (loaded instanceof String) {
                    backupVersions.add(loaded.toString());
                } else if (loaded instanceof Rss) {
                    versionsAvailable.add(loaded);
                }
            }
        } catch (FileNotFoundException exc) {
            ConsoleDialog.writeError("The " + SAVE_FILE_NAME + " file was not found.", exc);
            return null;
        } catch (EOFException exc) {
        } catch (ClassNotFoundException exc) {
            ConsoleDialog.writeError("The " + SAVE_FILE_NAME + "file was written incorrectly.", exc);
            return null;
        } catch (InvalidClassException exc) {
            updateClassModified(exc);
        } catch (IOException exc) {
            ConsoleDialog.writeError("There was an error  while reading the " + "save file (" + exc.getLocalizedMessage() + ").", exc);
        }
        state.setMessage("Loading Saved Feeds and Filters...");
        state.setValue(3, 3);
        if (updateClassChanged) {
            return extractRssData(backupVersions);
        } else {
            return versionsAvailable;
        }
    }

    /**
	 * It'll try to extract the Rss data from the vector of strings of titles
	 * and urls...  If it encounters a bad URL, it'll remove it.
	 * 
	 * @param badRssData
	 * @return
	 */
    private Vector extractRssData(Vector badRssData) {
        Vector foundRsses = new Vector();
        for (int i = 0; i < badRssData.size() / 2; i++) {
            try {
                foundRsses.add(i, new Rss(badRssData.get(i * 2).toString(), badRssData.get(i * 2 + 1).toString()));
            } catch (MalformedURLException exc) {
                foundRsses.remove(i);
                ConsoleDialog.writeError("RSS " + badRssData.get(i * 2).toString() + " (" + badRssData.get(i * 2 + 1).toString() + ") had a bad URL and was not recovered.", exc);
            }
        }
        return foundRsses;
    }

    /**
	 * When an class-reading type exception is thrown when reading the file, 
	 * this method is run, showing a correct error and informing the program 
	 * that the update file structure is (as far as the program can tell) 
	 * invalid. 
	 * 
	 * @param exc the exception to write on the console
	 */
    private void updateClassModified(InvalidClassException exc) {
        ConsoleDialog.writeError("The " + SAVE_FILE_NAME + " is unreadable " + "by this version of Buttress, your Feeds may be recovered, " + "but your Filters are lost.", exc);
        JOptionPane.showMessageDialog(null, "The " + SAVE_FILE_NAME + " file is unreadable by this " + "version of" + "\nButtress, your Feeds may be recovered, but your Filters" + "\nare lost." + "\n\nIf you have a version of Buttress that the " + SAVE_FILE_NAME + "\nfile worked with, open that version of Buttress, copy out " + "\nyour filters now, and quit it, before you close this version " + "\nof Buttress." + "\n\nWhen Buttress closes, it will overwrite your old save file " + "to " + "\nthe new version.", "Cannot Read Save File", JOptionPane.WARNING_MESSAGE);
        updateClassChanged = true;
    }
}
