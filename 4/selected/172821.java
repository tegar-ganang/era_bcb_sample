package robocode.manager;

import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import robocode.dialog.*;
import robocode.battle.*;
import robocode.editor.*;
import robocode.packager.*;
import robocode.util.*;

/**
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen, Luis Crespo (current)
 */
public class WindowManager {

    private RobocodeEditor robocodeEditor;

    private RobotPackager robotPackager;

    private RobotExtractor robotExtractor;

    private RobocodeFrame robocodeFrame;

    private RobocodeManager manager;

    private TeamCreator teamCreator;

    private RankingDialog rankingDialog;

    public WindowManager(RobocodeManager manager) {
        this.manager = manager;
    }

    public void setRobocodeFrame(RobocodeFrame newRobocodeFrame) {
        robocodeFrame = newRobocodeFrame;
    }

    public RobocodeFrame getRobocodeFrame() {
        if (robocodeFrame == null) {
            robocodeFrame = new RobocodeFrame(manager);
        }
        return robocodeFrame;
    }

    public void showRobocodeFrame() {
        Utils.packCenterShow(getRobocodeFrame());
        robocodeFrame.setVisible(true);
        Utils.setStatusLabel(getRobocodeFrame().getStatusLabel());
    }

    public void showAboutBox() {
        AboutBox aboutBox = new AboutBox(robocodeFrame, manager);
        Utils.packCenterShow(robocodeFrame, aboutBox);
    }

    public void showBattleOpenDialog() {
        manager.getBattleManager().pauseBattle();
        JFileChooser chooser = new JFileChooser(manager.getBattleManager().getBattlePath());
        chooser.setFileFilter(new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return true;
                }
                String filename = pathname.getName();
                int idx = filename.lastIndexOf('.');
                String extension = "";
                if (idx >= 0) {
                    extension = filename.substring(idx);
                }
                if (extension.equalsIgnoreCase(".battle")) {
                    return true;
                }
                return false;
            }

