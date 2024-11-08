package com.sipresponse.mp3wavemachine;

import javax.media.control.TrackControl;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.format.AudioFormat;
import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PullDataSource;
import javax.media.protocol.URLDataSource;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

public class FormatConverter implements ControllerListener, DataSinkListener {

    private DataSink dsink;

    private Processor p;

    private IConverterListener listener;

    Object waitFileSync = new Object();

    boolean fileDone = false;

    boolean fileSuccess = true;

    private Object waitSync = new Object();

    boolean stateTransitionOK = true;

    private int timeoutMsecs = 60000;

    public FormatConverter(IConverterListener listener) {
        this.listener = listener;
    }

    private void fireConverterEvent(int status, int reason) {
        if (null != listener) {
            listener.onConverterEvent(status, reason);
        }
    }

    public void convert(MediaLocator inML, MediaLocator outML, boolean synchronous) {
        if (true == synchronous) {
            new ConverterThread(this, inML, outML).run();
        } else {
            new ConverterThread(this, inML, outML).start();
        }
    }

    private boolean _convert(MediaLocator inML, MediaLocator outML) {
        try {
            p = Manager.createProcessor(inML);
        } catch (Exception e) {
            e.printStackTrace();
            fireConverterEvent(IConverterListener.CONVERTER_FAILURE, IConverterListener.CONVERTER_FAILURE_PROCESSOR_CREATION);
            return false;
        }
        p.addControllerListener(this);
        p.configure();
        if (!waitForState(p, Processor.Configured)) {
            fireConverterEvent(IConverterListener.CONVERTER_FAILURE, IConverterListener.CONVERTER_FAILURE_CONFIGURATION_TIMEOUT);
            return false;
        }
        setContentDescriptor(p, outML);
        Format[] fmts = new Format[1];
        AudioFileFormat inFormat = null;
        try {
            File f = new File(inML.getURL().getPath());
            inFormat = AudioSystem.getAudioFileFormat(f);
        } catch (Exception e) {
            fireConverterEvent(IConverterListener.CONVERTER_FAILURE, IConverterListener.CONVERTER_FAILURE_TRACK_FORMAT);
            return false;
        }
        fmts[0] = new AudioFormat(AudioFormat.MPEG, Math.max(inFormat.getFormat().getSampleRate(), 22050.0), inFormat.getFormat().getFrameSize() * 8 / inFormat.getFormat().getChannels(), 2);
        if (!setTrackFormats(p, fmts)) {
            fireConverterEvent(IConverterListener.CONVERTER_FAILURE, IConverterListener.CONVERTER_FAILURE_TRACK_FORMAT);
            return false;
        }
        p.realize();
        if (!waitForState(p, Processor.Realized)) {
            fireConverterEvent(IConverterListener.CONVERTER_FAILURE, IConverterListener.CONVERTER_FAILURE_NOT_REALIZED);
            return false;
        }
        if ((dsink = createDataSink(p, outML)) == null) {
            fireConverterEvent(IConverterListener.CONVERTER_FAILURE, IConverterListener.CONVERTER_FAILURE_OUTPUT_FILE_CREATION);
            return false;
        }
        dsink.addDataSinkListener(this);
        fileDone = false;
        try {
            p.start();
            dsink.start();
        } catch (IOException e) {
            fireConverterEvent(IConverterListener.CONVERTER_FAILURE, IConverterListener.CONVERTER_FAILURE_IO_ERROR);
            return false;
        }
        return true;
    }

