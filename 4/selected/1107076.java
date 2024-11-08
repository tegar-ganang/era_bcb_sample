package bluej;

import bluej.extensions.BlueJ;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import util.Sysutil;

/**
 *
 * @author jmadar
 */
public class OnlineExampleAction extends AbstractAction {

    private static BlueJ bluej;

    private BEnv env;

    private static Preferences prefs;

    private String exampleDir;

    public OnlineExampleAction(String menuName, String msg) {
        putValue(AbstractAction.NAME, menuName);
        exampleDir = msg;
    }

    /**
     * @return the bluej
     */
    public static BlueJ getBluej() {
        return bluej;
    }

    /**
     * @param aBluej the bluej to set
     */
    public static void setBluej(BlueJ aBluej) {
        bluej = aBluej;
    }

    /**
     * @return the prefs
     */
    public static Preferences getPrefs() {
        return prefs;
    }

    /**
     * @param prefs the prefs to set
     */
    public static void setPrefs(Preferences myPrefs) {
        prefs = myPrefs;
    }

    public void actionPerformed(ActionEvent ae) {
        try {
            String exampleUrl = prefs.getServerUrl() + "/examples/?dir=" + exampleDir;
            String[] files = Sysutil.readUrl(exampleUrl).split("\n");
            for (String file : files) {
                File dir = bluej.getCurrentPackage().getDir();
                String fileName = dir.getAbsolutePath() + File.separator + file;
                createFile(file.substring(0, file.indexOf(".")), fileName, prefs.getServerUrl() + "/examples/" + exampleDir + "/" + file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a file from the fileURL
     *
     * @param fileName
     * @param fileURL
     */
    public void createFile(String className, String fileName, String fileURL) throws Exception {
        System.out.println("Creating " + fileName);
        System.out.println("    from " + fileURL);
        File outFile = new File(fileName);
        if (!outFile.exists()) {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
            bw.write(Sysutil.readUrl(fileURL));
            bw.close();
            bluej.getCurrentPackage().newClass(className);
        } else {
            throw new Exception("File already exists");
        }
    }
}
