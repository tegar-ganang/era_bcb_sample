package com.safi.workshop.audio.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteOrder;
import java.util.List;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import com.safi.db.server.config.TelephonySubsystem;
import com.safi.server.manager.SafiServerRemoteManager;
import com.safi.server.plugin.SafiServerPlugin;
import com.safi.server.saflet.util.FileUtils;
import com.safi.workshop.audio.PCMToGSMConverter;
import com.safi.workshop.part.AsteriskDiagramEditorPlugin;
import com.safi.workshop.part.SafiWorkshopEditorUtil;
import com.safi.workshop.preferences.AudioDevicesPrefPage;

public class AudioUtils {

    public static Image playImage;

    public static Image stopImage;

    public static Image pauseImage;

    public static Image recordImage;

    public static Image rewindImage;

    public static Image cutImage;

    public static Image copyImage;

    public static Image pasteImage;

    public static Image deleteImage;

    public static Image undoImage;

    public static Image redoImage;

    public static Image zoomInImage;

    public static Image zoomOutImage;

    public static Image zoomToFitImage;

    public static Image amplitudeUpImage;

    public static Image amplitudeDownImage;

    public static Image editWaveformImage;

    public static Image muteImage;

    public static Image editPromptImage;

    public static Image refreshImage;

    public static Image newPromptImage;

    public static Image sliderLeftmost;

    public static Image sliderLeftTile;

    public static Image sliderThumb;

    public static Image sliderRightTile;

    public static Image sliderRightmost;

    public static Image clearTextImage;

    public static Image insertSilenceImage;

    public static Image synchronizeImage;

    static {
        try {
            playImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/play.gif");
            stopImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/stop.gif");
            pauseImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/pause.gif");
            recordImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/record.gif");
            rewindImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/rewindToStart.gif");
            cutImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/cut.gif");
            copyImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/copy.gif");
            pasteImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/paste.gif");
            deleteImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/delete.gif");
            undoImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/undo.gif");
            redoImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/redo.gif");
            zoomInImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/zoom_in.gif");
            zoomOutImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/zoom_out.gif");
            zoomToFitImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/zoomtofit.gif");
            amplitudeUpImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/amp_up.gif");
            amplitudeDownImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/amp_dn.gif");
            muteImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/mute.gif");
            insertSilenceImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/insertSilence.gif");
            editWaveformImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/edit_waveform.gif");
            editPromptImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/edit_prompt.gif");
            refreshImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/refresh.gif");
            newPromptImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/new_prompt.gif");
            clearTextImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/clear_text.gif");
            synchronizeImage = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/audio/synch.gif");
            sliderLeftmost = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/slider/slider_leftmost.png");
            sliderLeftTile = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/slider/slider_left_tile.png");
            sliderThumb = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/slider/slider_thumb.png");
            sliderRightTile = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/slider/slider_right_tile.png");
            sliderRightmost = AsteriskDiagramEditorPlugin.getInstance().getBundledImage("icons/slider/slider_rightmost.png");
        } catch (Exception e) {
        }
    }

    public static void loadStandaloneImages() {
        sliderLeftmost = loadImage("icons/slider/slider_leftmost.png");
        sliderLeftTile = loadImage("icons/slider/slider_left_tile.png");
        sliderThumb = loadImage("icons/slider/slider_thumb.png");
        sliderRightTile = loadImage("icons/slider/slider_right_tile.png");
        sliderRightmost = loadImage("icons/slider/slider_rightmost.png");
    }

    public static Image loadImage(String imageFilename) {
        InputStream stream = ClassLoader.getSystemResourceAsStream(imageFilename);
        if (stream == null) {
            return null;
        }
        Image image = null;
        try {
            image = new Image(Display.getDefault(), stream);
        } catch (SWTException ex) {
        } finally {
            try {
                stream.close();
            } catch (IOException ex) {
            }
        }
        return image;
    }

    public static AudioFileFormat.Type RAW_TYPE = new AudioFileFormat.Type("raw", "raw");

    public static final AudioFormat DEFAULT_AUDIO_FORMAT = new AudioFormat(8000.0f, 16, 1, true, AudioCommon.SYSTEM_BYTE_ORDER == ByteOrder.BIG_ENDIAN);