            public String getDescription() {
                return "Battles";
            }
        });
        BattleManager battleManager = manager.getBattleManager();
        if (chooser.showOpenDialog(robocodeFrame) == JFileChooser.APPROVE_OPTION) {
            battleManager.setBattleFilename(chooser.getSelectedFile().getPath());
            battleManager.loadBattleProperties();
            showNewBattleDialog(battleManager.getBattleProperties());
        }
        battleManager.resumeBattle();
    }

    public void showVersionsTxt() {
        showInBrowser("file://" + new File(Constants.cwd(), "").getAbsoluteFile() + System.getProperty("file.separator") + "versions.txt");
    }

    public void showHelpApi() {
        showInBrowser("file://" + new File(Constants.cwd(), "").getAbsoluteFile() + System.getProperty("file.separator") + "javadoc" + System.getProperty("file.separator") + "index.html");
    }

    public void showFaq() {
        showInBrowser("http://robocode.sourceforge.net/help/robocode.faq.txt");
    }

    public void showOnlineHelp() {
        showInBrowser("http://robocode.sourceforge.net/help");
    }

    public void showRobocodeHome() {
        showInBrowser("http://robocode.sourceforge.net");
    }

    public void showRoboWiki() {
        showInBrowser("http://robowiki.net");
    }

    public void showYahooGroupRobocode() {
        showInBrowser("http://groups.yahoo.com/group/robocode");
    }

    public void showRobocodeRepository() {
        showInBrowser("http://robocoderepository.com");
    }

    public void showOptionsPreferences() {
        manager.getBattleManager().pauseBattle();
        PreferencesDialog preferencesDialog = new PreferencesDialog(manager);
        Utils.packCenterShow(robocodeFrame, preferencesDialog);
    }

    public void showResultsDialog(Battle battle) {
        ResultsDialog resultsDialog = new ResultsDialog(robocodeFrame, battle);
        resultsDialog.setSize(0, 0);
        Utils.packCenterShow(robocodeFrame, resultsDialog);
    }

    public void showRankingDialog(boolean visible) {
        if (rankingDialog == null) {
            rankingDialog = new RankingDialog(robocodeFrame, manager);
            rankingDialog.setSize(200, 300);
            Utils.centerShow(robocodeFrame, rankingDialog);
        }
        rankingDialog.setVisible(visible);
    }

    public void showRobocodeEditor() {
        if (robocodeEditor == null) {
            robocodeEditor = new robocode.editor.RobocodeEditor(manager);
            Utils.packCenterShow(robocodeEditor);
        } else {
            robocodeEditor.setVisible(true);
        }
    }

    public void showRobotPackager() {
        if (robotPackager != null) {
            robotPackager.dispose();
            robotPackager = null;
        }
        robotPackager = new robocode.packager.RobotPackager(manager.getRobotRepositoryManager(), false);
        Utils.packCenterShow(robotPackager);
    }

    public void showRobotExtractor(JFrame owner) {
        if (robotExtractor != null) {
            robotExtractor.dispose();
            robotExtractor = null;
        }
        robotExtractor = new robocode.dialog.RobotExtractor(owner, manager.getRobotRepositoryManager());
        Utils.packCenterShow(robotExtractor);
    }

    public void showSplashScreen() {
        SplashScreen splashScreen = new SplashScreen(manager);
        Utils.packCenterShow(splashScreen);
        for (int i = 0; i < 5 * 20 && !splashScreen.isPainted(); i++) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ie) {
            }
        }
        Utils.setStatusLabel(splashScreen.getSplashLabel());
        manager.getRobotRepositoryManager().getRobotRepository();
        Utils.setStatusLabel(splashScreen.getSplashLabel());
        manager.getImageManager();
        manager.getCpuManager().getCpuConstant();
        Utils.setStatus("");
        Utils.setStatusLabel(null);
        splashScreen.dispose();
    }

    public void showNewBattleDialog(BattleProperties battleProperties) {
        manager.getBattleManager().pauseBattle();
        NewBattleDialog newBattleDialog = new NewBattleDialog(manager, battleProperties);
        Utils.packCenterShow(robocodeFrame, newBattleDialog);
    }

    public boolean closeRobocodeEditor() {
        if (robocodeEditor == null) {
            return true;
        }
        if (!robocodeEditor.isVisible()) {
            return true;
        }
        return robocodeEditor.close();
    }

    /**
	 * Gets the manager.
	 * 
	 * @return Returns a RobocodeManager
	 */
    public RobocodeManager getManager() {
        return manager;
    }

    public void showCreateTeamDialog() {
        teamCreator = new robocode.dialog.TeamCreator(manager.getRobotRepositoryManager());
        Utils.packCenterShow(teamCreator);
    }

    public void showImportRobotDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.isHidden()) {
                    return false;
                }
                if (pathname.isDirectory()) {
                    return true;
                }
                String filename = pathname.getName();
                if (filename.equals("robocode.jar")) {
                    return false;
                }
                int idx = filename.lastIndexOf('.');
                String extension = "";
                if (idx >= 0) {
                    extension = filename.substring(idx);
                }
                if (extension.equalsIgnoreCase(".jar")) {
                    return true;
                }
                if (extension.equalsIgnoreCase(".zip")) {
                    return true;
                }
                return false;
            }

            public String getDescription() {
                return "Jar Files";
            }
        });
        chooser.setDialogTitle("Select the robot .jar file to copy to " + manager.getRobotRepositoryManager().getRobotsDirectory());
        if (chooser.showDialog(getRobocodeFrame(), "Import") == JFileChooser.APPROVE_OPTION) {
            File inputFile = chooser.getSelectedFile();
            String fileName = chooser.getSelectedFile().getName();
            int idx = fileName.lastIndexOf('.');
            String extension = "";
            if (idx >= 0) {
                extension = fileName.substring(idx);
            }
            if (!extension.equalsIgnoreCase(".jar")) {
                fileName += ".jar";
            }
            File outputFile = new File(manager.getRobotRepositoryManager().getRobotsDirectory(), fileName);
            if (inputFile.equals(outputFile)) {
                JOptionPane.showMessageDialog(getRobocodeFrame(), outputFile.getName() + " is already in the robots directory!");
                return;
            }
            if (outputFile.exists()) {
                if (JOptionPane.showConfirmDialog(getRobocodeFrame(), outputFile + " already exists.  Overwrite?", "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            if (JOptionPane.showConfirmDialog(getRobocodeFrame(), "Robocode will now copy " + inputFile.getName() + " to " + outputFile.getParent(), "Import robot", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                try {
                    Utils.copy(inputFile, outputFile);
                    manager.getRobotRepositoryManager().clearRobotList();
                    JOptionPane.showMessageDialog(getRobocodeFrame(), "Robot imported successfully.");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(getRobocodeFrame(), "Import failed: " + e);
                }
            }
        }
    }

    /**
	 * Shows a web page using the browser manager.
	 * 
	 * @param url
	 *        The URL of the web page
	 */
    private void showInBrowser(String url) {
        try {
            BrowserManager.openURL(url);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(robocodeFrame, e.getMessage(), "Unable to open browser!", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showSaveResultsDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.isHidden()) {
                    return false;
                }
                if (pathname.isDirectory()) {
                    return true;
                }
                String filename = pathname.getName();
                int idx = filename.lastIndexOf('.');
                String extension = "";
                if (idx >= 0) {
                    extension = filename.substring(idx);
                }
                if (extension.equalsIgnoreCase(".csv")) {
                    return true;
                }
                return false;
            }

            public String getDescription() {
                return "Comma Separated Value (CSV) File Format";
            }
        });
        chooser.setDialogTitle("Save battle results");
        if (chooser.showSaveDialog(getRobocodeFrame()) == JFileChooser.APPROVE_OPTION) {
            BattleResultsTableModel tableModel = new BattleResultsTableModel(manager.getBattleManager().getBattle());
            String filename = chooser.getSelectedFile().getPath();
            if (!filename.endsWith(".csv")) {
                filename += ".csv";
            }
            boolean append = manager.getProperties().getOptionsCommonAppendWhenSavingResults();
            tableModel.saveToFile(filename, append);
        }
    }
}
