package org.columba.mail.folder.command;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.columba.api.command.ICommandReference;
import org.columba.api.command.IWorkerStatusController;
import org.columba.core.command.Command;
import org.columba.core.command.StatusObservableImpl;
import org.columba.core.command.Worker;
import org.columba.mail.command.IMailFolderCommandReference;
import org.columba.mail.folder.IMailbox;
import org.columba.mail.util.MailResourceLoader;

/**
 * Defines command for saving message source to file
 *
 * @author Karl Peder Olesen (karlpeder), 20030615
 *
 */
public class SaveMessageSourceAsCommand extends Command {

    /** JDK 1.4+ logging framework logger, used for logging. */
    private static final Logger LOG = Logger.getLogger("org.columba.mail.folder.command");

    /**
     * Constructor for SaveMessageSourceAsCommand.
     * @param frameMediator
     * @param references
     */
    public SaveMessageSourceAsCommand(ICommandReference reference) {
        super(reference);
    }

    /**
     * Executes the command, i.e. saves source for selected
     * messages.
     * @see org.columba.api.command.Command#execute(Worker)
     */
    public void execute(IWorkerStatusController worker) throws Exception {
        IMailFolderCommandReference r = (IMailFolderCommandReference) getReference();
        Object[] uids = r.getUids();
        IMailbox srcFolder = (IMailbox) r.getSourceFolder();
        ((StatusObservableImpl) srcFolder.getObservable()).setWorker(worker);
        JFileChooser fileChooser = new JFileChooser();
        for (int j = 0; j < uids.length; j++) {
            Object uid = uids[j];
            LOG.info("Saving UID=" + uid);
            String subject = (String) srcFolder.getHeaderFields(uid, new String[] { "Subject" }).get("Subject");
            String defaultName = getValidFilename(subject, false);
            if (defaultName.length() == 0) {
                defaultName = srcFolder.getHeaderList().get(uid).get("columba.from").toString();
            }
            fileChooser.setSelectedFile(new File(defaultName));
            fileChooser.setDialogTitle(MailResourceLoader.getString("dialog", "saveas", "save_msg_source"));
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File f = fileChooser.getSelectedFile();
                if (f.exists()) {
                    int confirm = JOptionPane.showConfirmDialog(null, MailResourceLoader.getString("dialog", "saveas", "overwrite_existing_file"), MailResourceLoader.getString("dialog", "saveas", "file_exists"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (confirm == JOptionPane.NO_OPTION) {
                        j--;
                        continue;
                    }
                }
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = new BufferedInputStream(srcFolder.getMessageSourceStream(uid));
                    out = new BufferedOutputStream(new FileOutputStream(f));
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer, 0, buffer.length)) > 0) {
                        out.write(buffer, 0, read);
                    }
                } catch (IOException ioe) {
                    LOG.severe("Error saving msg source to file: " + ioe.getMessage());
                    JOptionPane.showMessageDialog(null, MailResourceLoader.getString("dialog", "saveas", "err_save_msg"), MailResourceLoader.getString("dialog", "saveas", "err_save_title"), JOptionPane.ERROR_MESSAGE);
                } finally {
                    try {
                        in.close();
                    } catch (IOException ioe) {
                    }
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException ioe) {
                    }
                }
            }
        }
    }

    /**
     * Private utility to extract a valid filename from a message
     * subject or another string.<br>
     * This means remove the chars: / \ : , \n \t
     * NB: If the input string is null, an empty string is returned
     * @param        subj                Message subject
     * @param        replSpaces        If true, spaces are replaced by _
     * @return        A valid filename without the chars mentioned
     */
    private String getValidFilename(String subj, boolean replSpaces) {
        if (subj == null) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < subj.length(); i++) {
            char c = subj.charAt(i);
            if ((c == '\\') || (c == '/') || (c == ':') || (c == ',') || (c == '\n') || (c == '\t') || (c == '[') || (c == ']')) {
            } else if ((c == ' ') && (replSpaces)) {
                buf.append('_');
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }
}
