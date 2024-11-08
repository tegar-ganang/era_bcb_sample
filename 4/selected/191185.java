package br.com.visualmidia.ui.wizard.backup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import br.com.visualmidia.core.Constants;
import br.com.visualmidia.system.GDServer;
import br.com.visualmidia.system.GDSystem;

public class DescompactFilesFromFolders {

    static final int BUFFER = 1024;

    private String sourcePath;

    private ProgressBar progressBar;

    private int cont = 0;

    private Shell _parent;

    public DescompactFilesFromFolders(String sourcePath, ProgressBar progressBar, String destPath, Shell parent) {
        _parent = parent;
        this.sourcePath = sourcePath;
        this.progressBar = progressBar;
    }

    public void run() {
        progressBar.getDisplay().syncExec(new Runnable() {

            public void run() {
                progressBar.setSelection(cont++);
            }
        });
        deleteFilesDataFolder();
        progressBar.getDisplay().syncExec(new Runnable() {

            public void run() {
                progressBar.setSelection(cont++);
            }
        });
        unzip();
        try {
            if (GDSystem.isServerMode()) {
                MessageBox box = new MessageBox(_parent, IMessageProvider.WARNING);
                box.setMessage("Os dados foram restaurados com sucesso! \nO GerenteDigital Server ser� finalizado. Inicie-o novamente.");
                box.setText("Alerta do GerenteDigital Server!");
                box.open();
                System.exit(0);
            } else {
                GDSystem.getInstance().startPrevayler();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void deleteFilesDataFolder() {
        File[] filesInTheDataFolderSnapshot;
        if (GDSystem.isServerMode()) {
            filesInTheDataFolderSnapshot = new File(Constants.PREVAYLER_SERVER_DATA_DIRETORY).listFiles();
        } else {
            filesInTheDataFolderSnapshot = new File(Constants.PREVAYLER_DATA_DIRETORY).listFiles();
        }
        for (File file : filesInTheDataFolderSnapshot) {
            file.delete();
        }
    }

    private void deleteFilesSnapshotsFolder() {
        File[] filesInFolderSnapshot = new File(Constants.PREVAYLER_SNAPSHOT_DIRETORY).listFiles();
        for (File file : filesInFolderSnapshot) {
            file.delete();
        }
    }

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public void unzip() {
        Enumeration entries;
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(sourcePath);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String[] fileName = entry.getName().split("/");
                String folderName = (GDSystem.isServerMode()) ? Constants.PREVAYLER_SERVER_DATA_DIRETORY : Constants.PREVAYLER_DATA_DIRETORY;
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(folderName + "\\" + fileName[fileName.length - 1])));
                progressBar.getDisplay().syncExec(new Runnable() {

                    public void run() {
                        progressBar.setSelection(cont++);
                    }
                });
            }
            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Erro ao descompactar:" + ioe.getMessage());
            return;
        }
    }
}
