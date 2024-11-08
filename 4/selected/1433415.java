package no.ugland.utransprod.gui.handlers;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.ListModel;
import no.ugland.utransprod.ProTransException;
import no.ugland.utransprod.ProTransRuntimeException;
import no.ugland.utransprod.gui.WindowInterface;
import no.ugland.utransprod.util.ApplicationParamUtil;
import no.ugland.utransprod.util.DesktopUtil;
import no.ugland.utransprod.util.Util;
import org.apache.commons.io.FileUtils;
import org.jdesktop.jdic.desktop.Desktop;
import org.jdesktop.jdic.desktop.DesktopException;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.list.SelectionInList;

public class AttachmentViewHandler {

    private static String attachmentBaseDir;

    private final ArrayListModel fileNameList;

    private File attachmentDir;

    private final SelectionInList fileNameSelectionList;

    private WindowInterface window;

    private JButton buttonDeleteAttachment;

    private JButton buttonShowAttachment;

    private JButton buttonAddAttachment;

    public AttachmentViewHandler(String aAttachmentDir) throws ProTransException {
        attachmentBaseDir = ApplicationParamUtil.findParamByName("attachment_dir");
        fileNameList = new ArrayListModel();
        fileNameSelectionList = new SelectionInList((ListModel) fileNameList);
        fileNameSelectionList.addPropertyChangeListener(SelectionInList.PROPERTYNAME_SELECTION_EMPTY, new EmptyListSelectionListener());
        setupPresentationModel(aAttachmentDir);
    }

    private void setupPresentationModel(String aAttachmentDir) throws ProTransException {
        try {
            attachmentDir = new File(attachmentBaseDir + "/" + aAttachmentDir);
            if (!attachmentDir.exists()) {
                FileUtils.forceMkdir(attachmentDir);
            }
            String[] fileNames = attachmentDir.list();
            fileNameList.addAll(new ArrayList<String>(Arrays.asList(fileNames)));
        } catch (IOException e) {
            e.printStackTrace();
            throw new ProTransException(e.getMessage());
        }
    }

    public JList getListAttachments(final WindowInterface aWindow) {
        window = aWindow;
        JList list = BasicComponentFactory.createList(fileNameSelectionList);
        list.setName("ListAttachments");
        return list;
    }

    public JButton getButtonAddAttachment(final WindowInterface aWindow) {
        window = aWindow;
        buttonAddAttachment = new JButton(new AddAttachmentAction());
        buttonAddAttachment.setName("ButtonAddAttachment");
        return buttonAddAttachment;
    }

    public JButton getButtonShowAttachment(final WindowInterface aWindow) {
        window = aWindow;
        buttonShowAttachment = new JButton(new ShowAttachmentAction());
        buttonShowAttachment.setName("ButtonShowAttachment");
        buttonShowAttachment.setEnabled(false);
        return buttonShowAttachment;
    }

    public JButton getButtonDeleteAttachment(final WindowInterface aWindow) {
        window = aWindow;
        buttonDeleteAttachment = new JButton(new DeleteAttachmentAction());
        buttonDeleteAttachment.setName("ButtonDeleteAttachment");
        buttonDeleteAttachment.setEnabled(false);
        return buttonDeleteAttachment;
    }

    private class AddAttachmentAction extends AbstractAction {

        public AddAttachmentAction() {
            super("Legg til fil...");
        }

        public void actionPerformed(ActionEvent e) {
            addFile();
        }
    }

    private class ShowAttachmentAction extends AbstractAction {

        public ShowAttachmentAction() {
            super("Vis fil...");
        }

        public void actionPerformed(ActionEvent e) {
            showFile();
        }
    }

    private class DeleteAttachmentAction extends AbstractAction {

        public DeleteAttachmentAction() {
            super("Slett fil...");
        }

        public void actionPerformed(final ActionEvent e) {
            deleteFile();
        }
    }

    public final void addFile() {
        try {
            String fileName = Util.getFileName(window != null ? window.getComponent() : null, null, null);
            File srcFile = new File(fileName);
            FileUtils.copyFileToDirectory(srcFile, attachmentDir);
            fileNameList.add(srcFile.getName());
        } catch (IOException e) {
            e.printStackTrace();
            Util.showErrorDialog(window, "Feil", e.getMessage());
        }
    }

    public final void showFile() {
        try {
            String fileName = (String) fileNameSelectionList.getSelection();
            File showFile = fileName != null ? new File(attachmentDir.getAbsolutePath() + "/" + fileName) : null;
            DesktopUtil desktopUtil = new DesktopUtil();
            desktopUtil.openFile(showFile, window);
        } catch (DesktopException e) {
            throw new ProTransRuntimeException(e.getMessage());
        }
    }

    public final void deleteFile() {
        if (Util.showConfirmDialog(window, "Slette?", "Vil du virkelig slette fil?")) {
            String fileName = (String) fileNameSelectionList.getSelection();
            File deleteFile = fileName != null ? new File(attachmentDir.getAbsolutePath() + "/" + fileName) : null;
            boolean success = deleteFile != null ? deleteFile.delete() : false;
            success = success ? fileNameList.remove(fileName) : false;
        }
    }

    private class EmptyListSelectionListener implements PropertyChangeListener {

        public void propertyChange(final PropertyChangeEvent evt) {
            boolean hasSelection = fileNameSelectionList.hasSelection();
            buttonDeleteAttachment.setEnabled(hasSelection);
            buttonShowAttachment.setEnabled(hasSelection);
        }
    }

    public final void setComponentEnablement(final boolean enable) {
        buttonDeleteAttachment.setEnabled(enable ? fileNameSelectionList.hasSelection() : false);
        buttonAddAttachment.setEnabled(enable);
        buttonShowAttachment.setEnabled(enable ? fileNameSelectionList.hasSelection() : false);
    }
}
