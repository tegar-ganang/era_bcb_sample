package util;

import gui.Menu;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import common.User;

/**
 * @author Jamal Bullock
 * 
 */
public class ManageUsers {

    private File dataFile = null;

    private File cfgFile = new File("MyRx.cfg");

    private FileInputStream inDataFile;

    private static FileOutputStream outDataFile;

    private ObjectInputStream inDataFileStream;

    private ObjectOutputStream outDataFileStream;

    private static final Logger logger = Logger.getLogger(ManageUsers.class);

    /**
	 * 
	 */
    public ManageUsers() {
        if (System.getProperty("user.home") != null) {
            dataFile = new File(System.getProperty("user.home") + File.separator + "MyRx" + File.separator + "MyRx.dat");
            File dataFileDir = new File(System.getProperty("user.home") + File.separator + "MyRx");
            dataFileDir.mkdirs();
        } else {
            dataFile = new File("MyRx.dat");
        }
        try {
            dataFile.createNewFile();
        } catch (IOException e1) {
            logger.error(e1);
            JOptionPane.showMessageDialog(Menu.getMainMenu(), e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        File oldDataFile = new File("MyRx.dat");
        if (oldDataFile.exists()) {
            FileChannel src = null, dst = null;
            try {
                src = new FileInputStream(oldDataFile.getAbsolutePath()).getChannel();
                dst = new FileOutputStream(dataFile.getAbsolutePath()).getChannel();
                dst.transferFrom(src, 0, src.size());
                if (!oldDataFile.delete()) {
                    oldDataFile.deleteOnExit();
                }
            } catch (FileNotFoundException e) {
                logger.error(e);
                JOptionPane.showMessageDialog(Menu.getMainMenu(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                logger.error(e);
                JOptionPane.showMessageDialog(Menu.getMainMenu(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    src.close();
                    dst.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }

    /**
	 * @param users
	 */
    public void saveUsers(LinkedList<?> users) {
        logger.info("users:" + users);
        try {
            outDataFile = new FileOutputStream(dataFile.getAbsolutePath());
            outDataFileStream = new ObjectOutputStream(outDataFile);
            outDataFileStream.writeObject(users);
            outDataFileStream.flush();
            outDataFileStream.close();
        } catch (IOException ioe) {
            logger.error(ioe);
            JOptionPane.showMessageDialog(Menu.getMainMenu(), ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void saveConfig(boolean showWelcome) {
        PrefUtil.prefs.putBoolean(PrefUtil.SHOW_WELCOME, showWelcome);
    }

    /**
	 * @return
	 */
    public boolean loadConfig() {
        boolean b = false;
        cfgFile.deleteOnExit();
        b = PrefUtil.prefs.getBoolean(PrefUtil.SHOW_WELCOME, true);
        return (b);
    }

    /**
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public LinkedList<User> loadUsers() {
        LinkedList<User> loaded = new LinkedList<User>();
        if (dataFile.exists() && dataFile.length() != 0) {
            logger.info("MyRx.dat size=" + dataFile.length());
            try {
                inDataFile = new FileInputStream(dataFile.getAbsolutePath());
                inDataFileStream = new ObjectInputStream(inDataFile);
                Object obj = inDataFileStream.readObject();
                if (logger.isDebugEnabled()) {
                    logger.debug("readObject:" + obj);
                }
                while (obj != null) {
                    if (obj instanceof LinkedList) {
                        loaded = (LinkedList<User>) obj;
                    }
                    obj = inDataFileStream.readObject();
                    if (logger.isDebugEnabled()) {
                        logger.debug("readObject:" + obj);
                    }
                }
                inDataFileStream.close();
            } catch (IOException ioe) {
                logger.error(ioe.getMessage(), ioe);
            } catch (Exception cne) {
                logger.error(cne.getMessage(), cne);
            }
        }
        logger.info("users:" + loaded);
        return (loaded);
    }
}
