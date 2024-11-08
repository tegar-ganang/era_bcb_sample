package be.lassi.ui.main;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import be.lassi.base.Dirty;
import be.lassi.base.DirtyIndicator;
import be.lassi.context.ShowContext;
import be.lassi.cues.Cue;
import be.lassi.cues.CueDetailFactory;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.domain.ShowCopier;
import be.lassi.io.ShowFileException;
import be.lassi.io.ShowReader;
import be.lassi.io.ShowWriter;
import be.lassi.preferences.AllPreferences;
import be.lassi.ui.base.BasicFrame;
import be.lassi.ui.show.NewShowParametersDialog;
import be.lassi.ui.show.ShowParameters;
import be.lassi.ui.show.ShowParametersDialog;
import be.lassi.ui.show.StartParameters;
import be.lassi.ui.show.StartParametersDialog;
import be.lassi.ui.util.mac.MacApplicationListener;
import be.lassi.util.NLS;

public class MainPresentationModel {

    private final ApplicationListener applicationListener = new ApplicationListener();

    private final WindowListener windowListener = new WindowListener();

    private final ShowContext context;

    private boolean exit = false;

    private boolean exitEnabled = true;

    private final MainActions actions;

    /**
     * The frame title.
     */
    public static final String TITLE = NLS.get("main.window.title");

    /**
     * All 'child' frames that are started from the main frame.
     */
    private final MainFrames frames;

    private final Component parent;

    public MainPresentationModel(final Component parent, final ShowContext context, final MainFrames frames) {
        this.parent = parent;
        this.context = context;
        this.frames = frames;
        actions = new MainActions(this);
    }

    public MainActions getActions() {
        return actions;
    }

    public ApplicationListener getApplicationListener() {
        return applicationListener;
    }

    public MainFrames getFrames() {
        return frames;
    }

    public WindowListener getWindowListener() {
        return windowListener;
    }

    /**
     * Read given show file.
     *
     * @param file file in which show was saved
     * @throws ShowFileException
     */
    private void readShow(final File file) throws ShowFileException {
        Show show = new ShowReader(context.getDirtyShow(), file.getAbsolutePath()).getShow();
        context.setShow(show);
        setFrameTitle(file.getName());
    }

    public void start() {
        StartParameters parameters = context.getPreferences().getStartParameters();
        if (parameters.getFilename().length() > 0) {
            parameters.setOpenShow(true);
        }
        StartParametersDialog dialog = new StartParametersDialog(parent, parameters);
        if (dialog.show()) {
            start(parameters);
        }
    }

    public boolean isExit() {
        return exit;
    }

    public void setExitEnabled(final boolean exitEnabled) {
        this.exitEnabled = exitEnabled;
    }

    private void start(final StartParameters parameters) {
        if (parameters.isOpenShow()) {
            File file = new File(parameters.getFilename());
            try {
                readShow(file);
                context.getDirtyShow().clear();
            } catch (ShowFileException e) {
                showFileOpenError(e);
            }
        } else {
            newShow(parameters);
        }
    }

    public void actionNew() {
        StartParameters parameters = context.getPreferences().getStartParameters();
        NewShowParametersDialog dialog = new NewShowParametersDialog(parent, parameters);
        if (dialog.show()) {
            newShow(parameters);
        }
    }

    public void actionOpen() {
        String filename = context.getPreferences().getStartParameters().getFilename();
        File file = new ShowFileDialog(parent).open(filename);
        if (file != null) {
            try {
                open(file);
            } catch (ShowFileException e) {
                showFileOpenError(e);
            }
        }
    }

    public void actionSave() {
        String filename = context.getPreferences().getStartParameters().getFilename();
        File file = new ShowFileDialog(parent).save(filename);
        if (file != null) {
            captureFrameProperties();
            save(file);
        }
    }

    public void actionSetup() {
        Show oldShow = context.getShow();
        ShowParameters parameters = new ShowParameters();
        parameters.setChannelCount("" + oldShow.getNumberOfChannels());
        parameters.setSubmasterCount("" + oldShow.getNumberOfSubmasters());
        ShowParametersDialog dialog = new ShowParametersDialog(parent, parameters);
        if (dialog.show()) {
            StartParameters startParameters = context.getPreferences().getStartParameters();
            startParameters.setChannelCount(parameters.getChannelCount());
            startParameters.setSubmasterCount(parameters.getSubmasterCount());
            int channelCount = parameters.getIntChannelCount();
            int submasterCount = parameters.getIntSubmasterCount();
            DirtyIndicator dirty = context.getDirtyShow();
            Show newShow = ShowBuilder.build(dirty, channelCount, submasterCount, channelCount, "");
            new ShowCopier(newShow, oldShow).copy();
            context.setShow(newShow);
        }
    }