    public static final AudioFileFormat.Type GSM_FORMAT = new AudioFileFormat.Type("GSM", "gsm");

    public static class LineAndStream {

        private final TargetDataLine mLine;

        private final AudioInputStream mStream;

        public TargetDataLine getLine() {
            return mLine;
        }

        public AudioInputStream getStream() {
            return mStream;
        }

        public LineAndStream(TargetDataLine line, AudioInputStream stream) {
            mLine = line;
            mStream = stream;
        }
    }

    public static LineAndStream getRecordingStream(AudioFormat format) throws AudioException {
        Mixer mixer = null;
        try {
            mixer = AudioDevicesPrefPage.getInputMixer();
            TargetDataLine line = AudioSystem.getTargetDataLine(format, mixer.getMixerInfo());
            return new LineAndStream(line, new AudioInputStream(line));
        } catch (Exception iae) {
            iae.printStackTrace();
            LineAndStream lineAndStream = null;
            if (mixer != null) {
                lineAndStream = getLineAndStream(format, mixer);
                if (lineAndStream != null) return lineAndStream;
            } else {
                for (Mixer.Info mInfo : AudioSystem.getMixerInfo()) {
                    mixer = AudioSystem.getMixer(mInfo);
                    lineAndStream = getLineAndStream(format, mixer);
                    if (lineAndStream != null) return lineAndStream;
                }
            }
            throw new AudioException("Couldn't make line for format " + format, iae);
        }
    }

    private static LineAndStream getLineAndStream(AudioFormat format, Mixer mixer) {
        for (Line.Info lInfo : mixer.getTargetLineInfo()) {
            try {
                if (lInfo.getLineClass().equals(TargetDataLine.class)) {
                    DataLine.Info dInfo = (DataLine.Info) lInfo;
                    for (AudioFormat f : dInfo.getFormats()) {
                        if (AudioSystem.isConversionSupported(format, f)) {
                            try {
                                TargetDataLine candidate = (TargetDataLine) mixer.getLine(dInfo);
                                AudioFormat specifiedFormat = new AudioFormat(f.getEncoding(), format.getSampleRate(), f.getSampleSizeInBits(), f.getChannels(), f.getFrameSize(), format.getSampleRate(), f.isBigEndian());
                                candidate.open(specifiedFormat);
                                AudioInputStream baseStream = new AudioInputStream(candidate);
                                return new LineAndStream(candidate, AudioSystem.getAudioInputStream(format, baseStream));
                            } catch (IllegalArgumentException iae1) {
                                iae1.printStackTrace();
                            }
                        }
                    }
                }
            } catch (LineUnavailableException lue) {
                lue.printStackTrace();
            }
        }
        return null;
    }

    public static File convertToGSM(File input) throws Exception {
        FileInputStream fis = new FileInputStream(input);
        InputStream decorated = new BufferedInputStream(fis);
        InputStream in = PCMToGSMConverter.convert(decorated);
        File outputFile = File.createTempFile(input.getName(), ".gsm");
        outputFile.deleteOnExit();
        FileOutputStream out = new FileOutputStream(outputFile);
        FileUtils.copyStreams(in, out);
        return outputFile;
    }

    public static void synchronizeTelephonySubsystemPrompts(final List<TelephonySubsystem> servers) {
        ProgressMonitorDialog pm = new ProgressMonitorDialog(SafiWorkshopEditorUtil.getActiveShell());
        try {
            pm.run(true, true, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask("Synchronizing prompts with TelephonySubsystem(s)", servers.size());
                        for (TelephonySubsystem server : servers) {
                            monitor.subTask("Synchronizing with " + server.getPlatformId() + server.getName());
                            SafiServerRemoteManager.getInstance().synchAudioFiles(server);
                            monitor.worked(1);
                        }
                        PromptCache.clear();
                    } catch (final Exception e) {
                        e.printStackTrace();
                        final Display d = Display.getDefault();
                        d.asyncExec(new Runnable() {

                            public void run() {
                                MessageDialog.openError(d.getActiveShell(), "Save Error", "Error caught while synchronizing audio prompts: " + e.getLocalizedMessage());
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.openError(SafiWorkshopEditorUtil.getActiveShell(), "Save Error", "Error caught while synchronizing prompts: " + e.getLocalizedMessage());
        }
    }
}
