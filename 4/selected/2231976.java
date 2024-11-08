package sssvmtoolbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;

public class NewExperimentAction extends ImportExperimentAction {

    private final IWorkbenchWindow window;

    NewExperimentAction(final String text, final IWorkbenchWindow window) {
        super(text, window);
        this.window = window;
        setId(ICommandIds.CMD_NEW_EXPERIMENT);
        setActionDefinitionId(ICommandIds.CMD_NEW_EXPERIMENT);
        setImageDescriptor(sssvmtoolbox.Activator.getImageDescriptor("/icons/sample2.gif"));
    }

    @Override
    public void run() {
        final String[] filterExtensions = { "*.xml", "*.*" };
        final FileDialog fileDialog = new FileDialog(window.getShell(), SWT.SAVE);
        fileDialog.setText("Create New Experiment");
        fileDialog.setFileName("Experiment.xml");
        fileDialog.setFilterExtensions(filterExtensions);
        final String selectedFile = fileDialog.open();
        if (selectedFile == null) return;
        final FileInputStream stream = (FileInputStream) getClass().getClassLoader().getResourceAsStream("./template/template.xml");
        try {
            copyFile(stream, new File(selectedFile));
            importExperiment(selectedFile);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(final FileInputStream in, final File out) throws IOException {
        final FileChannel inChannel = in.getChannel();
        final FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (final IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }
}
