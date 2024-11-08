package org.columba.mail.folder.command;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import javax.swing.JOptionPane;
import org.columba.api.command.ICommandReference;
import org.columba.api.command.IWorkerStatusController;
import org.columba.core.command.Command;
import org.columba.mail.command.IMailFolderCommandReference;
import org.columba.mail.folder.IMailbox;
import org.columba.mail.util.MailResourceLoader;

/**
 * Export all selected folders to a single MBOX mailbox file.
 * 
 * MBOX mailbox format: http://www.qmail.org/qmail-manual-html/man5/mbox.html
 * 
 * @author fdietz
 */
public class ExportFolderCommand extends Command {

    protected Object[] destUids;

    /**
	 * @param references
	 */
    public ExportFolderCommand(ICommandReference reference) {
        super(reference);
    }

    public void execute(IWorkerStatusController worker) throws Exception {
        IMailFolderCommandReference r = (IMailFolderCommandReference) getReference();
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(r.getDestFile()));
            int counter = 0;
            IMailbox srcFolder;
            Object[] uids;
            InputStream in;
            int read;
            byte[] buffer = new byte[1024];
            srcFolder = (IMailbox) r.getSourceFolder();
            uids = srcFolder.getUids();
            worker.setProgressBarMaximum(uids.length);
            worker.setProgressBarValue(0);
            for (int j = 0; (j < uids.length) && !worker.cancelled(); j++) {
                in = new BufferedInputStream(srcFolder.getMessageSourceStream(uids[j]));
                os.write(new String("From \r\n").getBytes());
                while ((read = in.read(buffer, 0, buffer.length)) > 0) {
                    os.write(buffer, 0, read);
                }
                try {
                    in.close();
                } catch (IOException ioe_) {
                }
                os.write(new String("\r\n").getBytes());
                os.flush();
                worker.setProgressBarValue(j);
                counter++;
            }
            if (worker.cancelled()) {
                worker.setDisplayText(MailResourceLoader.getString("statusbar", "message", "export_messages_cancelled"));
            } else {
                worker.setDisplayText(MessageFormat.format(MailResourceLoader.getString("statusbar", "message", "export_messages_success"), new Object[] { Integer.toString(counter) }));
            }
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(null, MailResourceLoader.getString("statusbar", "message", "err_export_messages_msg"), MailResourceLoader.getString("statusbar", "messages", "err_export_messages_title"), JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException ioe) {
            }
        }
    }
}
