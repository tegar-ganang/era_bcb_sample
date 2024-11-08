package l2connectionmanager;

import java.nio.channels.FileChannel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.swing.*;

public class DirectoryCopier extends SwingWorker<Void, Void> {

    @Override
    public Void doInBackground() throws Exception {
        deleteDirectory(new File(Settings.getPrivatePath()));
        if (Installer.isFirstInstallationAttempt()) {
            String commandCall = null;
            if (Settings.getOS() == 2) {
                commandCall = "cmd.exe /c ";
            } else {
                commandCall = "command.com /C ";
            }
            String command = commandCall + "Xcopy \"" + Settings.getRetailPath() + "\" \"" + Settings.getPrivatePath() + "\" /E /H /I /K /Y";
            System.out.println("The following command was issued to run the installation:");
            System.out.println(command + "\n");
            L2ConnectionManager.debugWriter.write("The following command was issued to run the installation:");
            L2ConnectionManager.debugWriter.newLine();
            L2ConnectionManager.debugWriter.write(command);
            L2ConnectionManager.debugWriter.newLine();
            L2ConnectionManager.debugWriter.flush();
            Runtime.getRuntime().exec(command);
        } else {
            try {
                copyFiles(Settings.getRetailPath(), Settings.getPrivatePath());
            } catch (Exception directoryCopyFailed) {
                Dialogs.showUserMessage(7, "Error", 0);
                System.out.println("Error in DirectoryCopier.run(); " + directoryCopyFailed + "\n");
                L2ConnectionManager.debugWriter.writeError("Error in DirectoryCopier.run();", directoryCopyFailed);
                System.exit(0);
            }
        }
        return null;
    }

    @Override
    public void done() {
    }

    private static void copyFiles(String strPath, String dstPath) throws Exception {
        File src = new File(strPath);
        File dest = new File(dstPath);
        if (src.isDirectory()) {
            dest.mkdirs();
            String list[] = src.list();
            for (int i = 0; i < list.length; i++) {
                String dest1 = dest.getAbsolutePath() + "\\" + list[i];
                String src1 = src.getAbsolutePath() + "\\" + list[i];
                copyFiles(src1, dest1);
            }
        } else {
            FileChannel sourceChannel = new FileInputStream(src).getChannel();
            FileChannel targetChannel = new FileOutputStream(dest).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
            sourceChannel.close();
            targetChannel.close();
        }
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }
}
