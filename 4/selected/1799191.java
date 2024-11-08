package nhap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Replay {

    private static final Logger logger = Logger.getLogger(Replay.class);

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        PropertyConfigurator.configure(NhaPlayer.class.getResource("/log4j.properties"));
        final String playerName = "Beer30";
        final String backupDirectory = "D:/games/play/";
        final File file = new File(backupDirectory + "Administrator-" + playerName + ".NetHack-saved-game");
        final File backup = new File(backupDirectory + "Administrator-" + playerName + ".NetHack-saved-game.bak");
        if (file.exists()) {
            try {
                copy(file, backup);
            } catch (IOException exception) {
                logger.error("Error while copying file to backup", exception);
                System.exit(1);
            }
        } else if (backup.exists()) {
            try {
                copy(backup, file);
            } catch (IOException exception) {
                logger.error("Error while restoring backup", exception);
                System.exit(1);
            }
        }
        new NhaUmpire().playGame(NhaPlayer.getNhapParams(playerName));
        System.exit(0);
    }

    private static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        IOUtils.copy(in, out);
    }
}