    public void actionStartServer() {
        context.getServer().start();
        JOptionPane.showMessageDialog(parent, NLS.get("main.server.started"));
    }

    /**
     * Store the properties (position, size, visibility) of
     * all frames in the Show.
     */
    private void captureFrameProperties() {
        for (BasicFrame frame : frames) {
            frame.captureProperties();
        }
    }

    /**
     * "File->Exit" menu option action.
     */
    public void actionExit() {
        exit = exitConfirmed();
        if (exit) {
            savePreferences();
            if (exitEnabled) {
                System.exit(0);
            }
        }
    }

    private void savePreferences() {
        try {
            context.getPreferences().save();
        } catch (FileNotFoundException e) {
            savePreferencesFailed();
        }
    }

    private void savePreferencesFailed() {
        JOptionPane.showMessageDialog(parent, NLS.get("dialog.warning"), NLS.get("main.save.failed") + AllPreferences.FILE, JOptionPane.ERROR_MESSAGE);
    }

    private boolean exitConfirmed() {
        String message = NLS.get("quit.message1");
        if (context.getDirtyShow().isDirty()) {
            message = NLS.get("quit.message2");
        }
        int response = JOptionPane.showConfirmDialog(parent, message, NLS.get("quit.title"), JOptionPane.YES_NO_OPTION);
        return response == JOptionPane.YES_OPTION;
    }

    private void newShow(final ShowParameters parameters) {
        int channelCount = parameters.getIntChannelCount();
        int submasterCount = parameters.getIntSubmasterCount();
        int cueCount = parameters.getIntCueCount();
        Dirty dirty = context.getDirtyShow();
        Show show = ShowBuilder.build(dirty, channelCount, submasterCount, channelCount, "");
        context.getCuesController().insertCues(0, cueCount);
        CueDetailFactory factory = new CueDetailFactory(channelCount, submasterCount);
        for (int i = 0; i < cueCount; i++) {
            String number = "" + (i + 1);
            Cue cue = new Cue(number, "", "", "L 2");
            factory.update(cue);
            show.getCues().add(cue);
        }
        context.setShow(show);
    }

    /**
     * Open/read an existing show file selected by the user.
     * @throws ShowFileException
     */
    private void open(final File file) throws ShowFileException {
        readShow(file);
        context.getDirtyShow().clear();
    }

    /**
     * Save the current show in a file selected by the user.
     */
    private void save(final File file) {
        String filename = file.getAbsolutePath();
        context.getPreferences().getStartParameters().setFilename(filename);
        writeShow(file);
        context.getDirtyShow().clear();
    }

    /**
     * Set the frame window title to the show file name.
     *
     * @param filename
     */
    private void setFrameTitle(final String filename) {
        String title = TITLE;
        if (filename.length() > 0) {
            title += ": " + filename;
        }
        frames.getMainFrame().setTitle(title);
    }

    /**
     * Popup dialog with file open error message.
     *
     * @param e Exception that was raised while reading show file.
     */
    private void showFileOpenError(final ShowFileException e) {
        JOptionPane.showMessageDialog(parent, e.getMessage(), NLS.get("showFileDialog.error"), JOptionPane.ERROR_MESSAGE, null);
    }

    /**
     * Write show information to given file.
     *
     * @param file
     */
    private void writeShow(final File file) {
        try {
            ShowWriter writer = new ShowWriter(file.getAbsolutePath());
            writer.write(context.getShow());
            setFrameTitle(file.getName());
        } catch (ShowFileException e) {
            showFileOpenError(e);
        }
    }

    private class ApplicationListener implements MacApplicationListener {

        public void handleAbout() {
            new AboutDialog().show(parent);
        }

        public void handlePreferences() {
            JFrame frame = frames.getPreferencesFrame();
            frame.setState(Frame.NORMAL);
            frame.setVisible(true);
        }

        public boolean handleQuit() {
            actionExit();
            return exit;
        }
    }

    private class WindowListener extends WindowAdapter {

        @Override
        public void windowClosing(final WindowEvent evt) {
            actionExit();
        }
    }
}
