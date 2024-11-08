package Application;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.Preferences;
import images.ImageLibrary;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import presentation.PresentationSetLibrary;
import songs.*;
import ui.MainWindow;

public class OpenSongJ {

    static Preferences pref_root = null;

    public static Preferences getPrefs() {
        return pref_root;
    }

    void extractDll(String name) {
        InputStream inputStream = OpenSongJ.class.getResourceAsStream(name);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(name);
            byte[] array = new byte[8192];
            for (int i = inputStream.read(array); i != -1; i = inputStream.read(array)) outputStream.write(array, 0, i);
            outputStream.close();
        } catch (FileNotFoundException e) {
            System.err.println("Failed to export dll " + name);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Failed to export dll " + name);
            e.printStackTrace();
        }
    }

    /**
	 * @param args
	 */
    public OpenSongJ() {
        pref_root = Preferences.userNodeForPackage(this.getClass());
        String song_ll = getPrefs().get("song_library_location", null);
        String set_ll = getPrefs().get("set_library_location", null);
        String image_ll = getPrefs().get("image_library_location", null);
        if (song_ll == null) {
            song_ll = System.getProperty("user.home") + "\\songs";
            getPrefs().put("song_library_location", song_ll);
            System.out.println("Storing new song library location at " + song_ll);
        }
        if (set_ll == null) {
            set_ll = System.getProperty("user.home") + "\\sets";
            getPrefs().put("set_library_location", set_ll);
            System.out.println("Storing new sets library location at " + set_ll);
        }
        if (image_ll == null) {
            image_ll = System.getProperty("user.home") + "\\backgrounds";
            getPrefs().put("image_library_location", image_ll);
            System.out.println("Storing new backgrounds library location at " + image_ll);
        }
        SongLibrary.importLibraryFromDirectory(song_ll);
        PresentationSetLibrary.importLibraryFromDirectory(set_ll);
        ImageLibrary.importLibraryFromDirectory(image_ll);
        new MainWindow();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException e) {
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        new OpenSongJ();
    }
}
