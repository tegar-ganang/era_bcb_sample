package com.jme3.gde.assetpack.project.actions;

import com.jme3.gde.assetpack.Installer;
import com.jme3.gde.assetpack.online.OnlinePacksConnector;
import com.jme3.gde.assetpack.project.AssetPackProject;
import com.jme3.gde.assetpack.project.wizards.PublishAssetPackWizardPanel1;
import com.jme3.gde.assetpack.project.wizards.PublishAssetPackWizardPanel2;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.Action;
import javax.swing.JComponent;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbPreferences;

@SuppressWarnings("unchecked")
public final class PublishAssetPackAction implements Action {

    private final Project context;

    private WizardDescriptor.Panel[] panels;

    public PublishAssetPackAction(Project context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        final WizardDescriptor wizardDescriptor = new WizardDescriptor(getPanels());
        wizardDescriptor.setTitleFormat(new MessageFormat("{0}"));
        wizardDescriptor.setTitle("Publish AssetPack");
        wizardDescriptor.putProperty("project", context);
        String projectName = ((AssetPackProject) context).getProjectName().replaceAll(" ", "_") + ".zip";
        wizardDescriptor.putProperty("filename", projectName);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(wizardDescriptor);
        dialog.setVisible(true);
        dialog.toFront();
        boolean cancelled = wizardDescriptor.getValue() != WizardDescriptor.FINISH_OPTION;
        if (!cancelled) {
            new Thread(new Runnable() {

                public void run() {
                    ProgressHandle handle = ProgressHandleFactory.createHandle("Publishing AssetPack..");
                    handle.start();
                    packZip(wizardDescriptor);
                    copyData(wizardDescriptor);
                    handle.progress("Uploading AssetPack..");
                    uploadData(wizardDescriptor);
                    cleanup(wizardDescriptor);
                    handle.finish();
                }
            }).start();
        }
    }

    private void packZip(WizardDescriptor wiz) {
        try {
            String outFilename = context.getProjectDirectory().getPath() + "/" + wiz.getProperty("filename");
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(outFilename)));
            out.setLevel(9);
            zipDir(((AssetPackProject) context).getProjectDirectory(), out, (String) wiz.getProperty("filename"));
            out.close();
        } catch (IOException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error creating ZIP file!");
        }
    }

    public void zipDir(FileObject dir2zip, ZipOutputStream zos, String fileName) {
        try {
            FileObject[] dirList = dir2zip.getChildren();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                FileObject f = dirList[i];
                if (f.isFolder()) {
                    zipDir(f, zos, fileName);
                    continue;
                }
                InputStream fis = f.getInputStream();
                if (!f.getNameExt().equals(fileName) && !f.getNameExt().startsWith(".")) {
                    String filePathName = f.getPath().replaceAll(context.getProjectDirectory().getPath(), "");
                    ZipEntry anEntry = new ZipEntry(filePathName);
                    zos.putNextEntry(anEntry);
                    while ((bytesIn = fis.read(readBuffer)) != -1) {
                        zos.write(readBuffer, 0, bytesIn);
                    }
                    fis.close();
                }
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error creating ZIP file!");
        }
    }

    private void copyData(WizardDescriptor wiz) {
        String folder = (String) wiz.getProperty("publish_folder");
        if (folder == null) {
            return;
        }
        String zipFilename = context.getProjectDirectory().getPath() + "/" + wiz.getProperty("filename");
        try {
            FileObject source = FileUtil.toFileObject(new File(zipFilename));
            FileObject destination = FileUtil.toFileObject(new File(folder));
            source.copy(destination, source.getName(), source.getExt());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error copying ZIP file!");
        }
    }

    private void uploadData(WizardDescriptor wiz) {
        if (wiz.getProperty("publish_jmeorg") == null) {
            return;
        }
        String file = context.getProjectDirectory().getPath() + "/" + wiz.getProperty("filename");
        String user = NbPreferences.forModule(Installer.class).get("assetpack_user", null);
        String pass = NbPreferences.forModule(Installer.class).get("assetpack_pass", null);
        OnlinePacksConnector.upload(file, user, pass);
    }

    private void cleanup(WizardDescriptor wiz) {
        String file = context.getProjectDirectory().getPath() + "/" + wiz.getProperty("filename");
        new File(file).delete();
    }

    /**
     * Initialize panels representing individual wizard's steps and sets
     * various properties for them influencing wizard appearance.
     */
    private WizardDescriptor.Panel[] getPanels() {
        if (panels == null) {
            panels = new WizardDescriptor.Panel[] { new PublishAssetPackWizardPanel1(), new PublishAssetPackWizardPanel2() };
            String[] steps = new String[panels.length];
            for (int i = 0; i < panels.length; i++) {
                Component c = panels[i].getComponent();
                steps[i] = c.getName();
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent) c;
                    jc.putClientProperty("WizardPanel_contentSelectedIndex", new Integer(i));
                    jc.putClientProperty("WizardPanel_contentData", steps);
                    jc.putClientProperty("WizardPanel_autoWizardStyle", Boolean.TRUE);
                    jc.putClientProperty("WizardPanel_contentDisplayed", Boolean.TRUE);
                    jc.putClientProperty("WizardPanel_contentNumbered", Boolean.TRUE);
                }
            }
        }
        return panels;
    }

    public Object getValue(String key) {
        if (key.equals(NAME)) {
            return "Publish AssetPack..";
        }
        return null;
    }

    public void putValue(String key, Object value) {
    }

    public void setEnabled(boolean b) {
    }

    public boolean isEnabled() {
        return true;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }
}
