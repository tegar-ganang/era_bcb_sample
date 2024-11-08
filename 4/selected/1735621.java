package br.com.visualmidia.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import br.com.visualmidia.core.Constants;
import br.com.visualmidia.core.ServerAdress;
import br.com.visualmidia.ui.MainScreen;
import br.com.visualmidia.ui.Server;
import br.com.visualmidia.ui.UpdateUI;
import br.com.visualmidia.ui.splash.SplashScreen;
import br.com.visualmidia.update.UpdateSetup;

public class GDStarter {

    private static Shell fakeShell;

    private static Display fakeDisplay;

    public static void main(String[] args) {
        fakeDisplay = new Display();
        if (args.length > 0) {
            if (isCommand(args, "-server")) {
                if (isServerAlreadyOpen()) {
                    GDSystem.setServerMode();
                    new Server().run();
                    if (isCommand(args, "-console")) {
                        new GDConsole().start();
                    }
                } else {
                    MessageDialog.openError(new Shell(), "Gerente Digital Server!", "J� existe um Gerente Digital Server aberto.");
                }
            } else if (isCommand(args, "-update")) {
                GDSystem.setStandAloneMode();
                new UpdateUI(null);
            } else if (isCommand(args, "-setup")) {
                GDSystem.setStandAloneMode();
                new GDSetup().start();
            } else if (isCommand(args, "-client")) {
                startGDClientMode();
            } else if (isCommand(args, "-standalone")) {
                startGDStandAloneMode();
            }
        } else {
            MessageDialog.openError(new Shell(), "Gerente Digital", "Parametro incorreto.");
        }
    }

    private static void startGDClientMode() {
        File file = new File(Constants.CURRENT_DIR + "server");
        String serverIp = "";
        if (file.exists()) {
            try {
                FileInputStream fin = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fin);
                serverIp = (String) ois.readObject();
                ois.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        GDSystem.setServerIp(serverIp);
        File directory = new File(Constants.PREVAYLER_DATA_DIRETORY);
        File fileExist = null;
        for (String fileStr : directory.list()) {
            if (fileStr.contains(".snapshot")) {
                fileExist = new File(Constants.PREVAYLER_DATA_DIRETORY + Constants.FILE_SEPARATOR + fileStr);
            }
        }
        if (fileExist == null) {
            new ContactServerLocalAndCheckSnapshot(ServerAdress.getServerAdress());
        } else {
            File fileserver = new File(Constants.CURRENT_DIR + ".server");
            if (!fileserver.exists()) {
                new ContactServerLocalAndCheckUpdates(ServerAdress.getServerAdress());
                new ContactServerLocalAndCheckLogo();
            }
        }
        if (isGdOpen()) {
            fakeShell = new Shell(fakeDisplay);
            fakeLoadSystem(fakeShell);
            new UpdateSetup();
            Thread checkServerIsAvailable = new Thread(new CheckServerIsAvailable());
            checkServerIsAvailable.start();
            new MainScreen().run();
        } else {
            MessageDialog.openError(new Shell(), "Gerente Digital", "J� existe um Gerente Digital aberto.");
        }
    }

    private static void startGDStandAloneMode() {
        if (isGdOpen()) {
            fakeShell = new Shell(fakeDisplay);
            fakeLoadSystem(fakeShell);
            GDSystem.setStandAloneMode();
            new UpdateSetup();
            new MainScreen().run();
        } else {
            MessageDialog.openError(new Shell(), "Gerente Digital", "J� existe um Gerente Digital aberto.");
        }
    }

    private static boolean isGdOpen() {
        File file = new File(Constants.CURRENT_DIR + ".gd");
        file.delete();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e1) {
            }
            try {
                FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
                try {
                    channel.tryLock();
                } catch (OverlappingFileLockException e) {
                }
            } catch (Exception e) {
            }
            return true;
        }
        return false;
    }

    public static boolean isServerAlreadyOpen() {
        File file = new File(Constants.CURRENT_DIR + ".server");
        file.delete();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e1) {
            }
            try {
                FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
                try {
                    channel.tryLock();
                } catch (OverlappingFileLockException e) {
                }
            } catch (Exception e) {
            }
            return true;
        }
        return false;
    }

    private static boolean isCommand(String[] args, String command) {
        for (int i = 0; i < args.length; i++) if (args[i].equals(command)) return true;
        return false;
    }

    private static void fakeLoadSystem(final Shell shell) {
        final int waitTime = 375;
        final SplashScreen splashScreen = new SplashScreen(shell.getShell());
        splashScreen.setRaiseAmount(waitTime);
        splashScreen.open();
        splashScreen.setMessage("Carregando o Gerente Digital........");
    }

    public static void disposeFakeSystem() {
        fakeDisplay.sleep();
        fakeDisplay.dispose();
    }
}
