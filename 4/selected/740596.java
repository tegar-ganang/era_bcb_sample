package net.sf.groofy.player.integratedplayer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import net.sf.groofy.GroofyApp;
import net.sf.groofy.datamodel.Correspondence;
import net.sf.groofy.datamodel.MatchesPlaylist;
import net.sf.groofy.i18n.Messages;
import net.sf.groofy.jobs.DownloadCorrespondenceToPathJob;
import net.sf.groofy.jobs.Job;
import net.sf.groofy.jobs.JobStatus;
import net.sf.groofy.logger.GroofyLogger;
import net.sf.groofy.player.AbstractUiPlayer;
import net.sf.groofy.player.IPlayer;
import net.sf.groofy.preferences.GroofyConstants;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import com.google.gson.JsonParseException;
import com.richclientgui.toolbox.button.CoolButton;
import com.richclientgui.toolbox.button.CoolButtonSelectionEvent;
import com.richclientgui.toolbox.button.CoolButtonSelectionListener;
import com.richclientgui.toolbox.label.ScrollingLabel;
import com.richclientgui.toolbox.progressIndicator.CoolProgressBar;
import com.richclientgui.toolbox.slider.CoolSlider;
import com.richclientgui.toolbox.slider.CoolSliderPositionChangeListener;

/**
 * @author agomez (abelgomez@users.sourceforge.net)
 *
 */
public class IntegratedGroovesharkPlayer extends AbstractUiPlayer implements IPlayer {

    private final class SaveSelectionListener implements CoolButtonSelectionListener {

        @Override
        public void selectionOnRelease(CoolButtonSelectionEvent arg0) {
        }