    private void setContentDescriptor(Processor p, MediaLocator outputFile) {
        ContentDescriptor cd;
        if ((cd = fileExtensionToContentDescriptor(outputFile.getRemainder())) != null) {
            if ((p.setContentDescriptor(cd)) == null) {
                p.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW));
            }
        }
    }

    private boolean setTrackFormats(Processor p, Format fmts[]) {
        if (fmts.length == 0) return true;
        TrackControl tcs[];
        if ((tcs = p.getTrackControls()) == null) {
            return false;
        }
        for (int i = 0; i < fmts.length; i++) {
            if (!setEachTrackFormat(p, tcs, fmts[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean setEachTrackFormat(Processor p, TrackControl tcs[], Format fmt) {
        Format supported[];
        Format f;
        for (int i = 0; i < tcs.length; i++) {
            supported = tcs[i].getSupportedFormats();
            if (supported == null) continue;
            for (int j = 0; j < supported.length; j++) {
                if (fmt.matches(supported[j]) && (f = fmt.intersects(supported[j])) != null && tcs[i].setFormat(f) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Create the DataSink.
     */
    private DataSink createDataSink(Processor p, MediaLocator outML) {
        DataSource ds;
        if ((ds = p.getDataOutput()) == null) {
            return null;
        }
        DataSink dsink;
        try {
            dsink = Manager.createDataSink(ds, outML);
            dsink.open();
        } catch (Exception e) {
            return null;
        }
        return dsink;
    }

    private boolean waitForState(Processor p, int state) {
        synchronized (waitSync) {
            try {
                while (p.getState() < state && stateTransitionOK) {
                    waitSync.wait();
                }
            } catch (Exception e) {
            }
        }
        return stateTransitionOK;
    }

    public void controllerUpdate(ControllerEvent evt) {
        if (evt instanceof ConfigureCompleteEvent || evt instanceof RealizeCompleteEvent || evt instanceof PrefetchCompleteEvent) {
            synchronized (waitSync) {
                stateTransitionOK = true;
                waitSync.notifyAll();
            }
        } else if (evt instanceof ResourceUnavailableEvent) {
            synchronized (waitSync) {
                stateTransitionOK = false;
                waitSync.notifyAll();
            }
        } else if (evt instanceof EndOfMediaEvent) {
            evt.getSourceController().close();
            try {
                dsink.close();
            } catch (Exception e) {
            }
            p.removeControllerListener(this);
        }
    }

    public void dataSinkUpdate(DataSinkEvent evt) {
        if (evt instanceof EndOfStreamEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                fileSuccess = true;
                waitFileSync.notifyAll();
            }
        } else if (evt instanceof DataSinkErrorEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                fileSuccess = false;
                waitFileSync.notifyAll();
            }
        }
    }

    private ContentDescriptor fileExtensionToContentDescriptor(String name) {
        String ext;
        int p;
        if ((p = name.lastIndexOf('.')) < 0) return null;
        ext = (name.substring(p + 1)).toLowerCase();
        String type;
        if (ext.equals("mp3")) {
            type = FileTypeDescriptor.MPEG_AUDIO;
        } else {
            if ((type = com.sun.media.MimeManager.getMimeType(ext)) == null) return null;
            type = ContentDescriptor.mimeTypeToPackageName(type);
        }
        return new FileTypeDescriptor(type);
    }

    private class ConverterThread extends Thread {

        private FormatConverter owner = null;

        private MediaLocator inML = null;

        private MediaLocator outML = null;

        public ConverterThread(FormatConverter owner, MediaLocator inML, MediaLocator outML) {
            this.owner = owner;
            this.inML = inML;
            this.outML = outML;
        }

        public void run() {
            boolean ret = owner._convert(inML, outML);
            if (true == ret) {
                int waitTime = 0;
                while (fileDone != true && waitTime < timeoutMsecs) {
                    try {
                        Thread.sleep(20);
                        waitTime += 20;
                    } catch (InterruptedException e) {
                        fireConverterEvent(IConverterListener.CONVERTER_FAILURE, IConverterListener.CONVERTER_FAILURE_INTERRUPTED);
                    }
                }
                if (fileDone == true && fileSuccess == true) {
                    fireConverterEvent(IConverterListener.CONVERTER_SUCCESS, IConverterListener.CONVERTER_OK);
                } else {
                    fireConverterEvent(IConverterListener.CONVERTER_FAILURE, IConverterListener.CONVERTER_FAILURE_IO_ERROR);
                }
                p.close();
            } else {
                if (p != null) {
                    p.close();
                }
            }
        }
    }

    public void setTimeoutMsecs(int timeoutMsecs) {
        this.timeoutMsecs = timeoutMsecs;
    }
}
