package gui;

import java.io.File;
import java.io.IOException;
import metadata.JpegFileMetadata;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import com.trolltech.qt.core.QDir;
import com.trolltech.qt.core.QDirIterator;
import com.trolltech.qt.gui.QFileDialog;
import com.trolltech.qt.gui.QMessageBox;
import com.trolltech.qt.gui.QWidget;

public class FormUnify extends QWidget {

    Ui_FormUnify ui = new Ui_FormUnify();

    String pathCamera1;

    String pathCamera2;

    final String PREFIX = "Prefix";

    final String SUFFIX = "Suffix";

    final String REPLACE = "ReplaceIndex";

    final String EXT_SEPARATOR = ".";

    Signal1<Integer> progres = new Signal1<Integer>();

    Signal1<Integer> maxProgres = new Signal1<Integer>();

    public FormUnify(String pathC1, String pathC2) {
        ui.setupUi(this);
        pathCamera1 = pathC1;
        pathCamera2 = pathC2;
        progres.connect(ui.progressBar, "setValue(int)");
        maxProgres.connect(ui.progressBar, "setMaximum(int)");
    }

    public void on_buttonBox_rejected() {
        this.close();
    }

    public void on_pbChoose_clicked() {
        String path = QFileDialog.getExistingDirectory();
        ui.leDestination.setText(path);
    }

    public void on_buttonBox_accepted() {
        if (!ui.leDestination.text().isEmpty()) {
            try {
                doRenaming(pathCamera1, ui.leRenameCamera1.text(), ui.cbModeCamera1.currentText(), ui.chkDateCamera1.isChecked(), ui.chkSeparateDirDate.isChecked(), ui.chkRenameCamera1.isChecked());
                doRenaming(pathCamera2, ui.leRenameCamera2.text(), ui.cbModeCamera2.currentText(), ui.chkDateCamera2.isChecked(), ui.chkSeparateDirDate.isChecked(), ui.chkRenameCamera2.isChecked());
            } catch (IOException e) {
                QMessageBox.information(this, "Error opening directory", e.getMessage());
            }
            QMessageBox.information(this, "Unification progress", "Unification have been made.");
            this.close();
        } else {
            QMessageBox.information(this, "Some field empty", "Destination path must be filled.");
        }
    }

    private void doRenaming(String pathCamera, String text, String mode, boolean chkdate, boolean chkDateDir, boolean chkRename) throws IOException {
        int countProgress = 0;
        String newFileName = null;
        String newPath = null;
        String path;
        String relativePath;
        String dateDir;
        String date = "";
        File oldFile;
        QDir dir = new QDir(pathCamera);
        maxProgres.emit(dir.count());
        QDirIterator dirIt = new QDirIterator(dir);
        dirIt.next();
        dirIt.next();
        while (dirIt.hasNext()) {
            path = dirIt.next();
            oldFile = new File(path);
            relativePath = dir.relativeFilePath(path);
            dateDir = "";
            try {
                if (chkdate) {
                    JpegFileMetadata metadata = new JpegFileMetadata(path);
                    date = metadata.getTimeDate();
                    date = "-" + date;
                }
                if (chkdate && chkDateDir) {
                    JpegFileMetadata metadata = new JpegFileMetadata(path);
                    date = metadata.getOnlyTime();
                    dateDir = QDir.separator() + metadata.getOnlyDate();
                    date = "-" + date;
                }
                if (!chkdate && chkDateDir) {
                    JpegFileMetadata metadata = new JpegFileMetadata(path);
                    dateDir = QDir.separator() + metadata.getOnlyDate();
                }
                if (chkRename) {
                    if (mode.equals(PREFIX)) {
                        newFileName = text + date + relativePath;
                    }
                    if (mode.equals(SUFFIX)) {
                        newFileName = FilenameUtils.getBaseName(path) + text + date + FilenameUtils.EXTENSION_SEPARATOR + FilenameUtils.getExtension(path);
                    }
                    if (mode.equals(REPLACE)) {
                        newFileName = text + "[" + countProgress + "]" + date + FilenameUtils.EXTENSION_SEPARATOR + FilenameUtils.getExtension(path);
                    }
                } else {
                    newFileName = FilenameUtils.getName(path);
                }
                newPath = ui.leDestination.text() + QDir.separator() + dateDir + QDir.separator() + newFileName;
                File newFile = new File(newPath);
                progres.emit(countProgress++);
                FileUtils.copyFile(oldFile, newFile);
            } catch (Exception e) {
            }
        }
        progres.emit(dir.count());
    }
}