        @Override
        public void selectionOnPress(CoolButtonSelectionEvent arg0) {
            Correspondence correspondence = groovesharkTrack;
            FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
            dialog.setText(Messages.getString("DownloadSongSelectionListener.SaveFileAs"));
            dialog.setFileName(String.format("%s - %s - %s", correspondence.getArtistName(), correspondence.getSongName(), correspondence.getAlbumName()));
            dialog.setFilterExtensions(new String[] { "*.mp3", "*.*" });
            final String filePath = dialog.open();
            if (StringUtils.isEmpty(filePath)) {
                return;
            }
            Job downloadSongJob = new Job(Messages.getString("IntegratedGroovesharkPlayer.SavingTrack"), new Image(Display.getDefault(), getClass().getResourceAsStream(GroofyConstants.PATH_16_SAVE))) {

                @Override
                public JobStatus run() {
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
                        InputStream source = new ByteArrayInputStream(player.getMusicBytes());
                        byte buffer[] = new byte[1024];
                        int read;
                        do {
                            read = source.read(buffer);
                            if (read > 0) {
                                fileOutputStream.write(buffer, 0, read);
                            }
                        } while (read != -1);
                        fileOutputStream.close();
                        source.close();
                    } catch (IOException e) {
                        GroofyLogger.getInstance().logError(e.getLocalizedMessage());
                    }
                    return JobStatus.OK;
                }
            };
            downloadSongJob.schedule();
        }
    }

    private final class PauseButtonSelectionListener implements CoolButtonSelectionListener {

        @Override
        public void selectionOnPress(CoolButtonSelectionEvent arg0) {
            player.pause();
        }

        @Override
        public void selectionOnRelease(CoolButtonSelectionEvent arg0) {
            showPlayButton(true);
        }
    }

    private final class PlayButtonSelectionListener implements CoolButtonSelectionListener {

        @Override
        public void selectionOnPress(CoolButtonSelectionEvent arg0) {
            showPauseButton();
        }

        @Override
        public void selectionOnRelease(CoolButtonSelectionEvent arg0) {
            player.play();
        }
    }

    private final class SongPositionChangeListener implements CoolSliderPositionChangeListener {

        @Override
        public void positionChanged(double percentage) {
            player.seek((int) (percentage * player.getTotalSize()));
        }
    }

    private class UpdateTimeThread extends Thread {

        private Player player;

        private volatile boolean terminate = false;

        public UpdateTimeThread(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            boolean wasRunning = false;
            do {
                updateDownloadProgress((float) player.getLoaded() / player.getTotalSize());
                if (player.isPlaying()) {
                    showDuration(player.getPosition(), player.getDuration());
                    wasRunning = true;
                } else if (player.isPaused()) {
                    showDuration(player.getPosition(), player.getDuration());
                    wasRunning = true;
                } else if (player.isComplete()) {
                    if (wasRunning) {
                        showPlayButton(true);
                        wasRunning = false;
                    }
                    showDuration(0, player.getDuration());
                    player.close();
                }
                if (player.isLoaded()) {
                    setSaveButtonEnabled(true);
                } else {
                    setSaveButtonEnabled(false);
                }
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                }
            } while (!terminate);
        }

        void terminate() {
            terminate = true;
        }
    }

    private static Color backgroundColor = new Color(Display.getDefault(), new RGB(92, 92, 92));

    private Composite playerComposite = null;

    private Composite buttonsComposite = null;

    private Composite infoComposite = null;

    private CoolProgressBar coolProgressBar;

    private CoolSlider progressBar = null;

    private ScrollingLabel infoLabel = null;

    private CoolButton pauseButton;

    private CoolButton playButton;

    private CoolButton saveButton;

    private StackLayout stackLayout;

    private Player player;

    private Label timeLabel = null;

    private Label totalTimeLabel;

    private UpdateTimeThread updateTimeThread;

    private Composite saveComposite = null;

    private Correspondence groovesharkTrack;

    public IntegratedGroovesharkPlayer(Composite parent, int style) {
        super(parent, style);
        initialize();
    }

    @Override
    public void destroy() {
        stopPlaying();
    }

    @Override
    public void playGroovesharkSong(final Correspondence groovesharkTrack) {
        this.groovesharkTrack = groovesharkTrack;
        Thread t = new Thread() {

            @Override
            public void run() {
                stopPlaying();
                showLoadingInfo();
                updateDownloadProgress(0);
                showDuration(0, 0);
                try {
                    player = new Player(groovesharkTrack.getSongID());
                    showSongInfo(groovesharkTrack);
                    startPlaying();
                } catch (IOException e) {
                    GroofyApp.getInstance().showError(String.format(Messages.getString("IntegratedGroovesharkPlayer.UnablePlaySong"), groovesharkTrack.getSongName(), groovesharkTrack.getArtistName()));
                } catch (JsonParseException e) {
                    GroofyApp.getInstance().showError(String.format(Messages.getString("IntegratedGroovesharkPlayer.UnablePlaySong"), groovesharkTrack.getSongName(), groovesharkTrack.getArtistName()));
                }
                showPauseButton();
            }
        };
        t.start();
    }

    @Override
    public void stop() {
        stopPlaying();
    }

    /**
	 * This method initializes buttonsComposite	
	 *
	 */
    private void createButtonsComposite() {
        GridData gridData11 = new GridData();
        gridData11.horizontalAlignment = GridData.BEGINNING;
        gridData11.verticalAlignment = GridData.CENTER;
        stackLayout = new StackLayout();
        buttonsComposite = new Composite(playerComposite, SWT.NONE);
        buttonsComposite.setLayout(stackLayout);
        buttonsComposite.setLayoutData(gridData11);
        buttonsComposite.setBackground(backgroundColor);
        playButton = createPlayButton();
        playButton.setBackground(backgroundColor);
        playButton.addSelectionListener(new PlayButtonSelectionListener());
        pauseButton = createPauseButton();
        pauseButton.setBackground(backgroundColor);
        pauseButton.addSelectionListener(new PauseButtonSelectionListener());
        showPlayButton(false);
    }

    /**
	 * This method initializes infoComposite	
	 *
	 */
    private void createInfoComposite() {
        GridData gridData61 = new GridData();
        gridData61.heightHint = 0;
        gridData61.widthHint = 0;
        GridData gridData5 = new GridData();
        gridData5.widthHint = 0;
        gridData5.heightHint = 0;
        GridData gridData4 = new GridData();
        gridData4.horizontalAlignment = GridData.FILL;
        gridData4.grabExcessHorizontalSpace = true;
        gridData4.verticalAlignment = GridData.CENTER;
        GridData gridData3 = new GridData();
        gridData3.horizontalAlignment = GridData.FILL;
        gridData3.grabExcessHorizontalSpace = true;
        gridData3.grabExcessVerticalSpace = true;
        gridData3.heightHint = -1;
        gridData3.verticalAlignment = GridData.FILL;
        GridData gridData21 = new GridData();
        gridData21.horizontalSpan = 3;
        gridData21.verticalAlignment = GridData.CENTER;
        gridData21.grabExcessHorizontalSpace = true;
        gridData21.grabExcessVerticalSpace = false;
        gridData21.heightHint = 16;
        gridData21.horizontalAlignment = GridData.FILL;
        GridLayout gridLayout4 = new GridLayout();
        gridLayout4.numColumns = 3;
        gridLayout4.verticalSpacing = 1;
        gridLayout4.marginWidth = 0;
        gridLayout4.makeColumnsEqualWidth = false;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.verticalAlignment = GridData.FILL;
        infoComposite = new Composite(playerComposite, SWT.NONE);
        infoComposite.setLayoutData(gridData);
        infoComposite.setLayout(gridLayout4);
        infoComposite.setBackground(backgroundColor);
        infoLabel = new ScrollingLabel(infoComposite, SWT.H_SCROLL);
        infoLabel.setText(Messages.getString("IntegratedGroovesharkPlayer.NoSongSelected"));
        infoLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        infoLabel.setLayoutData(gridData21);
        infoLabel.setBackground(backgroundColor);
        createTime();
        progressBar = new CoolSlider(infoComposite, SWT.HORIZONTAL, new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_GUI_SLIDER_LM)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_GUI_SLIDER_LT)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_GUI_SLIDER_TH)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_GUI_SLIDER_TH)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_GUI_SLIDER_TH)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_GUI_SLIDER_RT)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_GUI_SLIDER_RM)));
        progressBar.setBackground(backgroundColor);
        progressBar.setLayoutData(gridData4);
        progressBar.addPositionChangeListener(new SongPositionChangeListener());
        createTotalTime();
        Label filler = new Label(infoComposite, SWT.NONE);
        filler.setLayoutData(gridData5);
        coolProgressBar = new CoolProgressBar(infoComposite, SWT.HORIZONTAL, new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_GUI_PROGRESS_BL)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_GUI_PROGRESS_FILL)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_GUI_PROGRESS_EMPTY)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_GUI_PROGRESS_BR)));
        coolProgressBar.setBackground(backgroundColor);
        coolProgressBar.setLayoutData(gridData3);
        filler = new Label(infoComposite, SWT.NONE);
        filler.setLayoutData(gridData61);
    }

    private CoolButton createPauseButton() {
        return new CoolButton(buttonsComposite, new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_PLAYER_PAUSE)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_PLAYER_PAUSE_HOVER)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_PLAYER_PAUSE_SELECTED)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_PLAYER_PAUSE_DISABLED)));
    }

    private CoolButton createPlayButton() {
        return new CoolButton(buttonsComposite, new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_PLAYER_PLAY)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_PLAYER_PLAY_HOVER)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_PLAYER_PLAY_SELECTED)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_PLAYER_PLAY_DISABLED)));
    }

    private CoolButton createSaveButton() {
        return new CoolButton(saveComposite, new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_PLAYER_SAVE)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_PLAYER_SAVE_HOVER)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_PLAYER_SAVE_SELECTED)), new Image(getShell().getDisplay(), getClass().getResourceAsStream(GroofyConstants.PATH_PLAYER_SAVE_DISABLED)));
    }

    /**
	 * This method initializes playerComposite	
	 *
	 */
    private void createPlayerComposite() {
        GridData gridData1 = new GridData();
        gridData1.grabExcessVerticalSpace = true;
        gridData1.verticalAlignment = GridData.FILL;
        gridData1.widthHint = 300;
        gridData1.horizontalAlignment = GridData.BEGINNING;
        GridLayout gridLayout2 = new GridLayout();
        gridLayout2.horizontalSpacing = 0;
        gridLayout2.marginWidth = 3;
        gridLayout2.marginHeight = 0;
        gridLayout2.numColumns = 3;
        gridLayout2.verticalSpacing = 0;
        playerComposite = new Composite(this, SWT.NONE);
        playerComposite.setLayout(gridLayout2);
        playerComposite.setBackground(backgroundColor);
        createSaveComposite();
        createInfoComposite();
        createButtonsComposite();
        playerComposite.setLayoutData(gridData1);
    }

    private void createTime() {
        GridData gridData7 = new GridData();
        gridData7.widthHint = 30;
        gridData7.verticalAlignment = GridData.END;
        gridData7.horizontalAlignment = GridData.CENTER;
        timeLabel = new Label(infoComposite, SWT.RIGHT);
        timeLabel.setText("00:00");
        timeLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        timeLabel.setLayoutData(gridData7);
        timeLabel.setBackground(backgroundColor);
    }

    private void createTotalTime() {
        GridData gridData6 = new GridData();
        gridData6.widthHint = 30;
        gridData6.verticalAlignment = GridData.END;
        gridData6.horizontalAlignment = GridData.CENTER;
        totalTimeLabel = new Label(infoComposite, SWT.RIGHT);
        totalTimeLabel.setText("00:00");
        totalTimeLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        totalTimeLabel.setLayoutData(gridData6);
        totalTimeLabel.setBackground(backgroundColor);
    }

    private void initialize() {
        GridLayout gridLayout1 = new GridLayout();
        gridLayout1.horizontalSpacing = 0;
        gridLayout1.marginWidth = 0;
        gridLayout1.marginHeight = 0;
        gridLayout1.numColumns = 1;
        gridLayout1.verticalSpacing = 0;
        this.setLayout(gridLayout1);
        createPlayerComposite();
    }

    private void showDuration(final int currentPos, final int totalDuration) {
        if (!Display.getDefault().isDisposed()) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    float f = (float) currentPos / totalDuration;
                    if (!progressBar.isDisposed()) progressBar.updateSlider(f, false);
                    if (!timeLabel.isDisposed()) timeLabel.setText(String.format("%01d:%02d", currentPos / 60, currentPos % 60));
                    if (!totalTimeLabel.isDisposed()) totalTimeLabel.setText(String.format("%01d:%02d", totalDuration / 60, totalDuration % 60));
                }
            });
        }
    }

    private void showLoadingInfo() {
        if (!Display.getDefault().isDisposed()) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    if (!infoLabel.isDisposed()) infoLabel.setText(Messages.getString("IntegratedGroovesharkPlayer.LoadingSong"));
                }
            });
        }
    }

    private void showPauseButton() {
        if (!Display.getDefault().isDisposed()) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    if (stackLayout.topControl != pauseButton) {
                        stackLayout.topControl = pauseButton;
                        if (!buttonsComposite.isDisposed()) buttonsComposite.layout();
                    }
                }
            });
        }
    }

    private void showPlayButton(final boolean enabled) {
        if (!Display.getDefault().isDisposed()) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    if (stackLayout.topControl != playButton) {
                        stackLayout.topControl = playButton;
                        if (!playButton.isDisposed()) {
                            playButton.setEnabled(enabled);
                        }
                        if (!buttonsComposite.isDisposed()) buttonsComposite.layout();
                    }
                }
            });
        }
    }

    private void showSongInfo(final Correspondence groovesharkTrack) {
        if (!Display.getDefault().isDisposed()) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    if (!infoLabel.isDisposed()) infoLabel.setText(String.format("%s - %s - %s", groovesharkTrack.getArtistName(), groovesharkTrack.getSongName(), groovesharkTrack.getAlbumName()));
                }
            });
        }
    }

    private void startPlaying() {
        player.play();
        if (updateTimeThread != null) {
            updateTimeThread.terminate();
        }
        updateTimeThread = new UpdateTimeThread(player);
        updateTimeThread.start();
    }

    private void stopPlaying() {
        if (player != null) player.close();
        if (updateTimeThread != null) updateTimeThread.terminate();
    }

    private void updateDownloadProgress(final float percentage) {
        if (!Display.getDefault().isDisposed()) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    if (!coolProgressBar.isDisposed()) coolProgressBar.updateProgress(percentage);
                }
            });
        }
    }

    /**
	 * This method initializes saveComposite	
	 *
	 */
    private void createSaveComposite() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.verticalSpacing = 0;
        saveComposite = new Composite(playerComposite, SWT.NONE);
        saveComposite.setBackground(backgroundColor);
        saveComposite.setLayout(gridLayout);
        saveButton = createSaveButton();
        saveButton.setBackground(backgroundColor);
        saveButton.addSelectionListener(new SaveSelectionListener());
        setSaveButtonEnabled(false);
    }

    private void setSaveButtonEnabled(final boolean enabled) {
        if (!Display.getDefault().isDisposed()) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    if (!saveButton.isDisposed() && saveButton.isEnabled() != enabled) {
                        saveButton.setEnabled(enabled);
                    }
                }
            });
        }
    }
}
